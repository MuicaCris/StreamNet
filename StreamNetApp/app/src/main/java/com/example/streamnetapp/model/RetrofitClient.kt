package com.example.streamnetapp.model

import com.example.streamnetapp.api.StreamApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://localhost:5050/ws/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val streamApiService: StreamApiService by lazy {
        retrofit.create(StreamApiService::class.java)
    }
}