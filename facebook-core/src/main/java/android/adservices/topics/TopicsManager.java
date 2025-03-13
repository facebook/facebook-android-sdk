/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package android.adservices.topics;

import androidx.annotation.NonNull;
import android.content.Context;
import android.os.OutcomeReceiver;
import java.util.concurrent.Executor;

public final class TopicsManager {
    TopicsManager() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public static TopicsManager get(@NonNull Context context) {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public void getTopics(@NonNull GetTopicsRequest getTopicsRequest, @NonNull Executor executor, @NonNull OutcomeReceiver<GetTopicsResponse, Exception> callback) {
        throw new RuntimeException("Stub!");
    }
}
