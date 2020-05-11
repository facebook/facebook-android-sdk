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

package com.example.shareit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PageSelector extends View {
  private static final int CIRCLE_RADIUS = 20;
  private static final int CIRCLE_MARGIN = 20;
  private static final int CIRCLE_SPACING = CIRCLE_RADIUS * 2 + CIRCLE_MARGIN;
  private Paint paintGray;
  private Paint paintWhite;

  private int mPosition;
  private int mImageCount;

  public PageSelector(Context context) {
    super(context);
    init();
  }

  public PageSelector(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int start_x = canvas.getWidth() / 2 - (CIRCLE_SPACING) * (mImageCount / 2);
    if (mImageCount % 2 == 0) {
      start_x += CIRCLE_SPACING / 2;
    }

    for (int i = 0; i < mImageCount; ++i) {
      Paint paint = (i == mPosition) ? paintWhite : paintGray;
      float x = start_x + i * CIRCLE_SPACING;
      float y = canvas.getHeight() / 2;
      canvas.drawCircle(x, y, CIRCLE_RADIUS, paint);
    }
  }

  public void setPosition(int position) {
    this.mPosition = position;
    invalidate();
  }

  public void setImageCount(int imageCount) {
    this.mImageCount = imageCount;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(CIRCLE_SPACING * mImageCount, CIRCLE_SPACING);
  }

  private void init() {
    paintGray = new Paint();
    paintGray.setStyle(Paint.Style.FILL);
    paintGray.setColor(Color.GRAY);

    paintWhite = new Paint();
    paintWhite.setStyle(Paint.Style.FILL);
    paintWhite.setColor(Color.WHITE);
  }
}
