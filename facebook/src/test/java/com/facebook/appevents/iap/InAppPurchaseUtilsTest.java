/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.iap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({
  FacebookSdk.class,
})
public class InAppPurchaseUtilsTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();

  @Before
  @Override
  public void setup() {
    super.setup();
    PowerMockito.mockStatic(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.isInitialized()).thenReturn(true);
    PowerMockito.when(FacebookSdk.getExecutor()).thenReturn(mockExecutor);
  }

  @Test
  public void testGetClass() {
    InAppPurchaseTestClass myTestClass = new InAppPurchaseTestClass(new Object());
    assertThat(myTestClass).isNotNull();

    Class<?> clazz =
        InAppPurchaseUtils.getClass("com.facebook.appevents.iap.InAppPurchaseTestClass");
    assertThat(clazz).isNotNull();
  }

  @Test
  public void testGetMethod() {
    Class<?> clazz =
        InAppPurchaseUtils.getClass("com.facebook.appevents.iap.InAppPurchaseTestClass");
    if (clazz == null) {
      fail("Fail to get class");
    }
    Method testMethod = InAppPurchaseUtils.getMethod(clazz, "inAppPurchaseTestMethod");
    assertThat(testMethod).isNotNull();
  }

  @Test
  public void testInvokeMethod() {
    InAppPurchaseTestClass myTestClass = new InAppPurchaseTestClass(new Object());
    Class<?> clazz =
        InAppPurchaseUtils.getClass("com.facebook.appevents.iap.InAppPurchaseTestClass");
    if (clazz == null) {
      fail("Fail to get class");
    }
    Method testMethod = InAppPurchaseUtils.getMethod(clazz, "inAppPurchaseTestMethod");
    if (testMethod == null) {
      fail("Fail to get method");
    }

    Object result = InAppPurchaseUtils.invokeMethod(clazz, testMethod, myTestClass);
    assertThat(result).isNotNull();
  }
}
