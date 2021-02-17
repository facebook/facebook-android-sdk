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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.eventdeactivation.EventDeactivationManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({FacebookSdk.class, FetchedAppSettingsManager.class})
public class RemoteServiceParametersHelperTest extends FacebookPowerMockTestCase {

  private final String applicationId;
  private final AppEvent implicitEvent;
  private final AppEvent explicitEvent;
  private final AppEvent deprecatedEvent;
  private final AppEvent invalidChecksumEvent;

  private Context context;

  public RemoteServiceParametersHelperTest() throws JSONException {
    applicationId = "mock_app_id";

    implicitEvent = new AppEvent("context_name", "implicit_event", 0.0, null, true, false, null);
    explicitEvent = new AppEvent("context_name", "explicit_event", 0.0, null, false, false, null);
    deprecatedEvent =
        new AppEvent("context_name", "deprecated_event", 0.0, null, false, false, null);

    invalidChecksumEvent =
        new AppEvent("context_name", "invalid_checksum_event", 0.0, null, false, false, null);
    Whitebox.setInternalState(invalidChecksumEvent, "checksum", "invalid_checksum");
  }

  @Before
  public void setUp() throws Exception {
    context = ApplicationProvider.getApplicationContext();

    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(context);
  }

  @Test
  public void testBuildEventsBundle_FiltersEvents() throws Exception {
    // Arrange
    Whitebox.setInternalState(EventDeactivationManager.class, "enabled", true);
    Whitebox.setInternalState(
        EventDeactivationManager.class,
        "deprecatedEvents",
        new HashSet<>(Arrays.asList(deprecatedEvent.getName())));

    FetchedAppSettings mockAppSettings = mock(FetchedAppSettings.class);
    when(mockAppSettings.supportsImplicitLogging()).thenReturn(false);
    PowerMockito.mockStatic(FetchedAppSettingsManager.class);
    PowerMockito.when(FetchedAppSettingsManager.queryAppSettings(anyString(), anyBoolean()))
        .thenReturn(mockAppSettings);

    List<AppEvent> appEvents =
        Arrays.asList(implicitEvent, deprecatedEvent, explicitEvent, invalidChecksumEvent);
    RemoteServiceWrapper.EventType eventType = RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS;

    // Act
    Bundle eventsBundle =
        RemoteServiceParametersHelper.buildEventsBundle(eventType, applicationId, appEvents);

    // Assert
    assertThat(eventsBundle.getString("event"), is(eventType.toString()));
    assertThat(eventsBundle.getString("app_id"), is(applicationId));
    String expectedEventsJson = String.format("[%s]", explicitEvent.getJSONObject().toString());
    assertThat(eventsBundle.getString("custom_events"), is(expectedEventsJson));
  }

  @Test
  public void testBuildEventsBundle_ReturnNull() throws Exception {
    // Arrange
    List<AppEvent> appEvents = Arrays.asList(implicitEvent);

    // Act
    Bundle eventsBundle =
        RemoteServiceParametersHelper.buildEventsBundle(
            RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS, applicationId, appEvents);

    // Assert
    assertThat(eventsBundle, nullValue());
  }
}
