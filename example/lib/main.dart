import 'dart:async';

import 'package:flutter/material.dart';
import 'package:xunfeimsc/xunfeimsc.dart';
import 'package:xunfeimsc_example/secondary.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Xunfeimsc _xunfeimsc;
  StreamSubscription _streamSubscription;
  String _recognizedText = '';
  BuildContext _context;

  StateSetter _stateSetter;
  SpeechRecognizeVolume _volume;

  @override
  void initState() {
    super.initState();
    _xunfeimsc = Xunfeimsc();
    listenSpeechRecognition();
  }

  void listenSpeechRecognition() {
    _streamSubscription =
        _xunfeimsc.onSpeechRecognitionStateChanged().listen((state) {
      if (!mounted) return;

      String recognizedResult = '';

      switch (state.event) {
        case SpeechRecognitionEvent.onStart:
          showSpeechRecognitionDialog();
          break;
        case SpeechRecognitionEvent.onVolumeChanged:
          if (_stateSetter != null) {
            _stateSetter.call(() => _volume = state.data);
          }
          break;
        case SpeechRecognitionEvent.onResult:
          recognizedResult = state.data.result;
          break;
        case SpeechRecognitionEvent.onFinished:
          Navigator.of(_context).pop();
          break;
      }

      setState(() {
            _recognizedText = '$_recognizedText$recognizedResult';
          });
        });
  }

  void showSpeechRecognitionDialog() {
    showDialog(
        context: _context,
        builder: (context) {
          return AlertDialog(
            content: StatefulBuilder(
              builder: (BuildContext context, StateSetter setState) {
                _stateSetter = setState;
                return Container(
                  padding: EdgeInsets.all(16),
                    child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    CircularProgressIndicator(),
                    SizedBox(height: 16),
                    Text('音量: ${_volume?.volume ?? ''}')
                  ],
                ));
              },
            ),
          );
        }).whenComplete(() => stopSpeechRecognition());
  }

  void startSpeechRecognition() {
    setState(() {
      _recognizedText = '';
      _xunfeimsc.startSpeechRecognition();
    });
  }

  void stopSpeechRecognition() {
    _xunfeimsc.stopSpeechRecognition();
  }

  void cancelSpeechRecognition() {
    _xunfeimsc.cancelSpeechRecognition();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(home: Builder(builder: (context) {
      _context = context;
      return Scaffold(
        appBar: AppBar(
          title: const Text('讯飞 MSC Flutter 插件示例'),
          actions: <Widget>[
            IconButton(
                icon: Icon(Icons.settings),
                onPressed: () =>
                    Navigator.of(context)
                        .push(MaterialPageRoute(builder: (context) {
                      return SecondaryPage();
                    })))
          ],
        ),
        body: Column(
          children: <Widget>[
            Expanded(
                child: Container(
                  padding: EdgeInsets.all(64),
                  child: Text(_recognizedText, style: TextStyle(fontSize: 24),),
                )),
            Padding(
              padding: EdgeInsets.all(64),
              child: Row(
                children: [
                  RaisedButton(
                      child: Text('开始'),
                      onPressed: () => startSpeechRecognition()),
                  RaisedButton(
                      child: Text('停止'),
                      onPressed: () => stopSpeechRecognition()),
                  RaisedButton(
                      child: Text('取消'),
                      onPressed: () => cancelSpeechRecognition())
                ],
              ),
            )
          ],
        ),
      );
    }));
  }

  void dispose() {
    _streamSubscription.cancel();
    super.dispose();
  }

}
