package com.example.commentanalyze

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.commentanalyze.ui.theme.CommentAnalyzeTheme

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    // Screenshot permission launcher
    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                // Store the screenshot permission
                ScreenshotPermissionManager.setPermission(result.resultCode, data)
                println("Screenshot permission granted!")
            }
        } else {
            println("Screenshot permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Handle permission renewal request
        if (intent.getBooleanExtra("REQUEST_PERMISSION", false)) {
            requestScreenshotPermission()
        }

        setContent {
            CommentAnalyzeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestScreenshotPermission = { requestScreenshotPermission() },
                        onStartOverlayService = { startOverlayService() },
                        hasOverlayPermission = { Settings.canDrawOverlays(this) },
                        hasScreenshotPermission = { ScreenshotPermissionManager.hasPermission() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("MainActivity: onNewIntent called with REQUEST_PERMISSION: ${intent.getBooleanExtra("REQUEST_PERMISSION", false)}")
        if (intent.getBooleanExtra("REQUEST_PERMISSION", false)) {
            requestScreenshotPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestScreenshotPermission() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        screenshotPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenshotPermission: () -> Unit,
    onStartOverlayService: () -> Unit,
    hasOverlayPermission: () -> Boolean,
    hasScreenshotPermission: () -> Boolean
) {
    var overlayPermissionGranted by remember { mutableStateOf(hasOverlayPermission()) }
    var screenshotPermissionGranted by remember { mutableStateOf(hasScreenshotPermission()) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Comment Analyzer",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Overlay Permission
        if (overlayPermissionGranted) {
            Text("✅ Overlay permission granted!")
        } else {
            Text("📱 Overlay permission needed")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                onRequestOverlayPermission()
                overlayPermissionGranted = hasOverlayPermission()
            }) {
                Text("Grant Overlay Permission")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screenshot Permission
        if (screenshotPermissionGranted) {
            Text("✅ Screenshot permission granted!")
        } else {
            Text("📸 Screenshot permission needed")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                onRequestScreenshotPermission()
                screenshotPermissionGranted = hasScreenshotPermission()
            }) {
                Text("Grant Screenshot Permission")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Service Button
        if (overlayPermissionGranted && screenshotPermissionGranted) {
            Button(onClick = onStartOverlayService) {
                Text("Start Overlay Service")
            }
        }
    }
}