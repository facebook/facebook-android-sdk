/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import android.net.Uri;
import android.os.Bundle;

final class Utility {
    private static final String URL_SCHEME = "http";
    private static final long INVALID_BUNDLE_MILLISECONDS = Long.MIN_VALUE;

    static Date getBundleStringSecondsFromNow(Bundle bundle, String key) {
        if (bundle == null) {
            return null;
        }

        String numberString = bundle.getString(key);
        if (numberString == null) {
            return null;
        }

        long number;
        try {
            number = Long.parseLong(numberString);
        } catch (NumberFormatException e) {
            return null;
        }

        Date now = new Date();
        return new Date(now.getTime() + (number * 1000L));
    }
    
    static Date getBundleDate(Bundle bundle, String key) {
        if (bundle == null) {
            return null;
        }

        long n = bundle.getLong(key, INVALID_BUNDLE_MILLISECONDS);
        if (n == INVALID_BUNDLE_MILLISECONDS) {
            return null;
        }

        return new Date(n);
    }

    static void putBundleDate(Bundle bundle, String key, Date date) {
        bundle.putLong(key, date.getTime());
    }

    // Returns true iff all items in subset are in superset, treating null and empty collections as
    // the same.
    static <T> boolean isSubset(Collection<T> subset, Collection<T> superset) {
        if ((superset == null) || (superset.size() == 0)) {
            return ((subset == null) || (subset.size() == 0));
        }

        HashSet<T> hash = new HashSet<T>(superset);
        for (T t : subset) {
            if (!hash.contains(t)) {
                return false;
            }
        }
        return true;
    }

    static <T> boolean isNullOrEmpty(Collection<T> c) {
        return (c == null) || (c.size() == 0);
    }
    
    static boolean isNullOrEmpty(String s) {
    	return (s == null) || (s.length() == 0);
    }
    
    static <T> Collection<T> unmodifiableCollection(T...ts) {
    	ArrayList<T> list = new ArrayList<T>();
    	for (T t : ts) {
    		list.add(t);
    	}
    	return Collections.unmodifiableCollection(list);
    }

    static Uri buildUri(String authority, String path, Bundle parameters) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(URL_SCHEME);
        builder.authority(authority);
        builder.path(path);
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (parameter instanceof String) {
                builder.appendQueryParameter(key, (String)parameter);
            }
        }
        return builder.build();
    }
}
