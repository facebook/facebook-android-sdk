package com.facebook.internal;

import android.content.Context;
import android.os.Bundle;
import com.facebook.widget.FacebookDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public class PendingCallStore {
    private static final String CALL_ID_ARRAY_KEY = "com.facebook.internal.PendingCallStore.callIdArrayKey";
    private static final String CALL_KEY_PREFIX = "com.facebook.internal.PendingCallStore.";

    private static PendingCallStore mInstance;

    private Map<String, FacebookDialog.PendingCall> pendingCallMap = new HashMap<String, FacebookDialog.PendingCall>();

    public static PendingCallStore getInstance() {
        if (mInstance == null) {
            createInstance();
        }

        return mInstance;
    }

    private synchronized static void createInstance() {
        if (mInstance == null) {
            mInstance = new PendingCallStore();
        }
    }

    public void trackPendingCall(FacebookDialog.PendingCall pendingCall) {
        if (pendingCall != null) {
            pendingCallMap.put(pendingCall.getCallId().toString(), pendingCall);
        }
    }

    public void stopTrackingPendingCall(UUID callId) {
        if (callId != null) {
            pendingCallMap.remove(callId.toString());
        }
    }

    public FacebookDialog.PendingCall getPendingCallById(UUID callId) {
        if (callId == null) {
            return null;
        }
        return pendingCallMap.get(callId.toString());
    }

    public void saveInstanceState(Bundle outState) {
        ArrayList<String> callIds = new ArrayList<String>(pendingCallMap.keySet());
        outState.putStringArrayList(CALL_ID_ARRAY_KEY, callIds);

        for(FacebookDialog.PendingCall pendingCall : pendingCallMap.values()) {
            String stateKey = getSavedStateKeyForPendingCallId(pendingCall.getCallId().toString());
            outState.putParcelable(stateKey, pendingCall);
        }
    }

    public void restoreFromSavedInstanceState(Bundle savedInstanceState) {
        ArrayList<String> callIds = savedInstanceState.getStringArrayList(CALL_ID_ARRAY_KEY);
        if (callIds != null) {
            for (String callId : callIds) {
                String stateKey = getSavedStateKeyForPendingCallId(callId);
                FacebookDialog.PendingCall pendingCall = savedInstanceState.getParcelable(stateKey);

                if (pendingCall != null) {
                    pendingCallMap.put(pendingCall.getCallId().toString(), pendingCall);
                }
            }
        }
    }

    private String getSavedStateKeyForPendingCallId(String pendingCallId) {
        return CALL_KEY_PREFIX + pendingCallId;
    }
}
