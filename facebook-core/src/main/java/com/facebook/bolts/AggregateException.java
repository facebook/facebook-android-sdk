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

package com.facebook.bolts;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates multiple {@code Throwable}s that may be thrown in the process of a task's execution.
 *
 * @see Task#whenAll(java.util.Collection)
 */
public class AggregateException extends Exception {
  private static final long serialVersionUID = 1L;

  private List<Throwable> innerThrowables;

  /**
   * Constructs a new {@code AggregateException} with the current stack trace, the specified detail
   * message and with references to the inner throwables that are the cause of this exception.
   *
   * @param detailMessage The detail message for this exception.
   * @param innerThrowables The exceptions that are the cause of the current exception.
   */
  public AggregateException(String detailMessage, List<? extends Throwable> innerThrowables) {
    super(
        detailMessage,
        innerThrowables != null && innerThrowables.size() > 0 ? innerThrowables.get(0) : null);
    this.innerThrowables = Collections.unmodifiableList(innerThrowables);
  }

  @Override
  public void printStackTrace(PrintStream err) {
    super.printStackTrace(err);

    int currentIndex = -1;
    for (Throwable throwable : innerThrowables) {
      err.append("\n");
      err.append("  Inner throwable #");
      err.append(Integer.toString(++currentIndex));
      err.append(": ");
      throwable.printStackTrace(err);
      err.append("\n");
    }
  }

  @Override
  public void printStackTrace(PrintWriter err) {
    super.printStackTrace(err);

    int currentIndex = -1;
    for (Throwable throwable : innerThrowables) {
      err.append("\n");
      err.append("  Inner throwable #");
      err.append(Integer.toString(++currentIndex));
      err.append(": ");
      throwable.printStackTrace(err);
      err.append("\n");
    }
  }
}
