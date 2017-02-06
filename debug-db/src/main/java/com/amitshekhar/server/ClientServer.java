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

/**
 * Created by amitshekhar on 15/11/16.
 */


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.amitshekhar.model.Response;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.TableDataResponse.ColumnData;
import com.amitshekhar.model.UpdateRowResponse;
import com.amitshekhar.utils.Constants;
import com.amitshekhar.utils.DataType;
import com.amitshekhar.utils.DatabaseFileProvider;
import com.amitshekhar.utils.PrefUtils;
import com.amitshekhar.utils.QueryExecutor;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientServer implements Runnable {

    private static final String TAG = "SimpleWebServer";

    /**
     * The port number we listen to
     */
    private final int mPort;

    /**
     * {@link AssetManager} for loading files to serve.
     */
    private final AssetManager mAssets;

    /**
     * True if the server is running.
     */
    private boolean mIsRunning;

    /**
     * The {@link ServerSocket} that we listen to.
     */
    private ServerSocket mServerSocket;


    private Context mContext;
    private SQLiteDatabase mDatabase;
    private Gson mGson;
    private boolean isDbOpened;
    HashMap<String, File> databaseFiles;

    /**
     * Hold the selected database name
     */
    private String mSelectedDatabase = null;

    /**
     * WebServer constructor.
     */
    public ClientServer(Context context, int port) {
        mPort = port;
        mAssets = context.getResources().getAssets();
        mContext = context;
        mGson = new Gson();
    }

    /**
     * This method starts the web server listening to the specified port.
     */
    public void start() {
        mIsRunning = true;
        new Thread(this).start();
    }

    /**
     * This method stops the web server
     */
    public void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(mPort);
            while (mIsRunning) {
                Socket socket = mServerSocket.accept();
                handle(socket);
                socket.close();
            }
        } catch (SocketException e) {
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.e(TAG, "Web server error.", e);
        } catch (Exception ignore) {

        }
    }

    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */
    private void handle(Socket socket) throws IOException {
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

            if (route.startsWith("getAllDataFromTheTable")) {
                String tableName = null;

                if (route.contains("?tableName=")) {
                    tableName = route.substring(route.indexOf("=") + 1, route.length());
                }

                TableDataResponse response = null;

                if (isDbOpened) {
                    String sql = "SELECT * FROM " + tableName;
                    response = QueryExecutor.getTableData(mDatabase, sql, tableName);
                } else {
                    response = getAllPrefData(tableName);
                }

                String data = mGson.toJson(response);
                bytes = data.getBytes();

            } else if (route.startsWith("query")) {
                String query = null;
                if (route.contains("?query=")) {
                    query = route.substring(route.indexOf("=") + 1, route.length());
                }
                try {
                    query = java.net.URLDecoder.decode(query, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String first = query.split(" ")[0].toLowerCase();

                String data = "";
                if (first.equals("select")) {
                    TableDataResponse response = query(query);
                    data = mGson.toJson(response);
                } else {
                    Response response = exec(query);
                    data = mGson.toJson(response);
                }

                bytes = data.getBytes();

            } else if (route.startsWith("getDbList")) {
                Response response = getDBList();
                String data = mGson.toJson(response);
                bytes = data.getBytes();
            } else if (route.startsWith("getTableList")) {
                String database = null;
                if (route.contains("?database=")) {
                    database = route.substring(route.indexOf("=") + 1, route.length());
                }

                Response response;

                if (Constants.APP_SHARED_PREFERENCES.equals(database)) {
                    response = getAllPrefTableName();
                    closeDatabase();
                    mSelectedDatabase = null;
                } else {
                    openDatabase(database);
                    response = getAllTableName();
                    mSelectedDatabase = database;
                }

                String data = mGson.toJson(response);
                bytes = data.getBytes();
            } else if (route.startsWith("downloadDb")) {
                bytes = getDatabaseFile();
            } else if (route.startsWith("updateTableData")) {

                Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
                String tableName = uri.getQueryParameter("tableName");
                String updatedData = uri.getQueryParameter("updatedData");

                UpdateRowResponse response = QueryExecutor.updateRow(mDatabase, tableName, updatedData);
                String data = mGson.toJson(response);
                bytes = data.getBytes();
            } else {
                bytes = loadContent(route);
            }


            if (null == bytes) {
                writeServerError(output);
                return;
            }

            // Send out the content.
            output.println("HTTP/1.0 200 OK");
            output.println("Content-Type: " + detectMimeType(route));

            if (route.startsWith("downloadDb")) {
                output.println("Content-Disposition: attachment; filename=" + mSelectedDatabase);
            } else {
                output.println("Content-Length: " + bytes.length);
            }
            output.println();
            output.write(bytes);
            output.flush();
        } finally {
            if (null != output) {
                output.close();
            }
            if (null != reader) {
                reader.close();
            }
        }
    }

    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    /**
     * Loads all the content of {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return The content of the file.
     * @throws IOException
     */
    private byte[] loadContent(String fileName) throws IOException {
        InputStream input = null;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input = mAssets.open(fileName);
            byte[] buffer = new byte[1024];
            int size;
            while (-1 != (size = input.read(buffer))) {
                output.write(buffer, 0, size);
            }
            output.flush();
            return output.toByteArray();
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (null != input) {
                input.close();
            }
        }
    }

    /**
     * Detects the MIME type from the {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return A MIME type.
     */
    private String detectMimeType(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }

    private byte[] getDatabaseFile() {
        if (TextUtils.isEmpty(mSelectedDatabase)) {
            return null;
        }

        File file = databaseFiles.get(mSelectedDatabase);

        byte[] byteArray = null;
        try {
            InputStream inputStream = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[(int) file.length()];
            int bytesRead = 0;

            while ((bytesRead = inputStream.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "getDatabaseFile: ", e);
        }

        return byteArray;
    }


    private void openDatabase(String database) {
        mDatabase = mContext.openOrCreateDatabase(database, 0, null);
        isDbOpened = true;
    }

    private void closeDatabase() {
        mDatabase = null;
        isDbOpened = false;
    }

    private Response exec(String sql) {
        Response response = new Response();
        try {
            mDatabase.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
            response.isSuccessful = false;
            response.error = e.getMessage();
            return response;
        }
        response.isSuccessful = true;
        return response;
    }

    private TableDataResponse query(String sql) {
        Cursor cursor;
        try {
            cursor = mDatabase.rawQuery(sql, null);
        } catch (Exception e) {
            e.printStackTrace();
            TableDataResponse errorResponse = new TableDataResponse();
            errorResponse.isSuccessful = false;
            errorResponse.errorMessage = e.getMessage();
            return errorResponse;
        }

        if (cursor != null) {
            cursor.moveToFirst();
            TableDataResponse response = new TableDataResponse();
            response.isSuccessful = true;

            response.tableInfos = new ArrayList<>();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                TableDataResponse.TableInfo tableInfo = new TableDataResponse.TableInfo();
                tableInfo.title = cursor.getColumnName(i);
                tableInfo.isPrimary = false;

                response.tableInfos.add(tableInfo);
            }

            response.rows = new ArrayList<>();
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
                                columnData.value = cursor.getDouble(i);
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
                    response.rows.add(row);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return response;
        } else {
            TableDataResponse errorResponse = new TableDataResponse();
            errorResponse.isSuccessful = false;
            errorResponse.errorMessage = "Cursor is null";
            return errorResponse;
        }
    }

    public Response getDBList() {
        databaseFiles = DatabaseFileProvider.getDatabaseFiles(mContext);
        Response response = new Response();
        if (databaseFiles != null) {
            for (HashMap.Entry<String, File> entry : databaseFiles.entrySet()) {
                response.rows.add(entry.getKey());
            }
        }
        response.rows.add(Constants.APP_SHARED_PREFERENCES);
        response.isSuccessful = true;
        return response;
    }

    public Response getAllTableName() {
        Response response = new Response();
        Cursor c = mDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                response.rows.add(c.getString(0));
                c.moveToNext();
            }
        }
        c.close();
        response.isSuccessful = true;
        try {
            response.dbVersion = mDatabase.getVersion();
        } catch (Exception ignore) {

        }
        return response;
    }

    public Response getAllPrefTableName() {
        Response response = new Response();
        List<String> prefTags = PrefUtils.getSharedPreferenceTags(mContext);

        for (String tag : prefTags) {
            response.rows.add(tag);
        }
        response.isSuccessful = true;
        return response;
    }

    public TableDataResponse getAllPrefData(String tag) {
        TableDataResponse response = new TableDataResponse();
        response.isSuccessful = true;

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

        SharedPreferences preferences = mContext.getSharedPreferences(tag, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            List<ColumnData> row = new ArrayList<>();
            ColumnData keyColumnData = new ColumnData();
            keyColumnData.dataType = DataType.TEXT;
            keyColumnData.value = entry.getKey();

            row.add(keyColumnData);

            ColumnData valueColumnData = new ColumnData();
            valueColumnData.value = entry.getValue().toString();
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof String) {
                    valueColumnData.dataType = DataType.TEXT;
                } else if (entry.getValue() instanceof Integer) {
                    valueColumnData.dataType = DataType.INTEGER;
                } else if (entry.getValue() instanceof Long) {
                    valueColumnData.dataType = DataType.INTEGER;
                } else if (entry.getValue() instanceof Float) {
                    valueColumnData.dataType = DataType.REAL;
                } else if (entry.getValue() instanceof Boolean) {
                    valueColumnData.dataType = DataType.BOOLEAN;
                } else if (entry.getValue() instanceof Set) {
                    valueColumnData.dataType = DataType.TEXT;
                }
            } else {
                valueColumnData.dataType = DataType.TEXT;
            }
            row.add(valueColumnData);
            response.rows.add(row);
        }
        return response;
    }

}
