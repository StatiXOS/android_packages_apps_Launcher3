package com.google.android.libraries.gsa.launcherclient;

public interface IScrollCallback {
    void onServiceStateChanged(boolean overlayAttached);

    void onOverlayScrollChanged(float progress);
}
