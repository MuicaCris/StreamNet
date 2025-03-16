import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@Composable
fun LiveStreamUI() {
    val context = LocalContext.current
    var rtmpCamera by remember { mutableStateOf<RtmpCamera2?>(null) }
    var openGlView by remember { mutableStateOf<OpenGlView?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val streamUrl = "rtmp://10.0.2.2:1935/live/streamkey"

    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            errorMessage = "Permisiuni necesare refuzate!"
            Log.e("RTMP", "Permisiuni insuficiente")
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (hasCamera) {
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
                                        Log.i("RTMP", "Conexiune începută: $rtmpUrl")
                                    }
                                    override fun onConnectionSuccessRtmp() {
                                        Log.i("RTMP", "Conexiune reușită!")
                                    }
                                    override fun onConnectionFailedRtmp(reason: String) {
                                        Log.e("RTMP", "Conexiune eșuată: $reason")
                                    }
                                    override fun onNewBitrateRtmp(bitrate: Long) {
                                        Log.d("RTMP", "Bitrate nou: $bitrate")
                                    }
                                    override fun onDisconnectRtmp() {
                                        Log.i("RTMP", "Deconectat")
                                    }
                                    override fun onAuthErrorRtmp() {
                                        Log.e("RTMP", "Eroare de autentificare")
                                    }
                                    override fun onAuthSuccessRtmp() {
                                        Log.i("RTMP", "Autentificare reușită")
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

            Button(
                onClick = {
                    if (rtmpCamera == null) {
                        Log.e("RTMP", "Camera nu este inițializată!")
                        return@Button
                    }

                    if (!isStreaming) {
                        try {
                            Log.d("RTMP", "Pregătesc video și audio...")
                            rtmpCamera?.prepareVideo(640, 480, 30, 1200 * 1024, 0)
                            rtmpCamera?.prepareAudio(128 * 1024, 44100, true)

                            Log.d("RTMP", "Încep stream-ul către: $streamUrl")
                            rtmpCamera?.startStream(streamUrl)

                            if (rtmpCamera?.isStreaming == true) {
                                Log.i("RTMP", "Stream-ul a început cu succes!")
                            } else {
                                Log.e("RTMP", "Stream-ul nu a început!")
                            }
                        } catch (e: Exception) {
                            Log.e("RTMP", "Eroare la pornirea stream-ului: ${e.message}")
                        }
                    } else {
                        Log.d("RTMP", "Oprește stream-ul...")
                        rtmpCamera?.stopStream()
                        Log.i("RTMP", "Stream-ul a fost oprit.")
                    }
                    isStreaming = !isStreaming
                }
            ) {
                Text(if (isStreaming) "Stop Stream" else "Start Stream")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rtmpCamera?.apply {
                if (isStreaming) stopStream()
                stopPreview()
            }
            rtmpCamera = null
        }
    }
}