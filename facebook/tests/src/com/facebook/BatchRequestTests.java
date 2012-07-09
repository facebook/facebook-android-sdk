/**
 * Copyright 2012 Facebook
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;

import android.util.Log;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;

public class BatchRequestTests extends FacebookTestCase {
    // TODO need to set this via configuration or otherwise avoid having it hardcoded
    protected void setUp() throws Exception {
        super.setUp();
        Request.setSessionlessRequestApplicationId("171298632997486");
    }

    public void testBatchWithoutAppIDThrows() {
        Request.setSessionlessRequestApplicationId(null);
        try {
            Request request1 = new Request(null, "TourEiffel");
            Request request2 = new Request(null, "SpaceNeedle");
            Request.executeBatch(request1, request2);
            fail("expected FacebookException");
        } catch (FacebookException exception) {
        }
    }

    public void testExecuteBatchedGets() throws IOException {
        Request request1 = new Request(null, "TourEiffel");
        Request request2 = new Request(null, "SpaceNeedle");

        List<Response> responses = Request.executeBatch(request1, request2);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() == null);
        assertTrue(responses.get(1).getError() == null);

        GraphPlace eiffelTower = responses.get(0).getGraphObjectAs(GraphPlace.class);
        GraphPlace spaceNeedle = responses.get(1).getGraphObjectAs(GraphPlace.class);
        assertTrue(eiffelTower != null);
        assertTrue(spaceNeedle != null);

        assertEquals("Paris", eiffelTower.getLocation().getCity());
        assertEquals("Seattle", spaceNeedle.getLocation().getCity());
    }

    public void testFacebookErrorResponsesCreateErrors() {
        Request request1 = new Request(null, "somestringthatshouldneverbeavalidfobjectid");
        Request request2 = new Request(null, "someotherstringthatshouldneverbeavalidfobjectid");
        List<Response> responses = Request.executeBatch(request1, request2);

        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() != null);
        assertTrue(responses.get(1).getError() != null);
        
        FacebookException exception1 = responses.get(0).getError();
        assertTrue(exception1 instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException1 = (FacebookServiceErrorException)exception1;
        assertTrue(serviceException1.getFacebookErrorType() != null);
        assertTrue(serviceException1.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
    }

    @SuppressWarnings("unused")
    private void logHttpResult(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            Log.d("FBAndroidSDKTest", inputLine);
        in.close();

    }
}
