// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.4.1" apply false
    // No org.jetbrains.kotlin.android here: AGP 9 compiles Kotlin itself, and
    // applying the standalone plugin alongside it is an error rather than a
    // duplicate. jvmTarget now follows compileOptions.targetCompatibility.
    //
    // The Compose compiler is still its own plugin. Its version tracks the
    // Kotlin version AGP resolves, so the two move together.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
