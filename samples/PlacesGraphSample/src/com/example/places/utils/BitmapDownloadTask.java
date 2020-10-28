/*
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

package com.example.places.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

/**
 * In your applications, use Fresco for image download, caching and display.
 * https://github.com/facebook/fresco
 */
public class BitmapDownloadTask implements Runnable {

  private final String url;
  private final WeakReference<Listener> listenerWeakReference;

  public interface Listener {
    void onBitmapDownloadSuccess(String url, Bitmap bitmap);

    void onBitmapDownloadFailure(String url);
  }

  public BitmapDownloadTask(String url, Listener listener) {
    this.url = url;
    listenerWeakReference = new WeakReference<>(listener);
  }

  @Override
  public void run() {
    try {
      HttpURLConnection connection = (HttpURLConnection) new java.net.URL(url).openConnection();
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      Bitmap bitmap = BitmapFactory.decodeStream(input);
      Listener listener = listenerWeakReference.get();
      if (listener != null) {
        listener.onBitmapDownloadSuccess(url, bitmap);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      Listener listener = listenerWeakReference.get();
      if (listener != null) {
        listener.onBitmapDownloadFailure(url);
      }
    }
  }
}
