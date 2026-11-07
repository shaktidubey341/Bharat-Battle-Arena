plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bharatbattlearena.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bharatbattlearena.game"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-stage1"
    }

    // Signing is only configured when the environment provides a keystore.
    // No keys are stored in this repository.
    signingConfigs {
        val ksPath = System.getenv("KEYSTORE_PATH")
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Intentionally empty: the game uses only the Android SDK and the Kotlin stdlib.
}
