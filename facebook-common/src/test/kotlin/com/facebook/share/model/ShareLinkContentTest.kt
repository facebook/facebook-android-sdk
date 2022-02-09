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
import org.junit.Test

class ShareLinkContentTest : FacebookTestCase() {
  @Test
  fun `test create link content`() {
    val url = "https://facebook.com"
    val quote = "test quote"

    val linkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse(url)).setQuote(quote).build()

    assertThat(linkContent.quote).isEqualTo(quote)
    assertThat(linkContent.contentUrl).isEqualTo(Uri.parse(url))
  }

  @Test
  fun `test read link content from parcel`() {
    val url = "https://facebook.com"
    val quote = "test quote"

    val linkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse(url)).setQuote(quote).build()
    val parcel = Parcel.obtain()
    linkContent.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    val restoredLinkContent = ShareLinkContent.CREATOR.createFromParcel(parcel)

    assertThat(restoredLinkContent.quote).isEqualTo(quote)
    assertThat(restoredLinkContent.contentUrl).isEqualTo(Uri.parse(url))
  }
}
