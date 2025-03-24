package com.example.streamnetapp.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

/**
 * NetworkHelper - Utilitar pentru configurarea rețelei și streaming
 */
object NetworkHelper {
    private const val TAG = "NetworkHelper"

    private var cachedLocalIp: String? = null

    // Configurare pentru serverul RTMP
    private const val RTMP_LOCAL_PORT = "1935"
    private const val RTMP_APP_NAME = "live"

    // Adresa serverului RTMP public
    // Notă: Înlocuiește cu adresa reală a serverului tău RTMP public în producție
    private const val PUBLIC_RTMP_SERVER = "rtmp://your-rtmp-server.com/live"

    /**
     * Obține adresa IP locală a dispozitivului
     */
    fun getLocalIpAddress(context: Context): String {
        // Returnăm adresa din cache dacă există
        cachedLocalIp?.let { return it }

        try {
            // Prima metodă: Obținem adresa IP din WifiManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress

            if (ipAddress != 0) {
                // Convertim din format integer la string
                val formattedIp = String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
                Log.d(TAG, "IP local (WiFi): $formattedIp")
                cachedLocalIp = formattedIp
                return formattedIp
            }

            // A doua metodă: Enumerăm toate interfețele de rețea
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Ignorăm adresele loopback și IPv6
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null) {
                            Log.d(TAG, "IP local (interfață ${networkInterface.name}): $hostAddress")
                            cachedLocalIp = hostAddress
                            return hostAddress
                        }
                    }
                }
            }

            // Fallback la localhost dacă nu găsim o adresă validă
            Log.w(TAG, "Nu s-a găsit o adresă IP locală validă, folosim localhost")
            cachedLocalIp = "127.0.0.1"
            return "127.0.0.1"
        } catch (e: Exception) {
            Log.e(TAG, "Eroare la obținerea adresei IP", e)
            cachedLocalIp = "127.0.0.1"
            return "127.0.0.1"
        }
    }

    /**
     * Returnează URL-ul RTMP pentru un stream-key dat
     */
    fun getRtmpUrl(context: Context, streamKey: String, usePublicServer: Boolean): String {
        return if (usePublicServer) {
            "$PUBLIC_RTMP_SERVER/$streamKey"
        } else {
            // Pentru server local, folosim IP-ul local
            val ip = getLocalIpAddress(context)
            "rtmp://$ip:$RTMP_LOCAL_PORT/$RTMP_APP_NAME/$streamKey"
        }
    }

    /**
     * Generează un stream key unic care include identificatorul dispozitivului
     */
    fun generateUniqueStreamKey(context: Context): String {
        val deviceId = Settings.getDeviceId(context)
        val randomPart = UUID.randomUUID().toString().substring(0, 8)
        return "stream_${deviceId}_$randomPart"
    }
}