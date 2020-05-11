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

import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import com.example.places.model.Place;

public class PlaceFieldData {

  public enum Type {
    TEXT,
    LINK,
    PHONE,
    MAP,
    APP_LINK,
  }

  private Place place;
  private String field;
  private String title;
  private String text;
  private Type type;

  public PlaceFieldData(Place place, String field, String title, String text, Type type) {
    this.place = place;
    this.field = field;
    this.title = title;
    this.text = text;
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isClickable() {
    return type != Type.TEXT;
  }

  public Intent getActionIntent() {
    Intent intent = null;
    if (type == Type.LINK) {
      intent = new Intent(Intent.ACTION_VIEW);
      String url = text;
      if (!url.startsWith("http")) {
        url = "http://" + url;
      }
      intent.setData(Uri.parse(url));
    } else if (type == Type.MAP) {
      intent =
          new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + text));
    } else if (type == Type.PHONE) {
      String number = PhoneNumberUtils.stripSeparators(text);
      intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
    } else if (type == Type.APP_LINK) {
      intent = place.getAppLinkIntent("Facebook");
    }
    return intent;
  }
}
