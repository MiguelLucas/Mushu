// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

}


val appVersion by extra { "0.2.1-rc.3" }
val roomVersion by extra { "2.6.1" }
