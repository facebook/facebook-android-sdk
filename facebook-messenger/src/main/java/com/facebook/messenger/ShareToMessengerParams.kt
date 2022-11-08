/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.messenger

import android.net.Uri

/**
 * Parameters used by [MessengerUtils] for sending the media to Messenger to share. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
 */
class ShareToMessengerParams internal constructor(builder: ShareToMessengerParamsBuilder) {
    companion object {
        val VALID_URI_SCHEMES: Set<String>
        val VALID_MIME_TYPES: Set<String>
        val VALID_EXTERNAL_URI_SCHEMES: Set<String>

        /**
         * Creates a new builder for creating a [ShareToMessengerParams] instance
         *
         * @param uri the uri of the local content. Must be a file://, content://, or res:// URI.
         * @param mimeType the mime-type of the content. See [.VALID_MIME_TYPES] for what mime types
         * are supported.
         * @return the builder instance
         */
        @JvmStatic
        fun newBuilder(uri: Uri, mimeType: String): ShareToMessengerParamsBuilder {
            return ShareToMessengerParamsBuilder(uri, mimeType)
        }

        init {
            val validMimeTypes = HashSet<String>()
            validMimeTypes.add("image/*")
            validMimeTypes.add("image/jpeg")
            validMimeTypes.add("image/png")
            validMimeTypes.add("image/gif")
            validMimeTypes.add("image/webp")
            validMimeTypes.add("video/*")
            validMimeTypes.add("video/mp4")
            validMimeTypes.add("audio/*")
            validMimeTypes.add("audio/mpeg")
            VALID_MIME_TYPES = validMimeTypes.toSet()

            val validUriSchemes = HashSet<String>()
            validUriSchemes.add("content")
            validUriSchemes.add("android.resource")
            validUriSchemes.add("file")
            VALID_URI_SCHEMES = validUriSchemes.toSet()

            val validExternalUriSchemes = HashSet<String>()
            validExternalUriSchemes.add("http")
            validExternalUriSchemes.add("https")
            VALID_EXTERNAL_URI_SCHEMES = validExternalUriSchemes.toSet()
        }
    }

    /**
     * The URI of the local image, video, or audio clip to send to messenger. Must be a file://,
     * content://, or res:// URI.
     */
    val uri: Uri

    /**
     * The mime type of the content. See [.VALID_MIME_TYPES] for what mime types are supported.
     */
    val mimeType: String

    /**
     * The metadata to attach to the content to shared. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     */
    val metaData: String?

    /**
     * An external URI that Messenger can use to download the content on Facebook's servers instead of
     * requiring the Messenger application to upload the content. The content returned by the this URI
     * must be exactly the same as the content specified by [.uri]. If the content is different,
     * Messenger may fail to send the content. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     */
    val externalUri: Uri?

    init {
        uri = checkNotNull(builder.uri) { "Must provide non-null uri" }
        mimeType = checkNotNull(builder.mimeType) { "Must provide mimeType" }
        metaData = builder.metaData
        externalUri = builder.externalUri

        require(VALID_URI_SCHEMES.contains(uri.scheme)) { "Unsupported URI scheme: ${uri.scheme}" }
        require(VALID_MIME_TYPES.contains(mimeType)) { "Unsupported mime-type: $mimeType" }
        if (externalUri != null) {
            require(VALID_EXTERNAL_URI_SCHEMES.contains(externalUri.scheme)) { "Unsupported external uri scheme: ${externalUri.scheme}" }
        }
    }
}
