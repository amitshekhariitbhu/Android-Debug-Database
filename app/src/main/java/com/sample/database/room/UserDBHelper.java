package com.sample.database.room;

import android.arch.persistence.room.Room;
import android.content.Context;

import java.util.List;

/**
 * Created by anandgaurav on 12/02/18.
 */

public class UserDBHelper {

    private final AppDatabase appDatabase;

    public UserDBHelper(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "User-Database")
                .allowMainThreadQueries()
                .build();
    }

    public void insertUser(List<User> userList) {
        appDatabase.userDao().insertAll(userList);
    }

    public int count() {
        return appDatabase.userDao().loadAll().size();
    }

}
