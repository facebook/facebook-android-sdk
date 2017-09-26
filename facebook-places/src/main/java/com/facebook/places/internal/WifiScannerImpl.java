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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.facebook.internal.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * com.facebook.places.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
@SuppressWarnings("MissingPermission")
public class WifiScannerImpl implements WifiScanner {

    private static final String SSID_NOMAP = "_nomap";
    private static final String SSID_OPTOUT = "_optout";

    private Context context;
    private WifiManager wifiManager;
    private ScanResultBroadcastReceiver broadcastReceiver;
    private final Object scanLock = new Object();
    private final LocationPackageRequestParams params;

    WifiScannerImpl(Context context, LocationPackageRequestParams params) {
        this.context = context;
        this.params = params;
    }

    @Override
    public void initAndCheckEligibility() throws ScannerException {

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            throw new ScannerException(ScannerException.Type.NOT_SUPPORTED);
        }

        if (!Validate.hasWiFiPermission(context)) {
            throw new ScannerException(ScannerException.Type.PERMISSION_DENIED);
        }

        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }

        boolean isWifiScanningAlwaysOn = isWifiScanningAlwaysOn();

        if (!isWifiScanningAlwaysOn && !wifiManager.isWifiEnabled()) {
            throw new ScannerException(ScannerException.Type.DISABLED);
        }
    }

    @Override
    public WifiScanResult getConnectedWifi() throws ScannerException {
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null
                    || TextUtils.isEmpty(wifiInfo.getBSSID())
                    || wifiInfo.getSupplicantState() != SupplicantState.COMPLETED
                    || isWifiSsidBlacklisted(wifiInfo.getSSID())) {
                return null;
            }
            WifiScanResult wifiScanResult = new WifiScanResult();
            wifiScanResult.bssid = wifiInfo.getBSSID();
            wifiScanResult.ssid = wifiInfo.getSSID();
            wifiScanResult.rssi = wifiInfo.getRssi();
            if (Build.VERSION.SDK_INT >= ScannerFactory.OS_VERSION_LOLLIPOP) {
                wifiScanResult.frequency = wifiInfo.getFrequency();
            }
            return wifiScanResult;
        } catch (Exception e) {
            throw new ScannerException(ScannerException.Type.UNKNOWN_ERROR, e);
        }
    }

    @Override
    public boolean isWifiScanningEnabled() {
        try {
            initAndCheckEligibility();
            if (Validate.hasLocationPermission(context)) {
                return true;
            }
        } catch (ScannerException e) {
            // ignore
        }
        return false;
    }

    private boolean isWifiScanningAlwaysOn() {
        if (Build.VERSION.SDK_INT >= ScannerFactory.OS_VERSION_JELLY_BEAN_MR2) {
            return wifiManager.isScanAlwaysAvailable();
        }
        return false;
    }

    private List<WifiScanResult> getCachedScanResults()
            throws ScannerException {
        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            scanResults = filterWifiScanResultsByMaxAge(scanResults, params.getWifiScanMaxAgeMs());
            filterResults(scanResults, params.getWifiMaxScanResults());
            List<WifiScanResult> wifiScanResults = new ArrayList<>(scanResults.size());
            for (ScanResult scanResult : scanResults) {
                if (!isWifiSsidBlacklisted(scanResult.SSID)) {
                    WifiScanResult wifiScanResult = new WifiScanResult();
                    wifiScanResult.bssid = scanResult.BSSID;
                    wifiScanResult.ssid = scanResult.SSID;
                    wifiScanResult.rssi = scanResult.level;
                    wifiScanResult.frequency = scanResult.frequency;
                    wifiScanResults.add(wifiScanResult);
                }
            }
            return wifiScanResults;
        } catch (Exception e) {
            throw new ScannerException(ScannerException.Type.UNKNOWN_ERROR, e);
        }
    }

    private static boolean isWifiSsidBlacklisted(String ssid) {
        if (ssid != null) {
            if (ssid.endsWith(SSID_NOMAP)
                || ssid.contains(SSID_OPTOUT)) {
                return true;
            }
        }
        return false;
    }

    private static void filterResults(List<ScanResult> scanResults, int maxResults) {
        if (scanResults.size() > maxResults) {
            Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return rhs.level - lhs.level;
                }
            };
            Collections.sort(scanResults, comparator);
            scanResults.subList(maxResults, scanResults.size()).clear();
        }
    }

    private static List<ScanResult> filterWifiScanResultsByMaxAge(
            List<ScanResult> scanResults,
            long maxAgeMs) {
        List<ScanResult> filtered = new ArrayList<>();
        if (scanResults != null) {
            if (Build.VERSION.SDK_INT < ScannerFactory.OS_VERSION_JELLY_BEAN_MR1) {
                filtered.addAll(scanResults);
            } else {
                long nowSinceBootMs = SystemClock.elapsedRealtime();
                for (ScanResult result : scanResults) {
                    long ageMs = nowSinceBootMs - (result.timestamp / 1000);
                    if (ageMs < 0) {
                        // Some platform use unix timestmap
                        ageMs = System.currentTimeMillis() - result.timestamp;
                    }
                    if (ageMs < maxAgeMs) {
                        filtered.add(result);
                    }
                }
            }
        }
        return filtered;
    }

    @Override
    public synchronized List<WifiScanResult> getWifiScans()
            throws ScannerException{
        List<WifiScanResult> wifiScanResults = null;
        if (!params.isWifiActiveScanForced()) {
              wifiScanResults = getCachedScanResults();
        }
        boolean isListEmpty = wifiScanResults == null || wifiScanResults.isEmpty();
        if (params.isWifiActiveScanForced() || (params.isWifiActiveScanAllowed() && isListEmpty)) {
            wifiScanResults = getActiveScanResults();
        }
        return wifiScanResults;
    }

    private List<WifiScanResult> getActiveScanResults()
            throws ScannerException{
        List<WifiScanResult> wifiScanResults = null;
        try {
            if (Validate.hasChangeWifiStatePermission(context)) {
                registerBroadcastReceiver();
                boolean isScanStarted = wifiManager.startScan();
                if (isScanStarted) {
                    try {
                        synchronized (scanLock) {
                            scanLock.wait(params.getWifiScanTimeoutMs());
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    wifiScanResults = getCachedScanResults();
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            unregisterBroadcastReceiver();
        }
        return wifiScanResults;
    }

    private void registerBroadcastReceiver() {
        if (broadcastReceiver != null) {
            unregisterBroadcastReceiver();
        }
        broadcastReceiver = new ScanResultBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            try {
                context.unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                // ignore
            }
            broadcastReceiver = null;
        }
    }

    private class ScanResultBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    synchronized (scanLock) {
                       scanLock.notify();
                    }
                    unregisterBroadcastReceiver();
                }
            }
        }
    }
}
