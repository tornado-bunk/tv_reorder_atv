/* SPDX-License-Identifier: Apache-2.0 */
package com.tvreorder.devicetests;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.tv.*;
import android.os.*;
import java.lang.reflect.*;
import java.util.*;

public class SmokeRunner extends Instrumentation {
    Bundle args, results = new Bundle();
    Activity activity;
    Object manager, helper, tv;
    int failures;

    public void onCreate(Bundle a) {
        args = a;
        start();
    }

    Object call(Object o, String name) throws Exception {
        return o.getClass().getMethod(name).invoke(o);
    }

    Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    void main(Runnable r) {
        runOnMainSync(r);
    }

    void check(String label, boolean value) {
        results.putString(label, value ? "PASS" : "FAIL");
        if (!value) failures++;
    }

    void shell(String s) throws Exception {
        try (ParcelFileDescriptor fd =
                        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
                                .executeShellCommand(s);
                java.io.FileInputStream in = new java.io.FileInputStream(fd.getFileDescriptor())) {
            byte[] b = new byte[4096];
            while (in.read(b) != -1) {}
        }
    }

    void snapshot(String label) throws Exception {
        Bundle state = new Bundle();
        main(
                () -> {
                    try {
                        state.putInt("channels", (Integer) call(manager, "getChannelCount"));
                        state.putInt(
                                "browsable",
                                ((List<?>) call(manager, "getBrowsableChannelList")).size());
                        state.putBoolean("video", (Boolean) call(tv, "isVideoAvailable"));
                        state.putInt("reason", (Integer) call(tv, "getVideoUnavailableReason"));
                        state.putInt("width", (Integer) call(tv, "getVideoWidth"));
                        state.putInt("height", (Integer) call(tv, "getVideoHeight"));
                        Object channel = call(tv, "getCurrentChannel");
                        if (channel != null) {
                            state.putString(
                                    "channel",
                                    call(channel, "getDisplayNumber")
                                            + " "
                                            + call(channel, "getDisplayName"));
                        }
                    } catch (Exception e) {
                        state.putString("error", e.toString());
                    }
                });
        results.putString(label, state.toString());
    }

    boolean waitVideo() throws Exception {
        for (int i = 0; i < 30; i++) {
            boolean[] available = {false};
            main(
                    () -> {
                        try {
                            available[0] = (Boolean) call(tv, "isVideoAvailable");
                        } catch (Exception e) {
                        }
                    });
            if (available[0]) return true;
            Thread.sleep(500);
        }
        return false;
    }

    public void onStart() {
        int resultCode = Activity.RESULT_CANCELED;
        try {
            Intent intent =
                    new Intent()
                            .setClassName("com.tvreorder.tv", "com.android.tv.MainActivity")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(intent);
            waitForIdleSync();
            main(
                    () -> {
                        try {
                            manager = call(activity, "getChannelDataManager");
                            helper = call(activity, "getTvInputManagerHelper");
                            tv = field(activity, "mTvView");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            check("initial_video", waitVideo());
            snapshot("initial");
            int dbCount = 0, dbBrowsable = 0;
            StringBuilder channels = new StringBuilder();
            try (Cursor c =
                    getTargetContext()
                            .getContentResolver()
                            .query(
                                    TvContract.Channels.CONTENT_URI,
                                    new String[] {
                                        "_id",
                                        "input_id",
                                        "display_number",
                                        "display_name",
                                        "browsable"
                                    },
                                    null,
                                    null,
                                    null)) {
                while (c != null && c.moveToNext()) {
                    dbCount++;
                    if (c.getInt(4) != 0) dbBrowsable++;
                    channels.append(c.getLong(0))
                            .append('|')
                            .append(c.getString(1))
                            .append('|')
                            .append(c.getString(2))
                            .append('|')
                            .append(c.getString(3))
                            .append('|')
                            .append(c.getInt(4))
                            .append('\n');
                }
            }
            results.putInt("db_channels", dbCount);
            results.putInt("db_browsable", dbBrowsable);
            if (args.getString("mode", "inspect").equals("inspect"))
                results.putString("channel_list", channels.toString());
            String mode = args.getString("mode", "inspect");
            if (mode.equals("smoke")) {
                for (int i = 0; i < 6; i++) {
                    sendKeyDownUpSync(i < 3 ? 166 : 167);
                    Thread.sleep(1000);
                    check("zap_" + i, waitVideo());
                    snapshot("zap_state_" + i);
                }
                for (int i = 0; i < 5; i++) {
                    shell("input keyevent KEYCODE_HOME");
                    Thread.sleep(500);
                    shell("am start -n com.tvreorder.tv/com.android.tv.MainActivity");
                    Thread.sleep(1000);
                    check("resume_" + i, waitVideo());
                    snapshot("resume_state_" + i);
                }
                for (int i = 0; i < 3; i++) {
                    shell("input keyevent KEYCODE_SLEEP");
                    Thread.sleep(1500);
                    shell("input keyevent KEYCODE_WAKEUP");
                    Thread.sleep(1500);
                    check("standby_" + i, waitVideo());
                    snapshot("standby_state_" + i);
                }
            }
            if (mode.equals("setup")) {
                Object[] selected = {null};
                main(
                        () -> {
                            try {
                                selected[0] = ((List<?>) call(manager, "getChannelList")).get(0);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                final long id = (Long) call(selected[0], "getId");
                final String inputId = (String) call(selected[0], "getInputId");
                android.content.SharedPreferences prefs =
                        getTargetContext().getSharedPreferences("browsable_shared_preference", 0);
                Map<String, ?> original = new HashMap<>(prefs.getAll());
                main(
                        () -> {
                            try {
                                manager.getClass()
                                        .getMethod("updateBrowsable", Long.class, boolean.class)
                                        .invoke(manager, id, false);
                                call(manager, "applyUpdatedValuesToDb");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                snapshot("hidden_before_setup");
                java.util.concurrent.CountDownLatch done =
                        new java.util.concurrent.CountDownLatch(1);
                main(
                        () -> {
                            try {
                                Object setup = call(activity.getApplication(), "getSetupUtils");
                                setup.getClass()
                                        .getMethod(
                                                "onTvInputSetupFinished",
                                                String.class,
                                                Runnable.class)
                                        .invoke(setup, inputId, (Runnable) () -> done.countDown());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                check(
                        "setup_callback_completed",
                        done.await(20, java.util.concurrent.TimeUnit.SECONDS));
                main(
                        () -> {
                            try {
                                check(
                                        "setup_restores_browsability",
                                        (Boolean) call(selected[0], "isBrowsable"));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                snapshot("after_setup");
                android.content.SharedPreferences.Editor ed = prefs.edit().clear();
                for (Map.Entry<String, ?> e : original.entrySet()) {
                    if (e.getValue() instanceof Boolean)
                        ed.putBoolean(e.getKey(), (Boolean) e.getValue());
                }
                ed.commit();
            }
            if (mode.equals("callbacks")) {
                getTargetContext()
                        .getPackageManager()
                        .setComponentEnabledSetting(
                                new ComponentName("com.tvreorder.tv", "com.android.tv.TvActivity"),
                                0,
                                android.content.pm.PackageManager.DONT_KILL_APP);
                TvInputManager system =
                        (TvInputManager)
                                getTargetContext().getSystemService(Context.TV_INPUT_SERVICE);
                String[] inputId = {null};
                main(
                        () -> {
                            try {
                                Object ch = ((List<?>) call(manager, "getChannelList")).get(0);
                                inputId[0] = (String) call(ch, "getInputId");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                TvInputInfo tuner = system.getTvInputInfo(inputId[0]);
                final TvInputInfo input = tuner;
                Object callback = field(helper, "mInternalCallback");
                int[] expectedCount = {0};
                main(
                        () -> {
                            try {
                                expectedCount[0] = (Integer) call(manager, "getChannelCount");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                for (int i = 0; i < 3; i++) {
                    final int event = i;
                    main(
                            () -> {
                                try {
                                    ((TvInputManager.TvInputCallback) callback)
                                            .onInputRemoved(input.getId());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    snapshot("removed_" + i);
                    int[] removedCount = {-1};
                    main(
                            () -> {
                                try {
                                    removedCount[0] = (Integer) call(manager, "getChannelCount");
                                } catch (Exception e) {
                                }
                            });
                    check("removed_empty_" + i, removedCount[0] == 0);
                    if (event == 2) {
                        main(
                                () -> {
                                    try {
                                        Method resume =
                                                activity.getClass()
                                                        .getDeclaredMethod("resumeTvIfNeeded");
                                        resume.setAccessible(true);
                                        resume.invoke(activity);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                        snapshot("resume_while_input_missing");
                    }
                    main(
                            () -> {
                                TvInputManager.TvInputCallback cb =
                                        (TvInputManager.TvInputCallback) callback;
                                if (event == 0) cb.onInputUpdated(input.getId());
                                else if (event == 1) cb.onTvInputInfoUpdated(input);
                                else
                                    cb.onInputStateChanged(
                                            input.getId(), TvInputManager.INPUT_STATE_CONNECTED);
                            });
                    Thread.sleep(1000);
                    check("recovery_video_" + i, waitVideo());
                    snapshot("recovered_" + i);
                    int[] n = {0};
                    main(
                            () -> {
                                try {
                                    n[0] = (Integer) call(manager, "getChannelCount");
                                } catch (Exception e) {
                                }
                            });
                    check("recovery_" + i, n[0] == expectedCount[0] && n[0] > 0);
                    shell("am start -n com.tvreorder.tv/com.android.tv.MainActivity");
                    Thread.sleep(1000);
                }
                main(
                        () -> {
                            ((TvInputManager.TvInputCallback) callback).onInputAdded(input.getId());
                            ((TvInputManager.TvInputCallback) callback).onInputAdded(input.getId());
                        });
                snapshot("repeated_input_added");
                main(
                        () -> {
                            try {
                                check(
                                        "repeated_input_added_keeps_count",
                                        (Integer) call(manager, "getChannelCount")
                                                == expectedCount[0]);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            results.putInt("failures", failures);
            resultCode = failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED;
        } catch (Throwable e) {
            results.putString("error", android.util.Log.getStackTraceString(e));
        }
        // Standby can disconnect network ADB before instrumentation reports its results.
        try (java.io.FileOutputStream out =
                getTargetContext()
                        .openFileOutput("device-test-results.txt", Context.MODE_PRIVATE)) {
            out.write(results.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            android.util.Log.e("SmokeRunner", "Cannot save test results", e);
        }
        finish(resultCode, results);
    }
}
