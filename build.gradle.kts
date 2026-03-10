/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    mavenCentral()
    google()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:7.4.0")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.30")
    classpath("org.jacoco:org.jacoco.core:0.8.7")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    maven { url = uri("https://jitpack.io") }
  }
}
