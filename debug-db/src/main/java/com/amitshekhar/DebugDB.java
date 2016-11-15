package com.amitshekhar;

import android.content.Context;
import android.util.Log;

import com.amitshekhar.server.ClientServer;
import com.amitshekhar.utils.NetworkUtils;

/**
 * Created by amitshekhar on 15/11/16.
 */

public class DebugDB {

    private static final String TAG = DebugDB.class.getSimpleName();
    private static final int DEFAULT_PORT = 8080;
    private static ClientServer clientServer;

    private DebugDB() {
        // This class in not publicly instantiable
    }

    public static void initialize(Context context) {
        clientServer = new ClientServer(context, DEFAULT_PORT);
        clientServer.start();
        Log.d(TAG, NetworkUtils.getIpAccess(context, DEFAULT_PORT));
    }

    public static void initialize(Context context, int port) {
        clientServer = new ClientServer(context, port);
        clientServer.start();
        Log.d(TAG, NetworkUtils.getIpAccess(context, port));
    }

    public static void shutDown() {
        if (clientServer != null) {
            clientServer.stop();
            clientServer = null;
        }
    }

}
