/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.test.suitebuilder.annotation.LargeTest;
import com.facebook.model.GraphObject;

import java.util.Date;

// These tests relate to serialization/de-serialization of graph objects in a variety of scenarios, rather than
// to the underlying request/batch plumbing.
public class GraphRequestTests extends FacebookTestCase {

    @LargeTest
    public void testCommentRoundTrip() {
        TestSession session = openTestSessionWithSharedUser();

        GraphObject status = createStatusUpdate("");
        GraphObject createdStatus = batchCreateAndGet(session, "me/feed", status, null, GraphObject.class);
        String statusID = (String) createdStatus.getProperty("id");

        GraphObject comment = GraphObject.Factory.create();
        final String commentMessage = "It truly is a wonderful status update.";
        comment.setProperty("message", commentMessage);

        GraphObject createdComment1 = batchCreateAndGet(session, statusID + "/comments", comment, null,
                GraphObject.class);
        assertNotNull(createdComment1);

        String comment1ID = (String) createdComment1.getProperty("id");
        String comment1Message = (String) createdComment1.getProperty("message");
        assertNotNull(comment1ID);
        assertNotNull(comment1Message);
        assertEquals(commentMessage, comment1Message);

        // Try posting the same comment to the same status update. We need to clear its ID first.
        createdComment1.removeProperty("id");
        GraphObject createdComment2 = batchCreateAndGet(session, statusID + "/comments", createdComment1, null,
                GraphObject.class);
        assertNotNull(createdComment2);

        String comment2ID = (String) createdComment2.getProperty("id");
        String comment2Message = (String) createdComment2.getProperty("message");
        assertNotNull(comment2ID);
        assertFalse(comment1ID.equals(comment2ID));
        assertNotNull(comment2Message);
        assertEquals(commentMessage, comment2Message);
    }

    @LargeTest
    public void testEventRoundTrip() {
        TestSession session = openTestSessionWithSharedUserAndPermissions(null, "create_event");

        GraphObject event = GraphObject.Factory.create();
        // Android emulators tend to not have the right date/time. To avoid issues with posting events in the past
        // or too far in the future, we use a constant year. This test will break in 2030, angering our robot overlords.
        Date startTime = new Date(130, 2, 17, 12, 34, 56);
        event.setProperty("name", "My awesome St. Patrick's Day party on " + startTime.toString());
        final String eventDescription = "This is a great event. You should all come.";
        event.setProperty("description", eventDescription);
        Date endTime = new Date(startTime.getTime() + 3600 * 1000);
        event.setProperty("start_time", startTime);
        event.setProperty("end_time", endTime);
        event.setProperty("location", "My house");

        GraphObject event1 = batchCreateAndGet(session, "me/events", event, null, GraphObject.class);
        assertNotNull(event1);
        assertEquals(eventDescription, event1.getProperty("description"));

        event1.removeProperty("id");
        GraphObject event2 = batchCreateAndGet(session, "me/events", event1, null, GraphObject.class);
        assertNotNull(event2);
        assertEquals(eventDescription, event2.getProperty("description"));
    }

}
