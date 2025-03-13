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
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@AutoHandleExceptions
object GpsTopicsManager {
    private const val RECORD_OBSERVATION = true
    private val TAG = GpsTopicsManager::class.java.toString()
    private val executor: Executor by lazy { Executors.newCachedThreadPool() }

    @JvmStatic
    @TargetApi(34)
    fun getTopics(): List<String> {
        if (!shouldObserveTopics()) return emptyList()

        val context = FacebookSdk.getApplicationContext()
        var topicsResult: ArrayList<String> = ArrayList()
        try {
            val callback: OutcomeReceiver<GetTopicsResponse, Exception> =
                object : OutcomeReceiver<GetTopicsResponse, Exception> {
                    override fun onResult(response: GetTopicsResponse) {
                        try {
                            topicsResult.addAll(processObservedTopics(response))
                        } catch (e: Exception) {
                            // TODO - customized error handling
                            Log.w(TAG, "GPS_TOPICS_PROCESSING_FAILED")
                        }
                    }

                    override fun onError(error: java.lang.Exception) {
                        // TODO - customized error handling
                        Log.w(TAG, "GPS_TOPICS_OBSERVATION_ERROR")
                    }
                }

            val topicsRequestBuilder: GetTopicsRequest.Builder = GetTopicsRequest.Builder()
            topicsRequestBuilder.setShouldRecordObservation(RECORD_OBSERVATION)
            topicsRequestBuilder.setAdsSdkName(context.packageName)
            context.getSystemService(TopicsManager::class.java)?.getTopics(
                    topicsRequestBuilder.build(),
                    executor,
                    callback)
        } catch (e: Exception) {
            // TODO - customized error handling
            Log.w(TAG, "GPS_TOPICS_OBSERVATION_FAILED")
            return emptyList()
        }

        return topicsResult
    }

    private fun shouldObserveTopics(): Boolean {
        // TODO - default in false for fetcher skeleton
        return false
    }

    @TargetApi(34)
    private fun processObservedTopics(response: GetTopicsResponse): List<String> {
        // TODO - topics parser to process
        if (response.topics.isEmpty()) return emptyList()
        return response.topics.map { it.toString() }
    }
}
