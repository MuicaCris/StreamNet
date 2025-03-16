package com.example.streamnetapp.api

import retrofit2.http.*
import java.time.Instant

interface StreamApiService {
    @POST("api/stream/createStream")
    suspend fun createStream(@Body request: LiveStreamCreateRequest): CreateStreamResponse

    @GET("api/stream/rtmpUrl/{streamId}")
    suspend fun getRtmpUrl(@Path("streamId") streamId: Int): RtmpUrlResponse
}

data class LiveStreamCreateRequest(
    val title: String,
    val streamerId: Int,
    val thumbnail: String? = null,
    val timestamp: String = Instant.now().toString()
)

data class CreateStreamResponse(
    val streamId: Int,
    val rtmpUrl: String
)

data class RtmpUrlResponse(
    val rtmpUrl: String
)