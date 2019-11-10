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
package com.facebook.appevents.ml;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.facebook.FacebookSdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;

final class Model {
    private static final String DIR_NAME = "facebook_ml/";

    private String useCase;
    private String fileName;
    private int versionID;
    @Nullable private String urlStr;

    Model(String useCase, int versionID) {
        this.useCase = useCase;
        this.versionID = versionID;
        this.fileName = useCase + "_" + versionID;
    }

    Model(String useCase, int versionID, String urlStr) {
        this(useCase, versionID);
        this.urlStr = urlStr;
    }

    void initialize(final Runnable onModelInitialized) {
        File file = new File(
                FacebookSdk.getApplicationContext().getFilesDir(), DIR_NAME + fileName);
        Runnable onSucess = new Runnable() {
            @Override
            public void run() {
                initializeWeights();
                onModelInitialized.run();
            }
        };

        if (file.exists()) {
            onSucess.run();
        } else {
            download(onSucess);
        }
    }

    private void initializeWeights() {
        // TODO: (christina1012: T54293420) migrate initialize weights
    }

    @Nullable
    String predict(float[] dense, String text) {
        // TODO: (christina1012: T54293420) hook with JNI
        return "";
    }

    private void download(Runnable onSuccess) {
        if (urlStr == null) {
            return;
        }
        String[] args = new String[] {
                urlStr,
                fileName
        };
        new FileDownloadTask(onSuccess).execute(args);
    }

    static class FileDownloadTask extends AsyncTask<String, Void, Boolean> {
        Runnable onSuccess;
        FileDownloadTask(Runnable onSuccess) {
            this.onSuccess = onSuccess;
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                String urlStr = args[0];
                String filePath = DIR_NAME + args[1];
                Context context = FacebookSdk.getApplicationContext();

                File dir = new File(context.getFilesDir(), DIR_NAME);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                URL url = new URL(urlStr);
                URLConnection conn = url.openConnection();
                int contentLength = conn.getContentLength();

                DataInputStream stream = new DataInputStream(url.openStream());

                byte[] buffer = new byte[contentLength];
                stream.readFully(buffer);
                stream.close();

                File file = new File(context.getFilesDir(), filePath);
                DataOutputStream fos = new DataOutputStream(new FileOutputStream(file));
                fos.write(buffer);
                fos.flush();
                fos.close();
                return true;
            } catch (Exception e) {
                /** no op **/
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            if (isSuccess) {
                onSuccess.run();
            }
        }
    }
}
