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

extra["name"] = "Facebook-Applinks-Android-SDK"

extra["artifactId"] = "facebook-applinks"

extra["description"] = "Facebook Applinks Android SDK"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
    // Facebook Dependencies
    api(project(":facebook-core"))
    // Support Dependencies
    implementation(Libs.androidx_annotation)
    implementation(Libs.androidx_legacy_support_core_utils)
}

android {
    buildToolsVersion = Config.buildToolsVersion
    namespace = "com.facebook.applinks"
    compileSdk = Config.compileSdk


    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        consumerProguardFiles("proguard-rules.pro")
    }

    sourceSets {
        getByName("test")
        { java.srcDir("src/test/kotlin") }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

if (file("${rootDir}/internal/safekit-build.gradle").exists()) {
    project.apply(from = "${rootDir}/internal/safekit-build.gradle")
}

apply(from = "${rootDir}/maven.gradle")


