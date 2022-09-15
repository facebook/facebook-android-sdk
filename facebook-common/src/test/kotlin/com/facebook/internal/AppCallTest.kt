package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AppCallTest : FacebookPowerMockTestCase() {
  private lateinit var uuid1: UUID
  private lateinit var uuid2: UUID
  private lateinit var appCall1: AppCall
  private lateinit var appCall2: AppCall
  @Before
  fun init() {
    uuid1 = UUID.randomUUID()
    uuid2 = UUID.randomUUID()
    appCall1 = AppCall(1, uuid1)
    appCall2 = AppCall(2, uuid2)
  }
  @Test
  fun `test reset current pending AppCall`() {
    appCall1.setPending()
    assertThat(appCall2.setPending()).isTrue
  }

  @Test
  fun `test finish current pending AppCall`() {
    appCall1.setPending()
    val pendingAppCall = AppCall.finishPendingCall(uuid1, 1)
    assertThat(pendingAppCall).isEqualTo(appCall1)
  }

  @Test
  fun `test finish wrong current pending AppCall`() {
    appCall1.setPending()
    val pendingAppCall = AppCall.finishPendingCall(uuid2, 2)
    assertThat(pendingAppCall).isNull()
  }
}
