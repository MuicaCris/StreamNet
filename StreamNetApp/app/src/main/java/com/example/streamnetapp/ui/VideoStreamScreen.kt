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
    var streamTitle by remember { mutableStateOf("Stream-ul meu") }
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
            Log.d(TAG, "Toate permisiunile sunt acordate")
        } else {
            errorMessage = "Permisiuni necesare refuzate! Apasă pe reîncercați."
            Log.e(TAG, "Nu toate permisiunile sunt acordate")
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
            Log.d(TAG, "Permisiuni acordate de utilizator")
        } else {
            errorMessage = "Permisiuni necesare refuzate! Apasă pe reîncercați."
            hasAllPermissions = false
            Log.e(TAG, "Utilizatorul a refuzat unele permisiuni")
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
                    text = "Configurare Stream",
                    style = MaterialTheme.typography.titleMedium
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = streamTitle,
                    onValueChange = { streamTitle = it },
                    label = { Text("Titlu stream") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Folosește server public:")
                    Switch(
                        checked = usePublicServer,
                        onCheckedChange = {
                            if (!isStreaming) {
                                usePublicServer = it
                                Settings.setUsePublicServer(context, it)
                            } else {
                                errorMessage = "Nu poti schimba serverul in timpul streaming-ului!"
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cheie stream: $streamKey",
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
                            Text("Reincearca")
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
                                        Log.i(TAG, "Conexiune inceputa: $rtmpUrl")
                                        statusMessage = "Se conecteaza la server..."
                                    }
                                    override fun onConnectionSuccessRtmp() {
                                        Log.i(TAG, "Conexiune reusita!")
                                        statusMessage = "Stream activ!"
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
                                                Log.d(TAG, "Stream inregistrat pe server API: $registered")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Eroare la inregistrarea pe server API", e)
                                            }
                                        }
                                    }
                                    override fun onConnectionFailedRtmp(reason: String) {
                                        Log.e(TAG, "Conexiune eșuată: $reason")
                                        statusMessage = "Conexiune eșuată: $reason"
                                        errorMessage = "Eroare: $reason"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)

                                        DirectStreamDiscovery.unregisterLocalStream()
                                    }
                                    override fun onNewBitrateRtmp(bitrate: Long) {
                                        Log.d(TAG, "Bitrate nou: $bitrate")
                                    }
                                    override fun onDisconnectRtmp() {
                                        Log.i(TAG, "Deconectat")
                                        statusMessage = "Stream oprit"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)

                                        DirectStreamDiscovery.unregisterLocalStream()

                                        coroutineScope.launch {
                                            try {
                                                val deactivated = ApiClient.deactivateStream(streamKey)
                                                Log.d(TAG, "Stream dezactivat pe server API: $deactivated")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Eroare la dezactivarea pe server API", e)
                                            }
                                        }
                                    }
                                    override fun onAuthErrorRtmp() {
                                        Log.e(TAG, "Eroare de autentificare")
                                        statusMessage = "Eroare de autentificare"
                                        errorMessage = "Eroare de autentificare"
                                        isStreaming = false

                                        ApiClient.setLocalStreamKey(null)

                                        DirectStreamDiscovery.unregisterLocalStream()
                                    }
                                    override fun onAuthSuccessRtmp() {
                                        Log.i(TAG, "Autentificare reușită")
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
                            errorMessage = "Camera nu este initializata!"
                            return@Button
                        }

                        if (!isStreaming) {
                            try {
                                Log.d(TAG, "Pregatire stream...")
                                statusMessage = "Pregatire stream..."

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

                                Log.d(TAG, "Incepe stream-ul catre: $streamUrl")
                                rtmpCamera?.startStream(streamUrl)

                                if (rtmpCamera?.isStreaming == true) {
                                    Log.i(TAG, "Stream-ul a inceput cu succes!")
                                } else {
                                    Log.e(TAG, "Stream-ul nu a inceput!")
                                    errorMessage = "Stream-ul nu a putut fi pornit"
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Eroare la pornirea stream-ului", e)
                                errorMessage = "Eroare la pornirea stream-ului: ${e.message}"
                            }
                        } else {
                            Log.d(TAG, "Opreste stream-ul...")
                            rtmpCamera?.stopStream()
                            statusMessage = "Stream oprit"
                            Log.i(TAG, "Stream-ul a fost oprit.")

                            ApiClient.setLocalStreamKey(null)

                            DirectStreamDiscovery.unregisterLocalStream()

                            coroutineScope.launch {
                                try {
                                    val deactivated = ApiClient.deactivateStream(streamKey)
                                    Log.d(TAG, "Stream dezactivat pe server API: $deactivated")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Eroare la dezactivarea pe server API", e)
                                }
                            }

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
                        Log.d(TAG, "Camera comutata")
                    }
                ) {
                    Text("Schimba Camera")
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
                        text = "Stream intre dispozitive",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Asigura-te ca ambele dispozitive sunt conectate la internet\n" +
                                "• Pe dispozitivul care vizioneaza, activează 'Direct' din bara de sus\n" +
                                "• Stream-ul tau va aparea automat in lista de stream-uri",
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
                    text = "Dispozitivul nu are camera!",
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
                        text = "Aplicatia are nevoie de permisiuni pentru a accesa camera și microfonul",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    ) {
                        Text("Solicit permisiuni")
                    }
                }
            }
        }
    }

//    DisposableEffect(Unit) {
//        onDispose {
//            Log.d(TAG, "Eliberam resursele")
//            rtmpCamera?.apply {
//                if (isStreaming) {
//                    stopStream()
//                    Log.d(TAG, "Stream oprit la parasirea ecranului")
//
//                    ApiClient.setLocalStreamKey(null)
//                    DirectStreamDiscovery.unregisterLocalStream()
//
//                    coroutineScope.launch {
//                        try {
//                            val deactivated = ApiClient.deactivateStream(streamKey)
//                            Log.d(TAG, "Stream dezactivat pe server API: $deactivated")
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Eroare la dezactivarea pe server API", e)
//                        }
//                    }
//                }
//                stopPreview()
//            }
//            rtmpCamera = null
//        }
//    }
}