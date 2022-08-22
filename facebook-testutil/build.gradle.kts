/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    compileSdkVersion(Config.compileSdk)

    defaultConfig {
        minSdkVersion(Config.minSdk)
        targetSdkVersion(Config.targetSdk)
        vectorDrawables.useSupportLibrary = true
    }

    aaptOptions {
        additionalParameters("--no-version-vectors")
    }

    lintOptions {
        isAbortOnError = false
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
}

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

apply(from = "${rootDir}/maven.gradle")
