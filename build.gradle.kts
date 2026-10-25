// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9's built-in Kotlin support defaults to an older KGP version whose
        // metadata reader can't parse firebase-auth 25.0.0 (compiled with Kotlin 2.4.0).
        // Pinning a newer KGP here raises that internal compiler version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}