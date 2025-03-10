package com.example.streamnetapp.network


import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(url: String, listener: WebSocketListener) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.let {
            if (it.send("Client closing connection")) {
                it.close(1000, "Normal closing")
            }
        }
    }
}
