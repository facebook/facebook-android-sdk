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

import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.powermock.*"})
@PrepareForTest({
        AppEventsLoggerImpl.class,
        FacebookSdk.class,
        InternalAppEventsLogger.class,
})
public class InternalAppEventsLoggerTest extends FacebookPowerMockTestCase {

    private final String TAG = InternalAppEventsLoggerTest.class.getCanonicalName();

    private final String mockEventName = "fb_mock_event";

    private AppEventsLoggerImpl logger;

    @Before
    @Override
    public void setup() {
        super.setup();

        try {
            PowerMockito.mockStatic(FacebookSdk.class);
            logger = PowerMockito.mock(AppEventsLoggerImpl.class);
            PowerMockito.whenNew(AppEventsLoggerImpl.class).withAnyArguments().thenReturn(logger);
        } catch (Exception e) {
            Log.e(TAG, "Fail to set up InternalAppEventsLoggerTest: " + e.getMessage());
        }
    }

    @Test
    public void testAutoLogAppEventsDisabled() throws Exception {
        final Bundle mockPayload = new Bundle();
        final BigDecimal mockVal = new BigDecimal(1.0);
        final Currency mockCurrency = Currency.getInstance(Locale.US);

        PowerMockito.doReturn(false).when(FacebookSdk.class, "getAutoLogAppEventsEnabled");
        InternalAppEventsLogger internalLogger =
                new InternalAppEventsLogger(RuntimeEnvironment.application);

        internalLogger.logEvent(mockEventName, mockPayload);
        internalLogger.logEvent(mockEventName, 1.0, mockPayload);
        internalLogger.logEventImplicitly(mockEventName);
        internalLogger.logEventImplicitly(mockEventName, mockPayload);
        internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload);
        internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload);

        Mockito.verify(logger, Mockito.never()).logEvent(Matchers.anyString());
        Mockito.verify(logger, Mockito.never()).logEvent(Matchers.anyString(),
                Matchers.any(Bundle.class));
        Mockito.verify(logger, Mockito.never()).logEvent(Matchers.anyString(),
                Matchers.anyDouble());
        Mockito.verify(logger, Mockito.never()).logEvent(Matchers.anyString(),
                Matchers.anyDouble(), Matchers.any(Bundle.class));
        Mockito.verify(logger, Mockito.never()).logEventImplicitly(Matchers.anyString(),
                Matchers.anyDouble(), Matchers.any(Bundle.class));
        Mockito.verify(logger, Mockito.never()).logEventImplicitly(Matchers.anyString(),
                Matchers.any(BigDecimal.class), Matchers.any(Currency.class),
                Matchers.any(Bundle.class));
        Mockito.verify(logger, Mockito.never()).logPurchase(Matchers.any(BigDecimal.class),
                Matchers.any(Currency.class));
        Mockito.verify(logger, Mockito.never()).logPurchase(Matchers.any(BigDecimal.class),
                Matchers.any(Currency.class), Matchers.any(Bundle.class));
        Mockito.verify(logger, Mockito.never()).logPurchase(Matchers.any(BigDecimal.class),
                Matchers.any(Currency.class), Matchers.any(Bundle.class), Matchers.anyBoolean());
        Mockito.verify(logger, Mockito.never()).logPurchaseImplicitly(
                Matchers.any(BigDecimal.class), Matchers.any(Currency.class),
                Matchers.any(Bundle.class));
    }

    @Test
    public void testInternalAppEventsLoggerLogFunctions() throws Exception {
        final Bundle mockPayload = new Bundle();
        final BigDecimal mockVal = new BigDecimal(1.0);
        final Currency mockCurrency = Currency.getInstance(Locale.US);

        PowerMockito.doReturn(true).when(FacebookSdk.class, "getAutoLogAppEventsEnabled");
        InternalAppEventsLogger internalLogger =
                new InternalAppEventsLogger(RuntimeEnvironment.application);

        internalLogger.logEvent(mockEventName, null);
        Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName, null);

        internalLogger.logEvent(mockEventName, 1.0, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logEvent(Matchers.eq(mockEventName),
                Matchers.eq(1.0),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

        internalLogger.logEventImplicitly(mockEventName);
        Mockito.verify(logger, Mockito.times(1)).logEventImplicitly(mockEventName, null, null);

        internalLogger.logEventImplicitly(mockEventName, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logEventImplicitly(Matchers.eq(mockEventName),
                Matchers.isNull(Double.class),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

        internalLogger.logEventImplicitly(mockEventName, 1.0, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logEventImplicitly(Matchers.eq(mockEventName),
                Matchers.eq(1.0),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

        internalLogger.logEventImplicitly(mockEventName, mockVal, mockCurrency, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logEventImplicitly(Matchers.eq(mockEventName),
                Matchers.eq(mockVal), Matchers.eq(mockCurrency),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));

        internalLogger.logPurchaseImplicitly(mockVal, mockCurrency, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logPurchaseImplicitly(Matchers.eq(mockVal),
                Matchers.eq(mockCurrency),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));
    }
}
