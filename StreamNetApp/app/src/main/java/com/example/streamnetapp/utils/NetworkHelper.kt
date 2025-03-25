package com.example.streamnetapp.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

object NetworkHelper {
    private const val TAG = "NetworkHelper"

    private var cachedLocalIp: String? = null

    private const val RTMP_LOCAL_PORT = "1935"
    private const val RTMP_APP_NAME = "live"
    private const val PUBLIC_RTMP_SERVER = "rtmp://your-rtmp-server.com/live"

    fun getLocalIpAddress(context: Context): String {
        cachedLocalIp?.let { return it }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress

            if (ipAddress != 0) {
                val formattedIp = String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
                Log.d(TAG, "Local IP (WiFi): $formattedIp")
                cachedLocalIp = formattedIp
                return formattedIp
            }

            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null) {
                            Log.d(TAG, "Local IP (interface ${networkInterface.name}): $hostAddress")
                            cachedLocalIp = hostAddress
                            return hostAddress
                        }
                    }
                }
            }

            Log.w(TAG, "No valid local IP found, using localhost")
            cachedLocalIp = "127.0.0.1"
            return "127.0.0.1"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            cachedLocalIp = "127.0.0.1"
            return "127.0.0.1"
        }
    }

    fun getRtmpUrl(context: Context, streamKey: String, usePublicServer: Boolean): String {
        return if (usePublicServer) {
            "$PUBLIC_RTMP_SERVER/$streamKey"
        } else {
            val ip = getLocalIpAddress(context)
            "rtmp://$ip:$RTMP_LOCAL_PORT/$RTMP_APP_NAME/$streamKey"
        }
    }

    fun generateUniqueStreamKey(context: Context): String {
        val deviceId = Settings.getDeviceId(context)
        val randomPart = UUID.randomUUID().toString().substring(0, 8)
        return "stream_${deviceId}_$randomPart"
    }
}