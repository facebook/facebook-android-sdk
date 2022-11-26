package com.facebook

import android.net.Uri
import com.facebook.gamingservices.internal.TournamentJoinDialogURIBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class TournamentJoinDialogURIBuilderTest: FacebookPowerMockTestCase() {
    @Test
    fun `base uri`() {
        val uri = TournamentJoinDialogURIBuilder.uri()
        val expected = Uri.parse("https://fb.gg/dialog/join_tournament")
        assertEquals("URI builder produced the wrong URI", uri.toString(), expected.toString())
    }

    @Test
    fun `uri with tournament_id`() {
        val uri = TournamentJoinDialogURIBuilder.uri("123")
        val expected = Uri
                .parse("https://fb.gg/dialog/join_tournament")
                .buildUpon()
                .appendQueryParameter("tournament_id", "123")
                .build()
        assertEquals("URI builder produced the wrong URI", uri.toString(), expected.toString())
    }

    @Test
    fun `uri with payload`() {
        val uri = TournamentJoinDialogURIBuilder.uri(null, "test")
        val expected = Uri
                .parse("https://fb.gg/dialog/join_tournament")
                .buildUpon()
                .appendQueryParameter("payload", "test")
                .build()
        assertEquals("URI builder produced the wrong URI", uri.toString(), expected.toString())
    }

    @Test
    fun `uri with tournament_id and payload`() {
        val uri = TournamentJoinDialogURIBuilder.uri("123", "test")
        val expected = Uri
                .parse("https://fb.gg/dialog/join_tournament")
                .buildUpon()
                .appendQueryParameter("tournament_id", "123")
                .appendQueryParameter("payload", "test")
                .build()
        assertEquals("URI builder produced the wrong URI", uri.toString(), expected.toString())
    }
}