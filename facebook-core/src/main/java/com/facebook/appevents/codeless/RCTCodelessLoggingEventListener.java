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

package com.facebook.appevents.codeless;

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.appevents.codeless.internal.EventBinding;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.lang.ref.WeakReference;

@AutoHandleExceptions
public class RCTCodelessLoggingEventListener {
  public static AutoLoggingOnTouchListener getOnTouchListener(
      EventBinding mapping, View rootView, View hostView) {
    return new AutoLoggingOnTouchListener(mapping, rootView, hostView);
  }

  public static class AutoLoggingOnTouchListener implements View.OnTouchListener {

    public AutoLoggingOnTouchListener(
        final EventBinding mapping, final View rootView, final View hostView) {
      if (null == mapping || null == rootView || null == hostView) {
        return;
      }

      this.existingOnTouchListener = ViewHierarchy.getExistingOnTouchListener(hostView);

      this.mapping = mapping;
      this.hostView = new WeakReference<View>(hostView);
      this.rootView = new WeakReference<View>(rootView);
      supportCodelessLogging = true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (motionEvent.getAction() == MotionEvent.ACTION_UP && mapping != null) {
        CodelessLoggingEventListener.logEvent(mapping, rootView.get(), hostView.get());
      }

      // If there is an existing listener then call its onTouch function else return false
      return this.existingOnTouchListener != null
          && this.existingOnTouchListener.onTouch(view, motionEvent);
    }

    public boolean getSupportCodelessLogging() {
      return supportCodelessLogging;
    }

    private EventBinding mapping;
    private WeakReference<View> hostView;
    private WeakReference<View> rootView;
    private @Nullable View.OnTouchListener existingOnTouchListener;
    private boolean supportCodelessLogging = false;
  }
}
