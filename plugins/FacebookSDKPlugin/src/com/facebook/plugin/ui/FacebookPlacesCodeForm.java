/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.ui;

import javax.swing.*;

public class FacebookPlacesCodeForm extends CodeFormContainer {

    private JPanel mainPanel;
    private JPanel placeSearchCode;
    private JPanel handleResponseCode;
    private JPanel providedLocationCode;

    FacebookPlacesCodeForm() {
        setCodeForm("\n" +
                "PlaceSearchRequestParams.Builder builder = \n" +
                "      new PlaceSearchRequestParams.Builder();\n" +
                "\n" +
                "builder.setSearchText(\"Cafe\");\n" +
                "builder.setDistance(1000); // 1,000 m. max distance.\n" +
                "builder.setLimit(10);\n" +
                "builder.addField(PlaceFields.NAME);\n" +
                "builder.addField(PlaceFields.LOCATION);\n" +
                "builder.addField(PlaceFields.PHONE);\n" +
                "\n" +
                "PlaceSearchRequestCallback callback = new PlaceSearchRequestCallback();\n" +
                "\n" +
                "// The SDK will automatically retrieve the device location and invoke \n" +
                "// the OnRequestReadyCallback when the request is ready to be executed.\n" +
                "PlaceManager.newPlaceSearchRequest(builder.build(), callback);", placeSearchCode);

        setCodeForm("\n" +
                "private class PlaceSearchRequestCallback\n" +
                "    implements PlaceManager.OnRequestReadyCallback, GraphRequest.Callback {\n" +
                "\n" +
                "    @Override\n" +
                "    public void onRequestReady(GraphRequest graphRequest) {\n" +
                "        // The place search request is ready to be executed.\n" +
                "        // The request can be customized here if needed.\n" +
                "\n" +
                "        // Sets the callback and executes the request.\n" +
                "        graphRequest.setCallback(this);\n" +
                "        graphRequest.executeAsync();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void onCompleted(GraphResponse response) {\n" +
                "        // This event is invoked when the place search response is received.\n" +
                "        // Parse the places from the response object.\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void onLocationError(PlaceManager.LocationError error) {\n" +
                "        // Invoked if the Places Graph SDK failed to retrieve\n" +
                "        // the device location.\n" +
                "    }\n" +
                "}", handleResponseCode);

        setCodeForm("\n" +
                "PlaceSearchRequestParams.Builder builder = \n" +
                "      new PlaceSearchRequestParams.Builder();\n" +
                "\n" +
                "builder.setSearchText(\"Cafe\");\n" +
                "builder.setDistance(1000); // 1,000 meter maximum distance.\n" +
                "builder.setLimit(10);\n" +
                "builder.addField(PlaceFields.NAME);\n" +
                "builder.addField(PlaceFields.LOCATION);\n" +
                "builder.addField(PlaceFields.PHONE);\n" +
                "\n" +
                "// Get the current location from LocationManager or FusedLocationProviderApi\n" +
                "Location location = getCurrentLocation();\n" +
                "\n" +
                "GraphRequest request =\n" +
                "    PlaceManager.newPlaceSearchRequestForLocation(builder.build(), location);\n" +
                "\n" +
                "request.setCallback(new GraphRequest.Callback() {\n" +
                "    @Override\n" +
                "    public void onCompleted(GraphResponse response) {\n" +
                "        // Handle the response.\n" +
                "    }\n" +
                "});\n" +
                "      \n" +
                "request.executeAsync();", providedLocationCode);
    }

    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }
}
