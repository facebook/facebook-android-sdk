package com.facebook.util.common

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
