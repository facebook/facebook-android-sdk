/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */


@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("kotlin-android")
}

group = "com.facebook.android"

extra["name"] = "Facebook-Android-SDK-testutil"

extra["artifactId"] = "facebook-testutil"

extra["description"] = "Facebook Android SDK Testutil"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.legacy.support.core.utils)

    // Unit Tests
    implementation(libs.junit)
    implementation(libs.robolectric)
    implementation(libs.androidx.test.core)

    implementation(libs.mockito.inline)
    implementation(libs.mockito.kotlin)

    implementation(libs.powermock.core)
    implementation(libs.powermock.api.mockito2)
    implementation(libs.powermock.junit4)
    implementation(libs.powermock.junit4.rule)
    implementation(libs.powermock.classloading.xstream)
    implementation(libs.assertj.core)

    implementation(libs.android.installreferrer)
    implementation(libs.kotlin.test.junit)
}

android {
    buildToolsVersion = libs.versions.buildToolsVersion.get()
    namespace = "com.facebook"
    compileSdk = libs.versions.compileSdk.get().toInt()


    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        vectorDrawables.useSupportLibrary = true
    }

    androidResources { additionalParameters("--no-version-vectors") }

    lint { abortOnError = false }

    sourceSets {
        getByName("test")
        { java.srcDir("src/test/kotlin") }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories { maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } }

apply(from = "${rootDir}/maven.gradle")

