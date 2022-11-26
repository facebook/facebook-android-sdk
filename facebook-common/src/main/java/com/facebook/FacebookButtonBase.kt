/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.core.content.ContextCompat
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.FragmentWrapper
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

/** A base class for a facebook button. */
@AutoHandleExceptions
@SuppressLint("ResourceType")
abstract class FacebookButtonBase
protected constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
    analyticsButtonCreatedEventName: String,
    analyticsButtonTappedEventName: String
) : Button(context, attrs, 0) {
  /** Gets the logger eventName when login button is created. */
  protected val analyticsButtonCreatedEventName: String

  /** The logger eventName when login button is tap. */
  protected val analyticsButtonTappedEventName: String
  private var externalOnClickListener: OnClickListener? = null
  private var internalOnClickListener: OnClickListener? = null
  private var overrideCompoundPadding = false
  private var overrideCompoundPaddingLeft = 0
  private var overrideCompoundPaddingRight = 0
  private var parentFragment: FragmentWrapper? = null
  protected abstract val defaultRequestCode: Int

  /** The native fragment that contains this control. */
  val nativeFragment: Fragment?
    get() = parentFragment?.nativeFragment

  /**
   * Sets the fragment that contains this control. This allows the button to be embedded inside a
   * native Fragment, and will allow the fragment to receive the [onActivityResult]
   * [Fragment.onActivityResult] call rather than the Activity.
   *
   * @param fragment the android.app.Fragment that contains this control
   */
  fun setFragment(fragment: Fragment) {
    parentFragment = FragmentWrapper(fragment)
  }

  /**
   * Gets the AndroidX fragment that contains this control.
   *
   * @return The android.app.Fragment that contains this control.
   */
  val fragment: androidx.fragment.app.Fragment?
    get() = parentFragment?.supportFragment

  /**
   * Set the fragment that contains this control. This allows the button to be embedded inside an
   * AndroidX Fragment, and will allow the fragment to receive the [onActivityResult]
   * [Fragment.onActivityResult] call rather than the Activity.
   *
   * @param fragment the androidx.fragment.app.Fragment that contains this control
   */
  fun setFragment(fragment: androidx.fragment.app.Fragment) {
    parentFragment = FragmentWrapper(fragment)
  }

  /**
   * Get the context of activity result register owner that controls this button. Return null if the
   * context is not available.
   */
  val androidxActivityResultRegistryOwner: ActivityResultRegistryOwner?
    get() {
      val activity = activity
      return if (activity is ActivityResultRegistryOwner) {
        activity
      } else null
    }

  override fun setOnClickListener(l: OnClickListener?) {
    externalOnClickListener = l
  }

  /** Returns the request code used for this Button. */
  open val requestCode: Int
    get() = defaultRequestCode

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!isInEditMode) {
      logButtonCreated(context)
    }
  }

  override fun onDraw(canvas: Canvas) {
    val centered = this.gravity and Gravity.CENTER_HORIZONTAL != 0
    if (centered) {
      // if the text is centered, we need to adjust the frame for the titleLabel based on the
      // size of the text in order to keep the text centered in the button without adding
      // extra blank space to the right when unnecessary
      // 1. the text fits centered within the button without colliding with the image
      //    (imagePaddingWidth)
      // 2. the text would run into the image, so adjust the insets to effectively left align
      //    it (textPaddingWidth)
      val compoundPaddingLeft = compoundPaddingLeft
      val compoundPaddingRight = compoundPaddingRight
      val compoundDrawablePadding = compoundDrawablePadding
      val textX = compoundPaddingLeft + compoundDrawablePadding
      val textContentWidth = width - textX - compoundPaddingRight
      val textWidth = measureTextWidth(text.toString())
      val textPaddingWidth = (textContentWidth - textWidth) / 2
      val imagePaddingWidth = (compoundPaddingLeft - paddingLeft) / 2
      val inset = Math.min(textPaddingWidth, imagePaddingWidth)
      overrideCompoundPaddingLeft = compoundPaddingLeft - inset
      overrideCompoundPaddingRight = compoundPaddingRight + inset
      overrideCompoundPadding = true
    }
    super.onDraw(canvas)
    overrideCompoundPadding = false
  }

  override fun getCompoundPaddingLeft(): Int {
    return if (overrideCompoundPadding) overrideCompoundPaddingLeft
    else super.getCompoundPaddingLeft()
  }

  override fun getCompoundPaddingRight(): Int {
    return if (overrideCompoundPadding) overrideCompoundPaddingRight
    else super.getCompoundPaddingRight()
  }

  protected open val activity: Activity
    get() {
      var context = context
      while (context !is Activity && context is ContextWrapper) {
        context = context.baseContext
      }
      if (context is Activity) {
        return context
      }
      throw FacebookException("Unable to get Activity.")
    }

  protected open val defaultStyleResource: Int = 0

  protected open fun measureTextWidth(text: String?): Int {
    return Math.ceil(paint.measureText(text).toDouble()).toInt()
  }

  protected open fun configureButton(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) {
    parseBackgroundAttributes(context, attrs, defStyleAttr, defStyleRes)
    parseCompoundDrawableAttributes(context, attrs, defStyleAttr, defStyleRes)
    parseContentAttributes(context, attrs, defStyleAttr, defStyleRes)
    parseTextAttributes(context, attrs, defStyleAttr, defStyleRes)
    setupOnClickListener()
  }

  protected open fun callExternalOnClickListener(v: View?) {
    externalOnClickListener?.onClick(v)
  }

  protected open fun setInternalOnClickListener(l: OnClickListener?) {
    internalOnClickListener = l
  }

  protected open fun logButtonCreated(context: Context?) {
    val logger = InternalAppEventsLogger.createInstance(context, null)
    logger.logEventImplicitly(analyticsButtonCreatedEventName)
  }

  protected open fun logButtonTapped(context: Context?) {
    val logger = InternalAppEventsLogger.createInstance(context, null)
    logger.logEventImplicitly(analyticsButtonTappedEventName)
  }

  private fun parseBackgroundAttributes(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) {
    // TODO, figure out why com_facebook_button_like_background.xml doesn't work in designers
    if (isInEditMode) {
      return
    }
    val attrsResources = intArrayOf(android.R.attr.background)
    val a = context.theme.obtainStyledAttributes(attrs, attrsResources, defStyleAttr, defStyleRes)
    try {
      if (a.hasValue(0)) {
        val backgroundResource = a.getResourceId(0, 0)
        if (backgroundResource != 0) {
          setBackgroundResource(backgroundResource)
        } else {
          setBackgroundColor(a.getColor(0, 0))
        }
      } else {
        // fallback, if no background specified, fill with Facebook blue
        setBackgroundColor(
            ContextCompat.getColor(context, com.facebook.common.R.color.com_facebook_blue))
      }
    } finally {
      a.recycle()
    }
  }

  @SuppressLint("ResourceType")
  private fun parseCompoundDrawableAttributes(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) {
    val attrsResources =
        intArrayOf(
            android.R.attr.drawableLeft,
            android.R.attr.drawableTop,
            android.R.attr.drawableRight,
            android.R.attr.drawableBottom,
            android.R.attr.drawablePadding)
    val a = context.theme.obtainStyledAttributes(attrs, attrsResources, defStyleAttr, defStyleRes)
    compoundDrawablePadding =
        try {
          setCompoundDrawablesWithIntrinsicBounds(
              a.getResourceId(0, 0),
              a.getResourceId(1, 0),
              a.getResourceId(2, 0),
              a.getResourceId(3, 0))
          a.getDimensionPixelSize(4, 0)
        } finally {
          a.recycle()
        }
  }

  private fun parseContentAttributes(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) {
    val attrsResources =
        intArrayOf(
            android.R.attr.paddingLeft,
            android.R.attr.paddingTop,
            android.R.attr.paddingRight,
            android.R.attr.paddingBottom)
    val a = context.theme.obtainStyledAttributes(attrs, attrsResources, defStyleAttr, defStyleRes)
    try {
      setPadding(
          a.getDimensionPixelSize(0, 0),
          a.getDimensionPixelSize(1, 0),
          a.getDimensionPixelSize(2, 0),
          a.getDimensionPixelSize(3, 0))
    } finally {
      a.recycle()
    }
  }

  private fun parseTextAttributes(
      context: Context,
      attrs: AttributeSet?,
      defStyleAttr: Int,
      defStyleRes: Int
  ) {
    val colorResources = intArrayOf(android.R.attr.textColor)
    val colorAttrs =
        context.theme.obtainStyledAttributes(attrs, colorResources, defStyleAttr, defStyleRes)
    try {
      setTextColor(colorAttrs.getColorStateList(0))
    } finally {
      colorAttrs.recycle()
    }
    val gravityResources = intArrayOf(android.R.attr.gravity)
    val gravityAttrs =
        context.theme.obtainStyledAttributes(attrs, gravityResources, defStyleAttr, defStyleRes)
    gravity =
        try {
          gravityAttrs.getInt(0, Gravity.CENTER)
        } finally {
          gravityAttrs.recycle()
        }
    val attrsResources =
        intArrayOf(android.R.attr.textSize, android.R.attr.textStyle, android.R.attr.text)
    val a = context.theme.obtainStyledAttributes(attrs, attrsResources, defStyleAttr, defStyleRes)
    text =
        try {
          setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimensionPixelSize(0, 0).toFloat())
          val typeface = typeface
          setTypeface(Typeface.create(typeface, Typeface.BOLD))
          a.getString(2)
        } finally {
          a.recycle()
        }
  }

  private fun setupOnClickListener() {
    // set the listener on super so that consumers can set another listener that this will
    // forward to
    super.setOnClickListener { v ->
      logButtonTapped(context)
      val internalOnClickListener = internalOnClickListener
      if (internalOnClickListener != null) {
        internalOnClickListener.onClick(v)
      } else externalOnClickListener?.onClick(v)
    }
  }

  init {
    var defStyleRes = defStyleRes
    defStyleRes = if (defStyleRes == 0) defaultStyleResource else defStyleRes
    defStyleRes =
        if (defStyleRes == 0) com.facebook.common.R.style.com_facebook_button else defStyleRes
    configureButton(context, attrs, defStyleAttr, defStyleRes)
    this.analyticsButtonCreatedEventName = analyticsButtonCreatedEventName
    this.analyticsButtonTappedEventName = analyticsButtonTappedEventName
    isClickable = true
    isFocusable = true
  }
}
