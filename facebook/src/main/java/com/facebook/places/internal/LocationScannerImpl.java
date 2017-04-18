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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;

import com.facebook.internal.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * com.facebook.places.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
@SuppressWarnings("MissingPermission")
public class LocationScannerImpl implements LocationScanner, LocationListener {

    private static final long MIN_TIME_BETWEEN_UPDATES = 100L;
    private static final float MIN_DISTANCE_BETWEEN_UPDATES = 0f;

    private Context context;
    private LocationManager locationManager;
    private LocationPackageRequestParams params;
    private Location freshLocation;
    private final Object scanLock = new Object();
    private List<String> enabledProviders;

    public LocationScannerImpl(
            Context context,
            LocationPackageRequestParams params) {
        this.context = context;
        this.params = params;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void initAndCheckEligibility() throws ScannerException {

        if (!Validate.hasLocationPermission(context)) {
            throw new ScannerException(ScannerException.Type.PERMISSION_DENIED);
        }

        enabledProviders = new ArrayList<>(params.getLocationProviders().length);
        for (String provider : params.getLocationProviders()) {
            if (locationManager.isProviderEnabled(provider)) {
                enabledProviders.add(provider);
            }
        }

        if (enabledProviders.isEmpty()) {
            throw new ScannerException(ScannerException.Type.DISABLED);
        }
    }

    private Location getLastLocation(String provider) {
        Location lastLocation = locationManager.getLastKnownLocation(provider);
        if (lastLocation != null) {
            long lastLocationTs = lastLocation.getTime();
            long locationAgeMs = System.currentTimeMillis() - lastLocationTs;
            if (locationAgeMs < params.getLastLocationMaxAgeMs()) {
                return lastLocation;
            }
        }
        return null;
    }

    @Override
    public Location getLocation() throws ScannerException {
        for (String provider : enabledProviders) {
            Location lastLocation = getLastLocation(provider);
            if (lastLocation != null) {
                return lastLocation;
            }
        }
        return getFreshLocation();
    }

    private Location getFreshLocation() throws ScannerException {
        freshLocation = null;
        HandlerThread handlerThread = new HandlerThread("LocationScanner");
        try {
            handlerThread.start();
            for (String provider : enabledProviders) {
                locationManager.requestLocationUpdates(
                        provider,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_BETWEEN_UPDATES,
                        this,
                        handlerThread.getLooper());
            }
            try {
                synchronized (scanLock) {
                    scanLock.wait(params.getLocationRequestTimeoutMs());
                }
            } catch (Exception e) {
                // ignore
            }
        } finally {
            locationManager.removeUpdates(this);
            handlerThread.quit();
        }

        if (freshLocation == null) {
            throw new ScannerException(ScannerException.Type.TIMEOUT);
        }
        return freshLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (freshLocation == null) {
            if (location.getAccuracy() < params.getLocationMaxAccuracyMeters()) {
                synchronized (scanLock) {
                    freshLocation = location;
                     scanLock.notify();
                }
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ignore
    }

    @Override
    public void onProviderEnabled(String provider) {
        // ignore
    }

    @Override
    public void onProviderDisabled(String provider) {
        // ignore
    }
}
