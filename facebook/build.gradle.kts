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

    implementation(libs.kotlin.stdlib)

    // Unit Tests
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

    // Connected Tests
    androidTestImplementation(libs.dexmaker)
    androidTestImplementation(libs.dexmaker.mockito)

    testImplementation(libs.android.installreferrer)
    testImplementation(libs.kotlin.test.junit)
}

android {
    buildToolsVersion = libs.versions.buildToolsVersion.get()
    namespace = "com.facebook"
    compileSdk = libs.versions.compileSdk.get().toInt()


    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        consumerProguardFiles("proguard-project.txt")
        vectorDrawables.useSupportLibrary = true
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

            it.maxHeapSize = "1024m"
        }
    }

    sourceSets {
        getByName("test")
        { java.srcDir("src/test/kotlin") }
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

repositories { maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } }

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

