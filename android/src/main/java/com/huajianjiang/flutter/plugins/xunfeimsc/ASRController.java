package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;

/**
 * 讯飞语音离线命令词识别控制器
 */
public class ASRController {
    private static final String TAG = ASRController.class.getSimpleName();
    private Context appContext;
    private SpeechRecognizer speechRecognizer;

    private EventChannel.EventSink eventCall;

    public ASRController(Context context) {
        appContext = context.getApplicationContext();
        setup();
    }

    public void setEventCall(EventChannel.EventSink eventCall) {
        this.eventCall = eventCall;
    }

    /**
     * 开始录音并动态识别
     */
    public boolean startRecord() {
        if (speechRecognizer == null) return false;
        if (speechRecognizer.isListening()) {
            Log.e("INVALID_STATE", "Can not start a new record when in recording.");
            return false;
        }
        int retCode = speechRecognizer.startListening(recognizerListener);
        if (retCode != ErrorCode.SUCCESS) {
            Log.e("SPEECH_RECOGNITION_FAILED", "Failed to recognize speech.");
            return false;
        }
        return true;
    }

    /**
     * 停止录音并识别
     */
    public boolean stopRecord() {
        if (speechRecognizer == null) return false;
        speechRecognizer.stopListening();
        return true;
    }

    /**
     * 取消音频记录和识别操作
     */
    public boolean cancel() {
        if (speechRecognizer == null) return false;
        speechRecognizer.cancel();
        return true;
    }

    /**
     * 释放所有的资源
     */
    public void destroy() {
        eventCall = null;
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        appContext = null;
    }

    /**
     * 配置语音听写参数
     */
    private void setup() {
        speechRecognizer = SpeechRecognizer.createRecognizer(appContext, new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.e(TAG, "SpeechRecognition init failed.");
                }
            }
        });
        if (speechRecognizer != null) {
            speechRecognizer.setParameter(SpeechConstant.PARAMS, null);
            speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin");
            speechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
            speechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
            speechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
            speechRecognizer.setParameter(ResourceUtil.GRM_BUILD_PATH,
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/grm");
            speechRecognizer.setParameter(SpeechConstant.LOCAL_GRAMMAR, "openApp");
            speechRecognizer.setParameter(SpeechConstant.GRAMMAR_LIST, "openApp");
            // 设置识别的门限值
            speechRecognizer.setParameter(SpeechConstant.MIXED_THRESHOLD, "60");
            // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
            speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "5000");
            // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
            speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "1800");
            // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
            speechRecognizer.setParameter(SpeechConstant.ASR_PTT, "1");
            // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
            speechRecognizer.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            speechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/asr.wav");

            getApps();

            compileGrammar();
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

        // 更新 app 词典
        speechRecognizer.updateLexicon("app", getAppNames(), new LexiconListener() {
            @Override
            public void onLexiconUpdated(String s, SpeechError speechError) {
                if (speechError != null) {
                    Log.e(TAG, "SpeechRecognition onLexiconUpdated failed：" + speechError.getErrorDescription());
                } else {
                    Log.d(TAG, "SpeechRecognition onLexiconUpdated success：" + s);
                }
            }
        });
    }

    // 每个 app label 一行
    private String getAppNames() {
        StringBuilder names = new StringBuilder();
        for (String name : appMap.values()) {
            names.append(name).append("\n");
        }
        return names.toString();
    }

    // packageName -> label
    private Map<String, String> appMap = new HashMap<>();
    private void getApps() {
        if (!appMap.isEmpty()) return;
        PackageManager pm = appContext.getPackageManager();
        List<PackageInfo> packageInfos = pm
                .getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo pi : packageInfos) {
            CharSequence label = pm.getApplicationLabel(pi.applicationInfo);
            if (!TextUtils.isEmpty(label)) {
                appMap.put(pi.packageName, label.toString());
            }
        }
        Log.d(TAG, "getApps: " + appMap.toString());
    }

    private void oppApp(String app) {
        PackageManager pm = appContext.getPackageManager();

        String pkgName = null;
        for (Map.Entry<String, String> entry : appMap.entrySet()) {
            if (entry.getValue().equals(app)) {
                pkgName = entry.getKey();
                break;
            }
        }
        if (pkgName == null) {
            Log.e(TAG,"打开失败");
            return;
        }
        Intent intent = pm.getLaunchIntentForPackage(pkgName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } else {
            Log.e(TAG,"打开失败");
        }
    }

    // 解析离线资源文件路径
    private String getResourcePath() {
        return ResourceUtil.generateResourcePath(appContext, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet");
    }

    private String getGrmContent() {
        return Util.readFile(appContext, "open_app.bnf");
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
            if (TextUtils.isEmpty(parsedResult)) {
                parsedResult = "没有匹配结果";
            } else {
                oppApp(parsedResult);
            }
            Log.d(TAG, "onResult: " + parsedResult);
            Map<Object, Object> result = new HashMap<>();
            result.put("result", parsedResult);
            result.put("isLastResult", islast);
            dispatchSuccessEvent("onResult", result);

            if (islast) {
                cancel();
                startRecord();
            }
        }

        /**
         * 语音识别发生错误时的回调
         * @param speechError 错误类
         */
        @Override
        public void onError(SpeechError speechError) {
            dispatchErrorEvent("onSpeechError", speechError.getErrorDescription(), speechError);
            cancel();
            startRecord();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle bundle) {

        }
    };

    private static String parseRecognitionResult(String json) {
        Log.d(TAG, "parseRecognitionResult: input: " + json);
        String target = "";
        try {
            JSONObject joResult = new JSONObject(json);
            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                JSONObject wsItem = words.getJSONObject(i);
                String slot = wsItem.getString("slot");

                JSONArray items = wsItem.getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                String w = obj.getString("w");
                int sc = obj.getInt("sc");

                if (!w.contains("nomatch") && slot.equals("<app>") && sc > 0) {
                    target = w;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return target;
    }

}
