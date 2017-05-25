/**
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

package com.facebook.internal;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.facebook.FacebookPowerMockTestCase;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link com.facebook.internal.FacebookSignatureValidator}.
 */
@PrepareForTest({Utility.class})
public class FacebookSignatureValidatorTest extends FacebookPowerMockTestCase {

  private static final String PACKAGE_NAME = "com.facebook.orca";
  private static final String APP_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9";

  private Activity mMockActivity;
  private PackageManager mMockPackageManager;

  @Before
  public void setup() {
    mockStatic(Utility.class);
    mMockActivity = mock(Activity.class);
    mMockPackageManager = mock(PackageManager.class);
    when(mMockActivity.getPackageManager()).thenReturn(mMockPackageManager);
  }

  @Test
  public void testInvalidWhenAppNotInstalled() throws Exception {
    setupPackageManagerForApp(false, false);
    assertFalse(FacebookSignatureValidator.validateSignature(mMockActivity, PACKAGE_NAME));
  }

  @Test
  public void testInvalidWhenInstalledWithIncorrectSignature() throws Exception {
    setupPackageManagerForApp(true, false);
    assertFalse(FacebookSignatureValidator.validateSignature(mMockActivity, PACKAGE_NAME));
  }

  @Test
  public void testValidWhenInstalledWithCorrectSignature() throws Exception {
    setupPackageManagerForApp(true, true);
    assertTrue(FacebookSignatureValidator.validateSignature(mMockActivity, PACKAGE_NAME));
  }

  /**
   * Sets up the PackageManager to return what we expect depending on whether app is installed.
   * @param isInstalled true to simulate that app is installed
   */
  private void setupPackageManagerForApp(boolean isInstalled, boolean hasValidSignature)
      throws Exception {
    if (isInstalled) {
      PackageInfo packageInfo = new PackageInfo();
      when(mMockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
              .thenReturn(packageInfo);
      Signature signature = mock(Signature.class);
      packageInfo.signatures = new Signature[]{signature};

      if (hasValidSignature) {
        when(Utility.sha1hash(signature.toByteArray())).thenReturn(APP_HASH);
      }
    } else {
      when(mMockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
          .thenThrow(new PackageManager.NameNotFoundException());
    }
  }
}
