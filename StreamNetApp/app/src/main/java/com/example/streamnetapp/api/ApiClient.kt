package com.example.streamnetapp.api

import android.util.Log
import com.example.streamnetapp.model.LiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var localStreamKey: String? = null
    private var isStreaming = false

    const val LOCAL_STREAM_ID = 9999

    private const val API_URL = "http://192.168.1.5:5202/api/Streams"

    const val RTMP_SERVER = "192.168.1.5"
    const val RTMP_PORT = "1935"
    private const val RTMP_APP = "live"

    const val STREAM_TYPE_RTMP = "rtmp"
    const val STREAM_TYPE_HLS = "hls"

    private const val RTMP_HTTP_PORT = "8080"

    suspend fun fetchStreams(): List<LiveStream> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(API_URL)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API Error: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val streamList = parseStreams(responseBody)

                val localStreamExists = streamList.any {
                    it.streamKey == localStreamKey || it.id == LOCAL_STREAM_ID
                }

                if (isStreaming && localStreamKey != null && !localStreamExists) {
                    val streamWithLocal = streamList.toMutableList()
                    streamWithLocal.add(
                        LiveStream(
                            id = LOCAL_STREAM_ID,
                            title = "My stream",
                            streamerId = 1,
                            thumbnail = null,
                            timestamp = java.util.Date().toString(),
                            streamKey = localStreamKey!!
                        )
                    )

                    Log.d(TAG, "Local stream added to list: $localStreamKey")
                    return@withContext streamWithLocal
                }

                streamList
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching streams", e)

                if (isStreaming && localStreamKey != null) {
                    Log.d(TAG, "Returning only local stream on API error")
                    return@withContext listOf(
                        LiveStream(
                            id = LOCAL_STREAM_ID,
                            title = "My stream",
                            streamerId = 1,
                            thumbnail = null,
                            timestamp = java.util.Date().toString(),
                            streamKey = localStreamKey!!
                        )
                    )
                }

                emptyList()
            }
        }
    }

    suspend fun deactivateStream(streamKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("streamKey", streamKey)
                    put("active", false)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$API_URL/stop")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                if (success) {
                    Log.d(TAG, "Stream deactivated successfully: $streamKey")
                } else {
                    Log.e(TAG, "Error deactivating stream: ${response.code}")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception while deactivating stream", e)
                false
            }
        }
    }

    fun setLocalStreamKey(streamKey: String?) {
        localStreamKey = streamKey
        isStreaming = !streamKey.isNullOrEmpty()

        Log.d(TAG, "Local stream: key=$streamKey, active=$isStreaming")
    }

    fun hasLocalStream(): Boolean {
        return isStreaming && !localStreamKey.isNullOrEmpty()
    }

    fun getLocalStreamKey(): String? {
        return localStreamKey
    }

    fun getHlsUrl(streamKey: String): String {
        val safeStreamKey = streamKey.takeIf { it.isNotEmpty() } ?: "default"
        val hlsUrl = "http://$RTMP_SERVER:8080/hls/$safeStreamKey.m3u8"
        Log.d(TAG, "HLS URL: $hlsUrl")
        return hlsUrl
    }

    fun getRtmpUrl(streamKey: String, usePublicServer: Boolean = true): String {
        val safeStreamKey = streamKey.takeIf { it.isNotEmpty() } ?: "default"
        val rtmpUrl = "rtmp://$RTMP_SERVER:$RTMP_PORT/$RTMP_APP/$safeStreamKey"
        Log.d(TAG, "RTMP URL: $rtmpUrl")
        return rtmpUrl
    }

    suspend fun registerStream(streamKey: String, title: String, streamerId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                {
                    "streamKey": "$streamKey",
                    "title": "$title",
                    "streamerId": $streamerId
                }
            """.trimIndent()

                Log.d(TAG, "JSON payload: $jsonBody")

                val mediaType = "application/json".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val url = "http://192.168.1.5:5202/api/Streams/start"

                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Server response: ${response.code} - $responseBody")

                val success = response.isSuccessful

                if (success) {
                    Log.d(TAG, "Stream registered successfully: $streamKey")
                } else {
                    Log.e(TAG, "Error registering stream: ${response.code} - $responseBody")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception while registering stream", e)
                false
            }
        }
    }

    suspend fun stopStream(streamKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("streamKey", streamKey)
                    put("active", false)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$API_URL/stop")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val apiSuccess = response.isSuccessful

                if (apiSuccess) {
                    Log.d(TAG, "Server notified about stopped stream: $streamKey")
                } else {
                    Log.e(TAG, "Error notifying server: ${response.code}")
                }

                try {
                    val disconnectRequest = Request.Builder()
                        .url("http://$RTMP_SERVER:$RTMP_HTTP_PORT/control/drop/publisher?app=$RTMP_APP&name=$streamKey")
                        .get()
                        .build()

                    val rtmpResponse = client.newCall(disconnectRequest).execute()
                    if (rtmpResponse.isSuccessful) {
                        Log.d(TAG, "Drop command sent to RTMP server: $streamKey")
                    } else {
                        Log.e(TAG, "Error sending drop command to RTMP: ${rtmpResponse.code}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending drop command to RTMP", e)
                }

                isStreaming = false
                localStreamKey = null

                apiSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Exception while stopping stream", e)
                false
            }
        }
    }

    private fun parseStreams(jsonString: String): List<LiveStream> {
        return try {
            val jsonArray = JSONArray(jsonString)
            List(jsonArray.length()) { i ->
                val json = jsonArray.getJSONObject(i)
                LiveStream(
                    id = json.optInt("id", i + 1),
                    title = json.optString("title", "Stream ${i + 1}"),
                    streamerId = json.optInt("streamerId", 1),
                    thumbnail = json.optString("thumbnail").takeIf { it.isNotEmpty() && it != "null" },
                    timestamp = json.optString("timestamp", ""),
                    streamKey = json.optString("streamKey", "")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error", e)
            emptyList()
        }
    }
}