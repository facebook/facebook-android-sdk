/**
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

package com.facebook.samples.rps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.facebook.*;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static com.facebook.samples.rps.RpsGameUtils.*;

public class RpsFragment extends Fragment {

    private static final String SHARE_GAME_LINK = "https://developers.facebook.com/docs/android";
    private static final String SHARE_GAME_NAME = "Rock, Papers, Scissors Sample Application";
    private static final String DEFAULT_GAME_OBJECT_TITLE =
            "an awesome game of Rock, Paper, Scissors";
    private static final String WIN_KEY = "wins";
    private static final String LOSS_KEY = "losses";
    private static final String TIE_KEY = "ties";
    private static final String PLAYER_CHOICE_KEY = "player_choice";
    private static final String COMPUTER_CHOICE_KEY = "computer_choice";
    private static final String STATE_KEY = "state";
    private static final String RESULT_KEY = "result";
    private static final String PENDING_PUBLISH_KEY = "pending_publish";
    private static final String IMPLICIT_PUBLISH_KEY = "implicitly_publish";
    private static final String ADDITIONAL_PERMISSIONS = "publish_actions";
    private static final int INITIAL_DELAY_MILLIS = 500;
    private static final int DEFAULT_DELAY_MILLIS = 1000;
    private static final String TAG = RpsFragment.class.getName();

    private static String[] PHOTO_URIS = {null, null, null};

    private TextView[] gestureTextViews = new TextView[3];
    private TextView shootTextView;
    private ImageView playerChoiceView;
    private ImageView computerChoiceView;
    private TextView resultTextView;
    private ViewGroup shootGroup;
    private ViewGroup resultGroup;
    private ViewGroup playerChoiceGroup;
    private Button againButton;
    private ImageButton[] gestureImages = new ImageButton[3];
    private ImageButton fbButton;
    private TextView statsTextView;
    private ViewFlipper rpsFlipper;

    private int wins = 0;
    private int losses = 0;
    private int ties = 0;
    private int playerChoice = INVALID_CHOICE;
    private int computerChoice = INVALID_CHOICE;
    private RpsState currentState = RpsState.INIT;
    private RpsResult result = RpsResult.INVALID;
    private InitHandler handler = new InitHandler();
    private Random random = new Random(System.currentTimeMillis());
    private boolean pendingPublish;
    private boolean shouldImplicitlyPublish = true;
    private CallbackManager callbackManager;
    private ShareDialog shareDialog;
    private MessageDialog messageDialog;
    private AppInviteDialog appInviteDialog;

    private DialogInterface.OnClickListener canPublishClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (AccessToken.getCurrentAccessToken() != null) {
                // if they choose to publish, then we request for publish permissions
                shouldImplicitlyPublish = true;
                pendingPublish = true;

                LoginManager.getInstance()
                        .setDefaultAudience(DefaultAudience.FRIENDS)
                        .logInWithPublishPermissions(
                                RpsFragment.this,
                                Arrays.asList(ADDITIONAL_PERMISSIONS));
            }
        }
    };

    private DialogInterface.OnClickListener dontPublishClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            // if they choose not to publish, then we save that choice, and don't prompt them
            // until they restart the app
            pendingPublish = false;
            shouldImplicitlyPublish = false;
        }
    };

    private class InitHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (!isResumed()) {
                // if we're not in between onResume and onPause, don't do animation transitions
                return;
            }
            switch (msg.what) {
                case ROCK:
                    showViews(gestureTextViews[ROCK], gestureImages[ROCK]);
                    sendNextMessage(PAPER);
                    break;
                case PAPER:
                    showViews(gestureTextViews[PAPER], gestureImages[PAPER]);
                    sendNextMessage(SCISSORS);
                    break;
                case SCISSORS:
                    showViews(gestureTextViews[SCISSORS], gestureImages[SCISSORS]);
                    sendNextMessage(SHOOT);
                    break;
                case SHOOT:
                    showViews(shootTextView);
                    switchState(RpsState.PLAYING, false);
                    break;
                default:
                    Log.e(TAG, "Unexpected message received: " + msg.toString());
                    break;
            }
        }

        private void sendNextMessage(int what) {
            Message newMsg = new Message();
            newMsg.what = what;
            sendMessageDelayed(newMsg, DEFAULT_DELAY_MILLIS);
        }
    }

    private void switchState(RpsState newState, boolean isOnResume) {
        if (!isResumed()) {
            // if we're not in between onResume and onPause, don't transition states
            return;
        }
        switch (newState) {
            case INIT:
                playerChoice = INVALID_CHOICE;
                computerChoice = INVALID_CHOICE;
                result = RpsResult.INVALID;
                showViews(shootGroup, playerChoiceGroup, rpsFlipper);
                rpsFlipper.startFlipping();
                hideViews(gestureImages);
                hideViews(gestureTextViews);
                hideViews(resultGroup, shootTextView, againButton);
                enableViews(false, gestureImages);
                enableViews(false, againButton);
                Message initMessage = new Message();
                initMessage.what = ROCK;
                handler.sendMessageDelayed(initMessage, INITIAL_DELAY_MILLIS);
                break;
            case PLAYING:
                enableViews(true, gestureImages);
                showViews(rpsFlipper);
                rpsFlipper.startFlipping();
                break;
            case RESULT:
                hideViews(shootGroup, playerChoiceGroup);
                playerChoiceView.setImageResource(DRAWABLES_HUMAN[playerChoice]);
                computerChoiceView.setImageResource(DRAWABLES_COMPUTER[computerChoice]);
                resultTextView.setText(result.getStringId());
                showViews(resultGroup, againButton);
                enableViews(true, againButton);
                if (!isOnResume) {
                    // don't publish if we're switching states because onResumed is being called
                    publishResult();
                }
                break;
            default:
                Log.e(TAG, "Unexpected state reached: " + newState.toString());
                break;
        }

        String statsFormat = getResources().getString(R.string.stats_format);
        statsTextView.setText(String.format(statsFormat, wins, losses, ties));

        currentState = newState;
    }

    private void hideViews(View... views) {
        for (View view : views) {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void showViews(View... views) {
        for (View view : views) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void enableViews(boolean enabled, View... views) {
        for (View view : views) {
            view.setEnabled(enabled);
        }
    }

    private void playerPlayed(int choice) {
        playerChoice = choice;
        computerChoice = getComputerChoice();
        result = RESULTS[playerChoice][computerChoice];
        switch (result) {
            case WIN:
                wins++;
                break;
            case LOSS:
                losses++;
                break;
            case TIE:
                ties++;
                break;
            default:
                Log.e(TAG, "Unexpected result: " + result.toString());
                break;
        }
        switchState(RpsState.RESULT, false);
    }

    private int getComputerChoice() {
        return random.nextInt(3);
    }

    private boolean canPublish() {
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            if (accessToken.getPermissions().contains(ADDITIONAL_PERMISSIONS)) {
                // if we already have publish permissions, then go ahead and publish
                return true;
            } else {
                // otherwise we ask the user if they'd like to publish to facebook
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.share_with_friends_title)
                        .setMessage(R.string.share_with_friends_message)
                        .setPositiveButton(R.string.share_with_friends_yes, canPublishClickListener)
                        .setNegativeButton(R.string.share_with_friends_no, dontPublishClickListener)
                        .show();
                return false;
            }
        }
        return false;
    }

    private void showError(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.error_dialog_title).
                setMessage(messageId).
                setPositiveButton(R.string.error_ok_button, null);
        builder.show();
    }

    private void publishResult() {
        if (shouldImplicitlyPublish && canPublish()) {
            String photoUri = PHOTO_URIS[playerChoice];
            SharePhoto.Builder sharePhotoBuilder = new SharePhoto.Builder();
            if (photoUri == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(
                        getResources(),
                        DRAWABLES_HUMAN[playerChoice]);
                sharePhotoBuilder.setBitmap(bitmap);
            } else {
                sharePhotoBuilder.setImageUrl(Uri.parse(photoUri));
            }
            sharePhotoBuilder.setUserGenerated(false);
            final SharePhoto gesturePhoto = sharePhotoBuilder.build();

            ShareOpenGraphObject gameObject = createGameObject(gesturePhoto);
            ShareOpenGraphAction playAction = createPlayActionWithGame(gameObject);
            ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                    .setAction(playAction)
                    .setPreviewPropertyName("game")
                    .build();

            ShareApi.share(content, new FacebookCallback<Sharer.Result>() {
                @Override
                public void onSuccess(Sharer.Result result) {
                    Log.i(TAG, "Posted OG Action with id: " +
                            result.getPostId());
                }

                @Override
                public void onCancel() {
                    // This should not happen
                }

                @Override
                public void onError(FacebookException error) {
                    Log.e(TAG, "Play action creation failed: " + error.getMessage());
                }
            });
        }
    }

    private ShareOpenGraphObject createGameObject(final SharePhoto gesturePhoto) {
        return new ShareOpenGraphObject.Builder()
                .putString("og:title", DEFAULT_GAME_OBJECT_TITLE)
                .putString("og:type", "fb_sample_rps:game")
                .putString("fb_sample_rps:player_gesture",
                        CommonObjects.BUILT_IN_OPEN_GRAPH_OBJECTS[playerChoice])
                .putString("fb_sample_rps:opponent_gesture",
                        CommonObjects.BUILT_IN_OPEN_GRAPH_OBJECTS[computerChoice])
                .putString("fb_sample_rps:result", getString(result.getResultStringId()))
                .putPhotoArrayList("og:image", new ArrayList<SharePhoto>() {{
                    add(gesturePhoto);
                }})
                .build();
    }

    private ShareOpenGraphAction createPlayActionWithGame(ShareOpenGraphObject game) {
        return new ShareOpenGraphAction.Builder()
                .setActionType(OpenGraphConsts.PLAY_ACTION_TYPE)
                .putObject("game", game).build();
    }

    private String getBuiltInGesture(int choice) {
        if (choice < 0 || choice >= CommonObjects.BUILT_IN_OPEN_GRAPH_OBJECTS.length) {
            throw new IllegalArgumentException("Invalid choice");
        }

        return CommonObjects.BUILT_IN_OPEN_GRAPH_OBJECTS[choice];
    }

    private ShareOpenGraphAction getThrowAction() {
        // The OG objects have their own bitmaps we could rely on, but in order to demonstrate
        // attaching an in-memory bitmap (e.g., a game screencap) we'll send the bitmap explicitly
        // ourselves.
        ImageButton view = gestureImages[playerChoice];
        BitmapDrawable drawable = (BitmapDrawable) view.getBackground();
        final Bitmap bitmap = drawable.getBitmap();

        return new ShareOpenGraphAction.Builder()
                .setActionType(OpenGraphConsts.THROW_ACTION_TYPE)
                .putString("fb_sample_rps:gesture", getBuiltInGesture(playerChoice))
                .putString("fb_sample_rps:opposing_gesture", getBuiltInGesture(computerChoice))
                .putPhotoArrayList("og:image", new ArrayList<SharePhoto>() {{
                    add(new SharePhoto.Builder().setBitmap(bitmap).build());
                }})
                .build();
    }

    private ShareOpenGraphContent getThrowActionContent() {
        return new ShareOpenGraphContent.Builder()
                .setAction(getThrowAction())
                .setPreviewPropertyName(OpenGraphConsts.THROW_ACTION_PREVIEW_PROPERTY_NAME)
                .build();
    }

    private ShareLinkContent getLinkContent() {
        return new ShareLinkContent.Builder()
                .setContentTitle(SHARE_GAME_NAME)
                .setContentUrl(Uri.parse(SHARE_GAME_LINK))
                .build();
    }

    public void shareUsingNativeDialog() {
        if (playerChoice == INVALID_CHOICE || computerChoice == INVALID_CHOICE) {
            ShareContent content = getLinkContent();

            // share the app
            if (shareDialog.canShow(content, ShareDialog.Mode.NATIVE)) {
                shareDialog.show(content, ShareDialog.Mode.NATIVE);
            } else {
                showError(R.string.native_share_error);
            }
        } else {
            ShareContent content = getThrowActionContent();

            if (shareDialog.canShow(content, ShareDialog.Mode.NATIVE)) {
                shareDialog.show(content, ShareDialog.Mode.NATIVE);
            } else {
                showError(R.string.native_share_error);
            }
        }
    }

    public void shareUsingMessengerDialog() {
        if (playerChoice == INVALID_CHOICE || computerChoice == INVALID_CHOICE) {
            ShareContent content = getLinkContent();

            // share the app
            if (messageDialog.canShow(content)) {
                messageDialog.show(content);
            }
        } else {
            ShareContent content = getThrowActionContent();

            if (messageDialog.canShow(content)) {
                messageDialog.show(content);
            }
        }
    }

    public void presentAppInviteDialog() {
        AppInviteContent content = new AppInviteContent.Builder()
                .setApplinkUrl("http://hosting-rps.parseapp.com/applink.html")
                .setPreviewImageUrl("http://hosting-rps.parseapp.com/rps-preview-image.png")
                .build();
        if (AppInviteDialog.canShow()) {
            appInviteDialog.show(this, content);
        } else {
            showError(R.string.appinvite_error);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.rps_fragment, container, false);

        gestureTextViews[ROCK] = (TextView) view.findViewById(R.id.text_rock);
        gestureTextViews[PAPER] = (TextView) view.findViewById(R.id.text_paper);
        gestureTextViews[SCISSORS] = (TextView) view.findViewById(R.id.text_scissors);
        shootTextView = (TextView) view.findViewById(R.id.shoot);
        playerChoiceView = (ImageView) view.findViewById(R.id.player_choice);
        computerChoiceView = (ImageView) view.findViewById(R.id.computer_choice);
        resultTextView = (TextView) view.findViewById(R.id.who_won);
        shootGroup = (ViewGroup) view.findViewById(R.id.shoot_display_group);
        resultGroup = (ViewGroup) view.findViewById(R.id.result_display_group);
        playerChoiceGroup = (ViewGroup) view.findViewById(R.id.player_choice_display_group);
        againButton = (Button) view.findViewById(R.id.again_button);
        gestureImages[ROCK] = (ImageButton) view.findViewById(R.id.player_rock);
        gestureImages[PAPER] = (ImageButton) view.findViewById(R.id.player_paper);
        gestureImages[SCISSORS] = (ImageButton) view.findViewById(R.id.player_scissors);
        fbButton = (ImageButton) view.findViewById(R.id.facebook_button);
        statsTextView = (TextView) view.findViewById(R.id.stats);
        rpsFlipper = (ViewFlipper) view.findViewById(R.id.rps_flipper);

        gestureImages[ROCK].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerPlayed(ROCK);
            }
        });

        gestureImages[PAPER].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerPlayed(PAPER);
            }
        });

        gestureImages[SCISSORS].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerPlayed(SCISSORS);
            }
        });

        againButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchState(RpsState.INIT, false);
            }
        });

        fbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().openOptionsMenu();
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            wins = savedInstanceState.getInt(WIN_KEY);
            losses = savedInstanceState.getInt(LOSS_KEY);
            ties = savedInstanceState.getInt(TIE_KEY);
            computerChoice = savedInstanceState.getInt(COMPUTER_CHOICE_KEY);
            playerChoice = savedInstanceState.getInt(PLAYER_CHOICE_KEY);
            currentState = (RpsState) savedInstanceState.getSerializable(STATE_KEY);
            result = (RpsResult) savedInstanceState.getSerializable(RESULT_KEY);
            pendingPublish = savedInstanceState.getBoolean(PENDING_PUBLISH_KEY);
            shouldImplicitlyPublish = savedInstanceState.getBoolean(IMPLICIT_PUBLISH_KEY);
        }
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(
                callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        if (accessToken.getPermissions().contains(ADDITIONAL_PERMISSIONS)) {
                            publishResult();
                        } else {
                            handleError();
                        }
                    }

                    @Override
                    public void onCancel() {
                        handleError();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        handleError();
                    }

                    private void handleError() {
                        // this means the user did not grant us write permissions, so
                        // we don't do implicit publishes
                        shouldImplicitlyPublish = false;
                        pendingPublish = false;
                    }
                }
        );

        FacebookCallback<Sharer.Result> callback =
                new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Canceled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, String.format("Error: %s", error.toString()));
                    }

                    @Override
                    public void onSuccess(Sharer.Result result) {
                        Log.d(TAG, "Success!");
                    }
                };
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, callback);
        messageDialog = new MessageDialog(this);
        messageDialog.registerCallback(callbackManager, callback);

        FacebookCallback<AppInviteDialog.Result> appInviteCallback =
                new FacebookCallback<AppInviteDialog.Result>() {
                    @Override
                    public void onSuccess(AppInviteDialog.Result result) {
                        Log.d(TAG, "Success!");
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Canceled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, String.format("Error: %s", error.toString()));
                    }
                };
        appInviteDialog = new AppInviteDialog(this);
        appInviteDialog.registerCallback(callbackManager, appInviteCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        switchState(currentState, true);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(WIN_KEY, wins);
        bundle.putInt(LOSS_KEY, losses);
        bundle.putInt(TIE_KEY, ties);
        bundle.putInt(COMPUTER_CHOICE_KEY, computerChoice);
        bundle.putInt(PLAYER_CHOICE_KEY, playerChoice);
        bundle.putSerializable(STATE_KEY, currentState);
        bundle.putSerializable(RESULT_KEY, result);
        bundle.putBoolean(PENDING_PUBLISH_KEY, pendingPublish);
        bundle.putBoolean(IMPLICIT_PUBLISH_KEY, shouldImplicitlyPublish);
    }
}
