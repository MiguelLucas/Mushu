package com.mlucas.mushu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mlucas.mushu.utils.RemoteLogger

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "[Mushu][AlarmReceiver]"
    
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "=== ALARM RECEIVER TRIGGERED ===")
        Log.d(TAG, "Action: ${intent?.action}")
        Log.d(TAG, "Extras: ${intent?.extras}")
        RemoteLogger.log(context, TAG, "AlarmReceiver triggered, action: ${intent?.action}")
        
        intent?.let {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtras(it)
                action = "START_ALARM"
            }
            try {
                Log.d(TAG, "Starting AlarmService from receiver")
                RemoteLogger.log(context, TAG, "Starting AlarmService from receiver")
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "AlarmService started from receiver")
                RemoteLogger.log(context, TAG, "AlarmService start requested successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}", e)
                RemoteLogger.log(context, TAG, "ERROR: Failed to start service: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "Received null intent in AlarmReceiver")
            RemoteLogger.log(context, TAG, "ERROR: Received null intent in AlarmReceiver")
        }
        Log.d(TAG, "=== ALARM RECEIVER END ===")
    }
}