package com.amitshekhar.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.Map;

public abstract class DatabaseFileProvider {

    public static final String METADATA_TAG = "DatabaseFileProvider";
    protected final Context context;

    public DatabaseFileProvider(Context context) {
        this.context = context;
    }

    public static DatabaseFileProvider fromMetadata(Context context) {
        DatabaseFileProvider databaseFileProvider = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            String className = appInfo.metaData.getString(METADATA_TAG);
            if (className != null) {
                Class<DatabaseFileProvider> databaseFileProviderClass = (Class<DatabaseFileProvider>) Class.forName(className);
                databaseFileProvider = databaseFileProviderClass.getConstructor(Context.class).newInstance(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return databaseFileProvider;
    }

    protected abstract Map<String, File> getDatabaseFiles();
}
