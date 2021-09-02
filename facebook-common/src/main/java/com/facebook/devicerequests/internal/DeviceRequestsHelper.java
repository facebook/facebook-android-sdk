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

package com.facebook.devicerequests.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import androidx.annotation.Nullable;
import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.SmartLoginOption;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * com.facebook.devicerequests.internal is solely for the use of other packages within the Facebook
 * SDK for Android. Use of any of the classes in this package is unsupported, and they may be
 * modified or removed without warning at any time.
 */
@AutoHandleExceptions
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class DeviceRequestsHelper {

  private static final String TAG = DeviceRequestsHelper.class.getCanonicalName();
  public static final String DEVICE_INFO_PARAM = "device_info";
  public static final String DEVICE_TARGET_USER_ID = "target_user_id";

  static final String DEVICE_INFO_DEVICE = "device";
  static final String DEVICE_INFO_MODEL = "model";

  static final String SDK_HEADER = "fbsdk";
  static final String SDK_FLAVOR = "android";

  static final String SERVICE_TYPE = "_fb._tcp.";

  private static HashMap<String, NsdManager.RegistrationListener> deviceRequestsListeners =
      new HashMap<>();

  public static String getDeviceInfo(@Nullable Map<String, String> deviceInfo) {
    // Device info
    // We don't need all the information in Utility.setAppEventExtendedDeviceInfoParameters
    // We only want the model so we can show it to the user, so they know which device
    // the login request comes from
    if (deviceInfo == null) {
      deviceInfo = new HashMap<>();
    }
    deviceInfo.put(DEVICE_INFO_DEVICE, Build.DEVICE);
    deviceInfo.put(DEVICE_INFO_MODEL, Build.MODEL);
    return new JSONObject(deviceInfo).toString();
  }

  public static String getDeviceInfo() {
    return getDeviceInfo(null);
  }

  public static boolean startAdvertisementService(String userCode) {
    if (isAvailable()) {
      return startAdvertisementServiceImpl(userCode);
    }

    return false;
  }

  /*
  returns true if smart login is enabled and the android api level is supported.
   */
  public static boolean isAvailable() {
    FetchedAppSettings settings =
        FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId());
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        && settings != null
        && settings.getSmartLoginOptions().contains(SmartLoginOption.Enabled);
  }

  public static Bitmap generateQRCode(final String url) {
    Bitmap qrCode = null;
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, 2);
    try {
      BitMatrix matrix =
          new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 200, 200, hints);

      int h = matrix.getHeight();
      int w = matrix.getWidth();
      int[] pixels = new int[h * w];

      for (int i = 0; i < h; i++) {
        int offset = i * w;
        for (int j = 0; j < w; j++) {
          pixels[offset + j] = matrix.get(j, i) ? Color.BLACK : Color.WHITE;
        }
      }

      qrCode = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      qrCode.setPixels(pixels, 0, w, 0, 0, w, h);

    } catch (WriterException ignored) {
      // ignored because exception would be thrown from ZXing library.
    }

    return qrCode;
  }

  public static void cleanUpAdvertisementService(String userCode) {
    cleanUpAdvertisementServiceImpl(userCode);
  }

  @TargetApi(16)
  private static boolean startAdvertisementServiceImpl(final String userCode) {
    if (deviceRequestsListeners.containsKey(userCode)) {
      return true;
    }

    // Dots in the version will mess up the Bonjour DNS record parsing
    String sdkVersion = FacebookSdk.getSdkVersion().replace('.', '|');
    // Other SDKs that adopt this feature should use different flavor name
    // The whole name should not exceed 60 characters
    final String nsdServiceName =
        String.format(
            "%s_%s_%s",
            // static identifier
            SDK_HEADER,
            // sdk type and version
            // client app parses the string based on this version
            String.format("%s-%s", SDK_FLAVOR, sdkVersion),

            // Additional fields should be added here

            // short code for the login flow
            userCode);

    NsdServiceInfo nsdLoginAdvertisementService = new NsdServiceInfo();
    nsdLoginAdvertisementService.setServiceType(SERVICE_TYPE);
    nsdLoginAdvertisementService.setServiceName(nsdServiceName);
    nsdLoginAdvertisementService.setPort(80);

    NsdManager nsdManager =
        (NsdManager) FacebookSdk.getApplicationContext().getSystemService(Context.NSD_SERVICE);

    NsdManager.RegistrationListener nsdRegistrationListener =
        new NsdManager.RegistrationListener() {
          @Override
          public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            // Android may have changed the service name in order to resolve a conflict
            if (!nsdServiceName.equals(NsdServiceInfo.getServiceName())) {
              cleanUpAdvertisementService(userCode);
            }
          }

          @Override
          public void onServiceUnregistered(NsdServiceInfo serviceInfo) {}

          @Override
          public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            cleanUpAdvertisementService(userCode);
          }

          @Override
          public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {}
        };

    deviceRequestsListeners.put(userCode, nsdRegistrationListener);

    nsdManager.registerService(
        nsdLoginAdvertisementService, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener);

    return true;
  }

  @TargetApi(16)
  private static void cleanUpAdvertisementServiceImpl(String userCode) {
    NsdManager.RegistrationListener nsdRegistrationListener = deviceRequestsListeners.get(userCode);
    if (nsdRegistrationListener != null) {
      NsdManager nsdManager =
          (NsdManager) FacebookSdk.getApplicationContext().getSystemService(Context.NSD_SERVICE);

      try {
        nsdManager.unregisterService(nsdRegistrationListener);
      } catch (IllegalArgumentException e) {
        Utility.logd(TAG, e);
      }

      deviceRequestsListeners.remove(userCode);
    }
  }
}
