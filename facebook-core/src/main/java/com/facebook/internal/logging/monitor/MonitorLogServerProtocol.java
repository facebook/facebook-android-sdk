package com.facebook.internal.logging.monitor;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MonitorLogServerProtocol {
  public static final String PARAM_EVENT_NAME = "event_name";
  public static final String PARAM_CATEGORY = "category";
  public static final String PARAM_UNIQUE_APPLICATION_ID = "unique_application_identifier";
  public static final String PARAM_DEVICE_MODEL = "device_model";
  public static final String PARAM_DEVICE_OS_VERSION = "device_os_version";
  public static final String PARAM_TIME_START = "time_start";
  public static final String PARAM_TIME_SPENT = "time_spent";

  public static final String APPLICATION_FIELDS = "fields";
  public static final String MONITOR_CONFIG = "monitoring_config";
  public static final String SAMPLE_RATES = "sample_rates";
  public static final String DEFAULT_SAMPLE_RATES_KEY = "default";
}
