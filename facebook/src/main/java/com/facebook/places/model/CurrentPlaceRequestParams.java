/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.places.model;

import android.location.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * Describes the parameters of the current place request.
 * Use {@link com.facebook.places.PlaceManager} to create a new current place request.
 */
public class CurrentPlaceRequestParams {

    /**
     * Describes the confidence level of the current place response.
     *
     * A place with field {@code CONFIDENCE_LEVEL} set to {@code HIGH} indicates that there is a
     * high likelihood that the user is currently located at that place.
     */
    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH,
    };

    /**
     * Describes the location scanning behavior when creating a new current place request.
     */
    public enum ScanMode {

        /**
         * In high accuracy mode, {@link com.facebook.places.PlaceManager}, scans for
         * nearby Wi-Fi and Bluetooth Low Energy beacons to maximize the accuracy of the current
         * place request. High accuracy mode is the default value.
         */
        HIGH_ACCURACY,

        /**
         * In low latency mode, {@link com.facebook.places.PlaceManager}, reduces
         * nearby Wi-Fi and Bluetooth beacon scanning to a minimum to prioritize
         * low latency rather than accuracy. Using low latency mode reduces the maximum time
         * taken to generate the current place request. To minimize latency mode, a
         * location must be specified on the {@link Builder}.
         */
        LOW_LATENCY,
    }

    private final Location location;
    private final ScanMode scanMode;
    private final ConfidenceLevel minConfidenceLevel;
    private final int limit;
    private final Set<String> fields = new HashSet<>();

    private CurrentPlaceRequestParams(Builder b) {
        location = b.location;
        scanMode = b.scanMode;
        minConfidenceLevel = b.minConfidenceLevel;
        limit = b.limit;
        fields.addAll(b.fields);
    }

    /**
     * The current location of the user. If null, the SDK automatically retrieves the current
     * device location using {@link android.location.LocationManager}
     *
     * @return The current device location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * The scan mode used when generating the current place request.
     * @return The scan mode used when generating the current place request.
     */
    public ScanMode getScanMode() {
        return scanMode;
    }

    /**
     * Get the minimum confidence level of places to return.
     * @return The minimum {@link ConfidenceLevel} of places to return.
     */
    public ConfidenceLevel getMinConfidenceLevel() {
        return minConfidenceLevel;
    }

    /**
     * The maximum number of places to be returned.
     * @return The maximum number of places to be returned.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * The fields to be returned in the response.
     * @return The fields to be returned in the response.
     */
    public Set<String> getFields() {
        return fields;
    }

    /**
     * Describes the builder to create a {@link CurrentPlaceRequestParams}
     */
    public static class Builder {

        private Location location;
        private ScanMode scanMode = ScanMode.HIGH_ACCURACY;
        private ConfidenceLevel minConfidenceLevel;
        private int limit;
        private final Set<String> fields = new HashSet<>();

        /**
         * Sets the current user location. This parameter is optional.
         * If a location is not provided, the SDK automatically retrieves the current device
         * location using {@link android.location.LocationManager}.
         *
         * @param location The location at which the user is currently located.
         * @return this builder
         */
        public Builder setLocation(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the scan mode to be used. When creating a current place request, the SDK
         * scans for nearby Wi-Fi access points and Bluetooth Low Energy beacons. This parameter
         * determines the behavior of that scan. High accuracy is the default value.
         * In order to use low latency, you must specify a location on this builder.
         *
         * @param scanMode The scan mode used when generating the current place request.
         * @return this builder
         */
        public Builder setScanMode(ScanMode scanMode) {
            this.scanMode = scanMode;
            return this;
        }

        /**
         * The minimum confidence level of suggestions to return. See {@link ConfidenceLevel}.
         * @param minConfidenceLevel the minimum confidence level of suggestions to return.
         * @return this builder
         */
        public Builder setMinConfidenceLevel(ConfidenceLevel minConfidenceLevel) {
            this.minConfidenceLevel = minConfidenceLevel;
            return this;
        }

        /**
         * Sets the maximum number of results to be returned.
         *
         * @param limit the maximum number of results to be returned.
         * @return this builder
         */
        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Specifies a field to be added to the response. Refer to {@link PlaceFields} for a list
         * of known place fields. Refer to the online Places Graph documentation for an up to date
         * list of fields.
         * @param field The field to be returned in the response {@link PlaceFields}.
         * @return this builder
         */
        public Builder addField(String field) {
            fields.add(field);
            return this;
        }

        /**
         * Returns the {@link CurrentPlaceRequestParams}
         * @return the {@link CurrentPlaceRequestParams}
         */
        public CurrentPlaceRequestParams build() {
            return new CurrentPlaceRequestParams(this);
        }
    }
}
