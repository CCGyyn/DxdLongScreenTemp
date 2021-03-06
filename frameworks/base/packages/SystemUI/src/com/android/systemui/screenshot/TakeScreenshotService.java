/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;
import android.app.ActivityThread;
import java.lang.reflect.Field;
import java.util.Map;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.IActivityManager;
import java.util.ArrayList;

public class TakeScreenshotService extends Service {
    private static final String TAG = "TakeScreenshotService";

    private static GlobalScreenshot mScreenshot;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Messenger callback = msg.replyTo;
            Runnable finisher = new Runnable() {
                @Override
                public void run() {
                    Message reply = Message.obtain(null, 1);
                    try {
                        callback.send(reply);
                    } catch (RemoteException e) {
                    }
                }
            };

            // If the storage for this user is locked, we have no place to store
            // the screenshot, so skip taking it instead of showing a misleading
            // animation and error notification.
            if (!getSystemService(UserManager.class).isUserUnlocked()) {
                Log.w(TAG, "Skipping screenshot because storage is locked!");
                post(finisher);
                return;
            }

            if (mScreenshot == null) {
                mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
            }

            android.util.Log.d("ccgD", "s activity search start");
            /*ActivityThread activityThread = ActivityThread.currentActivityThread();
            try {
            Field activitiesField = activityThread.getClass().getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map activities = (Map) activitiesField.get(activityThread);
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                //Field pausedField = activityRecordClass.getDeclaredField("paused");
                //pausedField.setAccessible(true);
                Field activityField = activityRecordClass.getDeclaredField("activity");
                activityField.setAccessible(true);
                Activity activity = (Activity) activityField.get(activityRecord);
                android.util.Log.d("ccgD", "s activity = " + activity.getComponentName());
            }
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            /*IActivityManager a = ActivityManager.getService();
            ArrayList<ProcessRecord> lruProcesses = (ArrayList<ProcessRecord>) a.getLruProcesses();
            for(ProcessRecord p:lruProcesses) {
                android.util.Log.d("ccgD", "s shortStringName = " + p.toShortString());
            }*/
            /*IActivityManager a = ActivityManager.getService();
            try {
                a.testProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            switch (msg.what) {
                case WindowManager.TAKE_SCREENSHOT_FULLSCREEN:
                    mScreenshot.takeScreenshot(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                case WindowManager.TAKE_SCREENSHOT_SELECTED_REGION:
                    mScreenshot.takeScreenshotPartial(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                case 3:
                    mScreenshot.takeScreenshotScroll(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                default:
                    Log.d(TAG, "Invalid screenshot option: " + msg.what);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mScreenshot != null) mScreenshot.stopScreenshot();
        return true;
    }
}
