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

package com.amitshekhar.server;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.amitshekhar.model.Response;
import com.amitshekhar.model.RowDataRequest;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.UpdateRowResponse;
import com.amitshekhar.sqlite.DebugSQLiteDB;
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

import net.sqlcipher.database.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class RequestHandler {

    private static final long MAX_FILE_BYTES = 1024;
    private final Context mContext;
    private final Gson mGson;
    private final AssetManager mAssets;
    private boolean isDbOpened;
    private SQLiteDB sqLiteDB;
    private HashMap<String, Pair<File, String>> mDatabaseFiles;
    private HashMap<String, Pair<File, String>> mCustomDatabaseFiles;
    private String mSelectedDatabase = null;
    private HashMap<String, SupportSQLiteDatabase> mRoomInMemoryDatabases = new HashMap<>();

    private final String emulatedRootDir;

    public RequestHandler(Context context) {
        mContext = context;
        mAssets = context.getResources().getAssets();
        mGson = new GsonBuilder().serializeNulls().create();

        String[] types = {Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_PODCASTS,
                Environment.DIRECTORY_RINGTONES,
                Environment.DIRECTORY_ALARMS,
                Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_DCIM};
        StringBuffer ss = new StringBuffer();
        ss.append("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (String type : types) {
                appendToFileTree(ss, context.getExternalFilesDirs(type), "getExternalFilesDirs(" + type + ")");
            }
            appendToFileTree(ss, context.getExternalCacheDirs(), "getExternalCacheDirs");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appendToFileTree(ss, context.getExternalMediaDirs(), "getExternalMediaDirs");
            appendToFileTree(ss, context.getCodeCacheDir(), "getCodeCacheDir");
            appendToFileTree(ss, context.getNoBackupFilesDir(), "getNoBackupFilesDir");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appendToFileTree(ss, context.getDataDir(), "getDataDir");
        }
        for (String type : types) {
            appendToFileTree(ss, Environment.getExternalStoragePublicDirectory(type), "Environment.getExternalStoragePublicDirectory( " + type + " )");
        }
        appendToFileTree(ss, Environment.getDataDirectory(), "Environment.getDataDirectory");
        appendToFileTree(ss, Environment.getExternalStorageDirectory(), "Environment.getExternalStorageDirectory");
        appendToFileTree(ss, Environment.getRootDirectory(), "Environment.getRootDirectory");
        appendToFileTree(ss, Environment.getDownloadCacheDirectory(), "Environment.getDownloadCacheDirectory");
        appendToFileTree(ss, context.getFilesDir(), "getFilesDir");
        appendToFileTree(ss, context.getCacheDir(), "getCacheDir");
        appendToFileTree(ss, context.getExternalCacheDir(), "getExternalCacheDir");
        appendToFileTree(ss, context.getObbDir(), "getObbDir");
        ss.append("</ul>");
        emulatedRootDir = ss.toString();
    }

    public void handle(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            String route = null;
            String body = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    break;
                } else if (line.startsWith("POST /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);

                    Integer contentLength = null;
                    while (!TextUtils.isEmpty(line = reader.readLine())) {
                        if (line.toUpperCase().startsWith("CONTENT-LENGTH")) {
                            int clStart = line.indexOf(":") + 1;
                            String cl = line.substring(clStart);
                            contentLength = Integer.parseInt(cl.trim());
                        }
                        if (line.equals("\r\n")) {
                            break;
                        }
                    }
                    if (contentLength != null) {
                        char[] content = new char[contentLength];
                        if (reader.read(content) == contentLength) {
                            body = String.valueOf(content);
                        }
                    } else {
                        // TODO: read till the end if necesary.
                        body = reader.readLine();
                    }
                    break;
                }
            }

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            if (route == null || route.isEmpty()) {
                route = "index.html";
            }

            byte[] bytes;

            if (route.startsWith("fileTree")) {
                final String response = getFileListResponse(route, body);
                bytes = response.getBytes();
            } else if (route.startsWith("getFileData")) {
                bytes = getFileContent(route);
            } else if (route.startsWith("getDbList")) {
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
        } catch (Exception e) {
            e.printStackTrace();
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
            SQLiteDatabase.loadLibs(mContext);
            sqLiteDB = new DebugSQLiteDB(SQLiteDatabase.openOrCreateDatabase(databaseFile.getAbsolutePath(), password, null));
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

    private byte[] getFileContent(String route) {
        String query = null;
        if (route.contains("?fileName=")) {
            query = route.substring(route.indexOf("=") + 1, route.length());
        }
        try {
            query = URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (query == null) {
            return new byte[0];
        }
        File f = new File(query);
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        long readed = 0;
        try {
            int bufSize = (MAX_FILE_BYTES < 4096) ? (int) MAX_FILE_BYTES : 4096;
            byte[] buffer = new byte[bufSize];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(f);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                readed += read;
                if (readed > MAX_FILE_BYTES) {
                    break;
                }
                ous.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }
            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        if (ous != null) {
            return ous.toByteArray();
        }
        return new byte[0];
    }

    private String getFileListResponse(String route, String body) {
        String query = null;
        if (route.contains("?dir=")) {
            query = route.substring(route.indexOf("=") + 1, route.length());
        } else if (body != null && body.contains("dir=")) {
            query = body.substring(body.indexOf("=") + 1, body.length());
        }
        if (query != null) {
            try {
                query = URLDecoder.decode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (query.equals("/")) {
            return emulatedRootDir;
        }

        StringBuffer ss = new StringBuffer();
        ss.append("<ul class=\"jqueryFileTree\" style=\"display: none;\">");
        try {
            File dir = new File(query);
            if (dir.exists()) {
                File[] fileList = dir.listFiles();
                if (fileList != null) {
                    for (File entry : fileList) {
                        appendToFileTree(ss, entry);
                    }
                }

            } else {
                ss.append("Directory not found : " + query);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ss.append("Could not load directory: " + query);
        } finally {
            ss.append("</ul>");
        }
        return ss.toString();
    }

    private void appendToFileTree(StringBuffer ss, File[] fs, String extra) {
        if (fs == null) {
            return;
        }
        for (File entry : fs) {
            appendToFileTree(ss, entry, extra);
        }
    }

    private void appendToFileTree(StringBuffer ss, File entry) {
        appendToFileTree(ss, entry, null);

    }

    private void appendToFileTree(StringBuffer ss, File entry, String rootDirExtra) {
        String ff = entry.getAbsolutePath();
        String f;
        if (entry.isDirectory()) {
            // An empty root directory.
            if (rootDirExtra != null && (entry.list() == null || entry.list().length == 0)) {
                return;
            }
            ss.append("<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + ff + "/\">");
            f = entry.getName();
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(ff);
            ss.append("<li class=\"file ext_" + extension + "\"><a href=\"#\" rel=\"" + ff + "\">");
            f = entry.getName() + " ( " + entry.length() + " ) ";
        }
        if (rootDirExtra != null) {
            ss.append(rootDirExtra + " ");
        }
        ss.append(f + "</a></li>");
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

        if(mSelectedDatabase == null || !mDatabaseFiles.containsKey(mSelectedDatabase)){
            response.isSuccessful = false;
            return mGson.toJson(response);
        }

        try {
            closeDatabase();

            File dbFile = mDatabaseFiles.get(mSelectedDatabase).first;
            response.isSuccessful = dbFile.delete();

            if(response.isSuccessful){
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
