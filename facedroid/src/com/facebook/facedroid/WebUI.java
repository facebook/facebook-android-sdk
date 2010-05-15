package com.facebook.facedroid;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class WebUI {

	private WebView webView;
	private Activity activity;
	LinearLayout layout;
	boolean rendered;
	
	public WebUI(Activity activity) {
		this.activity = activity;
		layout = new LinearLayout(activity);
		activity.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		rendered = false;
		render();
	}
	
	public void render() {
		if (rendered) {
			return;
		}
		webView = new WebView(activity);
	  	webView.setWebViewClient(new AppWebViewClient());
	  	webView.getSettings().setJavaScriptEnabled(true);
        layout.addView(webView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        rendered = true;
	}
	
	public void remove() {
		layout.removeView(webView);
		rendered = false;
	}
	
	public boolean isRendered() {
		return rendered;
	}
	
	public void loadData(String html) {
		webView.loadDataWithBaseURL("http://nada", html, "text/html", "utf8", "");
	}

	public void loadFile(String file) {
		webView.loadUrl(getFilePath(file));
	}

	public static String getFilePath(String file) {
		return "file:///android_asset/" + file;
	}
	
	public WebView getWebView() {
		return webView;
	}
	
	public Activity getActivity() {
		return activity;
	}
	
	public void onUrl(String url) {
		
	}
	
	private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Facebook-WebView", "Webview loading URL: " + url);
            if (url.startsWith("app://")) {
            	WebUI.this.onUrl(url);
            	return true;	
            }
            return false;
        }        
    }
		
}
