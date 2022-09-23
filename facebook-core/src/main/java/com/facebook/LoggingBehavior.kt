/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/**
 * Specifies different categories of logging messages that can be generated.
 *
 * @see FacebookSdk.addLoggingBehavior
 */
enum class LoggingBehavior {
  /** Indicates that HTTP requests and a summary of responses should be logged. */
  REQUESTS,

  /**
   * Indicates that access tokens should be logged as part of the request logging; normally they are
   * not.
   */
  INCLUDE_ACCESS_TOKENS,

  /** Indicates that the entire raw HTTP response for each request should be logged. */
  INCLUDE_RAW_RESPONSES,

  /** Indicates that cache operations should be logged. */
  CACHE,

  /** Indicates the App Events-related operations should be logged. */
  APP_EVENTS,

  /**
   * Indicates that likely developer errors should be logged. (This is set by default in
   * LoggingBehavior.)
   */
  DEVELOPER_ERRORS,

  /**
   * Log debug warnings from API response, e.g. when friends fields requested, but user_friends
   * permission isn't granted.
   */
  GRAPH_API_DEBUG_WARNING,

  /**
   * Log warnings from API response, e.g. when requested feature will be deprecated in next version
   * of API. Info is the lowest level of severity, using it will result in logging all
   * GRAPH_API_DEBUG levels.
   */
  GRAPH_API_DEBUG_INFO
}
