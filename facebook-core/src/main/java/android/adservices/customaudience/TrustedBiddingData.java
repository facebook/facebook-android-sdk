/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package android.adservices.customaudience;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;

public class TrustedBiddingData {
    public static class Builder {
        public Builder setTrustedBiddingUri(@NonNull Uri trustedBiddingUri) {
            throw new RuntimeException("Stub!");
        }

        public Builder setTrustedBiddingKeys(@NonNull List<String> trustedBiddingKeys) {
            throw new RuntimeException("Stub!");
        }

        public TrustedBiddingData build() {
            throw new RuntimeException("Stub!");
        }
    }

}
