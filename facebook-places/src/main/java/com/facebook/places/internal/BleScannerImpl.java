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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.FacebookSdk;
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
@TargetApi(21)
@SuppressWarnings("MissingPermission")
public class BleScannerImpl implements BleScanner {

    private static final String TAG = "BleScannerImpl";

    private static final byte[] IBEACON_PREFIX = fromHexString("ff4c000215");
    private static final byte[] EDDYSTONE_PREFIX = fromHexString("16aafe");
    private static final byte[] GRAVITY_PREFIX = fromHexString("17ffab01");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private LocationPackageRequestParams params;
    private int errorCode;
    private final List<BluetoothScanResult> scanResults = new ArrayList<>();
    private boolean isScanInProgress;
    private ScanCallBackImpl scanCallBack;
    private Context context;

    BleScannerImpl(Context context, LocationPackageRequestParams params) {
        this.context = context;
        this.params = params;
    }

    @Override
    public synchronized void initAndCheckEligibility() throws ScannerException {
        if (Build.VERSION.SDK_INT < ScannerFactory.OS_VERSION_LOLLIPOP) {
            throw new ScannerException(ScannerException.Type.NOT_SUPPORTED);
        }
        if (!Validate.hasBluetoothPermission(context)) {
            throw new ScannerException(ScannerException.Type.PERMISSION_DENIED);
        }
        if (!Validate.hasLocationPermission(context)) {
            throw new ScannerException(ScannerException.Type.PERMISSION_DENIED);
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new ScannerException(ScannerException.Type.DISABLED);
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            throw new ScannerException(ScannerException.Type.UNKNOWN_ERROR);
        }
    }

    @Override
    public synchronized void startScanning() throws ScannerException {
        if (isScanInProgress) {
            throw new ScannerException(ScannerException.Type.SCAN_ALREADY_IN_PROGRESS);
        }
        scanCallBack = new ScanCallBackImpl();
        isScanInProgress = true;
        errorCode = 0;

        synchronized (scanResults) {
            scanResults.clear();
        }

        if (bluetoothLeScanner == null) {
            throw new ScannerException(ScannerException.Type.UNKNOWN_ERROR);
        }

        try {
            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            builder.setReportDelay(0);
            bluetoothLeScanner.startScan(null, builder.build(), scanCallBack);
            isScanInProgress = true;
        } catch (Exception e) {
            throw new ScannerException(ScannerException.Type.UNKNOWN_ERROR);
        }
    }

    @Override
    public synchronized void stopScanning() {
        bluetoothLeScanner.flushPendingScanResults(scanCallBack);
        bluetoothLeScanner.stopScan(scanCallBack);
        waitForMainLooper(params.getBluetoothFlushResultsTimeoutMs());
        isScanInProgress = false;
    }

    private void waitForMainLooper(long maxWaitTimeoutMs) {
        try {
            // wait until all callbacks queued in the MainLooper have been processed.
            final Object lock = new Object();
            synchronized (lock) {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (lock) {
                                lock.notify();
                            }
                        } catch (Exception e) {
                            logException("Exception waiting for main looper", e);
                        }
                    }
                });

                lock.wait(maxWaitTimeoutMs);
            }
        } catch (Exception e) {
            logException("Exception waiting for main looper", e);
        }
    }

    @Override
    public synchronized int getErrorCode() {
        return errorCode;
    }

    @Override
    public synchronized List<BluetoothScanResult> getScanResults() {
        List<BluetoothScanResult> output;
        synchronized (scanResults) {
            int maxSanResults = params.getBluetoothMaxScanResults();
            if (scanResults.size() > maxSanResults) {
                // Keep the scan results with strongest rssi
                output = new ArrayList<>(maxSanResults);
                Comparator<BluetoothScanResult> comparator = new Comparator<BluetoothScanResult>() {
                    @Override
                    public int compare(BluetoothScanResult lhs, BluetoothScanResult rhs) {
                        return rhs.rssi - lhs.rssi;
                    }
                };
                Collections.sort(scanResults, comparator);
                output.addAll(scanResults.subList(0, maxSanResults));
            } else {
                output = new ArrayList<>(scanResults.size());
                output.addAll(scanResults);
            }
        }
        return output;
    }

    private class ScanCallBackImpl extends ScanCallback {

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            BleScannerImpl.this.errorCode = errorCode;
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            try {
                synchronized (scanResults) {
                    for (ScanResult result : results) {
                        BluetoothScanResult bluetoothScanResult = newBluetoothScanResult(result);
                        if (bluetoothScanResult != null) {
                            scanResults.add(bluetoothScanResult);
                        }
                    }
                }
            } catch (Exception e) {
                logException("Exception in ble scan callback", e);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            try {
                synchronized (scanResults) {
                    BluetoothScanResult bluetoothScanResult = newBluetoothScanResult(result);
                    if (bluetoothScanResult != null) {
                        scanResults.add(bluetoothScanResult);
                    }
                }
            } catch (Exception e) {
                logException("Exception in ble scan callback", e);
            }
        }
    }

    private static BluetoothScanResult newBluetoothScanResult(ScanResult scanResult) {
        ScanRecord scanRecord = scanResult.getScanRecord();
        byte[] scanRecordBytes = scanRecord.getBytes();
        if (isBeacon(scanRecordBytes)) {
            String payload = formatPayload(scanRecord.getBytes());
            int rssi = scanResult.getRssi();
            BluetoothScanResult bluetoothScanResult = new BluetoothScanResult(payload, rssi);
            return bluetoothScanResult;
        }
        return null;
    }

    private static String formatPayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        int payloadLength = getPayloadLength(payload);
        return toHexString(payload, payloadLength);
    }

    private static int getPayloadLength(byte[] payload) {
        int offset = 0;
        while (offset < payload.length) {
            byte length = payload[offset];
            if (length == 0) {
                // the end of the content has been reached
                return offset;
            } else if (length < 0) {
                // unexpected, take the full payload
                return payload.length;
            }
            offset += 1 + length;
        }
        return payload.length;
    }

    private static String toHexString(byte[] bytes, int length) {
        StringBuffer sb = new StringBuffer();
        if (length < 0 || length > bytes.length) {
            length = bytes.length;
        }
        for (int i = 0; i < length; i++) {
            byte b = bytes[i];
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void logException(String message, Exception e) {
        if (FacebookSdk.isDebugEnabled()) {
            Log.e(TAG, message, e);
        }
    }

    private static boolean isBeacon(byte[] payload) {
        if (payload == null) {
            return false;
        }
        int startIndex = 0;
        int payloadLength = payload.length;
        while (startIndex < payloadLength) {
            int advLengthField = payload[startIndex];
            if (advLengthField <= 0) {
                return false;
            }
            int advPacketLength = 1 + advLengthField;
            if (startIndex + advPacketLength > payloadLength) {
                return false;
            }
            if (isAdvPacketBeacon(payload, startIndex)) {
                return true;
            }
            startIndex += advPacketLength;
        }
        return false;
    }

    private static boolean isAdvPacketBeacon(byte[] payload, int advStartIndex) {
        if (isArrayContained(payload, advStartIndex + 1, IBEACON_PREFIX)) {
            return true;
        }
        if (isArrayContained(payload, advStartIndex + 1, EDDYSTONE_PREFIX)) {
            return true;
        }
        if (isArrayContained(payload, advStartIndex, GRAVITY_PREFIX)) {
            return true;
        }
        if (isAltBeacon(payload, advStartIndex)) {
            return true;
        }
        return false;
    }

    private static boolean isAltBeacon(byte[] payload, int startIndex) {
        if (startIndex + 5 < payload.length) {
            byte length = payload[startIndex];
            byte packetType = payload[startIndex + 1];
            byte beaconCode1 = payload[startIndex + 4];
            byte beaconCode2 = payload[startIndex + 5];
            return length == (byte) 0x1b
                    && packetType == (byte) 0xff
                    && beaconCode1 == (byte) 0xbe
                    && beaconCode2 == (byte) 0xac;
        }
        return false;
    }

    private static boolean isArrayContained(byte[] array1, int startIndex1, byte[] array2) {
        int length = array2.length;
        if (startIndex1 + length > array1.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (array1[startIndex1 + i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] fromHexString(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] =
                    (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                            + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }
}
