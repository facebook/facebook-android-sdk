/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package android.adservices.customaudience;

import android.content.Context;
import android.os.OutcomeReceiver;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class CustomAudienceManager {

    public static CustomAudienceManager get(@NonNull Context context) {
        throw new RuntimeException("Stub!");
    }

    public void joinCustomAudience(
            @NonNull JoinCustomAudienceRequest joinCustomAudienceRequest,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Object, Exception> receiver) {
        throw new RuntimeException("Stub!");
    }
}
