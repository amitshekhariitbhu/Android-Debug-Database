/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
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

package com.amitshekhar.utils;

import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class DatabaseFileProvider {

    private final static String DB_PASSWORD_RESOURCE = "DB_PASSWORD_{0}";

    private DatabaseFileProvider() {
        // This class in not publicly instantiable
    }

    public static HashMap<String, Pair<File, String>> getDatabaseFiles(Context context) {
        HashMap<String, Pair<File, String>> databaseFiles = new HashMap<>();
        try {
            for (String databaseName : context.databaseList()) {
                String password = getDbPasswordFromStringResources(context, databaseName);
                databaseFiles.put(databaseName, new Pair<>(context.getDatabasePath(databaseName), password));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return databaseFiles;
    }

    private static String getDbPasswordFromStringResources(Context context, String name) {
        String nameWithoutExt = name;
        if (nameWithoutExt.endsWith(".db")) {
            nameWithoutExt = nameWithoutExt.substring(0, nameWithoutExt.lastIndexOf('.'));
        }
        String resourceName = MessageFormat.format(DB_PASSWORD_RESOURCE, nameWithoutExt.toUpperCase());
        String password = "";

        int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());

        if (resourceId != 0) {
            password = context.getString(resourceId);
        }

        return password;
    }
}
