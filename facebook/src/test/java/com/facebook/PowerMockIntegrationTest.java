/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

/** This test makes sure PowerMock integration works. */
@PrepareForTest({FacebookSdk.class})
public class PowerMockIntegrationTest extends FacebookPowerMockTestCase {

  @Test
  public void testStaticMethodOverrides() {
    mockStatic(FacebookSdk.class);
    String applicationId = "1234";

    when(FacebookSdk.getApplicationId()).thenReturn(applicationId);
    assertEquals(applicationId, FacebookSdk.getApplicationId());

    String clientToken = "clienttoken";
    when(FacebookSdk.getClientToken()).thenReturn(clientToken);
    assertEquals(clientToken, FacebookSdk.getClientToken());
  }
}
