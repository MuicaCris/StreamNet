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
    var streamType by remember { mutableStateOf(Settings.getStreamType(context)) }
    val coroutineScope = rememberCoroutineScope()
    var playbackStarted by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var testingNetwork by remember { mutableStateOf(false) }

    LaunchedEffect(streamId) {
        Log.d(TAG, "Received stream ID: $streamId")
    }

    LaunchedEffect(usePublicServer) {
        testingNetwork = true
        try {
            val publicHost = ApiClient.RTMP_SERVER
            val port = if (streamType == ApiClient.STREAM_TYPE_RTMP) {
                ApiClient.RTMP_PORT.toIntOrNull() ?: 1935
            } else {
                8080
            }

            Log.d(TAG, "Testing servers - public: $publicHost, port: $port, type: $streamType")

            val results = NetworkUtils.testRtmpServers(
                publicHost = publicHost,
                localHost = "10.0.2.2",
                port = port
            )
            serverStatus = results
            Log.d(TAG, "Server status: $results")

            if ((usePublicServer && serverStatus["public"] == false) ||
                (!usePublicServer && serverStatus["local"] == false)) {

                val serverType = if (usePublicServer) "public" else "local"
                errorMessage = "$serverType server not accessible. Check connection or try another server."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing server connectivity", e)
        } finally {
            testingNetwork = false
        }
    }

    LaunchedEffect(streamId) {
        Log.d(TAG, "Loading stream with ID: $streamId")
        coroutineScope.launch {
            try {
                val streams = ApiClient.fetchStreams()
                Log.d(TAG, "Loaded streams: ${streams.size}")
                Log.d(TAG, "Available IDs: ${streams.map { it.id }}")

                stream = streams.find { it.id == streamId }
                Log.d(TAG, "Found stream: ${stream?.title}, key: ${stream?.streamKey}")
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Error getting stream", e)
                errorMessage = "Error loading stream: ${e.message}"
                isLoading = false
            }
        }
    }

    val streamUrl = remember(stream, usePublicServer, streamType) {
        if (stream != null) {
            if (streamType == ApiClient.STREAM_TYPE_HLS) {
                val url = ApiClient.getHlsUrl(stream!!.streamKey)
                Log.d(TAG, "Built HLS URL: $url")
                url
            } else {
                val url = ApiClient.getRtmpUrl(stream!!.streamKey, usePublicServer)
                Log.d(TAG, "Built RTMP URL: $url (usePublicServer: $usePublicServer)")
                url
            }
        } else {
            if (streamId == ApiClient.LOCAL_STREAM_ID && ApiClient.hasLocalStream()) {
                val localKey = ApiClient.getLocalStreamKey() ?: "streamkey"
                if (streamType == ApiClient.STREAM_TYPE_HLS) {
                    val url = ApiClient.getHlsUrl(localKey)
                    Log.d(TAG, "Built local HLS URL: $url")
                    url
                } else {
                    val url = ApiClient.getRtmpUrl(localKey, usePublicServer)
                    Log.d(TAG, "Built local RTMP URL: $url (usePublicServer: $usePublicServer)")
                    url
                }
            } else {
                if (streamType == ApiClient.STREAM_TYPE_HLS) {
                    val url = ApiClient.getHlsUrl("fallback")
                    Log.d(TAG, "Using fallback HLS URL: $url")
                    url
                } else {
                    val url = ApiClient.getRtmpUrl("fallback", usePublicServer)
                    Log.d(TAG, "Using fallback RTMP URL: $url (usePublicServer: $usePublicServer)")
                    url
                }
            }
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true

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
                                errorMessage = "Stream ended"
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "Player: IDLE")
                                if (playbackStarted) {
                                    errorMessage = "Cannot play stream"
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}")

                        val errorMsg = when (error.errorCode) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Could not connect to server. Check internet connection."
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                "Connection to server timed out. Try again."
                            else -> "Stream playback error: ${error.message}"
                        }

                        errorMessage = errorMsg
                        isLoading = false
                    }
                })
            }
    }

    LaunchedEffect(streamUrl) {
        playbackStarted = false
        Log.d(TAG, "Setting media URL: $streamUrl")

        try {
            exoPlayer.apply {
                stop()
                clearMediaItems()

                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            delay(8000)
            if (!playbackStarted && exoPlayer.playbackState != Player.STATE_BUFFERING) {
                Log.d(TAG, "Stream failed to start after timeout")
                if (errorMessage == null) {
                    errorMessage = "Could not connect to stream. Is server available?"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting media item", e)
            errorMessage = "Player setup error: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stream?.title ?: "Live Stream")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigating back")
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = if (streamType == ApiClient.STREAM_TYPE_HLS) "HLS" else "RTMP",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(onClick = {
                        streamType = if (streamType == ApiClient.STREAM_TYPE_RTMP) {
                            ApiClient.STREAM_TYPE_HLS
                        } else {
                            ApiClient.STREAM_TYPE_RTMP
                        }
                        Settings.setStreamType(context, streamType)
                        Log.d(TAG, "Switched to $streamType")
                    }) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = if (streamType == ApiClient.STREAM_TYPE_HLS)
                                "HLS Stream" else "RTMP Stream"
                        )
                    }

                    if (streamType == ApiClient.STREAM_TYPE_RTMP) {
                        Text(
                            text = if (usePublicServer) "Public" else "Local",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        IconButton(onClick = {
                            usePublicServer = !usePublicServer
                            Settings.setUsePublicServer(context, usePublicServer)
                            Log.d(TAG, "Switched to ${if (usePublicServer) "public" else "local"} server")
                        }) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = if (usePublicServer) "Public server" else "Local server"
                            )
                        }
                    }

                    IconButton(onClick = {
                        Log.d(TAG, "Refreshing player")
                        playbackStarted = false
                        errorMessage = null
                        isLoading = true
                        exoPlayer.apply {
                            stop()
                            clearMediaItems()
                            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                            setMediaItem(mediaItem)
                            prepare()
                            play()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
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
                        Text("Testing server connectivity...")
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

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
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

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

                            Button(
                                onClick = {
                                    streamType = if (streamType == ApiClient.STREAM_TYPE_RTMP) {
                                        ApiClient.STREAM_TYPE_HLS
                                    } else {
                                        ApiClient.STREAM_TYPE_RTMP
                                    }
                                    Settings.setStreamType(context, streamType)

                                    errorMessage = null
                                    isLoading = true
                                    playbackStarted = false
                                }
                            ) {
                                Text("Try with ${if (streamType == ApiClient.STREAM_TYPE_RTMP) "HLS" else "RTMP"}")
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    playbackStarted = false

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
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Stream Info",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stream type:")
                        Text(
                            text = streamType.uppercase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text("URL:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = streamUrl,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            streamType = if (streamType == ApiClient.STREAM_TYPE_RTMP) {
                                ApiClient.STREAM_TYPE_HLS
                            } else {
                                ApiClient.STREAM_TYPE_RTMP
                            }
                            Settings.setStreamType(context, streamType)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Switch to ${if (streamType == ApiClient.STREAM_TYPE_RTMP) "HLS" else "RTMP"}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Troubleshooting tips:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (streamType == ApiClient.STREAM_TYPE_HLS) {
                            "1. HLS recommended for viewing on other devices\n" +
                                    "2. HLS has higher latency than RTMP\n" +
                                    "3. Make sure server has HLS configured\n" +
                                    "4. HLS URL can be accessed in browser: $streamUrl"
                        } else {
                            "1. RTMP has lower latency\n" +
                                    "2. For emulator, use 10.0.2.2 for localhost\n" +
                                    "3. On real device, use computer's real IP\n" +
                                    "4. HLS recommended for cross-device compatibility"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            stream?.let { currentStream ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = currentStream.title,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stream ID:")
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
                                Text("Date:")
                                Text(currentStream.timestamp)
                            }
                        }

                        if (currentStream.id == ApiClient.LOCAL_STREAM_ID ||
                            currentStream.streamKey == ApiClient.getLocalStreamKey()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "This is your stream!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    Log.d(TAG, "Navigating to stream screen")
                                    navController.navigate("liveStream")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Configure Stream")
                            }
                        }
                    }
                }
            } ?: run {
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

                            Text("Loading stream info...")
                        }
                    }
                } else {
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
                                text = "Stream not found",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { navController.popBackStack() }
                            ) {
                                Text("Back")
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Releasing player resources")
            exoPlayer.release()
        }
    }
}