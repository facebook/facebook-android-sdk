package com.facebook.util.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert

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

inline fun <reified T : Any> anyObject(): T = anyOrNull<T>()

fun mockLocalBroadcastManager(applicationContext: Context): LocalBroadcastManager {
  val localBroadcastManager = mock<LocalBroadcastManager>()
  val registeredReceiver = ArrayList<BroadcastReceiver>()
  doAnswer { invocation ->
        val receiver = invocation.getArgument<BroadcastReceiver>(0)
        registeredReceiver.add(receiver)
        null
      }
      .`when`(localBroadcastManager)
      .registerReceiver(anyObject(), anyObject())
  doAnswer { invocation ->
        val receiver = invocation.getArgument<BroadcastReceiver>(0)
        registeredReceiver.remove(receiver)
        null
      }
      .`when`(localBroadcastManager)
      .unregisterReceiver(anyObject())
  doAnswer { invocation ->
        val intent = invocation.getArgument<Intent>(0)
        for (receiver in registeredReceiver) {
          receiver.onReceive(applicationContext, intent)
        }
        null
      }
      .`when`(localBroadcastManager)
      .sendBroadcast(anyObject())
  return localBroadcastManager
}
