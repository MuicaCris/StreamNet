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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.streamnetapp.api.fetchStreams
import com.example.streamnetapp.model.LiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var streams by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var isStreaming by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        streams = withContext(Dispatchers.IO) {
            fetchStreams()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchBar(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        onSearch = { /* Logica pentru căutare */ },
                        active = isSearchActive,
                        onActiveChange = { isSearchActive = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Caută...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Căutare"
                            )
                        },
                        trailingIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Șterge"
                                    )
                                }
                            }
                        }
                    ) {
                        Column { Text("Rezultate căutare:") }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("loginMenu") }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Log In/Sign In",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { navController.navigate("settingsMenu") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isStreaming = !isStreaming },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isStreaming) "Stop Stream" else "Start Stream")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isStreaming) {
                    LiveStreamUI()
                }

                LazyColumn {
                    if (streams.isEmpty()) {
                        item {
                            Text(
                                text = "Nu există stream-uri disponibile",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(streams) { stream ->
                            StreamItem(stream, navController)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StreamItem(stream: LiveStream, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("watchStream/${stream.id}") }
    ) {
        AsyncImage(
            model = stream.thumbnail,
            contentDescription = "Thumbnail stream",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Text(
            text = stream.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Streamer: ${stream.streamerName}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
