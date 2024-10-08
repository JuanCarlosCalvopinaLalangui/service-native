import Foundation
import UIKit
import Starscream
import UserNotifications

class WebSocketService: NSObject, WebSocketDelegate {
    
    private var webSocket: WebSocket?
    private var reconnectAttempts = 0
    private let maxReconnectAttempts = 5
    private let reconnectDelay: TimeInterval = 5.0 // 5 seconds
    private var pingTimer: Timer?
    private var messageTimer: Timer?
    private var isConnected: Bool = false // Track connection status
    
    override init() {
        super.init()
        startWebSocket()
    }
    
    func startWebSocket() {
        guard let url = URL(string: "ws://192.168.1.146:8080?token=rHXXqh0sC0BrbLrP06pQZrU8W6O%2FZq66lhCeR4ZuOZk0Haa2uXAXAZ%2Fkgg6gr2ymLDtmW1LFJKDHikNj9iIK%2BpjP0YvayQfW3czB4ooql9mNn0eiB1eDZFXjgURGlQcjOkKQFY5TxZQCl4A8EYxkrGoufAojySjhhYcHV828PBnPJVe%2B0E2ftXKw5ApjfHyDSSreHCfJa0hGOyPfILAdJPIMRgF8IvwawryfWL34HQktkpjF%2Fwemz3oNs6IRY3er948vsOp6md1AWesu1Tn0AbdHsARRN3%2BkMxnK4nH5ufutSGVR1rKgBoMaDu4pNdizj1kvgYcDkxK1HrArnuUt%2Fg%3D%3D") else { return }
        var request = URLRequest(url: url)
        request.timeoutInterval = 5
        webSocket = WebSocket(request: request)
        webSocket?.delegate = self
        webSocket?.connect()
    }
    
    func stopWebSocket() {
        webSocket?.disconnect()
        webSocket = nil
        pingTimer?.invalidate()
        pingTimer = nil
        stopMessageTimer() // Invalidate the message timer
    }
    
    // MARK: - WebSocketDelegate Methods
    func didReceive(event: Starscream.WebSocketEvent, client: any Starscream.WebSocketClient) {
        switch event {
        case .connected(_):
            print("WebSocket connected")
            isConnected = true // Set connection status
            reconnectAttempts = 0
            startMessageTimer() // Start sending messages when connected
        case .disconnected(let reason, let code):
            print("WebSocket disconnected: \(reason) with code: \(code)")
            isConnected = false // Update connection status
            stopMessageTimer() // Stop sending messages on disconnect
            attemptReconnect()
        case .text(let message):
            print("Received message: \(message)")
            handleIncomingMessage(message: message)
        case .error(let error):
            print("WebSocket error: \(String(describing: error))")
            isConnected = false // Update connection status
            stopMessageTimer() // Stop sending messages on error
            attemptReconnect()
        default:
            break
        }
    }
    
    private func attemptReconnect() {
        if reconnectAttempts < maxReconnectAttempts {
            reconnectAttempts += 1
            print("Attempting to reconnect... (\(reconnectAttempts)/\(maxReconnectAttempts))")
            DispatchQueue.main.asyncAfter(deadline: .now() + reconnectDelay) {
                self.startWebSocket()
            }
        } else {
            print("Max reconnection attempts reached. Giving up.")
        }
    }
    
    private func handleIncomingMessage(message: String) {
        print("Message: \(message)")
        showNotification(title: "Nuevo Mensaje", body: message)
    }
    
    private func showNotification(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = UNNotificationSound.default
        
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
    
    // Function to send a message every 5 seconds
    @objc private func sendMessage() {
        guard isConnected else { // Check custom connection status
            return
        }

        let message = "{\"type\":0, \"last_notification\":6}"
        webSocket?.write(string: message)
        print("Sent message: \(message)")
    }
    
    private func startMessageTimer() {
        messageTimer = Timer.scheduledTimer(timeInterval: 5.0, target: self, selector: #selector(sendMessage), userInfo: nil, repeats: true)
    }

    private func stopMessageTimer() {
        messageTimer?.invalidate()  // Invalidate the message timer
        messageTimer = nil
    }
}
