
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:xunfeimsc/speech_recognition_result.dart';

class Xunfeimsc {
  static const MethodChannel _callChannel =
      const MethodChannel('com.huajianjiang.flutter.plugins/xunfeimsc');
  static const EventChannel _eventChannel = const EventChannel(
      'com.huajianjiang.flutter.plugins/speech_recognition_event');

  static const String cmd_speech_recognition_start = "startSpeechRecognition";
  static const String cmd_speech_recognition_stop = "stopSpeechRecognition";
  static const String cmd_speech_recognition_cancel = "cancelSpeechRecognition";

  Stream<Map<dynamic, dynamic>> listenSpeechRecognitionEvent() {
    return _eventChannel.receiveBroadcastStream();
  }

  Stream<SpeechRecognizeResult> onSpeechRecognitionResultAvailable() {
    return _eventChannel
        .receiveBroadcastStream()
        .where((event) => event['eventCode'] == 'onResult')
        .map((event) {
      Map<dynamic, dynamic> eventData = event['eventData'];
      return SpeechRecognizeResult(
          eventData['result'], eventData['isLastResult']);
    });
  }

  Future<void> startSpeechRecognition() async {
    return _callChannel.invokeMethod(cmd_speech_recognition_start);
  }

  Future<void> stopSpeechRecognition() async {
    return _callChannel.invokeMethod(cmd_speech_recognition_stop);
  }

  Future<void> cancelSpeechRecognition() async {
    return _callChannel.invokeMethod(cmd_speech_recognition_cancel);
  }

}
