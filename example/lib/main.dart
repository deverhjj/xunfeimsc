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
  BuildContext _context;

  StateSetter _stateSetter;
  SpeechRecognizeVolume _volume;
  bool _dialogIsShow = false;

  @override
  void initState() {
    super.initState();
    _xunfeimsc = Xunfeimsc();
    listenSpeechRecognition();
  }

  void showErrorBar(error) {
    String errorMessage = error.toString();
    if (error is PlatformException) {
      errorMessage = '${error.code}: ${error.message}';
    }
    Scaffold.of(_context).showSnackBar(SnackBar(content: Text('$errorMessage')));
  }

  void listenSpeechRecognition() {
    _streamSubscription =
        _xunfeimsc.onSpeechRecognitionStateChanged().listen((state) {
          if (!mounted) return;

          String recognizedResult = '';

          switch (state.event) {
            case SpeechRecognitionEvent.onStart:
              // showSpeechRecognitionDialog();
              break;
            case SpeechRecognitionEvent.onVolumeChanged:
              if (_stateSetter != null) {
                _stateSetter.call(() => _volume = state.data);
              }
              break;
            case SpeechRecognitionEvent.onResult:
              recognizedResult = state.data.result;
              setState(() {
                _recognizedText = recognizedResult;
                // _recognizedText = '$_recognizedText$recognizedResult';
              });
              break;
            case SpeechRecognitionEvent.onFinished:

              // Navigator.of(_context).pop();
              break;
          }

        }, onError: (error) {
          showErrorBar(error);
        });
  }

  void showSpeechRecognitionDialog() {
    if (_dialogIsShow) return;
    _dialogIsShow = true;
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
                        Text('音调: ${_volume?.volume ?? ''}')
                      ],
                    ));
              },
            ),
          );
        }).whenComplete(() {
      stopSpeechRecognition();
      _dialogIsShow = false;
    });
  }

  void startSpeechRecognition() {
    setState(() {
      _recognizedText = '';
      _xunfeimsc.startSpeechRecognition().catchError((error) {
        showErrorBar(error);
      });
    });
  }

  void stopSpeechRecognition() {
    _xunfeimsc.stopSpeechRecognition().catchError((error) {
      showErrorBar(error);
    });
  }

  void cancelSpeechRecognition() {
    _xunfeimsc.cancelSpeechRecognition().catchError((error) {
      showErrorBar(error);
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        home: Scaffold(
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
            body: Builder(builder: (context) {
              _context = context;
              return Column(
                children: <Widget>[
                  Expanded(
                      child: Container(
                        padding: EdgeInsets.all(64),
                        child: Text(
                          _recognizedText,
                          style: TextStyle(fontSize: 24),
                        ),
                      )),
                  Padding(
                    padding: EdgeInsets.all(64),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
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
              );
            })));
  }

  void dispose() {
    _streamSubscription.cancel();
    super.dispose();
  }

}
