package com.example.streamnetapp.api

import com.example.streamnetapp.model.LiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

suspend fun fetchStreams(): List<LiveStream> {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://10.0.2.2/api/liveStreams")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(responseBody)

            List(jsonArray.length()) { i ->
                val json = jsonArray.getJSONObject(i)
                LiveStream(
                    id = json.optInt("id", -1),
                    title = json.optString("title", ""),
                    streamerName = json.optString("streamerName", ""),
                    thumbnail = json.optString("thumbnail", null),
                    timestamp = json.optString("timestamp", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}