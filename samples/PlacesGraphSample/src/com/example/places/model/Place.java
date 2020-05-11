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

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.facebook.places.model.CurrentPlaceRequestParams;
import com.facebook.places.model.PlaceFields;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class Place implements PlaceFields, Parcelable {

  private static final String TAG = Place.class.getSimpleName();

  private JSONObject jsonData;

  public Place(JSONObject jsonData) {
    this.jsonData = jsonData;
  }

  public JSONObject getJson() {
    return jsonData;
  }

  public String get(String field) {
    return jsonData.optString(field);
  }

  public JSONObject getJson(String field) {
    return jsonData.optJSONObject(field);
  }

  public JSONArray getJsonArray(String field) {
    return jsonData.optJSONArray(field);
  }

  public int getInt(String field) {
    return jsonData.optInt(field);
  }

  public boolean getBoolean(String field) {
    return jsonData.optBoolean(field);
  }

  public boolean has(String field) {
    return jsonData.has(field);
  }

  public LatLng getPosition() {
    JSONObject location = jsonData.optJSONObject(LOCATION);
    if (location != null) {
      if (location.has("latitude") && location.has("longitude")) {
        double latitude = location.optDouble("latitude");
        double longitude = location.optDouble("longitude");
        return new LatLng(latitude, longitude);
      }
    }
    return null;
  }

  public CurrentPlaceRequestParams.ConfidenceLevel getConfidenceLevel() {
    if (jsonData.has(CONFIDENCE_LEVEL)) {
      String confidenceLevel = jsonData.optString(CONFIDENCE_LEVEL);
      if ("high".equalsIgnoreCase(confidenceLevel)) {
        return CurrentPlaceRequestParams.ConfidenceLevel.HIGH;
      } else if ("medium".equalsIgnoreCase(confidenceLevel)) {
        return CurrentPlaceRequestParams.ConfidenceLevel.MEDIUM;
      } else if ("low".equalsIgnoreCase(confidenceLevel)) {
        return CurrentPlaceRequestParams.ConfidenceLevel.LOW;
      }
    }
    return null;
  }

  public String getCoverPhotoUrl() {
    JSONObject coverPhotoJson = jsonData.optJSONObject("cover");
    if (coverPhotoJson != null) {
      return coverPhotoJson.optString("source");
    }
    return null;
  }

  public Intent getAppLinkIntent(String appName) {
    List<AppLink> appLinks = getAppLinks();
    if (appLinks != null) {
      for (AppLink appLink : appLinks) {
        if (appName.equals(appLink.getAppName())) {
          return appLink.getIntent();
        }
      }
    }
    return null;
  }

  public List<AppLink> getAppLinks() {
    List<AppLink> appLinks = new ArrayList<>();
    JSONObject appLinkJson = jsonData.optJSONObject(APP_LINKS);
    if (appLinkJson != null) {
      JSONArray appArray = appLinkJson.optJSONArray("android");
      if (appArray != null) {
        int length = appArray.length();
        for (int i = 0; i < length; i++) {
          JSONObject linkJson = appArray.optJSONObject(i);
          if (linkJson != null) {
            String appName = linkJson.optString("app_name");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String packageName = linkJson.optString("package");
            String className = linkJson.optString("class");
            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
              intent.setClassName(packageName, className);
            }
            String url = linkJson.optString("url");
            if (url != null) {
              intent.setData(Uri.parse(url));
            }
            appLinks.add(new AppLink(appName, intent));
          }
        }
      }
    }
    return appLinks;
  }

  public OpeningHours getOpeningHours() {
    return OpeningHours.parse(this);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(jsonData.toString());
  }

  public static final Parcelable.Creator<Place> CREATOR =
      new Parcelable.Creator<Place>() {

        public Place createFromParcel(Parcel in) {
          try {
            String json = in.readString();
            return new Place(new JSONObject(json));
          } catch (Exception e) {
            Log.e(TAG, "Failed to parse place", e);
          }
          return null;
        }

        public Place[] newArray(int size) {
          return new Place[size];
        }
      };
}
