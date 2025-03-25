package com.example.streamnetapp.utils

import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    suspend fun testRtmpServers(
        publicHost: String,
        localHost: String,
        port: Int,
        isHls: Boolean = false
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Boolean>()

        val protocol = if (isHls) "http" else "rtmp"

        result["public"] = try {
            if (isHls) {
                val url = URL("$protocol://$publicHost:$port/stat")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } else {
                val socket = Socket()
                socket.connect(InetSocketAddress(publicHost, port), 3000)
                socket.close()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Public server test error: $e")
            false
        }

        result["local"] = try {
            if (isHls) {
                val url = URL("$protocol://$localHost:$port/stat")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode == 200
            } else {
                val socket = Socket()
                socket.connect(InetSocketAddress(localHost, port), 3000)
                socket.close()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local server test error: $e")
            false
        }

        return@withContext result
    }
}