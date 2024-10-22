package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

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
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** 讯飞语音 Flutter 插件 */
public class XunfeimscPlugin implements FlutterPlugin,
        ActivityAware, MethodCallHandler, EventChannel.StreamHandler,
        PluginRegistry.RequestPermissionsResultListener {

  private static final String TAG = XunfeimscPlugin.class.getSimpleName();
  private static final int RQ_PRM_RECORD = 0x1;
  /**
   * 语音识别必要的 6.0+ 运行时权限
   *
   * <ul>音频记录</ul>
   * <ul>音频文件写入外部存储</ul>
   */
  private static final String[] REQUIRED_PERMISSIONS = {
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

  /**
   * 接收 flutter app 相关的业务调用通道
   */
  private static final String CHANNEL_METHOD = "com.huajianjiang.flutter.plugins/xunfeimsc";
  /**
   * 语音听写事件回调通道，传递给 flutter app 语音听写生命周期内相关的事件
   */
  private static final String CHANNEL_EVENT_SPEECH_RECOGNITION = "com.huajianjiang.flutter.plugins/speech_recognition_event";
  /**
   * 开始语音听写命令
   */
  private static final String CMD_SPEECH_RECOGNITION_START = "startSpeechRecognition";
  /**
   * 停止语音听写命令
   */
  private static final String CMD_SPEECH_RECOGNITION_STOP = "stopSpeechRecognition";
  /**
   * 取消语音听写命令
   */
  private static final String CMD_SPEECH_RECOGNITION_CANCEL= "cancelSpeechRecognition";

  private Application application;

  private MethodChannel methodChannel;
  private EventChannel eventChannel;

  private ActivityPluginBinding activityPluginBinding;
  private Lifecycle lifecycle;
  private ActivityLifecycleObserver activityLifecycleObserver;

  private EventChannel.EventSink eventcall;
  private MSCService mscService;
  private boolean bound;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine");
    onAttachedToEngine(flutterPluginBinding.getApplicationContext(),
            flutterPluginBinding.getBinaryMessenger());
  }

  // 向后兼容
  public static void registerWith(Registrar registrar) {
    Log.d(TAG, "registerWith");
    final XunfeimscPlugin plugin = new XunfeimscPlugin();
    plugin.onAttachedToEngine(registrar.context(), registrar.messenger());
    // 注册 Application context 层面的 activity 生命周期监听器
//    ((Application) registrar.context().getApplicationContext())
//            .registerActivityLifecycleCallbacks(plugin.activityLifecycleObserver);
  }

  /**
   * 初始化配置
   * @param context 上下文
   * @param messenger 通道消息信使
   */
  private void onAttachedToEngine(Context context, BinaryMessenger messenger) {
    Log.d(TAG, "onAttachedToEngine internal");
    application = (Application) context.getApplicationContext();
    methodChannel = new MethodChannel(messenger, CHANNEL_METHOD);
    methodChannel.setMethodCallHandler(this);
    eventChannel = new EventChannel(messenger, CHANNEL_EVENT_SPEECH_RECOGNITION);
    eventChannel.setStreamHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    Log.d(TAG, "onDetachedFromEngine");
    eventcall = null;
    methodChannel.setMethodCallHandler(null);
    methodChannel = null;
    eventChannel.setStreamHandler(null);
    eventChannel = null;
//  application.unregisterActivityLifecycleCallbacks(activityLifecycleObserver);
    application = null;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d(TAG, "onMethodCall: " + call.method);
    switch (call.method) {
      case CMD_SPEECH_RECOGNITION_START:
//        speechRecognitionController.startRecord(result);
        break;
      case CMD_SPEECH_RECOGNITION_STOP:
//        speechRecognitionController.stopRecord(result);
        break;
      case CMD_SPEECH_RECOGNITION_CANCEL:
//        speechRecognitionController.cancel(result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  // event stream
  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    Log.d(TAG, "StreamHandler：onListen");
    eventcall = events;
    if (bound) {
        mscService.getAsrController().setEventCall(events);
    }
//    speechRecognitionController.setEventCall(events);
  }

  @Override
  public void onCancel(Object arguments) {
    Log.d(TAG, "StreamHandler：onCancel");
    if (bound) {
      mscService.getAsrController().setEventCall(null);
    }
//    speechRecognitionController.setEventCall(null);
  }

  // 插件运行时关联的 activity 生命周期回调

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    Log.d(TAG, "onAttachedToActivity");
    activityPluginBinding = binding;
//    speechRecognitionController = new WakeupController(binding.getActivity());
    binding.addRequestPermissionsResultListener(this);
    activityLifecycleObserver = new ActivityLifecycleObserver();
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding);
    lifecycle.addObserver(activityLifecycleObserver);
    // 必要的权限验证
    if (!requestPermissions(RQ_PRM_RECORD)) {
      startMSCService();
    }
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
    if (bound) {
       application.unbindService(mscServiceConnection);
       bound = false;
    }
    activityPluginBinding.removeRequestPermissionsResultListener(this);
    activityPluginBinding = null;
//    speechRecognitionController.destroy();
//    speechRecognitionController = null;
    lifecycle.removeObserver(activityLifecycleObserver);
    activityLifecycleObserver = null;
    lifecycle = null;
  }

  private void startMSCService() {
    // 启动语音服务
    Intent mscIntent = new Intent(application, MSCService.class);
    application.startService(mscIntent);
    application.bindService(mscIntent, mscServiceConnection, Service.BIND_AUTO_CREATE);
  }

  /**
   * 操作前的权限请求，如果没有被授权，自动请求权限
   * @param requestCode 请求码
   * @return 被拒 true，否则 false
   */
  @SuppressWarnings("SameParameterValue")
  private boolean requestPermissions(int requestCode) {
    Activity activity = activityPluginBinding.getActivity();
    boolean permissionsDenied = !PermissionUtil
            .verifyAllPermissions(activity, REQUIRED_PERMISSIONS);
    if (permissionsDenied) {
      PermissionUtil.requestPermissions(activity, requestCode, REQUIRED_PERMISSIONS);
    }
    return permissionsDenied;
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    // 必要的权限被授予后自动继续上次的操作
    if (requestCode == RQ_PRM_RECORD) {
      if (PermissionUtil.verifyAllPermissionRequestResult(grantResults)) {
        startMSCService();
      } else {
        // 权限被拒，抛出异常，flutter 层处理相关逻辑，显示友好的 UI 提示
        Log.e("PERMISSION_DENIED", "Required permissions denied");
      }
      return true;
    }
    return false;
  }

  private ServiceConnection mscServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(TAG, "onServiceConnected");
      MSCService.MSCBinder mscBinder = (MSCService.MSCBinder) service;
      mscService = mscBinder.getService();
      bound = true;
      if (eventcall != null) {
        mscService.getAsrController().setEventCall(eventcall);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(TAG, "onServiceDisconnected");
      bound = false;
    }
  };

  // activity 生命周期监听器
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
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityCreated");
    }

    @Override
    public void onActivityStarted(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityStarted");
    }

    @Override
    public void onActivityResumed(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityResumed");
    }

    @Override
    public void onActivityPaused(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityPaused");
    }

    @Override
    public void onActivityStopped(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityStopped");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivitySaveInstanceState");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      Log.d(TAG, "ActivityLifecycleObserver-app: onActivityDestroyed");
    }

  }

}
