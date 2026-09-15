plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val releaseKeystore = rootProject.file(System.getenv("AETHER_KEYSTORE_PATH") ?: "aether-release.keystore")
val keystorePassword = providers.environmentVariable("AETHER_KEYSTORE_PASSWORD").orNull
val keyAliasValue = providers.environmentVariable("AETHER_KEY_ALIAS").orNull
val keyPasswordValue = providers.environmentVariable("AETHER_KEY_PASSWORD").orNull
val releaseSigningReady = releaseKeystore.exists() && !keystorePassword.isNullOrBlank() && !keyAliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()

android {
    namespace = "com.aether.launcher"
    compileSdk = 35
    defaultConfig {
        // Keep the existing package id stable so updates install over the current Aether build.
        applicationId = "com.aetherlaucher.glassline"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.4.0"
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
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") {
            isMinifyEnabled = false
            check(releaseSigningReady) { "Aether release signing is not configured. Provide AETHER_KEYSTORE_BASE64 plus AETHER_KEYSTORE_PASSWORD, AETHER_KEY_ALIAS and AETHER_KEY_PASSWORD to the workflow." }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
}
