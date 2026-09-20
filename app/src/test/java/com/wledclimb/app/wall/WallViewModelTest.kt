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

        val state = viewModel.uiState.value as WallUiState.Error
        assertTrue(state.message.contains("Couldn't reach"))
        assertTrue(state.message.contains("Wi-Fi"))
    }

    @Test
    fun `a controller that isn't a 2D matrix reports a config problem, not a network one`() = runTest {
        val viewModel = WallViewModel(FakeWledClient(config = ONE_DIMENSIONAL_CONFIG))

        val state = viewModel.uiState.value as WallUiState.Error
        assertTrue(state.message.contains("2D matrix"))
        // The controller answered fine - telling the user to check Wi-Fi would
        // send them off debugging the wrong thing entirely.
        assertTrue(!state.message.contains("Wi-Fi"))
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
}
