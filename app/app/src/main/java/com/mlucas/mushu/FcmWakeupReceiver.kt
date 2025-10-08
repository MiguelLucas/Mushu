package com.mlucas.mushu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class FcmWakeupReceiver : BroadcastReceiver() {
    private val TAG = "[Mushu][FcmWakeupReceiver]"

    override fun onReceive(context: Context, intent: Intent?) {
        val firebaseAnalytics = Firebase.analytics

        Log.d(TAG, "FcmWakeupReceiver triggered while app was dead")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android version: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Intent action: ${intent?.action}")

        firebaseAnalytics.logEvent("fcm_wakeup_receiver_triggered", Bundle().apply {
            putString("device_manufacturer", Build.MANUFACTURER)
            putString("device_model", Build.MODEL)
            putInt("android_version", Build.VERSION.SDK_INT)
            putString("intent_action", intent?.action ?: "null")
        })

        // This will be called even when app is force-closed
        // Start the FCM service to process the message
        try {
            val serviceIntent = Intent(context, FirebaseCloudMessagingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "FCM service started via startForegroundService from dead state")

                firebaseAnalytics.logEvent("fcm_service_started_from_dead", Bundle().apply {
                    putString("method", "startForegroundService")
                    putString("device_model", Build.MODEL)
                })
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "FCM service started via startService from dead state")

                firebaseAnalytics.logEvent("fcm_service_started_from_dead", Bundle().apply {
                    putString("method", "startService")
                    putString("device_model", Build.MODEL)
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "ERROR starting FCM service: ${e.message}", e)

            firebaseAnalytics.logEvent("fcm_wakeup_error", Bundle().apply {
                putString("error_message", e.message ?: "unknown")
                putString("device_model", Build.MODEL)
                putString("exception_type", e.javaClass.simpleName)
            })
        }
    }
}
