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

package com.facebook.share.model

import android.os.Parcel
import android.os.Parcelable
import com.facebook.internal.NativeProtocol

/**
 * Describes an Open Graph Object to be created.
 *
 * Use [ShareOpenGraphObject.Builder] to create instances
 *
 * See the documentation for
 * [Open Graph Objects](https://developers.facebook.com/docs/opengraph/objects/).
 */
@Deprecated(
    "Effective May 1st 2019, all newly published Open Graph stories will render as a plain link share in newsfeed. This means that Open Graph actions will not appear when posting.")
class ShareOpenGraphObject :
    ShareOpenGraphValueContainer<ShareOpenGraphObject, ShareOpenGraphObject.Builder> {
  private constructor(builder: Builder) : super(builder)
  internal constructor(parcel: Parcel?) : super(parcel)

  /** Builder for the [com.facebook.share.model.ShareOpenGraphObject] interface. */
  class Builder : ShareOpenGraphValueContainer.Builder<ShareOpenGraphObject, Builder>() {
    override fun build(): ShareOpenGraphObject {
      return ShareOpenGraphObject(this)
    }

    internal fun readFrom(parcel: Parcel): Builder? {
      return this.readFrom(
          parcel.readParcelable(ShareOpenGraphObject::class.java.classLoader) as
              ShareOpenGraphObject?)
    }

    init {
      putBoolean(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY, true)
    }
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ShareOpenGraphObject> =
        object : Parcelable.Creator<ShareOpenGraphObject> {
          override fun createFromParcel(parcel: Parcel): ShareOpenGraphObject? {
            return ShareOpenGraphObject(parcel)
          }

          override fun newArray(size: Int): Array<ShareOpenGraphObject?> {
            return arrayOfNulls(size)
          }
        }
  }
}
