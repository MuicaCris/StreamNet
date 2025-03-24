package com.example.streamnetapp.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.streamnetapp.api.ApiClient
import com.example.streamnetapp.model.LiveStream
import com.example.streamnetapp.utils.DirectStreamDiscovery
import com.example.streamnetapp.utils.Settings
import kotlinx.coroutines.launch

private const val TAG = "MainMenu"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(navController: NavController) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var streams by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var useDirectDiscovery by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    fun loadStreams() {
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val fetchedStreams = ApiClient.fetchStreams()
                Log.d(TAG, "Stream-uri încărcate din API: ${fetchedStreams.size}")
                streams = fetchedStreams
                isLoading = false
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Eroare la încărcarea stream-urilor din API", e)
                errorMessage = "Eroare API: ${e.message}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Pornire DirectStreamDiscovery")
        DirectStreamDiscovery.startDiscovery { discoveredStreams ->
            Log.d(TAG, "Stream-uri descoperite direct: ${discoveredStreams.size}")
            streams = discoveredStreams
            isLoading = false
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Oprire DirectStreamDiscovery")
            DirectStreamDiscovery.stopDiscovery()
        }
    }

    LaunchedEffect(useDirectDiscovery) {
        if (!useDirectDiscovery) {
            loadStreams()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Caută...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Căutare") },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Șterge")
                                }
                            }
                        }
                    )
                },
                actions = {
                    Text(
                        text = if (useDirectDiscovery) "Direct" else "API",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            useDirectDiscovery = !useDirectDiscovery
                            loadStreams()

                        }
                    ) {
                        Icon(
                            if (useDirectDiscovery) Icons.Default.Check else Icons.Default.LocationOn,
                            contentDescription = "Metodă descoperire"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (useDirectDiscovery) {
                                DirectStreamDiscovery.discoverStreams()
                            } else {
                                loadStreams()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reîmprospătează")
                    }

                    IconButton(onClick = { navController.navigate("loginMenu") }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Log In/Sign In")
                    }

                    IconButton(onClick = { navController.navigate("settingsMenu") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bine ai venit la StreamNet!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate("liveStream")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ApiClient.hasLocalStream())
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (ApiClient.hasLocalStream()) "Continua Stream-ul" else "Start Stream")
                }

                if (ApiClient.hasLocalStream()) {
                    Text(
                        text = "Stream-ul tau este activ!",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = "Cheie: ${ApiClient.getLocalStreamKey()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stream-uri disponibile:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Ultima actualizare: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(lastRefreshTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (useDirectDiscovery) {
                                        DirectStreamDiscovery.discoverStreams()
                                    } else {
                                        loadStreams()
                                    }
                                }
                            ) {
                                Text("Reincearca")
                            }
                        }
                    }
                } else if (isLoading && streams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        if (streams.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Nu există stream-uri disponibile",
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                if (useDirectDiscovery) {
                                                    DirectStreamDiscovery.discoverStreams()
                                                } else {
                                                    loadStreams()
                                                }
                                            }
                                        ) {
                                            Text("Reimprospateaza")
                                        }
                                    }
                                }
                            }
                        } else {
                            val filteredStreams = streams.filter {
                                it.title.contains(searchText, ignoreCase = true)
                            }

                            if (filteredStreams.isEmpty()) {
                                item {
                                    Text(
                                        text = "Nu s-au gasit stream-uri care sa corespunda cautarii",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(filteredStreams) { stream ->
                                    StreamItem(stream = stream, navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StreamItem(stream: LiveStream, navController: NavController) {
    val isLocalStream = stream.id == ApiClient.LOCAL_STREAM_ID ||
            stream.streamKey == ApiClient.getLocalStreamKey()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                try {
                    // Convert the ID to String for navigation
                    val streamIdString = stream.id.toString()
                    navController.navigate("watchStream/${streamIdString}")
                } catch (e: Exception) {
                    Log.e("StreamItem", "Navigation error", e)
                }
            }
    ) {
        // Rest of your StreamItem content...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isLocalStream) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text("STREAM-UL TAU")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "ID: ${stream.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stream.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stream Key: ${stream.streamKey}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Streamer ID: ${stream.streamerId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        try {
                            // Convert the ID to String for navigation
                            val streamIdString = stream.id.toString()
                            navController.navigate("watchStream/${streamIdString}")
                        } catch (e: Exception) {
                            Log.e("StreamItem", "Navigation error", e)
                        }
                    }
                ) {
                    Text("Vizionează")
                }
            }
        }
    }
}