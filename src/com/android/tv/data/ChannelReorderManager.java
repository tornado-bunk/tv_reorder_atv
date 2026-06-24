package com.android.tv.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.tv.common.BaseApplication;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for storing and retrieving the custom channel order.
 */
public class ChannelReorderManager {
    private static final String PREF_NAME = "custom_channel_order";
    private static final Map<Long, String> sCache = new HashMap<>();
    private static boolean sLoaded = false;

    private static synchronized void ensureLoaded() {
        if (sLoaded) return;
        BaseApplication app = BaseApplication.getInstance();
        if (app != null) {
            SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                try {
                    long id = Long.parseLong(entry.getKey());
                    sCache.put(id, String.valueOf(entry.getValue()));
                } catch (NumberFormatException e) {
                    // Ignore malformed keys
                }
            }
        }
        sLoaded = true;
    }

    public static String getCustomDisplayNumber(long channelId, String defaultNum) {
        ensureLoaded();
        String custom = sCache.get(channelId);
        return custom != null ? custom : defaultNum;
    }

    public static void setCustomDisplayNumber(long channelId, String customNum) {
        ensureLoaded();
        if (customNum == null) {
            sCache.remove(channelId);
        } else {
            sCache.put(channelId, customNum);
        }
        
        BaseApplication app = BaseApplication.getInstance();
        if (app != null) {
            SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (customNum == null) {
                prefs.edit().remove(String.valueOf(channelId)).apply();
            } else {
                prefs.edit().putString(String.valueOf(channelId), customNum).apply();
            }
        }
    }
}
