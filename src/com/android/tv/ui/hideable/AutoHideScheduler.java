/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tv.ui.hideable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import com.android.tv.common.WeakHandler;

/**
 * Schedules a view element to be hidden after a delay.
 *
 * <p>When accessibility is turned on elements are not automatically hidden.
 *
 * <p>Users of this class must pass it to {@link
 * AccessibilityManager#addAccessibilityStateChangeListener(AccessibilityStateChangeListener)} and
 * {@link
 * AccessibilityManager#removeAccessibilityStateChangeListener(AccessibilityStateChangeListener)}
 * during the appropriate live cycle event, or handle calling {@link
 * #onAccessibilityStateChanged(boolean)}.
 */
@UiThread
public final class AutoHideScheduler implements AccessibilityStateChangeListener {
    private static final String PREF_NAME = "tv_reorder_prefs";
    private static final String PREF_FORCE_AUTO_HIDE = "force_auto_hide";
    private static final String PREF_BANNER_DURATION_SEC = "banner_duration_sec";
    private static final int DEFAULT_BANNER_DURATION_SEC = 5;

    private static final int MSG_HIDE = 1;

    private final HideHandler mHandler;
    private final Runnable mRunnable;
    private final Context mContext;

    public AutoHideScheduler(Context context, Runnable runnable) {
        this(
                runnable,
                context.getSystemService(AccessibilityManager.class),
                Looper.getMainLooper(),
                context);
    }

    @VisibleForTesting
    AutoHideScheduler(Runnable runnable, AccessibilityManager accessibilityManager, Looper looper,
            Context context) {
        // Keep a reference here because HideHandler only has a weak reference to it.
        mRunnable = runnable;
        mContext = context;
        mHandler = new HideHandler(looper, mRunnable);
        mHandler.setAllowAutoHide(!accessibilityManager.isEnabled());
        boolean forceAutoHide = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_FORCE_AUTO_HIDE, false);
        mHandler.setForceAutoHide(forceAutoHide);
    }

    public void cancel() {
        mHandler.removeMessages(MSG_HIDE);
    }

    public void schedule(long delayMs) {
        cancel();
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean forceAutoHide = prefs.getBoolean(PREF_FORCE_AUTO_HIDE, false);
        mHandler.setForceAutoHide(forceAutoHide);
        if (forceAutoHide || mHandler.mAllowAutoHide) {
            // Use the custom duration from prefs if force auto-hide is enabled, otherwise use
            // the default delay passed in.
            long actualDelay = delayMs;
            if (forceAutoHide) {
                int customSec = prefs.getInt(PREF_BANNER_DURATION_SEC, DEFAULT_BANNER_DURATION_SEC);
                if (customSec <= 0) {
                    // Duration set to "Never" – do not schedule
                    return;
                }
                actualDelay = customSec * 1000L;
            }
            mHandler.sendEmptyMessageDelayed(MSG_HIDE, actualDelay);
        }
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        mHandler.onAccessibilityStateChanged(enabled, mContext);
    }

    public boolean isScheduled() {
        return mHandler.hasMessages(MSG_HIDE);
    }

    private static class HideHandler extends WeakHandler<Runnable> {

        private boolean mAllowAutoHide;
        private boolean mForceAutoHide;

        public HideHandler(Looper looper, Runnable hideRunner) {
            super(looper, hideRunner);
        }

        @Override
        protected void handleMessage(Message msg, @NonNull Runnable runnable) {
            switch (msg.what) {
                case MSG_HIDE:
                    if (mAllowAutoHide || mForceAutoHide) {
                        runnable.run();
                    }
                    break;
                default:
                    // do nothing
            }
        }

        public void setAllowAutoHide(boolean allow) {
            this.mAllowAutoHide = allow;
        }

        public void setForceAutoHide(boolean force) {
            this.mForceAutoHide = force;
        }

        public void onAccessibilityStateChanged(boolean enabled, Context context) {
            // When accessibility turns on, disable auto-hide unless the user has forced it on.
            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean forceAutoHide = prefs.getBoolean(PREF_FORCE_AUTO_HIDE, false);
            mAllowAutoHide = forceAutoHide || !enabled;
            mForceAutoHide = forceAutoHide;
        }
    }
}
