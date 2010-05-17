/**
 * 
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        addContentView(mWebView, new LayoutParams(WIDTH, HEIGHT));
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mListener.onDialogCancel();
    }

    private class FbWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Facebook-WebView", "Webview loading URL: " + url);
            if (url.startsWith(Facebook.SUCCESS_URI)) {
                mListener.onDialogSucceed(Util.parseUrl(url));
                FbDialog.this.dismiss();
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onDialogFail(failingUrl + " failed: " + description);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
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
