package com.mlucas.mushu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager

import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager


class AlarmService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var timer: CountDownTimer

    private var timeToStopAlarm: Long = 60
    private var alarmEnabled: Boolean = true
    private val TAG: String = "[Mushu][AlarmService]"
    private val notificationChannelId: String = "alarm_notification_channel"
    private val notificationChannelName: String = "Alarm Notifications"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        alarmEnabled = sharedPreferences.getBoolean(SettingsActivity.ALARM_ENABLED, true)
        timeToStopAlarm = sharedPreferences.getString(SettingsActivity.ALARM_MAX_TIME_TO_PLAY, "60")?.toLong()!!

        Log.d(TAG, "Alarm enabled: $alarmEnabled, Time to stop: $timeToStopAlarm")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!alarmEnabled) {
            Log.d(TAG, "Alarm service is not enabled!")
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == "START_ALARM") {
            val title = intent.getStringExtra("title") ?: "Mushu Alert"
            val message = intent.getStringExtra("message") ?: "This is your alarm message"

            val randomId: Long = System.currentTimeMillis() % 10000
            val notification = createNotification(title, message)

            Log.d(TAG, "Starting alarm service, finishing in " + (timeToStopAlarm * 1000))

            startForeground(randomId.toInt(), notification)
            playAlarm()

            // Stop alarm automatically after 10 seconds
            timer = object : CountDownTimer(timeToStopAlarm * 1000, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    // Log remaining time if needed
                }
                override fun onFinish() {
                    stopAlarm()
                }
            }
            timer.start()
        } else if (intent?.action == "STOP_ALARM") {
            stopAlarm()
        }
        return START_STICKY
    }

    private fun playAlarm() {
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    private fun stopAlarm() {
        // Safely check if mediaPlayer is initialized and playing
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }

        // Cancel the countdown timer if it's still running
        if (::timer.isInitialized) {
            timer.cancel()
        }

        // Stop the foreground service
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(title: String, message: String): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_dialog_alert, "Stop", stopPendingIntent)
            .setAutoCancel(true)
            .setDeleteIntent(stopPendingIntent)

        return notificationBuilder.build()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH

        val notificationChannel = NotificationChannel(notificationChannelId, notificationChannelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}