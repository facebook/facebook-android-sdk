package com.facebook.internal.qualityvalidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** @see ExcusesForDesignViolations */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Excuse {

  String type();

  String reason();
}
