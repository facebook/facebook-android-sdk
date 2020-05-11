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

package com.example.places.model;

import android.text.TextUtils;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class OpeningHours {

  public static final int MONDAY = 0;
  public static final int TUESDAY = 1;
  public static final int WEDNESDAY = 2;
  public static final int THURSDAY = 3;
  public static final int FRIDAY = 4;
  public static final int SATURDAY = 5;
  public static final int SUNDAY = 6;

  private static final String[] DAY_PREFIX =
      new String[] {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

  private final SparseArray<List<String>> openingHours = new SparseArray<>();

  public List<String> getHoursInterval(int day) {
    return openingHours.get(day);
  }

  public static OpeningHours parse(Place place) {
    JSONObject jsonHours = place.getJson(Place.HOURS);
    if (jsonHours != null) {
      OpeningHours instance = new OpeningHours();
      for (int i = MONDAY; i <= SUNDAY; i++) {
        List<String> hoursIntervals = getOpeningHoursOfADay(jsonHours, DAY_PREFIX[i]);
        if (hoursIntervals != null) {
          instance.openingHours.put(i, hoursIntervals);
        }
      }
      return instance;
    }
    return null;
  }

  private static List<String> getOpeningHoursOfADay(JSONObject jsonHours, String dayPrefix) {
    List<String> hoursIntervals = null;
    String open1 = jsonHours.optString(dayPrefix + "_1_open");
    String close1 = jsonHours.optString(dayPrefix + "_1_close");

    if (!TextUtils.isEmpty(open1) && !TextUtils.isEmpty(close1)) {
      hoursIntervals = new ArrayList<>(4);
      hoursIntervals.add(open1);
      hoursIntervals.add(close1);

      String open2 = jsonHours.optString(dayPrefix + "_2_open");
      String close2 = jsonHours.optString(dayPrefix + "_2_close");

      if (!TextUtils.isEmpty(open2) && !TextUtils.isEmpty(close2)) {
        hoursIntervals.add(open2);
        hoursIntervals.add(close2);
      }
    }
    return hoursIntervals;
  }
}
