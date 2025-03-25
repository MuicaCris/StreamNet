package com.example.streamnetapp.utils

import android.content.Context
import android.preference.PreferenceManager
import com.example.streamnetapp.api.ApiClient
import java.util.*

/**
 * Settings - Gestionează preferințele aplicației
 */
object Settings {
    private const val PREFS_NAME = "stream_net_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_USE_PUBLIC_SERVER = "use_public_server"
    private const val KEY_USERNAME = "username"

    /**
     * Obține ID-ul unic al dispozitivului
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        return deviceId
    }

    /**
     * Verifică dacă se folosește serverul public
     */
    fun usePublicServer(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_PUBLIC_SERVER, true) // Implicit folosim serverul public
    }

    /**
     * Setează utilizarea serverului public sau local
     */
    fun setUsePublicServer(context: Context, usePublic: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_PUBLIC_SERVER, usePublic).apply()
    }

    /**
     * Obține numele de utilizator
     */
    fun getStreamType(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("stream_type", ApiClient.STREAM_TYPE_HLS) ?: ApiClient.STREAM_TYPE_HLS
    }

    /**
     * Salvează tipul de stream care va fi folosit pentru vizionare
     */
    fun setStreamType(context: Context, streamType: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString("stream_type", streamType).apply()
    }
}