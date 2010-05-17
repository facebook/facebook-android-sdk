package com.facebook.facedroid;

import android.app.Activity;

public abstract class Handler {
	
	protected Dispatcher dispatcher;
	
	public Handler() {
	}
	
	public void setDispatcher(Dispatcher webui) {
		this.dispatcher = webui;
	}
	
	public abstract void go();
	
	public Activity getActivity() {
		return dispatcher.getActivity();
	}
}
