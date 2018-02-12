package com.sample.database.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by anandgaurav on 12/02/18.
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    public Long id;

    public String name;

}
