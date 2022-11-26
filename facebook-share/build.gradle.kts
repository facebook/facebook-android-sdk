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

extra["name"] = "Facebook-Share-Android-SDK"

extra["artifactId"] = "facebook-share"

extra["description"] = "Facebook Share Android SDK"

extra["url"] = "https://github.com/facebook/facebook-android-sdk"

dependencies {
  // Facebook Dependencies
  api(project(":facebook-core"))
  api(project(":facebook-common"))
  implementation(Libs.androidx_appcompat)
}

android {
  compileSdkVersion(Config.compileSdk)

  defaultConfig {
    minSdkVersion(Config.minSdk)
    targetSdkVersion(Config.targetSdk)
    consumerProguardFiles("proguard-rules.pro")
    vectorDrawables.useSupportLibrary = true
  }

  aaptOptions { additionalParameters("--no-version-vectors") }

  lintOptions { isAbortOnError = false }

  compileOptions {
    sourceCompatibility(JavaVersion.VERSION_1_8)
    targetCompatibility(JavaVersion.VERSION_1_8)
  }

  sourceSets { named("test") { java.srcDir("src/test/kotlin") } }

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
