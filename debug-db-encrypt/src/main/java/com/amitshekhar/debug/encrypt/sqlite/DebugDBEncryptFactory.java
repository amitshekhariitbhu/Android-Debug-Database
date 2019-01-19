package com.amitshekhar.debug.encrypt.sqlite;

import android.content.Context;

import com.amitshekhar.sqlite.DBFactory;
import com.amitshekhar.sqlite.SQLiteDB;

import net.sqlcipher.database.SQLiteDatabase;

public class DebugDBEncryptFactory implements DBFactory {

    @Override
    public SQLiteDB create(Context context, String path, String password) {
        SQLiteDatabase.loadLibs(context);
        return new DebugEncryptSQLiteDB(SQLiteDatabase.openOrCreateDatabase(path, password, null));
    }

}
