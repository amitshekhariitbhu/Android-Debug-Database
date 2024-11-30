package com.amitshekhar.debug.encrypt.sqlite;

import android.content.Context;

import com.amitshekhar.sqlite.DBFactory;
import com.amitshekhar.sqlite.SQLiteDB;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

public class DebugDBEncryptFactory implements DBFactory {

    static {
        System.loadLibrary("sqlcipher");
    }

    @Override
    public SQLiteDB create(Context context, String path, String password) {
        return new DebugEncryptSQLiteDB(SQLiteDatabase.openOrCreateDatabase(path, password, null, null));
    }

    @Override
    public boolean supportEncryptedDb() {
        return true;
    }
}
