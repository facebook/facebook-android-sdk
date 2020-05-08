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
package com.facebook.gamingservices;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

/**
 *  Callback Handler to show the Gaming Media Dialog after media is
 *  uploaded to the Gaming Media Library.
 */
public class OpenGamingMediaDialog implements GraphRequest.Callback {

    private Context context;
    private GraphRequest.Callback nestedCallback;

    public OpenGamingMediaDialog(Context context) {
        this(context, null);
    }

    public OpenGamingMediaDialog(Context context, GraphRequest.Callback callback) {
        this.context = context;
        this.nestedCallback = callback;
    }

    @Override
    public void onCompleted(GraphResponse response) {

        if (this.nestedCallback != null) {
            this.nestedCallback.onCompleted(response);
        }

        if (response == null || response.getError() != null) {
            return;
        }

        String id = response.getJSONObject().optString("id", null);
        if (id != null) {
            String dialog_uri = "https://fb.gg/me/media_asset/" + id;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dialog_uri));
            this.context.startActivity(intent);
        }
    }
}
