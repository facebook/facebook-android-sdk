/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.adservices.measurement;

import android.adservices.common.AdServicesOutcomeReceiver;
import android.content.Context;
import android.net.Uri;
import android.os.OutcomeReceiver;
import java.util.concurrent.Executor;

/**
 * Interface to access the android MeasurementManager class {@link
 * android.adservices.measurement.MeasurementManager}. The actual implementation is going to be
 * provided at runtime on the device.
 */
public class MeasurementManager {

    MeasurementManager() {
        throw new RuntimeException("Stub!");
    }

    public static MeasurementManager get(Context context) {
        throw new RuntimeException("Stub!");
    }

    public void registerTrigger(
            Uri trigger, Executor executor, OutcomeReceiver<Object, Exception> callback) {
        throw new RuntimeException("Stub!");
    }

    public void registerTrigger(
            Uri trigger, Executor executor, AdServicesOutcomeReceiver<Object, Exception> callback) {
        throw new RuntimeException("Stub!");
    }
}
