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

import android.content.Context;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.places.PlaceManager;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * com.facebook.places.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class LocationPackageManager {

    private static final String TAG = "LocationPackageManager";

    /**
     * Specifies the interface to be implemented to receive the location sensor data.
     */
    public interface Listener {
        /**
         * Invoked when the location sensor data has been collected. The location sensor data can
         * then be used to increase the accuracy when placing a current place request.
         * {@link PlaceManager}
         *
         * @param locationPackage the nearby wifi and bluetooth beacons that have been collected.
         */
        void onLocationPackage(LocationPackage locationPackage);
    }

    public static void requestLocationPackage(
            final LocationPackageRequestParams requestParams,
            final Listener listener) {
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                LocationPackage locationPackage = new LocationPackage();
                try {
                    // Start scanning tasks
                    FutureTask<LocationPackage> locationScanTask = null;
                    FutureTask<LocationPackage> wifiScanTask = null;
                    FutureTask<LocationPackage> bleScanTask = null;

                    if (requestParams.isLocationScanEnabled()) {

                        LocationScanner locationScanner = ScannerFactory.newLocationScanner(
                                FacebookSdk.getApplicationContext(),
                                requestParams);

                        // Check that location services are enabled, and if not, then abort
                        // sensor scan.
                        locationScanner.initAndCheckEligibility();

                        locationScanTask = newLocationScanFuture(locationScanner, requestParams);
                        FacebookSdk.getExecutor().execute(locationScanTask);
                    }

                    if (requestParams.isWifiScanEnabled()) {
                        wifiScanTask = newWifiScanFuture(requestParams);
                        FacebookSdk.getExecutor().execute(wifiScanTask);
                    }
                    if (requestParams.isBluetoothScanEnabled()) {
                        bleScanTask = newBluetoothScanFuture(requestParams);
                        FacebookSdk.getExecutor().execute(bleScanTask);
                    }

                    // Handle bluetooth results.
                    if (bleScanTask != null) {
                        try {
                            LocationPackage bleSensorData = bleScanTask.get();
                            locationPackage.ambientBluetoothLe = bleSensorData.ambientBluetoothLe;
                            locationPackage.isBluetoothScanningEnabled =
                                    bleSensorData.isBluetoothScanningEnabled;
                        } catch (Exception e) {
                            logException("Exception scanning for bluetooth beacons", e);
                        }
                    }

                    // Handle wifi results
                    if (wifiScanTask != null) {
                        try {
                            LocationPackage wifiSensorData = wifiScanTask.get();
                            locationPackage.isWifiScanningEnabled =
                                    wifiSensorData.isWifiScanningEnabled;
                            locationPackage.connectedWifi = wifiSensorData.connectedWifi;
                            locationPackage.ambientWifi = wifiSensorData.ambientWifi;
                        } catch (Exception e) {
                            logException("Exception scanning for wifi access points", e);
                        }
                    }

                    // Handle location results
                    if (locationScanTask != null) {
                        try {
                            LocationPackage locationSensorData = locationScanTask.get();
                            locationPackage.locationError = locationSensorData.locationError;
                            locationPackage.location = locationSensorData.location;
                        } catch (Exception e) {
                            logException("Exception getting location", e);
                        }
                    }
                } catch (ScannerException e) {
                    logException("Exception scanning for locations", e);
                    locationPackage.locationError = e.type;
                } catch (Exception e) {
                    logException("Exception requesting a location package", e);
                }
                listener.onLocationPackage(locationPackage);
            }
        });
    }

    private static FutureTask<LocationPackage> newLocationScanFuture(
            final LocationScanner locationScanner,
            final LocationPackageRequestParams requestParams) {
        return new FutureTask<>(new Callable<LocationPackage>() {
            @Override
            public LocationPackage call() throws Exception {
                LocationPackage locationPackage = new LocationPackage();
                try {
                    locationPackage.location = locationScanner.getLocation();
                } catch (ScannerException e) {
                    locationPackage.locationError = e.type;
                    logException("Exception while getting location", e);
                } catch (Exception e) {
                    locationPackage.locationError = ScannerException.Type.UNKNOWN_ERROR;
                }
                return locationPackage;
            }
        });
    }

    private static FutureTask<LocationPackage> newBluetoothScanFuture(
            final LocationPackageRequestParams requestParams) {
        return new FutureTask<>(new Callable<LocationPackage>() {
            @Override
            public LocationPackage call() throws Exception {
                LocationPackage locationPackage = new LocationPackage();
                try {
                    Context context = FacebookSdk.getApplicationContext();
                    BleScanner bleScanner = ScannerFactory.newBleScanner(context, requestParams);

                    bleScanner.initAndCheckEligibility();

                    try {
                        bleScanner.startScanning();
                        try {
                            Thread.sleep(requestParams.getBluetoothScanDurationMs());
                        } catch (Exception ex) {
                            // ignore
                        }
                    } finally {
                        bleScanner.stopScanning();
                    }

                    int errorCode = bleScanner.getErrorCode();
                    if (errorCode == 0) {
                        locationPackage.ambientBluetoothLe = bleScanner.getScanResults();
                        locationPackage.isBluetoothScanningEnabled = true;
                    } else {
                        if (FacebookSdk.isDebugEnabled()) {
                            Log.d(
                                TAG,
                                String.format(
                                    "Bluetooth LE scan failed with error: %d",
                                    errorCode));
                        }
                        locationPackage.isBluetoothScanningEnabled = false;
                    }
                } catch (Exception e) {
                    logException("Exception scanning for bluetooth beacons", e);
                    locationPackage.isBluetoothScanningEnabled = false;
                }
                return locationPackage;
            }
        });
    }

    private static FutureTask<LocationPackage> newWifiScanFuture(
            final LocationPackageRequestParams requestParams) {
        return new FutureTask<>(new Callable<LocationPackage>() {
            @Override
            public LocationPackage call() throws Exception {
                LocationPackage locationPackage = new LocationPackage();
                try {
                    Context context = FacebookSdk.getApplicationContext();
                    WifiScanner wifiScanner = ScannerFactory.newWifiScanner(context, requestParams);
                    wifiScanner.initAndCheckEligibility();

                    locationPackage.connectedWifi = wifiScanner.getConnectedWifi();
                    locationPackage.isWifiScanningEnabled = wifiScanner.isWifiScanningEnabled();

                    if (locationPackage.isWifiScanningEnabled) {
                        locationPackage.ambientWifi =
                                wifiScanner.getWifiScans();
                    }
                } catch (Exception e) {
                    logException("Exception scanning for wifi access points", e);
                    locationPackage.isWifiScanningEnabled = false;
                }
                return locationPackage;
            }
        });
    }

    private static void logException(String message, Throwable throwable) {
        if (FacebookSdk.isDebugEnabled()) {
            Log.e(TAG, message, throwable);
        }
    }
}
