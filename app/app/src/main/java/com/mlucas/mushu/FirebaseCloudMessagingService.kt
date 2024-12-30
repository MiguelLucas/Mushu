package com.mlucas.mushu

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FirebaseCloudMessagingService : FirebaseMessagingService() {
    private val TAG: String = "[Mushu][FirebaseCloudMessagingService]"
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a notification payload.
        firebaseAnalytics.logEvent("Notification", Bundle().apply {
            putBoolean("receivedNotification", true)
            putString("receivedNotificationMsg", remoteMessage.data["body"])
        })

        val notificationType = NotificationType.fromString(remoteMessage.data["type"])
        val notification = NotificationEntity(title = remoteMessage.data["title"]!!, message = remoteMessage.data["body"]!!, timestamp = System.currentTimeMillis(), type = notificationType)
        this.addNotificationToDatabase(notification)
        Log.d(TAG, "Message Notification Body: " + remoteMessage.data["body"]!!)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            // Check if data needs to be processed by long running job
            // For long-running tasks (10 seconds or more) use WorkManager (see scheduleJob())
            // For short lived tasks, execute them immediately
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val alarmEnabled = sharedPreferences.getBoolean(SettingsActivity.ALARM_ENABLED, true)

            if (notificationType == NotificationType.ALARM && alarmEnabled) {
                sendAlarm(notification)
            } else {
                sendNotification(notification)
            }
        } else {
            //TODO: Throw error here
            //sendNotification(notification)
        }
    }

    private fun addNotificationToDatabase(notification: NotificationEntity) {
        // Using a coroutine to insert into the database
        CoroutineScope(Dispatchers.IO).launch {
            val database = NotificationDatabase.getDatabase(applicationContext)
            val notificationDao = database.notificationDao()
            notificationDao.insert(notification)
            Log.d(TAG, "Inserting notification " + notification.title)
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
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", notification.title)
            putExtra("message", notification.message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 500 // Trigger in 0.5 seconds
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            Toast.makeText(this.applicationContext, "No permission to trigger alarm", Toast.LENGTH_LONG).show()
        }
    }

    class MyWorker(context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            // TODO(developer): add long running task here.
            return Result.success()
        }
    }
}