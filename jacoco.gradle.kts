/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

apply(plugin = "jacoco")

configure<JacocoPluginExtension> {
    toolVersion = "0.8.7"
}

tasks.register<JacocoReport>("debugCoverage") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports for the debug build."
    dependsOn("testDebugUnitTest")

    val excludes =
            listOf(
                    "**/R.class",
                    "**/R\$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*Test*.*",
                    "android/**/*.*",
                    "androidx/**/*.*",
                    "**/*\$ViewInjector*.*",
                    "**/*Dagger*.*",
                    "**/*MembersInjector*.*",
                    "**/*_Factory.*",
                    "**/*_Provide*Factory*.*",
                    "**/*_ViewBinding*.*",
                    "**/AutoValue_*.*",
                    "**/R2.class",
                    "**/R2\$*.class",
                    "**/*Directions\$*",
                    "**/*Directions.*",
                    "**/*Binding.*")

    val jClasses = "${project.buildDir}/intermediates/javac/debug/classes/com/facebook/core"
    val kClasses = "${project.buildDir}/tmp/kotlin-classes/debug/com/facebook"

    val javaClasses = fileTree(mapOf("dir" to jClasses, "excludes" to excludes))
    val kotlinClasses = fileTree(mapOf("dir" to kClasses, "excludes" to excludes))

    classDirectories.from(files(listOf(javaClasses, kotlinClasses)))
    val sourceDirs = listOf("${project.projectDir}/src/main/java")
    sourceDirectories.from(files(sourceDirs))

    executionData.from(files(listOf("${project.buildDir}/jacoco/testDebugUnitTest.exec")))
}
