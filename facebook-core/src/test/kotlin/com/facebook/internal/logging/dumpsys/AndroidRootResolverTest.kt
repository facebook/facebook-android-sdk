package com.facebook.internal.logging.dumpsys

import android.view.View
import com.facebook.FacebookTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock

class AndroidRootResolverTest : FacebookTestCase() {
  @Test
  fun `test list active roots`() {
    val rootResolver = AndroidRootResolver()
    val roots = rootResolver.listActiveRoots()
    assertNotNull(roots)
  }

  @Test
  fun `test listenable array list`() {
    val list = AndroidRootResolver.ListenableArrayList()
    val listener = FakeListener()
    list.setListener(listener)
    val mockView = mock(View::class.java)

    list.add(mockView)
    assertEquals(mutableListOf("add", "changed"), listener.operations)

    list.add(mockView)
    assertEquals(mutableListOf("add", "changed", "add", "changed", "changed"), listener.operations)

    list.remove(mockView)
    assertEquals(
        mutableListOf("add", "changed", "add", "changed", "changed", "remove", "changed"),
        listener.operations)

    list.removeAt(0)
    assertEquals(
        mutableListOf("add", "changed", "add", "changed", "changed", "remove", "changed", "remove"),
        listener.operations)
  }
}

class FakeListener : AndroidRootResolver.Listener {
  var operations = mutableListOf<String>()
  override fun onRootAdded(root: View?) {
    operations.add("add")
  }

  override fun onRootRemoved(root: View?) {
    operations.add("remove")
  }

  override fun onRootsChanged(roots: List<View?>?) {
    roots?.forEach { _ -> operations.add("changed") }
  }
}
