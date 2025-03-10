package com.example.streamnetapp.ui

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.streamnetapp.ui.theme.StreamNetTheme

@Composable
fun LiveStreamUI(isDarkMode: MutableState<Boolean>) {
    var isStreaming by remember { mutableStateOf(false) }
    val chatMessages = remember { mutableStateListOf("Welcome to the stream!") }

    // Context și lifecycle owner necesare pentru CameraX
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Video Preview cu CameraX
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // Inițializează CameraX
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                            // Configurare preview
                            val preview = androidx.camera.core.Preview.Builder().build().also { preview ->
                                preview.setSurfaceProvider(surfaceProvider)
                            }

                            // Selectează camera din spate
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                // Eliberează resursele anterioare și lansează camera
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                Log.e("CameraX", "Eroare la inițializarea camerei", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Butoane pentru control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isStreaming = !isStreaming },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isStreaming) "Stop Stream" else "Start Stream")
            }
            Button(
                onClick = { /* Schimbă camera */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Switch Camera")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat live
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(chatMessages) { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}