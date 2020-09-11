package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

/**
 * 讯飞语音离线命令词识别控制器
 */
public class WakeupController {
    private static final String TAG = WakeupController.class.getSimpleName();
    private Context appContext;
    private VoiceWakeuper voiceWakeuper;

    private EventChannel.EventSink eventCall;

    public WakeupController(Context context) {
        appContext = context.getApplicationContext();
        init();
    }

    /**
     * 配置语音听写参数
     */
    private void init() {
        voiceWakeuper = VoiceWakeuper.createWakeuper(appContext, new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.e(TAG, "VoiceWakeuper init failed.");
                }
            }
        });
        if (voiceWakeuper != null) {
            voiceWakeuper.setParameter(SpeechConstant.PARAMS, null);
            voiceWakeuper.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            voiceWakeuper.setParameter(ResourceUtil.IVW_RES_PATH, getResourcePath("ivw/" + Initializer.APP_ID + ".jet"));
            voiceWakeuper.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + 1450);
            voiceWakeuper.setParameter(SpeechConstant.IVW_SST, "wakeup");
            voiceWakeuper.setParameter(SpeechConstant.KEEP_ALIVE, "0");
            voiceWakeuper.setParameter(SpeechConstant.RESULT_TYPE, "json");
            voiceWakeuper.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            voiceWakeuper.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        }
    }

    public void setEventCall(EventChannel.EventSink eventCall) {
        this.eventCall = eventCall;
    }

    private boolean checkState(MethodChannel.Result resultCall) {
        boolean isOk = voiceWakeuper != null;
        if (!isOk) {
            resultCall.error("INIT_FAILED", "SpeechRecognition init failed.", null);
        }
        return !isOk;
    }

    /**
     * 开始录音并动态识别
     * @param resultCall 本此调用结果回调
     */
    public void startRecord(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
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
        eventCall = null;
        if (voiceWakeuper != null) {
            voiceWakeuper.cancel();
            voiceWakeuper.destroy();
            voiceWakeuper = null;
        }
    }



    // 解析离线资源文件路径
    @SuppressWarnings("SameParameterValue")
    private String getResourcePath(String res) {
        return ResourceUtil.generateResourcePath(appContext, ResourceUtil.RESOURCE_TYPE.assets, res);
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
            Log.d(TAG, "eventType:" + eventType + ", isLast: " + isLast + ", arg2: " + arg2);
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

}
