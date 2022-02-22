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
import com.facebook.FacebookTestCase
import com.facebook.share.model.AppGroupCreationContent.AppGroupPrivacy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AppGroupCreationContentTest : FacebookTestCase() {
  private lateinit var appGroupCreationContent: AppGroupCreationContent

  @Before
  fun init() {
    appGroupCreationContent =
        AppGroupCreationContent.Builder()
            .setName("name")
            .setDescription("description")
            .setAppGroupPrivacy(AppGroupPrivacy.Open)
            .build()
  }

  @Test
  fun `test build AppGroupCreationContent`() {
    assertThat(appGroupCreationContent).isNotNull
  }

  @Test
  fun `test getName`() {
    assertThat(appGroupCreationContent.name).isEqualTo("name")
  }

  @Test
  fun `test getDescription`() {
    assertThat(appGroupCreationContent.description).isEqualTo("description")
  }

  @Test
  fun `test getAppGroupPrivacy`() {
    assertThat(appGroupCreationContent.appGroupPrivacy).isEqualTo(AppGroupPrivacy.Open)
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(appGroupCreationContent, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<AppGroupCreationContent>(
            AppGroupCreationContent::class.java.classLoader)
    assertThat(recoveredContent?.name).isEqualTo(appGroupCreationContent.name)
    assertThat(recoveredContent?.description).isEqualTo(appGroupCreationContent.description)
    assertThat(recoveredContent?.appGroupPrivacy).isEqualTo(appGroupCreationContent.appGroupPrivacy)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent =
        AppGroupCreationContent.Builder().readFrom(appGroupCreationContent).build()
    assertThat(recoveredContent?.name).isEqualTo(appGroupCreationContent.name)
    assertThat(recoveredContent?.description).isEqualTo(appGroupCreationContent.description)
    assertThat(recoveredContent?.appGroupPrivacy).isEqualTo(appGroupCreationContent.appGroupPrivacy)
  }
}
