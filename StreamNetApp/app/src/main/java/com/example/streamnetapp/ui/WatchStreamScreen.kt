package com.example.streamnetapp.ui

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.streamnetapp.api.ApiClient
import com.example.streamnetapp.model.LiveStream
import com.example.streamnetapp.utils.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.streamnetapp.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "WatchStreamScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchStreamScreen(streamId: Int, navController: NavController) {
    val context = LocalContext.current
    var stream by remember { mutableStateOf<LiveStream?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var usePublicServer by remember { mutableStateOf(Settings.usePublicServer(context)) }
    val coroutineScope = rememberCoroutineScope()
    var playbackStarted by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var testingNetwork by remember { mutableStateOf(false) }

    // Improved logging for the stream ID
    LaunchedEffect(streamId) {
        Log.d(TAG, "Received stream ID: $streamId (${streamId::class.java.simpleName})")
    }

    // Testăm conectivitatea la serverele RTMP
    LaunchedEffect(usePublicServer) {
        testingNetwork = true
        try {
            // Assuming RTMP_SERVER and RTMP_PORT are accessible as public constants
            // If not, use hardcoded values matching your setup
            val publicHost = ApiClient.RTMP_SERVER
            val port = ApiClient.RTMP_PORT.toIntOrNull() ?: 1935

            Log.d(TAG, "Testing RTMP servers - public: $publicHost, port: $port")

            val results = NetworkUtils.testRtmpServers(
                publicHost = publicHost,
                localHost = "10.0.2.2", // For emulator
                port = port
            )
            serverStatus = results
            Log.d(TAG, "Server status: $results")

            // If the selected server is not reachable, show a warning
            if ((usePublicServer && serverStatus["public"] == false) ||
                (!usePublicServer && serverStatus["local"] == false)) {

                val serverType = if (usePublicServer) "public" else "local"
                errorMessage = "Serverul $serverType RTMP nu este accesibil. Verificați conexiunea sau încercați alt server."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing server connectivity", e)
        } finally {
            testingNetwork = false
        }
    }

    // Încărcăm informațiile despre stream
    LaunchedEffect(streamId) {
        Log.d(TAG, "Încărcăm stream cu ID: $streamId")
        coroutineScope.launch {
            try {
                val streams = ApiClient.fetchStreams()
                Log.d(TAG, "Streams încărcate: ${streams.size}")
                Log.d(TAG, "ID-uri disponibile: ${streams.map { it.id }}")

                stream = streams.find { it.id == streamId }
                Log.d(TAG, "Stream găsit: ${stream?.title}, key: ${stream?.streamKey}")
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Eroare la obținerea stream-ului", e)
                errorMessage = "Eroare la încărcarea stream-ului: ${e.message}"
                isLoading = false
            }
        }
    }

    // URL-ul RTMP pentru stream
    val streamUrl = remember(stream, usePublicServer) {
        if (stream != null) {
            // Pass the usePublicServer parameter
            val url = ApiClient.getRtmpUrl(stream!!.streamKey, usePublicServer)
            Log.d(TAG, "Built RTMP URL: $url (usePublicServer: $usePublicServer)")
            url
        } else {
            // Dacă am primit id-ul stream-ului local, dar acesta nu e încă în listă
            if (streamId == ApiClient.LOCAL_STREAM_ID && ApiClient.hasLocalStream()) {
                val localKey = ApiClient.getLocalStreamKey() ?: "streamkey"
                // Pass the usePublicServer parameter
                val url = ApiClient.getRtmpUrl(localKey, usePublicServer)
                Log.d(TAG, "Built local RTMP URL: $url (usePublicServer: $usePublicServer)")
                url
            } else {
                // URL implicit
                // Pass the usePublicServer parameter
                val url = ApiClient.getRtmpUrl("fallback", usePublicServer)
                Log.d(TAG, "Using fallback RTMP URL: $url (usePublicServer: $usePublicServer)")
                url
            }
        }
    }

    // Create ExoPlayer - simplified to use only stable APIs
    val exoPlayer = remember {
        // Basic ExoPlayer setup without unstable APIs
        ExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true

                // Basic player listener
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "Player: BUFFERING")
                                isLoading = true
                            }
                            Player.STATE_READY -> {
                                Log.d(TAG, "Player: READY")
                                isLoading = false
                                errorMessage = null
                                playbackStarted = true
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "Player: ENDED")
                                errorMessage = "Stream-ul s-a încheiat"
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "Player: IDLE")
                                if (playbackStarted) {
                                    // Only show error if we were previously playing
                                    errorMessage = "Stream-ul nu poate fi redat"
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // Basic error logging
                        Log.e(TAG, "Player error: ${error.message}")

                        // User-friendly error messages
                        val errorMsg = when (error.errorCode) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Nu s-a putut conecta la server. Verificați conexiunea la internet."
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                "Conexiunea la server a expirat. Încercați din nou."
                            else -> "Eroare la redarea stream-ului: ${error.message}"
                        }

                        errorMessage = errorMsg
                        isLoading = false
                    }
                })
            }
    }

    // Actualizăm URL-ul când se schimbă stream-ul
    LaunchedEffect(streamUrl) {
        playbackStarted = false
        Log.d(TAG, "Setting media URL: $streamUrl")

        try {
            exoPlayer.apply {
                stop()
                clearMediaItems()

                // Simple MediaItem construction
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            // Add a timeout to detect if the stream fails to start
            delay(8000) // Wait 8 seconds
            if (!playbackStarted && exoPlayer.playbackState != Player.STATE_BUFFERING) {
                Log.d(TAG, "Stream failed to start after timeout")
                if (errorMessage == null) {
                    errorMessage = "Nu s-a putut conecta la stream. Serverul este disponibil?"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting media item", e)
            errorMessage = "Eroare la configurarea player-ului: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stream?.title ?: "Stream Live")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigare înapoi")
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    // Indicator server public/local
                    Text(
                        text = if (usePublicServer) "Public" else "Local",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Buton pentru a schimba între server local și public
                    IconButton(onClick = {
                        usePublicServer = !usePublicServer
                        Settings.setUsePublicServer(context, usePublicServer)
                        Log.d(TAG, "Schimbat la server ${if (usePublicServer) "public" else "local"}")
                    }) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = if (usePublicServer) "Server public" else "Server local"
                        )
                    }

                    // Buton pentru refresh
                    IconButton(onClick = {
                        Log.d(TAG, "Refresh player")
                        playbackStarted = false
                        errorMessage = null
                        isLoading = true
                        exoPlayer.apply {
                            stop()
                            clearMediaItems()
                            // Simple MediaItem construction
                            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                            setMediaItem(mediaItem)
                            prepare()
                            play()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reîncarcă")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display server testing status
            if (testingNetwork) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Se testează conectivitatea la servere RTMP...")
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (serverStatus.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Status Servere RTMP",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Server Public:")
                            Text(
                                text = if (serverStatus["public"] == true) "Accesibil" else "Inaccesibil",
                                color = if (serverStatus["public"] == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Server Local:")
                            Text(
                                text = if (serverStatus["local"] == true) "Accesibil" else "Inaccesibil",
                                color = if (serverStatus["local"] == true)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Server activ: ${if (usePublicServer) "Public" else "Local"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if ((usePublicServer && serverStatus["public"] == false) ||
                            (!usePublicServer && serverStatus["local"] == false)) {

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Serverul selectat nu este accesibil. Încercați alt server.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    usePublicServer = !usePublicServer
                                    Settings.setUsePublicServer(context, usePublicServer)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Comută la server ${if (usePublicServer) "Local" else "Public"}")
                            }
                        }
                    }
                }
            }

            // Player pentru stream
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            // Use simpler API for PlayerView
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Indicator de încărcare
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Mesaj de eroare
                errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Buton pentru a încerca să schimbe serverul
                            Button(
                                onClick = {
                                    usePublicServer = !usePublicServer
                                    Settings.setUsePublicServer(context, usePublicServer)
                                    Log.d(TAG, "Încercare comutare la server ${if (usePublicServer) "public" else "local"}")

                                    // Reset error state
                                    errorMessage = null
                                    isLoading = true
                                    playbackStarted = false
                                }
                            ) {
                                Text("Încearcă server ${if (usePublicServer) "Local" else "Public"}")
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    playbackStarted = false

                                    // Reload the player
                                    exoPlayer.apply {
                                        stop()
                                        clearMediaItems()
                                        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                                        setMediaItem(mediaItem)
                                        prepare()
                                        play()
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Reîncearcă")
                            }
                        }
                    }
                }
            }

            // RTMP URL Debugging Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informații de Depanare",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("RTMP URL:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = streamUrl,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Sfaturi de depanare:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "1. Asigurați-vă că serverul RTMP rulează\n" +
                                "2. Verificați firewall-ul și portul 1935\n" +
                                "3. Dacă folosiți emulator, folosiți 10.0.2.2 pentru localhost\n" +
                                "4. Pe dispozitiv real, folosiți IP-ul real al computerului",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Rest of the component remains the same
            // Informații despre stream
            stream?.let { currentStream ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Titlu stream
                        Text(
                            text = currentStream.title,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Detalii stream
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID Stream:")
                            Text("${currentStream.id}")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Streamer ID:")
                            Text("${currentStream.streamerId}")
                        }

                        // Display stream key for debugging purposes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stream Key:")
                            Text("${currentStream.streamKey}")
                        }

                        if (currentStream.timestamp.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Data:")
                                Text(currentStream.timestamp)
                            }
                        }

                        // Informații suplimentare dacă e stream-ul local
                        if (currentStream.id == ApiClient.LOCAL_STREAM_ID ||
                            currentStream.streamKey == ApiClient.getLocalStreamKey()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Acesta este stream-ul tău!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    Log.d(TAG, "Navigare la ecranul de stream")
                                    navController.navigate("liveStream")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Configurează Stream")
                            }
                        }
                    }
                }
            } ?: run {
                // Afișăm un indicator de încărcare dacă stream-ul nu a fost găsit încă
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Se încarcă informațiile despre stream...")
                        }
                    }
                } else {
                    // Afișăm un mesaj de eroare dacă stream-ul nu a fost găsit
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Stream-ul nu a fost găsit",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { navController.popBackStack() }
                            ) {
                                Text("Înapoi")
                            }
                        }
                    }
                }
            }
        }
    }

    // Eliberăm resursele când ecranul este părăsit
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Eliberăm resursele player-ului")
            exoPlayer.release()
        }
    }
}