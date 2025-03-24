package com.example.streamnetapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * Check if the device has internet connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Test if a specific server and port are reachable
     */
    suspend fun isServerReachable(host: String, port: Int, timeoutMs: Int = 3000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing connection to $host:$port")
                val socket = Socket()
                val socketAddress: SocketAddress = InetSocketAddress(host, port)

                socket.connect(socketAddress, timeoutMs)
                socket.close()

                Log.d(TAG, "Successfully connected to $host:$port")
                true
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout to $host:$port", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $host:$port", e)
                false
            }
        }
    }

    /**
     * Test both local and public RTMP servers
     */
    suspend fun testRtmpServers(
        publicHost: String,
        localHost: String = "10.0.2.2",
        port: Int = 1935
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        try {
            results["public"] = isServerReachable(publicHost, port)
        } catch (e: Exception) {
            Log.e(TAG, "Error testing public server", e)
            results["public"] = false
        }

        try {
            results["local"] = isServerReachable(localHost, port)
        } catch (e: Exception) {
            Log.e(TAG, "Error testing local server", e)
            results["local"] = false
        }

        Log.d(TAG, "RTMP server test results: $results")
        return results
    }
}