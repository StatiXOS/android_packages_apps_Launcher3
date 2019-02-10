package com.google.android.libraries.gsa.launcherclient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.google.android.libraries.launcherclient.ILauncherOverlay;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;
import java.lang.ref.WeakReference;

public class LauncherClient {

    public static int apiVersion = -1;

    protected ILauncherOverlay mOverlay;
    private final IScrollCallback mScrollCallback;

    public final BaseClientService mBaseService;
    public final ClientService mLauncherService;

    public final EventService mEvent = new EventService("Client", 20);
    public final EventService mEventService = new EventService("Service", 10);

    public final BroadcastReceiver mInstallListener = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (VERSION.SDK_INT >= 19 || (data != null && "com.google.android.googlequicksearchbox".equals(data.getSchemeSpecificPart()))) {
                mBaseService.disconnect();
                mLauncherService.disconnect();
                LauncherClient.loadApiVersion(context);
                reconnect();
                redraw(mLayoutBundle);
            }
        }
    };

    public int mActivityState = 0;
    public int mServiceState = 0;
    public int mFlags;

    public LayoutParams mLayoutParams;
    public OverlayCallback mOverlayCallback;
    public final Activity mActivity;

    public boolean mDestroyed = false;
    private Bundle mLayoutBundle;

    public class OverlayCallback extends ILauncherOverlayCallback.Stub implements Callback {
        public LauncherClient mClient;
        private final Handler uIHandler = new Handler(Looper.getMainLooper(), this);
        public Window mWindow;
        private boolean windowHidden = false;
        public WindowManager mWindowManager;
        int mWindowShift;

        @Override
        public final void overlayScrollChanged(float f) {
            uIHandler.removeMessages(2);
            Message.obtain(uIHandler, 2, f).sendToTarget();
            if (f > 0.0f && windowHidden) {
                windowHidden = false;
            }
        }

        @Override
        public final void overlayStatusChanged(int i) {
            Message.obtain(uIHandler, 4, i, 0).sendToTarget();
        }

        @Override
        public boolean handleMessage(Message message) {
            if (mClient == null) {
                return true;
            }
            switch (message.what) {
                case 2:
                    if ((mClient.mServiceState & 1) != 0) {
                        float floatValue = (float) message.obj;
                        mClient.mScrollCallback.onOverlayScrollChanged(floatValue);
                        if (floatValue <= 0.0f) {
                            mEventService.parse(0, "onScroll 0, overlay closed", 0.0f);
                        } else if (floatValue >= 1.0f) {
                            mEventService.parse(0, "onScroll 1, overlay opened", 0.0f);
                        } else {
                            mEventService.parse(1, "onScroll", floatValue);
                        }
                    }
                    return true;
                case 3:
                    WindowManager.LayoutParams attributes = mWindow.getAttributes();
                    if ((boolean) message.obj) {
                        attributes.x = mWindowShift;
                        attributes.flags |= 512;
                    } else {
                        attributes.x = 0;
                        attributes.flags &= -513;
                    }
                    mWindowManager.updateViewLayout(mWindow.getDecorView(), attributes);
                    return true;
                case 4:
                    mClient.setServiceState(message.arg1);
                    mEventService.parse("stateChanged", message.arg1);
                    if (mClient.mScrollCallback instanceof LauncherClientCallbacks) {
                        ((LauncherClientCallbacks) mClient.mScrollCallback).setPersistentFlags(message.arg1);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    public LauncherClient(Activity activity, IScrollCallback IScrollCallback, ClientOptions clientOptions) {
        mActivity = activity;
        mScrollCallback = IScrollCallback;
        mBaseService = new BaseClientService(activity, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        mFlags = clientOptions.mOptions;

        mLauncherService = ClientService.getInstance(activity);
        ClientService ClientService = mLauncherService;
        ClientService.mClient = new WeakReference<>(this);
        mOverlay = ClientService.mOverlay;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        if (VERSION.SDK_INT >= 19) {
            intentFilter.addDataSchemeSpecificPart("com.google.android.googlequicksearchbox", 0);
        }
        mActivity.registerReceiver(mInstallListener, intentFilter);

        if (apiVersion <= 0) {
            loadApiVersion(activity);
        }

        reconnect();
        if (VERSION.SDK_INT >= 19 && mActivity.getWindow() != null &&
                mActivity.getWindow().peekDecorView() != null &&
                mActivity.getWindow().peekDecorView().isAttachedToWindow()) {
            onAttachedToWindow();
        }
    }

    public final void onAttachedToWindow() {
        if (!mDestroyed) {
            mEvent.parse(0, "attachedToWindow", 0.0f);
            setParams(mActivity.getWindow().getAttributes());
        }
    }

    public final void onDetachedFromWindow() {
        if (!mDestroyed) {
            mEvent.parse(0, "detachedFromWindow", 0.0f);
            setParams(null);
        }
    }

    public final void onResume() {
        if (!mDestroyed) {
            mActivityState |= 2;
            if (!(mOverlay == null || mLayoutParams == null)) {
                try {
                    if (apiVersion < 4) {
                        mOverlay.onResume();
                    } else {
                        mOverlay.setActivityState(mActivityState);
                    }
                } catch (RemoteException ignored) {
                }
            }
            mEvent.parse("stateChanged ", mFlags);
        }
    }

    public final void onPause() {
        if (!mDestroyed) {
            mActivityState &= -3;
            if (!(mOverlay == null || mLayoutParams == null)) {
                try {
                    if (apiVersion < 4) {
                        mOverlay.onPause();
                    } else {
                        mOverlay.setActivityState(mActivityState);
                    }
                } catch (RemoteException ignored) {
                }
            }
            mEvent.parse("stateChanged ", mFlags);
        }
    }

    public final void onStart() {
        if (!mDestroyed) {
            mLauncherService.setStopped(false);
            reconnect();
            mActivityState |= 1;
            if (!(mOverlay == null || mLayoutParams == null)) {
                try {
                    mOverlay.setActivityState(mActivityState);
                } catch (RemoteException ignored) {
                }
            }
            mEvent.parse("stateChanged ", mFlags);
        }
    }

    public final void onStop() {
        if (!mDestroyed) {
            mLauncherService.setStopped(true);
            mBaseService.disconnect();
            mActivityState &= -2;
            if (!(mOverlay == null || mLayoutParams == null)) {
                try {
                    mOverlay.setActivityState(mActivityState);
                } catch (RemoteException ignored) {
                }
            }
            mEvent.parse("stateChanged ", mFlags);
        }
    }

    public final void reconnect() {
        if (!mDestroyed) {
            if (!(mLauncherService.connect() && mBaseService.connect())) {
                mActivity.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        setServiceState(0);
                    }
                });
            }
        }
    }

    public final Activity getActivity() {
        return mActivity;
    }

    public final BaseClientService getBaseService(){
        return mBaseService;
    }

    public final ClientService getClientService(){
        return mLauncherService;
    }

    public final EventService getEventInfo(){
        return mEvent;
    }

    public boolean isDestroyed(){
        return mDestroyed;
    }

    public final void setDestroyed(boolean destroyed) {
        mDestroyed = destroyed;
    }

    public OverlayCallback getOverlayCallback(){
        return mOverlayCallback;
    }

    public final void setOverlayCallback(OverlayCallback overlayCallback){
        mOverlayCallback = overlayCallback;
    }

    public final LayoutParams getParams(){
        return mLayoutParams;
    }

    public final void setParams(LayoutParams layoutParams) {
        if (mLayoutParams != layoutParams) {
            mLayoutParams = layoutParams;
            if (mLayoutParams != null) {
                updateConfiguration();
                return;
            }
            if (mOverlay != null) {
                try {
                    mOverlay.windowDetached(mActivity.isChangingConfigurations());
                } catch (RemoteException ignored) {
                }
                mOverlay = null;
            }
        }
    }

    public final void updateConfiguration() {
        if (mOverlay != null) {
            try {
                if (mOverlayCallback == null) {
                    mOverlayCallback = new OverlayCallback();
                }
                OverlayCallback overlayCallback = mOverlayCallback;
                overlayCallback.mClient = this;
                overlayCallback.mWindowManager = mActivity.getWindowManager();
                Point point = new Point();
                overlayCallback.mWindowManager.getDefaultDisplay().getRealSize(point);
                overlayCallback.mWindowShift = -Math.max(point.x, point.y);
                overlayCallback.mWindow = mActivity.getWindow();
                if (apiVersion < 3) {
                    mOverlay.windowAttached(mLayoutParams, mOverlayCallback, mFlags);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("layout_params", mLayoutParams);
                    bundle.putParcelable("configuration", mActivity.getResources().getConfiguration());
                    bundle.putInt("client_options", mFlags);
                    if (mLayoutBundle != null) {
                        bundle.putAll(mLayoutBundle);
                    }
                    mOverlay.windowAttached2(bundle, mOverlayCallback);
                }
                if (apiVersion >= 4) {
                    mOverlay.setActivityState(mActivityState);
                } else if ((mActivityState & 2) != 0) {
                    mOverlay.onResume();
                } else {
                    mOverlay.onPause();
                }
            } catch (RemoteException ignored) {
            }
        }
    }

    public final boolean isConnected() {
        return mOverlay != null;
    }

    public final void startMove() {
        mEvent.parse(0, "startMove", 0.0f);
        if (isConnected()) {
            try {
                mOverlay.startScroll();
            } catch (RemoteException ignored) {
            }
        }
    }

    public final void endMove() {
        mEvent.parse(0, "endMove", 0.0f);
        if (isConnected()) {
            try {
                mOverlay.endScroll();
            } catch (RemoteException ignored) {
            }
        }
    }

    public final void updateMove(float f) {
        mEvent.parse(1, "updateMove", f);
        if (isConnected()) {
            try {
                mOverlay.onScroll(f);
            } catch (RemoteException ignored) {
            }
        }
    }

    public final void hideOverlay(boolean feedRunning) {
        mEvent.parse("hideOverlay", feedRunning);
        if (mOverlay != null) {
            try {
                mOverlay.closeOverlay(feedRunning ? 1 : 0);
            } catch (RemoteException ignored) {
            }
        }
    }

    public final void hideOverlay(int i) {
        if (i <= 0 || i > 2047) {
            throw new IllegalArgumentException("Invalid duration");
        }
        int i2 = 1 | (i << 2);
        mEvent.parse("hideOverlay", i);
        if (mOverlay != null) {
            try {
                mOverlay.closeOverlay(i2);
            } catch (RemoteException ignored) {
            }
        }
    }

    public final void showOverlay(boolean feedRunning) {
        mEvent.parse("showOverlay", feedRunning);
        if (mOverlay != null) {
            try {
                mOverlay.openOverlay(feedRunning ? 1 : 0);
            } catch (RemoteException ignored) {
            }
        }
    }

    public final boolean startSearch(byte[] bArr, Bundle bundle) {
        mEvent.parse(0, "startSearch", 0.0f);
        if (apiVersion >= 6 && mOverlay != null) {
            try {
                return mOverlay.startSearch(bArr, bundle);
            } catch (RemoteException e) {
                Log.e("DrawerOverlayClient", "Error starting session for search", e);
            }
        }
        return false;
    }

    public final void redraw(Bundle bundle) {
        mEvent.parse(0,
                "setPrivateOptions : " + (bundle == null ? "null" : TextUtils.join(",", bundle.keySet())),
                0.0f);
        mLayoutBundle = bundle;
        redraw();
    }

    public final void redraw() {
        if (mLayoutParams != null && apiVersion >= 7) {
            updateConfiguration();
        }
    }

    final void setOverlay(ILauncherOverlay iLauncherOverlay) {
        mEventService.parse("Connected", iLauncherOverlay != null);
        mOverlay = iLauncherOverlay;
        if (mOverlay == null) {
            setServiceState(0);
            return;
        }
        if (mLayoutParams != null) {
            updateConfiguration();
        }
    }

    private void setServiceState(int serviceState) {
        if (this.mServiceState != serviceState) {
            this.mServiceState = serviceState;
            mScrollCallback.onServiceStateChanged((serviceState & 1) != 0);
        }
    }

    static Intent getIntent(Context context) {
        String packageName = context.getPackageName();
        return new Intent("com.android.launcher3.WINDOW_OVERLAY")
                .setPackage("com.google.android.googlequicksearchbox")
                .setData(Uri.parse("app://" +
                        packageName +
                        ":" +
                        Process.myUid())
                .buildUpon()
                .appendQueryParameter("v", Integer.toString(9))
                .appendQueryParameter("cv", Integer.toString(14))
                .build());
    }

    private static void loadApiVersion(Context context) {
        ResolveInfo resolveService = context.getPackageManager().resolveService(getIntent(context), PackageManager.GET_META_DATA);
        apiVersion = resolveService == null || resolveService.serviceInfo.metaData == null ?
                1 :
                resolveService.serviceInfo.metaData.getInt("service.api.version", 1);
    }
}
