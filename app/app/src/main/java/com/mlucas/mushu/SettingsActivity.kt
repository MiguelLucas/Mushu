package com.mlucas.mushu

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            findPreference<SwitchPreferenceCompat>(ALARM_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                sharedPreferences.edit().putBoolean(ALARM_ENABLED, newValue as Boolean).apply()
                true
            }

            findPreference<EditTextPreference>(ALARM_MAX_TIME_TO_PLAY)?.setOnPreferenceChangeListener { _, newValue ->
                sharedPreferences.edit().putString(ALARM_MAX_TIME_TO_PLAY, newValue as String).apply()
                true
            }
        }
    }

    companion object {
        const val NAME = "settings"
        const val NOTIFICATIONS_MAX_NUMBER = "notifications_max_number"
        const val ALARM_ENABLED = "alarm_enabled"
        const val ALARM_MAX_TIME_TO_PLAY = "alarm_max_time_to_play"
    }
}