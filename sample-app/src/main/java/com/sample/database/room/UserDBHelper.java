package com.sample.database.room;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

/**
 * Created by anandgaurav on 12/02/18.
 */

public class UserDBHelper {

    private final AppDatabase appDatabase;
    private final AppDatabase inMemoryAppDatabase;

    public UserDBHelper(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "User.db")
                .allowMainThreadQueries()
                .build();
        inMemoryAppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    public void insertUser(List<User> userList) {
        appDatabase.userDao().insertAll(userList);
    }

    public void insertUserInMemory(List<User> userList) {
        inMemoryAppDatabase.userDao().insertAll(userList);
    }

    public int count() {
        return appDatabase.userDao().loadAll().size();
    }

    public int countInMemory() {
        return inMemoryAppDatabase.userDao().loadAll().size();
    }

    public SupportSQLiteDatabase getInMemoryDatabase() {
        return inMemoryAppDatabase.getOpenHelper().getWritableDatabase();
    }
}
