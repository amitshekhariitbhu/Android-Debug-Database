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

package com.amitshekhar.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.amitshekhar.model.Response;
import com.amitshekhar.model.RowDataRequest;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.UpdateRowResponse;
import com.amitshekhar.sqlite.DBFactory;
import com.amitshekhar.sqlite.InMemoryDebugSQLiteDB;
import com.amitshekhar.sqlite.SQLiteDB;
import com.amitshekhar.utils.Constants;
import com.amitshekhar.utils.DatabaseFileProvider;
import com.amitshekhar.utils.DatabaseHelper;
import com.amitshekhar.utils.PrefHelper;
import com.amitshekhar.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class RequestHandler {

    private final Context mContext;
    private final Gson mGson;
    private final AssetManager mAssets;
    private final DBFactory mDbFactory;
    private boolean isDbOpened;
    private SQLiteDB sqLiteDB;
    private HashMap<String, Pair<File, String>> mDatabaseFiles;
    private HashMap<String, Pair<File, String>> mCustomDatabaseFiles;
    private String mSelectedDatabase = null;
    private HashMap<String, SupportSQLiteDatabase> mRoomInMemoryDatabases = new HashMap<>();

    public RequestHandler(Context context, DBFactory dbFactory) {
        mContext = context;
        mAssets = context.getResources().getAssets();
        mGson = new GsonBuilder().serializeNulls().create();
        mDbFactory = dbFactory;
    }

    public void handle(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            String route = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    break;
                }
            }

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            if (route == null || route.isEmpty()) {
                route = "index.html";
            }

            byte[] bytes;

            if (route.startsWith("getDbList")) {
                final String response = getDBListResponse();
                bytes = response.getBytes();
            } else if (route.startsWith("getAllDataFromTheTable")) {
                final String response = getAllDataFromTheTableResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("getTableList")) {
                final String response = getTableListResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("addTableData")) {
                final String response = addTableDataAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("updateTableData")) {
                final String response = updateTableDataAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("deleteTableData")) {
                final String response = deleteTableDataAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("query")) {
                final String response = executeQueryAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("deleteDb")) {
                final String response = deleteSelectedDatabaseAndGetResponse();
                bytes = response.getBytes();
            } else if (route.startsWith("downloadDb")) {
                bytes = Utils.getDatabase(mSelectedDatabase, mDatabaseFiles);
            } else {
                bytes = Utils.loadContent(route, mAssets);
            }

            if (null == bytes) {
                writeServerError(output);
                return;
            }

            // Send out the content.
            output.println("HTTP/1.0 200 OK");
            output.println("Content-Type: " + Utils.detectMimeType(route));

            if (route.startsWith("downloadDb")) {
                output.println("Content-Disposition: attachment; filename=" + mSelectedDatabase);
            } else {
                output.println("Content-Length: " + bytes.length);
            }
            output.println();
            output.write(bytes);
            output.flush();
        } finally {
            try {
                if (null != output) {
                    output.close();
                }
                if (null != reader) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setCustomDatabaseFiles(HashMap<String, Pair<File, String>> customDatabaseFiles) {
        mCustomDatabaseFiles = customDatabaseFiles;
    }

    public void setInMemoryRoomDatabases(HashMap<String, SupportSQLiteDatabase> databases) {
        mRoomInMemoryDatabases = databases;
    }

    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    private void openDatabase(String database) {
        closeDatabase();
        if (mRoomInMemoryDatabases.containsKey(database)) {
            sqLiteDB = new InMemoryDebugSQLiteDB(mRoomInMemoryDatabases.get(database));
        } else {
            File databaseFile = mDatabaseFiles.get(database).first;
            String password = mDatabaseFiles.get(database).second;
            sqLiteDB = mDbFactory.create(mContext, databaseFile.getAbsolutePath(), password);
        }
        isDbOpened = true;
    }

    private void closeDatabase() {
        if (sqLiteDB != null && sqLiteDB.isOpen()) {
            sqLiteDB.close();
        }
        sqLiteDB = null;
        isDbOpened = false;
    }

    private String getDBListResponse() {
        mDatabaseFiles = DatabaseFileProvider.getDatabaseFiles(mContext);
        if (mCustomDatabaseFiles != null) {
            mDatabaseFiles.putAll(mCustomDatabaseFiles);
        }
        Response response = new Response();
        if (mDatabaseFiles != null) {
            for (HashMap.Entry<String, Pair<File, String>> entry : mDatabaseFiles.entrySet()) {
                String[] dbEntry = {entry.getKey(), !entry.getValue().second.equals("") ? "true" : "false", "true"};
                response.rows.add(dbEntry);
            }
        }
        if (mRoomInMemoryDatabases != null) {
            for (HashMap.Entry<String, SupportSQLiteDatabase> entry : mRoomInMemoryDatabases.entrySet()) {
                String[] dbEntry = {entry.getKey(), "false", "false"};
                response.rows.add(dbEntry);
            }
        }
        response.rows.add(new String[]{Constants.APP_SHARED_PREFERENCES, "false", "false"});
        response.isSuccessful = true;
        return mGson.toJson(response);
    }

    private String getAllDataFromTheTableResponse(String route) {

        String tableName = null;

        if (route.contains("?tableName=")) {
            tableName = route.substring(route.indexOf("=") + 1, route.length());
        }

        TableDataResponse response;

        if (isDbOpened) {
            String sql = "SELECT * FROM " + tableName;
            response = DatabaseHelper.getTableData(sqLiteDB, sql, tableName);
        } else {
            response = PrefHelper.getAllPrefData(mContext, tableName);
        }

        return mGson.toJson(response);

    }

    private String executeQueryAndGetResponse(String route) {
        String query = null;
        String data = null;
        String first;
        try {
            if (route.contains("?query=")) {
                query = route.substring(route.indexOf("=") + 1, route.length());
            }
            try {
                query = URLDecoder.decode(query, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (query != null) {
                String[] statements = query.split(";");

                for (int i = 0; i < statements.length; i++) {

                    String aQuery = statements[i].trim();
                    first = aQuery.split(" ")[0].toLowerCase();
                    if (first.equals("select") || first.equals("pragma")) {
                        TableDataResponse response = DatabaseHelper.getTableData(sqLiteDB, aQuery, null);
                        data = mGson.toJson(response);
                        if (!response.isSuccessful) {
                            break;
                        }
                    } else {
                        TableDataResponse response = DatabaseHelper.exec(sqLiteDB, aQuery);
                        data = mGson.toJson(response);
                        if (!response.isSuccessful) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (data == null) {
            Response response = new Response();
            response.isSuccessful = false;
            data = mGson.toJson(response);
        }

        return data;
    }

    private String getTableListResponse(String route) {
        String database = null;
        if (route.contains("?database=")) {
            database = route.substring(route.indexOf("=") + 1, route.length());
        }

        Response response;

        if (Constants.APP_SHARED_PREFERENCES.equals(database)) {
            response = PrefHelper.getAllPrefTableName(mContext);
            closeDatabase();
            mSelectedDatabase = Constants.APP_SHARED_PREFERENCES;
        } else {
            openDatabase(database);
            response = DatabaseHelper.getAllTableName(sqLiteDB);
            mSelectedDatabase = database;
        }
        return mGson.toJson(response);
    }


    private String addTableDataAndGetResponse(String route) {
        UpdateRowResponse response;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("addData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests);
            } else {
                response = DatabaseHelper.addRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }

    private String updateTableDataAndGetResponse(String route) {
        UpdateRowResponse response;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("updatedData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests);
            } else {
                response = DatabaseHelper.updateRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }


    private String deleteTableDataAndGetResponse(String route) {
        UpdateRowResponse response;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("deleteData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.deleteRow(mContext, tableName, rowDataRequests);
            } else {
                response = DatabaseHelper.deleteRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }

    private String deleteSelectedDatabaseAndGetResponse() {
        UpdateRowResponse response = new UpdateRowResponse();

        if (mSelectedDatabase == null || !mDatabaseFiles.containsKey(mSelectedDatabase)) {
            response.isSuccessful = false;
            return mGson.toJson(response);
        }

        try {
            closeDatabase();

            File dbFile = mDatabaseFiles.get(mSelectedDatabase).first;
            response.isSuccessful = dbFile.delete();

            if (response.isSuccessful) {
                mDatabaseFiles.remove(mSelectedDatabase);
                mCustomDatabaseFiles.remove(mSelectedDatabase);
            }

            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }
}
