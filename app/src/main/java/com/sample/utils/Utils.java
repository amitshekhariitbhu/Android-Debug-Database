/*
 *
 *  *    Copyright (C) 2016 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.sample.utils;

import android.content.Context;
import android.widget.Toast;

import com.sample.BuildConfig;
import com.sample.database.ExtTestDBHelper;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by amitshekhar on 07/02/17.
 */

public class Utils {

    private Utils() {
        // This class is not publicly instantiable
    }

    public static void showDebugDBAddressLogToast(Context context) {
        if (BuildConfig.DEBUG) {
            try {
                Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
                Method getAddressLog = debugDB.getMethod("getAddressLog");
                Object value = getAddressLog.invoke(null);
                Toast.makeText(context, (String) value, Toast.LENGTH_LONG).show();
            } catch (Exception ignore) {

            }
        }
    }

    public static void setCustomDatabaseFiles(Context context) {
        if (BuildConfig.DEBUG) {
            try {
                Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
                Class[] argTypes = new Class[]{HashMap.class};
                Method setCustomDatabaseFiles = debugDB.getMethod("setCustomDatabaseFiles", argTypes);
                HashMap<String, File> customDatabaseFiles = new HashMap<>();
                // set your custom database files
                customDatabaseFiles.put(ExtTestDBHelper.DATABASE_NAME,
                        new File(context.getFilesDir() + "/" + ExtTestDBHelper.DIR_NAME +
                                "/" + ExtTestDBHelper.DATABASE_NAME));
                setCustomDatabaseFiles.invoke(null, customDatabaseFiles);
            } catch (Exception ignore) {

            }
        }
    }

}
