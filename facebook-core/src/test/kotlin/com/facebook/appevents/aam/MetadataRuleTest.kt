/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.aam

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MetadataRuleTest : FacebookTestCase() {
  companion object {
    const val VALID_RULES_JSON =
        """{"r1":{"k": "key1,key2","v":"val1"},"r2":{"k":"key1","v":"val2"}}"""
    const val INVALID_JSON = """{"r1":{"k": "key1,key2","v":"val1"}"""
    const val EMPTY_KEY_JSON = """{"r1":{"v":"val1"}, "r2":{"k":"key1"}}"""
  }

  @Test
  fun `test valid metadata rules`() {
    MetadataRule.updateRules(VALID_RULES_JSON)
    val rules = MetadataRule.getRules()
    rules.forEach {
      if (it.name == "r1") {
        assertThat(it.keyRules).contains("key1")
        assertThat(it.keyRules).contains("key2")
        assertThat(it.valRule).isEqualTo("val1")
      }
    }
    assertThat(MetadataRule.getEnabledRuleNames()).contains("r1")
    assertThat(MetadataRule.getEnabledRuleNames()).contains("r2")
  }

  @Test
  fun `test invalid metadata rules`() {
    MetadataRule.updateRules(INVALID_JSON)
    assertThat(MetadataRule.getEnabledRuleNames()).isEmpty()
  }

  @Test
  fun `test empty key in metadata rules`() {
    MetadataRule.updateRules(EMPTY_KEY_JSON)
    assertThat(MetadataRule.getEnabledRuleNames()).doesNotContain("r1")
    assertThat(MetadataRule.getEnabledRuleNames()).contains("r2")
  }
}
