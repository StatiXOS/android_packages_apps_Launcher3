package com.google.android.libraries.gsa.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class BaseClientService implements ServiceConnection {
    public boolean mConnected;
    private final Context mContext;
    private final int mFlags;

    BaseClientService(Context context, int flags) {
        this.mContext = context;
        mFlags = flags;
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    }

    public void onServiceDisconnected(ComponentName componentName) {
    }

    public final void disconnect() {
        if (mConnected) {
            mContext.unbindService(this);
            mConnected = false;
        }
    }

    public final boolean connect() {
        if (!mConnected) {
            try {
                mConnected = mContext.bindService(LauncherClient.getIntent(mContext), this, mFlags);
            } catch (SecurityException e) {
                Log.e("LauncherClient", "Unable to connect to overlay service", e);
            }
        }
        return mConnected;
    }
}
