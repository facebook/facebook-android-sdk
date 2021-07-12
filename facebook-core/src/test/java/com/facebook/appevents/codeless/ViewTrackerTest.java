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

package com.facebook.appevents.codeless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.facebook.appevents.codeless.internal.EventBinding;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class ViewTrackerTest extends CodelessTestBase {
  @Test
  public void testFindView() throws JSONException {
    String sample =
        "{'event_name': 'sample_event',"
            + "'method': 'MANUAL', 'event_type': 'CLICK',"
            + "'app_version': '1.0', 'path_type': 'absolute',"
            + "'path': ["
            + "   {'class_name': 'android.widget.LinearLayout'},"
            + "   {'class_name': 'android.widget.LinearLayout'},"
            + "   {"
            + "       'class_name': 'android.widget.TextView',"
            + "       'text': 'Inner Label'"
            + "   }"
            + "]"
            + "}";
    JSONObject json = new JSONObject(sample);
    EventBinding sampleBinding = EventBinding.getInstanceFromJson(json);
    assertNotNull(sampleBinding);

    List<CodelessMatcher.MatchedView> matched =
        CodelessMatcher.ViewMatcher.findViewByPath(
            sampleBinding, root, sampleBinding.getViewPath(), 0, -1, "Activity");
    assertEquals(1, matched.size());
  }
}
