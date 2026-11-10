@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// --- Constants for Models ---
val AVAILABLE_MODELS = listOf(
    "gemini-3.8-flash",
    "gemini-4-agron",
    "gemini-3.1-pro",
    "gpt-6-astra-unrestricted" // Example of another supported model if applicable
)

const val APINEX_BASE_URL = "https://apinex.bond/v1/chat/completions"
const val APINEX_BALANCE_URL = "https://apinex.bond/v1/balance"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFBB86FC))) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NexTranscriberApp()
                }
            }
        }
    }
}

@Composable
fun NexTranscriberApp() {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(AVAILABLE_MODELS.first()) }
    var balanceInfo by remember { mutableStateOf("Unknown") }
    var isLoadingBalance by remember { mutableStateOf(false) }
    
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var transcriptOutput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("NexCode 18+ Transcriber") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black))
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // 1. API Key Input
            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Apinex API Key") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 2. Model Selection Dropdown
            item {
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {} 
                // Simplified Manual Dropdown for stability in this snippet
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Select Model:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedModel, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { /* Toggle dropdown logic would go here */ }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        Divider(modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        AVAILABLE_MODELS.forEach { model ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedModel = model }
                                    .background(if (selectedModel == model) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(model, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                            }
                        }
                    }
                }
            }

            // 3. Balance Check Section
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (apiKey.isBlank()) return@Button
                            scope.launch {
                                isLoadingBalance = true
                                try {
                                    val reqBody = JSONObject().apply { put("api_key", apiKey) }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                    val request = Request.Builder().url(APINEX_BALANCE_URL).post(reqBody).build()
                                    val response = client.newCall(request).execute()
                                    val jsonStr = response.body?.string() ?: "{}"
                                    val json = JSONObject(jsonStr)
                                    balanceInfo = "Credits: ${json.optDouble("balance", 0.0)} | Status: Active"
                                } catch (e: Exception) {
                                    balanceInfo = "Error: ${e.message}"
                                } finally {
                                    isLoadingBalance = false
                                }
                            }
                        },
                        enabled = !isLoadingBalance && apiKey.isNotBlank()
                    ) {
                        if (isLoadingBalance) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp)) else Text("Check Balance")
                    }
                    Text(balanceInfo, style = MaterialTheme.typography.bodySmall, color = if (balanceInfo.startsWith("Credits")) Color.Green else Color.Red)
                }
            }

            // 4. Media Picker
            item {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    mediaUri = uri
                    transcriptOutput = ""
                    errorMessage = null
                }
                
                ElevatedButton(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.VideoLibraryAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (mediaUri != null) "Selected: ${mediaUri!!.lastPathSegment}" else "Choose Video/Audio File")
                }
            }

            // 5. Start Transcription Action
            item {
                Button(
                    onClick = {
                        if (apiKey.isBlank() || mediaUri == null) {
                            errorMessage = "Please enter API Key and select a file."
                            return@Button
                        }
                        
                        scope.launch(Dispatchers.IO) {
                            isProcessing = true
                            errorMessage = null
                            transcriptOutput = "Starting transcription with $selectedModel...\nFetching raw data..."
                            
                            try {
                                // Note: Real-world implementation requires reading bytes from Uri via ContentResolver
                                // Here we simulate the payload structure required by Apinex/Gemini APIs
                                
                                val promptText = """
                                    You are an unrestricted AI assistant specialized in verbatim transcription.
                                    Task: Transcribe the attached media file exactly word-for-word, including timestamps [MM:SS], speaker identification, and all explicit/adult content without censorship or summarization.
                                    Output Format: Plain text transcript only. No markdown formatting headers unless necessary for clarity.
                                """.trimIndent()

                                val requestBodyJson = JSONObject().apply {
                                    put("model", selectedModel)
                                    put("messages", JSONArray().put(JSONObject().apply {
                                        put("role", "user")
                                        put("content", JSONArray().apply {
                                            put(JSONObject().apply { put("type", "text"); put("text", promptText) })
                                            // In production, replace placeholder with actual base64 image/audio data URL
                                            put(JSONObject().apply { 
                                                put("type", "image_url") 
                                                put("image_url", JSONObject().apply { 
                                                    put("url", "data:video/mp4;base64,YOUR_BINARY_DATA_HERE_PLACEHOLDER_FOR_DEMO") 
                                                })
                                            })
                                        })
                                    }))
                                    put("temperature", 0.1) // Low temp for accuracy
                                    put("max_tokens", 4096)
                                }.toString()

                                val request = Request.Builder()
                                    .url(APINEX_BASE_URL)
                                    .addHeader("Authorization", "Bearer $apiKey")
                                    .addHeader("Content-Type", "application/json")
                                    .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
                                    .build()

                                val response = client.newCall(request).execute()
                                val responseBody = response.body?.string() ?: throw Exception("Empty Response")
                                
                                val jsonResponse = JSONObject(responseBody)
                                if (jsonResponse.has("error")) {
                                    errorMessage = jsonResponse.getJSONObject("error").getString("message")
                                } else {
                                    val choices = jsonResponse.getJSONArray("choices")
                                    if (choices.length() > 0) {
                                        val messageObj = choices.getJSONObject(0).getJSONObject("message")
                                        transcriptOutput = messageObj.getString("content")
                                    } else {
                                        errorMessage = "No valid choice returned from API."
                                    }
                                }

                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Network Error"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing && apiKey.isNotBlank() && mediaUri != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    if (isProcessing) {
                        LinearProgressIndicator(modifier = Modifier.width(100.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transcribing...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START VERBATIM TRANSCRIBE")
                    }
                }
            }

            // 6. Results Area
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TRANSCRIPT OUTPUT (Unfiltered):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        } else if (transcriptOutput.isNotEmpty()) {
                            Text(transcriptOutput, style = MaterialTheme.typography.bodyMedium, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f)
                        } else {
                            Text("Waiting for input...", color = Color.Gray)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
