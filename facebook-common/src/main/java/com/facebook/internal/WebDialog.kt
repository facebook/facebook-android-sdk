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
package com.facebook.internal

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.AsyncTask
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.AccessToken.Companion.isCurrentAccessTokenActive
import com.facebook.FacebookDialogException
import com.facebook.FacebookException
import com.facebook.FacebookGraphResponseException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk.WEB_DIALOG_THEME
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getGraphApiVersion
import com.facebook.FacebookSdk.getSdkVersion
import com.facebook.FacebookServiceException
import com.facebook.GraphRequest
import com.facebook.GraphRequestAsyncTask
import com.facebook.common.R
import com.facebook.internal.ServerProtocol.getDialogAuthority
import com.facebook.internal.ServerProtocol.getInstagramDialogAuthority
import com.facebook.internal.Utility.buildUri
import com.facebook.internal.Utility.getMetadataApplicationId
import com.facebook.internal.Utility.isChromeOS
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.isWebUri
import com.facebook.internal.Utility.logd
import com.facebook.internal.Utility.mustFixWindowParamsForAutofill
import com.facebook.internal.Utility.parseUrlQueryString
import com.facebook.internal.Utility.putJSONValueInBundle
import com.facebook.internal.Validate.notNullOrEmpty
import com.facebook.internal.Validate.sdkInitialized
import com.facebook.login.LoginTargetApp
import com.facebook.share.internal.ShareConstants
import com.facebook.share.internal.ShareInternalUtility
import com.facebook.share.widget.ShareDialog
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern
import org.json.JSONArray

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * This class provides a mechanism for displaying Facebook Web dialogs inside a Dialog. Helper
 * methods are provided to construct commonly-used dialogs, or a caller can specify arbitrary
 * parameters to call other dialogs.
 */
open class WebDialog : Dialog {
  private var url: String? = null
  private var expectedRedirectUrl = ServerProtocol.DIALOG_REDIRECT_URI
  /**
   * Gets the listener which will be notified when the dialog finishes.
   *
   * @return the listener, or null if none has been specified
   */
  /**
   * Sets the listener which will be notified when the dialog finishes.
   *
   * @param listener the listener to notify, or null if no notification is desired
   */
  var onCompleteListener: OnCompleteListener? = null
  protected var webView: WebView? = null
    private set
  private var spinner: ProgressDialog? = null
  private var crossImageView: ImageView? = null
  private var contentFrameLayout: FrameLayout? = null
  private var uploadTask: UploadStagingResourcesTask? = null
  protected var isListenerCalled = false
    private set
  private var isDetached = false
  protected var isPageFinished = false
    private set

  // Used to work around an Android Autofill bug - see Utility.mustFixWindowParamsForAutofill
  private var windowParams: WindowManager.LayoutParams? = null

  fun interface InitCallback {
    fun onInit(webView: WebView?)
  }

  /**
   * Interface that implements a listener to be called when the user's interaction with the dialog
   * completes, whether because the dialog finished successfully, or it was cancelled, or an error
   * was encountered.
   */
  fun interface OnCompleteListener {
    /**
     * Called when the dialog completes.
     *
     * @param values on success, contains the values returned by the dialog
     * @param error on an error, contains an exception describing the error
     */
    fun onComplete(values: Bundle?, error: FacebookException?)
  }

  /**
   * Constructor which can be used to display a dialog with an already-constructed URL.
   *
   * @param context the context to use to display the dialog
   * @param url the URL of the Web Dialog to display; no validation is done on this URL, but it
   * should be a valid URL pointing to a Facebook Web Dialog
   */
  protected constructor(context: Context, url: String) : this(context, url, getWebDialogTheme())

  /**
   * Constructor which can be used to display a dialog with an already-constructed URL and a custom
   * theme.
   *
   * @param context the context to use to display the dialog
   * @param url the URL of the Web Dialog to display; no validation is done on this URL, but it
   * should be a valid URL pointing to a Facebook Web Dialog
   * @param theme identifier of a theme to pass to the Dialog class
   */
  private constructor(
      context: Context,
      url: String,
      theme: Int
  ) : super(context, if (theme == 0) getWebDialogTheme() else theme) {
    this.url = url
  }

  /**
   * Constructor which will construct the URL of the Web dialog based on the specified parameters.
   *
   * @param context the context to use to display the dialog
   * @param action the portion of the dialog URL following "dialog/"
   * @param parameters parameters which will be included as part of the URL
   * @param theme identifier of a theme to pass to the Dialog class
   * @param targetApp the target app associated with the oauth dialog
   * @param listener the listener to notify, or null if no notification is desired
   */
  private constructor(
      context: Context,
      action: String?,
      parameters: Bundle?,
      theme: Int,
      targetApp: LoginTargetApp,
      listener: OnCompleteListener?
  ) : super(context, if (theme == 0) getWebDialogTheme() else theme) {
    var parameters = parameters
    if (parameters == null) {
      parameters = Bundle()
    }
    val isChromeOS = isChromeOS(context)
    expectedRedirectUrl =
        if (isChromeOS) ServerProtocol.DIALOG_REDIRECT_CHROME_OS_URI
        else ServerProtocol.DIALOG_REDIRECT_URI

    // our webview client only handles the redirect uri we specify, so just hard code it here
    parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, expectedRedirectUrl)
    parameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, DISPLAY_TOUCH)
    parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getApplicationId())
    parameters.putString(
        ServerProtocol.DIALOG_PARAM_SDK_VERSION,
        String.format(Locale.ROOT, "android-%s", getSdkVersion()))
    onCompleteListener = listener
    if (action == ShareDialog.WEB_SHARE_DIALOG &&
        parameters.containsKey(ShareConstants.WEB_DIALOG_PARAM_MEDIA)) {
      uploadTask = UploadStagingResourcesTask(action, parameters)
    } else {
      val uri: Uri =
          when (targetApp) {
            LoginTargetApp.INSTAGRAM ->
                buildUri(
                    getInstagramDialogAuthority(), ServerProtocol.INSTAGRAM_OAUTH_PATH, parameters)
            else ->
                buildUri(
                    getDialogAuthority(),
                    getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
                    parameters)
          }
      url = uri.toString()
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (webView != null && webView?.canGoBack() == true) {
        webView?.goBack()
        return true
      } else {
        cancel()
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun dismiss() {
    webView?.let { it.stopLoading() }
    if (!isDetached) {
      spinner?.let {
        if (it.isShowing) {
          it.dismiss()
        }
      }
    }
    super.dismiss()
  }

  override fun onStart() {
    super.onStart()
    if (uploadTask != null && uploadTask?.status == AsyncTask.Status.PENDING) {
      uploadTask?.execute()
      spinner?.show()
    } else {
      resize()
    }
  }

  override fun onStop() {
    uploadTask?.let { task ->
      task.cancel(true)
      spinner?.let { it.dismiss() }
    }
    super.onStop()
  }

  override fun onDetachedFromWindow() {
    isDetached = true
    super.onDetachedFromWindow()
  }

  override fun onAttachedToWindow() {
    isDetached = false
    if (mustFixWindowParamsForAutofill(context) &&
        windowParams != null &&
        windowParams?.token == null) {
      windowParams?.token = ownerActivity?.window?.attributes?.token
      logd(LOG_TAG, "Set token on onAttachedToWindow(): " + windowParams?.token)
    }
    super.onAttachedToWindow()
  }

  override fun onWindowAttributesChanged(params: WindowManager.LayoutParams) {
    if (params.token == null) {
      // Always store the last params, so the token can be updated when the dialog is
      // attached to the window.
      windowParams = params
    }
    super.onWindowAttributesChanged(params)
  }

  override fun onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    spinner = ProgressDialog(context)
    spinner?.requestWindowFeature(Window.FEATURE_NO_TITLE)
    spinner?.setMessage(context.getString(R.string.com_facebook_loading))
    // Stops people from accidently cancelling the login flow
    spinner?.setCanceledOnTouchOutside(false)
    spinner?.setOnCancelListener { cancel() }
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    contentFrameLayout = FrameLayout(context)

    // First calculate how big the frame layout should be
    resize()
    window?.setGravity(Gravity.CENTER)

    // resize the dialog if the soft keyboard comes up
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    /* Create the 'x' image, but don't add to the contentFrameLayout layout yet
     * at this point, we only need to know its drawable width and height
     * to place the webview
     */
    createCrossImage()
    if (url != null) {
      /* Now we know 'x' drawable width and height,
       * layout the webview and add it the contentFrameLayout layout
       */
      val crossWidth = checkNotNull(crossImageView).drawable.intrinsicWidth
      setUpWebView(crossWidth / 2 + 1)
    }

    /* Finally add the 'x' image to the contentFrameLayout layout and
     * add contentFrameLayout to the Dialog view
     */
    contentFrameLayout?.addView(
        crossImageView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    setContentView(checkNotNull(contentFrameLayout))
  }

  protected fun setExpectedRedirectUrl(expectedRedirectUrl: String) {
    this.expectedRedirectUrl = expectedRedirectUrl
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  open fun parseResponseUri(urlString: String?): Bundle {
    val u = Uri.parse(urlString)
    val b = parseUrlQueryString(u.query)
    b.putAll(parseUrlQueryString(u.fragment))
    return b
  }

  fun resize() {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    // always use the portrait dimensions to do the scaling calculations so we always get a portrait
    // shaped
    // web dialog
    val width =
        if (metrics.widthPixels < metrics.heightPixels) metrics.widthPixels
        else metrics.heightPixels
    val height =
        if (metrics.widthPixels < metrics.heightPixels) metrics.heightPixels
        else metrics.widthPixels
    val dialogWidth =
        Math.min(
            getScaledSize(
                width, metrics.density, NO_PADDING_SCREEN_WIDTH, MAX_PADDING_SCREEN_WIDTH),
            metrics.widthPixels)
    val dialogHeight =
        Math.min(
            getScaledSize(
                height, metrics.density, NO_PADDING_SCREEN_HEIGHT, MAX_PADDING_SCREEN_HEIGHT),
            metrics.heightPixels)
    window?.setLayout(dialogWidth, dialogHeight)
  }

  /**
   * Returns a scaled size (either width or height) based on the parameters passed.
   *
   * @param screenSize a pixel dimension of the screen (either width or height)
   * @param density density of the screen
   * @param noPaddingSize the size at which there's no padding for the dialog
   * @param maxPaddingSize the size at which to apply maximum padding for the dialog
   * @return a scaled size.
   */
  private fun getScaledSize(
      screenSize: Int,
      density: Float,
      noPaddingSize: Int,
      maxPaddingSize: Int
  ): Int {
    val scaledSize = (screenSize.toFloat() / density).toInt()
    val scaleFactor: Double
    scaleFactor =
        if (scaledSize <= noPaddingSize) {
          1.0
        } else if (scaledSize >= maxPaddingSize) {
          MIN_SCALE_FACTOR
        } else {
          // between the noPadding and maxPadding widths, we take a linear reduction to go from 100%
          // of screen size down to MIN_SCALE_FACTOR
          (MIN_SCALE_FACTOR +
              (maxPaddingSize - scaledSize).toDouble() /
                  (maxPaddingSize - noPaddingSize).toDouble() * (1.0 - MIN_SCALE_FACTOR))
        }
    return (screenSize * scaleFactor).toInt()
  }

  protected fun sendSuccessToListener(values: Bundle?) {
    if (onCompleteListener != null && !isListenerCalled) {
      isListenerCalled = true
      onCompleteListener?.onComplete(values, null)
      dismiss()
    }
  }

  protected fun sendErrorToListener(error: Throwable?) {
    if (onCompleteListener != null && !isListenerCalled) {
      isListenerCalled = true
      val facebookException: FacebookException =
          if (error is FacebookException) {
            error
          } else {
            FacebookException(error)
          }
      onCompleteListener?.onComplete(null, facebookException)
      dismiss()
    }
  }

  override fun cancel() {
    if (onCompleteListener != null && !isListenerCalled) {
      sendErrorToListener(FacebookOperationCanceledException())
    }
  }

  private fun createCrossImage() {
    crossImageView = ImageView(context)
    // Dismiss the dialog when user click on the 'x'
    crossImageView?.setOnClickListener { cancel() }
    val crossDrawable = context.resources.getDrawable(R.drawable.com_facebook_close)
    crossImageView?.setImageDrawable(crossDrawable)
    /* 'x' should not be visible while webview is loading
     * make it visible only after webview has fully loaded
     */
    crossImageView?.visibility = View.INVISIBLE
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun setUpWebView(margin: Int) {
    val webViewContainer = LinearLayout(context)
    webView =
        object : WebView(context) {
          /* Prevent NPE on Motorola 2.2 devices
           * See https://groups.google.com/forum/?fromgroups=#!topic/android-developers/ktbwY2gtLKQ
           */
          override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
            try {
              super.onWindowFocusChanged(hasWindowFocus)
            } catch (e: NullPointerException) {}
          }
        }
    initCallback?.let { it.onInit(webView) }

    webView?.setVerticalScrollBarEnabled(false)
    webView?.setHorizontalScrollBarEnabled(false)
    webView?.setWebViewClient(DialogWebViewClient())
    webView?.getSettings()?.javaScriptEnabled = true
    webView?.loadUrl(checkNotNull(url))
    webView?.setLayoutParams(
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    webView?.setVisibility(View.INVISIBLE)
    webView?.getSettings()?.savePassword = false
    webView?.getSettings()?.saveFormData = false
    webView?.setFocusable(true)
    webView?.setFocusableInTouchMode(true)
    webView?.setOnTouchListener(
        View.OnTouchListener { v, event ->
          if (!v.hasFocus()) {
            v.requestFocus()
          }
          false
        })
    webViewContainer.setPadding(margin, margin, margin, margin)
    webViewContainer.addView(webView)
    webViewContainer.setBackgroundColor(BACKGROUND_GRAY)
    contentFrameLayout?.addView(webViewContainer)
  }

  private inner class DialogWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
      logd(LOG_TAG, "Redirect URL: $url")
      val parsedURL = Uri.parse(url)
      val isPlatformDialogURL =
          (parsedURL.path != null && Pattern.matches(PLATFORM_DIALOG_PATH_REGEX, parsedURL.path))
      if (url.startsWith(expectedRedirectUrl)) {
        val values = parseResponseUri(url)
        var error = values.getString("error")
        if (error == null) {
          error = values.getString("error_type")
        }
        var errorMessage = values.getString("error_msg")
        if (errorMessage == null) {
          errorMessage = values.getString("error_message")
        }
        if (errorMessage == null) {
          errorMessage = values.getString("error_description")
        }
        val errorCodeString = values.getString("error_code")
        var errorCode = FacebookRequestError.INVALID_ERROR_CODE
        if (errorCodeString !== null && !isNullOrEmpty(errorCodeString)) {
          errorCode =
              try {
                errorCodeString.toInt()
              } catch (ex: NumberFormatException) {
                FacebookRequestError.INVALID_ERROR_CODE
              }
        }
        if (isNullOrEmpty(error) &&
            isNullOrEmpty(errorMessage) &&
            errorCode == FacebookRequestError.INVALID_ERROR_CODE) {
          sendSuccessToListener(values)
        } else if (error != null &&
            (error == "access_denied" || error == "OAuthAccessDeniedException")) {
          cancel()
        } else if (errorCode == API_EC_DIALOG_CANCEL) {
          cancel()
        } else {
          val requestError = FacebookRequestError(errorCode, error, errorMessage)
          sendErrorToListener(FacebookServiceException(requestError, errorMessage))
        }
        return true
      } else if (url.startsWith(ServerProtocol.DIALOG_CANCEL_URI)) {
        cancel()
        return true
      } else if (isPlatformDialogURL || url.contains(DISPLAY_TOUCH)) {
        return false
      }
      // launch non-dialog URLs in a full browser
      return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
      } catch (e: ActivityNotFoundException) {
        false
      }
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
      super.onReceivedError(view, errorCode, description, failingUrl)
      sendErrorToListener(FacebookDialogException(description, errorCode, failingUrl))
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
      if (DISABLE_SSL_CHECK_FOR_TESTING) {
        handler.proceed()
      } else {
        super.onReceivedSslError(view, handler, error)
        handler.cancel()
        sendErrorToListener(FacebookDialogException(null, ERROR_FAILED_SSL_HANDSHAKE, null))
      }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
      logd(LOG_TAG, "Webview loading URL: $url")
      super.onPageStarted(view, url, favicon)
      if (!isDetached) {
        spinner?.show()
      }
    }

    override fun onPageFinished(view: WebView, url: String) {
      super.onPageFinished(view, url)
      if (!isDetached) {
        spinner?.dismiss()
      }
      /*
       * Once web view is fully loaded, set the contentFrameLayout background to be transparent
       * and make visible the 'x' image.
       */
      contentFrameLayout?.setBackgroundColor(Color.TRANSPARENT)
      webView?.visibility = View.VISIBLE
      crossImageView?.visibility = View.VISIBLE
      isPageFinished = true
    }
  }

  open class Builder {
    var context: Context? = null
      private set
    var applicationId: String? = null
      private set
    private var action: String? = null
    var theme = 0
      private set
    var listener: OnCompleteListener? = null
      private set
    var parameters: Bundle? = null
      private set
    private var accessToken: AccessToken? = null

    /**
     * Constructor that builds a dialog using either the current access token, or the application id
     * specified in the application/meta-data.
     *
     * @param context the Context within which the dialog will be shown.
     * @param action the portion of the dialog URL following www.facebook.com/dialog/. See
     * https://developers.facebook.com/docs/reference/dialogs/ for details.
     * @param parameters a Bundle containing parameters to pass as part of the URL.
     */
    constructor(context: Context, action: String, parameters: Bundle?) {
      accessToken = getCurrentAccessToken()
      if (!isCurrentAccessTokenActive()) {
        val applicationId = getMetadataApplicationId(context)
        if (applicationId != null) {
          this.applicationId = applicationId
        } else {
          throw FacebookException(
              "Attempted to create a builder without a valid" +
                  " access token or a valid default Application ID.")
        }
      }
      finishInit(context, action, parameters)
    }

    /**
     * Constructor that builds a dialog without an authenticated user.
     *
     * @param context the Context within which the dialog will be shown.
     * @param applicationId the application ID to be included in the dialog URL.
     * @param action the portion of the dialog URL following www.facebook.com/dialog/. See
     * https://developers.facebook.com/docs/reference/dialogs/ for details.
     * @param parameters a Bundle containing parameters to pass as part of the URL.
     */
    constructor(context: Context, applicationId: String?, action: String, parameters: Bundle?) {
      var applicationId = applicationId
      if (applicationId == null) {
        applicationId = getMetadataApplicationId(context)
      }
      notNullOrEmpty(applicationId, "applicationId")
      this.applicationId = applicationId
      finishInit(context, action, parameters)
    }

    /**
     * Sets a theme identifier which will be passed to the underlying Dialog.
     *
     * @param theme a theme identifier which will be passed to the Dialog class
     * @return the builder
     */
    fun setTheme(theme: Int): Builder {
      this.theme = theme
      return this
    }

    /**
     * Sets the listener which will be notified when the dialog finishes.
     *
     * @param listener the listener to notify, or null if no notification is desired
     * @return the builder
     */
    fun setOnCompleteListener(listener: OnCompleteListener?): Builder {
      this.listener = listener
      return this
    }

    /**
     * Constructs a WebDialog using the parameters provided. The dialog is not shown, but is ready
     * to be shown by calling Dialog.show().
     *
     * @return the WebDialog
     */
    open fun build(): WebDialog? {
      if (accessToken != null) {
        parameters?.putString(ServerProtocol.DIALOG_PARAM_APP_ID, accessToken?.applicationId)
        parameters?.putString(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, accessToken?.token)
      } else {
        parameters?.putString(ServerProtocol.DIALOG_PARAM_APP_ID, applicationId)
      }
      return newInstance(checkNotNull(context), action, parameters, theme, listener)
    }

    private fun finishInit(context: Context?, action: String, parameters: Bundle?) {
      this.context = context
      this.action = action
      if (parameters != null) {
        this.parameters = parameters
      } else {
        this.parameters = Bundle()
      }
    }
  }

  private inner class UploadStagingResourcesTask
  internal constructor(private val action: String, private val parameters: Bundle) :
      AsyncTask<Void, Void, Array<String?>?>() {
    private var exceptions: Array<Exception?> = emptyArray()
    override fun doInBackground(vararg p0: Void): Array<String?>? {
      val params = parameters.getStringArray(ShareConstants.WEB_DIALOG_PARAM_MEDIA) ?: return null
      val results = arrayOfNulls<String>(params.size)
      exceptions = arrayOfNulls(params.size)
      val latch = CountDownLatch(params.size)
      val tasks = ConcurrentLinkedQueue<GraphRequestAsyncTask>()
      val accessToken = getCurrentAccessToken()
      try {
        for (i in params.indices) {
          if (isCancelled) {
            for (task in tasks) {
              task.cancel(true)
            }
            return null
          }
          val uri = Uri.parse(params[i])
          if (isWebUri(uri)) {
            results[i] = uri.toString()
            latch.countDown()
            continue
          }
          val callback =
              GraphRequest.Callback { response ->
                try {
                  val error = response.error
                  if (error != null) {
                    var message = error.errorMessage
                    if (message == null) {
                      message = "Error staging photo."
                    }
                    throw FacebookGraphResponseException(response, message)
                  }
                  val data =
                      response.getJSONObject() ?: throw FacebookException("Error staging photo.")
                  val stagedImageUri =
                      data.optString("uri") ?: throw FacebookException("Error staging photo.")
                  results[i] = stagedImageUri
                } catch (e: Exception) {
                  exceptions[i] = e
                }
                latch.countDown()
              }
          val task =
              ShareInternalUtility.newUploadStagingResourceWithImageRequest(
                      accessToken, uri, callback)
                  .executeAsync()
          tasks.add(task)
        }
        latch.await()
      } catch (e: Exception) {
        for (task in tasks) {
          task.cancel(true)
        }
        return null
      }
      return results
    }

    override fun onPostExecute(results: Array<String?>?) {
      spinner?.dismiss()
      for (e in exceptions) {
        if (e != null) {
          sendErrorToListener(e)
          return
        }
      }
      if (results == null) {
        sendErrorToListener(FacebookException("Failed to stage photos for web dialog"))
        return
      }
      val resultList = results.asList()
      if (resultList.contains(null)) {
        sendErrorToListener(FacebookException("Failed to stage photos for web dialog"))
        return
      }
      putJSONValueInBundle(parameters, ShareConstants.WEB_DIALOG_PARAM_MEDIA, JSONArray(resultList))
      val uri =
          buildUri(
              getDialogAuthority(),
              getGraphApiVersion() + "/" + ServerProtocol.DIALOG_PATH + action,
              parameters)
      url = uri.toString()
      val crossWidth = checkNotNull(crossImageView).drawable.intrinsicWidth
      setUpWebView(crossWidth / 2 + 1)
    }
  }

  companion object {
    private const val LOG_TAG = Logger.LOG_TAG_BASE + "WebDialog"
    private const val DISPLAY_TOUCH = "touch"
    private const val PLATFORM_DIALOG_PATH_REGEX = "^/(v\\d+\\.\\d+/)??dialog/.*"
    private const val API_EC_DIALOG_CANCEL = 4201
    const val DISABLE_SSL_CHECK_FOR_TESTING = false

    // width below which there are no extra margins
    private const val NO_PADDING_SCREEN_WIDTH = 480

    // width beyond which we're always using the MIN_SCALE_FACTOR
    private const val MAX_PADDING_SCREEN_WIDTH = 800

    // height below which there are no extra margins
    private const val NO_PADDING_SCREEN_HEIGHT = 800

    // height beyond which we're always using the MIN_SCALE_FACTOR
    private const val MAX_PADDING_SCREEN_HEIGHT = 1280

    // the minimum scaling factor for the web dialog (50% of screen size)
    private const val MIN_SCALE_FACTOR = 0.5

    // translucent border around the webview
    private const val BACKGROUND_GRAY = 0xCC000000.toInt()
    private val DEFAULT_THEME = R.style.com_facebook_activity_theme

    @Volatile private var webDialogTheme = 0
    private var initCallback: InitCallback? = null

    @JvmStatic
    protected fun initDefaultTheme(context: Context?) {
      if (context == null) {
        return
      }
      val ai: ApplicationInfo? =
          try {
            context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA)
          } catch (e: PackageManager.NameNotFoundException) {
            return
          }
      if (ai?.metaData == null) {
        return
      }
      if (webDialogTheme == 0) {
        setWebDialogTheme(ai.metaData.getInt(WEB_DIALOG_THEME))
      }
    }

    @JvmStatic
    fun newInstance(
        context: Context,
        action: String?,
        parameters: Bundle?,
        theme: Int,
        listener: OnCompleteListener?
    ): WebDialog {
      initDefaultTheme(context)
      return WebDialog(context, action, parameters, theme, LoginTargetApp.FACEBOOK, listener)
    }

    @JvmStatic
    fun newInstance(
        context: Context,
        action: String?,
        parameters: Bundle?,
        theme: Int,
        targetApp: LoginTargetApp,
        listener: OnCompleteListener?
    ): WebDialog {
      initDefaultTheme(context)
      return WebDialog(context, action, parameters, theme, targetApp, listener)
    }

    /**
     * Gets the theme used by [com.facebook.internal.WebDialog]
     *
     * @return the theme
     */
    @JvmStatic
    fun getWebDialogTheme(): Int {
      sdkInitialized()
      return webDialogTheme
    }

    /**
     * Sets the theme used by [com.facebook.internal.WebDialog]
     *
     * @param theme A theme to use
     */
    @JvmStatic
    fun setWebDialogTheme(theme: Int) {
      webDialogTheme = if (theme != 0) theme else DEFAULT_THEME
    }

    @JvmStatic
    fun setInitCallback(callback: InitCallback?) {
      initCallback = callback
    }
  }
}
