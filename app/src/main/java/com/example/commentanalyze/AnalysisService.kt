package com.example.commentanalyze

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine

class AnalysisService(private val context: Context) {

    fun cleanup() {
        screenCaptureHelper?.release()
        serviceScope.cancel()
    }

    fun clearScreenCapture() {
        screenCaptureHelper?.release()
        screenCaptureHelper = null
        println("ScreenCaptureHelper cleared")
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val httpClient = OkHttpClient()
    private var screenCaptureHelper: ScreenCaptureHelper? = null



    fun initializeMediaProjection(resultCode: Int, data: android.content.Intent) {
        if (screenCaptureHelper == null) {
            screenCaptureHelper = ScreenCaptureHelper(context)
        }
        screenCaptureHelper?.setMediaProjection(resultCode, data)
        println("MediaProjection initialized in AnalysisService")
    }

    fun analyzeScreen(context: Context, onResult: (List<ToxicText>) -> Unit, onPermissionNeeded: () -> Unit) {
        serviceScope.launch {
            try {
                println("=== STARTING SCREEN ANALYSIS ===")

                // Check if we have valid, unused permission
                if (!ScreenshotPermissionManager.hasPermission()) {
                    println("No valid screenshot permission - requesting renewal")
                    withContext(Dispatchers.Main) {
                        onPermissionNeeded()
                    }
                    return@launch
                }

                // Mark permission as used
                ScreenshotPermissionManager.markPermissionUsed()

                // Debug screenCaptureHelper
                println("screenCaptureHelper is null: ${screenCaptureHelper == null}")

                if (screenCaptureHelper == null) {
                    println("ERROR: screenCaptureHelper is null - initializing...")
                    screenCaptureHelper = ScreenCaptureHelper(context)

                    // Re-initialize MediaProjection
                    val resultCode = ScreenshotPermissionManager.getResultCode()
                    val data = ScreenshotPermissionManager.getData()
                    if (resultCode != null && data != null) {
                        screenCaptureHelper?.setMediaProjection(resultCode, data)
                        println("Re-initialized MediaProjection")
                    }
                }

                // Take screenshot
                println("Taking screenshot...")
                val screenshot = screenCaptureHelper?.captureScreen()
                println("captureScreen() returned: ${screenshot != null}")

                if (screenshot != null) {
                    println("Screenshot captured successfully: ${screenshot.width}x${screenshot.height}")

                    // Extract text using OCR
                    val extractedText = extractTextFromBitmap(screenshot)
                    println("Extracted text: $extractedText")

                    if (extractedText.isNotEmpty()) {
                        // Analyze with ChatGPT
                        val toxicResults = analyzeWithChatGPT(extractedText)

                        withContext(Dispatchers.Main) {
                            onResult(toxicResults)
                        }
                    } else {
                        println("No text found in screenshot")
                        withContext(Dispatchers.Main) {
                            onResult(emptyList())
                        }
                    }
                } else {
                    println("Failed to capture screenshot - permission might be expired")
                    // Clear expired permission
                    ScreenshotPermissionManager.clearPermission()
                    withContext(Dispatchers.Main) {
                        onPermissionNeeded()
                    }
                }

            } catch (e: Exception) {
                println("Error in analyzeScreen: ${e.message}")
                e.printStackTrace()

                // Check if it's permission-related error
                if (e.message?.contains("permission", ignoreCase = true) == true) {
                    ScreenshotPermissionManager.clearPermission()
                    withContext(Dispatchers.Main) {
                        onPermissionNeeded()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(emptyList())
                    }
                }
            }
        }
    }

    private suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    println("OCR completed. Text length: ${extractedText.length}")
                    continuation.resumeWith(Result.success(extractedText))
                }
                .addOnFailureListener { exception ->
                    println("OCR failed: ${exception.message}")
                    continuation.resumeWith(Result.failure(exception))
                }
        }
    }

    private suspend fun analyzeWithChatGPT(text: String): List<ToxicText> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("text", text)
                }

                val request = Request.Builder()
                    .url("https://comment-analyzer-backend-z3nf.onrender.com/api/analyze-comments")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val results = parseChatGPTResponse(responseBody)
                    return@withContext results
                } else {
                    println("Server Error: ${response.code} - $responseBody")
                    emptyList()
                }

            } catch (e: Exception) {
                println("Exception in analyzeWithChatGPT: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun parseChatGPTResponse(responseBody: String): List<ToxicText> {
        return try {
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            println("ChatGPT content: $content")

            // Parse the JSON array from ChatGPT response
            val toxicArray = JSONArray(content.trim())
            val results = mutableListOf<ToxicText>()

            for (i in 0 until toxicArray.length()) {
                val item = toxicArray.getJSONObject(i)
                results.add(
                    ToxicText(
                        text = item.getString("text"),
                        type = item.getString("type"),
                        confidence = item.getDouble("confidence").toFloat()
                    )
                )
            }

            results
        } catch (e: Exception) {
            println("Parse error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}

data class ToxicText(
    val text: String,
    val type: String,
    val confidence: Float
)