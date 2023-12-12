package com.amitshekhar.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Created by anandgaurav on 12/02/18.
 */

public class InMemoryDebugSQLiteDB implements SQLiteDB {

    private final SupportSQLiteDatabase database;

    public InMemoryDebugSQLiteDB(SupportSQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return database.delete(table, whereClause, whereArgs);
    }

    @Override
    public boolean isOpen() {
        return database.isOpen();
    }

    @Override
    public void close() {
        // no ops
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return database.query(sql, selectionArgs);
    }

    @Override
    public void execSQL(String sql) throws SQLException {
        database.execSQL(sql);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        return database.insert(table, 0, values);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return database.update(table, 0, values, whereClause, whereArgs);
    }

    @Override
    public int getVersion() {
        return database.getVersion();
    }
}
