// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Required in Kotlin 2.0+ whenever Compose is enabled - handles the Compose compiler
    // that used to be configured via composeOptions.kotlinCompilerExtensionVersion.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
