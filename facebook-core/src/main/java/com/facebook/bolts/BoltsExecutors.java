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

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Collection of {@link Executor}s to use in conjunction with {@link Task}. */
/* package */ final class BoltsExecutors {

  private static final BoltsExecutors INSTANCE = new BoltsExecutors();

  private static boolean isAndroidRuntime() {
    String javaRuntimeName = System.getProperty("java.runtime.name");
    if (javaRuntimeName == null) {
      return false;
    }
    return javaRuntimeName.toLowerCase(Locale.US).contains("android");
  }

  private final ExecutorService background;
  private final ScheduledExecutorService scheduled;
  private final Executor immediate;

  private BoltsExecutors() {
    background =
        !isAndroidRuntime()
            ? java.util.concurrent.Executors.newCachedThreadPool()
            : AndroidExecutors.newCachedThreadPool();
    scheduled = Executors.newSingleThreadScheduledExecutor();
    immediate = new ImmediateExecutor();
  }

  /** An {@link java.util.concurrent.Executor} that executes tasks in parallel. */
  public static ExecutorService background() {
    return INSTANCE.background;
  }

  /* package */ static ScheduledExecutorService scheduled() {
    return INSTANCE.scheduled;
  }

  /**
   * An {@link java.util.concurrent.Executor} that executes tasks in the current thread unless the
   * stack runs too deep, at which point it will delegate to {@link BoltsExecutors#background} in
   * order to trim the stack.
   */
  /* package */ static Executor immediate() {
    return INSTANCE.immediate;
  }

  /**
   * An {@link java.util.concurrent.Executor} that runs a runnable inline (rather than scheduling it
   * on a thread pool) as long as the recursion depth is less than MAX_DEPTH. If the executor has
   * recursed too deeply, it will instead delegate to the {@link Task#BACKGROUND_EXECUTOR} in order
   * to trim the stack.
   */
  private static class ImmediateExecutor implements Executor {
    private static final int MAX_DEPTH = 15;
    private ThreadLocal<Integer> executionDepth = new ThreadLocal<>();

    /**
     * Increments the depth.
     *
     * @return the new depth value.
     */
    private int incrementDepth() {
      Integer oldDepth = executionDepth.get();
      if (oldDepth == null) {
        oldDepth = 0;
      }
      int newDepth = oldDepth + 1;
      executionDepth.set(newDepth);
      return newDepth;
    }

    /**
     * Decrements the depth.
     *
     * @return the new depth value.
     */
    private int decrementDepth() {
      Integer oldDepth = executionDepth.get();
      if (oldDepth == null) {
        oldDepth = 0;
      }
      int newDepth = oldDepth - 1;
      if (newDepth == 0) {
        executionDepth.remove();
      } else {
        executionDepth.set(newDepth);
      }
      return newDepth;
    }

    @Override
    public void execute(Runnable command) {
      int depth = incrementDepth();
      try {
        if (depth <= MAX_DEPTH) {
          command.run();
        } else {
          BoltsExecutors.background().execute(command);
        }
      } finally {
        decrementDepth();
      }
    }
  }
}
