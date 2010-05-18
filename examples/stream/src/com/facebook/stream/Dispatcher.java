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

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * Handles rendering the WebView instance and
 * mapping urls to Handlers.  
 * 
 * @author yariv
 */
public class Dispatcher {

	private WebView webView;
	private Activity activity;
	LinearLayout layout;
	boolean rendered;
	HashMap<String, Class> handlers;
	
	public Dispatcher(Activity activity) {
		this.activity = activity;
		handlers = new HashMap<String, Class>();
		layout = new LinearLayout(activity);
		activity.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		rendered = false;
		renderWebView();
	}
	
	public void addHandler(String name, Class clazz) {
		this.handlers.put(name, clazz);
	}
	
	public void runHandler(String name) {
		Class clazz = handlers.get(name);
		if (clazz != null) {
			try {
				Handler handler = (Handler)clazz.newInstance();
				handler.setDispatcher(this);
				handler.go();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void renderWebView() {
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
	
	private void onUrl(String url) {
		// the url has the form app://[handleName]
		String handlerName = url.substring(6);
		runHandler(handlerName);
	}
	

	private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("Facebook-WebView", "Webview loading URL: " + url);
            if (url.startsWith("app://")) {
            	Dispatcher.this.onUrl(url);
            	return true;	
            }
            return false;
        }        
    }
		
}
