package com.enmanuelbergling.corntime.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer

/**
 * A ViewModel together with the store that owns it. SwiftUI keeps this for as long as the screen
 * lives and calls [clear] when it goes away, which cancels the ViewModel's coroutines
 * (`viewModelScope`) exactly like Android does when a back stack entry is removed.
 */
class ScopedViewModel<out VM : ViewModel> internal constructor(
    val viewModel: VM,
    private val store: ViewModelStore,
) {
    fun clear() = store.clear()
}

internal inline fun <reified VM : ViewModel> scoped(crossinline create: () -> VM): ScopedViewModel<VM> {
    val store = ViewModelStore()
    val factory = viewModelFactory { initializer { create() } }
    val viewModel = ViewModelProvider.create(store, factory)[VM::class]
    return ScopedViewModel(viewModel, store)
}
