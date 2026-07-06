package il.arik.nadlantracker.core.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import il.arik.nadlantracker.NadlanApp
import il.arik.nadlantracker.di.AppContainer

/** Pulls the manual DI container out of [CreationExtras] for ViewModel factories. */
fun CreationExtras.appContainer(): AppContainer =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as NadlanApp).container
