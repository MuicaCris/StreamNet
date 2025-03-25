package com.example.streamnetapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.streamnetapp.api.ApiClient
import com.example.streamnetapp.utils.DirectStreamDiscovery
import com.example.streamnetapp.utils.NetworkHelper
import com.example.streamnetapp.utils.Settings
import kotlinx.coroutines.launch

private const val TAG = "LiveStreamUI"

@Composable
fun LiveStreamUI() {
    val context = LocalContext.current
    var rtmpCamera by remember { mutableStateOf<RtmpCamera2?>(null) }
    var openGlView by remember { mutableStateOf<OpenGlView?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var streamTitle by remember { mutableStateOf("My stream") }
    var usePublicServer by remember { mutableStateOf(Settings.usePublicServer(context)) }
    val coroutineScope = rememberCoroutineScope()

    var hasAllPermissions by remember { mutableStateOf(false) }
    var permissionsInitialized by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
    }

    val streamKey = remember {
        NetworkHelper.generateUniqueStreamKey(context)
    }

    val streamUrl = remember(streamKey, usePublicServer) {
        ApiClient.getRtmpUrl(streamKey)
    }

    val checkPermissions = {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        hasAllPermissions = allGranted
        permissionsInitialized = true

        if (allGranted) {
            errorMessage = null
            Log.d(TAG, "All permissions granted")
        } else {
            errorMessage = "Required permissions denied! Tap retry."
            Log.e(TAG, "Not all permissions granted")
        }
    }

    LaunchedEffect(Unit) {
        isStreaming = ApiClient.hasLocalStream()
        checkPermissions()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            errorMessage = null
            hasAllPermissions = true
            Log.d(TAG, "User granted permissions")
        } else {
            errorMessage = "Required permissions denied! Tap retry."
            hasAllPermissions = false
            Log.e(TAG, "User denied some permissions")
        }
    }

    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stream Setup",
                    style = MaterialTheme.typography.titleMedium
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = streamTitle,
                    onValueChange = { streamTitle = it },
                    label = { Text("Stream title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use public server:")
                    Switch(
                        checked = usePublicServer,
                        onCheckedChange = {
                            if (!isStreaming) {
                                usePublicServer = it
                                Settings.setUsePublicServer(context, it)
                            } else {
                                errorMessage = "Cannot change server while streaming!"
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stream key: $streamKey",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Status: ${if (isStreaming) "LIVE" else "Offline"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isStreaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        statusMessage?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (hasCamera && hasAllPermissions) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                factory = { ctx ->
                    OpenGlView(ctx).apply {
                        openGlView = this
                        post {
                            rtmpCamera = RtmpCamera2(
                                this,
                                object : ConnectCheckerRtmp {
                                    override fun onConnectionStartedRtmp(rtmpUrl: String) {
                                        Log.i(TAG, "Connection started: $rtmpUrl")
                                        statusMessage = "Connecting to server..."
                                    }
                                    override fun onConnectionSuccessRtmp() {
                                        Log.i(TAG, "Connection successful!")
                                        statusMessage = "Stream active!"
                                        isStreaming = true

                                        ApiClient.setLocalStreamKey(streamKey)
                                        DirectStreamDiscovery.registerLocalStream(streamKey, streamTitle)

                                        coroutineScope.launch {
                                            try {
                                                val registered = ApiClient.registerStream(
                                                    streamKey = streamKey,
                                                    title = streamTitle,
                                                    streamerId = 1
                                                )
                                                Log.d(TAG, "Stream registered on API server: $registered")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error registering on API server", e)
                                            }
                                        }
                                    }
                                    override fun onConnectionFailedRtmp(reason: String) {
                                        Log.e(TAG, "Connection failed: $reason")
                                        statusMessage = "Connection failed: $reason"
                                        errorMessage = "Error: $reason"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)
                                        DirectStreamDiscovery.unregisterLocalStream()
                                    }
                                    override fun onNewBitrateRtmp(bitrate: Long) {
                                        Log.d(TAG, "New bitrate: $bitrate")
                                    }
                                    override fun onDisconnectRtmp() {
                                        Log.i(TAG, "Disconnected")
                                        statusMessage = "Stream stopped"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)
                                        DirectStreamDiscovery.unregisterLocalStream()

                                        coroutineScope.launch {
                                            try {
                                                val deactivated = ApiClient.stopStream(streamKey)
                                                Log.d(TAG, "Stream stopped on server: $deactivated")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error stopping stream on server", e)
                                            }
                                        }
                                    }
                                    override fun onAuthErrorRtmp() {
                                        Log.e(TAG, "Authentication error")
                                        statusMessage = "Authentication error"
                                        errorMessage = "Authentication error"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)
                                        DirectStreamDiscovery.unregisterLocalStream()
                                    }
                                    override fun onAuthSuccessRtmp() {
                                        Log.i(TAG, "Authentication successful")
                                    }
                                }
                            )
                            rtmpCamera?.startPreview()
                        }
                    }
                },
                update = { view -> openGlView = view }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (rtmpCamera == null) {
                            errorMessage = "Camera not initialized!"
                            return@Button
                        }

                        if (!isStreaming) {
                            try {
                                Log.d(TAG, "Preparing stream...")
                                statusMessage = "Preparing stream..."

                                rtmpCamera?.prepareVideo(
                                    640, 480,
                                    30,
                                    1200 * 1024,
                                    0,
                                )

                                rtmpCamera?.prepareAudio(
                                    128 * 1024,
                                    44100,
                                    true,
                                )

                                Log.d(TAG, "Starting stream to: $streamUrl")
                                rtmpCamera?.startStream(streamUrl)

                                if (rtmpCamera?.isStreaming == true) {
                                    Log.i(TAG, "Stream started successfully!")
                                } else {
                                    Log.e(TAG, "Stream failed to start!")
                                    errorMessage = "Could not start stream"
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error starting stream", e)
                                errorMessage = "Error starting stream: ${e.message}"
                            }
                        } else {
                            Log.d(TAG, "Stopping stream...")
                            rtmpCamera?.stopStream()
                            statusMessage = "Stream stopped"
                            Log.i(TAG, "Stream stopped.")

                            coroutineScope.launch {
                                try {
                                    val deactivated = ApiClient.stopStream(streamKey)
                                    Log.d(TAG, "Stream stopped on server: $deactivated")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error stopping stream on server", e)
                                }
                            }

                            ApiClient.setLocalStreamKey(null)
                            DirectStreamDiscovery.unregisterLocalStream()
                            isStreaming = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStreaming) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isStreaming) "Stop Stream" else "Start Stream")
                }

                Button(
                    onClick = {
                        rtmpCamera?.switchCamera()
                        Log.d(TAG, "Camera switched")
                    }
                ) {
                    Text("Switch Camera")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Device-to-device streaming",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Make sure both devices are connected to internet\n" +
                                "• On viewing device, enable 'Direct' from top bar\n" +
                                "• Your stream will appear automatically in stream list",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else if (!hasCamera) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Device has no camera!",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else if (!hasAllPermissions && permissionsInitialized) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "App needs permissions to access camera and microphone",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    ) {
                        Text("Request permissions")
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Releasing resources")
            val currentStreamKey = ApiClient.getLocalStreamKey()

            rtmpCamera?.apply {
                if (isStreaming) {
                    stopStream()
                    Log.d(TAG, "Stream stopped when leaving screen")

                    coroutineScope.launch {
                        if (currentStreamKey != null) {
                            try {
                                val stopped = ApiClient.stopStream(currentStreamKey)
                                Log.d(TAG, "Stream stopped on server: $stopped")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error stopping stream on server", e)
                            }
                        }
                    }

                    ApiClient.setLocalStreamKey(null)
                    DirectStreamDiscovery.unregisterLocalStream()
                }
                stopPreview()
            }
            rtmpCamera = null
        }
    }
}