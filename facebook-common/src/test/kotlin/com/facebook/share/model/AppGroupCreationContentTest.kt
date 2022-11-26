/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.name).isEqualTo(appGroupCreationContent.name)
    assertThat(recoveredContent.description).isEqualTo(appGroupCreationContent.description)
    assertThat(recoveredContent.appGroupPrivacy).isEqualTo(appGroupCreationContent.appGroupPrivacy)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent =
        AppGroupCreationContent.Builder().readFrom(appGroupCreationContent).build()
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.name).isEqualTo(appGroupCreationContent.name)
    assertThat(recoveredContent.description).isEqualTo(appGroupCreationContent.description)
    assertThat(recoveredContent.appGroupPrivacy).isEqualTo(appGroupCreationContent.appGroupPrivacy)
  }
}
