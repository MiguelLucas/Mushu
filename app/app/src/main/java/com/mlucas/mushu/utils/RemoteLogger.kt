package com.mlucas.mushu.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object RemoteLogger {
    private const val PREFS_NAME = "debug_logs"
    private const val LOGS_KEY = "logs"
    private const val MAX_LOGS = 100
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $tag: $message"
        
        val prefs = getPrefs(context)
        val existingLogs = prefs.getString(LOGS_KEY, "") ?: ""
        val logsList = if (existingLogs.isEmpty()) mutableListOf() else existingLogs.split("\n").toMutableList()
        
        logsList.add(0, logEntry) // Add to beginning
        
        // Keep only last MAX_LOGS entries
        if (logsList.size > MAX_LOGS) {
            logsList.removeAt(logsList.size - 1)
        }
        
        prefs.edit().putString(LOGS_KEY, logsList.joinToString("\n")).apply()
    }
    
    fun getLogs(context: Context): String {
        return getPrefs(context).getString(LOGS_KEY, "No logs yet") ?: "No logs yet"
    }
    
    fun clearLogs(context: Context) {
        getPrefs(context).edit().remove(LOGS_KEY).apply()
    }
}
