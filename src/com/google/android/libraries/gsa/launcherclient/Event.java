package com.google.android.libraries.gsa.launcherclient;

class Event {
    String mName;
    float mFlags;
    int mOccurrences;
    long mTime;
    int mType;

    public final void create(int type, String name, float flags) {
        mType = type;
        mName = name;
        mFlags = flags;
        mTime = System.currentTimeMillis();
        mOccurrences = 0;
    }
}
