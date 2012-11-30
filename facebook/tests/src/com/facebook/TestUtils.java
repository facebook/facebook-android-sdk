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

import android.os.Bundle;
import junit.framework.Assert;

import java.io.*;
import java.util.Date;
import java.util.List;

public class TestUtils {
    public static <T extends Serializable> T serializeAndUnserialize(T t) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            new ObjectOutputStream(os).writeObject(t);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

            @SuppressWarnings("unchecked")
            T ret = (T) (new ObjectInputStream(is)).readObject();

            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static Date nowPlusSeconds(long offset) {
        return new Date(new Date().getTime() + (offset * 1000L));
    }

    static void assertSamePermissions(List<String> expected, AccessToken actual) {
        if (expected == null) {
            Assert.assertEquals(null, actual.getPermissions());
        } else {
            for (String p : expected) {
                Assert.assertTrue(actual.getPermissions().contains(p));
            }
            for (String p : actual.getPermissions()) {
                Assert.assertTrue(expected.contains(p));
            }
        }
    }

    static void assertSamePermissions(List<String> expected, List<String> actual) {
        if (expected == null) {
            Assert.assertEquals(null, actual);
        } else {
            for (String p : expected) {
                Assert.assertTrue(actual.contains(p));
            }
            for (String p : actual) {
                Assert.assertTrue(expected.contains(p));
            }
        }
    }

    static void assertAtLeastExpectedPermissions(List<String> expected, List<String> actual) {
        if (expected != null) {
            for (String p : expected) {
                Assert.assertTrue(actual.contains(p));
            }
        }
    }

    static void assertEqualContents(Bundle a, Bundle b) {
        for (String key : a.keySet()) {
            if (!b.containsKey(key)) {
                Assert.fail("bundle does not include key " + key);
            }
            Assert.assertEquals(a.get(key), b.get(key));
        }
        for (String key : b.keySet()) {
            if (!a.containsKey(key)) {
                Assert.fail("bundle does not include key " + key);
            }
        }
    }
}
