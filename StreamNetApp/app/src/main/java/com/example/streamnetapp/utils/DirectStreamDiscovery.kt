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

/**
 * DirectStreamDiscovery - Utilitar pentru descoperirea directă a streamurilor RTMP
 * Funcționează independent de API-ul server-ului C#
 */
object DirectStreamDiscovery {
    private const val TAG = "DirectStreamDiscovery"

    // Configurare RTMP
    private const val RTMP_SERVER = "10.0.2.2" // IP-ul serverului RTMP
    private const val RTMP_PORT = "1935"
    private const val RTMP_APP = "live"
    private const val RTMP_BASE_URL = "rtmp://$RTMP_SERVER:$RTMP_PORT/$RTMP_APP"

    // Client HTTP pentru verificări
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS) // Timeout redus pentru verificări rapide
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    // Lista streamurilor descoperite
    private val discoveredStreams = mutableListOf<LiveStream>()

    // Executor pentru verificări periodice
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    // Stare
    private var isDiscoveryRunning = false
    private var localStreamKey: String? = null
    private var discoveryCallback: ((List<LiveStream>) -> Unit)? = null

    /**
     * Pornește descoperirea streamurilor
     */
    fun startDiscovery(callback: (List<LiveStream>) -> Unit) {
        if (isDiscoveryRunning) {
            Log.d(TAG, "Descoperirea este deja pornită")
            return
        }

        discoveryCallback = callback
        isDiscoveryRunning = true

        // Pornește verificarea periodică
        runnable = object : Runnable {
            override fun run() {
                discoverStreams()
                handler.postDelayed(this, 5000) // Verifică la fiecare 5 secunde
            }
        }

        handler.post(runnable!!)
        Log.d(TAG, "Descoperirea streamurilor a fost pornită")
    }

    /**
     * Oprește descoperirea streamurilor
     */
    fun stopDiscovery() {
        isDiscoveryRunning = false
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
        Log.d(TAG, "Descoperirea streamurilor a fost oprită")
    }

    /**
     * Înregistrează un stream local
     */
    fun registerLocalStream(streamKey: String, title: String) {
        localStreamKey = streamKey

        // Adaugă direct streamul local în lista de streamuri descoperite
        val localStream = LiveStream(
            id = 9999,
            title = title,
            streamerId = 1,
            thumbnail = null,
            timestamp = Date().toString(),
            streamKey = streamKey
        )

        // Verifică dacă streamul există deja în listă
        val existingIndex = discoveredStreams.indexOfFirst { it.streamKey == streamKey }
        if (existingIndex >= 0) {
            discoveredStreams[existingIndex] = localStream
        } else {
            discoveredStreams.add(localStream)
        }

        // Notifică callback-ul
        discoveryCallback?.invoke(discoveredStreams.toList())

        Log.d(TAG, "Stream local înregistrat: $streamKey, $title")
    }

    /**
     * Șterge un stream local
     */
    fun unregisterLocalStream() {
        localStreamKey?.let { key ->
            // Elimină streamul din listă
            discoveredStreams.removeAll { it.streamKey == key }

            // Notifică callback-ul
            discoveryCallback?.invoke(discoveredStreams.toList())

            Log.d(TAG, "Stream local șters: $key")
        }

        localStreamKey = null
    }

    /**
     * Descoperă streamuri disponibile prin verificare directă
     * Această metodă combină mai multe strategii pentru a maximiza șansele de a găsi streamuri
     */
    fun discoverStreams() {
        Thread {
            try {
                Log.d(TAG, "Începe verificarea streamurilor...")

                // Strategia 1: Încearcă să obțină lista din API-ul C#
                try {
                    val streams = fetchStreamsBlocking()
                    if (streams.isNotEmpty()) {
                        updateDiscoveredStreams(streams)
                        Log.d(TAG, "Streamuri obținute din API C#: ${streams.size}")
                        return@Thread
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Eroare la obținerea streamurilor din API C#", e)
                }

                // Strategia 2: Verifică direct streamurile predefinite
                val predefinedStreamKeys = listOf(
                    "stream1", "stream2", "stream3", "streamkey", "test", "live"
                )

                val newStreams = mutableListOf<LiveStream>()

                // Verifică streamurile predefinite
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
                        Log.d(TAG, "Stream predefinit disponibil: $key")
                    }
                }

                // Strategia 3: Verifică streamul local actual (dacă există)
                localStreamKey?.let { key ->
                    // Adaugă streamul local în lista de streamuri descoperite
                    if (!newStreams.any { it.streamKey == key }) {
                        newStreams.add(
                            LiveStream(
                                id = 9999,
                                title = "Stream-ul meu local",
                                streamerId = 1,
                                thumbnail = null,
                                timestamp = Date().toString(),
                                streamKey = key
                            )
                        )
                        Log.d(TAG, "Stream local adăugat: $key")
                    }
                }

                // Actualizează lista de streamuri și notifică
                if (newStreams.isNotEmpty()) {
                    updateDiscoveredStreams(newStreams)
                    Log.d(TAG, "Streamuri descoperite direct: ${newStreams.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eroare în timpul descoperirii streamurilor", e)
            }
        }.start()
    }

    /**
     * Verifică dacă un stream este disponibil
     */
    private fun isStreamAvailable(streamKey: String): Boolean {
        try {
            // Construiește URL-ul pentru verificare
            val streamUrl = "$RTMP_BASE_URL/$streamKey"

            // Folosește o tehnică de verificare mai rapidă - încearcă să obțină headers
            val request = Request.Builder()
                .url(streamUrl)
                .head() // Doar headers, nu conținut
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: IOException) {
            Log.d(TAG, "Stream $streamKey nu este disponibil: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Eroare la verificarea stream-ului $streamKey", e)
            return false
        }
    }

    /**
     * Actualizează lista de streamuri descoperite
     */
    @Synchronized
    private fun updateDiscoveredStreams(newStreams: List<LiveStream>) {
        // Filtrăm streamurile existente
        discoveredStreams.removeAll { existing ->
            newStreams.none { it.streamKey == existing.streamKey }
        }

        // Adăugăm streamurile noi
        for (stream in newStreams) {
            val index = discoveredStreams.indexOfFirst { it.streamKey == stream.streamKey }
            if (index >= 0) {
                // Actualizăm streamul existent
                discoveredStreams[index] = stream
            } else {
                // Adăugăm streamul nou
                discoveredStreams.add(stream)
            }
        }

        // Notificăm callback-ul pe thread-ul principal
        handler.post {
            discoveryCallback?.invoke(discoveredStreams.toList())
        }
    }

    /**
     * Versiune blocantă pentru obținerea streamurilor din API
     */
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
            Log.e(TAG, "Eroare la obținerea streamurilor", e)
            return emptyList()
        }
    }

    /**
     * Parse streams from JSON
     */
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
            Log.e(TAG, "Eroare la parsarea JSON", e)
            emptyList()
        }
    }
}