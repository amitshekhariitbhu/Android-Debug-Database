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

package com.amitshekhar.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.amitshekhar.model.RowDataRequest;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.TableDataResponse.TableInfo;
import com.amitshekhar.model.UpdateRowResponse;
import com.amitshekhar.model.TableDataResponse.ColumnData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amitshekhar on 04/02/17.
 */

public class QueryExecutor {

    public static final Gson gson = new Gson();

    private QueryExecutor() {
        // This class in not publicly instantiable
    }

    public static TableDataResponse getTableData(SQLiteDatabase db, String selectQuery, String tableName) {

        TableDataResponse tableData = new TableDataResponse();

        // TODO : Handle JOIN query

        if (tableName == null) {
            tableName = getTableName(selectQuery);
        }

        if (tableName != null) {
            final String pragmaQuery = "PRAGMA table_info(" + tableName + ")";
            tableData.tableInfos = getTableInfo(db, pragmaQuery);
        }

        tableData.isEditable = tableName != null && tableData.tableInfos != null;

        Cursor cursor;
        try {
            cursor = db.rawQuery(selectQuery, null);
        } catch (Exception e) {
            e.printStackTrace();
            tableData.isSuccessful = false;
            tableData.errorMessage = e.getMessage();
            return tableData;
        }

        if (cursor != null) {
            cursor.moveToFirst();
            tableData.isSuccessful = true;
            tableData.rows = new ArrayList<>();
            if (cursor.getCount() > 0) {

                do {
                    List<ColumnData> row = new ArrayList<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        ColumnData columnData = new ColumnData();
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_BLOB:
                                columnData.dataType = DataType.TEXT;
                                columnData.value = cursor.getBlob(i);
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                columnData.dataType = DataType.REAL;
                                columnData.value = cursor.getFloat(i);
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                columnData.dataType = DataType.INTEGER;
                                columnData.value = cursor.getLong(i);
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                columnData.dataType = DataType.TEXT;
                                columnData.value = cursor.getString(i);
                                break;
                            default:
                                columnData.dataType = DataType.TEXT;
                                columnData.value = cursor.getString(i);
                        }
                        row.add(columnData);
                    }
                    tableData.rows.add(row);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return tableData;
        } else {
            tableData.isSuccessful = false;
            tableData.errorMessage = "Cursor is null";
            return tableData;
        }

    }

    public static List<TableInfo> getTableInfo(SQLiteDatabase db, String pragmaQuery) {

        Cursor cursor;
        try {
            cursor = db.rawQuery(pragmaQuery, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (cursor != null) {

            List<TableInfo> tableInfoList = new ArrayList<>();

            cursor.moveToFirst();

            if (cursor.getCount() > 0) {
                do {
                    TableInfo tableInfo = new TableInfo();

                    for (int i = 0; i < cursor.getColumnCount(); i++) {

                        final String columnName = cursor.getColumnName(i);

                        switch (columnName) {
                            case Constants.PK:
                                tableInfo.isPrimary = cursor.getInt(i) == 1;
                                break;
                            case Constants.NAME:
                                tableInfo.title = cursor.getString(i);
                                break;
                            default:
                        }

                    }
                    tableInfoList.add(tableInfo);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return tableInfoList;
        }
        return null;
    }

    public static UpdateRowResponse updateRow(SQLiteDatabase db, String tableName, String data) {

        UpdateRowResponse updateRowResponse = new UpdateRowResponse();

        if (data == null) {
            updateRowResponse.isSuccessful = false;
            return updateRowResponse;
        }

        List<RowDataRequest> rowDataRequests = gson.fromJson(data, new TypeToken<List<RowDataRequest>>() {
        }.getType());

        if (rowDataRequests != null) {
            ContentValues contentValues = new ContentValues();

            String whereClause = null;
            List<String> whereArgsList = new ArrayList<>();

            for (RowDataRequest rowDataRequest : rowDataRequests) {
                if (rowDataRequest.isPrimary) {
                    if (whereClause == null) {
                        whereClause = rowDataRequest.title + "=? ";
                    } else {
                        whereClause = "and " + rowDataRequest.title + "=? ";
                    }
                    whereArgsList.add(rowDataRequest.value);
                } else {
                    switch (rowDataRequest.dataType) {
                        case DataType.INTEGER:
                            contentValues.put(rowDataRequest.title, Long.valueOf(rowDataRequest.value));
                            break;
                        case DataType.REAL:
                            contentValues.put(rowDataRequest.title, Double.valueOf(rowDataRequest.value));
                            break;
                        case DataType.TEXT:
                            contentValues.put(rowDataRequest.title, rowDataRequest.value);
                            break;
                        default:
                    }
                }
            }

            String[] whereArgs = new String[whereArgsList.size()];

            for (int i = 0; i < whereArgsList.size(); i++) {
                whereArgs[i] = whereArgsList.get(i);
            }

            db.update(tableName, contentValues, whereClause, whereArgs);
            updateRowResponse.isSuccessful = true;
            return updateRowResponse;

        } else {
            updateRowResponse.isSuccessful = false;
            return updateRowResponse;
        }
    }

    public static String getTableName(String selectQuery) {
        return null;
    }
}
