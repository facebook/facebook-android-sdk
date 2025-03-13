/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package android.adservices.topics;

import androidx.annotation.NonNull;

public final class GetTopicsRequest {
    GetTopicsRequest() {
        throw new RuntimeException("Stub!");
    }

    @NonNull
    public String getAdsSdkName() {
        throw new RuntimeException("Stub!");
    }

    public boolean shouldRecordObservation() {
        throw new RuntimeException("Stub!");
    }

    public static final class Builder {
        public Builder() {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setAdsSdkName(@NonNull String adsSdkName) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public Builder setShouldRecordObservation(boolean recordObservation) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public GetTopicsRequest build() {
            throw new RuntimeException("Stub!");
        }
    }
}
