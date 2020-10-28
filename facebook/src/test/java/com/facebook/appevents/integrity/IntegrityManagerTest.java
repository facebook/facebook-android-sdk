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

package com.facebook.appevents.integrity;

import static org.junit.Assert.assertEquals;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.ml.ModelManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
  IntegrityManager.class,
  ModelManager.class,
  FacebookSdk.class,
})
public class IntegrityManagerTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();

  @Before
  @Override
  public void setup() {
    super.setup();
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    IntegrityManager.enable();
    PowerMockito.spy(IntegrityManager.class);
    Whitebox.setInternalState(IntegrityManager.class, "isSampleEnabled", true);
  }

  @Test
  public void testAddressDetection() throws Exception {
    Map<String, String> mockParameters = new HashMap<>();
    mockParameters.put("customer_Address", "1 Hacker way");
    mockParameters.put("customer_event", "event");

    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "customer_Address");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_ADDRESS)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "1 Hacker way");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "customer_event");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "event");

    Map<String, String> expectedParameters = new HashMap<>();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("customer_Address", "1 Hacker way");
    expectedParameters.put("customer_event", "event");
    expectedParameters.put("_onDeviceParams", jsonObject.toString());

    IntegrityManager.processParameters(mockParameters);
    assertEquals(2, mockParameters.size());
    assertEquals(expectedParameters, mockParameters);
  }

  @Test
  public void testHealthDataFiltering() throws Exception {
    Map<String, String> mockParameters = new HashMap<>();
    mockParameters.put("is_pregnant", "yes");
    mockParameters.put("customer_health", "heart attack");
    mockParameters.put("customer_event", "event");

    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_HEALTH)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "is_pregnant");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "yes");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "customer_health");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_HEALTH)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "heart attack");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "customer_event");
    PowerMockito.doReturn(IntegrityManager.INTEGRITY_TYPE_NONE)
        .when(IntegrityManager.class, "getIntegrityPredictionResult", "event");

    Map<String, String> expectedParameters = new HashMap<>();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("is_pregnant", "yes");
    jsonObject.put("customer_health", "heart attack");
    expectedParameters.put("customer_event", "event");
    expectedParameters.put("_onDeviceParams", jsonObject.toString());

    IntegrityManager.processParameters(mockParameters);
    assertEquals(2, mockParameters.size());
    assertEquals(expectedParameters, mockParameters);
  }
}
