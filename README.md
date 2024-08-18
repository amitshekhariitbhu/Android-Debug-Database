<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debug_db_banner.png >

# Android Debug Database

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

## About me

Hi, I am Amit Shekhar, Co-Founder @ [Outcome School](https://outcomeschool.com) • IIT 2010-14 • I have taught and mentored many developers, and their efforts landed them high-paying tech jobs, helped many tech companies in solving their unique problems, and created many open-source libraries being used by top companies. I am passionate about sharing knowledge through open-source, blogs, and videos.

You can connect with me on:

- [Twitter](https://twitter.com/amitiitbhu)
- [YouTube](https://www.youtube.com/@amitshekhar)
- [LinkedIn](https://www.linkedin.com/in/amit-shekhar-iitbhu)
- [GitHub](https://github.com/amitshekhariitbhu)

## [Outcome School Blog](https://outcomeschool.com/blog) - High-quality content to learn Android concepts.

### All these features work without rooting your device -> No need of rooted device

### Using Android Debug Database Library in your application

Add this in your `settings.gradle`:
```groovy
maven { url 'https://jitpack.io' }
```

If you are using `settings.gradle.kts`, add the following:
```kotlin
maven { setUrl("https://jitpack.io") }
```

Add this in your `build.gradle`
```groovy
debugImplementation 'com.github.amitshekhariitbhu.Android-Debug-Database:debug-db:1.0.7'
```

If you are using `build.gradle.kts`, add the following:
```kotlin
debugImplementation("com.github.amitshekhariitbhu.Android-Debug-Database:debug-db:1.0.7")
```

Using the Android Debug Database with encrypted database

Add this in your `build.gradle`
```groovy
debugImplementation 'com.github.amitshekhariitbhu.Android-Debug-Database:debug-db-encrypt:1.0.7'
```

If you are using `build.gradle.kts`, add the following:
```kotlin
debugImplementation("com.github.amitshekhariitbhu.Android-Debug-Database:debug-db-encrypt:1.0.7")
```

And to provide the password for the DB, you should add this in the Gradle:
DB_PASSWORD_{VARIABLE}, if for example, PERSON is the database name: DB_PASSWORD_PERSON
```groovy
debug {
    resValue("string", "DB_PASSWORD_PERSON", "password")
}
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

You can connect with me on:

- [Twitter](https://twitter.com/amitiitbhu)
- [LinkedIn](https://www.linkedin.com/in/amit-shekhar-iitbhu)
- [GitHub](https://github.com/amitshekhariitbhu)
- [Facebook](https://www.facebook.com/amit.shekhar.iitbhu)

[**Read all of our blogs here.**](https://outcomeschool.com/blog)

### License

```
   Copyright (C) 2024 Amit Shekhar

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
