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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

import android.content.Context;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.AppEventsConstants;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({FacebookSdk.class, RemoteServiceWrapper.class})
public class OnDeviceProcessingManagerTest extends FacebookPowerMockTestCase {

  private final String applicationId = "app_id";
  private Context context;

  @Before
  public void setUp() throws Exception {
    context = ApplicationProvider.getApplicationContext();

    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(context);
    PowerMockito.when(FacebookSdk.getExecutor()).thenCallRealMethod();

    PowerMockito.mockStatic(RemoteServiceWrapper.class);
  }

  @Test
  public void testIsOnDeviceProcessingEnabled() {
    setupPreconditions(true, true);
    assertThat(OnDeviceProcessingManager.isOnDeviceProcessingEnabled(), is(true));

    setupPreconditions(false, true);
    assertThat(OnDeviceProcessingManager.isOnDeviceProcessingEnabled(), is(false));

    setupPreconditions(true, false);
    assertThat(OnDeviceProcessingManager.isOnDeviceProcessingEnabled(), is(false));

    setupPreconditions(false, false);
    assertThat(OnDeviceProcessingManager.isOnDeviceProcessingEnabled(), is(false));
  }

  @Test
  public void testSendCustomEvent_AllowedEvents() throws Exception {
    // Arrange
    final CountDownLatch latch = new CountDownLatch(4);
    ArgumentCaptor<List<AppEvent>> captor = setupArgumentCaptor(latch);

    // Act
    OnDeviceProcessingManager.sendCustomEvent(applicationId, createEvent("explicit_event", false));
    OnDeviceProcessingManager.sendCustomEvent(
        applicationId, createEvent(AppEventsConstants.EVENT_NAME_PURCHASED, true));
    OnDeviceProcessingManager.sendCustomEvent(
        applicationId, createEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, true));
    OnDeviceProcessingManager.sendCustomEvent(
        applicationId, createEvent(AppEventsConstants.EVENT_NAME_START_TRIAL, true));
    latch.await(1, TimeUnit.SECONDS);

    // Assert
    assertThat(
        "RemoteServiceWrapper.sendCustomEvents(...) was invoked 4 times",
        captor.getAllValues().size(),
        is(4));
  }

  @Test
  public void testSendCustomEvent_NotAllowedEvents() throws Exception {
    // Arrange
    final CountDownLatch latch = new CountDownLatch(1);
    ArgumentCaptor<List<AppEvent>> captor = setupArgumentCaptor(latch);

    // Act
    OnDeviceProcessingManager.sendCustomEvent(
        applicationId, createEvent("other_implicit_event", true));
    latch.await(1, TimeUnit.SECONDS);

    // Assert
    assertThat(
        "RemoteServiceWrapper.sendCustomEvents(...) never invoked",
        captor.getAllValues().size(),
        is(0));
  }

  private AppEvent createEvent(String eventName, boolean isImplicitlyLogged) throws JSONException {
    return new AppEvent(
        "context_name", eventName, 0.0, new Bundle(), isImplicitlyLogged, false, null);
  }

  private void setupPreconditions(
      boolean isApplicationTrackingEnabled, boolean isServiceAvailable) {
    PowerMockito.when(FacebookSdk.getLimitEventAndDataUsage(context))
        .thenReturn(!isApplicationTrackingEnabled);
    PowerMockito.when(RemoteServiceWrapper.isServiceAvailable()).thenReturn(isServiceAvailable);

    // Reset internal state
    Boolean value = null;
    Whitebox.setInternalState(
        OnDeviceProcessingManager.class, "isEligibleForOnDeviceProcessing", value);
  }

  private ArgumentCaptor<List<AppEvent>> setupArgumentCaptor(final CountDownLatch latch) {
    ArgumentCaptor<List<AppEvent>> captor = ArgumentCaptor.forClass(List.class);
    PowerMockito.when(RemoteServiceWrapper.sendCustomEvents(anyString(), captor.capture()))
        .thenAnswer(
            new Answer<RemoteServiceWrapper.ServiceResult>() {
              @Override
              public RemoteServiceWrapper.ServiceResult answer(InvocationOnMock invocation)
                  throws Throwable {
                latch.countDown();
                return RemoteServiceWrapper.ServiceResult.OPERATION_SUCCESS;
              }
            });
    return captor;
  }
}
