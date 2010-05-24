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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * Handles the rendering of the WebView instance and the
 * mapping of app:// urls to their appropriate Handlers.  
 * 
 * @author yariv
 */
public class Dispatcher {

    // The WebView instance
    private WebView webView;

    // The app's main Activity
    private Activity activity;

    // Contains the webView object
    LinearLayout layout;

    // Has the webView been rendered?
    boolean isWebViewShown;

    // Holds mappings between handler names to their classes
    // (e.g. "login" -> LoginHandler.class)
    HashMap<String, Class> handlers;

    public Dispatcher(Activity activity) {
        this.activity = activity;
        handlers = new HashMap<String, Class>();
        layout = new LinearLayout(activity);
        activity.addContentView(
                layout, new LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        isWebViewShown = false;
        showWebView();
    }

    /**
     * Adds a handler name to handler class mapping. This should be called
     * for each handler when the application starts up.
     * 
     * @param name
     * @param clazz
     */
    public void addHandler(String name, Class clazz) {
        this.handlers.put(name, clazz);
    }

    /**
     * Executes the handler associated with the given name. For example,
     * dispatcher.runHandler("login") would render the Login page in the
     * WebView instance.
     * 
     * @param name
     */
    public void runHandler(String name) {
        Class clazz = handlers.get(name);
        if (clazz != null) {
            try {
                Handler handler = (Handler)clazz.newInstance();
                handler.setDispatcher(this);
                handler.go();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Show the app's WebView instance.
     */
    public void showWebView() {
        if (isWebViewShown) {
            return;
        }
        webView = new WebView(activity);
        webView.setWebViewClient(new AppWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        layout.addView(webView,
                new LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        isWebViewShown = true;
    }

    /**
     * Hide the app's WebView instance. This should be called if the
     * WebView instance is visible and the app wants to open another
     * WebView instance (e.g. for a Facebook dialog). Android doesn't
     * seem to be able to handle more than one WebView instance per
     * application.
     */
    public void hideWebView() {
        layout.removeView(webView);
        isWebViewShown = false;
    }

    /**
     * Returns true if the WebView instance is visible.
     */
    public boolean isWebViewShown() {
        return isWebViewShown;
    }

    /**
     * Loads the html string into the WebView instance.
     * 
     * @param html
     */
    public void loadData(String html) {
        webView.loadDataWithBaseURL(
                "http://nada", html, "text/html", "utf8", "");
    }

    /**
     * Loads a file from the assets directory into the
     * WebView instance.
     * 
     * @param file
     */
    public void loadFile(String file) {
        webView.loadUrl(getAbsoluteUrl(file));
    }

    /**
     * Returns the absolute URL for a local file.
     * 
     * @param file
     */
    public static String getAbsoluteUrl(String file) {
        return "file:///android_asset/" + file;
    }

    /**
     * Returns the Dispatcher's WebView instance.
     */
    public WebView getWebView() {
        return webView;
    }

    /**
     * Returns the Dispatcher's Activity
     */
    public Activity getActivity() {
        return activity;
    }


    /**
     * Enables the mapping of app:// urls to Handlers.
     * 
     * @author yariv
     */
    private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("app://")) {
                String handlerName = url.substring(6);
                runHandler(handlerName);
                return true;	
            }
            return false;
        }        
    }

}
