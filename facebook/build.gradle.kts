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
    id("org.jetbrains.dokka")
}

group = "com.facebook.android"

extra["name"] = "Facebook-Android-SDK"
extra["artifactId"] = "facebook-android-sdk"
extra["description"] = "Facebook Android SDK"
extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
  // Facebook Dependencies
    api(project(":facebook-core"))
    api(project(":facebook-common"))
    api(project(":facebook-login"))
    api(project(":facebook-share"))
    api(project(":facebook-applinks"))
    api(project(":facebook-messenger"))
    api(project(":facebook-gamingservices"))
    testImplementation(project(":facebook-testutil"))

    implementation(Libs.kotlin_stdlib)

    // Unit Tests
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

    // Connected Tests
    androidTestImplementation(Libs.dexmaker)
    androidTestImplementation(Libs.dexmaker_mockito)

    testImplementation(Libs.android_installreferrer)
    testImplementation(Libs.kotlin_test_junit)
}

android {
    compileSdkVersion(Config.compileSdk)

    defaultConfig {
        minSdkVersion(Config.minSdk)
        targetSdkVersion(Config.targetSdk)
        consumerProguardFiles("proguard-project.txt")
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

    testOptions {
        unitTests.all {
            it.jvmArgs("-XX:MaxPermSize=1024m")
            it.maxHeapSize = "1024m"
        }
    }

    if (System.getenv("SANDCASTLE") == "1") {
        testOptions {
            unitTests.all {
                it.systemProperty("robolectric.dependency.repo.url", "https://maven.thefacebook.com/nexus/content/repositories/central/")
                it.systemProperty("robolectric.dependency.repo.id", "central")
                it.systemProperty("java.net.preferIPv6Addresses", "true")
                it.systemProperty("java.net.preferIPv4Stack", "false")
            }
        }
    }
}

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

tasks.dokkaJavadoc.configure {
    dokkaSourceSets {
        named("main").configure {
            sourceRoots.from(file("../facebook-bolts/src/main"))
            sourceRoots.from(file("../facebook-core/src/main"))
            sourceRoots.from(file("../facebook-common/src/main"))
            sourceRoots.from(file("../facebook-login/src/main"))
            sourceRoots.from(file("../facebook-share/src/main"))
            sourceRoots.from(file("../facebook-applinks/src/main"))
            sourceRoots.from(file("../facebook-messenger/src/main"))
            sourceRoots.from(file("../facebook-gameservices/src/main"))
            includes.from("../facebook-core/src/main/java/com/facebook/internal/package-info.md")
        }
    }
}

apply(from = "${rootDir}/maven.gradle")
