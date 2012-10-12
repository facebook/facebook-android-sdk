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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityUnitTestCase;
import android.util.Log;

public class FacebookTestCase extends FacebookActivityTestCase<FacebookTestCase.FacebookTestActivity> {
    public FacebookTestCase() {
        super(FacebookTestCase.FacebookTestActivity.class);
        Settings.addLoggingBehavior(LoggingBehaviors.REQUESTS);
        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_RAW_RESPONSES);
    }

    public static class FacebookTestActivity extends Activity {
    }
}

