
import 'dart:async';

import 'package:flutter/services.dart';

class Xunfeimsc {
  static const MethodChannel _channel =
      const MethodChannel('com.huajianjiang.flutter.plugins/xunfeimsc');

  static const String cmd_speech_recognition_start = "startSpeechRecognition";
  static const String cmd_speech_recognition_stop = "stopSpeechRecognition";
  static const String cmd_speech_recognition_cancel = "cancelSpeechRecognition";


  static Future<String> startSpeechRecognition([dynamic args]) async {
    final String version = await _channel.invokeMethod(cmd_speech_recognition_start, args);
    return version;
  }


}
