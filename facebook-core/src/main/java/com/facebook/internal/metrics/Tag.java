package com.facebook.internal.metrics;

public enum Tag {
  FACEBOOK_CORE_STARTUP("facebook_core_startup");

  private final String suffix;

  Tag(String suffix) {
    this.suffix = suffix;
  }

  public String getSuffix() {
    return this.suffix;
  }
}
