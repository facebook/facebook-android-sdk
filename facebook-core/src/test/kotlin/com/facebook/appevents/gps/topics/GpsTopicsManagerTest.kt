package com.facebook.appevents.gps.topics

import android.adservices.topics.GetTopicsResponse
import android.adservices.topics.Topic
import android.adservices.topics.TopicsManager
import android.content.Context
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

@PrepareForTest(
    FacebookSdk::class,
    TopicsManager::class,
    GetTopicsResponse::class,
    Topic::class,
)
@Config(sdk = [23])
class GpsTopicsManagerTest : FacebookPowerMockTestCase() {
    private lateinit var mockContext: Context
    private lateinit var mockTopicsManager: TopicsManager

    @Before
    fun setUp() {
        mockContext = mock()
        mockTopicsManager = mock()
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
        PowerMockito.mockStatic(TopicsManager::class.java)
        whenever(mockContext.getSystemService(TopicsManager::class.java)).thenReturn(
            mockTopicsManager
        )
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            val getTopicsResponse = GetTopicsResponse.Builder(listOf()).build()
            callback.onResult(getTopicsResponse)
        }
    }

    @Test
    fun testGetTopicsSkeleton() {
        // TODO - skeleton workflow validation pending detailed test cases
        val topics = GpsTopicsManager.getTopics()
        assertNotNull(topics)
    }
}
