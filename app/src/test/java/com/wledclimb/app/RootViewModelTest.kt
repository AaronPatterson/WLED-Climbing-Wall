package com.wledclimb.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `routes to wall control when an address is already saved`() = runTest {
        val viewModel = RootViewModel(FakeWledSettings(initialIp = "http://192.168.1.50"))

        assertEquals(RootUiState.Ready("http://192.168.1.50"), viewModel.uiState.value)
    }

    @Test
    fun `routes to setup when no address has been saved`() = runTest {
        val viewModel = RootViewModel(FakeWledSettings(initialIp = null))

        assertEquals(RootUiState.NeedsSetup(currentUrl = null), viewModel.uiState.value)
    }

    @Test
    fun `completing setup moves on to wall control`() = runTest {
        val viewModel = RootViewModel(FakeWledSettings(initialIp = null))

        viewModel.onSetupComplete("http://192.168.1.60")

        assertEquals(RootUiState.Ready("http://192.168.1.60"), viewModel.uiState.value)
    }

    @Test
    fun `changing controller returns to setup pre-filled with the current address`() = runTest {
        val viewModel = RootViewModel(FakeWledSettings(initialIp = "http://192.168.1.50"))

        viewModel.onChangeController()

        assertEquals(RootUiState.NeedsSetup(currentUrl = "http://192.168.1.50"), viewModel.uiState.value)
    }

    @Test
    fun `changing controller does nothing when setup hasn't completed yet`() = runTest {
        val viewModel = RootViewModel(FakeWledSettings(initialIp = null))

        viewModel.onChangeController()

        // Still on setup, and crucially not NeedsSetup(currentUrl = something).
        assertEquals(RootUiState.NeedsSetup(currentUrl = null), viewModel.uiState.value)
    }
}
