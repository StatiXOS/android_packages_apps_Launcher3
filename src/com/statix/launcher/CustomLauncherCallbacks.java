/*
 * Copyright (C) 2019 Paranoid Android
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
package com.statix.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.settings.SettingsActivity;

import com.google.android.libraries.gsa.launcherclient.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CustomLauncherCallbacks implements LauncherCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";

    private final CustomLauncher mLauncher;

    private OverlayCallbackImpl mOverlayCallbacks;
    private LauncherClient mLauncherClient;

    private boolean mStarted;
    private boolean mResumed;
    private boolean mAlreadyOnHome;

    public CustomLauncherCallbacks(CustomLauncher launcher) {
        mLauncher = launcher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = Utilities.getPrefs(mLauncher);
        mOverlayCallbacks = new OverlayCallbackImpl(mLauncher);
        mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks, getClientOptions(prefs));
        mOverlayCallbacks.setClient(mLauncherClient);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void onResume() {
        mResumed = true;
        if (mStarted) {
            mAlreadyOnHome = true;
        }

        mLauncherClient.onResume();
    }

    public void onStart() {
        mStarted = true;
        mLauncherClient.onStart();
    }

    public void onStop() {
        mStarted = false;
        if (!mResumed) {
            mAlreadyOnHome = false;
        }

        mLauncherClient.onStop();
    }

    public void onPause() {
        mResumed = false;
        mLauncherClient.onPause();
    }

    public void onDestroy() {
        mLauncherClient.onDestroy();

        Utilities.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSaveInstanceState(Bundle outState) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    public void onAttachedToWindow() {
        mLauncherClient.onAttachedToWindow();
    }

    public void onDetachedFromWindow() {
        mLauncherClient.onDetachedFromWindow();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {

    }

    @Override
    public void onHomeIntent(boolean internalStateHandled) {
        mLauncherClient.hideOverlay(mAlreadyOnHome);
    }

    public boolean handleBackPressed() {
        return false;
    }

    public void onTrimMemory(int level) {

    }

    public void onLauncherProviderChange() {

    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.MINUS_ONE_KEY.equals(key)) {
            mLauncherClient.setClientOptions(getClientOptions(sharedPreferences));
        }
    }

    private LauncherClient.ClientOptions getClientOptions(SharedPreferences prefs) {
        boolean hasPackage = CustomUtils.hasPackageInstalled(mLauncher, SEARCH_PACKAGE);
        boolean isEnabled = prefs.getBoolean(SettingsActivity.MINUS_ONE_KEY, true);
        return new LauncherClient.ClientOptions(hasPackage && isEnabled,
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }
}
