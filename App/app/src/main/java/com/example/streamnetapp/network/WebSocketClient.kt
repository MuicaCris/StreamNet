package com.example.streamnetapp.network

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import android.util.Log
import java.net.URI

class WebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WebSocket", "Connected to server")
        send("Test de conexiune de la client")
    }

    override fun onMessage(message: String?) {
        message?.let {
            Log.d("WebSocket", "Received message: $it")
            onMessageReceived?.invoke(it) // Callback pentru UI
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WebSocket", "Disconnected: $reason")
    }

    override fun onError(ex: Exception?) {
        Log.e("WebSocket", "Error: ${ex?.message}")
    }

    var onMessageReceived: ((String) -> Unit)? = null
}
