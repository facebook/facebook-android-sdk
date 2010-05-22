/**
 * 
 */
package com.facebook.stream;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;

abstract class AsyncRequestListener implements RequestListener {

	public void onComplete(String response) {
		try {
			JSONObject obj = Util.parseJson(response);
			onComplete(obj);
		} catch (JSONException e) {
			e.printStackTrace();
			onError(e.getMessage());
		}
		
	}
	
	public abstract void onComplete(JSONObject obj);

	public void onError(String error) {
		Log.e("app", "fail");
		
	}
}