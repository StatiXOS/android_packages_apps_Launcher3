package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;

import com.android.launcher3.util.ResourceBasedOverride;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to filter out components from various lists
 */
public class AppFilter implements ResourceBasedOverride {

    private final Set<ComponentName> mFilteredComponents;

    // Should not be used anymore.
    public AppFilter(Context context) {
        mFilteredComponents = Arrays.stream(
                context.getResources().getStringArray(R.array.filtered_components))
                .map(ComponentName::unflattenFromString)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieve instance of this object that can be overridden in runtime based on the build
     * variant of the application.
     */
    public static AppFilter newInstance(Context context) {
        return Overrides.getObject(AppFilter.class,
                context.getApplicationContext(), R.string.app_filter_class);
    }

    public boolean shouldShowApp(ComponentName app) {
        return !mFilteredComponents.contains(app);
    }
}
