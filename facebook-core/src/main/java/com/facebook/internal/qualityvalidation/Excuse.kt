package com.facebook.internal.qualityvalidation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** @see ExcusesForDesignViolations */
@Retention(RetentionPolicy.SOURCE)
@Target
annotation class Excuse(val type: String, val reason: String)
