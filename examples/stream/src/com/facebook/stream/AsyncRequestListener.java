/*
 * Copyright 2010 Facebook, Inc.
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

package com.facebook.stream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;

abstract class AsyncRequestListener implements RequestListener {

    public void onComplete(String response, final Object state) {
        try {
            JSONObject obj = Util.parseJson(response);
            onComplete(obj, state);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("facebook-stream", "JSON Error:" + e.getMessage());
        } catch (FacebookError e) {
            Log.e("facebook-stream", "Facebook Error:" + e.getMessage());
        }

    }

    public abstract void onComplete(JSONObject obj, final Object state);

    public void onFacebookError(FacebookError e, final Object state) {
        Log.e("stream", "Facebook Error:" + e.getMessage());
    }

    public void onFileNotFoundException(FileNotFoundException e,
                                        final Object state) {
        Log.e("stream", "Resource not found:" + e.getMessage());      
    }

    public void onIOException(IOException e, final Object state) {
        Log.e("stream", "Network Error:" + e.getMessage());      
    }

    public void onMalformedURLException(MalformedURLException e,
                                        final Object state) {
        Log.e("stream", "Invalid URL:" + e.getMessage());            
    }

}