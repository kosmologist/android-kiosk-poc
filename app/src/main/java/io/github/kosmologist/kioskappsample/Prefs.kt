package io.github.kosmologist.kioskappsample

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE

/**
 * Created by kosmologist on 5/5/18.
 */
class Prefs {

    companion object {

        private const val PREF_FILE_NAME = "KioskModeDemoPrefs"
        private const val PREF_IS_KIOSK_MODE = "IS_KIOSK_MODE"

        private fun getPrefs(context: Context) =
                context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE)

        fun toggleKioskMode(context: Context) =
                getPrefs(context).edit().putBoolean(PREF_IS_KIOSK_MODE, !isKioskModeEnabled(context)).apply()

        fun isKioskModeEnabled(context: Context) =
                getPrefs(context).getBoolean(PREF_IS_KIOSK_MODE, false)

    }

}