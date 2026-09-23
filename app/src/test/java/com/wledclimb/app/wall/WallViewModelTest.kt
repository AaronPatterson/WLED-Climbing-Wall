package com.wledclimb.app.wall

import com.wledclimb.app.FakeWledClient
import com.wledclimb.app.MainDispatcherRule
import com.wledclimb.app.ONE_DIMENSIONAL_CONFIG
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class WallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun connectedState(viewModel: WallViewModel): WallUiState.Connected =
        viewModel.uiState.value as? WallUiState.Connected
            ?: error("Expected Connected but was ${viewModel.uiState.value}")

    @Test
    fun `loads power state and grid layout on creation`() = runTest {
        val viewModel = WallViewModel(FakeWledClient(on = true))

        val state = connectedState(viewModel)
        assertTrue(state.on)
        assertEquals(2, state.wall.width)
        assertEquals(2, state.wall.height)
        assertTrue(state.wall.hasHoldAt(x = 0, y = 0))
        assertTrue(state.wall.hasHoldAt(x = 1, y = 1))
    }

    @Test
    fun `applies the gap file to the grid when the controller has one`() = runTest {
        // Second cell has no LED behind it, so there is no hold to light there.
        val viewModel = WallViewModel(FakeWledClient(gaps = "[1,-1,1,1]"))

        val wall = connectedState(viewModel).wall
        assertTrue(wall.hasHoldAt(x = 0, y = 0))
        assertFalse(wall.hasHoldAt(x = 1, y = 0))
        assertTrue(wall.hasHoldAt(x = 0, y = 1))
        assertEquals(3, wall.holdCount)
    }

    @Test
    fun `a failed brightness change does not tear down the screen`() = runTest {
        // The reported bug: dragging the brightness slider dropped the whole
        // screen to "couldn't reach the wall", losing the grid and the route.
        // A dropped brightness request is not evidence the wall has gone - it
        // is one request among many during a drag.
        val client = FakeWledClient(on = true)
        val viewModel = WallViewModel(client)
        val before = connectedState(viewModel)
        viewModel.toggleHold(before.wall.segmentIndexAt(x = 0, y = 0))
        runCurrent()

        client.failWith = IOException("unexpected end of stream")
        viewModel.setBrightness(200)
        runCurrent()

        val after = viewModel.uiState.value
        assertTrue("expected to stay connected, was $after", after is WallUiState.Connected)
        assertEquals(
            "the route should survive a failed brightness change",
            1,
            (after as WallUiState.Connected).litHolds.size
        )
    }

    @Test
    fun `toggling flips the wall and keeps the grid`() = runTest {
        val client = FakeWledClient(on = false)
        val viewModel = WallViewModel(client)
        val wallBefore = connectedState(viewModel).wall

        viewModel.toggleWall()

        val state = connectedState(viewModel)
        assertTrue(state.on)
        assertEquals(listOf(true), client.setOnCalls)
        // The grid comes from /json/cfg, which a toggle doesn't re-read - it has
        // to be carried across or the grid would vanish on every tap.
        assertEquals(wallBefore, state.wall)
        assertEquals(false, state.busy)
    }

    @Test
    fun `toggling does nothing when not connected`() = runTest {
        val client = FakeWledClient(failWith = IOException("boom"))
        val viewModel = WallViewModel(client)

        viewModel.toggleWall()

        assertTrue(viewModel.uiState.value is WallUiState.Error)
        assertEquals(emptyList<Boolean>(), client.setOnCalls)
    }

    @Test
    fun `an unreachable controller reports a network problem`() = runTest {
        val viewModel = WallViewModel(FakeWledClient(failWith = IOException("connect timed out")))

        assertEquals(WallUiState.Error(WallProblem.Unreachable), viewModel.uiState.value)
    }

    @Test
    fun `a controller that isn't a 2D matrix reports a config problem, not a network one`() = runTest {
        val viewModel = WallViewModel(FakeWledClient(config = ONE_DIMENSIONAL_CONFIG))

        // The controller answered fine - reporting this as a network problem
        // would send the user off debugging the wrong thing entirely.
        assertEquals(WallUiState.Error(WallProblem.NotAWledMatrix), viewModel.uiState.value)
    }

    @Test
    fun `retrying after a failure reconnects`() = runTest {
        val client = FakeWledClient(failWith = IOException("down"))
        val viewModel = WallViewModel(client)
        assertTrue(viewModel.uiState.value is WallUiState.Error)

        client.failWith = null
        viewModel.refresh()

        assertTrue(viewModel.uiState.value is WallUiState.Connected)
    }

    @Test
    fun `tapping a hold lights it and pushes the route`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)

        viewModel.toggleHold(segmentIndex = 2)

        assertEquals(mapOf(2 to HoldColor.Red), connectedState(viewModel).litHolds)
        assertEquals(listOf(mapOf(2 to "FF0000")), client.pushedHolds)
        // The clear range has to cover the whole 2x2 segment buffer, or holds
        // outside it stay lit.
        assertEquals(4, client.lastPixelCount)
    }

    @Test
    fun `tapping a lit hold clears it`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)
        viewModel.toggleHold(segmentIndex = 2)

        viewModel.toggleHold(segmentIndex = 2)

        assertEquals(emptyMap<Int, HoldColor>(), connectedState(viewModel).litHolds)
        assertEquals(emptyMap<Int, String>(), client.pushedHolds.last())
    }

    @Test
    fun `each push carries the whole route, not just the hold that changed`() = runTest {
        // WLED keeps previously set pixels, so an incremental push would leave
        // a cleared hold lit on the wall.
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)

        viewModel.toggleHold(segmentIndex = 0)
        viewModel.toggleHold(segmentIndex = 3)

        assertEquals(mapOf(0 to "FF0000", 3 to "FF0000"), client.pushedHolds.last())
    }

    @Test
    fun `powering the wall off keeps the route in the app`() = runTest {
        val viewModel = WallViewModel(FakeWledClient(on = true))
        viewModel.toggleHold(segmentIndex = 1)

        viewModel.toggleWall()

        val state = connectedState(viewModel)
        assertEquals(false, state.on)
        assertEquals(mapOf(1 to HoldColor.Red), state.litHolds)
    }

    @Test
    fun `switching the wall back on re-pushes the route`() = runTest {
        // WLED unfreezes every segment when it's switched on, dropping the
        // per-pixel route - without re-pushing, the app would still show a
        // route the wall had already forgotten.
        val client = FakeWledClient(on = false)
        val viewModel = WallViewModel(client)
        viewModel.toggleHold(segmentIndex = 1)
        val pushesBefore = client.pushedHolds.size

        viewModel.toggleWall()

        assertEquals(true, connectedState(viewModel).on)
        assertEquals(pushesBefore + 1, client.pushedHolds.size)
        assertEquals(mapOf(1 to "FF0000"), client.pushedHolds.last())
    }

    @Test
    fun `switching the wall on with no route pushes nothing`() = runTest {
        val client = FakeWledClient(on = false)
        val viewModel = WallViewModel(client)

        viewModel.toggleWall()

        assertEquals(emptyList<Map<Int, String>>(), client.pushedHolds)
    }

    @Test
    fun `a failed push surfaces the error rather than leaving the wall out of sync`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)
        client.failWith = IOException("gone")

        viewModel.toggleHold(segmentIndex = 1)

        assertEquals(WallUiState.Error(WallProblem.Unreachable), viewModel.uiState.value)
    }

    @Test
    fun `tapping a hold does nothing when not connected`() = runTest {
        val client = FakeWledClient(failWith = IOException("down"))
        val viewModel = WallViewModel(client)

        viewModel.toggleHold(segmentIndex = 1)

        assertEquals(emptyList<Map<Int, String>>(), client.pushedHolds)
    }

    @Test
    fun `holds are painted in the selected colour`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)

        viewModel.selectColor(HoldColor.Blue)
        viewModel.toggleHold(segmentIndex = 1)

        assertEquals(mapOf(1 to HoldColor.Blue), connectedState(viewModel).litHolds)
        // The wire format is WLED's hex, not the enum.
        assertEquals(mapOf(1 to "2979FF"), client.pushedHolds.last())
    }

    @Test
    fun `tapping a hold already in the selected colour clears it`() = runTest {
        val viewModel = WallViewModel(FakeWledClient())
        viewModel.selectColor(HoldColor.Green)
        viewModel.toggleHold(segmentIndex = 1)

        viewModel.toggleHold(segmentIndex = 1)

        assertEquals(emptyMap<Int, HoldColor>(), connectedState(viewModel).litHolds)
    }

    @Test
    fun `tapping a hold showing a different colour repaints it`() = runTest {
        // Repaint rather than clear: needing to erase before recolouring would
        // be a fiddly extra step for a six-year-old.
        val viewModel = WallViewModel(FakeWledClient())
        viewModel.selectColor(HoldColor.Green)
        viewModel.toggleHold(segmentIndex = 1)

        viewModel.selectColor(HoldColor.Purple)
        viewModel.toggleHold(segmentIndex = 1)

        assertEquals(mapOf(1 to HoldColor.Purple), connectedState(viewModel).litHolds)
    }

    @Test
    fun `changing colour leaves holds already on the wall alone`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)
        viewModel.toggleHold(segmentIndex = 1)
        val pushesBefore = client.pushedHolds.size

        viewModel.selectColor(HoldColor.Yellow)

        assertEquals(mapOf(1 to HoldColor.Red), connectedState(viewModel).litHolds)
        assertEquals(pushesBefore, client.pushedHolds.size)
    }

    @Test
    fun `clearing turns every hold off in one push`() = runTest {
        // The point of this over tapping each hold: one request, not one per hold.
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)
        viewModel.toggleHold(segmentIndex = 0)
        viewModel.toggleHold(segmentIndex = 3)
        val pushesBefore = client.pushedHolds.size

        viewModel.clearWall()

        assertEquals(emptyMap<Int, HoldColor>(), connectedState(viewModel).litHolds)
        assertEquals(pushesBefore + 1, client.pushedHolds.size)
        assertEquals(emptyMap<Int, String>(), client.pushedHolds.last())
    }

    @Test
    fun `clearing an already empty wall does nothing`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)

        viewModel.clearWall()

        assertEquals(emptyList<Map<Int, String>>(), client.pushedHolds)
    }

    @Test
    fun `a failed clear surfaces the error`() = runTest {
        val client = FakeWledClient()
        val viewModel = WallViewModel(client)
        viewModel.toggleHold(segmentIndex = 1)
        client.failWith = IOException("gone")

        viewModel.clearWall()

        assertEquals(WallUiState.Error(WallProblem.Unreachable), viewModel.uiState.value)
    }
}
