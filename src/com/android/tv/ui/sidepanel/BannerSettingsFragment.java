/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.tv.ui.sidepanel;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.tv.R;
import java.util.ArrayList;
import java.util.List;

/** Settings fragment for channel banner auto-hide behavior. */
public class BannerSettingsFragment extends SideFragment {
    private static final String TRACKER_LABEL = "channel banner timeout";
    private static final String PREF_NAME = "tv_reorder_prefs";
    private static final String PREF_FORCE_AUTO_HIDE = "force_auto_hide";
    private static final String PREF_BANNER_DURATION_SEC = "banner_duration_sec";

    @Override
    protected String getTitle() {
        return getString(R.string.settings_channel_banner_timeout);
    }

    @Override
    public String getTrackerLabel() {
        return TRACKER_LABEL;
    }

    @Override
    protected List<Item> getItemList() {
        List<Item> items = new ArrayList<>();

        // Force auto-hide toggle
        items.add(
                new SwitchItem(
                        getString(R.string.settings_force_auto_hide),
                        getString(R.string.settings_force_auto_hide),
                        getString(R.string.settings_force_auto_hide_description)) {
                    @Override
                    protected void onUpdate() {
                        super.onUpdate();
                        setChecked(getPrefs().getBoolean(PREF_FORCE_AUTO_HIDE, false));
                    }

                    @Override
                    protected void onSelected() {
                        super.onSelected();
                        boolean checked = isChecked();
                        getPrefs().edit().putBoolean(PREF_FORCE_AUTO_HIDE, checked).apply();
                    }
                });

        // Duration options using radio buttons
        items.add(new DividerItem());

        final int[] durations = {3, 5, 8, -1}; // -1 = Never
        final int[] durationLabels = {
            R.string.banner_duration_3s,
            R.string.banner_duration_5s,
            R.string.banner_duration_8s,
            R.string.banner_duration_off
        };

        for (int i = 0; i < durations.length; i++) {
            final int duration = durations[i];
            final int labelRes = durationLabels[i];
            items.add(
                    new RadioButtonItem(getString(labelRes)) {
                        @Override
                        protected void onUpdate() {
                            super.onUpdate();
                            int current = getPrefs().getInt(PREF_BANNER_DURATION_SEC, 5);
                            setChecked(current == duration);
                        }

                        @Override
                        protected void onSelected() {
                            super.onSelected();
                            getPrefs().edit().putInt(PREF_BANNER_DURATION_SEC, duration).apply();
                            notifyDataSetChanged();
                        }
                    });
        }

        return items;
    }

    private SharedPreferences getPrefs() {
        return getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
