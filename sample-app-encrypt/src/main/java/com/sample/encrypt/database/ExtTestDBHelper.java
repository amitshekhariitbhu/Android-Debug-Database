package com.sample.encrypt.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.Calendar;

public class ExtTestDBHelper extends SQLiteOpenHelper {

    public static final String DIR_NAME = "custom_dir";
    public static final String DATABASE_NAME = "ExtTest.db";
    public static final String TEST_TABLE_NAME = "test";
    public static final String TEST_ID = "id";
    public static final String TEST_COLUMN_VALUE = "value";
    public static final String TEST_CREATED_AT = "createdAt";

    public ExtTestDBHelper(Context context) {
        super(new CustomDatabasePathContext(context), DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                String.format(
                        "create table %s (%s integer primary key, %s text, %s integer)",
                        TEST_TABLE_NAME,
                        TEST_ID,
                        TEST_COLUMN_VALUE,
                        TEST_CREATED_AT
                )
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertTest(String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("value", value);
        contentValues.put(TEST_CREATED_AT, Calendar.getInstance().getTimeInMillis());
        db.insert(TEST_TABLE_NAME, null, contentValues);
        return true;
    }

    public int count() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select COUNT(*) from " + TEST_TABLE_NAME, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } else {
            return 0;
        }
    }

    private static class CustomDatabasePathContext extends ContextWrapper {

        public CustomDatabasePathContext(Context base) {
            super(base);
        }

        @Override
        public File getDatabasePath(String name) {
            File databaseDir = new File(String.format("%s/%s", getFilesDir(), ExtTestDBHelper.DIR_NAME));
            databaseDir.mkdirs();
            File databaseFile = new File(String.format("%s/%s/%s", getFilesDir(), ExtTestDBHelper.DIR_NAME, name));
            return databaseFile;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
            return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        }
    }
}
