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

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.util.ObjectsCompat;

import io.flutter.Log;

public class MSCService extends Service implements WakeupController.OnWakeupListener {
    private static final String TAG = MSCService.class.getSimpleName();
    private static final int MSC_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID_MSC = BuildConfig.LIBRARY_PACKAGE_NAME + ".MSC";

    private final MSCBinder binder = new MSCBinder();

    private WakeupController wakeupController;
    private ASRController asrController;
    private ScreenStateReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        receiver = new ScreenStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        // 初始化 sdk
        Initializer.initSDK(this);
        wakeupController = new WakeupController(getApplicationContext());
        wakeupController.setListener(this);
        asrController = new ASRController(getApplicationContext());

        // 前台服务通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_MSC, "语音服务", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification notification = new NotificationCompat
                .Builder(this, CHANNEL_ID_MSC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(MSC_NOTIFICATION_ID, notification);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isScreenOn()) {
            wakeupController.cancel();
            asrController.startRecord();
        } else {
            asrController.cancel();
            wakeupController.startRecord();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG , "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG , "onDestroy");
        wakeupController.setListener(null);
        wakeupController.destroy();
        wakeupController = null;
        asrController.destroy();
        asrController = null;
        // 释放 sdk 占用的系统资源
        Initializer.destroy();
        unregisterReceiver(receiver);
        receiver = null;
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG , "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG , "onUnbind");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG , "onRebind");
        super.onRebind(intent);
    }

    private void turnScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        Log.d(TAG, "turnScreenOn: isScreenOn=" + pm.isScreenOn());
        if (!pm.isScreenOn()) {
            // 唤醒屏幕
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            wakeLock.acquire(60 * 1000); // 1 分钟唤醒超时
            // 解锁屏幕
            KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock(TAG);
            keyguardLock.disableKeyguard();
        }
    }

    @Override
    public void onWakeup() {
        Log.d(TAG, "onWakeup");
        turnScreenOn();
    }

    public ASRController getAsrController() {
        return asrController;
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "SCREEN_ON");
                // 手动唤醒情况下取消语音唤醒操作
                wakeupController.cancel();
                asrController.startRecord();
            } else if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "SCREEN_OFF");
                asrController.cancel();
                wakeupController.startRecord();
            }
        }
    }

    public class MSCBinder extends Binder {
        public MSCService getService() {
            return MSCService.this;
        }
    }

}
