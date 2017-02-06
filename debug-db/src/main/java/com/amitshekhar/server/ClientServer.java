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
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.amitshekhar.model.Response;
import com.amitshekhar.model.TableDataResponse;
import com.amitshekhar.model.UpdateRowResponse;
import com.amitshekhar.utils.Constants;
import com.amitshekhar.utils.DatabaseFileProvider;
import com.amitshekhar.utils.DatabaseHelper;
import com.amitshekhar.utils.PrefHelper;
import com.amitshekhar.utils.Utils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.HashMap;

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
    private HashMap<String, File> databaseFiles;

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

                TableDataResponse response;

                if (isDbOpened) {
                    String sql = "SELECT * FROM " + tableName;
                    response = DatabaseHelper.getTableData(mDatabase, sql, tableName);
                } else {
                    response = PrefHelper.getAllPrefData(mContext, tableName);
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

                String data;
                if (first.equals("select")) {
                    TableDataResponse response = DatabaseHelper.query(mDatabase, query);
                    data = mGson.toJson(response);
                } else {
                    Response response = DatabaseHelper.exec(mDatabase, query);
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
                    response = PrefHelper.getAllPrefTableName(mContext);
                    closeDatabase();
                    mSelectedDatabase = null;
                } else {
                    openDatabase(database);
                    response = DatabaseHelper.getAllTableName(mDatabase);
                    mSelectedDatabase = database;
                }

                String data = mGson.toJson(response);
                bytes = data.getBytes();
            } else if (route.startsWith("downloadDb")) {
                bytes = Utils.getDatabase(mSelectedDatabase, databaseFiles);
            } else if (route.startsWith("updateTableData")) {

                Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
                String tableName = uri.getQueryParameter("tableName");
                String updatedData = uri.getQueryParameter("updatedData");

                UpdateRowResponse response = DatabaseHelper.updateRow(mDatabase, tableName, updatedData);
                String data = mGson.toJson(response);
                bytes = data.getBytes();
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

    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }


    private void openDatabase(String database) {
        mDatabase = mContext.openOrCreateDatabase(database, 0, null);
        isDbOpened = true;
    }

    private void closeDatabase() {
        mDatabase = null;
        isDbOpened = false;
    }

    private Response getDBList() {
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

}
