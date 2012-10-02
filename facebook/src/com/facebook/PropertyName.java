package com.facebook;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use this annotation on getters and setters in an interface that derives from
 * GraphObject, if you wish to override the default property name that is inferred
 * from the name of the method.
 *
 * If this annotation is specified on a method, it must contain a non-empty String
 * value that represents the name of the property that the method is a getter or setter
 * for.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyName {
    String value();
}