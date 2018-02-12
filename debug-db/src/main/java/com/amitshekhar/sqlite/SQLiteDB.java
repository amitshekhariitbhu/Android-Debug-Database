package com.amitshekhar.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;


/**
 * Created by anandgaurav on 12/02/18.
 */

public interface SQLiteDB {

    int delete(String table, String whereClause, String[] whereArgs);

    boolean isOpen();

    void close();

    Cursor rawQuery(String sql, String[] selectionArgs);

    void execSQL(String sql) throws SQLException;

    long insert(String table, String nullColumnHack, ContentValues values);

    int update(String table, ContentValues values, String whereClause, String[] whereArgs);

    int getVersion();

}
