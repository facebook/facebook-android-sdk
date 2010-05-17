package com.facebook.facedroid;

import android.app.Activity;
import android.webkit.WebView;

public abstract class Handler {
	
	protected Dispatcher dispatcher;
	
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	public abstract void go();
	
	public Dispatcher getDispatcher() {
		return dispatcher;
	}
	
	public WebView getWebView() {
		return dispatcher.getWebView();
	}
	
	public Activity getActivity() {
		return dispatcher.getActivity();
	}
}
