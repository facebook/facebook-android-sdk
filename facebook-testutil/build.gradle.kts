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

extra["name"] = "Facebook-Android-SDK-testutil"

extra["artifactId"] = "facebook-testutil"

extra["description"] = "Facebook Android SDK Testutil"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.androidx_annotation)
    implementation(Libs.androidx_core_ktx)
    implementation(Libs.androidx_legacy_support_core_utils)

    // Unit Tests
    implementation(Libs.junit)
    implementation(Libs.robolectric)
    implementation(Libs.androidx_test_core)

    implementation(Libs.mockito_inline)
    implementation(Libs.mockito_kotlin)

    implementation(Libs.powermock_core)
    implementation(Libs.powermock_api_mockito2)
    implementation(Libs.powermock_junit4)
    implementation(Libs.powermock_junit4_rule)
    implementation(Libs.powermock_classloading_xstream)
    implementation(Libs.assertj_core)

    implementation(Libs.android_installreferrer)
    implementation(Libs.kotlin_test_junit)
}

android {
    buildToolsVersion = "35.0.0"
    namespace = "com.facebook"
    compileSdkVersion(Config.compileSdk)

    defaultConfig {
        minSdkVersion(Config.minSdk)
        targetSdkVersion(Config.targetSdk)
        vectorDrawables.useSupportLibrary = true
    }

    aaptOptions { additionalParameters("--no-version-vectors") }

    lintOptions { isAbortOnError = false }

    sourceSets { named("test") { java.srcDir("src/test/kotlin") } }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
}

repositories { maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } }

apply(from = "${rootDir}/maven.gradle")

