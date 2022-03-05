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

import android.net.Uri
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShareVideoTest : FacebookTestCase() {
  private lateinit var shareVideo: ShareVideo
  @Before
  fun init() {
    shareVideo = ShareVideo.Builder().setLocalUrl(Uri.parse("https://facebook.com")).build()
  }

  @Test
  fun `test build video`() {
    assertThat(shareVideo).isNotNull
  }

  @Test
  fun `test get video`() {
    assertThat(shareVideo.localUrl.toString()).isEqualTo("https://facebook.com")
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareVideo, 0)
    parcel.setDataPosition(0)

    val recoveredContent = parcel.readParcelable<ShareVideo>(ShareVideo::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.localUrl).isEqualTo(shareVideo.localUrl)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = ShareVideo.Builder().readFrom(shareVideo).build()
    assertThat(recoveredContent.localUrl).isEqualTo(shareVideo.localUrl)
  }
}
