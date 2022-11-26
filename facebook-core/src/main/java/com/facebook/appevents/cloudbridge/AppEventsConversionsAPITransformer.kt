/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.cloudbridge

import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.internal.Logger
import com.facebook.internal.Utility.convertJSONArrayToList
import com.facebook.internal.Utility.convertJSONObjectToHashMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// MARK: App parameters
// ======================================//======================================//======================================

enum class CustomEventField(val rawValue: String) {
  EVENT_TIME("_logTime"), // top level
  EVENT_NAME("_eventName"), // top level
  VALUE_TO_SUM("_valueToSum"),
  CONTENT_IDS("fb_content_id"),
  CONTENTS("fb_content"),
  CONTENT_TYPE("fb_content_type"),
  DESCRIPTION("fb_description"),
  LEVEL("fb_level"),
  MAX_RATING_VALUE("fb_max_rating_value"),
  NUM_ITEMS("fb_num_items"),
  PAYMENT_INFO_AVAILABLE("fb_payment_info_available"),
  REGISTRATION_METHOD("fb_registration_method"),
  SEARCH_STRING("fb_search_string"),
  SUCCESS("fb_success"),
  ORDER_ID("fb_order_id"),
  AD_TYPE("ad_type"),
  CURRENCY("fb_currency");

  internal companion object {
    operator fun invoke(rawValue: String): CustomEventField? =
        values().firstOrNull { it.rawValue == rawValue }
  }
}

enum class AppEventType {
  MOBILE_APP_INSTALL,
  CUSTOM,
  OTHER;

  internal companion object {
    operator fun invoke(rawValue: String): AppEventType = run {
      when (rawValue) {
        "MOBILE_APP_INSTALL" -> MOBILE_APP_INSTALL
        "CUSTOM_APP_EVENTS" -> CUSTOM
        else -> OTHER
      }
    }
  }
}

enum class AppEventUserAndAppDataField(val rawValue: String) {
  // user data fields
  ANON_ID("anon_id"),
  APP_USER_ID("app_user_id"),
  ADVERTISER_ID("advertiser_id"),
  PAGE_ID("page_id"),
  PAGE_SCOPED_USER_ID("page_scoped_user_id"),
  USER_DATA("ud"),

  // app data fields
  ADV_TE("advertiser_tracking_enabled"),
  APP_TE("application_tracking_enabled"),
  CONSIDER_VIEWS("consider_views"),
  DEVICE_TOKEN("device_token"),
  EXT_INFO("extInfo"),
  INCLUDE_DWELL_DATA("include_dwell_data"),
  INCLUDE_VIDEO_DATA("include_video_data"),
  INSTALL_REFERRER("install_referrer"),
  INSTALLER_PACKAGE("installer_package"),
  RECEIPT_DATA("receipt_data"),
  URL_SCHEMES("url_schemes");

  internal companion object {
    operator fun invoke(rawValue: String): AppEventUserAndAppDataField? =
        values().firstOrNull { it.rawValue == rawValue }
  }
}

// ======================================//======================================//======================================

// MARK: ConversionsAPI parameter JSON section
enum class ConversionsAPISection(val rawValue: String) {
  USER_DATA("user_data"),
  APP_DATA("app_data"),
  CUSTOM_DATA("custom_data"),
  CUSTOM_EVENTS("custom_events")
}

enum class ConversionsAPICustomEventField(val rawValue: String) {
  VALUE_TO_SUM("value"),
  EVENT_TIME("event_time"),
  EVENT_NAME("event_name"),
  CONTENT_IDS("content_ids"),
  CONTENTS("contents"),
  CONTENT_TYPE("content_type"),
  DESCRIPTION("description"),
  LEVEL("level"),
  MAX_RATING_VALUE("max_rating_value"),
  NUM_ITEMS("num_items"),
  PAYMENT_INFO_AVAILABLE("payment_info_available"),
  REGISTRATION_METHOD("registration_method"),
  SEARCH_STRING("search_string"),
  SUCCESS("success"),
  ORDER_ID("order_id"),
  AD_TYPE("ad_type"),
  CURRENCY("currency")
}

enum class ConversionsAPIUserAndAppDataField(val rawValue: String) {
  ANON_ID("anon_id"),
  FB_LOGIN_ID("fb_login_id"),
  MAD_ID("madid"),
  PAGE_ID("page_id"),
  PAGE_SCOPED_USER_ID("page_scoped_user_id"),
  USER_DATA("ud"),
  ADV_TE("advertiser_tracking_enabled"),
  APP_TE("application_tracking_enabled"),
  CONSIDER_VIEWS("consider_views"),
  DEVICE_TOKEN("device_token"),
  EXT_INFO("extInfo"),
  INCLUDE_DWELL_DATA("include_dwell_data"),
  INCLUDE_VIDEO_DATA("include_video_data"),
  INSTALL_REFERRER("install_referrer"),
  INSTALLER_PACKAGE("installer_package"),
  RECEIPT_DATA("receipt_data"),
  URL_SCHEMES("url_schemes")
}

enum class ConversionsAPIEventName(val rawValue: String) {
  UNLOCKED_ACHIEVEMENT("AchievementUnlocked"),
  ACTIVATED_APP("ActivateApp"),
  ADDED_PAYMENT_INFO("AddPaymentInfo"),
  ADDED_TO_CART("AddToCart"),
  ADDED_TO_WISHLIST("AddToWishlist"),
  COMPLETED_REGISTRATION("CompleteRegistration"),
  VIEWED_CONTENT("ViewContent"),
  INITIATED_CHECKOUT("InitiateCheckout"),
  ACHIEVED_LEVEL("LevelAchieved"),
  PURCHASED("Purchase"),
  RATED("Rate"),
  SEARCHED("Search"),
  SPENT_CREDITS("SpentCredits"),
  COMPLETED_TUTORIAL("TutorialCompletion"),
}

enum class OtherEventConstants(val rawValue: String) {
  EVENT("event"),
  ACTION_SOURCE("action_source"),
  APP("app"),
  MOBILE_APP_INSTALL("MobileAppInstall"),
  INSTALL_EVENT_TIME("install_timestamp")
}

// ======================================//======================================//======================================

// MARK: App Events to Conversions API Transformer dictionaries
object AppEventsConversionsAPITransformer {

  const val TAG = "AppEventsConversionsAPITransformer"

  data class SectionFieldMapping(
      var section: ConversionsAPISection,
      var field: ConversionsAPIUserAndAppDataField?
  )

  private val topLevelTransformations =
      mapOf(
          // user_data mapping
          AppEventUserAndAppDataField.ANON_ID to
              SectionFieldMapping(
                  ConversionsAPISection.USER_DATA, ConversionsAPIUserAndAppDataField.ANON_ID),
          AppEventUserAndAppDataField.APP_USER_ID to
              SectionFieldMapping(
                  ConversionsAPISection.USER_DATA, ConversionsAPIUserAndAppDataField.FB_LOGIN_ID),
          AppEventUserAndAppDataField.ADVERTISER_ID to
              SectionFieldMapping(
                  ConversionsAPISection.USER_DATA, ConversionsAPIUserAndAppDataField.MAD_ID),
          AppEventUserAndAppDataField.PAGE_ID to
              SectionFieldMapping(
                  ConversionsAPISection.USER_DATA, ConversionsAPIUserAndAppDataField.PAGE_ID),
          AppEventUserAndAppDataField.PAGE_SCOPED_USER_ID to
              SectionFieldMapping(
                  ConversionsAPISection.USER_DATA,
                  ConversionsAPIUserAndAppDataField.PAGE_SCOPED_USER_ID),

          // app_data mapping
          AppEventUserAndAppDataField.ADV_TE to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.ADV_TE),
          AppEventUserAndAppDataField.APP_TE to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.APP_TE),
          AppEventUserAndAppDataField.CONSIDER_VIEWS to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.CONSIDER_VIEWS),
          AppEventUserAndAppDataField.DEVICE_TOKEN to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.DEVICE_TOKEN),
          AppEventUserAndAppDataField.EXT_INFO to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.EXT_INFO),
          AppEventUserAndAppDataField.INCLUDE_DWELL_DATA to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA,
                  ConversionsAPIUserAndAppDataField.INCLUDE_DWELL_DATA),
          AppEventUserAndAppDataField.INCLUDE_VIDEO_DATA to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA,
                  ConversionsAPIUserAndAppDataField.INCLUDE_VIDEO_DATA),
          AppEventUserAndAppDataField.INSTALL_REFERRER to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA,
                  ConversionsAPIUserAndAppDataField.INSTALL_REFERRER),
          AppEventUserAndAppDataField.INSTALLER_PACKAGE to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA,
                  ConversionsAPIUserAndAppDataField.INSTALLER_PACKAGE),
          AppEventUserAndAppDataField.RECEIPT_DATA to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.RECEIPT_DATA),
          AppEventUserAndAppDataField.URL_SCHEMES to
              SectionFieldMapping(
                  ConversionsAPISection.APP_DATA, ConversionsAPIUserAndAppDataField.URL_SCHEMES),
          AppEventUserAndAppDataField.USER_DATA to
              SectionFieldMapping(ConversionsAPISection.USER_DATA, null),
      )

  data class SectionCustomEventFieldMapping(
      var section: ConversionsAPISection?,
      var field: ConversionsAPICustomEventField
  )

  @JvmField
  val customEventTransformations =
      mapOf(
          // custom_events mapping
          CustomEventField.EVENT_TIME to
              SectionCustomEventFieldMapping(null, ConversionsAPICustomEventField.EVENT_TIME),
          CustomEventField.EVENT_NAME to
              SectionCustomEventFieldMapping(null, ConversionsAPICustomEventField.EVENT_NAME),
          CustomEventField.VALUE_TO_SUM to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.VALUE_TO_SUM),
          CustomEventField.CONTENT_IDS to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA,
                  ConversionsAPICustomEventField
                      .CONTENT_IDS), // string to array conversion required
          CustomEventField.CONTENTS to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA,
                  ConversionsAPICustomEventField
                      .CONTENTS), // string to array conversion required, contents has an extra
          // field: price
          CustomEventField.CONTENT_TYPE to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.CONTENT_TYPE),
          CustomEventField.CURRENCY to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.CURRENCY),
          CustomEventField.DESCRIPTION to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.DESCRIPTION),
          CustomEventField.LEVEL to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.LEVEL),
          CustomEventField.MAX_RATING_VALUE to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA,
                  ConversionsAPICustomEventField.MAX_RATING_VALUE),
          CustomEventField.NUM_ITEMS to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.NUM_ITEMS),
          CustomEventField.PAYMENT_INFO_AVAILABLE to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA,
                  ConversionsAPICustomEventField.PAYMENT_INFO_AVAILABLE),
          CustomEventField.REGISTRATION_METHOD to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA,
                  ConversionsAPICustomEventField.REGISTRATION_METHOD),
          CustomEventField.SEARCH_STRING to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.SEARCH_STRING),
          CustomEventField.SUCCESS to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.SUCCESS),
          CustomEventField.ORDER_ID to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.ORDER_ID),
          CustomEventField.AD_TYPE to
              SectionCustomEventFieldMapping(
                  ConversionsAPISection.CUSTOM_DATA, ConversionsAPICustomEventField.AD_TYPE))

  @JvmField
  val standardEventTransformations =
      mapOf(
          AppEventsConstants.EVENT_NAME_UNLOCKED_ACHIEVEMENT to
              ConversionsAPIEventName.UNLOCKED_ACHIEVEMENT, // "achievementUnlocked",
          AppEventsConstants.EVENT_NAME_ACTIVATED_APP to ConversionsAPIEventName.ACTIVATED_APP,
          AppEventsConstants.EVENT_NAME_ADDED_PAYMENT_INFO to
              ConversionsAPIEventName.ADDED_PAYMENT_INFO,
          AppEventsConstants.EVENT_NAME_ADDED_TO_CART to ConversionsAPIEventName.ADDED_TO_CART,
          AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST to
              ConversionsAPIEventName.ADDED_TO_WISHLIST,
          AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION to
              ConversionsAPIEventName.COMPLETED_REGISTRATION,
          AppEventsConstants.EVENT_NAME_VIEWED_CONTENT to ConversionsAPIEventName.VIEWED_CONTENT,
          AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT to
              ConversionsAPIEventName.INITIATED_CHECKOUT,
          AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL to ConversionsAPIEventName.ACHIEVED_LEVEL,
          AppEventsConstants.EVENT_NAME_PURCHASED to
              ConversionsAPIEventName.PURCHASED, // fb_mobile_purchase
          AppEventsConstants.EVENT_NAME_RATED to ConversionsAPIEventName.RATED,
          AppEventsConstants.EVENT_NAME_SEARCHED to ConversionsAPIEventName.SEARCHED,
          AppEventsConstants.EVENT_NAME_SPENT_CREDITS to ConversionsAPIEventName.SPENT_CREDITS,
          AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL to
              ConversionsAPIEventName.COMPLETED_TUTORIAL,
      )

  enum class DataProcessingParameterName(val rawValue: String) {
    OPTIONS("data_processing_options"),
    COUNTRY("data_processing_options_country"),
    STATE("data_processing_options_state");

    internal companion object {
      operator fun invoke(rawValue: String): DataProcessingParameterName? =
          values().firstOrNull { it.rawValue == rawValue }
    }
  }

  enum class ValueTransformationType {
    ARRAY,
    BOOL,
    INT;

    internal companion object {
      operator fun invoke(rawValue: String): ValueTransformationType? = run {
        when (rawValue) {
          AppEventUserAndAppDataField.EXT_INFO.rawValue -> ARRAY
          AppEventUserAndAppDataField.URL_SCHEMES.rawValue -> ARRAY
          CustomEventField.CONTENT_IDS.rawValue -> ARRAY
          CustomEventField.CONTENTS.rawValue -> ARRAY
          DataProcessingParameterName.OPTIONS.rawValue -> ARRAY
          AppEventUserAndAppDataField.ADV_TE.rawValue -> BOOL
          AppEventUserAndAppDataField.APP_TE.rawValue -> BOOL
          CustomEventField.EVENT_TIME.rawValue -> INT
          else -> null
        }
      }
    }
  }

  @JvmStatic
  internal fun transformValue(field: String, value: Any): Any? {
    val type = ValueTransformationType(field)
    val param = value as? String

    if (type == null || param == null) {
      return value
    }
    when (type) {
      ValueTransformationType.ARRAY -> {
        return try {
          val strList = convertJSONArrayToList(JSONArray(param))
          val arrayOfMaps = arrayListOf<Any?>()
          strList.forEach { str ->
            val final =
                try {
                  convertJSONObjectToHashMap(JSONObject(str))
                } catch (e: JSONException) {
                  // OK Ignore as we assume that it can be checked as another value type
                  try {
                    convertJSONArrayToList(JSONArray(str))
                  } catch (e: JSONException) {
                    // OK Ignore as we assume that it can be checked as another value type
                    str
                  }
                }
            arrayOfMaps.add(final)
          }
          return arrayOfMaps
        } catch (e: JSONException) {
          Logger.log(
              LoggingBehavior.APP_EVENTS,
              TAG,
              "\n transformEvents JSONException: \n%s\n%s",
              value,
              e)
        }
      }
      ValueTransformationType.BOOL -> {
        val coercedInteger = param.toString().toIntOrNull()
        return if (coercedInteger != null) {
          coercedInteger != 0
        } else {
          null
        }
      }
      ValueTransformationType.INT -> {
        return value.toString().toIntOrNull()
      }
    }
  }

  @JvmStatic
  internal fun transformEvents(appEvents: String): ArrayList<Map<String, Any>>? {

    // events is an array of json objects
    val events = arrayListOf<Map<String, Any>>()
    try {
      val jsonObjArr = convertJSONArrayToList(JSONArray(appEvents))

      jsonObjArr.forEach { jsonObjString ->
        val hashMap = convertJSONObjectToHashMap(JSONObject(jsonObjString))
        events.add(hashMap)
      }
    } catch (e: JSONException) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "\n transformEvents JSONException: \n%s\n%s",
          appEvents,
          e)
      return null
    }

    if (events.isEmpty()) {
      return null
    }

    // return this
    val transformedEvents = ArrayList<Map<String, Any>>()

    events.forEach { eventMapObject ->
      val customData = mutableMapOf<String, Any>()
      val transformedEvent = mutableMapOf<String, Any>()

      // foe each keyVal pair in the event, convert it
      eventMapObject.keys.forEach inner@{ key ->
        val keyEnum = CustomEventField(key)
        val mapping = customEventTransformations[keyEnum]

        if (keyEnum == null || mapping == null) {
          return@inner // continue
        }

        val section = mapping.section
        if (section != null) {
          if (section == ConversionsAPISection.CUSTOM_DATA) {
            // TODO T101157887: Some of the parameters like fb_content_id, fb_content need to be
            // converted from string to array
            customData[mapping.field.rawValue] =
                transformValue(key, eventMapObject[key] as Any) as Any
          }
        } else { // Top level app event name to capi event name

          try {
            val capiFieldName = mapping.field.rawValue
            if (keyEnum == CustomEventField.EVENT_NAME && eventMapObject[key] as String? != null) {
              transformedEvent[capiFieldName] = transformEventName(eventMapObject[key] as String)
            } else
            // Top level app event name to capi event time
            if (keyEnum == CustomEventField.EVENT_TIME && eventMapObject[key] as Int? != null) {
              transformedEvent[capiFieldName] =
                  transformValue(key, eventMapObject[key] as Any) as Any
            }
          } catch (e: ClassCastException) {
            Logger.log(
                LoggingBehavior.APP_EVENTS,
                TAG,
                "\n transformEvents ClassCastException: \n %s ",
                e.stackTraceToString())
          }
        }
      }

      if (customData.isNotEmpty()) {
        transformedEvent[ConversionsAPISection.CUSTOM_DATA.rawValue] = customData
      }

      transformedEvents.add(transformedEvent as Map<String, Any>)
    }

    return transformedEvents
  }

  // MARK: user and app data transformations
  private fun transformAndUpdateAppData(
      appData: MutableMap<String, Any>,
      field: AppEventUserAndAppDataField,
      value: Any
  ) {
    val key = topLevelTransformations[field]?.field?.rawValue ?: return

    appData[key] = value
  }

  private fun transformAndUpdateUserData(
      userData: MutableMap<String, Any>,
      field: AppEventUserAndAppDataField,
      value: Any
  ) {

    if (field == AppEventUserAndAppDataField.USER_DATA) {
      try {
        val udParam = convertJSONObjectToHashMap(JSONObject(value as String))
        userData.putAll(udParam)
      } catch (e: JSONException) {

        Logger.log(
            LoggingBehavior.APP_EVENTS, TAG, "\n transformEvents JSONException: \n%s\n%s", value, e)
      }
    } else {
      val key = topLevelTransformations[field]?.field?.rawValue ?: return
      userData[key] = value
    }
  }

  internal fun transformAndUpdateAppAndUserData(
      userData: MutableMap<String, Any>,
      appData: MutableMap<String, Any>,
      field: AppEventUserAndAppDataField,
      value: Any
  ) {

    val section = topLevelTransformations[field]?.section ?: return
    when (section) {
      ConversionsAPISection.APP_DATA -> transformAndUpdateAppData(appData, field, value)
      ConversionsAPISection.USER_DATA -> transformAndUpdateUserData(userData, field, value)
      else -> return
    }
  }

  // MARK: events section transformations
  private fun transformEventName(input: String): String {

    return if (standardEventTransformations.containsKey(input)) {
      standardEventTransformations[input]?.rawValue ?: ""
    } else {
      input
    }
  }

  // MARK: combine transformed data
  internal fun combineCommonFields(
      userData: Map<String, Any>,
      appData: Map<String, Any>,
      restOfData: Map<String, Any>
  ): Map<String, Any> {

    val converted = mutableMapOf<String, Any>()
    converted[OtherEventConstants.ACTION_SOURCE.rawValue] = OtherEventConstants.APP.rawValue

    converted[ConversionsAPISection.USER_DATA.rawValue] = userData
    converted[ConversionsAPISection.APP_DATA.rawValue] = appData

    converted.putAll(restOfData)

    return converted
  }

  private fun combineAllTransformedDataForMobileAppInstall(
      commonFields: Map<String, Any>,
      eventTime: Any?
  ): List<Map<String, Any>>? {
    if (eventTime == null) {
      return null
    }
    val transformedEvent = mutableMapOf<String, Any>()

    // transformedEvent.merge(commonFields) { $1 }
    transformedEvent.putAll(commonFields)

    transformedEvent[ConversionsAPICustomEventField.EVENT_NAME.rawValue] =
        OtherEventConstants.MOBILE_APP_INSTALL.rawValue
    transformedEvent[ConversionsAPICustomEventField.EVENT_TIME.rawValue] = eventTime
    return listOf(transformedEvent)
  }

  private fun combineAllTransformedDataForCustom(
      commonFields: Map<String, Any>,
      customEvents: List<Map<String, Any>>
  ): List<Map<String, Any>>? {
    if (customEvents.isEmpty()) {
      return null
    }

    val transformedEvents = arrayListOf<Map<String, Any>>()

    customEvents.forEach { customEvent ->
      val customEventTransformed = mutableMapOf<String, Any>()

      // customEventTransformed.merge(commonFields) { $1 }
      customEventTransformed.putAll(commonFields)

      // customEventTransformed.merge(customEvent) { $1 }
      customEventTransformed.putAll(customEvent)

      // transformedEvents.append(customEventTransformed)
      transformedEvents.add(customEventTransformed)
    }

    return transformedEvents
  }

  internal fun combineAllTransformedData(
      eventType: AppEventType,
      userData: MutableMap<String, Any>,
      appData: MutableMap<String, Any>,
      restOfData: MutableMap<String, Any>,
      customEvents: List<Map<String, Any>>,
      eventTime: Any?
  ): List<Map<String, Any>>? {

    val commonFields = combineCommonFields(userData, appData, restOfData)

    return when (eventType) {
      AppEventType.MOBILE_APP_INSTALL ->
          combineAllTransformedDataForMobileAppInstall(commonFields, eventTime)
      AppEventType.CUSTOM -> combineAllTransformedDataForCustom(commonFields, customEvents)
      else -> null
    }
  }

  // customEvents is the equivalent of the request.tag, the actual "sub-events" under custom
  // event.
  // MARK: split app events parameters into user, app data and custom events
  private fun splitAppEventParameters(
      parameters: Map<String, Any>,
      userData: MutableMap<String, Any>,
      appData: MutableMap<String, Any>,
      customEvents: ArrayList<Map<String, Any>>,
      restOfData: MutableMap<String, Any>
  ): AppEventType {

    val eventTypeStr = parameters[OtherEventConstants.EVENT.rawValue]
    val eventType =
        AppEventType(
            eventTypeStr
                as String) // eventTypeStr?.let { AppEventType(it as String) } as AppEventType

    if (eventType == AppEventType.OTHER) {
      return eventType
    }

    parameters.forEach { (key, value) ->
      val field = AppEventUserAndAppDataField(key)

      if (field != null) { // either app data or user data
        transformAndUpdateAppAndUserData(userData, appData, field, value)
      } else {
        val isCustomEventsKey = (key == ConversionsAPISection.CUSTOM_EVENTS.rawValue)
        val isCustomEventsData = value is String

        if (eventType == AppEventType.CUSTOM && isCustomEventsKey && isCustomEventsData) {
          val events = transformEvents(value as String)
          if (events != null) {
            customEvents.addAll(events)
          }
        } else if (DataProcessingParameterName(key) != null) {
          restOfData[key] = value
        }
      }
    }
    return eventType
  }

  // MARK: main function
  internal fun conversionsAPICompatibleEvent(
      parameters: Map<String, Any>
  ): List<Map<String, Any>>? {
    val userData = mutableMapOf<String, Any>()
    val appData = mutableMapOf<String, Any>()
    val customEvents = arrayListOf<Map<String, Any>>()
    val restOfData = mutableMapOf<String, Any>()

    val eventType =
        splitAppEventParameters(
            parameters,
            userData,
            appData,
            customEvents,
            restOfData,
        )

    if (eventType == AppEventType.OTHER) {
      return null
    }

    return combineAllTransformedData(
        eventType,
        userData,
        appData,
        restOfData,
        customEvents,
        parameters[OtherEventConstants.INSTALL_EVENT_TIME.rawValue])
  }
}
