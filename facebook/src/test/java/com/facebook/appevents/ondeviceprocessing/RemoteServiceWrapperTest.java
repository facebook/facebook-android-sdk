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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.internal.FacebookSignatureValidator;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.ppml.receiver.IReceiverService;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
  FacebookSdk.class,
  FacebookSignatureValidator.class,
  RemoteServiceWrapper.class,
  IReceiverService.Stub.class,
  FetchedAppSettingsManager.class,
  AppEventUtility.class
})
public class RemoteServiceWrapperTest extends FacebookPowerMockTestCase {

  private final String applicationId;
  private final List<AppEvent> appEvents;
  private Context mockContext;

  public RemoteServiceWrapperTest() throws JSONException {
    applicationId = "app_id";
    appEvents =
        Arrays.asList(new AppEvent("context_name", "test_event", 0.0, null, false, false, null));
  }

  @Before
  public void setUp() throws Exception {
    mockContext = mock(Context.class);

    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(mockContext);

    // Disable AppEventUtility.isMainThread since executor now runs in main thread
    PowerMockito.spy(AppEventUtility.class);
    PowerMockito.doReturn(false).when(AppEventUtility.class, "isMainThread");

    // Reset internal state
    Boolean value = null;
    Whitebox.setInternalState(RemoteServiceWrapper.class, "isServiceAvailable", value);
  }

  @Test
  public void testSendCustomEvents_RemoteServiceNotAvailable() throws Exception {
    // Arrange
    IReceiverService mockRemoteService = mockRemoteService(null, false, false, true);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.SERVICE_NOT_AVAILABLE));
    verify(mockRemoteService, never()).sendEvents(any(Bundle.class));
  }

  @Test
  public void testSendCustomEvents_RemoteServiceAvailableButSignatureMismatch() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), false, false, true);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.SERVICE_NOT_AVAILABLE));
    verify(mockRemoteService, never()).sendEvents(any(Bundle.class));
  }

  @Test
  public void testSendCustomEvents_RemoteServiceAvailableButFailedToBind() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), true, false, true);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.SERVICE_ERROR));
    verify(mockRemoteService, never()).sendEvents(any(Bundle.class));
  }

  @Test
  public void testSendCustomEvents_RemoteServiceAvailableButBinderIsNull() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), true, true, true);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.SERVICE_NOT_AVAILABLE));
    verify(mockRemoteService, never()).sendEvents(any(Bundle.class));
  }

  @Test
  public void testSendCustomEvents_RemoteServiceAvailableButThrowsException() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), true, true, false);
    doThrow(RemoteException.class).when(mockRemoteService).sendEvents(any(Bundle.class));

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.SERVICE_ERROR));
    verify(mockRemoteService).sendEvents(any(Bundle.class));
  }

  @Test
  public void testSendCustomEvents_RemoteServiceAvailable() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), true, true, false);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendCustomEvents(applicationId, appEvents);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.OPERATION_SUCCESS));

    ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
    verify(mockRemoteService).sendEvents(captor.capture());
    assertThat(
        captor.getValue().getString("event"),
        is(RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS.toString()));
  }

  @Test
  public void testSendInstallEvent_RemoteServiceAvailable() throws Exception {
    // Arrange
    IReceiverService mockRemoteService =
        mockRemoteService(mock(ResolveInfo.class), true, true, false);

    // Act
    RemoteServiceWrapper.ServiceResult serviceResult =
        RemoteServiceWrapper.sendInstallEvent(applicationId);

    // Assert
    assertThat(serviceResult, is(RemoteServiceWrapper.ServiceResult.OPERATION_SUCCESS));

    ArgumentCaptor<Bundle> captor = ArgumentCaptor.forClass(Bundle.class);
    verify(mockRemoteService).sendEvents(captor.capture());
    assertThat(
        captor.getValue().getString("event"),
        is(RemoteServiceWrapper.EventType.MOBILE_APP_INSTALL.toString()));
    assertThat(captor.getValue().getString("custom_events"), nullValue());
  }

  @Test
  public void testIsServiceAvailable_RemoteServiceNotAvailable() throws Exception {
    // Arrange
    mockRemoteService(null, false, false, true);

    // Act
    boolean serviceAvailable = RemoteServiceWrapper.isServiceAvailable();

    // Assert
    assertThat(serviceAvailable, is(false));
  }

  @Test
  public void testIsServiceAvailable_RemoteServiceAvailable() throws Exception {
    // Arrange
    mockRemoteService(mock(ResolveInfo.class), true, false, true);

    // Act
    boolean serviceAvailable = RemoteServiceWrapper.isServiceAvailable();

    // Assert
    assertThat(serviceAvailable, is(true));
  }

  private IReceiverService mockRemoteService(
      ResolveInfo serviceResolveInfo,
      boolean isSignatureValid,
      boolean isServiceBindSuccessful,
      boolean isBinderNull)
      throws Exception {
    // Mock PackageManager
    PackageManager mockPackageManager = mock(PackageManager.class);
    when(mockPackageManager.resolveService(any(Intent.class), anyInt()))
        .thenReturn(serviceResolveInfo);
    when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

    // Mock FacebookSignatureValidator
    PowerMockito.mockStatic(FacebookSignatureValidator.class);
    PowerMockito.when(FacebookSignatureValidator.validateSignature(any(Context.class), anyString()))
        .thenReturn(isSignatureValid);

    // Mock Context.bindService
    when(mockContext.bindService(any(Intent.class), any(ServiceConnection.class), anyInt()))
        .thenReturn(isServiceBindSuccessful);

    // Mock FetchedAppSettings
    FetchedAppSettings mockAppSettings = mock(FetchedAppSettings.class);
    when(mockAppSettings.supportsImplicitLogging()).thenReturn(false);
    PowerMockito.mockStatic(FetchedAppSettingsManager.class);
    PowerMockito.when(FetchedAppSettingsManager.queryAppSettings(anyString(), anyBoolean()))
        .thenReturn(mockAppSettings);

    // Mock remote service creation
    RemoteServiceWrapper.RemoteServiceConnection mockRemoteServiceConnection =
        mock(RemoteServiceWrapper.RemoteServiceConnection.class);
    IBinder mockBinder = isBinderNull ? null : mock(IBinder.class);
    when(mockRemoteServiceConnection.getBinder()).thenReturn(mockBinder);
    PowerMockito.whenNew(RemoteServiceWrapper.RemoteServiceConnection.class)
        .withNoArguments()
        .thenReturn(mockRemoteServiceConnection);

    IReceiverService mockRemoteService = mock(IReceiverService.class);
    PowerMockito.mockStatic(IReceiverService.Stub.class);
    PowerMockito.when(IReceiverService.Stub.asInterface(mockBinder)).thenReturn(mockRemoteService);

    return mockRemoteService;
  }
}
