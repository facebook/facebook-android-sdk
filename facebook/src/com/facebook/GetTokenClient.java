package com.facebook;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;

final class GetTokenClient implements ServiceConnection {
    final Context context;
    final String applicationId;
    final Handler handler;
    CompletedListener listener;
    boolean running;
    Messenger sender;

    GetTokenClient(Context context, String applicationId) {
        Context applicationContext = context.getApplicationContext();

        this.context = (applicationContext != null) ? applicationContext : context;
        this.applicationId = applicationId;

        handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                GetTokenClient.this.handleMessage(message);
            }
        };
    }

    void setCompletedListener(CompletedListener listener) {
        this.listener = listener;
    }

    boolean start() {
        Intent intent = new Intent(NativeProtocol.INTENT_ACTION_PLATFORM_SERVICE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent = NativeProtocol.validateKatanaServiceIntent(context, intent);

        if (intent == null) {
            callback(null);
            return false;
        } else {
            running = true;
            context.bindService(intent, this, Context.BIND_AUTO_CREATE);
            return true;
        }
    }

    void cancel() {
        running = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        sender = new Messenger(service);
        getToken();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        sender = null;
        context.unbindService(this);
        callback(null);
    }

    private void getToken() {
        Bundle data = new Bundle();
        data.putString(NativeProtocol.EXTRA_APPLICATION_ID, applicationId);

        Message request = Message.obtain(null, NativeProtocol.MESSAGE_GET_ACCESS_TOKEN_REQUEST);
        request.arg1 = NativeProtocol.PROTOCOL_VERSION_20121101;
        request.setData(data);
        request.replyTo = new Messenger(handler);

        try {
            sender.send(request);
        } catch (RemoteException e) {
            callback(null);
        }
    }

    private void handleMessage(Message message) {
        if (message.what == NativeProtocol.MESSAGE_GET_ACCESS_TOKEN_REPLY) {
            Bundle extras = message.getData();
            String errorType = extras.getString(NativeProtocol.STATUS_ERROR_TYPE);
            if (errorType != null) {
                callback(null);
            } else {
                callback(extras);
            }
            context.unbindService(this);
        }
    }

    private void callback(Bundle result) {
        if (!running) {
            return;
        }
        running = false;

        CompletedListener callback = listener;
        if (callback != null) {
            callback.completed(result);
        }
    }

    interface CompletedListener {
        void completed(Bundle result);
    }
}
