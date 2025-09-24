package com.mlucas.mushu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mlucas.mushu.utils.RemoteLogger

class PixelKeepaliveReceiver : BroadcastReceiver() {
    private val TAG = "[Mushu][PixelKeepalive]"

    override fun onReceive(context: Context, intent: Intent?) {
        RemoteLogger.log(context, TAG, "PIXEL: Keepalive receiver triggered")

        try {
            val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
            if (!isPixelDevice) {
                RemoteLogger.log(context, TAG, "Not a Pixel device, stopping keepalive")
                return
            }

            // Reinitialize FCM to ensure it's active after force-close
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        RemoteLogger.log(context, TAG, "PIXEL: FCM token refreshed successfully")
                        Log.d(TAG, "FCM token refreshed: ${token?.take(20)}...")
                    } else {
                        RemoteLogger.log(context, TAG, "PIXEL: FCM token refresh failed: ${task.exception?.message}")
                    }
                }
            } catch (e: Exception) {
                RemoteLogger.log(context, TAG, "PIXEL: Error refreshing FCM token: ${e.message}")
            }

            // Log that we're keeping the app alive for debugging
            RemoteLogger.log(context, TAG, "PIXEL: Keepalive ping completed - FCM should be active")

        } catch (e: Exception) {
            RemoteLogger.log(context, TAG, "PIXEL: Keepalive error: ${e.message}")
        }
    }
}
