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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GameRequestContentTest : FacebookTestCase() {
  private lateinit var gameRequestContent: GameRequestContent

  override fun setUp() {
    super.setUp()

    gameRequestContent =
        GameRequestContent.Builder()
            .setMessage("message")
            .setCta("cta")
            .setRecipients(listOf("recipient1", "recipient2"))
            .setTitle("title")
            .setData("data")
            .setActionType(GameRequestContent.ActionType.INVITE)
            .setObjectId("objectId")
            .setFilters(GameRequestContent.Filters.APP_USERS)
            .setSuggestions(listOf("suggestion1", "suggestion2"))
            .build()
  }

  @Test
  fun `test build GameRequestContent`() {
    assertThat(gameRequestContent).isNotNull
  }

  @Test
  fun `test getMessage`() {
    assertThat(gameRequestContent.message).isEqualTo("message")
  }

  @Test
  fun `test getCta`() {
    assertThat(gameRequestContent.cta).isEqualTo("cta")
  }

  @Test
  fun `test getRecipients`() {
    assertThat(gameRequestContent.recipients).isEqualTo(listOf("recipient1", "recipient2"))
  }

  @Test
  fun `test getTitle`() {
    assertThat(gameRequestContent.title).isEqualTo("title")
  }

  @Test
  fun `test getData`() {
    assertThat(gameRequestContent.data).isEqualTo("data")
  }

  @Test
  fun `test getActionType`() {
    assertThat(gameRequestContent.actionType).isEqualTo(GameRequestContent.ActionType.INVITE)
  }

  @Test
  fun `test getObjectId`() {
    assertThat(gameRequestContent.objectId).isEqualTo("objectId")
  }

  @Test
  fun `test getFilters`() {
    assertThat(gameRequestContent.filters).isEqualTo(GameRequestContent.Filters.APP_USERS)
  }

  @Test
  fun `test getSuggestions`() {
    assertThat(gameRequestContent.suggestions).isEqualTo(listOf("suggestion1", "suggestion2"))
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(gameRequestContent, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<GameRequestContent>(GameRequestContent::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.message).isEqualTo(gameRequestContent.message)
    assertThat(recoveredContent.cta).isEqualTo(gameRequestContent.cta)
    assertThat(recoveredContent.recipients).isEqualTo(gameRequestContent.recipients)
    assertThat(recoveredContent.title).isEqualTo(gameRequestContent.title)
    assertThat(recoveredContent.data).isEqualTo(gameRequestContent.data)
    assertThat(recoveredContent.actionType).isEqualTo(gameRequestContent.actionType)
    assertThat(recoveredContent.objectId).isEqualTo(gameRequestContent.objectId)
    assertThat(recoveredContent.filters).isEqualTo(gameRequestContent.filters)
    assertThat(recoveredContent.suggestions).isEqualTo(gameRequestContent.suggestions)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = GameRequestContent.Builder().readFrom(gameRequestContent).build()
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.message).isEqualTo(gameRequestContent.message)
    assertThat(recoveredContent.cta).isEqualTo(gameRequestContent.cta)
    assertThat(recoveredContent.recipients).isEqualTo(gameRequestContent.recipients)
    assertThat(recoveredContent.title).isEqualTo(gameRequestContent.title)
    assertThat(recoveredContent.data).isEqualTo(gameRequestContent.data)
    assertThat(recoveredContent.actionType).isEqualTo(gameRequestContent.actionType)
    assertThat(recoveredContent.objectId).isEqualTo(gameRequestContent.objectId)
    assertThat(recoveredContent.filters).isEqualTo(gameRequestContent.filters)
    assertThat(recoveredContent.suggestions).isEqualTo(gameRequestContent.suggestions)
  }
}
