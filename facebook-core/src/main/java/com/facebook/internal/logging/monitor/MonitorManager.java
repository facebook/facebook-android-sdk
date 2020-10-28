/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.internal.logging.monitor;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;

// This MonitorManager manages the Monitoring Feature. It's different from MonitorLoggingManager.
// It plays the same role as InstrumentManager, AppEventsManager, etc.
// Using this name to keep consistent of naming specific feature manager file.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MonitorManager {

  /** Abstraction for testability. */
  @VisibleForTesting
  public interface MonitorCreator {
    void enable();
  }

  private static MonitorCreator monitorCreator =
      new MonitorCreator() {
        @Override
        public void enable() {
          Monitor.enable();
        }
      };

  /**
   * Start Monitoring functionality.
   *
   * <p>Note that the function should be called after FacebookSdk is initialized. Otherwise,
   * exception FacebookSdkNotInitializedException will be thrown when loading and sending crash
   * reports.
   */
  public static void start() {
    if (!FacebookSdk.getMonitorEnabled()) {
      return;
    }
    String appId = FacebookSdk.getApplicationId();
    FetchedAppSettings settings = FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId);

    if (settings != null && settings.getMonitorViaDialogEnabled()) {
      monitorCreator.enable();
    }
  }

  @VisibleForTesting
  static void setMonitorCreator(MonitorCreator monitorCreator) {
    MonitorManager.monitorCreator = monitorCreator;
  }
}
