package com.sample;

import android.app.Application;

import com.amitshekhar.DebugDB;

/**
 * Created by amitshekhar on 15/11/16.
 */

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DebugDB.initialize(getApplicationContext());
    }
}
