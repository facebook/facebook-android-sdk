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

import android.content.Context;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OnDeviceProcessingManager {

  private static Boolean isEligibleForOnDeviceProcessing;

  private static final Set<String> ALLOWED_IMPLICIT_EVENTS =
      new HashSet<>(
          Arrays.asList(
              AppEventsConstants.EVENT_NAME_PURCHASED,
              AppEventsConstants.EVENT_NAME_START_TRIAL,
              AppEventsConstants.EVENT_NAME_SUBSCRIBE));

  public static boolean isOnDeviceProcessingEnabled() {
    if (isEligibleForOnDeviceProcessing == null) {
      Context context = FacebookSdk.getApplicationContext();
      boolean isApplicationTrackingEnabled = !FacebookSdk.getLimitEventAndDataUsage(context);

      isEligibleForOnDeviceProcessing =
          isApplicationTrackingEnabled && RemoteServiceWrapper.isServiceAvailable();
    }

    return isEligibleForOnDeviceProcessing;
  }

  public static void sendCustomEvent(final String applicationId, final AppEvent event) {
    if (isEventEligibleForOnDeviceProcessing(event)) {
      FacebookSdk.getExecutor()
          .execute(
              new Runnable() {
                @Override
                public void run() {
                  RemoteServiceWrapper.sendCustomEvents(applicationId, Arrays.asList(event));
                }
              });
    }
  }

  private static boolean isEventEligibleForOnDeviceProcessing(AppEvent event) {
    boolean isAllowedImplicitEvent =
        event.getIsImplicit() && ALLOWED_IMPLICIT_EVENTS.contains(event.getName());
    boolean isExplicitEvent = !event.getIsImplicit();

    return isExplicitEvent || isAllowedImplicitEvent;
  }
}
