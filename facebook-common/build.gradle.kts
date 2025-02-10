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
    api(Libs.androidx_legacy_support_v4)
    implementation(Libs.androidx_appcompat)
    implementation(Libs.androidx_cardview)
    implementation(Libs.androidx_browser)
    implementation(Libs.androidx_activity)
    implementation(Libs.androidx_fragment)

    // Third-party Dependencies
    implementation(Libs.zxing)

    implementation(Libs.kotlin_stdlib)

    // Unit Tests
    testImplementation(project(":facebook-testutil"))
    testImplementation(Libs.junit)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.androidx_test_core)
    testImplementation(Libs.mockito_inline)
    testImplementation(Libs.mockito_kotlin)

    testImplementation(Libs.powermock_core)
    testImplementation(Libs.powermock_api_mockito2)
    testImplementation(Libs.powermock_junit4)
    testImplementation(Libs.powermock_junit4_rule)
    testImplementation(Libs.powermock_classloading_xstream)
    testImplementation(Libs.assertj_core)

    testImplementation(Libs.android_installreferrer)
    testImplementation(Libs.kotlin_test_junit)
}

android {
    buildToolsVersion = "35.0.0"
    namespace = "com.facebook.common"
    compileSdk = Config.compileSdk


    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        consumerProguardFiles("proguard-rules.pro")
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    aaptOptions { additionalParameters("--no-version-vectors") }

    lintOptions { isAbortOnError = false }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize = "4096m"
        }
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets { named("test") { java.srcDir("src/test/kotlin") } }

    buildTypes {
        getByName("debug") { isTestCoverageEnabled = true }
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



