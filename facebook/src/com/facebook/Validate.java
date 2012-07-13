package com.facebook;

import java.util.Collection;

final class Validate {
    static void notNull(Object arg, String name) {
        if (arg == null) {
            throw new NullPointerException("Argument " + name + " cannot be null");
        }
    }

    static <T> void notEmpty(Collection<T> container, String name) {
        if (container.isEmpty()) {
            throw new IllegalArgumentException("Container '" + name + "' cannot be empty");
        }
    }

    static <T> void containsNoNulls(Collection<T> container, String name) {
        Validate.notNull(container, name);
        for (T item : container) {
            if (item == null) {
                throw new NullPointerException("Container '" + name + "' cannot contain null values");
            }
        }
    }

    static <T> void notEmptyAndContainsNoNulls(Collection<T> container, String name) {
        Validate.containsNoNulls(container, name);
        Validate.notEmpty(container, name);
    }

    static void notNullOrEmpty(String arg, String name) {
        if (Utility.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("Argument " + name + " cannot be null or empty");
        }
    }

    static void oneOf(Object arg, String name, Object... values) {
        for (Object value : values) {
            if (value != null) {
                if (value.equals(arg)) {
                    return;
                }
            } else {
                if (arg == null) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Argument " + name + " was not one of the allowed values");
    }
}
