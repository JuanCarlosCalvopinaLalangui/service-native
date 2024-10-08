import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io' show Platform;


/// Displays detailed information about a SampleItem.
class SampleItemDetailsView extends StatelessWidget {
  const SampleItemDetailsView({super.key});

  // Define el MethodChannel
  static const platform = MethodChannel('com.example.flutter_application_services/websocket');


  static const routeName = '/sample_item';

// Función para iniciar el servicio desde Flutter
  Future<void> _startWebSocketService() async {
    if (Platform.isIOS) {
      try {
        await platform.invokeMethod('startWebSocket');
      } on PlatformException catch (e) {
        // ignore: avoid_print
        print("Failed to start WebSocket service: '${e.message}'.");
      }
    } else {
      try {
        await platform.invokeMethod('startWebSocketService');
      } on PlatformException catch (e) {
        // ignore: avoid_print
        print("Failed to start WebSocket service: '${e.message}'.");
      }
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text('Item Details'),
        ),
        body: Center(
          child: ElevatedButton(
            onPressed: _startWebSocketService,
            child: const Text('Start WebSocket Service'),
          ),
        ));
  }
}
