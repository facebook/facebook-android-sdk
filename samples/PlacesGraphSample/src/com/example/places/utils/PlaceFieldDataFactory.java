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

import android.content.Context;
import com.example.places.model.Place;
import com.example.places.model.PlaceTextUtils;

public class PlaceFieldDataFactory {

  public static PlaceFieldData newPlaceField(Context context, String field, Place place) {

    if (!place.has(field)) {
      return null;
    }

    String text = PlaceTextUtils.getFieldValue(context, place, field);

    if (text == null) {
      return null;
    }
    PlaceFieldData.Type type = getPlaceFieldType(field);
    String title = PlaceTextUtils.getFieldName(context, field);

    return new PlaceFieldData(place, field, title, text, type);
  }

  private static PlaceFieldData.Type getPlaceFieldType(String field) {
    switch (field) {
      case Place.LOCATION:
        return PlaceFieldData.Type.MAP;
      case Place.APP_LINKS:
        return PlaceFieldData.Type.APP_LINK;
      case Place.PHONE:
        return PlaceFieldData.Type.PHONE;
      case Place.LINK:
      case Place.WEBSITE:
        return PlaceFieldData.Type.LINK;
      default:
        return PlaceFieldData.Type.TEXT;
    }
  }
}
