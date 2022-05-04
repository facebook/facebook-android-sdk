/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.aam

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class MetadataRuleTest : FacebookTestCase() {
  companion object {
    const val VALID_RULES_JSON =
        """{"r1":{"k": "key1,key2","v":"val1"},"r2":{"k":"key1","v":"val2"}}"""
    const val INVALID_JSON = """{"r1":{"k": "key1,key2","v":"val1"}"""
    const val EMPTY_KEY_JSON = """{"r1":{"v":"val1"}, "r2":{"k":"key1"}}"""
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T101830281
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
