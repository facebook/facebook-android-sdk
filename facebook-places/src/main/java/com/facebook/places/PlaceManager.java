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

package com.facebook.places;

import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;

import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.internal.Utility;
import com.facebook.places.internal.LocationPackageManager;
import com.facebook.places.internal.LocationPackageRequestParams;
import com.facebook.places.internal.ScannerException;
import com.facebook.places.internal.BluetoothScanResult;
import com.facebook.places.model.CurrentPlaceFeedbackRequestParams;
import com.facebook.places.internal.LocationPackage;
import com.facebook.places.model.PlaceInfoRequestParams;
import com.facebook.places.model.PlaceSearchRequestParams;
import com.facebook.places.model.CurrentPlaceRequestParams;
import com.facebook.places.model.CurrentPlaceRequestParams.ConfidenceLevel;
import com.facebook.places.internal.WifiScanResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides an interface to search and query the Places Graph.
 * Supports querying the end user's' current place, searching nearby places, and fetching
 * place information details.
 */
public class PlaceManager {

    private static final String SEARCH = "search";
    private static final String CURRENT_PLACE_RESULTS = "current_place/results";
    private static final String CURRENT_PLACE_FEEDBACK = "current_place/feedback";

    private static final String PARAM_ACCESS_POINTS = "access_points";
    private static final String PARAM_ACCURACY = "accuracy";
    private static final String PARAM_ALTITUDE = "altitude";
    private static final String PARAM_BLUETOOTH = "bluetooth";
    private static final String PARAM_CATEGORIES = "categories";
    private static final String PARAM_CENTER = "center";
    private static final String PARAM_COORDINATES = "coordinates";
    private static final String PARAM_CURRENT_CONNECTION = "current_connection";
    private static final String PARAM_DISTANCE = "distance";
    private static final String PARAM_ENABLED = "enabled";
    private static final String PARAM_FIELDS = "fields";
    private static final String PARAM_FREQUENCY = "frequency";
    private static final String PARAM_HEADING = "heading";
    private static final String PARAM_LATITUDE = "latitude";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_LONGITUDE = "longitude";
    private static final String PARAM_MAC_ADDRESS = "mac_address";
    private static final String PARAM_MIN_CONFIDENCE_LEVEL = "min_confidence_level";
    private static final String PARAM_PAYLOAD = "payload";
    private static final String PARAM_PLACE_ID = "place_id";
    private static final String PARAM_Q = "q";
    private static final String PARAM_RSSI = "rssi";
    private static final String PARAM_SCANS = "scans";
    private static final String PARAM_SIGNAL_STRENGTH = "signal_strength";
    private static final String PARAM_SPEED = "speed";
    private static final String PARAM_SSID = "ssid";
    private static final String PARAM_SUMMARY = "summary";
    private static final String PARAM_TRACKING = "tracking";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_WAS_HERE = "was_here";
    private static final String PARAM_WIFI = "wifi";

    /**
     * Describes an error that occurred while retrieving the current location.
     */
    public enum LocationError {
        /**
         * The location permissions are denied. The SDK requires permissions
         * {@code "android.permission.ACCESS_FINE_LOCATION"} or
         * {@code "android.permission.ACCESS_COARSE_LOCATION"} to retrieve the current location.
         */
        LOCATION_PERMISSION_DENIED,
        /**
         * The location could not be retrieved because location services are not enabled.
         */
        LOCATION_SERVICES_DISABLED,
        /**
         * The location could be retrieved within the allotted amount of time.
         */
        LOCATION_TIMEOUT,
        /**
         * An unknown error occurred.
         */
        UNKNOWN_ERROR,
    }

    /**
     * Callback invoked when a request has been constructed and is ready to be executed.
     * To be used with {@link PlaceManager#newCurrentPlaceRequest(CurrentPlaceRequestParams,
     * OnRequestReadyCallback)} and {@link PlaceManager#newPlaceSearchRequest(
     * PlaceSearchRequestParams, OnRequestReadyCallback)}.
     */
    public interface OnRequestReadyCallback {

        /**
         * Method invoked when the request can't be generated due to an error retrieving the current
         * device location.
         * @param error the error description
         */
        void onLocationError(LocationError error);

        /**
         * Method invoked when the provided {@code GraphRequest} is ready to be executed.
         * Set a callback on it to handle the response using {@code setCallback}, and then
         * execute the request.
         *
         * @param graphRequest the request that's ready to be executed.
         */
        void onRequestReady(GraphRequest graphRequest);
    }

    private PlaceManager() {
        // No public constructor required as all methods are static
    }

    /**
     * Creates a new place search request centered around the current device location.
     * The SDK will retrieve the current device location using
     * {@link android.location.LocationManager}
     * <p>
     * With the Places Graph, you can search for millions of places worldwide and retrieve
     * information like number of checkins, ratings, and addresses all with one request.
     * <p>
     * The specified {@link OnRequestReadyCallback} will be invoked once the request has been
     * generated and is ready to be executed.
     *
     * @param requestParams the request parameters. See {@link PlaceSearchRequestParams}
     * @param callback the {@link OnRequestReadyCallback} invoked when the {@link GraphRequest}
     *                 has been generated and is ready to be executed
     */
    public static void newPlaceSearchRequest(
            final PlaceSearchRequestParams requestParams,
            final OnRequestReadyCallback callback) {

        LocationPackageRequestParams.Builder builder = new LocationPackageRequestParams.Builder();
        builder.setWifiScanEnabled(false);
        builder.setBluetoothScanEnabled(false);

        LocationPackageManager.requestLocationPackage(
                builder.build(),
                new LocationPackageManager.Listener() {
            @Override
            public void onLocationPackage(LocationPackage locationPackage) {
                if (locationPackage.locationError == null) {
                    GraphRequest graphRequest = newPlaceSearchRequestForLocation(
                            requestParams,
                            locationPackage.location);
                    callback.onRequestReady(graphRequest);
                } else {
                    callback.onLocationError(getLocationError(locationPackage.locationError));
                }
            }
        });
    }

    /**
     * Creates a new place search request centered around the specified location.
     * If the location provided is null, the search will be completed globally.
     * At least a location or a search text must be provided.
     * <p>
     * With the Places Graph, you can search for millions of places worldwide and retrieve
     * information like number of checkins, ratings, and addresses all with one request.
     * <p>
     * Returns a new GraphRequest that is configured to perform a place search.
     *
     * @param requestParams the request parameters. See {@link PlaceSearchRequestParams}
     * @param location the {@link Location} around which to search
     * @return a {@link GraphRequest} that is ready to be executed
     * @throws FacebookException thrown if neither {@code location} nor {@code searchText}
     * is specified
     */
    public static GraphRequest newPlaceSearchRequestForLocation(
            PlaceSearchRequestParams requestParams,
            Location location) {

        String searchText = requestParams.getSearchText();
        if (location == null && searchText == null) {
            throw new FacebookException("Either location or searchText must be specified.");
        }
        int limit = requestParams.getLimit();
        Set<String> fields = requestParams.getFields();
        Set<String> categories = requestParams.getCategories();

        Bundle parameters = new Bundle(7);
        parameters.putString(PARAM_TYPE, "place");

        if (location != null) {
            parameters.putString(
                    PARAM_CENTER,
                    String.format(
                            Locale.US,
                            "%f,%f",
                            location.getLatitude(),
                            location.getLongitude()));

            int distance = requestParams.getDistance();
            if (distance > 0) {
                parameters.putInt(PARAM_DISTANCE, distance);
            }
        }
        if (limit > 0) {
            parameters.putInt(PARAM_LIMIT, limit);
        }
        if (!Utility.isNullOrEmpty(searchText)) {
            parameters.putString(PARAM_Q, searchText);
        }
        if (categories != null && !categories.isEmpty()) {
            JSONArray array = new JSONArray();
            for (String category : categories) {
                array.put(category);
            }
            parameters.putString(PARAM_CATEGORIES, array.toString());
        }
        if (fields != null && !fields.isEmpty()) {
            parameters.putString(PARAM_FIELDS, TextUtils.join(",", fields));
        }

        return new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                SEARCH,
                parameters,
                HttpMethod.GET);
    }

    /**
     * Creates a new place info request.
     * <p>
     * The Places Graph exposes a rich set of information about places.
     * If the request is authenticated with a user access token,
     * you can also obtain social information such as the number of friends who have liked and
     * checked into the PlaceFields. The specific friends are also available if they have
     * authenticated the app with the user_tagged_places and user_likes permissions.
     * <p>
     * Returns a new {@link GraphRequest} that is configured to perform a place info request.
     *
     * @param requestParams the request parameters, a {@link PlaceInfoRequestParams#getPlaceId()}
     *                      must be specified.
     * @return a {@link GraphRequest} that is ready to be executed
     * @throws FacebookException thrown if a {@link PlaceInfoRequestParams#getPlaceId()} is not
     * specified.
     */
    public static GraphRequest newPlaceInfoRequest(
            PlaceInfoRequestParams requestParams) {

        String placeId = requestParams.getPlaceId();
        if (placeId == null) {
            throw new FacebookException("placeId must be specified.");
        }

        Bundle parameters = new Bundle(1);

        Set<String> fields = requestParams.getFields();
        if (fields != null && !fields.isEmpty()) {
            parameters.putString(PARAM_FIELDS, TextUtils.join(",", fields));
        }

        return new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                placeId,
                parameters,
                HttpMethod.GET);
    }

    /**
     * Creates a new current place request.
     * <p>
     * The current place request estimates the place where the user is currently located.
     * The response contains a list of places and their associated confidence levels.
     * <p>
     * If a location is not specified in {@link CurrentPlaceRequestParams}, then the SDK
     * retrieves the current location using {@link android.location.LocationManager}.
     *
     * @param requestParams the request parameters. See {@link CurrentPlaceRequestParams}
     * @param callback a {@link OnRequestReadyCallback} that is invoked when the
     * {@link GraphRequest} has been created and is ready to be executed.
     */
    public static void newCurrentPlaceRequest(
            final CurrentPlaceRequestParams requestParams,
            final OnRequestReadyCallback callback) {

        Location location = requestParams.getLocation();
        CurrentPlaceRequestParams.ScanMode scanMode = requestParams.getScanMode();

        LocationPackageRequestParams.Builder builder =
                new LocationPackageRequestParams.Builder();

        // Don't scan for a location if one is provided.
        builder.setLocationScanEnabled(location == null);

        if (scanMode != null && scanMode == CurrentPlaceRequestParams.ScanMode.LOW_LATENCY) {
            // In low-latency mode, avoid active Wi-Fi scanning which can takes
            // several seconds.
            builder.setWifiActiveScanAllowed(false);
        }

        LocationPackageManager.requestLocationPackage(
                builder.build(),
                new LocationPackageManager.Listener() {
                    @Override
                    public void onLocationPackage(LocationPackage locationPackage) {
                        if (locationPackage.locationError != null) {
                            callback.onLocationError(
                                    getLocationError(locationPackage.locationError));
                        } else {
                            Bundle parameters = getCurrentPlaceParameters(
                                    requestParams,
                                    locationPackage);

                            GraphRequest graphRequest = new GraphRequest(
                                    AccessToken.getCurrentAccessToken(),
                                    CURRENT_PLACE_RESULTS,
                                    parameters,
                                    HttpMethod.GET);
                            callback.onRequestReady(graphRequest);
                        }
                    }
                });
    }

    /**
     * Creates a new current place feedback request.
     * <p>
     * This request allows users to provide feedback on the accuracy of the current place
     * estimate. This information is used to improve the accuracy of our results.
     * <p>
     * Returns a new GraphRequest that is configured to perform a current place feedback request.
     *
     * @param requestParams the request parameters. See {@link CurrentPlaceFeedbackRequestParams}
     * @return a {@link GraphRequest} that is ready to be executed
     * @throws FacebookException thrown if parameters
     * {@link CurrentPlaceFeedbackRequestParams#getPlaceId()},
     * {@link CurrentPlaceFeedbackRequestParams#getTracking()}, or
     * {@link CurrentPlaceFeedbackRequestParams#wasHere()} are missing
     */
    public static GraphRequest newCurrentPlaceFeedbackRequest(
            CurrentPlaceFeedbackRequestParams requestParams) {

        String placeId = requestParams.getPlaceId();
        String tracking = requestParams.getTracking();
        Boolean wasHere = requestParams.wasHere();

        if (tracking == null || placeId == null || wasHere == null) {
            throw new FacebookException("tracking, placeId and wasHere must be specified.");
        }

        Bundle parameters = new Bundle(3);
        parameters.putString(PARAM_TRACKING, tracking);
        parameters.putString(PARAM_PLACE_ID, placeId);
        parameters.putBoolean(PARAM_WAS_HERE, wasHere);

        return new GraphRequest(
          AccessToken.getCurrentAccessToken(),
          CURRENT_PLACE_FEEDBACK,
          parameters,
          HttpMethod.POST);
    }

    private static Bundle getCurrentPlaceParameters(
            CurrentPlaceRequestParams request,
            LocationPackage locationPackage) throws FacebookException {

        if (request == null) {
            throw new FacebookException("Request and location must be specified.");
        }
        if (locationPackage == null) {
            locationPackage = new LocationPackage();
        }
        if (locationPackage.location == null) {
            locationPackage.location = request.getLocation();
        }
        if (locationPackage.location == null) {
            throw new FacebookException("A location must be specified");
        }

        try {
            Bundle parameters = new Bundle(6);
            parameters.putString(PARAM_SUMMARY, PARAM_TRACKING);
            int limit = request.getLimit();
            if (limit > 0) {
                parameters.putInt(PARAM_LIMIT, limit);
            }
            Set<String> fields = request.getFields();
            if (fields != null && !fields.isEmpty()) {
                parameters.putString(PARAM_FIELDS, TextUtils.join(",", fields));
            }

            // Coordinates.
            Location location = locationPackage.location;
            JSONObject coordinates = new JSONObject();
            coordinates.put(PARAM_LATITUDE, location.getLatitude());
            coordinates.put(PARAM_LONGITUDE, location.getLongitude());
            if (location.hasAccuracy()) {
                coordinates.put(PARAM_ACCURACY, location.getAccuracy());
            }
            if (location.hasAltitude()) {
                coordinates.put(PARAM_ALTITUDE, location.getAltitude());
            }
            if (location.hasBearing()) {
                coordinates.put(PARAM_HEADING, location.getBearing());
            }
            if (location.hasSpeed()) {
                coordinates.put(PARAM_SPEED, location.getSpeed());
            }
            parameters.putString(PARAM_COORDINATES, coordinates.toString());

            // min confidence level
            ConfidenceLevel minConfidenceLevel = request.getMinConfidenceLevel();
            if (minConfidenceLevel == ConfidenceLevel.LOW
                    || minConfidenceLevel == ConfidenceLevel.MEDIUM
                    || minConfidenceLevel == ConfidenceLevel.HIGH) {
                String minConfidenceLevelString =
                        minConfidenceLevel.toString().toLowerCase(Locale.US);
                parameters.putString(PARAM_MIN_CONFIDENCE_LEVEL, minConfidenceLevelString);
            }

            if (locationPackage != null) {
                // wifi
                JSONObject wifi = new JSONObject();
                wifi.put(PARAM_ENABLED, locationPackage.isWifiScanningEnabled);
                WifiScanResult connectedWifi = locationPackage.connectedWifi;
                if (connectedWifi != null) {
                    wifi.put(PARAM_CURRENT_CONNECTION, getWifiScanJson(connectedWifi));
                }
                List<WifiScanResult> ambientWifi = locationPackage.ambientWifi;
                if (ambientWifi != null) {
                    JSONArray array = new JSONArray();
                    for (WifiScanResult wifiScanResult : ambientWifi) {
                        array.put(getWifiScanJson(wifiScanResult));
                    }
                    wifi.put(PARAM_ACCESS_POINTS, array);
                }
                parameters.putString(PARAM_WIFI, wifi.toString());

                // bluetooth
                JSONObject bluetooth = new JSONObject();
                bluetooth.put(PARAM_ENABLED, locationPackage.isBluetoothScanningEnabled);
                List<BluetoothScanResult> bluetoothScanResults =
                        locationPackage.ambientBluetoothLe;
                if (bluetoothScanResults != null) {
                    JSONArray array = new JSONArray();
                    for (BluetoothScanResult bluetoothScanResult : bluetoothScanResults) {
                        JSONObject bluetoothData = new JSONObject();
                        bluetoothData.put(PARAM_PAYLOAD, bluetoothScanResult.payload);
                        bluetoothData.put(PARAM_RSSI, bluetoothScanResult.rssi);
                        array.put(bluetoothData);
                    }
                    bluetooth.put(PARAM_SCANS, array);
                }
                parameters.putString(PARAM_BLUETOOTH, bluetooth.toString());
            }

            return parameters;
        } catch (JSONException ex) {
            throw new FacebookException(ex);
        }
    }

    private static JSONObject getWifiScanJson(WifiScanResult wifiScanResult) throws JSONException {
        JSONObject wifiData = new JSONObject();
        wifiData.put(PARAM_MAC_ADDRESS, wifiScanResult.bssid);
        wifiData.put(PARAM_SSID, wifiScanResult.ssid);
        wifiData.put(PARAM_SIGNAL_STRENGTH, wifiScanResult.rssi);
        wifiData.put(PARAM_FREQUENCY, wifiScanResult.frequency);
        return wifiData;
    }

    private static LocationError getLocationError(ScannerException.Type type) {
        if (type == ScannerException.Type.PERMISSION_DENIED) {
            return LocationError.LOCATION_PERMISSION_DENIED;
        } else if (type == ScannerException.Type.DISABLED) {
            return LocationError.LOCATION_SERVICES_DISABLED;
        } else if (type == ScannerException.Type.TIMEOUT) {
            return LocationError.LOCATION_TIMEOUT;
        }
        return LocationError.UNKNOWN_ERROR;
    }
}
