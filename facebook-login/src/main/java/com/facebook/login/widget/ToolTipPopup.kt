/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.login.widget


import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.R
import java.lang.ref.WeakReference

/**
 * This displays a popup tool tip for a specified view.
 * @param text The text to be displayed in the tool tip
 * @param anchor The view to anchor this tool tip to.
 */
@AutoHandleExceptions
class ToolTipPopup(private val text: String, anchor: View) {
  /** The values here describe the styles available for the tool tip class.  */
  enum class Style {
    /**
     * The tool tip will be shown with a blue style; including a blue background and blue arrows.
     */
    BLUE,

    /**
     * The tool tip will be shown with a black style; including a black background and black arrows.
     */
    BLACK
  }

  private val anchorViewRef: WeakReference<View> = WeakReference(anchor)
  private val context: Context = anchor.context
  private var popupContent: PopupContentView? = null
  private var popupWindow: PopupWindow? = null
  private var style = Style.BLUE
  private var nuxDisplayTime = DEFAULT_POPUP_DISPLAY_TIME
  private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
    if (anchorViewRef.get() != null) {
      popupWindow?.let {
        if (it.isShowing) {
          if (it.isAboveAnchor) {
            popupContent?.showBottomArrow()
          } else {
            popupContent?.showTopArrow()
          }
        }
      }
    }
  }

  /**
   * Sets the [Style] of this tool tip.
   *
   * @param style the style for the tool tip
   */
  fun setStyle(style: Style) {
    this.style = style
  }

  /** Display this tool tip to the user  */
  fun show() {
    if (anchorViewRef.get() != null) {
      val popupContent = PopupContentView(context)
      this.popupContent = popupContent
      val body = popupContent.findViewById<View>(R.id.com_facebook_tooltip_bubble_view_text_body) as TextView
      body.text = text
      if (style == Style.BLUE) {
        popupContent.bodyFrame.setBackgroundResource(R.drawable.com_facebook_tooltip_blue_background)
        popupContent.bottomArrow.setImageResource(R.drawable.com_facebook_tooltip_blue_bottomnub)
        popupContent.topArrow.setImageResource(R.drawable.com_facebook_tooltip_blue_topnub)
        popupContent.xOut.setImageResource(R.drawable.com_facebook_tooltip_blue_xout)
      } else {
        popupContent.bodyFrame.setBackgroundResource(R.drawable.com_facebook_tooltip_black_background)
        popupContent.bottomArrow.setImageResource(R.drawable.com_facebook_tooltip_black_bottomnub)
        popupContent.topArrow.setImageResource(R.drawable.com_facebook_tooltip_black_topnub)
        popupContent.xOut.setImageResource(R.drawable.com_facebook_tooltip_black_xout)
      }
      val window = (context as Activity).window
      val decorView = window.decorView
      val decorWidth = decorView.width
      val decorHeight = decorView.height
      registerObserver()
      popupContent.measure(
          View.MeasureSpec.makeMeasureSpec(decorWidth, View.MeasureSpec.AT_MOST),
          View.MeasureSpec.makeMeasureSpec(decorHeight, View.MeasureSpec.AT_MOST),
      )
      val popupWindow = PopupWindow(
          popupContent,
          popupContent.measuredWidth,
          popupContent.measuredHeight,
      )
      this.popupWindow = popupWindow
      popupWindow.showAsDropDown(anchorViewRef.get())
      updateArrows()
      if (nuxDisplayTime > 0) {
        popupContent.postDelayed({ dismiss() }, nuxDisplayTime)
      }
      popupWindow.isTouchable = true
      popupContent.setOnClickListener { dismiss() }
    }
  }

  /**
   * Set the time (in milliseconds) the tool tip will be displayed. Any number less than or equal to
   * 0 will cause the tool tip to be displayed indefinitely
   *
   * @param displayTime The amount of time (in milliseconds) to display the tool tip
   */
  fun setNuxDisplayTime(displayTime: Long) {
    nuxDisplayTime = displayTime
  }

  private fun updateArrows() {
    popupWindow?.let {
      if (it.isShowing) {
        if (it.isAboveAnchor) {
          popupContent?.showBottomArrow()
        } else {
          popupContent?.showTopArrow()
        }
      }
    }
  }

  /** Dismiss the tool tip  */
  fun dismiss() {
    unregisterObserver()
    popupWindow?.dismiss()
  }

  private fun registerObserver() {
    unregisterObserver()
    anchorViewRef.get()?.viewTreeObserver?.addOnScrollChangedListener(scrollListener)
  }

  private fun unregisterObserver() {
    anchorViewRef.get()?.viewTreeObserver?.removeOnScrollChangedListener(scrollListener)
  }

  private inner class PopupContentView(context: Context) : FrameLayout(context) {
    val topArrow: ImageView
    val bottomArrow: ImageView
    val bodyFrame: View
    val xOut: ImageView

    init {
      val inflater = LayoutInflater.from(context)
      inflater.inflate(R.layout.com_facebook_tooltip_bubble, this)
      topArrow = findViewById<View>(R.id.com_facebook_tooltip_bubble_view_top_pointer) as ImageView
      bottomArrow = findViewById<View>(R.id.com_facebook_tooltip_bubble_view_bottom_pointer) as ImageView
      bodyFrame = findViewById(R.id.com_facebook_body_frame)
      xOut = findViewById<View>(R.id.com_facebook_button_xout) as ImageView
    }

    fun showTopArrow() {
      topArrow.visibility = VISIBLE
      bottomArrow.visibility = INVISIBLE
    }

    fun showBottomArrow() {
      topArrow.visibility = INVISIBLE
      bottomArrow.visibility = VISIBLE
    }
  }

  companion object {
    /** The default time that the tool tip will be displayed  */
    const val DEFAULT_POPUP_DISPLAY_TIME: Long = 6000
  }
}
