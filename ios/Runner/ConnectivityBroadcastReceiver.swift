import Foundation
import SystemConfiguration

class ConnectivityBroadcastReceiver {
    
    private var networkReachability: SCNetworkReachability?
    
    init() {
        startMonitoring()
    }
    
    private func startMonitoring() {
        var zeroAddress = sockaddr_in()
        zeroAddress.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        zeroAddress.sin_family = sa_family_t(AF_INET)
        
        // Aquí convertimos el `sockaddr_in` a `sockaddr`
        let address = withUnsafePointer(to: &zeroAddress) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { $0 }
        }
        
        networkReachability = SCNetworkReachabilityCreateWithAddress(nil, address)
        
        var context = SCNetworkReachabilityContext(version: 0, info: nil, retain: nil, release: nil, copyDescription: nil)
        
        SCNetworkReachabilitySetCallback(networkReachability!, { (_, flags, _) in
            let isConnected = flags.contains(.reachable)
            if isConnected {
                // Aquí puedes reiniciar el WebSocket
                WebSocketService().startWebSocket()
            }
        }, &context)
        
        SCNetworkReachabilityScheduleWithRunLoop(networkReachability!, CFRunLoopGetCurrent(), CFRunLoopMode.defaultMode.rawValue)
    }
}
