val appVersion: String by rootProject.extra
val roomVersion: String by rootProject.extra


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mlucas.mushu"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.mlucas.mushu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            buildConfigField("Boolean", "SUBSCRIBE_DEBUG_NOTIFICATIONS", "true")
        }
        release {
            buildConfigField("Boolean", "SUBSCRIBE_DEBUG_NOTIFICATIONS", "false")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Import Firebase Cloud Messaging library
    implementation(libs.firebase.messaging.ktx)

    // For an optimal experience using FCM, add the Firebase SDK
    // for Google Analytics. This is recommended, but not required.
    implementation(libs.firebase.analytics)

    // Used to store FCM Registration Token.
    // This is recommended, but not required.
    // See: https://firebase.google.com/docs/cloud-messaging/manage-tokens
    implementation(libs.firebase.firestore)

    // Import the Room library
    implementation(libs.androidx.room.runtime)
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android) // Add Coroutines

    // Import third-party RecyclerView Animators
    implementation(libs.recyclerview.animators)


}