package com.example.streamnetapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streamnetapp.network.WebSocketClient
import com.example.streamnetclient.LiveStreamScreen
import com.example.streamnetclient.MainMenuScreen
import java.net.URI

class MainActivity : ComponentActivity() {
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webSocketClient = WebSocketClient(
            URI("ws://10.0.2.2:5050/ws/"))
        webSocketClient.connect()

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "menu") {
                composable("menu") { backStackEntry ->
                    val userType = backStackEntry.arguments?.getString("userType") ?: "user"
                    MainMenuScreen(navController, userType)
                }
                composable("liveStream") { backStackEntry ->
                    val userType = backStackEntry.arguments?.getString("userType") ?: "user"
                    if (userType == "streamer") {
                        LiveStreamScreen()
                    } else {
                        Text("Acces restricționat: doar streameri pot începe un live stream.")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }
}

@Composable
fun StreamNetUI(webSocketClient: WebSocketClient) {
    var message by remember { mutableStateOf(TextFieldValue("")) }
    val messages = remember { mutableStateListOf("Salut!", "Text mesaj") }

    LaunchedEffect(Unit) {
        webSocketClient.onMessageReceived = { newMessage ->
            messages.add(newMessage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { messageItem ->
                Text(
                    text = messageItem,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Scrie un mesaj") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (message.text.isNotEmpty()) {
                    if (webSocketClient.isOpen) {
                        webSocketClient.send(message.text)
                        message = TextFieldValue("")
                    } else {
                        Log.e("WebSocket", "Conexiunea nu este deschisă!")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trimite")
        }
    }
}
