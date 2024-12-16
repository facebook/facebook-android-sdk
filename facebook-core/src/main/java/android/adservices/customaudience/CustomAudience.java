/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package android.adservices.customaudience;

import android.adservices.common.AdData;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CustomAudience {
    public static class Builder {
        @NonNull
        public CustomAudience.Builder setBuyer(@NonNull AdTechIdentifier buyer) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public CustomAudience.Builder setName(@NonNull String name) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public CustomAudience.Builder setDailyUpdateUri(@NonNull Uri dailyUpdateUri) {
            throw new RuntimeException("Stub!");
        }

        @NonNull
        public CustomAudience.Builder setBiddingLogicUri(@NonNull Uri biddingLogicUri) {
            throw new RuntimeException("Stub!");
        }
        public CustomAudience.Builder setAds(@Nullable List<AdData> ads) {
            throw new RuntimeException("Stub!");
        }

        public CustomAudience.Builder setUserBiddingSignals(
                @Nullable AdSelectionSignals userBiddingSignals) {
            throw new RuntimeException("Stub!");
        }

        public CustomAudience.Builder setTrustedBiddingData(
                @Nullable TrustedBiddingData trustedBiddingData) {
            throw new RuntimeException("Stub!");
        }

        public CustomAudience build() {
            throw new RuntimeException("Stub!");
        }
    }
}
