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
import android.content.SharedPreferences;

import com.amitshekhar.model.Response;
import com.amitshekhar.model.RowDataRequest;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.UpdateRowResponse;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class PrefHelper {

    private static final String PREFS_SUFFIX = ".xml";

    private PrefHelper() {
        // This class in not publicly instantiable
    }

    public static List<String> getSharedPreferenceTags(Context context) {

        ArrayList<String> tags = new ArrayList<>();

        String rootPath = context.getApplicationInfo().dataDir + "/shared_prefs";
        File root = new File(rootPath);
        if (root.exists()) {
            for (File file : root.listFiles()) {
                String fileName = file.getName();
                if (fileName.endsWith(PREFS_SUFFIX)) {
                    tags.add(fileName.substring(0, fileName.length() - PREFS_SUFFIX.length()));
                }
            }
        }

        Collections.sort(tags);

        return tags;
    }

    public static Response getAllPrefTableName(Context context) {

        Response response = new Response();

        List<String> prefTags = getSharedPreferenceTags(context);

        for (String tag : prefTags) {
            response.rows.add(tag);
        }

        response.isSuccessful = true;

        return response;
    }

    public static TableDataResponse getAllPrefData(Context context, String tag) {

        TableDataResponse response = new TableDataResponse();
        response.isEditable = true;
        response.isSuccessful = true;
        response.isSelectQuery = true;

        TableDataResponse.TableInfo keyInfo = new TableDataResponse.TableInfo();
        keyInfo.isPrimary = true;
        keyInfo.title = "Key";

        TableDataResponse.TableInfo valueInfo = new TableDataResponse.TableInfo();
        valueInfo.isPrimary = false;
        valueInfo.title = "Value";

        response.tableInfos = new ArrayList<>();
        response.tableInfos.add(keyInfo);
        response.tableInfos.add(valueInfo);

        response.rows = new ArrayList<>();

        SharedPreferences preferences = context.getSharedPreferences(tag, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            List<TableDataResponse.ColumnData> row = new ArrayList<>();
            TableDataResponse.ColumnData keyColumnData = new TableDataResponse.ColumnData();
            keyColumnData.dataType = DataType.TEXT;
            keyColumnData.value = entry.getKey();

            row.add(keyColumnData);

            TableDataResponse.ColumnData valueColumnData = new TableDataResponse.ColumnData();
            valueColumnData.value = entry.getValue().toString();
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof String) {
                    valueColumnData.dataType = DataType.TEXT;
                } else if (entry.getValue() instanceof Integer) {
                    valueColumnData.dataType = DataType.INTEGER;
                } else if (entry.getValue() instanceof Long) {
                    valueColumnData.dataType = DataType.LONG;
                } else if (entry.getValue() instanceof Float) {
                    valueColumnData.dataType = DataType.FLOAT;
                } else if (entry.getValue() instanceof Boolean) {
                    valueColumnData.dataType = DataType.BOOLEAN;
                } else if (entry.getValue() instanceof Set) {
                    valueColumnData.dataType = DataType.STRING_SET;
                }
            } else {
                valueColumnData.dataType = DataType.TEXT;
            }
            row.add(valueColumnData);
            response.rows.add(row);
        }

        return response;

    }

    public static UpdateRowResponse addOrUpdateRow(Context context, String tableName,
                                                   List<RowDataRequest> rowDataRequests) {
        UpdateRowResponse updateRowResponse = new UpdateRowResponse();

        if (tableName == null) {
            return updateRowResponse;
        }

        RowDataRequest rowDataKey = rowDataRequests.get(0);
        RowDataRequest rowDataValue = rowDataRequests.get(1);

        String key = rowDataKey.value;
        String value = rowDataValue.value;
        String dataType = rowDataValue.dataType;

        if (Constants.NULL.equals(value)) {
            value = null;
        }

        SharedPreferences preferences = context.getSharedPreferences(tableName, Context.MODE_PRIVATE);

        try {
            switch (dataType) {
                case DataType.TEXT:
                    preferences.edit().putString(key, value).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                case DataType.INTEGER:
                    preferences.edit().putInt(key, Integer.valueOf(value)).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                case DataType.LONG:
                    preferences.edit().putLong(key, Long.valueOf(value)).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                case DataType.FLOAT:
                    preferences.edit().putFloat(key, Float.valueOf(value)).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                case DataType.BOOLEAN:
                    preferences.edit().putBoolean(key, Boolean.valueOf(value)).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                case DataType.STRING_SET:
                    JSONArray jsonArray = new JSONArray(value);
                    Set<String> stringSet = new HashSet<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        stringSet.add(jsonArray.getString(i));
                    }
                    preferences.edit().putStringSet(key, stringSet).apply();
                    updateRowResponse.isSuccessful = true;
                    break;
                default:
                    preferences.edit().putString(key, value).apply();
                    updateRowResponse.isSuccessful = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return updateRowResponse;
    }


    public static UpdateRowResponse deleteRow(Context context, String tableName,
                                              List<RowDataRequest> rowDataRequests) {
        UpdateRowResponse updateRowResponse = new UpdateRowResponse();

        if (tableName == null) {
            return updateRowResponse;
        }

        RowDataRequest rowDataKey = rowDataRequests.get(0);

        String key = rowDataKey.value;


        SharedPreferences preferences = context.getSharedPreferences(tableName, Context.MODE_PRIVATE);

        try {
            preferences.edit()
                    .remove(key).apply();
            updateRowResponse.isSuccessful = true;
        } catch (Exception ex) {
            updateRowResponse.isSuccessful = false;
        }

        return updateRowResponse;
    }
}
