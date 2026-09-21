package com.wledclimb.app.wall

import com.wledclimb.app.FakeWledClient
import com.wledclimb.app.MainDispatcherRule
import com.wledclimb.app.ONE_DIMENSIONAL_CONFIG
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertEquals(0, state.wall.ledIndexAt(x = 0, y = 0))
        assertEquals(3, state.wall.ledIndexAt(x = 1, y = 1))
    }

    @Test
    fun `applies the gap file to the grid when the controller has one`() = runTest {
        // Second cell has no LED behind it, so it should be blank and must not
        // shift the index of the cell after it.
        val viewModel = WallViewModel(FakeWledClient(gaps = "[1,-1,1,1]"))

        val wall = connectedState(viewModel).wall
        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertNull(wall.ledIndexAt(x = 1, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 0, y = 1))
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

        assertEquals(mapOf(2 to "FF0000"), connectedState(viewModel).litHolds)
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

        assertEquals(emptyMap<Int, String>(), connectedState(viewModel).litHolds)
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
        assertEquals(mapOf(1 to "FF0000"), state.litHolds)
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
}
