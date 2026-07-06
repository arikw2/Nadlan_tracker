package il.arik.nadlantracker.di

import android.content.Context

/**
 * Manual dependency container. Kept deliberately simple (no Hilt) — a single
 * module app with a handful of screens. Repositories are added as the data
 * layer is built out.
 */
class AppContainer(private val appContext: Context)
