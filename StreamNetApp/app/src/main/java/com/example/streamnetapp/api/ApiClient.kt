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

    private const val API_URL = "http://192.168.1.5:5080/api/Streams"

    const val RTMP_SERVER = "192.168.1.5"
    const val RTMP_PORT = "1935"
    private const val RTMP_APP = "live"

    suspend fun fetchStreams(): List<LiveStream> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(API_URL)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Eroare API: ${response.code}")
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
                            title = "Stream-ul meu",
                            streamerId = 1,
                            thumbnail = null,
                            timestamp = java.util.Date().toString(),
                            streamKey = localStreamKey!!
                        )
                    )

                    Log.d(TAG, "Stream local adăugat în listă: $localStreamKey")
                    return@withContext streamWithLocal
                }

                streamList
            } catch (e: Exception) {
                Log.e(TAG, "Excepție la obținerea stream-urilor", e)

                if (isStreaming && localStreamKey != null) {
                    Log.d(TAG, "Returnăm doar stream-ul local în caz de eroare API")
                    return@withContext listOf(
                        LiveStream(
                            id = LOCAL_STREAM_ID,
                            title = "Stream-ul meu",
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

    suspend fun registerStream(streamKey: String, title: String, streamerId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("streamKey", streamKey)
                    put("title", title)
                    put("streamerId", streamerId)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$API_URL/start")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                if (success) {
                    Log.d(TAG, "Stream inregistrat cu succes: $streamKey")
                } else {
                    Log.e(TAG, "Eroare la inregistrarea stream-ului: ${response.code}")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Exceptie la inregistrarea stream-ului", e)
                false
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
                    Log.d(TAG, "Stream dezactivat cu succes: $streamKey")
                } else {
                    Log.e(TAG, "Eroare la dezactivarea stream-ului: ${response.code}")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Exceptie la dezactivarea stream-ului", e)
                false
            }
        }
    }

    fun setLocalStreamKey(streamKey: String?) {
        localStreamKey = streamKey
        isStreaming = !streamKey.isNullOrEmpty()

        Log.d(TAG, "Stream local: cheie=$streamKey, activ=$isStreaming")
    }

    fun hasLocalStream(): Boolean {
        return isStreaming && !localStreamKey.isNullOrEmpty()
    }

    fun getLocalStreamKey(): String? {
        return localStreamKey
    }

    // This should be in your ApiClient.kt file
// Add or update the getRtmpUrl function

    // Update your getRtmpUrl function in ApiClient.kt

    // Update your getRtmpUrl function in ApiClient.kt to handle local connections better

    fun getRtmpUrl(streamKey: String, usePublicServer: Boolean = true): String {
        // Ensure streamKey is not null or empty
        val safeStreamKey = streamKey.takeIf { it.isNotEmpty() } ?: "default"

        // Determine the base URL based on server mode
        val baseRtmpUrl = if (usePublicServer) {
            // For public server
            "rtmp://$RTMP_SERVER:$RTMP_PORT/$RTMP_APP/"
        } else {
            // For local connection:
            // On an emulator, use 10.0.2.2 which points to the host's localhost
            // On a real device, use your computer's IP address on the local network

            // This format works for emulators
            "rtmp://10.0.2.2:$RTMP_PORT/$RTMP_APP/"

            // If you're using a physical device, uncomment and use your computer's actual IP:
            // "rtmp://192.168.X.X:$RTMP_PORT/$RTMP_APP/"
        }

        // Build the full URL
        val fullUrl = "$baseRtmpUrl$safeStreamKey"

        // Log for debugging
        Log.d(TAG, "RTMP URL: $fullUrl (usePublicServer: $usePublicServer, key: $safeStreamKey)")

        return fullUrl
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
            Log.e(TAG, "Eroare la parsarea JSON", e)
            emptyList()
        }
    }
}