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

package com.facebook.share.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogFeature;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.NativeAppCallAttachmentStore;
import com.facebook.internal.Utility;
import com.facebook.share.Sharer;
import com.facebook.share.internal.LegacyNativeDialogParameters;
import com.facebook.share.internal.NativeDialogParameters;
import com.facebook.share.internal.OpenGraphActionDialogFeature;
import com.facebook.share.internal.ShareContentValidation;
import com.facebook.share.internal.ShareDialogFeature;
import com.facebook.share.internal.ShareFeedContent;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.internal.WebDialogParameters;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareMediaContent;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideoContent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provides functionality to share content via the Facebook Share Dialog
 */
public final class ShareDialog
        extends FacebookDialogBase<ShareContent, Sharer.Result>
        implements Sharer {

    /**
     * The mode for the share dialog.
     */
    public enum Mode {
        /**
         * The mode is determined automatically.
         */
        AUTOMATIC,
        /**
         * The native dialog is used.
         */
        NATIVE,
        /**
         * The web dialog is used.
         */
        WEB,
        /**
         * The feed dialog is used.
         */
        FEED
    }

    private static final String FEED_DIALOG = "feed";
    public static final String WEB_SHARE_DIALOG = "share";
    private static final String WEB_OG_SHARE_DIALOG = "share_open_graph";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.Share.toRequestCode();

    private  boolean shouldFailOnDataError = false;
    // Keep track of Mode overrides for logging purposes.
    private boolean isAutomaticMode = true;

    /**
     * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
     * Activity. No callback will be invoked.
     *
     * @param activity Activity to use to share the provided content
     * @param shareContent Content to share
     */
    public static void show(
            final Activity activity,
            final ShareContent shareContent) {
        new ShareDialog(activity).show(shareContent);
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
     * Fragment. No callback will be invoked.
     *
     * @param fragment android.support.v4.app.Fragment to use to share the provided content
     * @param shareContent Content to share
     */
    public static void show(
            final Fragment fragment,
            final ShareContent shareContent) {
        show(new FragmentWrapper(fragment), shareContent);
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
     * Fragment. No callback will be invoked.
     *
     * @param fragment android.app.Fragment to use to share the provided content
     * @param shareContent Content to share
     */
    public static void show(
            final android.app.Fragment fragment,
            final ShareContent shareContent) {
        show(new FragmentWrapper(fragment), shareContent);
    }

    private static void show(
            final FragmentWrapper fragmentWrapper,
            final ShareContent shareContent) {
        new ShareDialog(fragmentWrapper).show(shareContent);
    }

    /**
     * Indicates whether it is possible to show the dialog for
     * {@link com.facebook.share.model.ShareContent} of the specified type.
     *
     * @param contentType Class of the intended {@link com.facebook.share.model.ShareContent} to
     *                    share.
     * @return True if the specified content type can be shown via the dialog
     */
    public static boolean canShow(Class<? extends ShareContent> contentType) {
        return canShowWebTypeCheck(contentType) || canShowNative(contentType);
    }

    private static boolean canShowNative(Class<? extends ShareContent> contentType) {
        DialogFeature feature = getFeature(contentType);

        return feature != null && DialogPresenter.canPresentNativeDialogWithFeature(feature);
    }

    private static boolean canShowWebTypeCheck(Class<? extends ShareContent> contentType) {
        // If we don't have an instance of a ShareContent, then all we can do is check whether
        // this is a ShareLinkContent, which can be shared if configured properly.
        // The instance method version of this check is more accurate and should be used on
        // ShareDialog instances.

        // SharePhotoContent currently requires the user staging endpoint, so we need a user access
        // token, so we need to see if we have one
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        final boolean haveUserAccessToken = accessToken != null && !accessToken.isExpired();

        return ShareLinkContent.class.isAssignableFrom(contentType)
                || ShareOpenGraphContent.class.isAssignableFrom(contentType)
                || (SharePhotoContent.class.isAssignableFrom(contentType) && haveUserAccessToken);
    }

    /**
     * Constructs a new ShareDialog.
     * @param activity Activity to use to share the provided content.
     */
    public ShareDialog(Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);

        ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a new ShareDialog.
     * @param fragment android.support.v4.app.Fragment to use to share the provided content.
     */
    public ShareDialog(Fragment fragment) {
        this(new FragmentWrapper(fragment));

    }

    /**
     * Constructs a new ShareDialog.
     * @param fragment android.app.Fragment to use to share the provided content.
     */
    public ShareDialog(android.app.Fragment fragment) {
        this(new FragmentWrapper(fragment));
    }

    private ShareDialog(FragmentWrapper fragmentWrapper) {
        super(fragmentWrapper, DEFAULT_REQUEST_CODE);

        ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
    }

    // for ShareDialog use only
    ShareDialog(Activity activity, int requestCode) {
        super(activity, requestCode);

        ShareInternalUtility.registerStaticShareCallback(requestCode);
    }

    // for ShareDialog use only
    ShareDialog(Fragment fragment, int requestCode) {
        this(new FragmentWrapper(fragment), requestCode);

    }

    ShareDialog(android.app.Fragment fragment, int requestCode) {
        this(new FragmentWrapper(fragment), requestCode);
    }

    private ShareDialog(FragmentWrapper fragmentWrapper, int requestCode) {
        super(fragmentWrapper, requestCode);

        ShareInternalUtility.registerStaticShareCallback(requestCode);
    }

    @Override
    protected void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {
        ShareInternalUtility.registerSharerCallback(
                getRequestCode(), callbackManager, callback);
    }

    @Override
    public boolean getShouldFailOnDataError() {
        return this.shouldFailOnDataError;
    }

    @Override
    public void setShouldFailOnDataError(boolean shouldFailOnDataError) {
        this.shouldFailOnDataError = shouldFailOnDataError;
    }

    /**
     * Call this to check if the Share Dialog can be shown in a specific mode.
     *
     * @param mode Mode of the Share Dialog
     * @return True if the dialog can be shown in the passed in Mode
     */
    public boolean canShow(ShareContent content, Mode mode) {
        return canShowImpl(content, (mode == Mode.AUTOMATIC) ? BASE_AUTOMATIC_MODE : mode);
    }

    /**
     * Call this to show the Share Dialog in a specific mode
     * @param mode Mode of the Share Dialog
     */
    public void show(ShareContent content, Mode mode) {
        isAutomaticMode = (mode == Mode.AUTOMATIC);

        showImpl(content, isAutomaticMode ? BASE_AUTOMATIC_MODE : mode);
    }

    @Override
    protected AppCall createBaseAppCall() {
        return new AppCall(getRequestCode());
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        ArrayList<ModeHandler> handlers = new ArrayList<>();
        handlers.add(new NativeHandler());
        handlers.add(new FeedHandler()); // Feed takes precedence for link-shares for Mode.AUTOMATIC
        handlers.add(new WebShareHandler());

        return handlers;
    }

    private class NativeHandler extends ModeHandler {
        @Override
        public Object getMode() {
            return Mode.NATIVE;
        }

        @Override
        public boolean canShow(final ShareContent content, boolean isBestEffort) {
            if (content == null) {
                return false;
            }

            boolean canShowResult = true;
            if (!isBestEffort) {
                // The following features are considered best-effort and will not prevent the
                // native share dialog from being presented, even if the installed version does
                // not support the feature.
                // However, to let apps pivot to a different approach or dialog (for example, Web),
                // we need to be able to signal back when native support is lacking.
                if (content.getShareHashtag() != null) {
                    canShowResult = DialogPresenter.canPresentNativeDialogWithFeature(
                            ShareDialogFeature.HASHTAG);
                }
                if ((content instanceof ShareLinkContent) &&
                        (!Utility.isNullOrEmpty(((ShareLinkContent)content).getQuote()))) {
                    canShowResult &= DialogPresenter.canPresentNativeDialogWithFeature(
                            ShareDialogFeature.LINK_SHARE_QUOTES);
                }
            }
            return canShowResult && ShareDialog.canShowNative(content.getClass());
        }

        @Override
        public AppCall createAppCall(final ShareContent content) {
            logDialogShare(getActivityContext(), content, Mode.NATIVE);

            ShareContentValidation.validateForNativeShare(content);

            final AppCall appCall = createBaseAppCall();
            final boolean shouldFailOnDataError = getShouldFailOnDataError();

            DialogPresenter.setupAppCallForNativeDialog(
                    appCall,
                    new DialogPresenter.ParameterProvider() {
                        @Override
                        public Bundle getParameters() {
                            return NativeDialogParameters.create(
                                    appCall.getCallId(),
                                    content,
                                    shouldFailOnDataError);
                        }

                        @Override
                        public Bundle getLegacyParameters() {
                            return LegacyNativeDialogParameters.create(
                                    appCall.getCallId(),
                                    content,
                                    shouldFailOnDataError);
                        }
                    },
                    getFeature(content.getClass()));

            return appCall;
        }
    }

    private class WebShareHandler extends ModeHandler {
        @Override
        public Object getMode() {
            return Mode.WEB;
        }

        @Override
        public boolean canShow(final ShareContent content, boolean isBestEffort) {
            return (content != null) && ShareDialog.canShowWebTypeCheck(content.getClass());
        }

        @Override
        public AppCall createAppCall(final ShareContent content) {
            logDialogShare(getActivityContext(), content, Mode.WEB);

            final AppCall appCall = createBaseAppCall();

            ShareContentValidation.validateForWebShare(content);

            Bundle params;
            if (content instanceof ShareLinkContent) {
                params = WebDialogParameters.create((ShareLinkContent)content);
            } else if (content instanceof SharePhotoContent) {
                final SharePhotoContent photoContent =
                        createAndMapAttachments((SharePhotoContent)content, appCall.getCallId());
                params = WebDialogParameters.create(photoContent);
            } else {
                params = WebDialogParameters.create((ShareOpenGraphContent)content);
            }

            DialogPresenter.setupAppCallForWebDialog(
                    appCall,
                    getActionName(content),
                    params);

            return appCall;
        }

        private String getActionName(ShareContent shareContent) {
            if (shareContent instanceof ShareLinkContent
                    || shareContent instanceof SharePhotoContent) {
                return WEB_SHARE_DIALOG;
            } else if (shareContent instanceof ShareOpenGraphContent) {
                return WEB_OG_SHARE_DIALOG;
            }

            return null;
        }

        private SharePhotoContent createAndMapAttachments(
                final SharePhotoContent content,
                final UUID callId) {
            final SharePhotoContent.Builder contentBuilder =
                    new SharePhotoContent.Builder().readFrom(content);
            final List<SharePhoto> photos = new ArrayList<>();
            final List<NativeAppCallAttachmentStore.Attachment> attachments = new ArrayList<>();
            for (int i = 0; i < content.getPhotos().size(); i++) {
                SharePhoto sharePhoto = content.getPhotos().get(i);
                final Bitmap photoBitmap = sharePhoto.getBitmap();

                if (photoBitmap != null) {
                    NativeAppCallAttachmentStore.Attachment attachment =
                            NativeAppCallAttachmentStore.createAttachment(callId, photoBitmap);
                    sharePhoto = new SharePhoto.Builder()
                            .readFrom(sharePhoto)
                            .setImageUrl(Uri.parse(attachment.getAttachmentUrl()))
                            .setBitmap(null)
                            .build();
                    attachments.add(attachment);
                }

                photos.add(sharePhoto);
            }
            contentBuilder.setPhotos(photos);
            NativeAppCallAttachmentStore.addAttachments(attachments);
            return contentBuilder.build();
        }
    }

    private class FeedHandler extends ModeHandler {
        @Override
        public Object getMode() {
            return Mode.FEED;
        }

        @Override
        public boolean canShow(final ShareContent content, boolean isBestEffort) {
            return (content instanceof ShareLinkContent)
                    || (content instanceof ShareFeedContent);
        }

        @Override
        public AppCall createAppCall(final ShareContent content) {
            logDialogShare(getActivityContext(), content, Mode.FEED);
            AppCall appCall = createBaseAppCall();
            Bundle params;
            if (content instanceof ShareLinkContent) {
                ShareLinkContent linkContent = (ShareLinkContent)content;
                ShareContentValidation.validateForWebShare(linkContent);
                params = WebDialogParameters.createForFeed(linkContent);
            } else {
                ShareFeedContent feedContent = (ShareFeedContent)content;
                params = WebDialogParameters.createForFeed(feedContent);
            }

            DialogPresenter.setupAppCallForWebDialog(
                    appCall,
                    FEED_DIALOG,
                    params);

            return appCall;
        }
    }

    private static DialogFeature getFeature(
            Class<? extends ShareContent> contentType) {
        if (ShareLinkContent.class.isAssignableFrom(contentType)) {
            return ShareDialogFeature.SHARE_DIALOG;
        } else if (SharePhotoContent.class.isAssignableFrom(contentType)) {
            return ShareDialogFeature.PHOTOS;
        } else if (ShareVideoContent.class.isAssignableFrom(contentType)) {
            return ShareDialogFeature.VIDEO;
        } else if (ShareOpenGraphContent.class.isAssignableFrom(contentType)) {
            return OpenGraphActionDialogFeature.OG_ACTION_DIALOG;
        } else if (ShareMediaContent.class.isAssignableFrom(contentType)) {
            return ShareDialogFeature.MULTIMEDIA;
        }
        return null;
    }

    private void logDialogShare(Context context, ShareContent content, Mode mode) {
        String displayType;
        if (isAutomaticMode) {
            mode = Mode.AUTOMATIC;
        }

        switch (mode) {
            case AUTOMATIC:
                displayType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_AUTOMATIC;
                break;
            case WEB:
                displayType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_WEB;
                break;
            case NATIVE:
                displayType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_NATIVE;
                break;
            default:
                displayType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_UNKNOWN;
                break;
        }

        String contentType;
        DialogFeature dialogFeature = getFeature(content.getClass());
        if (dialogFeature == ShareDialogFeature.SHARE_DIALOG) {
            contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_STATUS;
        } else if (dialogFeature == ShareDialogFeature.PHOTOS) {
            contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_PHOTO;
        } else if (dialogFeature == ShareDialogFeature.VIDEO) {
            contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_VIDEO;
        } else if (dialogFeature == OpenGraphActionDialogFeature.OG_ACTION_DIALOG) {
            contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_OPENGRAPH;
        } else {
            contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_UNKNOWN;
        }

        AppEventsLogger logger = AppEventsLogger.newLogger(context);
        Bundle parameters = new Bundle();
        parameters.putString(
                AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW,
                displayType
        );
        parameters.putString(
                AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_TYPE,
                contentType
        );
        logger.logSdkEvent(AnalyticsEvents.EVENT_SHARE_DIALOG_SHOW, null, parameters);
    }
}
