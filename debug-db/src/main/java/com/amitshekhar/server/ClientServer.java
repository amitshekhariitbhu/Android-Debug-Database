package com.amitshekhar.server;

/**
 * Created by amitshekhar on 15/11/16.
 */


import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.amitshekhar.model.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private File mDatabaseDir;
    private Gson mGson;

    /**
     * WebServer constructor.
     */
    public ClientServer(Context context, int port) {
        mPort = port;
        mAssets = context.getResources().getAssets();
        mContext = context;
        mGson = new Gson();
        getDatabaseDir();
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
        } catch (IOException e) {
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

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output);
                return;
            }
            byte[] bytes = null;

            if (route.startsWith("getAllDataFromTable")) {
                String query = null;
                if (route.contains("?query=")) {
                    query = route.substring(route.indexOf("=") + 1, route.length());
                }
                String sql = "SELECT * FROM " + query;
                Response response = query(sql);
                String data = mGson.toJson(response);
                bytes = data.getBytes();

            } else if (route.startsWith("query")) {
                String query = null;
                if (route.contains("?query=")) {
                    query = route.substring(route.indexOf("=") + 1, route.length());
                }
                Response response = query(java.net.URLDecoder.decode(query, "UTF-8"));
                String data = mGson.toJson(response);
                bytes = data.getBytes();

            } else if (route.startsWith("dblist")) {
                Response response = getDBList();
                String data = mGson.toJson(response);
                bytes = data.getBytes();
            } else if (route.startsWith("tablelist")) {
                String database = null;
                if (route.contains("?query=")) {
                    database = route.substring(route.indexOf("=") + 1, route.length());
                }
                openDatabase(database);
                Response response = getAllTableName();
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
            output.println("Content-Length: " + bytes.length);
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

    private void getDatabaseDir() {
        File root = mContext.getFilesDir().getParentFile();
        File dbRoot = new File(root, "/databases");
        mDatabaseDir = dbRoot;
    }

    private void openDatabase(String database) {
        mDatabase = mContext.openOrCreateDatabase(database, 0, null);
    }

    public Response onOpenRequest(Map<String, String> params) {
        String name = params.get("name");
        try {
            mDatabase = mContext.openOrCreateDatabase(name, 0, null);
            Response response = new Response();
            response.isSuccessful = true;
            return response;
        } catch (SQLiteException e) {
            e.printStackTrace();
            Response response = new Response();
            response.isSuccessful = false;
            response.error = e.getMessage();
            return response;
        }
    }

    private Response exec(String sql) {
        Response response = new Response();
        try {
            mDatabase.execSQL(sql);
        } catch (SQLiteException e) {
            e.printStackTrace();
            response.isSuccessful = false;
            response.error = e.getMessage();
            return response;
        }
        response.isSuccessful = true;
        return response;
    }

    private Response query(String sql) {
        Cursor cursor;
        try {
            cursor = mDatabase.rawQuery(sql, null);
        } catch (Exception e) {
            e.printStackTrace();
            Response msg = new Response();
            msg.isSuccessful = false;
            msg.error = e.getMessage();
            return msg;
        }

        if (cursor != null) {
            cursor.moveToFirst();
            Response response = new Response();
            response.isSuccessful = true;
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String name = cursor.getColumnName(i);
                columns.add(name);
            }
            response.columns = columns;

            if (cursor.getCount() > 0) {
                do {
                    List row = new ArrayList();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_BLOB:
                                row.add(cursor.getBlob(i));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                row.add(Float.valueOf(cursor.getFloat(i)));
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                row.add(Integer.valueOf(cursor.getInt(i)));
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                row.add(cursor.getString(i));
                                break;
                            default:
                                row.add("");
                        }
                    }
                    response.rows.add(row);

                } while (cursor.moveToNext());
            }

            return response;
        } else {
            Response response = new Response();
            response.isSuccessful = false;
            response.error = "Cursor is null";
            return response;
        }
    }

    public Response getDBList() {
        Response response = new Response();
        for (String name : mDatabaseDir.list()) {
            response.rows.add(name);
        }
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
        response.isSuccessful = true;
        return response;
    }


}
