package com.wledclimb.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Builds a ViewModel from a plain constructor call. Lets screens whose ViewModels
 * need constructor args (a saved URL, a settings instance) still use `viewModels()`/
 * `viewModel()` instead of hand-rolling a `ViewModelProvider.Factory` per class.
 */
class LambdaViewModelFactory<VM : ViewModel>(private val builder: () -> VM) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
}
