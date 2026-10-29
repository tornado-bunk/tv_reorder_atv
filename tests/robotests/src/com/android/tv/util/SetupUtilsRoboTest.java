/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.tv.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import com.android.tv.TvSingletons;
import com.android.tv.common.BaseApplication;
import com.android.tv.common.BaseSingletons;
import com.android.tv.data.ChannelDataManager;
import com.android.tv.data.api.Channel;
import com.android.tv.testing.constants.ConfigConstants;
import com.google.common.base.Optional;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = ConfigConstants.SDK, application = Application.class)
public class SetupUtilsRoboTest {
    @Test
    public void successfulScan_makesOnlyScannedInputsChannelsBrowsable() {
        Channel scanned = mock(Channel.class);
        when(scanned.getId()).thenReturn(1L);
        when(scanned.getInputId()).thenReturn("scanned-input");
        Channel other = mock(Channel.class);
        when(other.getId()).thenReturn(2L);
        when(other.getInputId()).thenReturn("other-input");

        ChannelDataManager manager = mock(ChannelDataManager.class);
        when(manager.isDbLoadFinished()).thenReturn(true);
        when(manager.getChannelList()).thenReturn(Arrays.asList(scanned, other));
        doAnswer(
                        invocation -> {
                            ((Runnable) invocation.getArguments()[0]).run();
                            return null;
                        })
                .when(manager)
                .updateChannels(any(Runnable.class));
        TvSingletons singletons = mock(TvSingletons.class);
        when(singletons.getChannelDataManager()).thenReturn(manager);

        BaseSingletons previous = BaseApplication.sSingletons;
        BaseApplication.sSingletons = singletons;
        try {
            SetupUtils setup = new SetupUtils(RuntimeEnvironment.application, Optional.absent());
            Runnable completed = mock(Runnable.class);
            setup.onTvInputSetupFinished("scanned-input", completed);

            verify(manager).updateBrowsable(1L, true, true);
            verify(manager, never()).updateBrowsable(2L, true, true);
            verify(manager).notifyChannelBrowsableChanged();
            verify(manager).applyUpdatedValuesToDb();
            verify(completed).run();
        } finally {
            BaseApplication.sSingletons = previous;
        }
    }
}
