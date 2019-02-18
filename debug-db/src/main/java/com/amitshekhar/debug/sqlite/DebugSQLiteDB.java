package com.amitshekhar.debug.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.amitshekhar.sqlite.SQLiteDB;

public class DebugSQLiteDB implements SQLiteDB {

    private final SQLiteDatabase database;

    public DebugSQLiteDB(SQLiteDatabase database) {
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
        database.close();
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return database.rawQuery(sql, selectionArgs);
    }

    @Override
    public void execSQL(String sql) throws SQLException {
        database.execSQL(sql);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        return database.insert(table, nullColumnHack, values);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return database.update(table, values, whereClause, whereArgs);
    }

    @Override
    public int getVersion() {
        return database.getVersion();
    }
}
