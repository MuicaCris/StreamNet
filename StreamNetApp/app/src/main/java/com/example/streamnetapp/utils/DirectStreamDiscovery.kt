package com.example.streamnetapp.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.streamnetapp.model.LiveStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

object DirectStreamDiscovery {
    private const val TAG = "DirectStreamDiscovery"

    private const val RTMP_SERVER = "10.0.2.2"
    private const val RTMP_PORT = "1935"
    private const val RTMP_APP = "live"
    private const val RTMP_BASE_URL = "rtmp://$RTMP_SERVER:$RTMP_PORT/$RTMP_APP"

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val discoveredStreams = mutableListOf<LiveStream>()
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var isDiscoveryRunning = false
    private var localStreamKey: String? = null
    private var discoveryCallback: ((List<LiveStream>) -> Unit)? = null

    fun startDiscovery(callback: (List<LiveStream>) -> Unit) {
        if (isDiscoveryRunning) {
            Log.d(TAG, "Discovery already running")
            return
        }

        discoveryCallback = callback
        isDiscoveryRunning = true

        runnable = object : Runnable {
            override fun run() {
                discoverStreams()
                handler.postDelayed(this, 5000)
            }
        }

        handler.post(runnable!!)
        Log.d(TAG, "Stream discovery started")
    }

    fun stopDiscovery() {
        isDiscoveryRunning = false
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
        Log.d(TAG, "Stream discovery stopped")
    }

    fun registerLocalStream(streamKey: String, title: String) {
        localStreamKey = streamKey

        val localStream = LiveStream(
            id = 9999,
            title = title,
            streamerId = 1,
            thumbnail = null,
            timestamp = Date().toString(),
            streamKey = streamKey
        )

        val existingIndex = discoveredStreams.indexOfFirst { it.streamKey == streamKey }
        if (existingIndex >= 0) {
            discoveredStreams[existingIndex] = localStream
        } else {
            discoveredStreams.add(localStream)
        }

        discoveryCallback?.invoke(discoveredStreams.toList())
        Log.d(TAG, "Local stream registered: $streamKey, $title")
    }

    fun unregisterLocalStream() {
        localStreamKey?.let { key ->
            discoveredStreams.removeAll { it.streamKey == key }
            discoveryCallback?.invoke(discoveredStreams.toList())
            Log.d(TAG, "Local stream removed: $key")
        }

        localStreamKey = null
    }

    fun discoverStreams() {
        Thread {
            try {
                Log.d(TAG, "Starting stream discovery...")

                try {
                    val streams = fetchStreamsBlocking()
                    if (streams.isNotEmpty()) {
                        updateDiscoveredStreams(streams)
                        Log.d(TAG, "Streams from C# API: ${streams.size}")
                        return@Thread
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting streams from C# API", e)
                }

                val predefinedStreamKeys = listOf(
                    "stream1", "stream2", "stream3", "streamkey", "test", "live"
                )

                val newStreams = mutableListOf<LiveStream>()

                for (key in predefinedStreamKeys) {
                    if (isStreamAvailable(key)) {
                        newStreams.add(
                            LiveStream(
                                id = key.hashCode(),
                                title = "Stream $key",
                                streamerId = 1,
                                thumbnail = null,
                                timestamp = Date().toString(),
                                streamKey = key
                            )
                        )
                        Log.d(TAG, "Predefined stream available: $key")
                    }
                }

                localStreamKey?.let { key ->
                    if (!newStreams.any { it.streamKey == key }) {
                        newStreams.add(
                            LiveStream(
                                id = 9999,
                                title = "My local stream",
                                streamerId = 1,
                                thumbnail = null,
                                timestamp = Date().toString(),
                                streamKey = key
                            )
                        )
                        Log.d(TAG, "Local stream added: $key")
                    }
                }

                if (newStreams.isNotEmpty()) {
                    updateDiscoveredStreams(newStreams)
                    Log.d(TAG, "Directly discovered streams: ${newStreams.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during stream discovery", e)
            }
        }.start()
    }

    private fun isStreamAvailable(streamKey: String): Boolean {
        try {
            val streamUrl = "$RTMP_BASE_URL/$streamKey"
            val request = Request.Builder()
                .url(streamUrl)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: IOException) {
            Log.d(TAG, "Stream $streamKey not available: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking stream $streamKey", e)
            return false
        }
    }

    @Synchronized
    private fun updateDiscoveredStreams(newStreams: List<LiveStream>) {
        discoveredStreams.removeAll { existing ->
            newStreams.none { it.streamKey == existing.streamKey }
        }

        for (stream in newStreams) {
            val index = discoveredStreams.indexOfFirst { it.streamKey == stream.streamKey }
            if (index >= 0) {
                discoveredStreams[index] = stream
            } else {
                discoveredStreams.add(stream)
            }
        }

        handler.post {
            discoveryCallback?.invoke(discoveredStreams.toList())
        }
    }

    private fun fetchStreamsBlocking(): List<LiveStream> {
        try {
            val request = Request.Builder()
                .url("http://localhost:5080/ws/api/stream/activeStreams")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return emptyList()
            }

            val body = response.body?.string() ?: return emptyList()
            return parseStreamsFromJson(body)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting streams", e)
            return emptyList()
        }
    }

    private fun parseStreamsFromJson(jsonString: String): List<LiveStream> {
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