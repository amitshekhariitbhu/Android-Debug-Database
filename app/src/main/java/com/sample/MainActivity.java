package com.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sample.database.ContactDBHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ContactDBHelper contactDBHelper = new ContactDBHelper(getApplicationContext());
        if (contactDBHelper.count() == 0) {
            for (int i = 0; i < 100; i++) {
                String name = "name_" + i;
                String phone = "phone_" + i;
                String email = "email_" + i;
                String street = "street_" + i;
                String place = "place_" + i;
                contactDBHelper.insertContact(name, phone, email, street, place);
            }
        }
    }
}
