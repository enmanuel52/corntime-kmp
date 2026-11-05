package com.enmanuelbergling.corntime.core.testing.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/** Clears the ViewModel the way leaving its screen does, so `onCleared()` runs. */
@Suppress("UNCHECKED_CAST")
fun ViewModel.clearForTest() {
    val viewModel = this
    val store = ViewModelStore()
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = viewModel as T
    }
    ViewModelProvider.create(store, factory)[viewModel::class as KClass<ViewModel>]
    store.clear()
}
