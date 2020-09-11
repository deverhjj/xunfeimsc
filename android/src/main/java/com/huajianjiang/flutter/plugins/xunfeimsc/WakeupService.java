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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import io.flutter.Log;

/**
 * Author: huajian.iang
 * Date: 2020/9/11
 * Email: huajian.jiang@envision-energy.com
 */
class WakeupService extends Service {
    private static final String TAG = WakeupService.class.getSimpleName();

    private WakeupController controller;
    private ScreenStateReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new WakeupController(getApplicationContext());
        receiver = new ScreenStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        controller.destroy();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "SCREEN_ON");
            } else if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "SCREEN_OFF");
            }
        }
    }

}
