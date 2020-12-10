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

package com.facebook.appevents.ondeviceprocessing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.internal.FacebookSignatureValidator;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.ppml.receiver.IReceiverService;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteServiceWrapper {

  private static final String TAG = RemoteServiceWrapper.class.getSimpleName();

  static final String RECEIVER_SERVICE_ACTION = "ReceiverService";
  static final String RECEIVER_SERVICE_PACKAGE = "com.facebook.katana";
  static final String RECEIVER_SERVICE_PACKAGE_WAKIZASHI = "com.facebook.wakizashi";

  private static Boolean isServiceAvailable;

  /**
   * Synchronously sends install event to the remote service. <b>Do not call from the UI thread.</b>
   *
   * @returns {@link ServiceResult#OPERATION_SUCCESS} if events were sent successfully. Other {@link
   *     ServiceResult}s indicate failure and are not recoverable.
   */
  public static ServiceResult sendInstallEvent(String applicationId) {
    return sendEvents(EventType.MOBILE_APP_INSTALL, applicationId, new LinkedList<AppEvent>());
  }

  /**
   * Synchronously sends custom app events to the remote service. <b>Do not call from the UI
   * thread.</b>
   *
   * @returns {@link ServiceResult#OPERATION_SUCCESS} if events were sent successfully. Other {@link
   *     ServiceResult}s indicate failure and are not recoverable.
   */
  public static ServiceResult sendCustomEvents(String applicationId, List<AppEvent> appEvents) {
    return sendEvents(EventType.CUSTOM_APP_EVENTS, applicationId, appEvents);
  }

  public static boolean isServiceAvailable() {
    if (isServiceAvailable == null) {
      Context context = FacebookSdk.getApplicationContext();
      isServiceAvailable = getVerifiedServiceIntent(context) != null;
    }
    return isServiceAvailable;
  }

  private static ServiceResult sendEvents(
      EventType eventType, String applicationId, List<AppEvent> appEvents) {
    ServiceResult serviceResult = ServiceResult.SERVICE_NOT_AVAILABLE;

    AppEventUtility.assertIsNotMainThread();

    Context context = FacebookSdk.getApplicationContext();
    Intent verifiedIntent = getVerifiedServiceIntent(context);

    if (verifiedIntent != null) {
      RemoteServiceConnection connection = new RemoteServiceConnection();
      if (context.bindService(verifiedIntent, connection, Context.BIND_AUTO_CREATE)) {
        try {
          IBinder binder = connection.getBinder();
          if (binder != null) {
            IReceiverService service = IReceiverService.Stub.asInterface(binder);
            Bundle eventBundle =
                RemoteServiceParametersHelper.buildEventsBundle(
                    eventType, applicationId, appEvents);

            if (eventBundle != null) {
              service.sendEvents(eventBundle);
              Utility.logd(TAG, "Successfully sent events to the remote service: " + eventBundle);
            }
            serviceResult = ServiceResult.OPERATION_SUCCESS;
          } else {
            serviceResult = ServiceResult.SERVICE_NOT_AVAILABLE;
          }
        } catch (InterruptedException | RemoteException exception) {
          serviceResult = ServiceResult.SERVICE_ERROR;
          Utility.logd(TAG, exception);
        } finally {
          context.unbindService(connection);
          Utility.logd(TAG, "Unbound from the remote service");
        }
      } else {
        serviceResult = ServiceResult.SERVICE_ERROR;
      }
    }

    return serviceResult;
  }

  @Nullable
  private static Intent getVerifiedServiceIntent(Context context) {
    PackageManager packageManager = context.getPackageManager();
    if (packageManager != null) {
      Intent serviceIntent = new Intent(RECEIVER_SERVICE_ACTION);
      serviceIntent.setPackage(RECEIVER_SERVICE_PACKAGE);
      ResolveInfo serviceInfo = packageManager.resolveService(serviceIntent, 0);

      if (serviceInfo != null
          && FacebookSignatureValidator.validateSignature(context, RECEIVER_SERVICE_PACKAGE)) {
        return serviceIntent;
      } else {
        Intent wakizashiServiceIntent = new Intent(RECEIVER_SERVICE_ACTION);
        wakizashiServiceIntent.setPackage(RECEIVER_SERVICE_PACKAGE_WAKIZASHI);
        ResolveInfo wakizashiServiceInfo = packageManager.resolveService(wakizashiServiceIntent, 0);

        if (wakizashiServiceInfo != null
            && FacebookSignatureValidator.validateSignature(
                context, RECEIVER_SERVICE_PACKAGE_WAKIZASHI)) {
          return wakizashiServiceIntent;
        }
      }
    }

    return null;
  }

  enum ServiceResult {
    OPERATION_SUCCESS,
    SERVICE_NOT_AVAILABLE,
    SERVICE_ERROR
  }

  enum EventType {
    MOBILE_APP_INSTALL("MOBILE_APP_INSTALL"),
    CUSTOM_APP_EVENTS("CUSTOM_APP_EVENTS");

    private String eventType;

    EventType(String eventType) {
      this.eventType = eventType;
    }

    @Override
    public String toString() {
      return eventType;
    }
  }

  static final class RemoteServiceConnection implements ServiceConnection {
    private final CountDownLatch latch = new CountDownLatch(1);
    @Nullable private IBinder binder;

    @Override
    public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
      binder = serviceBinder;
      latch.countDown();
    }

    @Override
    public void onNullBinding(ComponentName name) {
      latch.countDown();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    @Nullable
    public IBinder getBinder() throws InterruptedException {
      latch.await(5, TimeUnit.SECONDS);
      return binder;
    }
  }
}
