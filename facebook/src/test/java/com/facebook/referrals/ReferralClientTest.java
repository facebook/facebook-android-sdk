package com.facebook.referrals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.facebook.CustomTabMainActivity;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Validate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({FacebookSdk.class, Validate.class})
public class ReferralClientTest extends FacebookPowerMockTestCase {
  private static final String CHROME_PACKAGE = "com.android.chrome";
  private static final String MOCK_APP_ID = "1234";
  private static final String REFERRAL_DIALOG = "share_referral";

  @Mock public Fragment mockFragment;
  @Mock public FragmentActivity mockFragmentActivity;
  @Mock public PackageManager mockPackageManager;

  private ReferralClient referralClient;

  @Before
  public void before() {
    mockStatic(FacebookSdk.class);

    when(FacebookSdk.isInitialized()).thenReturn(true);
    when(FacebookSdk.getApplicationId()).thenReturn(MOCK_APP_ID);
    when(FacebookSdk.getApplicationContext()).thenReturn(mockFragmentActivity);
    when(mockFragment.getActivity()).thenReturn(mockFragmentActivity);
    when(mockFragment.isAdded()).thenReturn(true);
    when(mockFragmentActivity.getPackageManager()).thenReturn(mockPackageManager);
    when(mockFragmentActivity.getPackageName()).thenReturn("com.facebook.referrals");
    referralClient = new ReferralClient(mockFragment);
    mockChromeCustomTabsSupported(true);
  }

  @Test
  public void testStartCustomTabMainActivitySuccessWithDeveloperDefinedURL() {
    testStartCustomTabMainActivitySuccess(ReferralClient.getDeveloperDefinedRedirectUrl());
  }

  @Test
  public void testStartCustomTabMainActivitySuccessWithDefaultURL() {
    testStartCustomTabMainActivitySuccess(CustomTabUtils.getDefaultRedirectURI());
  }

  private void testStartCustomTabMainActivitySuccess(String redirectUri) {
    mockCustomTabRedirectActivity(redirectUri);

    referralClient.startReferral();

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockFragment).startActivityForResult(intentArgumentCaptor.capture(), anyInt());
    Intent intent = intentArgumentCaptor.getValue();

    ComponentName componentName = intent.getComponent();
    assertEquals(CustomTabMainActivity.class.getName(), componentName.getClassName());
    assertEquals(intent.getStringExtra(CustomTabMainActivity.EXTRA_ACTION), REFERRAL_DIALOG);
    assertEquals(intent.getStringExtra(CustomTabMainActivity.EXTRA_CHROME_PACKAGE), CHROME_PACKAGE);

    Bundle params = intent.getBundleExtra(CustomTabMainActivity.EXTRA_PARAMS);
    assertNotNull(params);
    assertEquals(params.getString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI), redirectUri);
    assertEquals(params.getString(ServerProtocol.DIALOG_PARAM_APP_ID), MOCK_APP_ID);
  }

  @Test
  public void testStartCustomTabMainActivityCustomTabNotAllowed() {
    mockChromeCustomTabsSupported(false);

    referralClient.startReferral();

    verify(mockFragment, never()).startActivityForResult(any(Intent.class), anyInt());

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockFragmentActivity)
        .setResult(eq(Activity.RESULT_CANCELED), intentArgumentCaptor.capture());
    Intent intent = intentArgumentCaptor.getValue();
    assertNotNull(intent.getStringExtra(ReferralClient.ERROR_MESSAGE_KEY));
  }

  @Test
  public void testStartCustomTabMainActivityNoInternetPermission() {
    when(mockFragmentActivity.checkCallingOrSelfPermission(eq(Manifest.permission.INTERNET)))
        .thenReturn(PackageManager.PERMISSION_DENIED);

    referralClient.startReferral();

    verify(mockFragment, never()).startActivityForResult(any(Intent.class), anyInt());

    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mockFragmentActivity)
        .setResult(eq(Activity.RESULT_CANCELED), intentArgumentCaptor.capture());
    Intent intent = intentArgumentCaptor.getValue();
    assertNotNull(intent.getStringExtra(ReferralClient.ERROR_MESSAGE_KEY));
  }

  private void mockCustomTabRedirectActivity(String redirectUri) {
    mockStatic(Validate.class);
    when(Validate.hasCustomTabRedirectActivity(nullable(Context.class), eq(redirectUri)))
        .thenReturn(true);
  }

  private void mockChromeCustomTabsSupported(final boolean supported) {
    final List<ResolveInfo> resolveInfos = new ArrayList<>();
    ResolveInfo resolveInfo = new ResolveInfo();
    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.packageName = CHROME_PACKAGE;
    resolveInfo.serviceInfo = serviceInfo;
    if (supported) {
      resolveInfos.add(resolveInfo);
    }
    when(mockPackageManager.queryIntentServices(any(Intent.class), anyInt()))
        .thenReturn(resolveInfos);
  }
}
