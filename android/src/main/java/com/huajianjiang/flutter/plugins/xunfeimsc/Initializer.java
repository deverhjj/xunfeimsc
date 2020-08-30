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

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

/**
 * <p>Author: Huajian Jiang
 * <br>Email: developer.huajianjiang@gmail.com
 */
public class Initializer {
    public static final String APP_ID = "5f49aa2f";

    public static void initSDK(Context context) {
        String params = SpeechConstant.APPID + "=" + APP_ID + "," +
                        SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC;
        SpeechUtility.createUtility(context, params);
    }

    public static void destroy() {
        SpeechUtility speechUtility = SpeechUtility.getUtility();
        if (speechUtility != null) {
            speechUtility.destroy();
        }
    }

}
