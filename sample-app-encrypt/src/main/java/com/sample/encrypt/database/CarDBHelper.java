/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.sample.encrypt.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class CarDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Car.db";
    public static final String CARS_TABLE_NAME = "cars";
    public static final String CARS_COLUMN_ID = "id";
    public static final String CARS_COLUMN_NAME = "name";
    public static final String CARS_COLUMN_COLOR = "color";
    public static final String CCARS_COLUMN_MILEAGE = "mileage";

    public CarDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table cars " +
                        "(id integer primary key, name text, color text, mileage real)"
        );

        db.execSQL("create table [transaction] (id integer primary key, name text)");

        for (int i = 0; i < 10; i++) {
            db.execSQL("insert into [transaction] (name) values ('hello');");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS cars");
        onCreate(db);
    }

    public boolean insertCar(String name, String color, float mileage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("color", color);
        contentValues.put("mileage", mileage);
        db.insert("cars", null, contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from cars where id=" + id + "", null);
        return res;
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CARS_TABLE_NAME);
        return numRows;
    }

    public boolean updateCar(Integer id, String name, String color, float mileage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("color", color);
        contentValues.put("mileage", mileage);
        db.update("cars", contentValues, "id = ? ", new String[]{Integer.toString(id)});
        return true;
    }

    public Integer deleteCar(Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("cars",
                "id = ? ",
                new String[]{Integer.toString(id)});
    }

    public ArrayList<String> getAllCars() {
        ArrayList<String> arrayList = new ArrayList<>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from cars", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            arrayList.add(res.getString(res.getColumnIndex(CARS_COLUMN_NAME)));
            res.moveToNext();
        }
        return arrayList;
    }

    public int count() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from cars", null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } else {
            return 0;
        }
    }
}
