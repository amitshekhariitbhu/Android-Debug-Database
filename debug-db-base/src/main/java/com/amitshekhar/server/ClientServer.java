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

/**
 * Created by amitshekhar on 15/11/16.
 */

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.amitshekhar.sqlite.DBFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ClientServer implements Runnable {
    public interface OnReadyListener {
        void onReady(int port);
    }

    private static final String TAG = "ClientServer";
    public static final int INVALID_PORT = -1;
    private static final int RANDOM_AVAILABLE_PORT = 0;
    private static final int PREFERRED_PORT = 8080;

    private int mPort = INVALID_PORT;
    private final RequestHandler mRequestHandler;
    private boolean mIsRunning;
    private ServerSocket mServerSocket;

    private final OnReadyListener listener;

    public ClientServer(Context context, DBFactory dbFactory, OnReadyListener listener) {
        mRequestHandler = new RequestHandler(context, dbFactory);
        this.listener = listener;
    }

    public int getPort() {
        return mPort;
    }

    public void start() {
        mIsRunning = true;
        new Thread(this).start();
    }

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
            try {
                mServerSocket = new ServerSocket(PREFERRED_PORT);
            }
            catch(IOException ignore) {
                mServerSocket = new ServerSocket(RANDOM_AVAILABLE_PORT);
            }

            mPort = mServerSocket.getLocalPort();
            listener.onReady(mPort);
            while (mIsRunning) {
                Socket socket = mServerSocket.accept();
                mRequestHandler.handle(socket);
                socket.close();
            }
        } catch (SocketException e) {
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.e(TAG, "Web server error.", e);
        } catch (Exception ignore) {
            Log.e(TAG, "Exception.", ignore);
        }
    }

    public void setCustomDatabaseFiles(HashMap<String, Pair<File, String>> customDatabaseFiles) {
        mRequestHandler.setCustomDatabaseFiles(customDatabaseFiles);
    }

    public void setInMemoryRoomDatabases(HashMap<String, SupportSQLiteDatabase> databases) {
        mRequestHandler.setInMemoryRoomDatabases(databases);
    }

    public boolean isRunning() {
        return mIsRunning;
    }
}
