package com.facebook.internal.logging.dumpsys

import android.content.res.Resources
import com.facebook.FacebookTestCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.`when`

class ResourcesUtilTest : FacebookTestCase() {

  private lateinit var mockResource: Resources

  private val resourceId1 = 0x7fffffff
  private val resourceId2 = 0xf
  private val resourceId3 = 0xa

  @Before
  fun before() {
    mockResource = mock(Resources::class.java)
    `when`(mockResource.getResourceTypeName(resourceId1)).thenReturn("type_name1")
    `when`(mockResource.getResourceEntryName(resourceId1)).thenReturn("entry_name1")

    `when`(mockResource.getResourceTypeName(resourceId2)).thenReturn("type_name2")
    `when`(mockResource.getResourceEntryName(resourceId2)).thenReturn("entry_name2")
    `when`(mockResource.getResourcePackageName(resourceId2)).thenReturn("package_name2")

    `when`(mockResource.getResourcePackageName(resourceId3))
        .thenThrow(Resources.NotFoundException())
  }

  @Test
  fun `test fall back id string`() {
    assertEquals("#1", ResourcesUtil.getIdString(null, 1))
    assertEquals("#a", ResourcesUtil.getIdString(null, 10))
  }

  @Test
  fun `test resource with empty prefix`() {
    val id = ResourcesUtil.getIdString(mockResource, resourceId1)
    assertEquals("@type_name1/entry_name1", id)
  }

  @Test
  fun `test resource with prefix`() {
    val id = ResourcesUtil.getIdString(mockResource, resourceId2)
    assertEquals("@package_name2:type_name2/entry_name2", id)
  }

  @Test
  fun `test get id quietly`() {
    val id = ResourcesUtil.getIdStringQuietly(mockResource, resourceId3)
    assertEquals("#a", id)
  }
}
