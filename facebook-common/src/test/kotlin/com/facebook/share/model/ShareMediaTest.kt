/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.os.Parcel
import android.os.Parcelable
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShareMediaTest : FacebookTestCase() {
  class TestShareMedia : ShareMedia<TestShareMedia, TestShareMedia.Builder> {
    constructor(builder: Builder) : super(builder)
    constructor(parcel: Parcel) : super(parcel)

    override val mediaType: Type = Type.PHOTO

    class Builder : ShareMedia.Builder<TestShareMedia, Builder>() {
      override fun build(): TestShareMedia {
        return TestShareMedia(this)
      }
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<TestShareMedia> =
          object : Parcelable.Creator<TestShareMedia> {
            override fun createFromParcel(source: Parcel): TestShareMedia = TestShareMedia(source)
            override fun newArray(size: Int): Array<TestShareMedia?> = arrayOfNulls(size)
          }
    }
  }

  @Test
  fun `test parcelize share media`() {
    val shareMedia = TestShareMedia.Builder().setParameter("key", "value").build()
    val parcel = Parcel.obtain()

    parcel.writeParcelable(shareMedia, 0)
    parcel.setDataPosition(0)
    val restoredShareMedia =
        parcel.readParcelable<TestShareMedia>(TestShareMedia::class.java.classLoader)
    checkNotNull(restoredShareMedia)
    assertThat(restoredShareMedia.getParameters().get("key")).isEqualTo("value")

    parcel.recycle()
  }

  @Test
  fun `test write and read share media list`() {
    val medias =
        listOf(
            TestShareMedia.Builder().setParameter("k1", "v1").build(),
            TestShareMedia.Builder().setParameter("k2", "v2").build())
    val parcel = Parcel.obtain()

    ShareMedia.Builder.writeListTo(parcel, 0, medias)
    parcel.setDataPosition(0)
    val restoredMedias = ShareMedia.Builder.readListFrom(parcel)

    assertThat(restoredMedias.size).isEqualTo(2)
    assertThat(restoredMedias[0].getParameters().get("k1")).isEqualTo("v1")
    assertThat(restoredMedias[1].getParameters().get("k2")).isEqualTo("v2")
  }
}
