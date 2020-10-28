/*
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

package com.example.places.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CurrentPlaceResult {

  private static final String PARAM_DATA = "data";
  private static final String PARAM_SUMMARY = "summary";
  private static final String PARAM_TRACKING = "tracking";

  private List<Place> places;
  private String tracking;

  public List<Place> getPlaces() {
    return places;
  }

  public String getTracking() {
    return tracking;
  }

  public static CurrentPlaceResult fromJson(JSONObject json) throws JSONException {

    CurrentPlaceResult response = new CurrentPlaceResult();

    if (json.has(PARAM_DATA)) {
      JSONArray array = json.getJSONArray(PARAM_DATA);
      int length = array.length();
      response.places = new ArrayList<>(length);
      for (int i = 0; i < length; i++) {
        JSONObject placeJson = array.getJSONObject(i);
        response.places.add(new Place(placeJson));
      }
    }
    if (json.has(PARAM_SUMMARY)) {
      JSONObject summaryJson = json.getJSONObject(PARAM_SUMMARY);
      response.tracking = summaryJson.getString(PARAM_TRACKING);
    }

    return response;
  }
}
