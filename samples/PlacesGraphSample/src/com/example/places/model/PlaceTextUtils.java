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

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import com.example.places.R;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlaceTextUtils {

  private static final int[] DAY_OF_WEEK_STRINGID =
      new int[] {
        R.string.monday,
        R.string.tuesday,
        R.string.wednesday,
        R.string.thursday,
        R.string.friday,
        R.string.saturday,
        R.string.sunday,
      };

  public static String getFieldName(Context context, String placeField) {
    Resources resources = context.getResources();
    if (Place.ABOUT.equals(placeField)) {
      return resources.getString(R.string.place_field_about);
    } else if (Place.APP_LINKS.equals(placeField)) {
      return resources.getString(R.string.place_field_app_link);
    } else if (Place.CATEGORY_LIST.equals(placeField)) {
      return resources.getString(R.string.place_field_categories);
    } else if (Place.CHECKINS.equals(placeField)) {
      return resources.getString(R.string.place_field_checkins);
    } else if (Place.DESCRIPTION.equals(placeField)) {
      return resources.getString(R.string.place_field_description);
    } else if (Place.ENGAGEMENT.equals(placeField)) {
      return resources.getString(R.string.place_field_engagement);
    } else if (Place.HOURS.equals(placeField)) {
      return resources.getString(R.string.place_field_hours);
    } else if (Place.LOCATION.equals(placeField)) {
      return resources.getString(R.string.place_field_address);
    } else if (Place.LINK.equals(placeField)) {
      return resources.getString(R.string.place_field_link);
    } else if (Place.OVERALL_STAR_RATING.equals(placeField)) {
      return resources.getString(R.string.place_field_rating);
    } else if (Place.PARKING.equals(placeField)) {
      return resources.getString(R.string.place_field_parking);
    } else if (Place.PAYMENT_OPTIONS.equals(placeField)) {
      return resources.getString(R.string.place_field_payment_options);
    } else if (Place.PHONE.equals(placeField)) {
      return resources.getString(R.string.place_field_phone);
    } else if (Place.PRICE_RANGE.equals(placeField)) {
      return resources.getString(R.string.place_field_price_range);
    } else if (Place.RATING_COUNT.equals(placeField)) {
      return resources.getString(R.string.place_field_rating_count);
    } else if (Place.RESTAURANT_SPECIALTIES.equals(placeField)) {
      return resources.getString(R.string.place_field_specialties);
    } else if (Place.RESTAURANT_SERVICES.equals(placeField)) {
      return resources.getString(R.string.place_field_services);
    } else if (Place.WEBSITE.equals(placeField)) {
      return resources.getString(R.string.place_field_website);
    }
    return null;
  }

  public static String getFieldValue(Context context, Place place, String field) {
    Resources resources = context.getResources();
    if (Place.CATEGORY_LIST.equals(field)) {
      List<String> categories = getCategories(place);
      return TextUtils.join(", ", categories);
    } else if (Place.LOCATION.equals(field)) {
      return getAddress(place);
    } else if (Place.PHONE.equals(field)) {
      return place.get(field);
    } else if (Place.WEBSITE.equals(field)) {
      return place.get(field);
    } else if (Place.LINK.equals(field)) {
      return place.get(field);
    } else if (Place.HOURS.equals(field)) {
      return getOpeningHours(context, place);
    } else if (Place.DESCRIPTION.equals(field)) {
      return place.get(field);
    } else if (Place.ABOUT.equals(field)) {
      return place.get(field);
    } else if (Place.CHECKINS.equals(field)) {
      int checkins = place.getInt(field);
      return resources.getString(R.string.place_info_checkins, checkins);
    } else if (Place.OVERALL_STAR_RATING.equals(field)) {
      String rating = place.get(Place.OVERALL_STAR_RATING);
      int ratingCount = place.getInt(Place.RATING_COUNT);
      if (!TextUtils.isEmpty(rating) && ratingCount > 0) {
        return resources.getString(R.string.place_info_rating, rating, ratingCount);
      }
    } else if (Place.ENGAGEMENT.equals(field)) {
      JSONObject jsonObject = place.getJson(Place.ENGAGEMENT);
      if (jsonObject != null) {
        return jsonObject.optString("social_sentence");
      }
    } else if (Place.RESTAURANT_SPECIALTIES.equals(field)) {
      List<String> specialties = getRestaurantSpecialties(context, place);
      if (!specialties.isEmpty()) {
        return TextUtils.join(", ", specialties);
      }
    } else if (Place.PRICE_RANGE.equals(field)) {
      return place.get(Place.PRICE_RANGE);
    } else if (Place.IS_ALWAYS_OPEN.equals(field)) {
      boolean isAlwaysOpen = place.getBoolean(field);
      if (isAlwaysOpen) {
        return resources.getString(R.string.place_always_open);
      }
    } else if (Place.IS_PERMANENTLY_CLOSED.equals(field)) {
      boolean isPermanentlyClosed = place.getBoolean(field);
      if (isPermanentlyClosed) {
        return resources.getString(R.string.place_permanently_closed);
      }
    } else if (Place.APP_LINKS.equals(field)) {
      if (hasFacebookAppLink(place)) {
        return resources.getString(R.string.place_app_link);
      }
    } else if (Place.PARKING.equals(field)) {
      List<String> parking = getParking(context, place);
      if (!parking.isEmpty()) {
        return TextUtils.join(", ", parking);
      }
    } else if (Place.RESTAURANT_SERVICES.equals(field)) {
      List<String> services = getRestaurantServices(context, place);
      if (!services.isEmpty()) {
        return TextUtils.join(", ", services);
      }
    } else if (Place.PAYMENT_OPTIONS.equals(field)) {
      List<String> paymentOptions = getPaymentOptions(context, place);
      if (!paymentOptions.isEmpty()) {
        return TextUtils.join(", ", paymentOptions);
      }
    }
    return null;
  }

  public static String getAddress(Place place) {
    if (place.has(Place.SINGLE_LINE_ADDRESS)) {
      return place.get(Place.SINGLE_LINE_ADDRESS);
    } else if (place.has(Place.LOCATION)) {
      JSONObject location = place.getJson(Place.LOCATION);
      List<String> address = new ArrayList<>(5);
      String street = location.optString("street");
      if (!TextUtils.isEmpty(street)) {
        address.add(street);
      }
      String city = location.optString("city");
      if (!TextUtils.isEmpty(city)) {
        address.add(city);
      }
      String state = location.optString("state");
      if (!TextUtils.isEmpty(state)) {
        address.add(state);
      }
      String country = location.optString("city");
      if (!TextUtils.isEmpty(country)) {
        address.add(country);
      }
      return TextUtils.join(", ", address);
    }
    return null;
  }

  public static List<String> getCategories(Place place) {
    JSONObject jsonData = place.getJson();
    List<String> categories = new ArrayList<>();
    JSONArray jsonCategories = jsonData.optJSONArray(Place.CATEGORY_LIST);
    if (jsonCategories != null) {
      int length = jsonCategories.length();
      for (int i = 0; i < length; i++) {
        JSONObject jsonCategory = jsonCategories.optJSONObject(i);
        if (jsonCategory != null) {
          String category = jsonCategory.optString("name");
          categories.add(category);
        }
      }
    }
    return categories;
  }

  public static String getOpeningHours(Context context, Place place) {
    OpeningHours hours = place.getOpeningHours();
    if (hours != null) {
      StringBuilder openingHours = new StringBuilder();
      for (int day = OpeningHours.MONDAY; day < OpeningHours.SUNDAY; day++) {
        String hourIntervalText = getOpeningHourText(context.getResources(), hours, day);
        if (hourIntervalText != null) {
          if (openingHours.length() > 0) {
            openingHours.append("\n");
          }
          openingHours.append(hourIntervalText);
        }
      }
      if (openingHours.length() > 0) {
        return openingHours.toString();
      }
    }
    return null;
  }

  private static String getOpeningHourText(Resources resources, OpeningHours hours, int day) {
    List<String> hourInterval = hours.getHoursInterval(day);
    if (hourInterval != null) {
      StringBuilder builder = new StringBuilder();
      if (hourInterval.size() >= 2) {
        builder.append(hourInterval.get(0));
        builder.append(' ');
        builder.append(hourInterval.get(1));
      }
      if (hourInterval.size() == 4) {
        builder.append(", ");
        builder.append(hourInterval.get(2));
        builder.append(' ');
        builder.append(hourInterval.get(3));
      }
      if (hourInterval.size() > 0) {
        builder.append(" - ");
        builder.append(resources.getString(DAY_OF_WEEK_STRINGID[day]));
        return builder.toString();
      }
    }
    return null;
  }

  public static List<String> getPaymentOptions(Context context, Place place) {
    return extractValidValues(
        context,
        place.getJson(Place.PAYMENT_OPTIONS),
        R.array.payment_options_keys,
        R.array.payment_options_labels);
  }

  public static List<String> getParking(Context context, Place place) {
    return extractValidValues(
        context, place.getJson(Place.PARKING), R.array.parking_keys, R.array.parking_labels);
  }

  public static List<String> getRestaurantSpecialties(Context context, Place place) {
    return extractValidValues(
        context,
        place.getJson(Place.RESTAURANT_SPECIALTIES),
        R.array.restaurant_specialties_keys,
        R.array.restaurant_specialties_labels);
  }

  public static List<String> getRestaurantServices(Context context, Place place) {
    return extractValidValues(
        context,
        place.getJson(Place.RESTAURANT_SERVICES),
        R.array.restaurant_services_keys,
        R.array.restaurant_services_labels);
  }

  private static List<String> extractValidValues(
      Context context, JSONObject jsonObject, int keysId, int labelsId) {
    List<String> labels = new ArrayList<>();
    if (jsonObject != null) {
      String[] keys = context.getResources().getStringArray(keysId);
      String[] labelArray = context.getResources().getStringArray(labelsId);
      for (int i = 0; i < keys.length; i++) {
        int value = jsonObject.optInt(keys[i]);
        if (value == 1) {
          labels.add(labelArray[i]);
        }
      }
    }
    return labels;
  }

  private static boolean hasFacebookAppLink(Place place) {
    List<AppLink> appLinks = place.getAppLinks();
    for (AppLink appLink : appLinks) {
      if ("Facebook".equals(appLink.getAppName())) {
        return true;
      }
    }
    return false;
  }
}
