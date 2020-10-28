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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import com.example.places.fragments.LoginFragment;
import com.example.places.fragments.PlaceInfoFragment;
import com.example.places.fragments.PlaceSearchFragment;
import com.example.places.model.Place;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;

/**
 * This sample application illustrates how to use the Places Graph SDK to:
 *
 * <ul>
 *   <li>Retrieve a collection of place candidates along with confidence level estimates, that the
 *       user is at those places.
 *   <li>Allow the user to provide feedback on the accuracy of the current place estimate.
 *   <li>Search for nearby places.
 *   <li>Retrieve detailed place information.
 *   <li>Retrieve a cover photo associated with a particular place.
 *   <li>Display places on a map.
 * </ul>
 *
 * <p>{@link PlacesGraphSDKHelper} demonstrates how to create requests with the Places Graph SDK.
 *
 * <p>{@link com.example.places.model.Place} and {@link com.example.places.model.PlaceTextUtils}
 * demonstrates how to parse place information.
 */
public class MainActivity extends AppCompatActivity
    implements LoginFragment.Listener, PlaceSearchFragment.Listener, PlaceInfoFragment.Listener {

  private static final int REQUEST_LOCATION = 0;
  private static final int REQUEST_CALL_PHONE = 1;

  /**
   * The Places Graph SDK can be accessed using either a User Access Token or a Client Token. A User
   * Access Token will require users to login to Facebook, but a Client Token won't.
   *
   * <p>This sample app demonstrates how to use both User Access Tokens and Client Tokens. To use
   * either one, just change the value of {@code authenticationType} to USER_TOKEN or CLIENT_TOKEN.
   */
  private enum AuthenticationType {
    USER_TOKEN,
    CLIENT_TOKEN,
  };

  /**
   * This variable specifies whether the sample app uses a User Access Token or a Client Token.
   *
   * <p>Change it to AuthenticationType.CLIENT_TOKEN to use a Client Token.
   *
   * <p>To use a client token, you will also need to: update "app_id" in strings.xml to the
   * application ID of your app, and then update the CLIENT_TOKEN constant in this class to the
   * value of your app client token. You can find the client token value on the Developer Portal
   * page of your app, under the Advanced Settings section.
   */
  private final AuthenticationType authenticationType = AuthenticationType.USER_TOKEN;

  /**
   * To use client token authentication, get your app client token from the developer portal, and
   * enter it below. The client token is available under your application page in the Developer
   * Portal, under the Advanced Settings section. Note that you must also change the app_id to your
   * own Application ID in the file "strings.xml".
   */
  private static final String CLIENT_TOKEN = "";

  private Intent callPhoneIntent;

  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    setContentView(R.layout.main_activity);

    /**
     * You can use the Places Graph SDK with either a User Access Token, or a Client Token. By
     * default, this code sample is configured to use a User Access Token.
     */
    if (authenticationType == AuthenticationType.USER_TOKEN) {
      if (!AccessToken.isCurrentAccessTokenActive()) {
        /**
         * When a User Access Token is used, and if the token is not present, then prompt the user
         * to log into Facebook.
         */
        displayLoginFragment();
      } else {
        displayPlaceListFragment();
      }
    } else {
      /**
       * When a Client Token is used, set the client token to the Facebook SDK class as illustrated
       * below. Users do not need to log into Facebook. PlaceManager requests can be placed once the
       * client token has been set.
       */
      FacebookSdk.setClientToken(CLIENT_TOKEN);
      displayPlaceListFragment();
    }
  }

  public void onStart() {
    super.onStart();
    if (!hasLocationPermission()) {
      requestLocationPermission();
    }
  }

  private void requestLocationPermission() {
    /*
     * Prompts the user to grant location permissions. This sample app uses the
     * device's' location to get the current place from the Place Graph SDK,
     * and to perform local place searches.
     */
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.ACCESS_FINE_LOCATION)) {

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.permission_prompt_location);
      builder.setPositiveButton(
          R.string.ok,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ActivityCompat.requestPermissions(
                  MainActivity.this,
                  new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                  },
                  REQUEST_LOCATION);
            }
          });
      builder.create().show();
    } else {
      ActivityCompat.requestPermissions(
          this,
          new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
          },
          REQUEST_LOCATION);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_CALL_PHONE) {
      try {
        if (callPhoneIntent != null) {
          startActivity(callPhoneIntent);
          callPhoneIntent = null;
        }
      } catch (SecurityException e) {
        callPhoneIntent = null;
        // ignore
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void onLoginComplete() {
    // Event invoked by the LoginFragment when login completes.
    displayPlaceListFragment();
  }

  @Override
  public void onPlaceSelected(Place place) {
    // Event invoked by the PlaceSearchFragment when a place is selected.
    displayPlaceInfoFragment(place);
  }

  @Override
  public void onLocationPermissionsError() {
    // Event invoked by the PlaceSearchFragment when the Places Graph SDK fails to retrieve
    // the current device location due to missing location permissions.
    requestLocationPermission();
  }

  private void displayPlaceListFragment() {
    PlaceSearchFragment placeListFragment = PlaceSearchFragment.newInstance();
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_placeholder, placeListFragment);
    transaction.commit();
  }

  private void displayLoginFragment() {
    LoginFragment loginFragment = LoginFragment.newInstance();
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_placeholder, loginFragment);
    transaction.commit();
  }

  private void displayPlaceInfoFragment(Place place) {
    PlaceInfoFragment placeInfoFragment = PlaceInfoFragment.newInstance();
    Bundle bundle = new Bundle();
    bundle.putParcelable(PlaceInfoFragment.EXTRA_PLACE, place);
    placeInfoFragment.setArguments(bundle);
    getSupportFragmentManager()
        .beginTransaction()
        .add(R.id.fragment_placeholder, placeInfoFragment, "details")
        .addToBackStack(place.get(Place.NAME))
        .commit();
  }

  @Override
  public void onCallPhone(final Intent intent) {
    /*
     * Prompts the user for permission to place a phone call, and then places the call.
     */
    if (hasPermission(Manifest.permission.CALL_PHONE)) {
      startActivity(intent);
    } else if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.CALL_PHONE)) {

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.permission_prompt_call_phone);
      builder.setPositiveButton(
          R.string.ok,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              callPhoneIntent = intent;
              ActivityCompat.requestPermissions(
                  MainActivity.this,
                  new String[] {Manifest.permission.CALL_PHONE},
                  REQUEST_CALL_PHONE);
            }
          });
      builder.create().show();
    } else {
      callPhoneIntent = intent;
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
    }
  }

  private boolean hasPermission(String permission) {
    return ActivityCompat.checkSelfPermission(this, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public boolean hasLocationPermission() {
    return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }
}
