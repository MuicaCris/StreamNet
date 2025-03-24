package com.example.streamnetapp.model

data class LiveStream(
    val id: Int,
    val title: String,
    val streamerId: Int,
    val thumbnail: String? = null,
    val timestamp: String,
    val streamKey: String
)
