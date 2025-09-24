package com.mlucas.mushu

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mlucas.mushu.data.database.NotificationDatabase
import com.mlucas.mushu.data.entities.NotificationEntity
import com.mlucas.mushu.data.entities.NotificationType
import com.mlucas.mushu.utils.RemoteLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FirebaseCloudMessagingService : FirebaseMessagingService() {
    private val TAG: String = "[Mushu][FirebaseCloudMessagingService]"
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        RemoteLogger.log(this, TAG, "=== FCM MESSAGE RECEIVED ===")
        RemoteLogger.log(this, TAG, "App state: ${getAppState()}")
        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: " + remoteMessage.from)
        RemoteLogger.log(this, TAG, "FCM Message received from: ${remoteMessage.from}")
        RemoteLogger.log(this, TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}, API: ${Build.VERSION.SDK_INT}")

        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL" else "STANDARD"} device processing FCM message")

        // Check if message contains a notification payload.
        firebaseAnalytics.logEvent("Notification", Bundle().apply {
            putBoolean("receivedNotification", true)
            putString("receivedNotificationMsg", remoteMessage.data["body"])
        })

        val notificationType = NotificationType.fromString(remoteMessage.data["type"])
        val notification = NotificationEntity(title = remoteMessage.data["title"]!!, message = remoteMessage.data["body"]!!, timestamp = System.currentTimeMillis(), type = notificationType)
        this.addNotificationToDatabase(notification)
        Log.d(TAG, "Message Notification Body: " + remoteMessage.data["body"]!!)
        Log.d(TAG, "Notification Type: $notificationType")
        RemoteLogger.log(this, TAG, "Notification type: $notificationType, title: ${notification.title}")

        if (isPixelDevice && notificationType == NotificationType.ALARM) {
            RemoteLogger.log(this, TAG, "PIXEL ALARM DETECTED: Special handling initiated for ${notification.title}")
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            RemoteLogger.log(this, TAG, "Processing data payload, size: ${remoteMessage.data.size}")

            // Check if data needs to be processed by long running job
            // For long-running tasks (10 seconds or more) use WorkManager (see scheduleJob())
            // For short lived tasks, execute them immediately
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val alarmEnabled = sharedPreferences.getBoolean(SettingsActivity.ALARM_ENABLED, true)

            Log.d(TAG, "Alarm enabled in settings: $alarmEnabled")
            RemoteLogger.log(this, TAG, "Alarm enabled: $alarmEnabled")

            when (notificationType) {
            NotificationType.ALARM -> {
                Log.d(TAG, "ALARM notification type detected")
                RemoteLogger.log(this, TAG, "ALARM type detected, alarmEnabled: $alarmEnabled")
                if (alarmEnabled) {
                    Log.d(TAG, "Calling sendAlarm()")
                    RemoteLogger.log(this, TAG, "Calling sendAlarm() for: ${notification.title}")
                    if (isPixelDevice) {
                        RemoteLogger.log(this, TAG, "PIXEL: Initiating specialized alarm handling")
                    }

                    // For force-close resilience, use both AlarmManager and immediate service start
                    sendAlarm(notification) // Use AlarmManager approach

                    // On Pixel devices, also start service immediately as backup
                    if (isPixelDevice) {
                        RemoteLogger.log(this, TAG, "PIXEL: Starting immediate backup service for force-close resilience")
                        try {
                            val immediateIntent = Intent(this, AlarmService::class.java).apply {
                                putExtra("title", notification.title + " (Direct)")
                                putExtra("message", notification.message)
                                action = "START_ALARM"
                            }
                            startForegroundService(immediateIntent)
                            RemoteLogger.log(this, TAG, "PIXEL: Immediate backup service started successfully")
                        } catch (e: Exception) {
                            RemoteLogger.log(this, TAG, "PIXEL: Failed to start immediate backup service: ${e.message}")
                        }
                    }
                } else {
                    Log.d(TAG, "Alarm disabled, sending normal notification")
                    RemoteLogger.log(this, TAG, "Alarm disabled, sending normal notification")
                    sendNotification(notification)
                }
            }
            else -> {
                Log.d(TAG, "Non-ALARM notification type: $notificationType")
                RemoteLogger.log(this, TAG, "Non-ALARM type: $notificationType")
                sendNotification(notification)
            }
        }
        } else {
            Log.w(TAG, "Empty data payload received")
            RemoteLogger.log(this, TAG, "WARNING: Empty data payload received")
            //TODO: Throw error here
            //sendNotification(notification)
        }

        RemoteLogger.log(this, TAG, "=== FCM MESSAGE PROCESSING COMPLETE ===")
    }

    private fun getAppState(): String {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            val myProcess = runningAppProcesses?.find { it.processName == packageName }
            when (myProcess?.importance) {
                android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
                android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND -> "BACKGROUND"
                android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED"
                null -> "NOT_RUNNING"
                else -> "UNKNOWN(${myProcess.importance})"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    private fun addNotificationToDatabase(notification: NotificationEntity) {
        RemoteLogger.log(this, TAG, "addNotificationToDatabase() called for: ${notification.title}")
        // Using a coroutine to insert into the database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = NotificationDatabase.getDatabase(applicationContext)
                val notificationDao = database.notificationDao()
                notificationDao.insert(notification)
                Log.d(TAG, "Inserting notification " + notification.title)
                RemoteLogger.log(this@FirebaseCloudMessagingService, TAG, "Database insert successful for: ${notification.title}")
            } catch (e: Exception) {
                RemoteLogger.log(this@FirebaseCloudMessagingService, TAG, "ERROR: Database insert failed: ${e.message}")
            }
        }
    }


    // [END receive_message]
    // [START on_new_token]
    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }


    // [END on_new_token]
    private fun scheduleJob() {
        // [START dispatch_job]
        val work: OneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .build()
        WorkManager.getInstance(this).beginWith(work).enqueue()
        // [END dispatch_job]
    }

    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
    }

    private fun sendNotification(notification: NotificationEntity) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,  /* Request code */intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = R.string.default_notification_channel_id.toString()
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            "General notification channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0,  /* ID of notification */notificationBuilder.build())
    }


    private fun sendAlarm(notification: NotificationEntity) {
        RemoteLogger.log(this, TAG, "=== ALARM DEBUG START ===")
        Log.d(TAG, "=== ALARM DEBUG START ===")
        Log.d(TAG, "sendAlarm called for: ${notification.title}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android version: ${Build.VERSION.RELEASE}")
        RemoteLogger.log(this, TAG, "sendAlarm called for device: ${Build.MANUFACTURER} ${Build.MODEL}")
        RemoteLogger.log(this, TAG, "Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        // Special handling for Google Pixel devices
        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        Log.d(TAG, "Is Pixel device: $isPixelDevice")
        RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL DEVICE DETECTED" else "Standard device"}: ${Build.MODEL}")

        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", notification.title)
            putExtra("message", notification.message)
            // Add device-specific flags for Pixel
            if (isPixelDevice) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                RemoteLogger.log(this@FirebaseCloudMessagingService, TAG, "PIXEL: Added enhanced intent flags for alarm receiver")
            }
        }

        RemoteLogger.log(this, TAG, "Created alarm intent with title: '${notification.title}', message: '${notification.message}'")

        // Use unique request code to avoid conflicts
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        RemoteLogger.log(this, TAG, "Generated unique request code: $requestCode")

        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        RemoteLogger.log(this, TAG, "Created PendingIntent for alarm broadcast")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Slightly longer delay for Pixel devices to ensure proper wake-up
        val triggerTime = System.currentTimeMillis() + if (isPixelDevice) 2000 else 1000
        RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL" else "STANDARD"}: Alarm trigger time set for ${if (isPixelDevice) 2 else 1} seconds from now")

        Log.d(TAG, "Can schedule exact alarms: ${alarmManager.canScheduleExactAlarms()}")
        RemoteLogger.log(this, TAG, "Can schedule exact alarms: ${alarmManager.canScheduleExactAlarms()}")

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && alarmManager.canScheduleExactAlarms() -> {
                    if (isPixelDevice) {
                        RemoteLogger.log(this, TAG, "PIXEL: Attempting AlarmClock API for enhanced reliability")
                        // For Pixel devices, try multiple scheduling approaches
                        try {
                            alarmManager.setAlarmClock(
                                AlarmManager.AlarmClockInfo(triggerTime, null),
                                pendingIntent
                            )
                            Log.d(TAG, "Scheduled alarm clock for Pixel device")
                            RemoteLogger.log(this, TAG, "PIXEL: AlarmClock scheduled successfully")
                        } catch (e: Exception) {
                            Log.w(TAG, "AlarmClock failed, falling back to setExactAndAllowWhileIdle", e)
                            RemoteLogger.log(this, TAG, "PIXEL: AlarmClock failed (${e.message}), falling back to setExactAndAllowWhileIdle")
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            RemoteLogger.log(this, TAG, "PIXEL: Fallback setExactAndAllowWhileIdle completed")
                        }
                    } else {
                        RemoteLogger.log(this, TAG, "STANDARD: Using setExactAndAllowWhileIdle for non-Pixel device")
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        RemoteLogger.log(this, TAG, "STANDARD: setExactAndAllowWhileIdle completed successfully")
                    }
                    Log.d(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle")
                    RemoteLogger.log(this, TAG, "Alarm scheduled successfully using exact alarm method")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    RemoteLogger.log(this, TAG, "Using legacy setExact for Android 4.4+ (API ${Build.VERSION.SDK_INT})")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "Scheduled exact alarm (legacy)")
                    RemoteLogger.log(this, TAG, "Legacy setExact alarm scheduled successfully")
                }
                else -> {
                    RemoteLogger.log(this, TAG, "Using basic alarm.set for older Android (API ${Build.VERSION.SDK_INT})")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "Scheduled regular alarm")
                    RemoteLogger.log(this, TAG, "Basic alarm.set completed successfully")
                }
            }

            // Track successful scheduling
            val method = when {
                isPixelDevice -> "AlarmClock"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> "setExactAndAllowWhileIdle"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> "setExact"
                else -> "set"
            }

            firebaseAnalytics.logEvent("AlarmScheduled", Bundle().apply {
                putString("device", "${Build.MANUFACTURER}_${Build.MODEL}")
                putString("method", method)
                putBoolean("canScheduleExact", alarmManager.canScheduleExactAlarms())
                putBoolean("isPixel", isPixelDevice)
            })
            RemoteLogger.log(this, TAG, "Analytics logged: AlarmScheduled with method '$method' for ${Build.MANUFACTURER}_${Build.MODEL}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm via AlarmManager: ${e.message}", e)
            RemoteLogger.log(this, TAG, "CRITICAL ERROR: AlarmManager scheduling failed: ${e.message}")
            RemoteLogger.log(this, TAG, "Exception details: ${e.javaClass.simpleName} - ${e.stackTrace.firstOrNull()}")
            startAlarmServiceDirectly(notification)
        }

        Log.d(TAG, "=== ALARM DEBUG END ===")
        RemoteLogger.log(this, TAG, "=== ALARM DEBUG END ===")
    }

    private fun startAlarmServiceDirectly(notification: NotificationEntity) {
        RemoteLogger.log(this, TAG, "=== DIRECT SERVICE START ===")
        RemoteLogger.log(this, TAG, "AlarmManager failed - attempting direct service fallback")
        Log.d(TAG, "=== DIRECT SERVICE START ===")

        val isPixelDevice = Build.MANUFACTURER.equals("Google", ignoreCase = true)
        RemoteLogger.log(this, TAG, "${if (isPixelDevice) "PIXEL" else "STANDARD"}: Starting AlarmService directly as fallback")

        try {
            val serviceIntent = Intent(this, AlarmService::class.java).apply {
                putExtra("title", notification.title)
                putExtra("message", notification.message)
                action = "START_ALARM"
            }

            RemoteLogger.log(this, TAG, "Created direct service intent for: '${notification.title}'")
            Log.d(TAG, "Starting AlarmService directly")

            startForegroundService(serviceIntent)
            RemoteLogger.log(this, TAG, "Direct foreground service start requested successfully")
            Log.d(TAG, "AlarmService start requested")
            
            firebaseAnalytics.logEvent("AlarmScheduled", Bundle().apply {
                putString("device", "${Build.MANUFACTURER}_${Build.MODEL}")
                putString("method", "DirectService")
                putBoolean("isPixel", isPixelDevice)
                putBoolean("isFallback", true)
            })
            RemoteLogger.log(this, TAG, "Analytics logged: Direct service fallback for ${Build.MANUFACTURER}_${Build.MODEL}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmService directly: ${e.message}", e)
            RemoteLogger.log(this, TAG, "CRITICAL FAILURE: Direct service start failed: ${e.message}")
            RemoteLogger.log(this, TAG, "Exception type: ${e.javaClass.simpleName}")

            firebaseAnalytics.logEvent("AlarmFailed", Bundle().apply {
                putString("error", e.message)
                putString("device", "${Build.MANUFACTURER}_${Build.MODEL}")
                putBoolean("isPixel", isPixelDevice)
                putString("failurePoint", "DirectService")
            })
            RemoteLogger.log(this, TAG, "Analytics logged: Complete alarm failure for ${Build.MANUFACTURER}_${Build.MODEL}")
        }

        RemoteLogger.log(this, TAG, "=== DIRECT SERVICE END ===")
        Log.d(TAG, "=== DIRECT SERVICE END ===")
    }

    class MyWorker(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            // TODO(developer): add long running task here.
            return Result.success()
        }
    }
}