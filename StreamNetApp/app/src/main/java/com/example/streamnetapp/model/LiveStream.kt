package com.example.streamnetapp.model

data class LiveStream(
    val id: Int,
    val title: String,
    val streamerName: String,
    val thumbnail: String?,
    val timestamp: String
)
