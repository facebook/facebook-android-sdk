package com.facebook.util.common

import android.content.BroadcastReceiver
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.FacebookSdk
import java.util.*
import org.junit.Assert
import org.mockito.Mockito

inline fun <reified T : Exception> assertThrows(runnable: () -> Any?) {
  try {
    runnable.invoke()
  } catch (e: Throwable) {
    if (e is T) {
      return
    }
    Assert.fail(
        "expected ${T::class.qualifiedName} but caught " + "${e::class.qualifiedName} instead")
  }
  Assert.fail("expected ${T::class.qualifiedName}")
}

inline fun <reified T> anyObject(): T = Mockito.any<T>(T::class.java) ?: castNullAsNonnull()

@Suppress("UNCHECKED_CAST") fun <T> castNullAsNonnull(): T = null as T

fun mockLocalBroadcastManager(): LocalBroadcastManager {
  val localBroadcastManager = Mockito.mock(LocalBroadcastManager::class.java)
  val registeredReceiver = ArrayList<BroadcastReceiver>()
  Mockito.doAnswer { invocation ->
        val receiver = invocation.getArgument<BroadcastReceiver>(0)
        registeredReceiver.add(receiver)
        null
      }
      .`when`(localBroadcastManager)
      .registerReceiver(anyObject(), anyObject())
  Mockito.doAnswer { invocation ->
        val receiver = invocation.getArgument<BroadcastReceiver>(0)
        registeredReceiver.remove(receiver)
        null
      }
      .`when`(localBroadcastManager)
      .unregisterReceiver(anyObject())
  Mockito.doAnswer { invocation ->
        val intent = invocation.getArgument<Intent>(0)
        for (receiver in registeredReceiver) {
          receiver.onReceive(FacebookSdk.getApplicationContext(), intent)
        }
        null
      }
      .`when`(localBroadcastManager)
      .sendBroadcast(anyObject())
  return localBroadcastManager
}
