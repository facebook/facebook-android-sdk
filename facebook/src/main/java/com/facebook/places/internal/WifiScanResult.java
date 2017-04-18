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
 * Describes a wifi access point scan result.
 */
public class WifiScanResult {

    /**
     * The name of the wifi access point.
     */
    public String ssid;

    /**
     * The hardware/mac-address of the access point.
     */
    public String bssid;

    /**
     * The detected signal strength in dBm.
     */
    public int rssi;

    /**
     * The frequency in MHz of the channel used by the access point.
     * {@link android.net.wifi.ScanResult}
     */
    public int frequency;

    /**
     * Constructs a new {@code WifiScanResult}
     */
    public WifiScanResult() {

    }

    /**
     * Constructs a new {@code WifiScanResult}
     *
     * @param ssid The name of the wifi access point.
     * @param bssid The hardware/mac-address of the access point.
     * @param rssi The detected signal strength in dBm.
     * @param frequency The frequency in MHz of the channel used by the access point.
     * {@link android.net.wifi.ScanResult}
     */
    public WifiScanResult(String ssid, String bssid, int rssi, int frequency) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.frequency = frequency;
    }
}
