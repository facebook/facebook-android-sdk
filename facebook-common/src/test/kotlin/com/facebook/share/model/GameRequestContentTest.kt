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
