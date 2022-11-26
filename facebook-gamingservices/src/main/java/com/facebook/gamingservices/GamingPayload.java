/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class GamingPayload {

  private static final String TAG = GamingPayload.class.getSimpleName();
  private static final String KEY_CONTEXT_TOKEN_ID = "context_token_id";
  private static final String KEY_GAME_REQUEST_ID = "game_request_id";
  private static final String KEY_PAYLOAD = "payload";
  private static final String KEY_APPLINK_DATA = "al_applink_data";
  private static final String KEY_EXTRAS = "extras";
  private static final String KEY_TOURNAMENT_ID = "tournament_id";

  private static Map<String, String> payloadData;

  private GamingPayload() {}

  /**
   * Retrieves the Game Request ID that referred the user to the game.
   *
   * <p>When a user sends a Game Request, the recipient can launch the game directly from Facebook,
   * the resulting deeplink will provide the referring Game Request ID.
   *
   * @return GameRequestID if any.
   */
  public static @Nullable String getGameRequestID() {
    if (GamingPayload.payloadData == null) {
      return null;
    }

    if (GamingPayload.payloadData.containsKey(KEY_GAME_REQUEST_ID)) {
      return GamingPayload.payloadData.get(KEY_GAME_REQUEST_ID);
    } else {
      return null;
    }
  }

  /**
   * Retrieves a payload sent from Facebook to this game.
   *
   * <p>When a GameRequest contains the data field, it will be forwarded here as a payload.
   *
   * @return GameRequest payload (if any).
   */
  public static @Nullable String getPayload() {
    if (GamingPayload.payloadData == null) {
      return null;
    }

    if (GamingPayload.payloadData.containsKey(KEY_PAYLOAD)) {
      return GamingPayload.payloadData.get(KEY_PAYLOAD);
    } else {
      return null;
    }
  }

  /**
   * Retrieves the tournament id sent from Facebook to this game.
   *
   * <p>When a user clicks play from the tournament share post on their news feed, the tournament id
   * will be sent to the game via deeplink.
   *
   * @return tournament id (if any).
   */
  public static @Nullable String getTournamentId() {
    if (GamingPayload.payloadData == null) {
      return null;
    }

    if (GamingPayload.payloadData.containsKey(KEY_TOURNAMENT_ID)) {
      return GamingPayload.payloadData.get(KEY_TOURNAMENT_ID);
    } else {
      return null;
    }
  }

  /**
   * Retireves any Gaming Payload bundled in the start arguments for a Game running on Facebook
   * Cloud. This is called automatically by the Cloud Init handler.
   *
   * @param payloadString JSON Encoded payload.
   */
  public static void loadPayloadFromCloudGame(String payloadString) {
    Map<String, String> loadedPayload = new HashMap<>();
    try {
      JSONObject payloadJSON = new JSONObject(payloadString);
      loadedPayload.put(KEY_GAME_REQUEST_ID, payloadJSON.optString(KEY_GAME_REQUEST_ID));
      loadedPayload.put(KEY_PAYLOAD, payloadJSON.optString(KEY_PAYLOAD));
      loadedPayload.put(KEY_TOURNAMENT_ID, payloadJSON.optString(KEY_TOURNAMENT_ID));

      GamingPayload.payloadData = loadedPayload;
    } catch (JSONException e) {
      Log.e(TAG, e.toString(), e);
    }
  }

  /**
   * Retrieves any Gaming Payload bundled within the Intent that launched the Game.
   *
   * @param intent Intent that lanched this Game.
   */
  public static void loadPayloadFromIntent(Intent intent) {
    Map<String, String> loadedPayload = new HashMap<>();
    if (intent == null) {
      return;
    }
    Bundle extras = intent.getExtras();

    if (extras != null && extras.containsKey(KEY_APPLINK_DATA)) {
      Bundle appLinkData = extras.getBundle(KEY_APPLINK_DATA);
      Bundle appLinkExtras = appLinkData.getBundle(KEY_EXTRAS);

      if (appLinkExtras != null) {
        String gameRequestId = appLinkExtras.getString(KEY_GAME_REQUEST_ID);
        String payload = appLinkExtras.getString(KEY_PAYLOAD);
        String contextTokenId = appLinkExtras.getString(KEY_CONTEXT_TOKEN_ID);
        String tournamentId = appLinkExtras.getString(KEY_TOURNAMENT_ID);

        if (contextTokenId != null) {
          GamingContext.setCurrentGamingContext(new GamingContext(contextTokenId));
        }

        loadedPayload.put(KEY_GAME_REQUEST_ID, gameRequestId);
        loadedPayload.put(KEY_PAYLOAD, payload);
        loadedPayload.put(KEY_TOURNAMENT_ID, tournamentId);
        GamingPayload.payloadData = loadedPayload;
      }
    }
  }
}
