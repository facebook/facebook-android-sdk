package com.facebook.share.model

import android.net.Uri
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShareContentTest : FacebookTestCase() {
  private lateinit var shareContent: ShareLinkContent
  private lateinit var contentUri: Uri
  private lateinit var shareHashtag: ShareHashtag
  @Before
  fun init() {
    contentUri = Uri.parse("www.facebook.com/test")
    shareHashtag = ShareHashtag.Builder().setHashtag("test").build()
    shareContent =
        ShareLinkContent.Builder()
            .setQuote(quote)
            .setContentUrl(contentUri)
            .setPageId(pageID)
            .setPeopleIds(friendIds)
            .setRef(ref)
            .setShareHashtag(shareHashtag)
            .build()
  }
  @Test
  fun `test share content properties`() {
    assertThat(shareContent.quote).isEqualTo(quote)
    assertThat(shareContent.contentUrl).isEqualTo(contentUri)
    assertThat(shareContent.pageId).isEqualTo(pageID)
    assertThat(shareContent.peopleIds).isEqualTo(friendIds)
    assertThat(shareContent.ref).isEqualTo(ref)
    assertThat(shareContent.shareHashtag).isEqualTo(shareHashtag)
  }

  @Test
  fun `test write to parcel`() {
    val parcel = Parcel.obtain()
    shareContent.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)

    assertThat(parcel.readParcelable<Uri>(Uri::class.java.classLoader)).isEqualTo(contentUri)
    val list = mutableListOf<String>()
    parcel.readStringList(list)
    assertThat(list).isEqualTo(friendIds)
    assertThat(parcel.readString()).isEqualTo(null) // place id
    assertThat(parcel.readString()).isEqualTo(pageID)
    assertThat(parcel.readString()).isEqualTo(ref)
    val ht = ShareHashtag.Builder().readFrom(parcel).build()
    assertThat(ht.hashtag).isEqualTo(shareHashtag.hashtag)
  }

  companion object {
    val friendIds = listOf("111", "222", "333")
    const val quote = "quoteText"
    const val pageID = "123"
    const val ref = "ref"
  }
}
