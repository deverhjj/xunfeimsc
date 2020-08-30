package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** XunfeimscPlugin */
public class XunfeimscPlugin implements FlutterPlugin,
        ActivityAware, MethodCallHandler, EventChannel.StreamHandler
{
  private static final String TAG = XunfeimscPlugin.class.getSimpleName();

  private static final String CHANNEL_METHOD = "com.huajianjiang.flutter.plugins/xunfeimsc";
  private static final String CHANNEL_EVENT_SPEECH_RECOGNITION = "com.huajianjiang.flutter.plugins/speech_recognition";
  private static final String CMD_SPEECH_RECOGNITION_START = "startSpeechRecognition";
  private static final String CMD_SPEECH_RECOGNITION_STOP = "stopSpeechRecognition";
  private static final String CMD_SPEECH_RECOGNITION_CANCEL= "cancelSpeechRecognition";

  private Application application;

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodChannel;
  private EventChannel eventChannel;

  private Lifecycle lifecycle;
  private ActivityLifecycleObserver activityLifecycleObserver;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine");
    onAttachedToEngine(flutterPluginBinding.getApplicationContext(),
            flutterPluginBinding.getBinaryMessenger());
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    Log.d(TAG, "registerWith");
    final XunfeimscPlugin plugin = new XunfeimscPlugin();
    plugin.onAttachedToEngine(registrar.context(), registrar.messenger());
    ((Application) registrar.context().getApplicationContext())
            .registerActivityLifecycleCallbacks(plugin.activityLifecycleObserver);
  }

  private void onAttachedToEngine(Context context, BinaryMessenger messenger) {
    Log.d(TAG, "onAttachedToEngine internal");
    application = (Application) context;
    activityLifecycleObserver = new ActivityLifecycleObserver();
    methodChannel = new MethodChannel(messenger, CHANNEL_METHOD);
    methodChannel.setMethodCallHandler(this);
    eventChannel = new EventChannel(messenger, CHANNEL_EVENT_SPEECH_RECOGNITION);
    eventChannel.setStreamHandler(this);
    Initializer.initSDK(context);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    Log.d(TAG, "onDetachedFromEngine");
    methodChannel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
    Initializer.destroy();
    application.unregisterActivityLifecycleCallbacks(activityLifecycleObserver);
    application = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d(TAG, "onMethodCall");
    Object args = call.arguments();
    switch (call.method) {
      case CMD_SPEECH_RECOGNITION_START:
        result.success("CMD_SPEECH_RECOGNITION_START: " + args.toString());
        break;
      case CMD_SPEECH_RECOGNITION_STOP:
        result.success("CMD_SPEECH_RECOGNITION_STOP");
        break;
      case CMD_SPEECH_RECOGNITION_CANCEL:
        result.success("CMD_SPEECH_RECOGNITION_CANCEL");
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  // event stream
  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    Log.d(TAG, "onListen");
  }

  @Override
  public void onCancel(Object arguments) {
    Log.d(TAG, "onCancel");
  }

  // activity aware
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    Log.d(TAG, "onAttachedToActivity");
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
    lifecycle.addObserver(activityLifecycleObserver);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    Log.d(TAG, "onDetachedFromActivityForConfigChanges");
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    Log.d(TAG, "onReattachedToActivityForConfigChanges");
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    Log.d(TAG, "onDetachedFromActivity");
    lifecycle.removeObserver(activityLifecycleObserver);
    lifecycle = null;
  }


  // activity lifecycle
  private static class ActivityLifecycleObserver
          implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks
  {

    // 新版 flutter sdk lifecycle

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onCreate");
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onStart");
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onResume");
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onPause");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onStop");
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
      Log.d(TAG, "ActivityLifecycleObserver: onDestroy");
    }

    // 兼容老版本 flutter sdk

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      Log.d(TAG, "ActivityLifecycleObserver: onActivityCreated");
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver: onActivityDestroyed");
    }

  }

}
