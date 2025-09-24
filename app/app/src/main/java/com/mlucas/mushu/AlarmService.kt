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
import android.os.Build

import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mlucas.mushu.utils.RemoteLogger


class AlarmService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var timer: CountDownTimer
    private lateinit var wakeLock: PowerManager.WakeLock

    private var timeToStopAlarm: Long = 60
    private var alarmEnabled: Boolean = true
    private val TAG: String = "[Mushu][AlarmService]"
    private val notificationChannelId: String = "alarm_notification_channel"
    private val notificationChannelName: String = "Alarm Notifications"

    override fun onCreate() {
        super.onCreate()
        RemoteLogger.log(this, TAG, "AlarmService.onCreate() started")
        RemoteLogger.log(this, TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}, API: ${Build.VERSION.SDK_INT}")

        createNotificationChannel()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        alarmEnabled = sharedPreferences.getBoolean(SettingsActivity.ALARM_ENABLED, true)
        timeToStopAlarm = sharedPreferences.getString(SettingsActivity.ALARM_MAX_TIME_TO_PLAY, "60")?.toLong()!!

        Log.d(TAG, "Alarm enabled: $alarmEnabled, Time to stop: $timeToStopAlarm")
        RemoteLogger.log(this, TAG, "AlarmService configuration - enabled: $alarmEnabled, timeout: ${timeToStopAlarm}s")

        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        RemoteLogger.log(this, TAG, "AlarmService initialized on ${if (isPixelDevice) "PIXEL" else "NON-PIXEL"} device")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RemoteLogger.log(this, TAG, "=== ALARM SERVICE START COMMAND ===")
        RemoteLogger.log(this, TAG, "Intent action: ${intent?.action}")
        RemoteLogger.log(this, TAG, "Start ID: $startId, Flags: $flags")
        RemoteLogger.log(this, TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")

        // Log to Crashlytics for remote debugging
        FirebaseCrashlytics.getInstance().log("AlarmService.onStartCommand called - enabled: $alarmEnabled")
        RemoteLogger.log(this, TAG, "Alarm enabled in settings: $alarmEnabled")

        if (!alarmEnabled) {
            Log.d(TAG, "Alarm service is not enabled!")
            FirebaseCrashlytics.getInstance().log("Alarm service disabled, stopping")
            RemoteLogger.log(this, TAG, "CRITICAL: Alarm service disabled in settings - stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Enhanced wake lock logging for Pixel devices
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL" else "STANDARD"}: Acquiring wake lock")

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Mushu::AlarmWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) //10 minutes
        RemoteLogger.log(this, TAG, "Wake lock acquired successfully - timeout: 10 minutes")

        if (intent?.action == "START_ALARM") {
            val title = intent.getStringExtra("title") ?: "Mushu Alert"
            val message = intent.getStringExtra("message") ?: "This is your alarm message"

            RemoteLogger.log(this, TAG, "ALARM START: Title='$title', Message='$message'")
            FirebaseCrashlytics.getInstance().log("Starting alarm: $title")
            
            val randomId: Long = System.currentTimeMillis() % 10000
            RemoteLogger.log(this, TAG, "Generated notification ID: $randomId")

            val notification = createNotification(title, message)

            Log.d(TAG, "Starting alarm service, finishing in " + (timeToStopAlarm * 1000))
            RemoteLogger.log(this, TAG, "Starting foreground service with ${timeToStopAlarm}s timeout")

            try {
                startForeground(randomId.toInt(), notification)
                RemoteLogger.log(this, TAG, "Foreground service started successfully")

                playAlarm()
                RemoteLogger.log(this, TAG, "Alarm sound playback initiated")

                FirebaseCrashlytics.getInstance().log("Alarm started successfully, will stop in ${timeToStopAlarm}s")

                // Stop alarm automatically after specified time
                timer = object : CountDownTimer(timeToStopAlarm * 1000, 1_000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Log remaining time if needed
                    }
                    override fun onFinish() {
                        FirebaseCrashlytics.getInstance().log("Timer finished, stopping alarm")
                        RemoteLogger.log(this@AlarmService, TAG, "Timer expired - auto-stopping alarm")
                        stopAlarm()
                    }
                }
                timer.start()
                RemoteLogger.log(this, TAG, "Countdown timer started for ${timeToStopAlarm}s")
            } catch (e: Exception) {
                RemoteLogger.log(this, TAG, "CRITICAL ERROR: Failed to start foreground service: ${e.message}")
                FirebaseCrashlytics.getInstance().log("Failed to start foreground service: ${e.message}")
            }
        } else if (intent?.action == "STOP_ALARM") {
            RemoteLogger.log(this, TAG, "Manual alarm stop requested")
            FirebaseCrashlytics.getInstance().log("Manual alarm stop requested")
            stopAlarm()
        } else {
            RemoteLogger.log(this, TAG, "Unknown intent action: ${intent?.action}")
        }

        RemoteLogger.log(this, TAG, "=== ALARM SERVICE START COMMAND COMPLETE ===")
        return START_STICKY
    }

    private fun playAlarm() {
        RemoteLogger.log(this, TAG, "playAlarm() called - initializing MediaPlayer")
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            RemoteLogger.log(this, TAG, "Alarm URI: $alarmUri")

            mediaPlayer = MediaPlayer.create(this, alarmUri)
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.isLooping = true
                mediaPlayer.start()
                RemoteLogger.log(this, TAG, "MediaPlayer created and started successfully - looping enabled")

                val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
                if (isPixelDevice) {
                    RemoteLogger.log(this, TAG, "PIXEL: Alarm sound started with volume: ${mediaPlayer.getAudioSessionId()}")
                }
            } else {
                RemoteLogger.log(this, TAG, "CRITICAL ERROR: MediaPlayer failed to initialize")
            }
        } catch (e: Exception) {
            RemoteLogger.log(this, TAG, "CRITICAL ERROR: Exception in playAlarm(): ${e.message}")
            FirebaseCrashlytics.getInstance().log("playAlarm failed: ${e.message}")
        }
    }

    private fun stopAlarm() {
        RemoteLogger.log(this, TAG, "=== STOP ALARM CALLED ===")
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)

        // Safely check if mediaPlayer is initialized and playing
        if (::mediaPlayer.isInitialized) {
            if (mediaPlayer.isPlaying) {
                RemoteLogger.log(this, TAG, "Stopping MediaPlayer - was playing")
                mediaPlayer.stop()
                RemoteLogger.log(this, TAG, "MediaPlayer stopped successfully")
            } else {
                RemoteLogger.log(this, TAG, "MediaPlayer was not playing")
            }
            try {
                mediaPlayer.release()
                RemoteLogger.log(this, TAG, "MediaPlayer resources released")
            } catch (e: Exception) {
                RemoteLogger.log(this, TAG, "ERROR releasing MediaPlayer: ${e.message}")
            }
        } else {
            RemoteLogger.log(this, TAG, "MediaPlayer was not initialized")
        }

        // Cancel the countdown timer if it's still running
        if (::timer.isInitialized) {
            timer.cancel()
            RemoteLogger.log(this, TAG, "Countdown timer cancelled")
        } else {
            RemoteLogger.log(this, TAG, "Timer was not initialized")
        }

        // Release the wake lock if it exists
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
            RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL" else "STANDARD"}: Wake lock released")
        } else {
            RemoteLogger.log(this, TAG, "Wake lock was not held or not initialized")
        }

        // Stop the foreground service
        try {
            stopForeground(true)
            RemoteLogger.log(this, TAG, "Foreground service stopped")
            stopSelf()
            RemoteLogger.log(this, TAG, "Service stopped successfully")
        } catch (e: Exception) {
            RemoteLogger.log(this, TAG, "ERROR stopping service: ${e.message}")
        }

        RemoteLogger.log(this, TAG, "=== STOP ALARM COMPLETE ===")
    }

    private fun createNotification(title: String, message: String): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Device-specific notification configuration
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        val priority = if (isPixelDevice) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .addAction(android.R.drawable.ic_dialog_alert, "Stop", stopPendingIntent)
            .setDeleteIntent(stopPendingIntent)

        // Apply Pixel-specific enhancements only for Pixel devices
        if (isPixelDevice) {
            notificationBuilder
                .setOngoing(true) // Prevents dismissal on Pixel devices
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 1000, 500))
                .setLights(0xFFFF0000.toInt(), 1000, 1000)
                .setAutoCancel(false)
        } else {
            // Standard behavior for other devices
            notificationBuilder
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
        }

        return notificationBuilder.build()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)

        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(notificationChannelId, notificationChannelName, importance)

        // Basic channel configuration for all devices
        notificationChannel.description = "Critical alarm notifications that require immediate attention"
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationChannel.setShowBadge(true)

        // Apply Pixel-specific aggressive settings only for Pixel devices
        if (isPixelDevice) {
            notificationChannel.lightColor = android.graphics.Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 500, 1000, 500)
            notificationChannel.setBypassDnd(true) // Only for Pixel devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notificationChannel.setAllowBubbles(true)
            }
        } else {
            // Standard settings for other devices
            notificationChannel.lightColor = android.graphics.Color.MAGENTA
            // Use default vibration pattern for non-Pixel devices
        }

        notificationManager.createNotificationChannel(notificationChannel)
        Log.d(TAG, "Notification channel created for device: ${Build.MANUFACTURER} ${Build.MODEL}, Pixel-optimized: $isPixelDevice")
    }
}

