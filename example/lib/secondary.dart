import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:xunfeimsc/xunfeimsc.dart';

import 'third.dart';

class SecondaryPage extends StatefulWidget {

  @override
  State<StatefulWidget> createState() {
    return SecondaryState();
  }

}

class SecondaryState extends State<SecondaryPage> {
  String _platformVersion = 'Unknown';

  void initState() {
    super.initState();
    init();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Secondary page'),actions: <Widget>[
        IconButton(
            icon: Icon(Icons.settings),
            onPressed: () =>
                Navigator.of(context)
                    .push(MaterialPageRoute(builder: (context) {
                  return ThirdPage();
                })))
      ],),
      body: Center(child: Text(_platformVersion)),
    );
  }

  Future init() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await Xunfeimsc.startSpeechRecognition('SecondaryPage');
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

}