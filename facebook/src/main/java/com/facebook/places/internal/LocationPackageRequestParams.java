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

package com.facebook.places.internal;

import android.location.LocationManager;

/**
 * Describes the nearby location sensors.
 * Use {@link LocationPackageManager} to instantiate an instance.
 */
public class LocationPackageRequestParams {

    private static final boolean DEFAULT_LOCATION_ENABLED = true;
    private static final String[] DEFAULT_LOCATION_PROVIDERS =
            new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER};
    private static final float DEFAULT_LOCATION_MAX_ACCURACY_METERS = 100f;
    private static final long DEFAULT_LOCATION_REQUEST_TIMEOUT_MS = 30 * 1000;
    private static final long DEFAULT_LAST_LOCATION_MAX_AGE_MS = 60 * 1000;

    private static final boolean DEFAULT_WIFI_ENABLED = true;
    private static final long DEFAULT_WIFI_SCAN_MAX_AGE_MS = 30 * 1000;
    private static final long DEFAULT_WIFI_SCAN_TIMEOUT_MS = 6 * 1000;
    private static final int DEFAULT_WIFI_MAX_SCAN_RESULTS = 25;
    private static final boolean DEFAULT_WIFI_ACTIVE_SCAN_ALLOWED = true;
    private static final boolean DEFAULT_WIFI_ACTIVE_SCAN_FORCED = false;

    private static final boolean DEFAULT_BLUETOOTH_ENABLED = true;
    private static final long DEFAULT_BLUETOOTH_SCAN_DURATION_MS = 500;
    private static final int DEFAULT_BLUETOOTH_MAX_SCAN_RESULTS = 25;
    private static final long DEFAULT_BLUETOOTH_FLUSH_RESULTS_TIMEOUT_MS = 300;

    private boolean isLocationScanEnabled;
    private final String[] locationProviders;
    private float locationMaxAccuracyMeters;
    private long locationRequestTimeoutMs;
    private long lastLocationMaxAgeMs;

    private boolean isWifiScanEnabled;
    private long wifiScanMaxAgeMs;
    private int wifiMaxScanResults;
    private long wifiScanTimeoutMs;
    private boolean isWifiActiveScanAllowed;
    private boolean isWifiActiveScanForced;

    private boolean isBluetoothScanEnabled;
    private long bluetoothScanDurationMs;
    private int bluetoothMaxScanResults;
    private long bluetoothFlushResultsTimeoutMs;

    private LocationPackageRequestParams(Builder b) {
        isLocationScanEnabled = b.isLocationScanEnabled;
        locationProviders = b.locationProviders;
        locationMaxAccuracyMeters = b.locationMaxAccuracyMeters;
        locationRequestTimeoutMs = b.locationRequestTimeoutMs;
        lastLocationMaxAgeMs = b.lastLocationMaxAgeMs;

        isWifiScanEnabled = b.isWifiScanEnabled;
        wifiScanMaxAgeMs = b.wifiScanMaxAgeMs;
        wifiMaxScanResults = b.wifiMaxScanResults;
        wifiScanTimeoutMs = b.wifiScanTimeoutMs;
        isWifiActiveScanAllowed = b.isWifiActiveScanAllowed;
        isWifiActiveScanForced = b.isWifiActiveScanForced;

        isBluetoothScanEnabled = b.isBluetoothScanEnabled;
        bluetoothScanDurationMs = b.bluetoothScanDurationMs;
        bluetoothMaxScanResults = b.bluetoothMaxScanResults;
        bluetoothFlushResultsTimeoutMs = b.bluetoothFlushResultsTimeoutMs;
    }

    public boolean isLocationScanEnabled() {
        return isLocationScanEnabled;
    }

    public String[] getLocationProviders() {
        return locationProviders;
    }

    public float getLocationMaxAccuracyMeters() {
        return locationMaxAccuracyMeters;
    }

    public long getLocationRequestTimeoutMs() {
        return locationRequestTimeoutMs;
    }

    public long getLastLocationMaxAgeMs() {
        return lastLocationMaxAgeMs;
    }

    public boolean isWifiScanEnabled() {
        return isWifiScanEnabled;
    }

    public long getWifiScanMaxAgeMs() {
        return wifiScanMaxAgeMs;
    }

    public int getWifiMaxScanResults() {
        return wifiMaxScanResults;
    }

    public long getWifiScanTimeoutMs() {
        return wifiScanTimeoutMs;
    }

    public boolean isWifiActiveScanAllowed() {
        return isWifiActiveScanAllowed;
    }

    public boolean isWifiActiveScanForced() {
        return isWifiActiveScanForced;
    }

    public boolean isBluetoothScanEnabled() {
        return isBluetoothScanEnabled;
    }

    public long getBluetoothScanDurationMs() {
        return bluetoothScanDurationMs;
    }

    public long getBluetoothFlushResultsTimeoutMs() {
        return bluetoothFlushResultsTimeoutMs;
    }

    public int getBluetoothMaxScanResults() {
        return bluetoothMaxScanResults;
    }

    public static class Builder {
        private boolean isLocationScanEnabled = DEFAULT_LOCATION_ENABLED;
        private String[] locationProviders = DEFAULT_LOCATION_PROVIDERS;
        private float locationMaxAccuracyMeters = DEFAULT_LOCATION_MAX_ACCURACY_METERS;
        private long locationRequestTimeoutMs = DEFAULT_LOCATION_REQUEST_TIMEOUT_MS;
        private long lastLocationMaxAgeMs = DEFAULT_LAST_LOCATION_MAX_AGE_MS;

        private boolean isWifiScanEnabled = DEFAULT_WIFI_ENABLED;
        private long wifiScanMaxAgeMs = DEFAULT_WIFI_SCAN_MAX_AGE_MS;
        private int wifiMaxScanResults = DEFAULT_WIFI_MAX_SCAN_RESULTS;
        private long wifiScanTimeoutMs = DEFAULT_WIFI_SCAN_TIMEOUT_MS;
        private boolean isWifiActiveScanAllowed = DEFAULT_WIFI_ACTIVE_SCAN_ALLOWED;
        private boolean isWifiActiveScanForced = DEFAULT_WIFI_ACTIVE_SCAN_FORCED;

        private boolean isBluetoothScanEnabled = DEFAULT_BLUETOOTH_ENABLED;
        private long bluetoothScanDurationMs = DEFAULT_BLUETOOTH_SCAN_DURATION_MS;
        private int bluetoothMaxScanResults = DEFAULT_BLUETOOTH_MAX_SCAN_RESULTS;
        private long bluetoothFlushResultsTimeoutMs = DEFAULT_BLUETOOTH_FLUSH_RESULTS_TIMEOUT_MS;

        public LocationPackageRequestParams build() {
            return new LocationPackageRequestParams(this);
        }

        public Builder setLocationScanEnabled(boolean locationScanEnabled) {
            isLocationScanEnabled = locationScanEnabled;
            return this;
        }

        public Builder setLastLocationMaxAgeMs(long lastLocationMaxAgeMs) {
            this.lastLocationMaxAgeMs = lastLocationMaxAgeMs;
            return this;
        }

        public Builder setLocationProviders(String[] locationProviders) {
            this.locationProviders = locationProviders;
            return this;
        }

        public Builder setLocationMaxAccuracyMeters(float locationMaxAccuracyMeters) {
            this.locationMaxAccuracyMeters = locationMaxAccuracyMeters;
            return this;
        }

        public Builder setLocationRequestTimeoutMs(long locationRequestTimeoutMs) {
            this.locationRequestTimeoutMs = locationRequestTimeoutMs;
            return this;
        }

        public Builder setWifiScanEnabled(boolean wifiScanEnabled) {
            isWifiScanEnabled = wifiScanEnabled;
            return this;
        }

        public Builder setWifiScanMaxAgeMs(long wifiScanMaxAgeMs) {
            this.wifiScanMaxAgeMs = wifiScanMaxAgeMs;
            return this;
        }

        public Builder setWifiMaxScanResults(int wifiMaxScanResults) {
            this.wifiMaxScanResults = wifiMaxScanResults;
            return this;
        }

        public Builder setWifiScanTimeoutMs(long wifiScanTimeoutMs) {
            this.wifiScanTimeoutMs = wifiScanTimeoutMs;
            return this;
        }

        public Builder setWifiActiveScanAllowed(boolean wifiActiveScanAllowed) {
            isWifiActiveScanAllowed = wifiActiveScanAllowed;
            return this;
        }

        public Builder setWifiActiveScanForced(boolean wifiActiveScanForced) {
            isWifiActiveScanForced = wifiActiveScanForced;
            return this;
        }

        public Builder setBluetoothScanEnabled(boolean bluetoothScanEnabled) {
            isBluetoothScanEnabled = bluetoothScanEnabled;
            return this;
        }

        public Builder setBluetoothScanDurationMs(long bluetoothScanDurationMs) {
            this.bluetoothScanDurationMs = bluetoothScanDurationMs;
            return this;
        }

        public Builder setBluetoothMaxScanResults(int bluetoothMaxScanResults) {
            this.bluetoothMaxScanResults = bluetoothMaxScanResults;
            return this;
        }

        public Builder setBluetoothFlushResultsTimeoutMs(long bluetoothFlushResultsTimeoutMs) {
            this.bluetoothFlushResultsTimeoutMs = bluetoothFlushResultsTimeoutMs;
            return this;
        }
    }
}
