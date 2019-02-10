package com.google.android.libraries.launcherclient;

import android.view.WindowManager.LayoutParams;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;

interface ILauncherOverlay {

    oneway void startScroll();

    oneway void onScroll(in float progress);

    oneway void endScroll();

    oneway void windowAttached(in LayoutParams lp, in ILauncherOverlayCallback cb, in int flags);

    oneway void windowDetached(in boolean isChangingConfigurations);

    oneway void closeOverlay(in int flags);

    oneway void onPause();

    oneway void onResume();

    oneway void openOverlay(in int flags);

    oneway void windowAttached2(in Bundle bundle, in ILauncherOverlayCallback cb);

    oneway void setActivityState(in int flags);

    boolean startSearch(in byte[] data, in Bundle bundle);

}