package com.amitshekhar.sqlite;

import android.content.Context;

public interface DBFactory {

    SQLiteDB create(Context context, String path, String password);

}
