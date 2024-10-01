package com.mlucas.mushu

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
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


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "[Mushu][MainActivity]"
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        firebaseAnalytics = Firebase.analytics

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        askNotificationPermission()
        configureNotifications()
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
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MLUCAS", "Notification permission already granted")
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("notificationPermission", true)
                })
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("notificationPermissionSpecial", true)
                })
            } else {
                // Directly ask for the permission
                firebaseAnalytics.logEvent("Notification", Bundle().apply {
                    putBoolean("askingNotificationPermission", true)
                })
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    private fun configureNotifications() {
        // Subscribe to topic "allUsers"
        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
            .addOnCompleteListener { task: Task<Void?> ->
                var msg = "Subscribed to allUsers"
                if (!task.isSuccessful) {
                    msg = "Subscription to allUsers failed"
                }
                Log.d(TAG, msg)
                firebaseAnalytics.logEvent("topicSubscription", Bundle().apply {
                    putBoolean("allUsersSubscription", task.isSuccessful)
                    putString("allUsersSubscriptionMsg", msg)
                })
            }

        if (BuildConfig.SUBSCRIBE_DEBUG_NOTIFICATIONS) {
            FirebaseMessaging.getInstance().subscribeToTopic("debug")
                .addOnCompleteListener { task: Task<Void?> ->
                    var msg = "Subscribed to debug"
                    if (!task.isSuccessful) {
                        msg = "Subscription to debug failed"
                    }
                    Log.d(TAG, msg)
                    firebaseAnalytics.logEvent("topicSubscription", Bundle().apply {
                        putBoolean("debugSubscription", task.isSuccessful)
                        putString("debugSubscriptionMsg", msg)
                    })
                }
        }
    }
}