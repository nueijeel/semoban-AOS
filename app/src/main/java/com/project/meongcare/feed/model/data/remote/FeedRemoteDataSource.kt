package com.project.meongcare.feed.model.data.remote

import android.util.Log
import com.project.meongcare.excreta.utils.SUCCESS
import com.project.meongcare.feed.model.entities.FeedGetResponse
import com.project.meongcare.feed.model.entities.FeedRecords
import org.json.JSONObject
import javax.inject.Inject

class FeedRemoteDataSource
    @Inject
    constructor() {
        private val accessToken =
            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpZCI6NiwiZXhwIjoxNzAzMzY1MjQ2fQ.qbSYeabyBpAni3yISWDUGYgFkQdKYfdFqPlMlz7DKCs"
        private val feedApiService = FeedClient.feedService

        suspend fun postFeed(): Int? {
            try {
                val postFeedResponse =
                    feedApiService.postFeed(
                        accessToken,
                    )
                return if (postFeedResponse.code() == SUCCESS) {
                    Log.d("FeedPostSuccess", postFeedResponse.code().toString())
                    postFeedResponse.body()
                } else {
                    val stringToJson = JSONObject(postFeedResponse.code().toString())
                    Log.d("FeedPostFailure", postFeedResponse.code().toString())
                    Log.d("FeedPostFailure", "$stringToJson")
                    null
                }
            } catch (e: Exception) {
                Log.e("FeedPostException", e.toString())
                return null
            }
        }

        suspend fun getFeed(): FeedGetResponse? {
            try {
                val getFeedResponse =
                    feedApiService.getFeed(
                        accessToken,
                        2L,
                    )
                return if (getFeedResponse.code() == SUCCESS) {
                    Log.d("FeedGetSuccess", getFeedResponse.code().toString())
                    getFeedResponse.body()
                } else {
                    val stringToJson = JSONObject(getFeedResponse.errorBody()?.string()!!)
                    Log.d("FeedGetFailure", getFeedResponse.code().toString())
                    Log.d("FeedGetFailure", "$stringToJson")
                    null
                }
            } catch (e: Exception) {
                Log.e("FeedGetException", e.toString())
                return null
            }
        }

        suspend fun getFeedPart(feedRecordId: Long): FeedRecords? {
            try {
                val getFeedPartResponse =
                    feedApiService.getFeedPart(
                        accessToken,
                        2L,
                        feedRecordId,
                    )
                return if (getFeedPartResponse.code() == SUCCESS) {
                    Log.d("FeedPartGetSuccess", getFeedPartResponse.code().toString())
                    getFeedPartResponse.body()
                } else {
                    val stringToJson = JSONObject(getFeedPartResponse.errorBody()?.string()!!)
                    Log.d("FeedPartGetFailure", getFeedPartResponse.code().toString())
                    Log.d("FeedPartGetFailure", "$stringToJson")
                    null
                }
            } catch (e: Exception) {
                Log.e("FeedPartGetException", e.toString())
                return null
            }
        }

        suspend fun getPreviousFeed(feedRecordId: Long): FeedRecords? {
            try {
                val getPreviousFeedResponse =
                    feedApiService.getPreviousFeed(
                        accessToken,
                        2L,
                        feedRecordId,
                    )
                return if (getPreviousFeedResponse.code() == SUCCESS) {
                    Log.d("PreviousFeedGetSuccess", getPreviousFeedResponse.code().toString())
                    getPreviousFeedResponse.body()
                } else {
                    val stringToJson = JSONObject(getPreviousFeedResponse.errorBody()?.string()!!)
                    Log.d("PreviousFeedGetFailure", getPreviousFeedResponse.code().toString())
                    Log.d("PreviousFeedGetFailure", "$stringToJson")
                    null
                }
            } catch (e: Exception) {
                Log.e("PreviousFeedGetException", e.toString())
                return null
            }
        }
    }
