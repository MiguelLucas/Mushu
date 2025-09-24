package com.mlucas.mushu.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.mlucas.mushu.R

object BatteryOptimizationHelper {
    private const val TAG = "[Mushu][BatteryOptimization]"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // No battery optimization on older versions
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption(context: Context): Intent? {
        return if (!isIgnoringBatteryOptimizations(context)) {
            try {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create battery optimization intent", e)
                // Fallback to general battery optimization settings
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
        } else {
            null
        }
    }

    fun getOptimizationStatus(context: Context): String {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> "Not applicable (Android < 6.0)"
            isIgnoringBatteryOptimizations(context) -> "Exempted from battery optimization"
            else -> "Subject to battery optimization"
        }
    }

    fun shouldRequestExemption(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
               !isIgnoringBatteryOptimizations(context)
    }

    /**
     * Pixel-specific checks for additional power management features
     */
    fun getPixelSpecificStatus(context: Context): String {
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        if (!isPixelDevice) {
            return "Not a Pixel device"
        }

        val status = StringBuilder()
        status.append("Pixel device detected: ${Build.MODEL}\n")

        // Check battery optimization
        status.append("Battery optimization: ${getOptimizationStatus(context)}\n")

        // Check if adaptive battery might be affecting the app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            status.append("Adaptive Battery: Potentially active (Android 9+)\n")
        }

        // Check app standby bucket (approximate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                status.append("App Standby: May be subject to restrictions\n")
            } catch (e: Exception) {
                status.append("App Standby: Cannot determine status\n")
            }
        }

        return status.toString()
    }

    /**
     * Creates an intent to guide Pixel users to the right battery settings
     */
    fun getPixelBatterySettingsIntent(context: Context): Intent {
        return try {
            // Try to open app-specific battery optimization settings
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            // Fallback to general battery settings
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Checks if this is a Pixel device that might have alarm issues
     */
    fun isProblematicPixelDevice(): Boolean {
        val isPixel = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        val isPixel6Series = Build.MODEL.contains("Pixel 6", ignoreCase = true)
        return isPixel && (isPixel6Series || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
}
