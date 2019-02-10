package com.google.android.libraries.gsa.launcherclient;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventService {
    private final Event[] events;
    private int agR = 0;
    private final String mName;

    public EventService(String name, int bufferSize) {
        mName = name;
        events = new Event[bufferSize];
    }

    public final void parse(String name, int i) {
        parse(2, name, (float) i);
    }

    public final void parse(String name, boolean z) {
        parse(z ? 3 : 4, name, 0.0f);
    }

    public final void parse(int type, String name, float f) {
        int length = ((agR + events.length) - 1) % events.length;
        int length2 = ((agR + events.length) - 2) % events.length;
        if (EventService.isSameEvent(events[length], type, name) && EventService.isSameEvent(events[length2], type, name)) {
            events[length].create(type, name, f);
            Event event = events[length2];
            event.mOccurrences++;
            return;
        }
        if (events[agR] == null) {
            events[agR] = new Event();
        }
        events[agR].create(type, name, f);
        agR = (agR + 1) % events.length;
    }

    public final void dump(String str, PrintWriter printWriter) {
        printWriter.println(str + mName + " event history:");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("  HH:mm:ss.SSSZ  ", Locale.US);
        Date date = new Date();
        for (int i = 0; i < events.length; i++) {
            Event event = events[(((agR + events.length) - i) - 1) % events.length];
            if (event != null) {
                date.setTime(event.mTime);
                String msg = str + simpleDateFormat.format(date) + event.mName;
                switch (event.mType) {
                    case 1:
                        msg += ": " + event.mFlags;
                        break;
                    case 2:
                        msg += ": " + (int) event.mFlags;
                        break;
                    case 3:
                        msg += ": true";
                        break;
                    case 4:
                        msg += ": false";
                        break;
                }
                if (event.mOccurrences > 0) {
                    msg += " & " + event.mOccurrences + " similar events";
                }
                printWriter.println(msg);
            }
        }
    }

    private static boolean isSameEvent(Event event, int type, String name) {
        return event != null && event.mType == type && event.mName.equals(name);
    }
}
