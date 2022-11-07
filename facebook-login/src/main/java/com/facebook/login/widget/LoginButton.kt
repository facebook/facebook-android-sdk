/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login.widget

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import com.facebook.CallbackManager
import com.facebook.FacebookButtonBase
import com.facebook.FacebookCallback
import com.facebook.FacebookSdk
import com.facebook.Profile
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.ServerProtocol
import com.facebook.internal.Utility
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.DefaultAudience
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.LoginTargetApp
import com.facebook.login.R
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A Log In/Log Out button that maintains login state and logs in/out for the app.
 *
 * This control requires the app ID to be specified in the AndroidManifest.xml.
 */
open class LoginButton
protected constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
    analyticsButtonCreatedEventName: String,
    analyticsButtonTappedEventName: String
) : FacebookButtonBase(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    analyticsButtonCreatedEventName,
    analyticsButtonTappedEventName
) {
  // ***
  // Keep all the enum values in sync with attrs.xml
  // ***
  /** The display modes for the login button tool tip.  */
  enum class ToolTipMode(private val stringValue: String, val intValue: Int) {
    /**
     * Default display mode. A server query will determine if the tool tip should be displayed and,
     * if so, what the string shown to the user should be.
     */
    AUTOMATIC("automatic", 0),

    /** Display the tool tip with a local string--regardless of what the server returns  */
    DISPLAY_ALWAYS("display_always", 1),

    /** Never display the tool tip--regardless of what the server says  */
    NEVER_DISPLAY("never_display", 2);

    override fun toString(): String {
      return stringValue
    }

    companion object {
      val DEFAULT = AUTOMATIC

      fun fromInt(enumValue: Int): ToolTipMode? = values().find { it.intValue == enumValue }
    }
  }

  private var confirmLogout = false

  var loginText: String? = null
    set(value) {
      field = value
      setButtonText()
    }

  var logoutText: String? = null
    set(value) {
      field = value
      setButtonText()
    }

  protected val properties: LoginButtonProperties = LoginButtonProperties()

  private var toolTipChecked = false

  /**
   * Style (background) of the Tool Tip popup. Currently a blue style and a black style are
   * supported. Blue is default
   */
  var toolTipStyle = ToolTipPopup.Style.BLUE

  /**
   * The mode of the Tool Tip popup. Currently supported modes are default (normal behavior),
   * always_on (popup remains up until forcibly dismissed), and always_off (popup doesn't show)
   */
  var toolTipMode: ToolTipMode = ToolTipMode.DEFAULT

  /**
   * The amount of time (in milliseconds) that the tool tip will be shown to the user. The
   * default is {@value com.facebook.login.widget.ToolTipPopup#DEFAULT_POPUP_DISPLAY_TIME}. Any
   * value that is less than or equal to zero will cause the tool tip to be displayed indefinitely.
   */
  var toolTipDisplayTime = ToolTipPopup.DEFAULT_POPUP_DISPLAY_TIME

  private var toolTipPopup: ToolTipPopup? = null
  private var accessTokenTracker: AccessTokenTracker? = null

  protected var loginManagerLazy: Lazy<LoginManager> = lazy { LoginManager.getInstance() }

  private var customButtonRadius: Float? = null
  private var customButtonTransparency: Int = MAX_BUTTON_TRANSPARENCY

  /**
   * The logger ID associated with the LoginButton
   */
  val loggerID = UUID.randomUUID().toString()

  /** The callback manager registered to the login button. */
  var callbackManager: CallbackManager? = null
    private set

  private var androidXLoginCaller: ActivityResultLauncher<Collection<String>>? = null

  open class LoginButtonProperties {
    var defaultAudience = DefaultAudience.FRIENDS
    var permissions: List<String> = emptyList()
    var loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK
    var authType = ServerProtocol.DIALOG_REREQUEST_AUTH_TYPE
    var loginTargetApp = LoginTargetApp.FACEBOOK
    var shouldSkipAccountDeduplication = false
      protected set
    var messengerPageId: String? = null
    var resetMessengerState = false

    fun clearPermissions() {
      permissions = emptyList()
    }
  }

  /**
   * Create the LoginButton by inflating from XML
   */
  constructor(context: Context) : this(
      context,
      null,
      0,
      0,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_DID_TAP
  )

  /**
   * Create the LoginButton by inflating from XML
   */
  constructor(context: Context, attrs: AttributeSet?) : this(
      context,
      attrs,
      0,
      0,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_DID_TAP
  )

  /**
   * Create the LoginButton by inflating from XML and applying a style.
   */
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(
      context,
      attrs,
      defStyle,
      0,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_CREATE,
      AnalyticsEvents.EVENT_LOGIN_BUTTON_DID_TAP
  )

  /**
   * Default audience to use when the user logs in. This value is only useful when
   * specifying publish permissions for the native login dialog.
   */
  var defaultAudience: DefaultAudience
    get() = properties.defaultAudience
    set(value) {
      properties.defaultAudience = value
    }

  /**
   * Set the permissions to use when the user logs in. The permissions here can only be read
   * permissions. If any publish permissions are included, the login attempt by the user will fail.
   * The LoginButton can only be associated with either read permissions or publish permissions, but
   * not both. Calling both setReadPermissions and setPublishPermissions on the same instance of
   * LoginButton will result in an exception being thrown unless clearPermissions is called in
   * between.
   *
   * This method is only meaningful if called before the user logs in. If this is called after
   * login, and the list of permissions passed in is not a subset of the permissions granted during
   * the authorization, it will log an error.
   *
   * It's important to always pass in a consistent set of permissions to this method, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   *
   * @param permissions the read permissions to use
   * @throws UnsupportedOperationException if setPublishPermissions has been called
   */
  @Deprecated("Use setPermissions instead", replaceWith = ReplaceWith("setPermissions"))
  fun setReadPermissions(permissions: List<String>) {
    properties.permissions = permissions
  }

  /**
   * Set the permissions to use when the user logs in. The permissions here can only be read
   * permissions. If any publish permissions are included, the login attempt by the user will fail.
   * The LoginButton can only be associated with either read permissions or publish permissions, but
   * not both. Calling both setReadPermissions and setPublishPermissions on the same instance of
   * LoginButton will result in an exception being thrown unless clearPermissions is called in
   * between.
   *
   * This method is only meaningful if called before the user logs in. If this is called after
   * login, and the list of permissions passed in is not a subset of the permissions granted during
   * the authorization, it will log an error.
   *
   * It's important to always pass in a consistent set of permissions to this method, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   *
   * @param permissions the read permissions to use
   * @throws UnsupportedOperationException if setPublishPermissions has been called
   */
  @Deprecated("Use setPermissions instead", replaceWith = ReplaceWith("setPermissions"))
  fun setReadPermissions(vararg permissions: String?) {
    properties.permissions = listOfNotNull(*permissions)
  }

  /**
   * Set the permissions to use when the user logs in. The permissions here can be either read or
   * write permissions.
   *
   * This method is only meaningful if called before the user logs in. If this is called
   * after login, and the list of permissions passed in is not a subset of the permissions granted
   * during the authorization, it will log an error.
   *
   * It's important to always pass in a consistent set of permissions to this method, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   *
   * @param permissions the read and write permissions to use
   */
  fun setPermissions(vararg permissions: String?) {
    properties.permissions = listOfNotNull(*permissions)
  }

  /**
   * Set the permissions to use when the user logs in. The permissions here should only be publish
   * permissions. If any read permissions are included, the login attempt by the user may fail. The
   * LoginButton can only be associated with either read permissions or publish permissions, but not
   * both. Calling both setReadPermissions and setPublishPermissions on the same instance of
   * LoginButton will result in an exception being thrown unless clearPermissions is called in
   * between.
   *
   * This method is only meaningful if called before the user logs in. If this is called after
   * login, and the list of permissions passed in is not a subset of the permissions granted during
   * the authorization, it will log an error.
   *
   * It's important to always pass in a consistent set of permissions to this method, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   *
   * @param permissions the publish permissions to use
   * @throws UnsupportedOperationException if setReadPermissions has been called
   * @throws IllegalArgumentException if permissions is null or empty
   */
  @Deprecated("Use setPermissions instead", replaceWith = ReplaceWith("setPermissions"))
  fun setPublishPermissions(permissions: List<String>) {
    properties.permissions = permissions
  }

  /**
   * Set the permissions to use when the user logs in. The permissions here should only be publish
   * permissions. If any read permissions are included, the login attempt by the user may fail. The
   * LoginButton can only be associated with either read permissions or publish permissions, but not
   * both. Calling both setReadPermissions and setPublishPermissions on the same instance of
   * LoginButton will result in an exception being thrown unless clearPermissions is called in
   * between.
   *
   * This method is only meaningful if called before the user logs in. If this is called after
   * login, and the list of permissions passed in is not a subset of the permissions granted during
   * the authorization, it will log an error.
   *
   * It's important to always pass in a consistent set of permissions to this method, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   *
   * @param permissions the publish permissions to use
   * @throws UnsupportedOperationException if setReadPermissions has been called
   * @throws IllegalArgumentException if permissions is null or empty
   */
  @Deprecated("Use setPermissions instead", replaceWith = ReplaceWith("setPermissions"))
  fun setPublishPermissions(vararg permissions: String?) {
    properties.permissions = listOfNotNull(*permissions)
  }

  /** Clears the permissions currently associated with this LoginButton. */
  fun clearPermissions() {
    properties.clearPermissions()
  }

  /**
   * The [LoginBehavior][com.facebook.login.LoginBehavior] that specifies what behaviors
   * should be attempted during authorization. If it is null, the default
   * [LoginBehavior.NATIVE_WITH_FALLBACK] will be used.
   */
  var loginBehavior: LoginBehavior
    get() = properties.loginBehavior
    set(value) {
      properties.loginBehavior = value
    }

  /**
   * The [LoginTargetApp] that specifies what app in the Facebook Family of Apps will be used for
   * authorization. If null is specified, the default [LoginTargetApp.FACEBOOK] will be used.
   */
  var loginTargetApp: LoginTargetApp
    get() = properties.loginTargetApp
    set(value) {
      properties.loginTargetApp = value
    }

  /**
   * The authType to be used.
   */
  var authType: String
    get() = properties.authType
    set(value) {
      properties.authType = value
    }

  /**
   * The messengerPageId being used.
   */
  var messengerPageId: String?
    get() = properties.messengerPageId
    set(value) {
      properties.messengerPageId = value
    }

  /**
   * The resetMessengerState being used.
   */
  var resetMessengerState: Boolean
    get() = properties.resetMessengerState
    set(value) {
      properties.resetMessengerState = value
    }

  /**
   * Indicates if we should skip account deduplication. The setter is only available in
   * FamilyLoginButton as this is an x-FoA specific feature. Default is false(not skipping).
   */
  val shouldSkipAccountDeduplication: Boolean
    get() = properties.shouldSkipAccountDeduplication

  /** Dismisses the Tooltip if it is currently visible */
  fun dismissToolTip() {
    toolTipPopup?.dismiss()
    toolTipPopup = null
  }

  /**
   * Registers a login callback to the given callback manager.
   *
   * @param callbackManager The callback manager that will encapsulate the callback.
   * @param callback The login callback that will be called on login completion.
   */
  fun registerCallback(callbackManager: CallbackManager, callback: FacebookCallback<LoginResult>) {
    loginManagerLazy.value.registerCallback(callbackManager, callback)
    if (this.callbackManager == null) {
      this.callbackManager = callbackManager
    } else if (this.callbackManager !== callbackManager) {
      Log.w(
          TAG,
          "You're registering a callback on the one Facebook login button with two different callback managers. "
              + "It's almost wrong and may cause unexpected results. "
              + "Only the first callback manager will be used for handling activity result with androidx."
      )
    }
  }

  /**
   * Unregisters a login callback to the given callback manager.
   *
   * @param callbackManager The callback manager that will encapsulate the callback.
   */
  fun unregisterCallback(callbackManager: CallbackManager) =
      loginManagerLazy.value.unregisterCallback(callbackManager)

  @get:StringRes
  protected val loginButtonContinueLabel: Int
    get() = R.string.com_facebook_loginview_log_in_button_continue

  @AutoHandleExceptions
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (context is ActivityResultRegistryOwner) {
      val context = context as ActivityResultRegistryOwner
      androidXLoginCaller = context
          .activityResultRegistry
          .register(
              "facebook-login",
              loginManagerLazy.value.createLogInActivityResultContract(callbackManager, loggerID)
          ) { }
    }
    accessTokenTracker?.let {
      if (it.isTracking) {
        it.startTracking()
        setButtonText()
      }
    }
  }

  @AutoHandleExceptions
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (!toolTipChecked && !isInEditMode) {
      toolTipChecked = true
      checkToolTipSettings()
    }
  }

  @AutoHandleExceptions
  private fun showToolTipPerSettings(settings: FetchedAppSettings?) {
    if (settings != null && settings.nuxEnabled && visibility == VISIBLE) {
      displayToolTip(settings.nuxContent)
    }
  }

  @AutoHandleExceptions
  private fun displayToolTip(toolTipString: String) {
    val toolTipPopup = ToolTipPopup(toolTipString, this)
    toolTipPopup.setStyle(toolTipStyle)
    toolTipPopup.setNuxDisplayTime(toolTipDisplayTime)
    toolTipPopup.show()
    this.toolTipPopup = toolTipPopup
  }

  @AutoHandleExceptions
  private fun checkToolTipSettings() {
    when (toolTipMode) {
      ToolTipMode.AUTOMATIC -> {
        // kick off an async request
        val appId = Utility.getMetadataApplicationId(context)
        FacebookSdk.getExecutor().execute {
          val settings = FetchedAppSettingsManager.queryAppSettings(appId, false)
          activity.runOnUiThread { showToolTipPerSettings(settings) }
        }
      }
      ToolTipMode.DISPLAY_ALWAYS -> {
        val toolTipString = resources.getString(R.string.com_facebook_tooltip_default)
        displayToolTip(toolTipString)
      }
      ToolTipMode.NEVER_DISPLAY -> {}
    }
  }

  @AutoHandleExceptions
  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    setButtonText()
  }

  @AutoHandleExceptions
  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    androidXLoginCaller?.unregister()
    accessTokenTracker?.stopTracking()
    dismissToolTip()
  }

  @AutoHandleExceptions
  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    // If the visibility is not VISIBLE, we want to dismiss the tooltip if it is there
    if (visibility != VISIBLE) {
      dismissToolTip()
    }
  }

  /**
   * The permissions to use when the user logs in. The permissions here can be either read or
   * write permissions.
   *
   * Setting this field is only meaningful if called before the user logs in. If this is set
   * after login, and the list of permissions passed in is not a subset of the permissions granted
   * during the authorization, it will log an error.
   *
   * It's important to always set a consistent set of permissions to this field, or manage
   * the setting of permissions outside of the LoginButton class altogether (by using the
   * LoginManager explicitly).
   */
  // For testing purposes only
  var permissions: List<String>
    get() = properties.permissions
    set(value) {
      properties.permissions = value
    }

  @AutoHandleExceptions
  override fun configureButton(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int) {
    super.configureButton(context, attrs, defStyleAttr, defStyleRes)
    setInternalOnClickListener(newLoginClickListener)

    parseLoginButtonAttributes(context, attrs, defStyleAttr, defStyleRes)

    if (isInEditMode) {
      // cannot use a drawable in edit mode, so setting the background color instead
      // of a background resource.
      setBackgroundColor(resources.getColor(com.facebook.common.R.color.com_facebook_blue))
      // hardcoding in edit mode as getResources().getString() doesn't seem to work in
      // Android Studio
      loginText = "Continue with Facebook"
    } else {
      accessTokenTracker = object : AccessTokenTracker() {
        override fun onCurrentAccessTokenChanged(
            oldAccessToken: AccessToken?, currentAccessToken: AccessToken?) {
          setButtonText()
          setButtonIcon()
        }
      }
    }
    setButtonText()
    setButtonRadius()
    setButtonTransparency()
    setButtonIcon()
  }

  protected open val newLoginClickListener: LoginClickListener
    get() = LoginClickListener()

  override val defaultStyleResource: Int
    get() = R.style.com_facebook_loginview_default_style

  @AutoHandleExceptions
  protected fun parseLoginButtonAttributes(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int) {
    toolTipMode = ToolTipMode.DEFAULT
    val styleAttributes =
        context
            .theme
            .obtainStyledAttributes(
                attrs, R.styleable.com_facebook_login_view, defStyleAttr, defStyleRes)
    try {
      confirmLogout =
          styleAttributes.getBoolean(R.styleable.com_facebook_login_view_com_facebook_confirm_logout, true)
      loginText = styleAttributes.getString(R.styleable.com_facebook_login_view_com_facebook_login_text)
      logoutText = styleAttributes.getString(R.styleable.com_facebook_login_view_com_facebook_logout_text)
      toolTipMode =
          ToolTipMode.fromInt(
              styleAttributes.getInt(
                  R.styleable.com_facebook_login_view_com_facebook_tooltip_mode,
                  ToolTipMode.DEFAULT.intValue)) ?: ToolTipMode.DEFAULT
      // If no button radius specified, defaults to 'com_facebook_button_corner_radius'
      if (styleAttributes.hasValue(R.styleable.com_facebook_login_view_com_facebook_login_button_radius)) {
        customButtonRadius =
            styleAttributes.getDimension(
                R.styleable.com_facebook_login_view_com_facebook_login_button_radius, 0.0f)
      }
      customButtonTransparency =
          styleAttributes.getInteger(
              R.styleable.com_facebook_login_view_com_facebook_login_button_transparency,
              MAX_BUTTON_TRANSPARENCY)
      customButtonTransparency = max(MIN_BUTTON_TRANSPARENCY, customButtonTransparency)
      customButtonTransparency = min(MAX_BUTTON_TRANSPARENCY, customButtonTransparency)
    } finally {
      styleAttributes.recycle()
    }
  }

  @AutoHandleExceptions
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val fontMetrics = paint.fontMetrics
    val height =
        (compoundPaddingTop
            + ceil((abs(fontMetrics.top) + abs(fontMetrics.bottom)).toDouble()).toInt()
            + compoundPaddingBottom)

    val resources = resources
    val logInWidth = getLoginButtonWidth(widthMeasureSpec)

    val text = logoutText ?: resources.getString(R.string.com_facebook_loginview_log_out_button)

    val logOutWidth = measureButtonWidth(text)

    val width = resolveSize(max(logInWidth, logOutWidth), widthMeasureSpec)
    setMeasuredDimension(width, height)
  }

  @AutoHandleExceptions
  protected fun getLoginButtonWidth(widthMeasureSpec: Int): Int {
    val resources = resources
    var logInWidth: Int
    var text = loginText
    val width: Int
    if (text == null) {
      text = resources.getString(R.string.com_facebook_loginview_log_in_button_continue)
      logInWidth = measureButtonWidth(text)
      width = resolveSize(logInWidth, widthMeasureSpec)
      if (width < logInWidth) {
        text = resources.getString(R.string.com_facebook_loginview_log_in_button)
      }
    }
    logInWidth = measureButtonWidth(text)
    return logInWidth
  }

  @AutoHandleExceptions
  private fun measureButtonWidth(text: String): Int {
    val textWidth = measureTextWidth(text)
    return (compoundPaddingLeft
        + compoundDrawablePadding
        + textWidth
        + compoundPaddingRight)
  }

  @AutoHandleExceptions
  protected fun setButtonText() {
    val resources = resources
    if (!isInEditMode && AccessToken.isCurrentAccessTokenActive()) {
      text = logoutText ?: resources.getString(R.string.com_facebook_loginview_log_out_button)
    } else {
      if (loginText != null) {
        text = loginText
      } else {
        var text = resources.getString(loginButtonContinueLabel)
        val width = width
        // if the width is 0, we are going to measure size, so use the long text
        if (width != 0) {
          // we have a specific width, check if the long text fits
          val measuredWidth = measureButtonWidth(text)
          if (measuredWidth > width) {
            // it doesn't fit, use the shorter text
            text = resources.getString(R.string.com_facebook_loginview_log_in_button)
          }
        }
        setText(text)
      }
    }
  }

  @AutoHandleExceptions
  protected fun setButtonIcon() {
    this.setCompoundDrawablesWithIntrinsicBounds(
        AppCompatResources.getDrawable(
            context, com.facebook.common.R.drawable.com_facebook_button_icon),
        null,
        null,
        null)
  }

  @TargetApi(29)
  @AutoHandleExceptions
  protected fun setButtonRadius() {
    val customButtonRadius = this.customButtonRadius ?: return
    val buttonDrawable = this.background
    // Only configure corner radius for GradientDrawables or wrappers
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && buttonDrawable is StateListDrawable) {
      for (i in 0 until buttonDrawable.stateCount) {
        val childDrawable = buttonDrawable.getStateDrawable(i) as? GradientDrawable
        childDrawable?.let { it.cornerRadius = customButtonRadius }
      }
    }
    if (buttonDrawable is GradientDrawable) {
      buttonDrawable.cornerRadius = customButtonRadius
    }
  }

  @AutoHandleExceptions
  protected fun setButtonTransparency() {
    background.alpha = customButtonTransparency
  }

  @get:AutoHandleExceptions
  override val defaultRequestCode: Int
    get() = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()

  @AutoHandleExceptions
  protected open inner class LoginClickListener : OnClickListener {
    override fun onClick(v: View) {
      callExternalOnClickListener(v)
      val accessToken = AccessToken.getCurrentAccessToken()
      val accessTokenActive = AccessToken.isCurrentAccessTokenActive()
      if (accessTokenActive) {
        // Log out
        performLogout(context)
      } else {
        performLogin()
      }
      val logger = InternalAppEventsLogger(context)
      val parameters = Bundle()
      parameters.putInt("logging_in", if (accessToken != null) 0 else 1)
      parameters.putInt("access_token_expired", if (accessTokenActive) 1 else 0)
      logger.logEventImplicitly(AnalyticsEvents.EVENT_LOGIN_VIEW_USAGE, parameters)
    }

    protected fun performLogin() {
      val loginManager = getLoginManager()
      val androidXLoginCaller = androidXLoginCaller
      if (androidXLoginCaller != null) {
        // if no callback manager is registered with the button, an empty callback manager is
        // created for calling the static callbacks.
        val resultContact = androidXLoginCaller.contract
            as LoginManager.FacebookLoginActivityResultContract

        resultContact.callbackManager = callbackManager ?: CallbackManagerImpl()
        androidXLoginCaller.launch(properties.permissions)
      } else if (fragment !== null) {
        fragment?.let { loginManager.logIn(it, properties.permissions, loggerID) }
      } else if (nativeFragment !== null) {
        nativeFragment?.let { loginManager.logIn(it, properties.permissions, loggerID) }
      } else {
        loginManager.logIn(activity, properties.permissions, loggerID)
      }
    }

    protected fun performLogout(context: Context) {
      val loginManager = getLoginManager()
      if (confirmLogout) {
        // Create a confirmation dialog
        val logout = resources.getString(R.string.com_facebook_loginview_log_out_action)
        val cancel = resources.getString(R.string.com_facebook_loginview_cancel_action)
        val message: String
        val profile = Profile.getCurrentProfile()
        message = if (profile?.name != null) {
          String.format(
              resources.getString(R.string.com_facebook_loginview_logged_in_as), profile.name)
        } else {
          resources.getString(R.string.com_facebook_loginview_logged_in_using_facebook)
        }
        val builder = AlertDialog.Builder(context)
        builder
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(logout) { _, _ -> loginManager.logOut() }
            .setNegativeButton(cancel, null)
        builder.create().show()
      } else {
        loginManager.logOut()
      }
    }

    protected open fun getLoginManager(): LoginManager {
      val manager = LoginManager.getInstance()
      manager.setDefaultAudience(defaultAudience)
      manager.setLoginBehavior(loginBehavior)
      manager.setLoginTargetApp(loginTargetApp)
      manager.setAuthType(authType)
      manager.setFamilyLogin(isFamilyLogin)
      manager.setShouldSkipAccountDeduplication(shouldSkipAccountDeduplication)
      manager.setMessengerPageId(messengerPageId)
      manager.setResetMessengerState(resetMessengerState)
      return manager
    }

    protected val loginTargetApp: LoginTargetApp
      get() = LoginTargetApp.FACEBOOK
    protected val isFamilyLogin: Boolean
      get() = false
  }

  companion object {
    private val TAG = LoginButton::class.java.name
    private const val MAX_BUTTON_TRANSPARENCY = 255
    private const val MIN_BUTTON_TRANSPARENCY = 0
  }
}
