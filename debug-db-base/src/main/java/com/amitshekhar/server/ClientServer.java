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

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.amitshekhar.sqlite.DBFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ClientServer implements Runnable {

    private static final String TAG = "ClientServer";

    private final int mPort;
    private final RequestHandler mRequestHandler;
    private boolean mIsRunning;
    private ServerSocket mServerSocket;

    public ClientServer(Context context, int port, DBFactory dbFactory) {
        mRequestHandler = new RequestHandler(context, dbFactory);
        mPort = port;
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
            mServerSocket = new ServerSocket(mPort);
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
