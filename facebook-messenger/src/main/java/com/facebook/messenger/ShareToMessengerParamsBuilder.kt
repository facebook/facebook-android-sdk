/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.messenger

import android.net.Uri

/** Builder for [ShareToMessengerParams].  */
class ShareToMessengerParamsBuilder internal constructor(
    /**
     * Gets the URI of the local image, video, or audio clip to send to messenger. Must be a file://,
     * content://, or res:// URI.
     *
     * @return the uri
     */
    val uri: Uri,
    /**
     * Gets the mime type of the content. See [ShareToMessengerParams.VALID_MIME_TYPES] for what
     * mime types are supported.
     *
     * @return the mime type
     */
    val mimeType: String
) {

    /**
     * Gets the metadata to attach to the content to shared. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     *
     * @return the metadata to attach to the message
     */
    var metaData: String? = null
        private set

    /**
     * Gets an external URI that Messenger can use to download the content on Facebook's servers
     * instead of requiring the Messenger application to upload the content. The content returned by
     * the this URI must be exactly the same as the content specified by [.getUri]. If the
     * content is different, Messenger may fail to send the content. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     *
     * @return the external URI
     */
    var externalUri: Uri? = null
        private set

    /**
     * Sets the metadata to attach to the content to shared. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     *
     * @param metaData the metadata to attach to the message
     * @return this builder
     */
    fun setMetaData(metaData: String?): ShareToMessengerParamsBuilder {
        this.metaData = metaData
        return this
    }

    /**
     * Sets an external URI that Messenger can use to download the content on Facebook's servers
     * instead of requiring the Messenger application to upload the content. The content returned by
     * the this URI must be exactly the same as the content specified by [.getUri]. If the
     * content is different, Messenger may fail to send the content. See [developer docs](https://developers.facebook.com/docs/messenger/android) for more info.
     *
     * @param externalUri the external uri to set
     * @return this builder
     */
    fun setExternalUri(externalUri: Uri?): ShareToMessengerParamsBuilder {
        this.externalUri = externalUri
        return this
    }

    /**
     * Builds the parameter object.
     *
     * @return the parameter object
     */
    fun build(): ShareToMessengerParams {
        return ShareToMessengerParams(this)
    }
}