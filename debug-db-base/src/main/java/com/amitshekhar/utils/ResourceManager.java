package com.amitshekhar.utils;

import android.content.Context;

public class ResourceManager {

    public static String getResourceString(Context context, String resourceName) {
        int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
        if (resourceId != 0) {
            return context.getString(resourceId);
        } else {
            return null;
        }
    }

}