package com.example.flutter_application_services

import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.flutter_application_services/websocket"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "startWebSocketService") {
                startWebSocketService()
                result.success(null)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun startWebSocketService() {
        val intent = Intent(this, WebSocketService::class.java)
        startService(intent)  // Or startForegroundService(intent) if necessary
    }
}