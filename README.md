<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debug_db_banner.png >

# Android Debug Database

[![Mindorks](https://img.shields.io/badge/mindorks-opensource-blue.svg)](https://mindorks.com/open-source-projects)
[![Mindorks Community](https://img.shields.io/badge/join-community-blue.svg)](https://mindorks.com/join-community)
[![Mindorks Android Store](https://img.shields.io/badge/Mindorks%20Android%20Store-Android%20Debug%20Database-blue.svg?style=flat)](https://mindorks.com/android/store)
[![API](https://img.shields.io/badge/API-9%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=9)
[![Download](https://api.bintray.com/packages/amitshekhariitbhu/maven/debug-db/images/download.svg) ](https://bintray.com/amitshekhariitbhu/maven/debug-db/_latestVersion)
[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=102)](https://opensource.org/licenses/Apache-2.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/amitshekhariitbhu/Android-Debug-Database/blob/master/LICENSE)

## Android Debug Database is a powerful library for debugging databases and shared preferences in Android applications

### Android Debug Database allows you to view databases and shared preferences directly in your browser in a very simple way

### What can Android Debug Database do?

* See all the databases.
* See all the data in the shared preferences used in your application.
* Run any sql query on the given database to update and delete your data.
* Directly edit the database values.
* Directly edit the shared preferences.
* Directly add a row in the database.
* Directly add a key-value in the shared preferences.
* Delete database rows and shared preferences.
* Search in your data.
* Sort data.
* Download database.
* Debug Room inMemory database.

### All these features work without rooting your device -> No need of rooted device

### Check out another awesome library for fast and simple networking in Android

* [Fast Android Networking Library](https://github.com/amitshekhariitbhu/Fast-Android-Networking)

### Using Android Debug Database Library in your application

Add this to your app's build.gradle

```groovy
debugImplementation 'com.amitshekhar.android:debug-db:1.0.6'
```

Using the Android Debug Database with encrypted database

```groovy
debugImplementation 'com.amitshekhar.android:debug-db-encrypt:1.0.6'
```

Use `debugImplementation` so that it will only compile in your debug build and not in your release build.

That’s all, just start the application, you will see in the logcat an entry like follows :

* D/DebugDB: Open http://XXX.XXX.X.XXX:8080 in your browser

* You can also always get the debug address url from your code by calling the method `DebugDB.getAddressLog();`

Now open the provided link in your browser.

Important:

* Your Android phone and laptop should be connected to the same Network (Wifi or LAN).
* If you are using it over usb, run `adb forward tcp:8080 tcp:8080`

Note      : If you want use different port other than 8080.
            In the app build.gradle file under buildTypes do the following change

```groovy
debug {
    resValue("string", "PORT_NUMBER", "8081")
}
```

You will see something like this :

### Seeing values

<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debugdb.png >

### Editing values

<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debugdb_edit.png >

### Working with emulator

* Android Default Emulator: Run the command in the terminal - `adb forward tcp:8080 tcp:8080` and open http://localhost:8080
* Genymotion Emulator: Enable bridge from configure virtual device (option available in genymotion)

### Getting address with toast, in case you missed the address log in logcat

As this library is auto-initialize, if you want to get the address log, add the following method and call (we have to do like this to avoid build error in release build as this library will not be included in the release build) using reflection.

```java
public static void showDebugDBAddressLogToast(Context context) {
    if (BuildConfig.DEBUG) {
       try {
            Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
            Method getAddressLog = debugDB.getMethod("getAddressLog");
            Object value = getAddressLog.invoke(null);
            Toast.makeText(context, (String) value, Toast.LENGTH_LONG).show();
       } catch (Exception ignore) {

       }
    }
}
```

### Adding custom database files

As this library is auto-initialize, if you want to debug custom database files, add the following method and call

```java
public static void setCustomDatabaseFiles(Context context) {
    if (BuildConfig.DEBUG) {
        try {
            Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
            Class[] argTypes = new Class[]{HashMap.class};
            Method setCustomDatabaseFiles = debugDB.getMethod("setCustomDatabaseFiles", argTypes);
            HashMap<String, Pair<File, String>> customDatabaseFiles = new HashMap<>();
            // set your custom database files
            customDatabaseFiles.put(ExtTestDBHelper.DATABASE_NAME,
                    new Pair<>(new File(context.getFilesDir() + "/" + ExtTestDBHelper.DIR_NAME +
                                                    "/" + ExtTestDBHelper.DATABASE_NAME), ""));
            setCustomDatabaseFiles.invoke(null, customDatabaseFiles);
        } catch (Exception ignore) {

        }
    }
}
```

### Adding InMemory Room databases

As this library is auto-initialize, if you want to debug inMemory Room databases, add the following method and call

```java
public static void setInMemoryRoomDatabases(SupportSQLiteDatabase... database) {
    if (BuildConfig.DEBUG) {
        try {
            Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
            Class[] argTypes = new Class[]{HashMap.class};
            HashMap<String, SupportSQLiteDatabase> inMemoryDatabases = new HashMap<>();
            // set your inMemory databases
            inMemoryDatabases.put("InMemoryOne.db", database[0]);
            Method setRoomInMemoryDatabase = debugDB.getMethod("setInMemoryRoomDatabases", argTypes);
            setRoomInMemoryDatabase.invoke(null, inMemoryDatabases);
        } catch (Exception ignore) {

        }
    }
}
```

### Find this project useful ? :heart:

* Support it by clicking the :star: button on the upper right of this page. :v:

### TODO

* Simplify emulator issue [Issue Link](https://github.com/amitshekhariitbhu/Android-Debug-Database/issues/6)
* And of course many more features and bug fixes.

### [Check out Mindorks awesome open source projects here](https://mindorks.com/open-source-projects)

### Contact - Let's become friends

* [Twitter](https://twitter.com/amitiitbhu)
* [GitHub](https://github.com/amitshekhariitbhu)
* [Medium](https://medium.com/@amitshekhar)
* [Facebook](https://www.facebook.com/amit.shekhar.iitbhu)

### License

```
   Copyright (C) 2019 Amit Shekhar
   Copyright (C) 2011 Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

### Contributing to Android Debug Database

All pull requests are welcome, make sure to follow the [contribution guidelines](CONTRIBUTING.md)
when you submit pull request.
