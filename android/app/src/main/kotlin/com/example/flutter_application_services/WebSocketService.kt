package com.example.flutter_application_services

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WebSocketService : Service() {

    private var webSocketClient: WebSocketClient? = null
    private val CHANNEL_ID = "WebSocketServiceChannel"
    private val CHANNEL_ID_NOTIFICATION = "NotificationChannel"
    private var scheduler: ScheduledExecutorService? = null
    private var networkReceiver: BroadcastReceiver? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 5000L // 5 seconds

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        createNotificationChannel()
        registerNetworkReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "WebSocket Service Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WebSocket Service")
            .setContentText("WebSocket connection is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startWebSocket() {
        if (webSocketClient != null && webSocketClient!!.isOpen) {
            Log.d("WebSocketService", "WebSocket is already connected and open")
            return
        }

        val uri: URI
        try {
            uri = URI("ws://192.168.1.146:8080?token=rHXXqh0sC0BrbLrP06pQZrU8W6O%2FZq66lhCeR4ZuOZk0Haa2uXAXAZ%2Fkgg6gr2ymLDtmW1LFJKDHikNj9iIK%2BpjP0YvayQfW3czB4ooql9mNn0eiB1eDZFXjgURGlQcjOkKQFY5TxZQCl4A8EYxkrGoufAojySjhhYcHV828PBnPJVe%2B0E2ftXKw5ApjfHyDSSreHCfJa0hGOyPfILAdJPIMRgF8IvwawryfWL34HQktkpjF%2Fwemz3oNs6IRY3er948vsOp6md1AWesu1Tn0AbdHsARRN3%2BkMxnK4nH5ufutSGVR1rKgBoMaDu4pNdizj1kvgYcDkxK1HrArnuUt%2Fg%3D%3D")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshake: ServerHandshake) {
                Log.d("WebSocketService", "WebSocket connection opened")
                println("WebSocket connection opened")
                reconnectAttempts = 0 // Reset reconnection attempts on successful connection

                scheduler = Executors.newScheduledThreadPool(1)
                scheduler?.scheduleAtFixedRate({
                    if (webSocketClient != null && webSocketClient!!.isOpen) {
                        webSocketClient!!.send("{\"type\":0, \"last_notification\":7}")
                    }
                }, 0, 5, TimeUnit.SECONDS)
            }

            override fun onMessage(message: String) {
                println("Received message: $message")
                val broadcastIntent = Intent(ACTION_WEBSOCKET_MESSAGE).apply {
                    putExtra(EXTRA_MESSAGE, message)
                }
                sendBroadcast(broadcastIntent)
                handleIncomingMessage(message)
                Log.d("Message Server", message)
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                println("WebSocket connection closed: $reason")
                Log.d("WebSocketService", "WebSocket connection closed: $reason")
                resetWebSocketClient()
                attemptReconnect()
            }

            override fun onError(ex: Exception) {
                println("WebSocket error: ${ex.message}")
                Log.d("WebSocketService", "WebSocket error: ${ex.message}")
                resetWebSocketClient()
                attemptReconnect()
            }
        }
        webSocketClient!!.connect()
    }

    private fun resetWebSocketClient() {
        webSocketClient?.close()
        webSocketClient = null
        shutdownScheduler()
    }

    private fun shutdownScheduler() {
        scheduler?.shutdown()
        scheduler = null
    }

    private fun attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            Log.d("WebSocketService", "Attempting to reconnect... ($reconnectAttempts/$maxReconnectAttempts)")
            scheduler = Executors.newScheduledThreadPool(1)
            scheduler?.schedule({
                startWebSocket()
            }, reconnectDelay, TimeUnit.MILLISECONDS)
        } else {
            Log.d("WebSocketService", "Max reconnection attempts reached. Giving up.")
        }
    }

    //override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    //    startWebSocket()
    //    return START_STICKY
    //}

    override fun onDestroy() {
        super.onDestroy()
        resetWebSocketClient()
        unregisterReceiver(networkReceiver)
    }

    private fun handleIncomingMessage(message: String) {
        Log.d("Message", message)
        try {
            val jsonObject = JSONObject(message)
            if (jsonObject.getInt("type") == 2) {
                val notification = jsonObject.getJSONObject("notification")
                val textEntry = notification.getString("text_entry")
                val localName = notification.getString("local_name")
                showNotification(localName, textEntry)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun showNotification(title: String, text: String) {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Use high priority to make sure it's shown even if the app is in the background
            .setAutoCancel(true)  // Auto-cancel the notification when clicked
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // Ensure sound/vibration is triggered

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun registerNetworkReceiver() {
        networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isNetworkAvailable() && (webSocketClient == null || !webSocketClient!!.isOpen)) {
                    resetWebSocketClient()
                    startWebSocket()
                }
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }

    companion object {
        const val ACTION_WEBSOCKET_MESSAGE = "com.example.background_notifications.WEBSOCKET_MESSAGE"
        const val EXTRA_MESSAGE = "message"
    }
}