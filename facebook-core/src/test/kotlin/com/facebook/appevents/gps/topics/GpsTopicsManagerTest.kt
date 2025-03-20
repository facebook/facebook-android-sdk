/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.gps.topics

import android.adservices.topics.GetTopicsRequest
import android.adservices.topics.GetTopicsResponse
import android.adservices.topics.Topic
import android.adservices.topics.TopicsManager
import android.content.Context
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.test.fail
import com.facebook.internal.FeatureManager

@PrepareForTest(
    FacebookSdk::class,
    TopicsManager::class,
    GpsTopicsManager::class,
    GetTopicsRequest::class,
    GetTopicsResponse::class,
    Topic::class,
    FeatureManager::class,
)
@Config(sdk = [23])
class GpsTopicsManagerTest : FacebookPowerMockTestCase() {
    private lateinit var mockTopicsManager: TopicsManager

    @Before
    fun setUp() {
        val mockContext: Context = mock(Context::class.java)
        whenever(mockContext.applicationContext).thenReturn(mockContext)

        mockTopicsManager = mock(TopicsManager::class.java)
        whenever(mockContext.getSystemService(TopicsManager::class.java)).thenReturn(mockTopicsManager)

        mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.isInitialized()).thenReturn(true)
        whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
        whenever(FacebookSdk.getExecutor()).thenCallRealMethod()
        whenever(FacebookSdk.getFacebookDomain()).thenCallRealMethod()
    }

    @Test
    fun testGetTopicsEmptyResponseCase() {
        mockStatic(GetTopicsResponse::class.java)

        val getTopicsResponse: GetTopicsResponse = mock(GetTopicsResponse::class.java)
        whenever(getTopicsResponse.topics).thenReturn(listOf())
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            callback.onResult(getTopicsResponse)
        }

        mockStatic(GetTopicsRequest.Builder::class.java)
        val getTopicsRequest: GetTopicsRequest = mock(GetTopicsRequest::class.java)
        val getTopicsRequestBuilder: GetTopicsRequest.Builder = mock(GetTopicsRequest.Builder::class.java)
        whenNew(GetTopicsRequest.Builder::class.java)
            .withNoArguments()
            .thenReturn(getTopicsRequestBuilder)

        whenever(getTopicsRequestBuilder.setAdsSdkName(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.setShouldRecordObservation(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.build()).thenReturn(getTopicsRequest)

        try {
            val future = GpsTopicsManager.getTopics()
            val topics = future.get()
            assertNotNull(topics)
            assertEquals(0, topics.size)
        } catch (e: Exception) {
            fail("[Topics] execution error occurred: $e")
        }
    }

    @Test
    fun testGetTopicsWithSingleTopicAsResponseCase() {
        mockStatic(GetTopicsResponse::class.java)

        val topic: Topic = mock(Topic::class.java)
        whenever(topic.taxonomyVersion).thenReturn(2L)
        whenever(topic.modelVersion).thenReturn(5L)
        whenever(topic.topicId).thenReturn(10111)

        val getTopicsResponse: GetTopicsResponse = mock(GetTopicsResponse::class.java)
        whenever(getTopicsResponse.topics).thenReturn(listOf(topic))
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            callback.onResult(getTopicsResponse)
        }

        mockStatic(GetTopicsRequest.Builder::class.java)
        val getTopicsRequest: GetTopicsRequest = mock(GetTopicsRequest::class.java)
        val getTopicsRequestBuilder: GetTopicsRequest.Builder = mock(GetTopicsRequest.Builder::class.java)
        whenNew(GetTopicsRequest.Builder::class.java)
            .withNoArguments()
            .thenReturn(getTopicsRequestBuilder)

        whenever(getTopicsRequestBuilder.setAdsSdkName(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.setShouldRecordObservation(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.build()).thenReturn(getTopicsRequest)

        try {
            spy(GpsTopicsManager::class.java)
            whenever(GpsTopicsManager.shouldObserveTopics()).thenReturn(true)

            val future = GpsTopicsManager.getTopics()
            val topics = future.get()
            assertNotNull(topics)
            assertEquals(1, topics.size)

            val topicData = topics[0]
            assertEquals(2L, topicData.taxonomyVersion)
            assertEquals(5L, topicData.modelVersion)
            assertEquals(10111, topicData.topicId)
        } catch (e: Exception) {
            fail("[Topics] execution error occurred: $e")
        }
    }

    @Test
    fun testGetTopicsForFastReturnCase() {
        mockStatic(GetTopicsResponse::class.java)

        val topic: Topic = mock(Topic::class.java)

        val getTopicsResponse: GetTopicsResponse = mock(GetTopicsResponse::class.java)
        whenever(getTopicsResponse.topics).thenReturn(listOf(topic))
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            callback.onResult(getTopicsResponse)
        }

        mockStatic(GetTopicsRequest.Builder::class.java)
        val getTopicsRequest: GetTopicsRequest = mock(GetTopicsRequest::class.java)
        val getTopicsRequestBuilder: GetTopicsRequest.Builder = mock(GetTopicsRequest.Builder::class.java)
        whenNew(GetTopicsRequest.Builder::class.java)
            .withNoArguments()
            .thenReturn(getTopicsRequestBuilder)

        whenever(getTopicsRequestBuilder.setAdsSdkName(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.setShouldRecordObservation(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.build()).thenReturn(getTopicsRequest)

        try {
            spy(GpsTopicsManager::class.java)
            whenever(GpsTopicsManager.shouldObserveTopics()).thenReturn(false)

            val future = GpsTopicsManager.getTopics()
            val topics = future.get()
            assertNotNull(topics)
            assertEquals(0, topics.size)
        } catch (e: Exception) {
            fail("[Topics] execution error occurred: $e")
        }
    }

    @Test
    fun testGetTopicsWithMultipleTopicsAsResponseCase() {
        mockStatic(GetTopicsResponse::class.java)

        val topicTestDataList =
            listOf(
                TopicData(2L, 5L, 10111),
                TopicData(3L, 5L, 10121),
                TopicData(2L, 5L, 10131),
            )

        val topics = mutableListOf<Topic>()
        for (i in topicTestDataList.indices) {
            val topic: Topic = mock(Topic::class.java)
            whenever(topic.taxonomyVersion).thenReturn(topicTestDataList[i].taxonomyVersion)
            whenever(topic.modelVersion).thenReturn(topicTestDataList[i].modelVersion)
            whenever(topic.topicId).thenReturn(topicTestDataList[i].topicId)
            topics.add(topic)
        }

        val getTopicsResponse: GetTopicsResponse = mock(GetTopicsResponse::class.java)
        whenever(getTopicsResponse.topics).thenReturn(topics)
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            callback.onResult(getTopicsResponse)
        }

        mockStatic(GetTopicsRequest.Builder::class.java)
        val getTopicsRequest: GetTopicsRequest = mock(GetTopicsRequest::class.java)
        val getTopicsRequestBuilder: GetTopicsRequest.Builder = mock(GetTopicsRequest.Builder::class.java)
        whenNew(GetTopicsRequest.Builder::class.java)
            .withNoArguments()
            .thenReturn(getTopicsRequestBuilder)

        whenever(getTopicsRequestBuilder.setAdsSdkName(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.setShouldRecordObservation(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.build()).thenReturn(getTopicsRequest)

        try {
            spy(GpsTopicsManager::class.java)
            whenever(GpsTopicsManager.shouldObserveTopics()).thenReturn(true)

            val future = GpsTopicsManager.getTopics()
            val resultTopics = future.get()
            assertNotNull(resultTopics)
            assertEquals(3, resultTopics.size)

            for (i in resultTopics.indices) {
                val topicDataFetched = resultTopics[i]
                assertEquals(topicTestDataList[i].taxonomyVersion, topicDataFetched.taxonomyVersion)
                assertEquals(topicTestDataList[i].modelVersion, topicDataFetched.modelVersion)
                assertEquals(topicTestDataList[i].topicId, topicDataFetched.topicId)
            }
        } catch (e: Exception) {
            fail("[Topics] execution error occurred: $e")
        }
    }

    @Test
    fun testGetTopicsForFeatureEnablementFastReturnCase() {
        val topic: Topic = mock(Topic::class.java)
        whenever(topic.taxonomyVersion).thenReturn(2L)
        whenever(topic.modelVersion).thenReturn(5L)
        whenever(topic.topicId).thenReturn(10111)

        mockStatic(GetTopicsResponse::class.java)
        val getTopicsResponse: GetTopicsResponse = mock(GetTopicsResponse::class.java)
        whenever(getTopicsResponse.topics).thenReturn(listOf(topic))
        whenever(mockTopicsManager.getTopics(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<OutcomeReceiver<GetTopicsResponse, Exception>>(2)
            callback.onResult(getTopicsResponse)
        }

        mockStatic(GetTopicsRequest.Builder::class.java)
        val getTopicsRequest: GetTopicsRequest = mock(GetTopicsRequest::class.java)
        val getTopicsRequestBuilder: GetTopicsRequest.Builder = mock(GetTopicsRequest.Builder::class.java)
        whenNew(GetTopicsRequest.Builder::class.java)
            .withNoArguments()
            .thenReturn(getTopicsRequestBuilder)

        whenever(getTopicsRequestBuilder.setAdsSdkName(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.setShouldRecordObservation(any())).thenReturn(getTopicsRequestBuilder)
        whenever(getTopicsRequestBuilder.build()).thenReturn(getTopicsRequest)

        try {
            mockStatic(FeatureManager::class.java)
            whenever(FeatureManager.isEnabled(FeatureManager.Feature.GPSTopicsObservation)).thenReturn(false)
            val future = GpsTopicsManager.getTopics()
            val topics = future.get()
            assertNotNull(topics)
            assertEquals(0, topics.size)
        } catch (e: Exception) {
            fail("[Topics] execution error occurred: $e")
        }
    }
}
