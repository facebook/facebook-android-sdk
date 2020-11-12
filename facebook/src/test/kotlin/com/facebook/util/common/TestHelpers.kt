package com.facebook.util.common

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
