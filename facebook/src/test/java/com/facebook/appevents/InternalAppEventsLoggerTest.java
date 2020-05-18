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

package com.facebook.appevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Bundle;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

public class InternalAppEventsLoggerTest extends FacebookPowerMockTestCase {

  private final String mockEventName = "fb_mock_event";
  private final Executor serialExecutor = new FacebookSerialExecutor();

  @Mock public AppEventsLoggerImpl logger;

  @Before
  public void setupTest() {
    FacebookSdk.setApplicationId("123456789");
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    Whitebox.setInternalState(FacebookSdk.class, "executor", serialExecutor);
  }

  @Test
  public void testAutoLogAppEventsDisabled() throws Exception {
    final Bundle mockPayload = new Bundle();
    final BigDecimal mockVal = new BigDecimal(1.0);
    final Currency mockCurrency = Currency.getInstance(Locale.US);

    FacebookSdk.setAutoLogAppEventsEnabled(false);
    InternalAppEventsLogger internalLogger = new InternalAppEventsLogger(logger);

    internalLogger.logEvent(mockEventName, mockPayload);
    internalLogger.logEvent(mockEventName, 1.0, mockPayload);
    internalLogger.logEventImplicitly(mockEventName);
    internalLogger.logEventImplicitly(mockEventName, mockPayload);
    internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload);
    internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload);

    verify(logger, never()).logEvent(anyString());
    verify(logger, never()).logEvent(anyString(), any(Bundle.class));
    verify(logger, never()).logEvent(anyString(), anyDouble());
    verify(logger, never()).logEvent(anyString(), anyDouble(), any(Bundle.class));
    verify(logger, never()).logEventImplicitly(anyString(), anyDouble(), any(Bundle.class));
    verify(logger, never())
        .logEventImplicitly(
            anyString(), any(BigDecimal.class), any(Currency.class), any(Bundle.class));
    verify(logger, never()).logPurchase(any(BigDecimal.class), any(Currency.class));
    verify(logger, never())
        .logPurchase(any(BigDecimal.class), any(Currency.class), any(Bundle.class));
    verify(logger, never())
        .logPurchase(any(BigDecimal.class), any(Currency.class), any(Bundle.class), anyBoolean());
    verify(logger, never())
        .logPurchaseImplicitly(any(BigDecimal.class), any(Currency.class), any(Bundle.class));
  }

  @Test
  public void testInternalAppEventsLoggerLogFunctions() throws Exception {
    final Bundle mockPayload = new Bundle();
    final BigDecimal mockVal = new BigDecimal(1.0);
    final Currency mockCurrency = Currency.getInstance(Locale.US);

    FacebookSdk.setAutoLogAppEventsEnabled(true);
    InternalAppEventsLogger internalLogger = new InternalAppEventsLogger(logger);

    internalLogger.logEvent(mockEventName, null);
    verify(logger, times(1)).logEvent(mockEventName, null);

    internalLogger.logEvent(mockEventName, 1.0, mockPayload);
    verify(logger, times(1))
        .logEvent(
            eq(mockEventName),
            eq(1.0),
            argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

    internalLogger.logEventImplicitly(mockEventName);
    verify(logger, times(1)).logEventImplicitly(mockEventName, null, null);

    internalLogger.logEventImplicitly(mockEventName, mockPayload);
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName),
            isNull(Double.class),
            argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

    internalLogger.logEventImplicitly(mockEventName, 1.0, mockPayload);
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName),
            eq(1.0),
            argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

    internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload);
    verify(logger, times(1))
        .logEventImplicitly(
            eq(mockEventName),
            eq(mockVal),
            eq(mockCurrency),
            argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

    internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload);
    verify(logger, times(1))
        .logPurchaseImplicitly(
            eq(mockVal),
            eq(mockCurrency),
            argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));
  }
}
