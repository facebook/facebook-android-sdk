/**
 * Copyright 2010-present Facebook.
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

import android.os.ConditionVariable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import com.facebook.internal.Utility;

import java.io.IOException;
import java.util.concurrent.Executor;

public final class SettingsTests extends AndroidTestCase {

    @SmallTest @MediumTest @LargeTest
    public void testGetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        Settings.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                condition.open();
            }
        });

        boolean success = condition.block(5000);
        assertTrue(success);
    }

    @SmallTest @MediumTest @LargeTest
    public void testSetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() { }
        };

        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                assertEquals(runnable, command);
                command.run();

                condition.open();
            }
        };

        Executor original = Settings.getExecutor();
        try {
            Settings.setExecutor(executor);
            Settings.getExecutor().execute(runnable);

            boolean success = condition.block(5000);
            assertTrue(success);
        } finally {
            Settings.setExecutor(original);
        }
    }

    @SmallTest @MediumTest @LargeTest
    public void testLogdException() {
        try {
            throw new IOException("Simulated error");
        } catch (IOException e) {
            Utility.logd("SettingsTest", e);
        }

        try {
            throw new IOException(null);
        } catch (IOException e) {
            Utility.logd("SettingsTest", e);
        }
    }
}
