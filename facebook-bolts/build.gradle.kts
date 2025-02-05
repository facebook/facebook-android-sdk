/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    id("com.android.library")
    id("kotlin-android")
}

group = "com.facebook.android"

extra["name"] = "Facebook-Bolts-Android-SDK"

extra["artifactId"] = "facebook-bolts"

extra["description"] = "Facebook Android SDK Bolts support library"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
    implementation(Libs.androidx_annotation)
    implementation(Libs.androidx_core_ktx)
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
    testImplementation(Libs.kotlin_stdlib)
    testImplementation(Libs.kotlin_test_junit)
}

android {
    buildToolsVersion = "35.0.0"
    compileSdkVersion(Config.compileSdk)


    defaultConfig {
        minSdkVersion(Config.minSdk)
        targetSdkVersion(Config.targetSdk)
        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = true
        }
    }

    lintOptions { isAbortOnError = false }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    testOptions {
        unitTests.all {

            it.maxHeapSize = "4096m"
        }
    }

    sourceSets { named("test") { java.srcDir("src/test/kotlin") } }

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

apply(from = "${rootDir}/jacoco.gradle.kts")



repositories { mavenCentral() }
