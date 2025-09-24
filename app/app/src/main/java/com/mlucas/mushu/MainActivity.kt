package com.mlucas.mushu

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.mlucas.mushu.databinding.ActivityMainBinding
import com.mlucas.mushu.utils.BatteryOptimizationHelper
import com.mlucas.mushu.utils.RemoteLogger


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "[Mushu][MainActivity]"
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enhanced logging for app startup
        RemoteLogger.log(this, TAG, "MainActivity.onCreate() started")
        RemoteLogger.log(this, TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        RemoteLogger.log(this, TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        firebaseAnalytics = Firebase.analytics

        RemoteLogger.log(this, TAG, "Firebase Analytics initialized")

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, /*R.id.navigation_dashboard,*/ R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        RemoteLogger.log(this, TAG, "Navigation setup completed")

        askAlarmPermission()
        askNotificationPermission()
        configureNotifications()
        checkPixelSpecificOptimizations()

        if (BuildConfig.DEBUG) {
            RemoteLogger.log(this, TAG, "Debug mode: Adding debug button")
            val debugButton = Button(this)
            debugButton.text = "Debug Logs"
            debugButton.setOnClickListener {
                RemoteLogger.log(this, TAG, "Debug button clicked - launching DebugActivity")
                startActivity(Intent(this, DebugActivity::class.java))
            }
            // Add debugButton to your layout
            val layout = findViewById<LinearLayout>(R.id.debug_button_container)
            layout?.addView(debugButton) ?: RemoteLogger.log(this, TAG, "WARNING: debug_button_container not found")
        }

        RemoteLogger.log(this, TAG, "MainActivity.onCreate() completed successfully")
    }

    private fun askAlarmPermission() {
        RemoteLogger.log(this, TAG, "askAlarmPermission() called, Android API: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExact = alarmManager.canScheduleExactAlarms()
            RemoteLogger.log(this, TAG, "Can schedule exact alarms: $canScheduleExact")

            if (!canScheduleExact) {
                RemoteLogger.log(this, TAG, "Exact alarm permission missing - showing dialog")
                showPermissionExplanationDialog()
            } else {
                RemoteLogger.log(this, TAG, "Exact alarm permission already granted")
            }
        } else {
            RemoteLogger.log(this, TAG, "Android < 12 - exact alarm permission not required")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showPermissionExplanationDialog() {
        firebaseAnalytics.logEvent("AlarmPermission", Bundle().apply {
            putBoolean("alarmPermissionAsked", true)
        })
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.dialog_alarm_permission_title)
            .setMessage(R.string.dialog_alarm_permission_body)
            .setPositiveButton(R.string.dialog_alarm_permission_positive_btn) { dialog, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                dialog.dismiss()
                firebaseAnalytics.logEvent("AlarmPermission", Bundle().apply {
                    putBoolean("alarmPermissionGranted", true)
                })
            }
            .setNegativeButton(R.string.dialog_alarm_permission_negative_btn) { dialog, _ ->
                dialog.dismiss()
                firebaseAnalytics.logEvent("AlarmPermission", Bundle().apply {
                    putBoolean("alarmPermissionGranted", false)
                })
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Handle the settings action
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            firebaseAnalytics.logEvent("Notification", Bundle().apply {
                putBoolean("notificationPermissionGranted", true)
            })
        } else {
            // TODO: Inform user that that your app will not show notifications.
            firebaseAnalytics.logEvent("Notification", Bundle().apply {
                putBoolean("notificationPermissionGranted", false)
            })
        }
    }

    private fun askNotificationPermission() {
        RemoteLogger.log(this, TAG, "askNotificationPermission() called, Android API: ${Build.VERSION.SDK_INT}")
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
            RemoteLogger.log(this, TAG, "Notification permission status: $permissionStatus")

            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                RemoteLogger.log(this, TAG, "Notification permission already granted")
                Log.d("MLUCAS", "Notification permission already granted")
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("notificationPermission", true)
                })
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                RemoteLogger.log(this, TAG, "Should show notification permission rationale")
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("notificationPermissionSpecial", true)
                })
            } else {
                RemoteLogger.log(this, TAG, "Requesting notification permission directly")
                // Directly ask for the permission
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("askingNotificationPermission", true)
                })
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        } else {
            RemoteLogger.log(this, TAG, "Android < 13 - notification permission not required")
        }
    }

    private fun configureNotifications() {
        RemoteLogger.log(this, TAG, "configureNotifications() started")
        // Subscribe to topic "allUsers"
        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
            .addOnCompleteListener { task: Task<Void?> ->
                var msg = "Subscribed to allUsers"
                if (!task.isSuccessful) {
                    msg = "Subscription to allUsers failed"
                }
                Log.d(TAG, msg)
                RemoteLogger.log(this, TAG, "FCM Topic subscription result: $msg")
                firebaseAnalytics.logEvent("topicSubscription", Bundle().apply {
                    putBoolean("allUsersSubscription", task.isSuccessful)
                    putString("allUsersSubscriptionMsg", msg)
                })
            }

        if (BuildConfig.SUBSCRIBE_DEBUG_NOTIFICATIONS) {
            RemoteLogger.log(this, TAG, "Debug notifications enabled - subscribing to debug topic")
            FirebaseMessaging.getInstance().subscribeToTopic("debug")
                .addOnCompleteListener { task: Task<Void?> ->
                    var msg = "Subscribed to debug"
                    if (!task.isSuccessful) {
                        msg = "Subscription to debug failed"
                    }
                    Log.d(TAG, msg)
                    RemoteLogger.log(this, TAG, "FCM Debug topic subscription result: $msg")
                    firebaseAnalytics.logEvent("topicSubscription", Bundle().apply {
                        putBoolean("debugSubscription", task.isSuccessful)
                        putString("debugSubscriptionMsg", msg)
                    })
                }
        } else {
            RemoteLogger.log(this, TAG, "Debug notifications disabled - skipping debug topic subscription")
        }
    }

    private fun checkPixelSpecificOptimizations() {
        RemoteLogger.log(this, TAG, "checkPixelSpecificOptimizations() started")
        val isPixel = BatteryOptimizationHelper.isProblematicPixelDevice()
        RemoteLogger.log(this, TAG, "Is problematic Pixel device: $isPixel")

        if (isPixel) {
            Log.d(TAG, "Detected Pixel device that may have alarm issues")
            RemoteLogger.log(this, TAG, "PIXEL DEVICE DETECTED: ${Build.MODEL} - initiating specialized alarm optimizations")
            RemoteLogger.log(this, TAG, "Pixel device details: Manufacturer=${Build.MANUFACTURER}, Model=${Build.MODEL}, API=${Build.VERSION.SDK_INT}")

            // Check battery optimization status with detailed logging
            val isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)
            RemoteLogger.log(this, TAG, "PIXEL: Battery optimization status - optimized: $isBatteryOptimized")

            if (BatteryOptimizationHelper.shouldRequestExemption(this)) {
                RemoteLogger.log(this, TAG, "PIXEL: Battery optimization exemption needed - showing dialog")
                showPixelBatteryOptimizationDialog()
            } else {
                RemoteLogger.log(this, TAG, "PIXEL: Battery optimization already exempted or not needed")
            }

            // Set up Pixel keepalive system for force-close scenarios
            setupPixelKeepaliveSystem()

            // Log comprehensive Pixel-specific status for debugging
            val pixelStatus = BatteryOptimizationHelper.getPixelSpecificStatus(this)
            Log.d(TAG, "Pixel status: $pixelStatus")
            RemoteLogger.log(this, TAG, "PIXEL: Detailed status report: $pixelStatus")

            // Track Pixel device usage for analytics with enhanced data
            firebaseAnalytics.logEvent("PixelDeviceDetected", Bundle().apply {
                putString("deviceModel", Build.MODEL)
                putString("androidVersion", Build.VERSION.RELEASE)
                putBoolean("batteryOptimized", isBatteryOptimized)
                putString("pixelSeries", when {
                    Build.MODEL.contains("Pixel 6") -> "Pixel6"
                    Build.MODEL.contains("Pixel 7") -> "Pixel7"
                    Build.MODEL.contains("Pixel 8") -> "Pixel8"
                    else -> "Other"
                })
            })
            RemoteLogger.log(this, TAG, "PIXEL: Analytics event logged for device tracking")
        } else {
            RemoteLogger.log(this, TAG, "Non-Pixel device detected: ${Build.MANUFACTURER} ${Build.MODEL} - using standard alarm behavior")
        }
    }

    private fun setupPixelKeepaliveSystem() {
        RemoteLogger.log(this, TAG, "PIXEL: Setting up keepalive system for force-close scenarios")
        try {
            val intent = Intent(this, PixelKeepaliveReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                12345, // Fixed request code for keepalive
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                // Cancel any existing keepalive alarms first
                alarmManager.cancel(pendingIntent)

                // Set a repeating alarm every 15 minutes to keep FCM active
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + (15 * 60 * 1000), // Start in 15 minutes
                    15 * 60 * 1000, // Repeat every 15 minutes
                    pendingIntent
                )
                RemoteLogger.log(this, TAG, "PIXEL: Keepalive system activated - 15-minute intervals")
            } else {
                RemoteLogger.log(this, TAG, "PIXEL: Cannot schedule keepalive - exact alarm permission missing")
            }
        } catch (e: Exception) {
            RemoteLogger.log(this, TAG, "PIXEL: ERROR setting up keepalive system: ${e.message}")
        }
    }

    private fun showPixelBatteryOptimizationDialog() {
        RemoteLogger.log(this, TAG, "PIXEL: Showing battery optimization dialog to user")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pixel Device Optimization")
            .setMessage("Your Pixel device may have aggressive battery optimization that can prevent critical alarms from working properly. Would you like to disable battery optimization for this app?")
            .setPositiveButton("Optimize for Alarms") { dialog, _ ->
                RemoteLogger.log(this, TAG, "PIXEL: User accepted battery optimization - attempting to open settings")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = BatteryOptimizationHelper.requestBatteryOptimizationExemption(this)
                        if (intent != null) {
                            RemoteLogger.log(this, TAG, "PIXEL: Opening battery optimization settings")
                            startActivity(intent)
                        } else {
                            RemoteLogger.log(this, TAG, "PIXEL: No battery optimization intent available")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open battery optimization settings", e)
                    RemoteLogger.log(this, TAG, "PIXEL: ERROR - Failed to open battery optimization settings: ${e.message}")
                    // Fallback: open general battery settings
                    try {
                        RemoteLogger.log(this, TAG, "PIXEL: Attempting fallback battery settings")
                        startActivity(BatteryOptimizationHelper.getPixelBatterySettingsIntent(this))
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to open any battery settings", e2)
                        RemoteLogger.log(this, TAG, "PIXEL: CRITICAL ERROR - All battery settings failed: ${e2.message}")
                    }
                }
                dialog.dismiss()

                firebaseAnalytics.logEvent("PixelBatteryOptimization", Bundle().apply {
                    putBoolean("userAccepted", true)
                    putString("deviceModel", Build.MODEL)
                })
                RemoteLogger.log(this, TAG, "PIXEL: Battery optimization dialog completed - user accepted")
            }
            .setNegativeButton("Maybe Later") { dialog, _ ->
                RemoteLogger.log(this, TAG, "PIXEL: User declined battery optimization")
                dialog.dismiss()
                firebaseAnalytics.logEvent("PixelBatteryOptimization", Bundle().apply {
                    putBoolean("userAccepted", false)
                    putString("deviceModel", Build.MODEL)
                })
            }
            .show()
    }
}