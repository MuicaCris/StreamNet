package com.example.streamnetapp.utils

import java.util.*

object StreamManager {
    private var localStreamKey: String? = null
    private var isStreamingNow: Boolean = false

    const val LOCAL_STREAM_ID = 9999

    fun isStreaming(): Boolean {
        return isStreamingNow && !localStreamKey.isNullOrEmpty()
    }

    fun getLocalStreamKey(): String {
        if (localStreamKey == null) {
            localStreamKey = "stream_${UUID.randomUUID().toString().substring(0, 8)}"
        }
        return localStreamKey!!
    }

    fun setStreamingState(isStreaming: Boolean) {
        isStreamingNow = isStreaming
        if (!isStreaming) {
            localStreamKey = null
        }
    }
}