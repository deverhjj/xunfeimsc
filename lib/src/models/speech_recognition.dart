/*
 * Copyright (C) $year Huajian Jiang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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