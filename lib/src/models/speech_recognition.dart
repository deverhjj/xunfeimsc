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

class SpeechRecognizeVolume {
    int volume;
    Uint8List data;

    SpeechRecognizeVolume(this.volume, this.data);
}

class SpeechRecognizeData {
    SpeechRecognitionEvent event;
    dynamic data;

    SpeechRecognizeData(this.event, this.data);
}