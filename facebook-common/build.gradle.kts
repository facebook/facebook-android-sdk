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

extra["name"] = "Facebook-Common-Android-SDK"

extra["artifactId"] = "facebook-common"

extra["description"] = "Facebook Common Android SDK"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
    // Facebook Dependencies
    api(project(":facebook-core"))
    // Support Dependencies
    api(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)

    // Third-party Dependencies
    implementation(libs.zxing)

    implementation(libs.kotlin.stdlib)

    // Unit Tests
    testImplementation(project(":facebook-testutil"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)

    testImplementation(libs.powermock.core)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(libs.powermock.junit4)
    testImplementation(libs.powermock.junit4.rule)
    testImplementation(libs.powermock.classloading.xstream)
    testImplementation(libs.assertj.core)

    testImplementation(libs.android.installreferrer)
    testImplementation(libs.kotlin.test.junit)
}

android {
    buildToolsVersion = libs.versions.buildToolsVersion.get()
    namespace = "com.facebook.common"
    compileSdk = libs.versions.compileSdk.get().toInt()


    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        consumerProguardFiles("proguard-rules.pro")
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    androidResources { additionalParameters("--no-version-vectors") }

    lint { abortOnError = false }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize = "4096m"
        }
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("test")
        { java.srcDir("src/test/kotlin") }
    }

    buildTypes {
        getByName("debug") { enableUnitTestCoverage = true }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    if (System.getenv("SANDCASTLE") == "1") {
        testOptions {
            unitTests.all {
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://maven.thefacebook.com/nexus/content/repositories/central/"
                )
                it.systemProperty("robolectric.dependency.repo.id", "central")
                it.systemProperty("java.net.preferIPv6Addresses", "true")
                it.systemProperty("java.net.preferIPv4Stack", "false")
            }
        }
    }
}

if (file("${rootDir}/internal/safekit-build.gradle").exists()) {
    project.apply(from = "${rootDir}/internal/safekit-build.gradle")
}

apply(from = "${rootDir}/jacoco.gradle.kts")

apply(from = "${rootDir}/maven.gradle")
