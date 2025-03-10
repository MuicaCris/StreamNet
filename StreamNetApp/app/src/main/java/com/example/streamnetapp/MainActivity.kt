package com.example.streamnetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streamnetapp.ui.LiveStreamUI
import com.example.streamnetapp.ui.LoginScreen
import com.example.streamnetapp.ui.MainMenu
import com.example.streamnetapp.ui.SettingsMenu
import com.example.streamnetapp.ui.SignupScreen
import com.example.streamnetapp.ui.theme.StreamNetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode = remember { mutableStateOf(true)}

            StreamNetTheme(darkTheme = isDarkMode.value) {
                MainApp(isDarkMode)
            }
        }
    }
}

@Composable
fun MainApp(isDarkMode: MutableState<Boolean>) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mainMenu") {
        composable("mainMenu") { MainMenu(navController) }
        composable("liveStream") { LiveStreamUI(isDarkMode) }
        composable ("settingsMenu") { SettingsMenu(isDarkMode) }
        composable ("loginMenu") { LoginScreen(navController) }
        composable ("signupMenu") { SignupScreen(navController) }
    }
}

