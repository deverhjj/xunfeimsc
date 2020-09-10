package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

/**
 * 讯飞语音离线命令词识别控制器
 */
public class WakeupController implements PluginRegistry.RequestPermissionsResultListener {
    private static final String TAG = WakeupController.class.getSimpleName();
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

    private WeakReference<Activity> activityRef;
    private SpeechRecognizer speechRecognizer;
    private VoiceWakeuper voiceWakeuper;

    private MethodChannel.Result pendingResultCall;
    private EventChannel.EventSink eventCall;

    public WakeupController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        speechRecognizer = SpeechRecognizer.createRecognizer(activity.getApplicationContext(), new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.e(TAG, "SpeechRecognition init failed.");
                }
            }
        });
        voiceWakeuper = VoiceWakeuper.createWakeuper(activity, new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.e(TAG, "VoiceWakeuper init failed.");
                }
            }
        });
        setup();
    }

    public void setEventCall(EventChannel.EventSink eventCall) {
        this.eventCall = eventCall;
    }

    private boolean checkState(MethodChannel.Result resultCall) {
        boolean isOk = speechRecognizer != null && voiceWakeuper != null;
        if (!isOk) {
            resultCall.error("INIT_FAILED", "SpeechRecognition init failed.", null);
        }
        return !isOk;
    }

    /**
     * 操作前的权限请求，如果没有被授权，自动请求权限
     * @param requestCode 请求码
     * @return 被拒 true，否则 false
     */
    @SuppressWarnings("SameParameterValue")
    private boolean requestPermissions(int requestCode) {
        Activity activity = activityRef.get();
        if (activity == null) {
            return true;
        }
        boolean permissionsDenied = !PermissionUtil
                .verifyAllPermissions(activity, REQUIRED_PERMISSIONS);
        if (permissionsDenied) {
            PermissionUtil.requestPermissions(activity, requestCode, REQUIRED_PERMISSIONS);
        }
        return permissionsDenied;
    }

    /**
     * 开始录音并动态识别
     * @param resultCall 本此调用结果回调
     */
    public void startRecord(MethodChannel.Result resultCall) {
        pendingResultCall = resultCall;
        if (checkState(resultCall)) return;
        if (requestPermissions(RQ_PRM_RECORD)) return;
        if (voiceWakeuper.isListening()) {
            resultCall.error("INVALID_STATE", "Can not start a new record when in recording.", null);
            return;
        }
        int retCode = voiceWakeuper.startListening(wakeuperListener);
        if (retCode != ErrorCode.SUCCESS) {
            resultCall.error("SPEECH_RECOGNITION_FAILED", "Failed to recognize speech.", retCode);
        } else {
            resultCall.success(null);
        }
    }

    /**
     * 停止录音并识别
     * @param resultCall 本此调用结果回调
     */
    public void stopRecord(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        voiceWakeuper.stopListening();
        resultCall.success(null);
    }

    /**
     * 取消音频记录和识别操作
     * @param resultCall 本此调用结果回调
     */
    public void cancel(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        voiceWakeuper.cancel();
        resultCall.success(null);
    }

    /**
     * 释放所有的资源
     */
    public void destroy() {
        pendingResultCall = null;
        eventCall = null;
        activityRef.clear();
        activityRef = null;
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (voiceWakeuper != null) {
            voiceWakeuper.cancel();
            voiceWakeuper.destroy();
            voiceWakeuper = null;
        }
    }

    /**
     * 配置语音听写参数
     */
    private void setup() {
        if (speechRecognizer != null) {
            speechRecognizer.setParameter(SpeechConstant.PARAMS, null);
            speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            speechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
            speechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath("asr/common.jet"));
            speechRecognizer.setParameter(ResourceUtil.GRM_BUILD_PATH,
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/grm");
        }
        if (voiceWakeuper != null) {
            voiceWakeuper.setParameter(SpeechConstant.PARAMS, null);
            voiceWakeuper.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            voiceWakeuper.setParameter(ResourceUtil.IVW_RES_PATH, getResourcePath("ivw/" + Initializer.APP_ID + ".jet"));
            voiceWakeuper.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + 1450);
            voiceWakeuper.setParameter(SpeechConstant.IVW_SST, "oneshot");
            voiceWakeuper.setParameter(SpeechConstant.RESULT_TYPE, "json");
            voiceWakeuper.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            voiceWakeuper.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            voiceWakeuper.setParameter(ResourceUtil.ASR_RES_PATH,
                    getResourcePath("asr/common.jet"));
            voiceWakeuper.setParameter(ResourceUtil.GRM_BUILD_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/grm");
            voiceWakeuper.setParameter(SpeechConstant.LOCAL_GRAMMAR, "wake");
        }
    }

    private void compileGrammar() {
        // 编译语法并写入本地存储
        speechRecognizer.buildGrammar("bnf", getGrmContent(), new GrammarListener() {
            @Override
            public void onBuildFinish(String grmId, SpeechError speechError) {
                if (speechError != null) {
                    Log.e(TAG, "SpeechRecognition buildGrammar failed：" + speechError.getErrorDescription());
                } else {
                    Log.d(TAG, "SpeechRecognition buildGrammar success：" + grmId);
                }
            }
        });
    }


    // 解析离线资源文件路径
    private String getResourcePath(String res) {
        Activity activity = activityRef.get();
        if (activity == null) {
            return null;
        }
        Context context = activity.getApplicationContext();
        return ResourceUtil.generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, res);
    }

    private String getGrmContent() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return null;
        }
        Context context = activity.getApplicationContext();
        return Util.readFile(context, "wake.bnf");
    }

    /**
     * 分发语音识别事件给 flutter app
     * @param eventCode 事件码
     * @param eventData 事件数据
     */
    private void dispatchSuccessEvent(String eventCode, Object eventData) {
        if (eventCall == null) return;
        Log.d(TAG, "dispatchSuccessEvent: " + eventCode);
        Map<Object, Object> event = new HashMap<Object, Object>();
        event.put("eventCode", eventCode);
        event.put("eventData", eventData);
        eventCall.success(event);
    }

    @SuppressWarnings("SameParameterValue")
    private void dispatchErrorEvent(String errorCode, String errorMessage, Object errorDetails) {
        if (eventCall == null) return;
        eventCall.error(errorCode, errorMessage, errorDetails);
    }

    private WakeuperListener wakeuperListener = new WakeuperListener() {

        // 唤醒结果
        @Override
        public void onResult(WakeuperResult result) {
            if (result == null) return;
            String jsonResult = result.getResultString();
            if (TextUtils.isEmpty(jsonResult)) return;
            Log.d(TAG, "onResult: " + jsonResult);
        }

        @Override
        public void onError(SpeechError error) {
            dispatchErrorEvent("onSpeechError", error.getErrorDescription(), error);
        }

        @Override
        public void onBeginOfSpeech() {
            dispatchSuccessEvent("onStartSpeech", null);
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.d(TAG, "eventType:" + eventType + "arg1:" + isLast + "arg2:" + arg2);
            // 识别结果
            if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult reslut = ((RecognizerResult) obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
                String jsonResult = reslut.getResultString();
                if (TextUtils.isEmpty(jsonResult)) return;
                Log.d(TAG, "onEvent: " + jsonResult);
            }
        }

        @Override
        public void onVolumeChanged(int volume) {
            Map<Object, Object> eventData = new HashMap<>();
            eventData.put("volume", volume);
            dispatchSuccessEvent("onVolumeChanged", eventData);
        }

    };

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 必要的权限被授予后自动继续上次的操作
        if (PermissionUtil.verifyAllPermissionRequestResult(grantResults)) {
            if (requestCode == RQ_PRM_RECORD) {
                compileGrammar();
                startRecord(pendingResultCall);
            }
        } else {
            // 权限被拒，抛出异常，flutter 层处理相关逻辑，显示友好的 UI 提示
            pendingResultCall.error("PERMISSION_DENIED", "Required permissions denied", null);
        }
        return true;
    }

}
