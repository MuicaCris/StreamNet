package com.example.streamnetclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

class MainScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //MainMenuScreen()
        }
    }
}

@Composable
fun MainMenuScreen(navController: NavController, userType: String) {
    val darkPurple = Color(0xFF1A1A2E)
    val purple = Color(0xFF2F2F4F)
    val greenAccent = Color(0xFF00FF7F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkPurple)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "StreamNet",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (userType == "streamer") {
            CustomButton(
                text = "Start Live Stream",
                onClick = { navController.navigate("live_stream") },
                color = greenAccent
            )
        }


        CustomButton(
            text = "Audio Settings",
            onClick = { navController.navigate("audio_settings") },
            color = purple
        )
    }
}

@Composable
fun CustomButton(text: String, onClick: () -> Unit, color: Color) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(50.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

fun LiveStreamScreen() {

}
