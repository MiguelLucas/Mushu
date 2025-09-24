package com.mlucas.mushu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.mlucas.mushu.utils.BatteryOptimizationHelper
import com.mlucas.mushu.utils.RemoteLogger

class BootReceiver : BroadcastReceiver() {
    private val TAG = "[Mushu][BootReceiver]"

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Boot receiver triggered: ${intent?.action}")
        RemoteLogger.log(context, TAG, "Boot receiver triggered: ${intent?.action}")

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device booted or app updated, initializing alarm system")
                RemoteLogger.log(context, TAG, "Device booted/app updated - initializing alarm system")

                try {
                    // 1. Check if alarms are enabled in settings
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    val alarmEnabled = sharedPreferences.getBoolean(SettingsActivity.ALARM_ENABLED, true)

                    if (!alarmEnabled) {
                        Log.d(TAG, "Alarms disabled in settings, skipping initialization")
                        return
                    }

                    // 2. Check exact alarm permission (critical for Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            Log.w(TAG, "Cannot schedule exact alarms after boot - user needs to grant permission")
                            RemoteLogger.log(context, TAG, "WARNING: Cannot schedule exact alarms after boot")
                        } else {
                            Log.d(TAG, "Exact alarm permission available")
                        }
                    }

                    // 3. Check battery optimization status
                    val batteryOptStatus = BatteryOptimizationHelper.getOptimizationStatus(context)
                    Log.d(TAG, "Battery optimization status: $batteryOptStatus")
                    RemoteLogger.log(context, TAG, "Battery optimization: $batteryOptStatus")

                    // 4. For Pixel devices, create a keepalive alarm
                    val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
                    if (isPixelDevice) {
                        RemoteLogger.log(context, TAG, "PIXEL: Setting up keepalive mechanism for force-close scenarios")
                        setupPixelKeepalive(context)
                    }

                    // 5. Log device info for debugging
                    Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android: ${Build.VERSION.RELEASE}")
                    RemoteLogger.log(context, TAG, "Alarm system ready on ${Build.MANUFACTURER} ${Build.MODEL}")

                    Log.d(TAG, "Alarm system initialization completed successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "Error during alarm system initialization", e)
                    RemoteLogger.log(context, TAG, "ERROR: Boot initialization failed: ${e.message}")
                }
            }
        }
    }

    private fun setupPixelKeepalive(context: Context) {
        try {
            RemoteLogger.log(context, TAG, "PIXEL: Setting up 15-minute keepalive alarm for FCM reliability")

            val intent = Intent(context, PixelKeepaliveReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                12345, // Fixed request code for keepalive
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Set a repeating alarm every 15 minutes to keep the app "alive" in Android's memory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + (15 * 60 * 1000), // Start in 15 minutes
                    15 * 60 * 1000, // Repeat every 15 minutes
                    pendingIntent
                )
                RemoteLogger.log(context, TAG, "PIXEL: Keepalive alarm scheduled successfully")
            }
        } catch (e: Exception) {
            RemoteLogger.log(context, TAG, "PIXEL: ERROR setting up keepalive: ${e.message}")
        }
    }
}
