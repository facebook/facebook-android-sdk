/**
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

package com.facebook.appevents.restrictivedatafilter;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.ml.Model;
import com.facebook.appevents.ml.ModelManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

@PrepareForTest({
        AddressFilterManager.class,
        ModelManager.class,
        FacebookSdk.class,
})
public class AddressFilterManagerTest extends FacebookPowerMockTestCase {

    private final Executor mockExecutor = new FacebookSerialExecutor();

    @Before
    @Override
    public void setup() {
        super.setup();
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
        AddressFilterManager.enable();
        PowerMockito.spy(ModelManager.class);
        PowerMockito.when(ModelManager.predict(Matchers.anyString(),
                Matchers.any(float[].class),
                Matchers.anyString())).thenReturn(Model.SHOULD_FILTER);
        PowerMockito.spy(AddressFilterManager.class);
        Whitebox.setInternalState(AddressFilterManager.class, "isSampleEnabled", true);
    }

    @Test
    public void testProcessParameters() throws JSONException {
        Map<String, String> mockParameters = new HashMap<>();
        mockParameters.put("customer_Address", "1 Hacker way");
        Map<String, String> expectedParameters = new HashMap<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("customer_Address", "1 Hacker way");
        expectedParameters.put("_addressParams", jsonObject.toString());

        AddressFilterManager.processParameters(mockParameters);
        assertEquals(1, mockParameters.size());
        assertEquals(expectedParameters, mockParameters);
    }
}
