/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

/**
 * This is a wrapper class for emphasizing that task failed due to bad `Executor`, rather than the
 * continuation block it self.
 */
class ExecutorException(e: Exception) :
    RuntimeException("An exception was thrown by an Executor", e)
