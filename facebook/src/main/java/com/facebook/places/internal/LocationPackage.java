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

import android.location.Location;

import java.util.List;

/**
 * Describes the nearby location sensors.
 * Use LocationPackageManager to generate an instance. {@link LocationPackageManager}
 */
public class LocationPackage {

    /**
     * The location (latitude/longitude).
     */
    public Location location;

    /**
     * Indicates the error type that occured when fetching the location, or null if there was
     * no error.
     */
    public ScannerException.Type locationError;

    /**
     * Indicates whether wifi scanning was enabled/possible at the moment of the sensor collection.
     */
    public boolean isWifiScanningEnabled;

    /**
     * Describes the wifi network connected at the moment of the sensor collection.
     */
    public WifiScanResult connectedWifi;

    /**
     * Describes the list of nearby wifi networks at the moment of the sensor collection.
     */
    public List<WifiScanResult> ambientWifi;

    /**
     * Indicates whether bluetooth scanning was enabled at the moment of the sensor collection.
     */
    public boolean isBluetoothScanningEnabled;

    /**
     * Describes the list of nearby bluetooth Low Energy beacons at the moment of the sensor
     * collection.
     */
    public List<BluetoothScanResult> ambientBluetoothLe;
}
