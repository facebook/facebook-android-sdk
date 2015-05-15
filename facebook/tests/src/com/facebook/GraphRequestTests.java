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

package com.facebook;

import android.test.suitebuilder.annotation.LargeTest;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

// These tests relate to serialization/de-serialization of graph objects in a variety of scenarios, rather than
// to the underlying request/batch plumbing.
public class GraphRequestTests extends FacebookTestCase {

    protected String[] getDefaultPermissions()
    {
        return new String[] { "email", "publish_actions", "read_stream" };
    };

    @LargeTest
    public void testCommentRoundTrip() throws JSONException {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        JSONObject status = createStatusUpdate("");
        JSONObject createdStatus = batchCreateAndGet(accessToken, "me/feed", status, null);
        String statusID = createdStatus.optString("id");

        JSONObject comment = new JSONObject();
        final String commentMessage = "It truly is a wonderful status update.";
        comment.put("message", commentMessage);

        JSONObject createdComment1 = batchCreateAndGet(accessToken, statusID + "/comments", comment, null);
        assertNotNull(createdComment1);

        String comment1ID = createdComment1.optString("id");
        String comment1Message = createdComment1.optString("message");
        assertNotNull(comment1ID);
        assertNotNull(comment1Message);
        assertEquals(commentMessage, comment1Message);

        // Try posting the same comment to the same status update. We need to clear its ID first.
        createdComment1.remove("id");
        JSONObject createdComment2 = batchCreateAndGet(accessToken, statusID + "/comments", createdComment1, null);
        assertNotNull(createdComment2);

        String comment2ID = createdComment2.optString("id");
        String comment2Message = createdComment2.optString("message");
        assertNotNull(comment2ID);
        assertFalse(comment1ID.equals(comment2ID));
        assertNotNull(comment2Message);
        assertEquals(commentMessage, comment2Message);
    }
}
