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
import android.adservices.topics.TopicsManager
import android.annotation.TargetApi
import android.os.OutcomeReceiver
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
object GpsTopicsManager {
    private const val RECORD_OBSERVATION = true
    private val TAG = GpsTopicsManager::class.java.toString()
    private val executor: Executor by lazy { Executors.newCachedThreadPool() }
    private val isTopicsObservationEnabled = AtomicBoolean(false)

    @JvmStatic
    fun enableTopicsObservation() {
        isTopicsObservationEnabled.set(true)
    }

    @JvmStatic
    @TargetApi(34)
    fun getTopics(): CompletableFuture<List<TopicData>> {
        if (!shouldObserveTopics()) {
            return CompletableFuture.completedFuture(emptyList())
        }

        val futureResult: CompletableFuture<List<TopicData>> = CompletableFuture()
        try {
            val context = FacebookSdk.getApplicationContext()
            val callback: OutcomeReceiver<GetTopicsResponse, Exception> =
                object : OutcomeReceiver<GetTopicsResponse, Exception> {
                    override fun onResult(response: GetTopicsResponse) {
                        try {
                            futureResult.complete(processObservedTopics(response))
                        } catch (error: Throwable) {
                            // TODO - customized error handling
                            Log.w(TAG, "GPS_TOPICS_PROCESSING_FAILURE", error)
                            futureResult.completeExceptionally(error)
                        }
                    }

                    override fun onError(error: Exception) {
                        // TODO - customized error handling
                        Log.w(TAG, "GPS_TOPICS_OBSERVATION_FAILURE", error)
                        futureResult.completeExceptionally(error)
                    }
                }

            val topicsRequestBuilder: GetTopicsRequest.Builder = GetTopicsRequest.Builder()
            topicsRequestBuilder.setShouldRecordObservation(RECORD_OBSERVATION)
            topicsRequestBuilder.setAdsSdkName(context.packageName)

            context.getSystemService(TopicsManager::class.java)?.getTopics(
                topicsRequestBuilder.build(),
                executor,
                callback,
            )
        } catch (error: Throwable) {
            // TODO - customized error handling
            Log.w(TAG, "GPS_TOPICS_OBSERVATION_FAILURE", error)
            futureResult.completeExceptionally(error)
        }
        return futureResult
    }

    @JvmStatic
    fun shouldObserveTopics(): Boolean {
        if (!isTopicsObservationEnabled.get()) {
            return false
        }

        try {
            Class.forName("android.adservices.topics.TopicsManager")
        } catch (error: Throwable) {
            // TODO - customized error handling
            Log.w(TAG, "GPS_TOPICS_DEPENDENCY_FAILURE", error)
            return false
        }
        return true
    }

    @TargetApi(34)
    private fun processObservedTopics(response: GetTopicsResponse): List<TopicData> {
        return response.topics.map { topic ->
            TopicData(
                taxonomyVersion = topic.taxonomyVersion,
                modelVersion = topic.modelVersion,
                topicId = topic.topicId,
            )
        }
    }
}
