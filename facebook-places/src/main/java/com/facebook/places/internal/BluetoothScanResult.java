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

/**
 * Describes the result of a bluetooth Low Energy scan.
 */
public class BluetoothScanResult {

    /**
     * The payload received from the bluetooth Low Energy device.
     * This must be the raw bluetooth Low Energy advertisement payload,
     * as returned by {@code scanRecord.getBytes()} {@link android.bluetooth.le.ScanRecord}
     * E.g., for an iBeacon: 0201041aff4c00021566622e6d652f40ca9e6f6f71666163653e3a5f06c5
     */
    public String payload;

    /**
     * The received signal strength in dBm. {@link android.bluetooth.le.ScanResult}
     * E.g., -92
     */
    public int rssi;

    /**
     * Construct a new Bluetooth Low Energy scan result.
     *
     * @param payload The payload received from the bluetooth Low Energy device.
     * This must be the raw bluetooth Low Energy advertisement payload,
     * as returned by {@code scanRecord.getBytes()} {@link android.bluetooth.le.ScanRecord}
     * E.g., for an iBeacon: 0201041aff4c00021566622e6d652f40ca9e6f6f71666163653e3a5f06c5
     * @param rssi The received signal strength in dBm. {@link android.bluetooth.le.ScanResult}
     * E.g., -92
     */
    public BluetoothScanResult(String payload, int rssi) {
        this.payload = payload;
        this.rssi = rssi;
    }
}
