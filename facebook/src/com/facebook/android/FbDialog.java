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

package com.facebook.android;

import com.facebook.android.Facebook.DialogListener;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout.LayoutParams;

public class FbDialog extends Dialog {
    
    static final int WIDTH = 280;
    static final int HEIGHT = 360;
    
    private String mUrl;
    private DialogListener mListener;
    private WebView mWebView;
    ProgressDialog mSpinner;

    public FbDialog(Context context, String url, DialogListener listener) {
        super(context);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpinner = new ProgressDialog(getContext());
        mWebView = new WebView(getContext());
        mWebView.setWebViewClient(new FbDialog.FbWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        //TODO(ssoneff) add title to window 
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        addContentView(mWebView, new LayoutParams(WIDTH, HEIGHT));
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mListener.onCancel();
    }

    private class FbWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Facebook-WebView", "Redirect URL: " + url);
            if (url.startsWith(Facebook.REDIRECT_URI)) {
                Bundle values = Util.parseUrl(url);
                String error = values.getString("error_reason");
                if (error == null) {
                    mListener.onSuccess(values);
                } else {
                    mListener.onError(error);
                }
                FbDialog.this.dismiss();
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(failingUrl + " failed: " + description);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d("Facebook-WebView", "Webview loading URL: " + url);
            super.onPageStarted(view, url, favicon);
            mSpinner.setTitle("Facebook");
            mSpinner.setMessage("Loading...");
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mSpinner.dismiss();
        }   
        
    }
}
