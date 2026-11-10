val selectedVideoUri = selectedVideoUri // already declared as nullable
if (selectedVideoUri != null) {
    val result = performRawTranscription(context, selectedVideoUri)
    // handle result
} else {
    // show error to user: no video selected
}