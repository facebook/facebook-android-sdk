/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.model

import android.graphics.Bitmap
import android.util.Base64
import com.facebook.gamingservices.GamingContext
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.util.HashMap
import org.json.JSONObject

private const val CONTEXT_TOKEN_KEY = "context_token_id"
private const val CTA_KEY = "cta"
private const val DATA_KEY = "data"
private const val DEFAULT_KEY = "default"
private const val GIF_KEY = "gif"
private const val IMAGE_KEY = "image"
private const val LOCALIZATIONS_KEY = "localizations"
private const val MEDIA_KEY = "media"
private const val TEXT_KEY = "text"
private const val URL_KEY = "url"
private const val VIDEO_KEY = "video"

/**
 * Represents a text string that can have different Locale values provided.
 *
 * @property default The default text to use if an override for the locale is not included in the
 * localizations Map.
 * @property localizations A key-value Map of Locale_Code: Locale String Value for this text. For a
 * list of valid locale codes see:
 * https://lookaside.facebook.com/developers/resources/?id=FacebookLocales.xml
 *
 * Example: default: Hello , localizations {'es_LA': 'hola'}
 */
data class CustomUpdateLocalizedText(
    val default: String,
    val localizations: HashMap<String, String>? = null
) {
  fun toJSONObject(): JSONObject {
    val out = JSONObject()
    out.put(DEFAULT_KEY, default)
    localizations?.let {
      val localizationsJson = JSONObject()
      for ((k, v) in it) {
        localizationsJson.put(k, v)
      }
      out.put(LOCALIZATIONS_KEY, localizationsJson)
    }
    return out
  }
}

/**
 * Represents a media that will be included in a Custom Update Message
 *
 * <b>Note:</b> gif and video are mutually exclusive.
 *
 * @property gif Gif that will be included in the Update Message
 * @property video Video that will be included in the Update Message. Currently this is not yet
 * supported but will be in a server side update so it is already included in the SDK. this
 * disclaimer will be removed as soon as it is.
 */
data class CustomUpdateMedia(
    val gif: CustomUpdateMediaInfo? = null,
    val video: CustomUpdateMediaInfo? = null,
) {
  fun toJSONObject(): JSONObject {
    val out = JSONObject()
    gif?.let {
      val gifObject = JSONObject()
      gifObject.put(URL_KEY, it.url)
      out.put(GIF_KEY, gifObject)
    }
    video?.let {
      val videoObject = JSONObject()
      videoObject.put(URL_KEY, it.url)
      out.put(VIDEO_KEY, videoObject)
    }
    return out
  }
}

/**
 * Stores Information about a Media that will be part of a Custom Update.
 *
 * @property url: The URL to fetch this media from.
 */
data class CustomUpdateMediaInfo(val url: String)

class CustomUpdateContent
private constructor(
    val contextTokenId: String,
    val text: CustomUpdateLocalizedText,
    val cta: CustomUpdateLocalizedText? = null,
    val image: String? = null,
    val media: CustomUpdateMedia? = null,
    val data: String? = null
) {

  /**
   * Converts this CustomUpdateContent to a JSONObject to be used by a GraphRequest
   *
   * This will be called automatically by CustomUpdate.newCustomUpdateRequest()
   *
   * @return a JSONObject that can be attached to a GraphRequest
   */
  fun toGraphRequestContent(): JSONObject {
    var json = JSONObject()
    json.put(CONTEXT_TOKEN_KEY, contextTokenId)
    json.put(TEXT_KEY, text.toJSONObject().toString())
    cta?.let { json.put(CTA_KEY, it.toJSONObject().toString()) }
    image?.let { json.put(IMAGE_KEY, it) }
    media?.let { json.put(MEDIA_KEY, it.toJSONObject().toString()) }
    data?.let { json.put(DATA_KEY, it) }
    return json
  }

  class Builder
  private constructor(
      private val contextTokenId: String?,
      private val text: CustomUpdateLocalizedText,
      private val image: Bitmap?,
      private val media: CustomUpdateMedia?,
  ) {
    /**
     * Creates a CustomUpdateContent Builder
     *
     * @param contextToken GamingContext A valid GamingContext to send the update to.
     * @param text The text that will be included in the update
     * @param image An image that will be included in the update
     */
    constructor(
        contextToken: GamingContext,
        text: CustomUpdateLocalizedText,
        image: Bitmap
    ) : this(contextToken.contextID, text, image, null)
    /**
     * Creates a CustomUpdateContent Builder
     *
     * @param contextToken GamingContext A valid GamingContext to send the update to.
     * @param text The text that will be included in the update
     * @param media A gif or video that will be included in the update
     */
    constructor(
        contextToken: GamingContext,
        text: CustomUpdateLocalizedText,
        media: CustomUpdateMedia,
    ) : this(contextToken.contextID, text, null, media)

    var cta: CustomUpdateLocalizedText? = null
      private set

    var data: String? = null
      private set

    /**
     * Sets the CTA (Call to Action) text in the update message
     *
     * If none is provided then a localized version of 'play' will be used.
     *
     * @param cta The Custom CTA to use.
     */
    fun setCta(cta: CustomUpdateLocalizedText): Builder {
      this.cta = cta
      return this
    }

    /**
     * Sets a Data that will be sent back to the game when a user clicks on the message. When the
     * game is launched from a Custom Update message the data here will be forwarded as a Payload.
     * Please use the GamingPayload class to retrieve it.
     *
     * @param data A String that will be sent back to the game.
     */
    fun setData(data: String): Builder {
      this.data = data
      return this
    }

    /**
     * Returns a CustomUpdateContent with the values defined in this builder.
     *
     * @return CustomUpdateContent instance that can be used with
     * CustomUpdate.newCustomUpdateRequest()
     * @throws IllegalArgumentException if CustomUpdateMedia is invalid or if the ContextToken is
     * null.
     */
    fun build(): CustomUpdateContent {
      if (media != null && !((media.gif != null).xor(media.video != null))) {
        throw IllegalArgumentException("Invalid CustomUpdateMedia, please set either gif or video")
      }

      var imageStr = bitmapToBase64String(image)
      if (this.contextTokenId == null) {
        throw IllegalArgumentException("parameter contextToken must not be null")
      }
      return CustomUpdateContent(
          this.contextTokenId, this.text, this.cta, imageStr, this.media, this.data)
    }

    private fun bitmapToBase64String(bitmap: Bitmap?): String? {
      if (bitmap == null) {
        return null
      }
      val outputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

      return "data:image/png;base64," +
          Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
  }
}
