package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

/**
 * 讯飞语音识别控制器
 */
public class SpeechRecognitionController implements PluginRegistry.RequestPermissionsResultListener {
    private static final String TAG = SpeechRecognitionController.class.getSimpleName();
    private static final int RQ_PRM_RECORD = 0x1;
    /**
     * 语音听写必要的运行时权限
     *
     * <ul>音频记录</ul>
     * <ul>音频文件写入外部存储</ul>
     */
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private WeakReference<Activity> activityRef;
    private SpeechRecognizer speechRecognizer;

    private MethodChannel.Result pendingResultCall;
    private EventChannel.EventSink eventCall;

    public SpeechRecognitionController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        speechRecognizer = SpeechRecognizer.createRecognizer(activity.getApplicationContext(), new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.e(TAG, "SpeechRecognition init failed.");
                }
            }
        });
        setup();
    }

    public void setEventCall(EventChannel.EventSink eventCall) {
        this.eventCall = eventCall;
    }

    private boolean checkState(MethodChannel.Result resultCall) {
        boolean isOk = speechRecognizer != null;
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
        if (speechRecognizer.isListening()) {
            resultCall.error("INVALID_STATE", "Can not start a new record when in recording.", null);
            return;
        }
        int retCode = speechRecognizer.startListening(recognizerListener);
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
        speechRecognizer.stopListening();
        resultCall.success(null);
    }

    /**
     * 取消音频记录和识别操作
     * @param resultCall 本此调用结果回调
     */
    public void cancel(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        speechRecognizer.cancel();
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
    }

    /**
     * 配置语音听写参数
     */
    private void setup() {
        if (speechRecognizer == null) return;
        speechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        speechRecognizer.setParameter(SpeechConstant.ACCENT,"mandarin");
        speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        speechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "1800");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        speechRecognizer.setParameter(SpeechConstant.ASR_PTT,  "1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        speechRecognizer.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        speechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    // 解析离线资源文件路径
    private String getResourcePath() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return null;
        }
        Context context = activity.getApplicationContext();
        String commonResPath = ResourceUtil
                .generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet");
        String smsResPath = ResourceUtil
                .generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets,
                        "iat/sms_16k.jet");
        return commonResPath + ";" + smsResPath;
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

    private void dispatchErrorEvent(String errorCode, String errorMessage, Object errorDetails) {
        if (eventCall == null) return;
        eventCall.error(errorCode, errorMessage, errorDetails);
    }

    private RecognizerListener recognizerListener = new RecognizerListener() {

        /**
         * 语音音频音调发生变化时的回调
         * @param volume 音调，范围 0~30
         * @param data 音频数据
         */
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG , "onVolumeChanged: "+ volume);
            Map<Object, Object> eventData = new HashMap<>();
            eventData.put("volume", volume);
            eventData.put("data", data);
            dispatchSuccessEvent("onVolumeChanged", eventData);
        }

        /**
         * 录音开始
         */
        @Override
        public void onBeginOfSpeech() {
            Log.d(TAG , "onBeginOfSpeech");
            dispatchSuccessEvent("onStartSpeech", null);
        }

        /**
         * 录音结束
         */
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG , "onEndOfSpeech");
            dispatchSuccessEvent("onFinishSpeech", null);
        }

        /**
         * 动态语音识别后的结果，一次语音识别生命周期内可能会多次触发识别操作并返回识别结果
         * @param recognizerResult 识别结果
         * @param islast 是否是最后一次识别结果
         */
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean islast) {
            if (recognizerResult == null) return;
            String jsonResult = recognizerResult.getResultString();
            if (TextUtils.isEmpty(jsonResult)) return;
            String parsedResult = parseRecognitionResult(jsonResult);
            Log.d(TAG, "onResult: " + parsedResult);
            Map<Object, Object> result = new HashMap<>();
            result.put("result", parsedResult);
            result.put("isLastResult", islast);
            dispatchSuccessEvent("onResult", result);
        }

        /**
         * 语音识别发生错误时的回调
         * @param speechError 错误类
         */
        @Override
        public void onError(SpeechError speechError) {
            dispatchErrorEvent("onSpeechError", speechError.getErrorDescription(), speechError);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle bundle) {

        }
    };

    private static String parseRecognitionResult(String json) {
        StringBuilder ret = new StringBuilder();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);
            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 必要的权限被授予后自动继续上次的操作
        if (PermissionUtil.verifyAllPermissionRequestResult(grantResults)) {
            if (requestCode == RQ_PRM_RECORD) {
                startRecord(pendingResultCall);
            }
        } else {
            // 权限被拒，抛出异常，flutter 层处理相关逻辑，显示友好的 UI 提示
            pendingResultCall.error("PERMISSION_DENIED", "Required permissions denied", null);
        }
        return true;
    }

}
