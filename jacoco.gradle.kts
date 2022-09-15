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
