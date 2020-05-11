package com.facebook.internal.metrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import com.facebook.internal.Utility;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/*
   startMeasureFor and stopMeasureFor should always be called in pairs.
   Possible issue is that the scheduler might switch threads inbetween the calls, i.e
    1) call to startTag is made
    2) expectation is that the code you want to measure is ran here
    3) thread switches and unrelated code runs
    4) thread switches back to your code
    5) endTag is called, now it has logged more time than actually was spent in your context

    No data persistence if app crashes before the log is stored in SharedPreferences, but a crash should trigger a new cold start anyway.

    Measure as little code as possible and make sure you are measuring the right thing in the right places.

*/

public class MetricsUtil {
  private final String CLASS_TAG = "internal.MetricsUtil";
  private final String STARTUP_METRICS_PREFERENCES = "MetricsUtil";
  private final String TIME_DIFFERENCE_BASE_PREF = "time_difference";
  private MetricsUtil metricsUtil;
  private HashMap<Tag, Long> taggedStartTimer;

  private WeakReference<Context> ctx;

  private MetricsUtil(Context ctx) {
    taggedStartTimer = new HashMap<>();
    this.ctx = new WeakReference<>(ctx);
  }
  /*
     Should be given applicationContext
  */
  public synchronized MetricsUtil getInstance(Context ctx) {
    if (metricsUtil == null) {
      metricsUtil = new MetricsUtil(ctx);
      return metricsUtil;
    }
    return metricsUtil;
  }

  /*
     This method will override previous start time.
  */
  public void startMeasureFor(Tag tag) {
    taggedStartTimer.put(tag, SystemClock.elapsedRealtime());
  }

  /*
     This is no-op if startTag has not be called before
  */
  public void stopMeasureFor(Tag tag) {
    long endTime = SystemClock.elapsedRealtime();
    if (!taggedStartTimer.containsKey(tag)) {
      return;
    }
    long deltaTime = endTime - taggedStartTimer.get(tag);
    taggedStartTimer.remove(tag);
    updateLastTimeDifferenceFor(tag, deltaTime);
  }

  public long getLastTimeDifferenceFor(Tag tag) {
    if (ctx.get() == null) {
      Utility.logd(CLASS_TAG, "getLastTimeDifferenceFor: Context is null");
      return -1L;
    }
    SharedPreferences preferences =
        ctx.get().getSharedPreferences(STARTUP_METRICS_PREFERENCES, Context.MODE_PRIVATE);
    return preferences.getLong(TIME_DIFFERENCE_BASE_PREF + tag.getSuffix(), -1L);
  }

  private void updateLastTimeDifferenceFor(Tag tag, long deltaTime) {
    if (ctx.get() == null) {
      Utility.logd(CLASS_TAG, "updateLastTimeDifferenceFor: Context is null");
      return;
    }
    SharedPreferences preferences =
        ctx.get().getSharedPreferences(STARTUP_METRICS_PREFERENCES, Context.MODE_PRIVATE);
    preferences.edit().putLong(TIME_DIFFERENCE_BASE_PREF + tag.getSuffix(), deltaTime).apply();
  }
}
