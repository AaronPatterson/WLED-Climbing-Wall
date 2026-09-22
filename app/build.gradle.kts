import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The release keystore never goes in the repo. On Android the signing
// certificate *is* the app's identity: updates are only accepted if they're
// signed by the same key, so anyone holding it can ship a build that devices
// trust as genuine - and losing it means never being able to update the
// installs already out there. keystore.properties lives only on the machine
// that cuts releases; see keystore.properties.example for its shape.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.wledclimb.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wledclimb.app"
        minSdk = 26
        targetSdk = 35
        // Bump both when cutting a release. versionCode is what decides
        // "is there a newer build?" - Android rejects an update whose code is
        // lower than what's installed, and Obtainium/Play won't offer one that
        // isn't higher. Day-to-day `adb install -r` tolerates an unchanged
        // code, so this only has to move when a build actually goes out.
        // Forgetting shows up as an update that silently doesn't apply, which
        // is why versionName is on screen in the app.
        versionCode = 2
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // v3 is what makes key rotation possible at all (API 28+): a later
            // build can carry a lineage proving a new key was authorised by
            // this one. It is the only escape hatch if this key is ever
            // compromised - it does nothing for a key that is simply lost,
            // since rotating requires the old key to sign off on the new.
            // v1 stays off: minSdk is 26 and v2 already covers API 24+.
            enableV3Signing = true

            // Left unconfigured on a machine without the keystore, so a fresh
            // clone still builds and runs tests.
            keystoreProperties.getProperty("storeFile")?.let { path ->
                storeFile = rootProject.file(path)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only wired up when the keystore is actually present. Without
            // this guard a machine lacking it fails the release build with a
            // null keystore path rather than an obvious "no keystore here".
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Off by default since AGP 8. Needed for BuildConfig.VERSION_NAME,
        // which the app shows so you can tell which build is on which device.
        buildConfig = true
    }

    testOptions {
        // android.util.Log is a stub in local unit tests and throws "not mocked"
        // by default - which would fail any test covering a code path that logs.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Simple HTTP client for talking to the WLED JSON API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Persists the saved WLED controller address across app restarts
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // org.json is part of the Android SDK, but local unit tests run against a
    // stub android.jar whose org.json methods throw "not mocked". This pulls
    // in a real implementation so tests that parse JSON actually work.
    testImplementation("org.json:json:20231013")
    // Fake HTTP server so WledClient's request/response handling (headers,
    // status codes, timeouts) can be tested without a real WLED controller.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // Lets tests substitute Dispatchers.Main, without which anything using
    // viewModelScope fails to run at all in a local JVM test. Version tracks
    // the kotlinx-coroutines-core that Compose/lifecycle resolve to.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
