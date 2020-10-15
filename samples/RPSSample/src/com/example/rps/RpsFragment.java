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

package com.example.rps;

import static com.example.rps.RpsGameUtils.*;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.android.vending.billing.IInAppBillingService;
import com.facebook.*;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.internal.Utility;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.AppInviteDialog;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

public class RpsFragment extends Fragment {

  private static final String SHARE_GAME_LINK = "https://developers.facebook.com/docs/android";
  private static final String SHARE_GAME_NAME = "Rock, Papers, Scissors Sample Application";
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
  private static final String TEST_SKU = "android.test.purchased";
  // TOKEN_SKU should only be used for licensed test users to purchase in-app products set by
  // developers. Please follow Google's guide for reference
  // https://developer.android.com/google/play/billing/billing_testing.html#testing-purchases
  private static final String TOKEN_SKU = "com.rpssample.token";
  private static final String TOKEN_SUBSCRIPTION_SKU = "com.rpssample.tokensubs";
  private static final String INAPP_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
  private static final String INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
  private static final String INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
  private static final String BUY_INTENT = "BUY_INTENT";
  private static final String RESPONSE_CODE = "RESPONSE_CODE";

  private static final String APP_INSTALL_TITLE = "Share with Facebook";
  private static final String INSTALL_BUTTON = "Install or Upgrade Now";
  private static final String CANCEL_BUTTON = "Decide Later";

  public static final int BILLING_RESPONSE_RESULT_OK = 0;
  public static final int IN_APP_PURCHASE_RESULT = 1001;
  public static final int IN_APP_PURCHASE_VERSION = 3;

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
  private Button buyButton;

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
  private IInAppBillingService inAppBillingService;
  private ServiceConnection serviceConnection;
  private Context context;

  private DialogInterface.OnClickListener canPublishClickListener =
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          if (AccessToken.isCurrentAccessTokenActive()) {
            // if they choose to publish, then we request for publish permissions
            shouldImplicitlyPublish = true;
            pendingPublish = true;

            LoginManager.getInstance()
                .setDefaultAudience(DefaultAudience.FRIENDS)
                .logInWithPublishPermissions(
                    RpsFragment.this, Arrays.asList(ADDITIONAL_PERMISSIONS));
          }
        }
      };

  private DialogInterface.OnClickListener dontPublishClickListener =
      new DialogInterface.OnClickListener() {
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
    if (AccessToken.isCurrentAccessTokenActive()) {
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
    builder
        .setTitle(R.string.error_dialog_title)
        .setMessage(messageId)
        .setPositiveButton(R.string.error_ok_button, null);
    builder.show();
  }

  private void publishResult() {
    if (shouldImplicitlyPublish && canPublish()) {
      String photoUri = PHOTO_URIS[playerChoice];
      SharePhoto.Builder sharePhotoBuilder = new SharePhoto.Builder();
      if (photoUri == null) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), DRAWABLES_HUMAN[playerChoice]);
        sharePhotoBuilder.setBitmap(bitmap);
      } else {
        sharePhotoBuilder.setImageUrl(Uri.parse(photoUri));
      }
      sharePhotoBuilder.setUserGenerated(false);
      final SharePhoto gesturePhoto = sharePhotoBuilder.build();

      SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(gesturePhoto).build();

      ShareApi.share(
          content,
          new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(@NonNull Sharer.Result result) {
              Log.i(TAG, "Posted OG Action with id: " + result.getPostId());
            }

            @Override
            public void onCancel() {
              // This should not happen
            }

            @Override
            public void onError(@NonNull FacebookException error) {
              Log.e(TAG, "Play action creation failed: " + error.getMessage());
            }
          });
    }
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
        .putPhotoArrayList(
            "og:image",
            new ArrayList<SharePhoto>() {
              {
                add(new SharePhoto.Builder().setBitmap(bitmap).build());
              }
            })
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

  // Workaround to bug where sometimes response codes come as Long instead of Integer
  private int getResponseCodeFromBundle(Bundle b) {
    Object o = b.get(RESPONSE_CODE);
    if (o == null) {
      Log.e(TAG, "Bundle with null response code, assuming OK (known issue)");
      return BILLING_RESPONSE_RESULT_OK;
    } else if (o instanceof Integer) return ((Integer) o).intValue();
    else if (o instanceof Long) return (int) ((Long) o).longValue();
    else {
      Log.e(TAG, "Unexpected type for bundle response code.");
      Log.e(TAG, o.getClass().getName());
      throw new RuntimeException(
          "Unexpected type for bundle response code: " + o.getClass().getName());
    }
  }

  private void makePurchase(boolean isSubscription) {
    try {
      Bundle buyIntentBundle =
          inAppBillingService.getBuyIntent(
              3,
              getActivity().getPackageName(),
              TEST_SKU,
              isSubscription ? "subs" : "inapp",
              "this is a test");

      int response = getResponseCodeFromBundle(buyIntentBundle);
      if (response != BILLING_RESPONSE_RESULT_OK) {
        Log.e(TAG, "Unable to buy item, Error response: " + response);
        return;
      }

      PendingIntent pendingIntent = buyIntentBundle.getParcelable(BUY_INTENT);
      getActivity()
          .startIntentSenderForResult(
              pendingIntent.getIntentSender(),
              IN_APP_PURCHASE_RESULT,
              new Intent(),
              Integer.valueOf(0),
              Integer.valueOf(0),
              Integer.valueOf(0));
    } catch (IntentSender.SendIntentException e) {
      Log.e(TAG, "In app purchase send intent exception.", e);
    } catch (RemoteException e) {
      Log.e(TAG, "In app purchase remote exception.", e);
    }
  }

  public void onInAppPurchaseSuccess(JSONObject jo) {
    String token = null;
    try {
      token = jo.getString("purchaseToken");
      String packageName = jo.getString("packageName");
      consumePurchase(token, packageName);
    } catch (JSONException e) {
      Log.e(TAG, "In app purchase invalid json.", e);
    }
  }

  private void consumePurchase(String packageName, String token) {
    try {
      int consumeResponse =
          inAppBillingService.consumePurchase(IN_APP_PURCHASE_VERSION, packageName, token);
      if (consumeResponse == 0) {
        Log.d(TAG, "Successfully consumed package: " + packageName);
      } else {
        Log.d(TAG, "Faileds to consume package: " + packageName + " " + consumeResponse);
      }
    } catch (RemoteException e) {
      Log.e(TAG, "Consuming purchase remote exception.", e);
    }
  }

  public void shareUsingMessengerDialog() {
    if (playerChoice == INVALID_CHOICE || computerChoice == INVALID_CHOICE) {
      ShareContent content = getLinkContent();

      // share the app
      if (messageDialog.canShow(content)) {
        messageDialog.show(content);
      } else {
        showInstallMessengerAppInGooglePlay();
      }
    } else {
      ShareContent content = getThrowActionContent();

      if (messageDialog.canShow(content)) {
        messageDialog.show(content);
      } else {
        showInstallMessengerAppInGooglePlay();
      }
    }
  }

  private void showInstallMessengerAppInGooglePlay() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setCancelable(true);
    builder.setTitle(APP_INSTALL_TITLE);
    builder.setMessage(
        "Install or upgrade the Messenger application on your device and "
            + "get cool new sharing features for this application. "
            + "What do you want to do?");
    builder.setInverseBackgroundForced(true);
    builder.setPositiveButton(
        INSTALL_BUTTON,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            String uri = "http://play.google.com/store/apps/details?id=com.facebook.orca";
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
          }
        });
    builder.setNegativeButton(
        CANCEL_BUTTON,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
    AlertDialog alert = builder.create();
    alert.show();
  }

  public void presentAppInviteDialog() {
    AppInviteContent content =
        new AppInviteContent.Builder()
            .setApplinkUrl("https://d3uu10x6fsg06w.cloudfront.net/hosting-rps/applink.html")
            .setPreviewImageUrl(
                "https://d3uu10x6fsg06w.cloudfront.net/hosting-rps/rps-preview-image.jpg")
            .build();
    if (AppInviteDialog.canShow()) {
      appInviteDialog.show(this, content);
    } else {
      showError(R.string.appinvite_error);
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    buyButton = (Button) view.findViewById(R.id.buy_button);

    gestureImages[ROCK].setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            playerPlayed(ROCK);
          }
        });

    gestureImages[PAPER].setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            playerPlayed(PAPER);
          }
        });

    gestureImages[SCISSORS].setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            playerPlayed(SCISSORS);
          }
        });

    againButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            switchState(RpsState.INIT, false);
          }
        });

    fbButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            getActivity().openOptionsMenu();
          }
        });

    buyButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            makePurchase(false);
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
    LoginManager.getInstance()
        .registerCallback(
            callbackManager,
            new FacebookCallback<LoginResult>() {
              @Override
              public void onSuccess(@NonNull LoginResult loginResult) {
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
              public void onError(@NonNull FacebookException exception) {
                handleError();
              }

              private void handleError() {
                // this means the user did not grant us write permissions, so
                // we don't do implicit publishes
                shouldImplicitlyPublish = false;
                pendingPublish = false;
              }
            });

    FacebookCallback<Sharer.Result> callback =
        new FacebookCallback<Sharer.Result>() {
          @Override
          public void onCancel() {
            Log.d(TAG, "Canceled");
          }

          @Override
          public void onError(@NonNull FacebookException error) {
            Log.d(TAG, String.format("Error: %s", error.toString()));
          }

          @Override
          public void onSuccess(@NonNull Sharer.Result result) {
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
          public void onSuccess(@NonNull AppInviteDialog.Result result) {
            Log.d(TAG, "Success!");
          }

          @Override
          public void onCancel() {
            Log.d(TAG, "Canceled");
          }

          @Override
          public void onError(@NonNull FacebookException error) {
            Log.d(TAG, String.format("Error: %s", error.toString()));
          }
        };
    appInviteDialog = new AppInviteDialog(this);
    appInviteDialog.registerCallback(callbackManager, appInviteCallback);

    // Initialize in-app billing service
    serviceConnection =
        new ServiceConnection() {
          @Override
          public void onServiceDisconnected(ComponentName name) {
            inAppBillingService = null;
            Utility.logd(TAG, "In-app billing service disconnected");
          }

          @Override
          public void onServiceConnected(ComponentName name, IBinder service) {
            inAppBillingService = IInAppBillingService.Stub.asInterface(service);
            Utility.logd(TAG, "In app billing service connected");
            try {
              Bundle ownedItems =
                  inAppBillingService.getPurchases(
                      IN_APP_PURCHASE_VERSION, context.getPackageName(), "inapp", null);
              int response = ownedItems.getInt("RESPONSE_CODE");
              if (response == 0) {
                ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList(INAPP_PURCHASE_ITEM_LIST);
                ArrayList<String> purchaseDataList =
                    ownedItems.getStringArrayList(INAPP_PURCHASE_DATA_LIST);
                ArrayList<String> signatureList =
                    ownedItems.getStringArrayList(INAPP_DATA_SIGNATURE_LIST);

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                  String purchaseData = purchaseDataList.get(i);

                  try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String token = jo.getString("purchaseToken");
                    consumePurchase(context.getPackageName(), token);
                  } catch (JSONException e) {
                    Log.e(TAG, "Error parsing purchase data.", e);
                  }
                }
              }
            } catch (RemoteException e) {
              Log.e(TAG, "Consuming purchase remote exception.", e);
            }
          }
        };

    context = this.getActivity().getApplicationContext();
    Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
    serviceIntent.setPackage("com.android.vending");
    context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (inAppBillingService != null) {
      context.unbindService(serviceConnection);
    }
  }
}
