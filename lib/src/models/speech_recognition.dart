import 'dart:typed_data';

import 'package:xunfeimsc/src/enums/speech_recognition.dart';

/// 语言识别接收的数据模型
class SpeechRecognizeResult {
    /// 识别的内容
    String result;
    /// 是否是最后一次识别结果
    bool isLastResult;

    SpeechRecognizeResult(this.result, this.isLastResult);
}

/// 每次音调发生变化的相关数据
class SpeechRecognizeVolume {
    int volume;
    Uint8List data;

    SpeechRecognizeVolume(this.volume, this.data);
}

/// 语音识别事件数据类
class SpeechRecognizeData {
    /// 事件类型
    SpeechRecognitionEvent event;
    /// 事件关联的数据，例如 SpeechRecognizeResult ， SpeechRecognizeVolume
    dynamic data;

    SpeechRecognizeData(this.event, this.data);
}