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

import android.app.Activity;
import android.webkit.WebView;

/**
 * An abstract superclass for handlers. Handlers are similar to
 * controllers in traditional web apps. Each page has a handler
 * that is responsible for rendering the page.
 * 
 * @author yariv
 */
public abstract class Handler {

    // The app's dispatcher.
    protected Dispatcher dispatcher;

    /**
     * The dispatcher calls this method when the Handler
     * is expected to render its page.
     */
    public abstract void go();

    /**
     * A setter for the dispatcher.
     * 
     * @param dispatcher
     */
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Returns the dispatcher.
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }


    /**
     * Returns the dispatcher's WebView
     */
    public WebView getWebView() {
        return dispatcher.getWebView();
    }

    /**
     * Returns the dispatcher's Activity
     */
    public Activity getActivity() {
        return dispatcher.getActivity();
    }
}
