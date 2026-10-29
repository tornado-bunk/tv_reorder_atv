/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Build;
import android.os.Handler;
import com.android.tv.common.flags.impl.DefaultLegacyFlags;
import com.android.tv.testing.constants.ConfigConstants;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link TvInputManagerHelper}.
 *
 * <p>This test is named ...RoboTest because there is already a test named <code>
 * TvInputManagerHelperTest</code>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = ConfigConstants.SDK)
public class TvInputManagerHelperRoboTest {

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void updatedInputMissingFromInitialList_isAdded() {
        assertLateInputRecovered(0);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void updatedInputInfoMissingFromInitialList_isAdded() {
        assertLateInputRecovered(1);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void stateChangedForInputMissingFromInitialList_isAdded() {
        assertLateInputRecovered(2);
    }

    private void assertLateInputRecovered(int event) {
        String inputId = "com.example/.Tuner";
        TvInputInfo input = mock(TvInputInfo.class);
        ServiceInfo service = new ServiceInfo();
        service.packageName = "com.example";
        service.applicationInfo = new ApplicationInfo();
        service.applicationInfo.packageName = service.packageName;
        service.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        when(input.getId()).thenReturn(inputId);
        when(input.getType()).thenReturn(TvInputInfo.TYPE_TUNER);
        when(input.getServiceInfo()).thenReturn(service);
        when(input.loadLabel(any())).thenReturn("Tuner");

        TvInputManagerHelper.TvInputManagerInterface manager =
                mock(TvInputManagerHelper.TvInputManagerInterface.class);
        when(manager.getTvInputList()).thenReturn(Collections.emptyList());
        when(manager.getTvContentRatingSystemList()).thenReturn(Collections.emptyList());
        when(manager.getTvInputInfo(inputId)).thenReturn(input);
        when(manager.getInputState(inputId)).thenReturn(TvInputManager.INPUT_STATE_CONNECTED);
        TvInputManagerHelper helper =
                new TvInputManagerHelper(
                        RuntimeEnvironment.application, manager, DefaultLegacyFlags.DEFAULT);
        helper.start();
        try {
            TvInputManager.TvInputCallback listener = mock(TvInputManager.TvInputCallback.class);
            helper.addCallback(listener);
            ArgumentCaptor<TvInputManager.TvInputCallback> callback =
                    ArgumentCaptor.forClass(TvInputManager.TvInputCallback.class);
            verify(manager).registerCallback(callback.capture(), any(Handler.class));
            assertThat(helper.getTvInputInfos(true, true)).isEmpty();

            if (event == 0) {
                callback.getValue().onInputUpdated(inputId);
            } else if (event == 1) {
                callback.getValue().onTvInputInfoUpdated(input);
            } else {
                callback.getValue()
                        .onInputStateChanged(inputId, TvInputManager.INPUT_STATE_CONNECTED);
            }

            assertThat(helper.getTvInputInfos(true, true)).containsExactly(input);
            verify(listener).onInputAdded(inputId);
        } finally {
            helper.stop();
        }
    }

    @Test
    public void getInputState_null() {
        TvInputInfo tvinputInfo = null;
        TvInputManagerHelper tvInputManagerHelper =
                new TvInputManagerHelper(
                        RuntimeEnvironment.application, DefaultLegacyFlags.DEFAULT);
        assertThat(TvInputManager.INPUT_STATE_DISCONNECTED)
                .isSameInstanceAs(tvInputManagerHelper.getInputState(tvinputInfo));
    }
}
