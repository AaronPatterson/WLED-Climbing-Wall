package com.wledclimb.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wledclimb.app.settings.WledSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Which screen the app should show. */
sealed interface RootUiState {
    data object Loading : RootUiState

    /** [currentUrl] pre-fills the setup screen when returning to it to change an already-working address. */
    data class NeedsSetup(val currentUrl: String? = null) : RootUiState

    data class Ready(val wledBaseUrl: String) : RootUiState
}

/**
 * Decides whether to show the setup screen or wall control, based on whether
 * a WLED controller address has already been saved.
 */
class RootViewModel(private val settings: WledSettings) : ViewModel() {

    private val _uiState = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUrl = settings.wledIp.first()
            _uiState.value = if (savedUrl != null) RootUiState.Ready(savedUrl) else RootUiState.NeedsSetup()
        }
    }

    fun onSetupComplete(wledBaseUrl: String) {
        _uiState.value = RootUiState.Ready(wledBaseUrl)
    }

    /** Called from the wall screen to go back and point the app at a different controller. */
    fun onChangeController() {
        val current = _uiState.value as? RootUiState.Ready ?: return
        _uiState.value = RootUiState.NeedsSetup(currentUrl = current.wledBaseUrl)
    }
}

/**
 * Builds a ViewModel from a plain constructor call. Lets screens whose ViewModels
 * need constructor args (a saved URL, a settings instance) still use `viewModels()`/
 * `viewModel()` instead of hand-rolling a `ViewModelProvider.Factory` per class.
 */
class LambdaViewModelFactory<VM : ViewModel>(private val create: () -> VM) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
