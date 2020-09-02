/// 语音识别事件类型
enum SpeechRecognitionEvent {
  /// 开始录入语音时
  onStart,
  /// 结束语音录入时
  onFinished,
  /// 录音过程中音调的变化
  onVolumeChanged,
  /// 识别结果事件
  onResult,
}