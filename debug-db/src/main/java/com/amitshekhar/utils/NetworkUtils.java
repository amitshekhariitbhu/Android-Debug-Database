package com.amitshekhar.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Created by amitshekhar on 15/11/16.
 */

public final class NetworkUtils {

    private NetworkUtils() {
        // This class in not publicly instantiable
    }

    public static String getIpAccess(Context context, int port) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":" + port + "/index.html";
    }

}
