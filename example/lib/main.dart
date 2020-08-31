import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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

  @override
  void initState() {
    super.initState();
    _xunfeimsc = Xunfeimsc();
    listenSpeechRecognition();
  }

  void listenSpeechRecognition() {
    _streamSubscription =
        _xunfeimsc.onSpeechRecognitionResultAvailable().listen((event) {
          if (!mounted) return;
          setState(() {
            _recognizedText = '$_recognizedText${event.content}';
          });
        });
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
      return Scaffold(
        appBar: AppBar(
          title: const Text('讯飞 MSC Flutter 插件示例'),
          actions: <Widget>[
            IconButton(
                icon: Icon(Icons.settings),
                onPressed: () => Navigator.of(context)
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
              child: Text(_recognizedText),
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
