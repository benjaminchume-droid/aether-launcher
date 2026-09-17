plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystore = rootProject.file(System.getenv("AETHER_KEYSTORE_PATH") ?: "aether-release.keystore")
val keystorePassword = providers.environmentVariable("AETHER_KEYSTORE_PASSWORD").orNull
val keyAliasValue = providers.environmentVariable("AETHER_KEY_ALIAS").orNull
val keyPasswordValue = providers.environmentVariable("AETHER_KEY_PASSWORD").orNull
val releaseSigningReady = releaseKeystore.exists() &&
    !keystorePassword.isNullOrBlank() &&
    !keyAliasValue.isNullOrBlank() &&
    !keyPasswordValue.isNullOrBlank()

android {
    namespace = "com.aether.launcher"
    compileSdk = 35

    defaultConfig {
        // IMPORTANT: keep this exact application id. Changing it would create a different app.
        applicationId = "com.aetherlaucher.glassline"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.6.0"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = releaseKeystore
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Allow local debug-signed release builds when secrets are absent
                // CI still requires the stable keystore via the workflow check
                signingConfig = null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Ensure vector adaptive icons resolve on older tools
    sourceSets.getByName("main").res.srcDirs("src/main/res")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}
