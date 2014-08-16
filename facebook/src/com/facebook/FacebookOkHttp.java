package com.facebook;

import com.squareup.okhttp.OkHttpClient;

/**
 * Created by Adhi on 8/16/2014.
 */
public class FacebookOkHttp {

    static OkHttpClient client;

    public static OkHttpClient getClient() {
        return client;
    }

    public static void setClient(OkHttpClient client) {
        FacebookOkHttp.client = client;
    }
}
