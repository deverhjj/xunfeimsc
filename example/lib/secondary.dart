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


}