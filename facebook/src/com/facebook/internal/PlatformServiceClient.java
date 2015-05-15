/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
abstract public class PlatformServiceClient implements ServiceConnection {
    private final Context context;
    private final Handler handler;
    private CompletedListener listener;
    private boolean running;
    private Messenger sender;
    private int requestMessage;
    private int replyMessage;
    private final String applicationId;
    private final int protocolVersion;

    public PlatformServiceClient(
            Context context,
            int requestMessage,
            int replyMessage,
            int protocolVersion,
            String applicationId) {
        Context applicationContext = context.getApplicationContext();

        this.context = (applicationContext != null) ? applicationContext : context;
        this.requestMessage = requestMessage;
        this.replyMessage = replyMessage;
        this.applicationId = applicationId;
        this.protocolVersion = protocolVersion;

        handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                PlatformServiceClient.this.handleMessage(message);
            }
        };
    }

    public void setCompletedListener(CompletedListener listener) {
        this.listener = listener;
    }

    protected Context getContext() {
        return context;
    }

    public boolean start() {
        if (running) {
            return false;
        }

        // Make sure that the service can handle the requested protocol version
        int availableVersion = NativeProtocol.getLatestAvailableProtocolVersionForService(
                protocolVersion);
        if (availableVersion == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
            return false;
        }

        Intent intent = NativeProtocol.createPlatformServiceIntent(context);
        if (intent == null) {
            return false;
        } else {
            running = true;
            context.bindService(intent, this, Context.BIND_AUTO_CREATE);
            return true;
        }
    }

    public void cancel() {
        running = false;
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        sender = new Messenger(service);
        sendMessage();
    }

    public void onServiceDisconnected(ComponentName name) {
        sender = null;
        try {
            context.unbindService(this);
        } catch (IllegalArgumentException ex) {
            // Do nothing, the connection was already unbound
        }
        callback(null);
    }

    private void sendMessage() {
        Bundle data = new Bundle();
        data.putString(NativeProtocol.EXTRA_APPLICATION_ID, applicationId);

        populateRequestBundle(data);

        Message request = Message.obtain(null, requestMessage);
        request.arg1 = protocolVersion;
        request.setData(data);
        request.replyTo = new Messenger(handler);

        try {
            sender.send(request);
        } catch (RemoteException e) {
            callback(null);
        }
    }

    protected abstract void populateRequestBundle(Bundle data);

    protected void handleMessage(Message message) {
        if (message.what == replyMessage) {
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

    public interface CompletedListener {
        void completed(Bundle result);
    }
}
