/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.mockito.configuration

class MockitoConfiguration : DefaultMockitoConfiguration() {
  override fun enableClassCache(): Boolean {
    return false
  }
}
