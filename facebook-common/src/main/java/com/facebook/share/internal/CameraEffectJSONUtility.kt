// Copyright 2004-present Facebook. All Rights Reserved.
package com.facebook.share.internal

import com.facebook.share.model.CameraEffectArguments
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * Utility methods for JSON representation of Open Graph models.
 */
object CameraEffectJSONUtility {
  private interface Setter {
    @Throws(JSONException::class)
    fun setOnArgumentsBuilder(builder: CameraEffectArguments.Builder, key: String, value: Any?)

    @Throws(JSONException::class) fun setOnJSON(json: JSONObject, key: String, value: Any?)
  }

  private val SETTERS: HashMap<Class<*>, Setter> =
      hashMapOf(
          String::class.java to
              object : Setter {
                @Throws(JSONException::class)
                override fun setOnArgumentsBuilder(
                    builder: CameraEffectArguments.Builder,
                    key: String,
                    value: Any?
                ) {
                  builder.putArgument(key, value as String)
                }

                @Throws(JSONException::class)
                override fun setOnJSON(json: JSONObject, key: String, value: Any?) {
                  json.put(key, value)
                }
              },
          Array<String>::class.java to
              object : Setter {
                @Throws(JSONException::class)
                override fun setOnArgumentsBuilder(
                    builder: CameraEffectArguments.Builder,
                    key: String,
                    value: Any?
                ) {
                  throw IllegalArgumentException("Unexpected type from JSON")
                }

                @Throws(JSONException::class)
                override fun setOnJSON(json: JSONObject, key: String, value: Any?) {
                  val jsonArray = JSONArray()
                  for (stringValue in value as Array<String?>) {
                    jsonArray.put(stringValue)
                  }
                  json.put(key, jsonArray)
                }
              },
          JSONArray::class.java to
              object : Setter {
                @Throws(JSONException::class)
                override fun setOnArgumentsBuilder(
                    builder: CameraEffectArguments.Builder,
                    key: String,
                    value: Any?
                ) {
                  // Only strings are supported for now
                  val jsonArray = value as JSONArray
                  val argsArray = arrayOfNulls<String>(jsonArray.length())
                  for (i in 0 until jsonArray.length()) {
                    val current = jsonArray[i]
                    if (current is String) {
                      argsArray[i] = current
                    } else {
                      throw IllegalArgumentException(
                          "Unexpected type in an array: " + current.javaClass)
                    }
                  }
                  builder.putArgument(key, argsArray)
                }

                @Throws(JSONException::class)
                override fun setOnJSON(json: JSONObject, key: String, value: Any?) {
                  throw IllegalArgumentException("JSONArray's are not supported in bundles.")
                }
              })

  @JvmStatic
  @Throws(JSONException::class)
  fun convertToJSON(arguments: CameraEffectArguments?): JSONObject? {
    if (arguments == null) {
      return null
    }
    val json = JSONObject()
    for (key in arguments.keySet()) {
      val value =
          arguments[key] ?: // Null is not supported.
          continue
      val setter =
          SETTERS[value.javaClass]
              ?: throw IllegalArgumentException("Unsupported type: " + value.javaClass)
      setter.setOnJSON(json, key, value)
    }
    return json
  }

  @JvmStatic
  @Throws(JSONException::class)
  fun convertToCameraEffectArguments(jsonObject: JSONObject?): CameraEffectArguments? {
    if (jsonObject == null) {
      return null
    }
    val builder = CameraEffectArguments.Builder()
    val jsonIterator = jsonObject.keys()
    while (jsonIterator.hasNext()) {
      val key = jsonIterator.next()
      val value = jsonObject[key]
      if (value === JSONObject.NULL) {
        // Null is not supported.
        continue
      }
      val setter =
          SETTERS[value.javaClass]
              ?: throw IllegalArgumentException("Unsupported type: " + value.javaClass)
      setter.setOnArgumentsBuilder(builder, key, value)
    }
    return builder.build()
  }
}
