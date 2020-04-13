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

package com.facebook.login;

import android.net.Uri;
import android.content.ComponentName;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsSession;
import android.support.customtabs.CustomTabsServiceConnection;

public class CustomTabPrefetchHelper extends CustomTabsServiceConnection {

  private static CustomTabsClient client = null;
  private static CustomTabsSession session = null;

  private static void prepareSession() {
    if (session == null) {
      if (client != null) {
        session = client.newSession(null);
      }
    }
  }
  public static void mayLaunchUrl(Uri url) {
    if (session == null) {
      prepareSession();
    }
    if (session != null) {
      session.mayLaunchUrl(url, null, null);
    }
  }

  public static CustomTabsSession getPreparedSessionOnce() {
    CustomTabsSession result = session;
    session = null;
    return result;
  }

  @Override
  public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient newClient) {
    client = newClient;
    client.warmup(0);
    prepareSession();
  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {}
}
