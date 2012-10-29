package com.facebook;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.facebook.android.*;

/**
 * This class provides a mechanism for displaying Facebook Web dialogs inside a Dialog. Helper
 * methods are provided to construct commonly-used dialogs, or a caller can specify arbitrary
 * parameters to call other dialogs.
 */
public class WebDialog extends Dialog {
    private static final String LOG_TAG = Settings.LOG_TAG_BASE + "WebDialog";
    private static final String DISPLAY_TOUCH = "touch";
    private static final String USER_AGENT = "user_agent";
    private static final String OAUTH_DIALOG = "oauth";
    private static final String FEED_DIALOG = "feed";
    private static final String APPREQUESTS_DIALOG = "apprequests";
    private static final String TO_PARAM = "to";
    private static final String CAPTION_PARAM = "caption";
    private static final String DESCRIPTION_PARAM = "description";
    private static final String PICTURE_PARAM = "picture";
    private static final String NAME_PARAM = "name";
    private static final String MESSAGE_PARAM = "message";
    static final String REDIRECT_URI = "fbconnect://success";
    static final String CANCEL_URI = "fbconnect://cancel";

    protected static final int DEFAULT_THEME = android.R.style.Theme_Translucent_NoTitleBar;

    private static String applicationIdForSessionlessDialogs;
    private String url;
    private OnCompleteListener onCompleteListener;
    private WebView webView;
    private ProgressDialog spinner;
    private ImageView crossImageView;
    private FrameLayout contentFrameLayout;
    private boolean listenerCalled = false;

    /**
     * Interface that implements a listener to be called when the user's interaction with the
     * dialog completes, whether because the dialog finished successfully, or it was cancelled,
     * or an error was encountered.
     */
    public interface OnCompleteListener {
        /**
         * Called when the dialog completes.
         *
         * @param values on success, contains the values returned by the dialog
         * @param error  on an error, contains an exception describing the error
         */
        void onComplete(Bundle values, FacebookException error);
    }

    /**
     * Gets the default application ID that will be used for dialogs created without a corresponding
     * valid Session.
     *
     * @return the application ID
     */
    public static String getApplicationIdForSessionlessDialogs() {
        return applicationIdForSessionlessDialogs;
    }

    /**
     * If a dialog is created without a corresponding valid Session, the application ID to pass as
     * part of the URL must first be specified via this method. This application ID will be used for
     * all dialogs created without a Session. If a Session is specified, the application ID associated
     * with that Session will override this default setting.
     *
     * @param applicationIdForSessonlessDialogs
     *         the application ID to use
     */
    public static void setApplicationIdForSessionlessDialogs(String applicationIdForSessonlessDialogs) {
        WebDialog.applicationIdForSessionlessDialogs = applicationIdForSessonlessDialogs;
    }

    /**
     * Creates a new WebDialog populated with a URL constructed from the provided parameters. The
     * dialog is ready to be shown via {@link android.app.Dialog#show()}.
     *
     * @param context    the Context to use for displaying the dialog
     * @param session    a Session which, if opened, will be used to populate the application ID and
     *                   access token to use for the dialog call
     * @param action     the portion of the dialog URL after "dialog/"
     * @param parameters a Bundle containing parameters which will be encoded as part of the URL
     * @param listener   an optional listener which will be notified when the dialog has completed
     * @return a WebDialog which is ready to be shown
     */
    public static WebDialog createDialog(Context context, Session session, String action, Bundle parameters,
            OnCompleteListener listener) {
        return createDialog(context, session, action, parameters, DEFAULT_THEME, listener);
    }

    /**
     * Creates a new WebDialog populated with a URL constructed from the provided parameters. The
     * dialog is ready to be shown via {@link android.app.Dialog#show()}. The dialog's theme can be
     * customized to modify how it is displayed.
     *
     * @param context    the Context to use for displaying the dialog
     * @param session    a Session which, if opened, will be used to populate the application ID and
     *                   access token to use for the dialog call
     * @param action     the portion of the dialog URL after "dialog/"
     * @param parameters a Bundle containing parameters which will be encoded as part of the URL
     * @param theme      a theme identifier which will be passed to the Dialog class
     * @param listener   an optional listener which will be notified when the dialog has completed
     * @return a WebDialog which is ready to be shown
     */
    public static WebDialog createDialog(Context context, Session session, String action, Bundle parameters, int theme,
            OnCompleteListener listener) {
        if (parameters == null) {
            parameters = new Bundle();
        }

        if (session == null && Utility.isNullOrEmpty(applicationIdForSessionlessDialogs)) {
            throw new FacebookException(
                    "Must specify either a Session or a default application ID for session-less dialogs");
        }

        parameters.putString("app_id",
                (session != null) ? session.getApplicationId() : applicationIdForSessionlessDialogs);
        if (session != null && session.isOpened()) {
            parameters.putString("access_token", session.getAccessToken());
        }

        if (!parameters.containsKey(ServerProtocol.DIALOG_PARAM_REDIRECT_URI)) {
            parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, REDIRECT_URI);
        }

        WebDialog result = new WebDialog(context, action, parameters, theme);
        result.setOnCompleteListener(listener);
        return result;
    }

    /**
     * Creates a WebDialog which will display the Feed dialog to post an item to the user's Timeline.
     * The dialog is ready to be shown via {@link android.app.Dialog#show()}. The dialog's theme can be
     * customized to modify how it is displayed. More documentation of the Feed dialog is available at
     * https://developers.facebook.com/docs/reference/dialogs/feed/.
     *
     * @param context     the Context to use for displaying the dialog
     * @param session     a Session which, if opened, will be used to populate the application ID and
     *                    access token to use for the dialog call
     * @param caption     an optional caption to display in the dialog
     * @param description an optional description to display in the dialog
     * @param pictureUrl  an optional URL of an icon to display in the dialog
     * @param name        an optional name to display in the dialog
     * @param parameters  additional parameters to pass to the dialog
     * @param theme       a theme identifier which will be passed to the Dialog class
     * @param listener    an optional listener which will be notified when the dialog has completed
     * @return a WebDialog which is ready to be shown
     */
    public static WebDialog createFeedDialog(Context context, Session session, String caption, String description,
            String pictureUrl, String name, Bundle parameters, int theme, OnCompleteListener listener) {
        return createFeedDialog(context, session, null, caption, description, pictureUrl, name, parameters, theme,
                listener);
    }

    /**
     * Creates a WebDialog which will display the Feed dialog to post an item to the another user's Timeline.
     * The dialog is ready to be shown via {@link android.app.Dialog#show()}. The dialog's theme can be
     * customized to modify how it is displayed. More documentation of the Feed dialog is available at
     * https://developers.facebook.com/docs/reference/dialogs/feed/.
     *
     * @param context     the Context to use for displaying the dialog
     * @param session     a Session which, if opened, will be used to populate the application ID and
     *                    access token to use for the dialog call
     * @param toProfileId the ID of the profile to post to; if null or empty, this will post to the
     *                    user's own Timeline
     * @param caption     an optional caption to display in the dialog
     * @param description an optional description to display in the dialog
     * @param pictureUrl  an optional URL of an icon to display in the dialog
     * @param name        an optional name to display in the dialog
     * @param parameters  additional parameters to pass to the dialog
     * @param theme       a theme identifier which will be passed to the Dialog class
     * @param listener    an optional listener which will be notified when the dialog has completed
     * @return a WebDialog which is ready to be shown
     */
    public static WebDialog createFeedDialog(Context context, Session session, String toProfileId, String caption,
            String description,
            String pictureUrl, String name, Bundle parameters, int theme, OnCompleteListener listener) {

        if (parameters == null) {
            parameters = new Bundle();
        }
        if (!Utility.isNullOrEmpty(toProfileId)) {
            parameters.putString(TO_PARAM, toProfileId);
        }
        if (!Utility.isNullOrEmpty(caption)) {
            parameters.putString(CAPTION_PARAM, caption);
        }
        if (!Utility.isNullOrEmpty(description)) {
            parameters.putString(DESCRIPTION_PARAM, description);
        }
        if (!Utility.isNullOrEmpty(pictureUrl)) {
            parameters.putString(PICTURE_PARAM, pictureUrl);
        }
        if (!Utility.isNullOrEmpty(name)) {
            parameters.putString(NAME_PARAM, name);
        }

        WebDialog result = createDialog(context, session, FEED_DIALOG, parameters, theme, listener);
        return result;
    }

    /**
     * Creates a WebDialog which will display the Request dialog to send a request to another user.
     * The dialog is ready to be shown via {@link android.app.Dialog#show()}. The dialog's theme can be
     * customized to modify how it is displayed. More documentation of the Requests dialog is available at
     * https://developers.facebook.com/docs/reference/dialogs/requests/.
     *
     * @param context    the Context to use for displaying the dialog
     * @param session    a Session which, if opened, will be used to populate the application ID and
     *                   access token to use for the dialog call
     * @param toProfileId an optional profile ID which the request will be sent to; if not specified,
     *                    the dialog will prompt the user to select a profile
     * @param message    an optional message to display in the dialog
     * @param parameters additional parameters to pass to the dialog
     * @param theme      a theme identifier which will be passed to the Dialog class
     * @param listener   an optional listener which will be notified when the dialog has completed
     * @return a WebDialog which is ready to be shown
     */
    public static WebDialog createAppRequestDialog(Context context, Session session, String toProfileId, String message,
            Bundle parameters, int theme, OnCompleteListener listener) {
        if (parameters == null) {
            parameters = new Bundle();
        }
        if (!Utility.isNullOrEmpty(message)) {
            parameters.putString(MESSAGE_PARAM, message);
        }
        if (!Utility.isNullOrEmpty(toProfileId)) {
            parameters.putString(TO_PARAM, toProfileId);
        }

        WebDialog result = createDialog(context, session, APPREQUESTS_DIALOG, parameters, theme, listener);
        return result;
    }

    static WebDialog createAuthDialog(Context context, String applicationId, int theme) {
        Bundle parameters = new Bundle();
        parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, REDIRECT_URI);
        parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, applicationId);

        return new WebDialog(context, OAUTH_DIALOG, parameters, theme);
    }

    /**
     * Constructor which can be used to display a dialog with an already-constructed URL.
     *
     * @param context the context to use to display the dialog
     * @param url     the URL of the Web Dialog to display; no validation is done on this URL, but it should
     *                be a valid URL pointing to a Facebook Web Dialog
     */
    public WebDialog(Context context, String url) {
        this(context, url, DEFAULT_THEME);
    }

    /**
     * Constructor which can be used to display a dialog with an already-constructed URL and a custom theme.
     *
     * @param context the context to use to display the dialog
     * @param url     the URL of the Web Dialog to display; no validation is done on this URL, but it should
     *                be a valid URL pointing to a Facebook Web Dialog
     * @param theme   identifier of a theme to pass to the Dialog class
     */
    public WebDialog(Context context, String url, int theme) {
        super(context, (theme == 0) ? DEFAULT_THEME : theme);
        this.url = url;
    }

    /**
     * Constructor which will construct the URL of the Web dialog based on the specified parameters.
     *
     * @param context    the context to use to display the dialog
     * @param action     the portion of the dialog URL following "dialog/"
     * @param parameters parameters which will be included as part of the URL
     * @param theme      identifier of a theme to pass to the Dialog class
     */
    public WebDialog(Context context, String action, Bundle parameters, int theme) {
        this(context, null, theme);

        if (parameters == null) {
            parameters = new Bundle();
        }
        parameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, DISPLAY_TOUCH);
        parameters.putString(ServerProtocol.DIALOG_PARAM_TYPE, USER_AGENT);

        Uri uri = Utility.buildUri(ServerProtocol.DIALOG_AUTHORITY, ServerProtocol.DIALOG_PATH + action, parameters);
        this.url = uri.toString();
    }

    /**
     * Sets the listener which will be notified when the dialog finishes.
     *
     * @param listener the listener to notify, or null if no notification is desired
     */
    public void setOnCompleteListener(OnCompleteListener listener) {
        onCompleteListener = listener;
    }

    /**
     * Gets the listener which will be notified when the dialog finishes.
     *
     * @return the listener, or null if none has been specified
     */
    public OnCompleteListener getOnCompleteListener() {
        return onCompleteListener;
    }

    @Override
    public void dismiss() {
        if (webView != null) {
            webView.stopLoading();
        }
        if (spinner.isShowing()) {
            spinner.dismiss();
        }
        super.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                sendCancelToListener();
            }
        });

        spinner = new ProgressDialog(getContext());
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(getContext().getString(R.string.com_facebook_loading));
        spinner.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                sendCancelToListener();
                WebDialog.this.dismiss();
            }
        });

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        contentFrameLayout = new FrameLayout(getContext());

        /* Create the 'x' image, but don't add to the contentFrameLayout layout yet
         * at this point, we only need to know its drawable width and height
         * to place the webview
         */
        createCrossImage();

        /* Now we know 'x' drawable width and height,
        * layout the webivew and add it the contentFrameLayout layout
        */
        int crossWidth = crossImageView.getDrawable().getIntrinsicWidth();
        setUpWebView(crossWidth / 2);

        /* Finally add the 'x' image to the contentFrameLayout layout and
        * add contentFrameLayout to the Dialog view
        */
        contentFrameLayout.addView(crossImageView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addContentView(contentFrameLayout,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void sendSuccessToListener(Bundle values) {
        if (onCompleteListener != null && !listenerCalled) {
            listenerCalled = true;
            onCompleteListener.onComplete(values, null);
        }
    }

    private void sendErrorToListener(Throwable error) {
        if (onCompleteListener != null && !listenerCalled) {
            listenerCalled = true;
            FacebookException facebookException = null;
            if (error instanceof FacebookException) {
                facebookException = (FacebookException) error;
            } else {
                facebookException = new FacebookException(error);
            }
            onCompleteListener.onComplete(null, facebookException);
        }
    }

    private void sendCancelToListener() {
        sendErrorToListener(new FacebookOperationCanceledException());
    }

    private void createCrossImage() {
        crossImageView = new ImageView(getContext());
        // Dismiss the dialog when user click on the 'x'
        crossImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCancelToListener();
                WebDialog.this.dismiss();
            }
        });
        Drawable crossDrawable = getContext().getResources().getDrawable(R.drawable.com_facebook_close);
        crossImageView.setImageDrawable(crossDrawable);
        /* 'x' should not be visible while webview is loading
         * make it visible only after webview has fully loaded
        */
        crossImageView.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebView(int margin) {
        LinearLayout webViewContainer = new LinearLayout(getContext());
        webView = new WebView(getContext());
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new DialogWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.INVISIBLE);
        webView.getSettings().setSavePassword(false);

        webViewContainer.setPadding(margin, margin, margin, margin);
        webViewContainer.addView(webView);
        contentFrameLayout.addView(webViewContainer);
    }

    private class DialogWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Util.logd(LOG_TAG, "Redirect URL: " + url);
            if (url.startsWith(WebDialog.REDIRECT_URI)) {
                Bundle values = Util.parseUrl(url);

                String error = values.getString("error");
                if (error == null) {
                    error = values.getString("error_type");
                }

                if (error == null) {
                    sendSuccessToListener(values);
                } else if (error.equals("access_denied") ||
                        error.equals("OAuthAccessDeniedException")) {
                    sendCancelToListener();
                } else {
                    sendErrorToListener(new FacebookException(error));
                }

                WebDialog.this.dismiss();
                return true;
            } else if (url.startsWith(WebDialog.CANCEL_URI)) {
                sendCancelToListener();
                WebDialog.this.dismiss();
                return true;
            } else if (url.contains(DISPLAY_TOUCH)) {
                return false;
            }
            // launch non-dialog URLs in a full browser
            getContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            sendErrorToListener(new FacebookDialogException(description, errorCode, failingUrl));
            WebDialog.this.dismiss();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);

            sendErrorToListener(new FacebookDialogException(null, ERROR_FAILED_SSL_HANDSHAKE, null));
            handler.cancel();
            WebDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Util.logd(LOG_TAG, "Webview loading URL: " + url);
            super.onPageStarted(view, url, favicon);
            spinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            spinner.dismiss();
            /*
             * Once web view is fully loaded, set the contentFrameLayout background to be transparent
             * and make visible the 'x' image.
             */
            contentFrameLayout.setBackgroundColor(Color.TRANSPARENT);
            webView.setVisibility(View.VISIBLE);
            crossImageView.setVisibility(View.VISIBLE);
        }
    }

}
