/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.qualityvalidation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Annotations to mark code files as having known design issues and avoid failing the design
 * integration test on them
 *
 * <p>Please avoid using it for everything that can be fixed trivially, and delaying fixes to future
 * diffs without a good reason.
 *
 * <p>To use it list one of the type of issue you want to ignore, and supply and extra string
 * explaining why this is okay for this case.
 *
 * <pre>{@code
 * @ExcusesForDesignViolations(
 * @Excuse(type = "STORY_VIEWER_CAPITALIZATION", reason = "Legacy"),
 * @Excuse(type = "MISSING_JAVA_DOC", reason = "DI Module") ) }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class ExcusesForDesignViolations(vararg val value: Excuse)
