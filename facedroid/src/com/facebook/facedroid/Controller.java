package com.facebook.facedroid;

import android.app.Activity;

public abstract class Controller {
	
	protected WebUI webui;
	
	public Controller(WebUI webui) {
		this.webui = webui;
	}
	
	public abstract void render();
	
	public Activity getActivity() {
		return webui.getActivity();
	}
}
