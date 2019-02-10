/*
 * Copyright (C) 2017 Paranoid Android
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

package com.android.launcher3;

import com.android.launcher3.Launcher.LauncherOverlay;
import com.android.launcher3.Launcher.LauncherOverlayCallbacks;

import com.google.android.libraries.gsa.launcherclient.LauncherClient;
import com.google.android.libraries.gsa.launcherclient.LauncherClientCallbacks;

public class LauncherTab implements LauncherOverlay, LauncherClientCallbacks {
    public final static String PREF_PERSIST_FLAGS = "pref_persistent_flags";
    public static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";

    private Launcher mLauncher;
    private LauncherClient mClient;
    private LauncherOverlayCallbacks mOverlayCallbacks;
    boolean mAttached = false;
    private int mFlags;
    boolean mFlagsChanged = false;

    public LauncherTab(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setClient(LauncherClient client) {
        mClient = client;
    }

    @Override
    public void onServiceStateChanged(boolean overlayAttached) {
        if (overlayAttached != mAttached) {
            mAttached = overlayAttached;
            mLauncher.setLauncherOverlay(overlayAttached ? this : null);
        }
    }

    @Override
    public void onOverlayScrollChanged(float n) {
        if (mOverlayCallbacks != null) {
            mOverlayCallbacks.onScrollChanged(n);
        }
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        mClient.updateMove(progress);
    }

    @Override
    public void onScrollInteractionBegin() {
        mClient.startMove();
    }

    @Override
    public void onScrollInteractionEnd() {
        mClient.endMove();
    }

    @Override
    public void setOverlayCallbacks(LauncherOverlayCallbacks cb) {
        mOverlayCallbacks = cb;
    }

    @Override
    public void setPersistentFlags(int flags) {
        //flags = 8 | 16; //Always enable app drawer Google style search bar

        flags &= (8 | 16);
        if (flags != mFlags) {
            mFlagsChanged = true;
            mFlags = flags;
            Utilities.getDevicePrefs(mLauncher).edit().putInt(PREF_PERSIST_FLAGS, flags).apply();
        }
    }
}
