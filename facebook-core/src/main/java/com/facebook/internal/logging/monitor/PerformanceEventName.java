package com.facebook.internal.logging.monitor;

public enum PerformanceEventName {
  FB_CORE_STARTUP("FB_CORE_STARTUP");

  private String eventName;

  PerformanceEventName(String eventName) {
    this.eventName = eventName;
  }

  @Override
  public String toString() {
    return this.eventName;
  }
}
