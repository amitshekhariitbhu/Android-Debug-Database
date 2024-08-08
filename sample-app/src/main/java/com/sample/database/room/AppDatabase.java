package com.sample.database.room;


import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Created by anandgaurav on 12/02/18.
 */
@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

}
