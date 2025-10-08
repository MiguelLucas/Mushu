package com.mlucas.mushu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtras(intent!!)
            action = "START_ALARM"
        }

        context.startForegroundService(serviceIntent)
    }
}