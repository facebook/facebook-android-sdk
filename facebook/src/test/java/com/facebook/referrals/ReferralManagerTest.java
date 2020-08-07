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

package com.facebook.referrals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({FacebookSdk.class})
public class ReferralManagerTest extends FacebookPowerMockTestCase {
  private static final String MOCK_APP_ID = "1234";

  @Mock public Activity mockActivity;
  @Mock public Fragment mockFragment;
  @Mock public Context mockApplicationContext;
  @Mock public PackageManager mockPackageManager;
  @Mock public FacebookCallback<ReferralResult> mockCallback;
  @Mock public FragmentActivity mockFragmentActivity;
  @Mock public SharedPreferences mockSharedPreferences;

  @Before
  public void before() throws Exception {
    mockStatic(FacebookSdk.class);

    when(FacebookSdk.isInitialized()).thenReturn(true);
    when(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID);
    when(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext);
    when(mockFragment.getActivity()).thenReturn(mockFragmentActivity);
    when(mockActivity.getApplicationContext()).thenReturn(mockApplicationContext);
    when(FacebookSdk.getApplicationContext().getSharedPreferences(anyString(), anyInt()))
        .thenReturn(mockSharedPreferences);
    ResolveInfo resolveInfo = new ResolveInfo();
    when(mockApplicationContext.getPackageManager()).thenReturn(mockPackageManager);
    when(mockPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(resolveInfo);
  }

  @Test
  public void testRequiresSdkToBeInitialized() {
    try {
      when(FacebookSdk.isInitialized()).thenReturn(false);

      ReferralManager referralManager = new ReferralManager();

      fail();
    } catch (FacebookSdkNotInitializedException exception) {
    }
  }

  @Test
  public void testGetInstance() {
    ReferralManager referralManager1 = ReferralManager.getInstance();
    assertNotNull(referralManager1);
    ReferralManager referralManager2 = ReferralManager.getInstance();
    assertNotNull(referralManager2);
    assertEquals(referralManager1, referralManager2);
  }

  @Test
  public void testThrowsIfCannotResolveFacebookActivity() {
    when(mockPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(null);

    ReferralManager referralManager = new ReferralManager();

    try {
      referralManager.startReferral(mockActivity);
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testThrowsIfCannotStartFacebookActivity() {
    doThrow(new ActivityNotFoundException())
        .when(mockActivity)
        .startActivityForResult(any(Intent.class), anyInt());

    ReferralManager referralManager = new ReferralManager();

    try {
      referralManager.startReferral(mockActivity);
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testRequiresNonNullActivity() {
    try {
      ReferralManager referralManager = new ReferralManager();
      referralManager.startReferral((Activity) null);
      fail();
    } catch (NullPointerException exception) {
    }
  }

  @Test
  public void testRequiresNonNullFragment() {
    try {
      ReferralManager referralManager = new ReferralManager();
      referralManager.startReferral((Fragment) null);
      fail();
    } catch (NullPointerException exception) {
    }
  }

  @Test
  public void testThrowsIfCallbackManagerInvalid() {
    ReferralManager referralManager = new ReferralManager();

    try {
      referralManager.registerCallback(
          new CallbackManager() {
            @Override
            public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
              return false;
            }
          },
          mockCallback);
      fail();
    } catch (FacebookException exception) {
    }
  }

  @Test
  public void testActivityStartsFacebookActivityWithCorrectRequest() {
    ReferralManager referralManager = new ReferralManager();
    referralManager.startReferral(mockActivity);

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockActivity).startActivityForResult(intentArgumentCaptor.capture(), anyInt());
    Intent intent = intentArgumentCaptor.getValue();

    ComponentName componentName = intent.getComponent();
    assertEquals(FacebookActivity.class.getName(), componentName.getClassName());
    assertEquals(ReferralFragment.TAG, intent.getAction());
  }

  @Test
  public void testFragmentStartsFacebookActivityWithCorrectRequest() {
    ReferralManager referralManager = new ReferralManager();
    referralManager.startReferral(mockFragment);

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockFragment).startActivityForResult(intentArgumentCaptor.capture(), anyInt());
    Intent intent = intentArgumentCaptor.getValue();

    ComponentName componentName = intent.getComponent();
    assertEquals(FacebookActivity.class.getName(), componentName.getClassName());
    assertEquals(ReferralFragment.TAG, intent.getAction());
  }

  @Test
  public void testCallbackOnSuccess() throws Exception {
    ReferralManager referralManager = new ReferralManager();
    referralManager.startReferral(mockFragment);

    Intent data = new Intent();
    List<String> referralCodes = Arrays.asList("abc", "def");
    data.putExtra(ReferralFragment.REFERRAL_CODES_KEY, referralCodes.toString());
    boolean result = ReferralManager.onActivityResult(Activity.RESULT_OK, data, mockCallback);

    assertTrue(result);
    verify(mockCallback, never()).onCancel();
    verify(mockCallback, times(1)).onSuccess(new ReferralResult(referralCodes));
    verify(mockCallback, never()).onError(isA(FacebookException.class));
  }

  @Test
  public void testCallbackOnCancel() {
    ReferralManager referralManager = new ReferralManager();
    referralManager.startReferral(mockFragment);

    boolean result = ReferralManager.onActivityResult(Activity.RESULT_CANCELED, null, mockCallback);

    assertTrue(result);
    verify(mockCallback, times(1)).onCancel();
    verify(mockCallback, never()).onSuccess(isA(ReferralResult.class));
    verify(mockCallback, never()).onError(isA(FacebookException.class));
  }

  @Test
  public void testCallbackOnError() {
    ReferralManager referralManager = new ReferralManager();
    referralManager.startReferral(mockFragment);

    Intent data = new Intent();
    data.putExtra(ReferralFragment.ERROR_MESSAGE_KEY, "test");
    boolean result = ReferralManager.onActivityResult(Activity.RESULT_CANCELED, data, mockCallback);

    assertTrue(result);
    verify(mockCallback, never()).onCancel();
    verify(mockCallback, never()).onSuccess(isA(ReferralResult.class));
    verify(mockCallback, times(1)).onError(isA(FacebookException.class));
  }
}
