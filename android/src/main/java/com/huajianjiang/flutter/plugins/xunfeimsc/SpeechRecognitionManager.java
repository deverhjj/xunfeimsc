/*
 * Copyright (C) $year Huajian Jiang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

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

import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

/**
 * <p>Author: Huajian Jiang
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class SpeechRecognitionManager {
    private static final String TAG = SpeechRecognitionManager.class.getSimpleName();
    private Context context;
    private SpeechRecognizer speechRecognizer;

    private EventChannel.EventSink eventCall;

    public SpeechRecognitionManager(Context context) {
        this.context = context;
        speechRecognizer = SpeechRecognizer.createRecognizer(context, new InitListener() {
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

    public void startRecord(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        if (speechRecognizer.isListening()) {
            resultCall.error("INVALID_STATE", "Can not start a new record when in recording.", null);
            return;
        }
        int retCode = speechRecognizer.startListening(recognizerListener);
        if (retCode != ErrorCode.SUCCESS) {
            resultCall.error("SPEECH_RECOGNITION_FAILED", "Failed to recognize speech.", retCode);
        }
    }

    public void stopRecord(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        speechRecognizer.stopListening();
    }

    public void cancel(MethodChannel.Result resultCall) {
        if (checkState(resultCall)) return;
        speechRecognizer.cancel();
    }

    public void destroy() {
        eventCall = null;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void setup() {
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
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "3000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        speechRecognizer.setParameter(SpeechConstant.ASR_PTT,  "1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        speechRecognizer.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        speechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    private String getResourcePath() {
        String commonResPath = ResourceUtil
                .generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet");
        String smsResPath = ResourceUtil
                .generateResourcePath(context, ResourceUtil.RESOURCE_TYPE.assets,
                        "iat/sms_16k.jet");
        return commonResPath + ";" + smsResPath;
    }

    private void dispatchSuccessEvent(Object event) {
        if (eventCall == null) return;
        eventCall.success(event);
    }

    private void dispatchErrorEvent(String errorCode, String errorMessage, Object errorDetails) {
        if (eventCall == null) return;
        eventCall.error(errorCode, errorMessage, errorDetails);
    }

    private RecognizerListener recognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
        }

        @Override
        public void onBeginOfSpeech() {
            dispatchSuccessEvent("onStartSpeech");
        }

        @Override
        public void onEndOfSpeech() {
            dispatchSuccessEvent("onFinishSpeech");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            Map<String, Object> result = new HashMap<>();
            result.put("result", parseRecognitionResult(recognizerResult.getResultString()));
            result.put("isLastResult", b);
            dispatchSuccessEvent(result);
        }

        @Override
        public void onError(SpeechError speechError) {
            dispatchErrorEvent("RECOGNIZE_FAILED", speechError.getErrorDescription(),
                    speechError.getErrorCode());
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
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

}
