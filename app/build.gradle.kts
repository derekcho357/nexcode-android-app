dependencies {
    // ... existing dependencies ...
    
    // Media Processing & Permissions
    implementation("androidx.media3:media3-extractor:1.4.1")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    
    // Networking for AI API Calls (OkHttp already exists, adding Gson/Moshi for JSON parsing if needed)
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
}