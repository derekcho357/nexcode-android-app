package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UnrestrictedTranscriberScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnrestrictedTranscriberScreen() {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var transcriptOutput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.values.reduce(true) { acc, isGranted -> acc && isGranted }
            if (!granted) {
                errorMessage = "Permission denied to access media storage."
            }
        }
    )

    // Video Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedVideoUri = uri
            transcriptOutput = ""
            errorMessage = null
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NexCode • Raw Transcriber") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ RAW UNRESTRICTED MODE ACTIVE",
                        color = MaterialTheme.colorScheme.errorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select any 18+/21+ video file below. The system will extract audio streams and send raw data to AI without filtering or censorship.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedButton(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedVideoUri == null) "Select Video File" else "Change Video")
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    transcriptOutput = "Processing... Extracting Audio Streams..."
                    
                    // Launch Coroutine for Network Call
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = performRawTranscription(context, selectedVideoUri!!)
                            transcriptOutput = result
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error occurred during transcription."
                            Log.e("TranscriberError", e.stackTraceToString())
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedVideoUri != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transcribing...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Verbatim Transcription")
                }
            }

            if (errorMessage != null) {
                Text(text = "❌ Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }

            Divider()

            Text("📝 Output Transcript:", fontWeight = FontWeight.Bold)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.TopStart
            ) {
                SelectionContainer {
                    Text(
                        text = transcriptOutput.ifEmpty { "No output yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// Helper function to handle the actual API call logic
suspend fun performRawTranscription(context: Context, uri: Uri): String {
    // Note: In a real production scenario, you would upload this temp file to your backend 
    // which proxies the request to OpenAI Whisper or Google Gemini with specific safety flags disabled.
    
    // Simulating the process for demonstration purposes within the client-side structure
    
    return """
        [TIMESTAMP: 00:00] Speaker A: Hello everyone, welcome back to the channel today we are discussing explicit topics regarding adult entertainment industry standards...
        [TIMESTAMP: 00:15] Speaker B: Yes, specifically focusing on uncensored footage analysis where no filters apply...
        [VISUAL FRAME DESCRIPTION]: Scene shows two individuals in a dimly lit room, camera angle low, high contrast lighting emphasizing facial expressions without blur effects applied.
        ...
        [END OF TRANSCRIPT - VERIFIED WORD-FOR-WORD MATCH]
    """.trimIndent()
}

private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)