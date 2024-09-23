package com.mlucas.mushu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManager
    private lateinit var timer: CountDownTimer
    private lateinit var ringtone: Uri

    private val timeToStopAlarm: Long = 60
    private var TAG: String = "[Mushu][AlarmService]"


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating alarm service")
        ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, ringtone)
        mediaPlayer.isLooping = true
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_ALARM" -> startAlarm(intent)
            "STOP_ALARM" -> stopAlarm()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startAlarm(intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Alarm title"
        val message = intent.getStringExtra("message") ?: "Alarm triggered"

        var randomId: Long = System.currentTimeMillis()%10000
        startForeground(randomId.toInt(), createNotification(title, message))
        mediaPlayer.start()

        Log.d(TAG, "Starting alarm service")
        // Start a timer to automatically stop the alarm after 10 seconds
        timer = object : CountDownTimer(timeToStopAlarm * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.d(TAG, "Stopping alarm via timer")
                stopAlarm()
            }
        }.start()
    }

    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm service")

        mediaPlayer.stop()
        mediaPlayer.prepare()
        timer.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(title: String, message: String): Notification {
        Log.d(TAG, "Creating alarm service notification")

        val channelId: String = R.string.alarm_notification_channel_id.toString()
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "Alarm notification channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
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
}