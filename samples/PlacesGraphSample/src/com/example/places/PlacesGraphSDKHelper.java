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

package com.example.places;

import android.util.Log;
import androidx.annotation.Nullable;
import com.example.places.model.CurrentPlaceResult;
import com.example.places.model.Place;
import com.example.places.utils.BitmapDownloadTask;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.places.PlaceManager;
import com.facebook.places.model.CurrentPlaceFeedbackRequestParams;
import com.facebook.places.model.CurrentPlaceRequestParams;
import com.facebook.places.model.PlaceFields;
import com.facebook.places.model.PlaceInfoRequestParams;
import com.facebook.places.model.PlaceSearchRequestParams;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class illustrates how to use the Places Graph SDK to:
 *
 * <ul>
 *   <li>Get the current place.
 *   <li>Provide feedback on the current place response.
 *   <li>Search places.
 *   <li>Fetch detailed place info.
 * </ul>
 *
 * This class also illustrates how to parse the responses of the Places Graph SDK.
 */
public class PlacesGraphSDKHelper {

  private static final String TAG = "PlacesGraphSDKHelper";
  private static final String PARAM_DATA = "data";

  /** These are the place fields that are fetched in the place info request. */
  private static final String[] PLACE_INFO_REQUEST_FIELDS =
      new String[] {
        Place.ID,
        Place.NAME,
        Place.DESCRIPTION,
        Place.CHECKINS,
        Place.ABOUT,
        Place.ENGAGEMENT,
        Place.HOURS,
        Place.COVER,
        Place.IS_ALWAYS_OPEN,
        Place.IS_PERMANENTLY_CLOSED,
        Place.IS_VERIFIED,
        Place.LINK,
        Place.APP_LINKS,
        Place.OVERALL_STAR_RATING,
        Place.PARKING,
        Place.RESTAURANT_SPECIALTIES,
        Place.WEBSITE,
        Place.LOCATION,
        Place.PHONE,
        Place.PRICE_RANGE,
        Place.RATING_COUNT,
        Place.CATEGORY_LIST,
        Place.RESTAURANT_SERVICES,
        Place.PAYMENT_OPTIONS,
        Place.SINGLE_LINE_ADDRESS,
      };

  /** Defines the listener invoked when the current place response is received. */
  public interface CurrentPlaceRequestListener {

    /**
     * Invoked when the current place response is received.
     *
     * @param result This object contains the list of places, and the tracking ID parsed from the
     *     current place response; or it contains null if the request fails.
     * @param response The Places Graph response.
     */
    void onCurrentPlaceResult(@Nullable CurrentPlaceResult result, GraphResponse response);

    /**
     * Invoked when the Places Graph SDK fails to retrieve the current location.
     *
     * @param error Contains a message that explains why retrieving the location failed.
     */
    void onLocationError(PlaceManager.LocationError error);
  }

  /** Defines the listener invoked when the place search response is received. */
  public interface PlaceSearchRequestListener {

    /**
     * Invoked when the place search response has been received.
     *
     * @param places The list of places parsed from the response, or contains null if the request
     *     fails.
     * @param response The Places Graph response.
     */
    void onPlaceSearchResult(@Nullable List<Place> places, GraphResponse response);

    /**
     * Invoked when the Places Graph SDK fails to retrieve the current location.
     *
     * @param error Contains the reason why retrieving the location failed.
     */
    void onLocationError(PlaceManager.LocationError error);
  }

  /** Defines the listener invoked when the place info response is received. */
  public interface PlaceInfoRequestListener {

    /**
     * Invoked when the place info response is received.
     *
     * @param place The place instance parsed from the response, or contains null if the request
     *     fails.
     * @param response The Places Graph response.
     */
    void onPlaceInfoResult(@Nullable Place place, GraphResponse response);
  }

  /**
   * Creates and executes a place search request.
   *
   * @param searchQuery The text search term. This can be null to let the Places Graph SDK return
   *     nearby places.
   * @param listener Invoked when the place search response is received, and when the place search
   *     request fails.
   */
  public static void searchPlace(String searchQuery, PlaceSearchRequestListener listener) {

    // Creates the place search request builder.
    PlaceSearchRequestParams.Builder builder = new PlaceSearchRequestParams.Builder();
    // Specifies the text search query. This field is optional. If you don't specify it,
    // then places of importance within the maximum distance radius will be returned.
    builder.setSearchText(searchQuery);
    // The maximum search radius in meters.
    builder.setDistance(1000);
    // The maximum number of places to return.
    builder.setLimit(10);
    // The place fields to be returned.
    builder.addField(Place.NAME);
    builder.addField(Place.LOCATION);
    builder.addField(Place.PHONE);

    // Creates the callback invoked when the request is ready.
    OnPlaceSearchRequestReadyCallback onRequestReadyCallback =
        new OnPlaceSearchRequestReadyCallback(listener);

    // The Places Graph SDK starts retrieving the current device location, and constructs
    // the GraphRequest. The callback is invoked once the request is ready.
    PlaceManager.newPlaceSearchRequest(builder.build(), onRequestReadyCallback);
  }

  private static class OnPlaceSearchRequestReadyCallback
      implements PlaceManager.OnRequestReadyCallback, GraphRequest.Callback {

    private final WeakReference<PlaceSearchRequestListener> listenerRef;

    OnPlaceSearchRequestReadyCallback(PlaceSearchRequestListener listener) {
      listenerRef = new WeakReference<>(listener);
    }

    @Override
    public void onLocationError(PlaceManager.LocationError error) {
      // Invoked if the Places Graph SDK fails to retrieve the device location.
      PlaceSearchRequestListener listener = listenerRef.get();
      if (listener == null) {
        Log.d(TAG, "listener is null!");
      } else {
        listener.onLocationError(error);
      }
    }

    @Override
    public void onRequestReady(GraphRequest graphRequest) {

      // The place search request is ready to be executed.
      // The request can be customized here if needed.

      // Sets the callback, and executes the request.
      graphRequest.setCallback(this);
      graphRequest.executeAsync();
    }

    @Override
    public void onCompleted(GraphResponse response) {
      // Event invoked when the place search response is received.

      List<Place> places = null;

      FacebookRequestError error = response.getError();
      if (error == null) {
        // Parses the place search response.
        try {
          JSONObject jsonObject = response.getJSONObject();
          if (jsonObject != null) {
            JSONArray jsonArray = jsonObject.optJSONArray(PARAM_DATA);
            if (jsonArray != null) {
              int length = jsonArray.length();
              places = new ArrayList<>(length);
              for (int i = 0; i < length; i++) {
                places.add(new Place(jsonArray.getJSONObject(i)));
              }
            }
          }
        } catch (JSONException e) {
          Log.e(TAG, "failed to parse place the place search response", e);
        }
      } else {
        // The error object contains more information on the error.
        Log.d(TAG, "response error: " + error);
      }

      PlaceSearchRequestListener listener = listenerRef.get();
      if (listener == null) {
        Log.d(TAG, "no listener!");
      } else {
        listener.onPlaceSearchResult(places, response);
      }
    }
  }

  /**
   * Creates and executes a current place request. This request is used to determine the place where
   * the user is currently located.
   *
   * @param listener Listener invoked when the current place response is received.
   */
  public static void getCurrentPlace(CurrentPlaceRequestListener listener) {

    // Creates the current place request builder.
    CurrentPlaceRequestParams.Builder builder = new CurrentPlaceRequestParams.Builder();

    // Specifies the minimum confidence level of the results.
    builder.setMinConfidenceLevel(CurrentPlaceRequestParams.ConfidenceLevel.LOW);
    // Specifies the maximum number of places returned.
    builder.setLimit(10);
    // Specifies the place fields returned.
    builder.addField(PlaceFields.NAME);
    builder.addField(PlaceFields.CONFIDENCE_LEVEL);
    builder.addField(PlaceFields.LOCATION);
    builder.addField(PlaceFields.PHONE);

    OnCurrentPlaceRequestReadyCallback callback = new OnCurrentPlaceRequestReadyCallback(listener);

    PlaceManager.newCurrentPlaceRequest(builder.build(), callback);
  }

  private static class OnCurrentPlaceRequestReadyCallback
      implements PlaceManager.OnRequestReadyCallback, GraphRequest.Callback {

    private final WeakReference<CurrentPlaceRequestListener> listenerRef;

    private OnCurrentPlaceRequestReadyCallback(CurrentPlaceRequestListener listener) {
      listenerRef = new WeakReference<>(listener);
    }

    @Override
    public void onRequestReady(GraphRequest graphRequest) {

      // The current place request is ready. The request can be customized here.
      // Define the callback that will handle and parse the response,
      // and then execute the request.
      graphRequest.setCallback(this);
      graphRequest.executeAsync();
    }

    @Override
    public void onCompleted(GraphResponse response) {

      FacebookRequestError error = response.getError();

      // Parses the current place response. CurrentPlaceResult is used in
      // the sample app to illustrate how to parse the list of places, and
      // how to parse the tracking ID from the current place response.

      CurrentPlaceResult result = null;
      if (error == null) {
        try {
          JSONObject jsonObject = response.getJSONObject();
          if (jsonObject != null) {
            result = CurrentPlaceResult.fromJson(jsonObject);
          }
        } catch (JSONException e) {
          Log.e(TAG, "error while parsing current place response", e);
        }
      } else {
        Log.d(TAG, "response error: " + error);
      }

      CurrentPlaceRequestListener listener = listenerRef.get();
      if (listener == null) {
        Log.d(TAG, "no listener");
      } else {
        listener.onCurrentPlaceResult(result, response);
      }
    }

    @Override
    public void onLocationError(PlaceManager.LocationError error) {
      CurrentPlaceRequestListener listener = listenerRef.get();
      if (listener == null) {
        Log.d(TAG, "listener is null!");
      } else {
        listener.onLocationError(error);
      }
    }
  }

  /**
   * Creates and executes a current place feedback request. This request is used to provide feedback
   * on the accuracy of the current place response.
   *
   * @param currentPlaceResult The result of the current place request.
   * @param place The place where the user is (or isn't') located.
   * @param wasHere true to indicate that the user is located at the place, and false to indicate
   *     that the user is not located at the place.
   */
  public static void provideCurrentPlaceFeedback(
      final CurrentPlaceResult currentPlaceResult, final Place place, final boolean wasHere) {

    // Creates the builder of the current place feedback request.
    CurrentPlaceFeedbackRequestParams.Builder builder =
        new CurrentPlaceFeedbackRequestParams.Builder();

    /**
     * Sets the tracking ID, which is used as a correlator to the current place response. The
     * tracking ID is retrieved from the current place response.
     */
    builder.setTracking(currentPlaceResult.getTracking());

    // The place at which the user is (or is not) located.
    String placeId = place.get(Place.ID);
    builder.setPlaceId(placeId);

    // Indicates if the user is or is not at the place.
    builder.setWasHere(wasHere);

    GraphRequest request = PlaceManager.newCurrentPlaceFeedbackRequest(builder.build());

    GraphRequest.Callback callback =
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            boolean success = false;
            FacebookRequestError error = response.getError();
            if (error == null) {
              JSONObject jsonObject = response.getJSONObject();
              success = jsonObject.optBoolean("success");
            } else {
              Log.d(TAG, "response error: " + error);
            }
            Log.d(TAG, "provideCurrentPlaceFeedback: onCompleted: response: success=" + success);
          }
        };
    request.setCallback(callback);

    request.executeAsync();
  }

  /**
   * Creates and executes a Place Info request on the Places Graph SDK.
   *
   * @param place Specifies the place for retrieving additional fields.
   * @param listener The listener invoked when the place details have been fetched.
   */
  public static void fetchPlaceInfo(Place place, PlaceInfoRequestListener listener) {

    // Creates the builder for the place info request.
    PlaceInfoRequestParams.Builder builder = new PlaceInfoRequestParams.Builder();

    // Specifies the ID of the place to fetch information about.
    String placeId = place.get(Place.ID);
    builder.setPlaceId(placeId);
    /**
     * Specifies the fields to be fetched. Note the fields are defined in {@link PlaceFields} as
     * String constants. Refer to the Places Graph online documentation to see the complete list of
     * fields.
     */
    builder.addFields(PLACE_INFO_REQUEST_FIELDS);

    // Create the place information request.
    final GraphRequest request = PlaceManager.newPlaceInfoRequest(builder.build());

    // Define the callback that will parse the response.
    request.setCallback(new OnPlaceInfoResponseCallback(listener));
    request.executeAsync();
  }

  /** Callback used to parse the place information response. */
  private static class OnPlaceInfoResponseCallback implements GraphRequest.Callback {

    private final WeakReference<PlaceInfoRequestListener> listenerRef;

    private OnPlaceInfoResponseCallback(PlaceInfoRequestListener listener) {
      listenerRef = new WeakReference<>(listener);
    }

    @Override
    public void onCompleted(GraphResponse response) {

      Place place = null;

      // The place object is used in the sample app to parse the JSON response
      // from the Places Graph.
      FacebookRequestError error = response.getError();
      if (error == null) {
        try {
          JSONObject jsonObject = response.getJSONObject();
          if (jsonObject != null) {
            place = new Place(jsonObject);
          }
        } catch (Exception e) {
          Log.e(TAG, "failed to parse place info", e);
        }
      } else {
        Log.d(TAG, "response error: " + error);
      }

      PlaceInfoRequestListener listener = listenerRef.get();
      if (listener == null) {
        Log.d(TAG, "no listener");
      } else {
        listener.onPlaceInfoResult(place, response);
      }
    }
  }

  /**
   * Illustrates how to download the cover photo of a given place.
   *
   * @param place Specifies the place associated with the cover photo. The place must have a COVER
   *     field. See {@link PlaceFields}.
   * @param listener The listener invoked when the cover photo is downloaded.
   * @return true if the download is initiated. false if the place does not have a COVER field.
   */
  public static boolean downloadPlaceCoverPhoto(Place place, BitmapDownloadTask.Listener listener) {
    /**
     * In your applications, use Fresco to download, load, cache, and display images.
     * https://github.com/facebook/fresco
     */
    String coverPhotoUrl = place.getCoverPhotoUrl();
    if (coverPhotoUrl == null) {
      return false;
    }
    BitmapDownloadTask bitmapDownloadTask = new BitmapDownloadTask(coverPhotoUrl, listener);
    FacebookSdk.getExecutor().execute(bitmapDownloadTask);
    return true;
  }
}
