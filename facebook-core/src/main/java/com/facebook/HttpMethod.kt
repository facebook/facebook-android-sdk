/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** Enumeration of HTTP methods supported by Request */
enum class HttpMethod {
  /** Use HTTP method "GET" for the request */
  GET,

  /** Use HTTP method "POST" for the request */
  POST,

  /** Use HTTP method "DELETE" for the request */
  DELETE
}
