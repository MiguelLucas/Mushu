package com.mlucas.mushu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mlucas.mushu.utils.RemoteLogger

class DebugActivity : AppCompatActivity() {
    
    private lateinit var logsTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create layout programmatically
        val scrollView = ScrollView(this)
        scrollView.setPadding(16, 16, 16, 16)
        
        val linearLayout = android.widget.LinearLayout(this)
        linearLayout.orientation = android.widget.LinearLayout.VERTICAL
        
        // Title
        val titleView = TextView(this)
        titleView.text = "Debug Logs"
        titleView.textSize = 20f
        titleView.setPadding(0, 0, 0, 20)
        linearLayout.addView(titleView)
        
        // Refresh button
        val refreshButton = Button(this)
        refreshButton.text = "Refresh Logs"
        refreshButton.setOnClickListener { refreshLogs() }
        linearLayout.addView(refreshButton)
        
        // Copy button
        val copyButton = Button(this)
        copyButton.text = "Copy to Clipboard"
        copyButton.setOnClickListener { copyLogsToClipboard() }
        linearLayout.addView(copyButton)
        
        // Clear button
        val clearButton = Button(this)
        clearButton.text = "Clear Logs"
        clearButton.setOnClickListener { clearLogs() }
        linearLayout.addView(clearButton)
        
        // Test alarm button
        val testAlarmButton = Button(this)
        testAlarmButton.text = "Test Alarm"
        testAlarmButton.setOnClickListener { testAlarm() }
        linearLayout.addView(testAlarmButton)
        
        // Logs text view
        logsTextView = TextView(this)
        logsTextView.textSize = 12f
        logsTextView.setPadding(0, 20, 0, 0)
        logsTextView.typeface = android.graphics.Typeface.MONOSPACE
        linearLayout.addView(logsTextView)
        
        scrollView.addView(linearLayout)
        setContentView(scrollView)
        
        refreshLogs()
    }
    
    private fun refreshLogs() {
        logsTextView.text = RemoteLogger.getLogs(this)
    }
    
    private fun copyLogsToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Debug Logs", logsTextView.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearLogs() {
        RemoteLogger.clearLogs(this)
        refreshLogs()
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun testAlarm() {
        RemoteLogger.log(this, "DebugActivity", "Manual alarm test triggered")
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            putExtra("title", "Test Alarm")
            putExtra("message", "This is a manual test alarm from debug screen")
            action = "START_ALARM"
        }
        try {
            startForegroundService(serviceIntent)
            Toast.makeText(this, "Test alarm triggered", Toast.LENGTH_SHORT).show()
            RemoteLogger.log(this, "DebugActivity", "Test alarm service started successfully")
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start alarm: ${e.message}", Toast.LENGTH_LONG).show()
            RemoteLogger.log(this, "DebugActivity", "ERROR: Failed to start test alarm: ${e.message}")
        }
    }
}
