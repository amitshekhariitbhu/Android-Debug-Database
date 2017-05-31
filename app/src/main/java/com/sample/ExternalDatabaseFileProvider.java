package com.sample;

import android.content.Context;

import com.amitshekhar.utils.DatabaseFileProvider;
import com.sample.database.ExtTestDBHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExternalDatabaseFileProvider extends DatabaseFileProvider {

    public ExternalDatabaseFileProvider(Context context) {
        super(context);
    }

    @Override
    protected Map<String, File> getDatabaseFiles() {
        Map<String, File> map = new HashMap<>();
        map.put(ExtTestDBHelper.DATABASE_NAME, new File(context.getFilesDir() + "/" + ExtTestDBHelper.DIR_NAME + "/" + ExtTestDBHelper.DATABASE_NAME));
        return map;
    }

}
