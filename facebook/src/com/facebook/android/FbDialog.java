/**
 * 
 */
package com.facebook.android;

import com.facebook.android.Facebook.DialogListener;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout.LayoutParams;

public class FbDialog extends Dialog {
	
	private String mUrl;
	private String mData;
	private DialogListener mListener;
	private WebView mWebView;
	
    public FbDialog(Context context, String url, String data, DialogListener listener) {
		super(context);
		mUrl = url;
		mData = data;
		mListener = listener;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
	}

	private void initView() {
        mWebView = new WebView(getContext());
        mWebView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        mWebView.setWebViewClient(new FbDialog.FbWebViewClient());
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //mWebView.loadDataWithBaseURL(mUrl, mData, "text/html", "UTF-8", null);  // BUG: null pointer somewhere
        mWebView.loadUrl(mUrl);
        
        // extract title and size from data
        addContentView(mWebView, new LayoutParams(280, 360));
        setTitle("Facebook Rulz");
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
	    public void onPageFinished(WebView view, String url) {
	        super.onPageFinished(view, url);
	        Log.d("Facebook-WebView", "Loaded URL: " + url);	        
	        // HACK HACK HACK: oauth needs to be fixed on server-side
	        if (url.contains("auth_token=")) {
	    		mListener.onDialogSucceed(Util.parseUrl("http://success/#access_token=110862205611506%7Ce54333664a458cabe3ed8e3f-648474582%7CvcpLpG7BNiLF1QAyZgydkfQEBQU."));
				FbDialog.this.dismiss();
	        }
	    }
	}

}
