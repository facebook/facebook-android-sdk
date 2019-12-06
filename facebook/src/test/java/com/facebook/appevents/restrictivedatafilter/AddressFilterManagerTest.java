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
import com.facebook.appevents.ml.Model;
import com.facebook.appevents.ml.ModelManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@PrepareForTest({
        AddressFilterManager.class,
        ModelManager.class,
})
public class AddressFilterManagerTest extends FacebookPowerMockTestCase {

    @Before
    @Override
    public void setup() {
        super.setup();
        AddressFilterManager.enable();
        PowerMockito.spy(ModelManager.class);
        PowerMockito.when(ModelManager.predict(Matchers.anyString(),
                Matchers.any(float[].class),
                Matchers.anyString())).thenReturn(Model.SHOULD_FILTER);
    }

    @Test
    public void testProcessParameters() {
        Map<String, String> mockParameters = new HashMap<>();
        mockParameters.put("customer_Address", "1 Hacker way");

        AddressFilterManager.processParameters(mockParameters);
        assertEquals(0, mockParameters.size());
    }
}
