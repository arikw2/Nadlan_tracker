package il.arik.nadlantracker.core.ui

import android.content.Context

/** One-key preference store for the first-run gate. Kept trivial on purpose. */
object AppPrefs {
    private const val FILE = "nadlan_prefs"
    private const val KEY_ONBOARDED = "onboarded"

    fun isOnboarded(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDED, true).apply()
    }
}
