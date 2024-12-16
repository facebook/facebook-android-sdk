/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
