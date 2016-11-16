# Android Debug Database

## Android Debug Database is a powerful library for debugging databases and shared preferences in Android applications.

### Android Debug Database allows you to view databases and shared preferences directly in your browser in a very simple way.

### How Android Debug Database helps?
* When it comes to debugging database of an android application, it is very difficult to see what's happening inside database, it's make it too simple.
* You can see all the data of shared preferences used in your application.
* You can check the current value of any of the shared preferences.
* You can run any sql query on the given database to update and delete your data.
* You can directly edit the data of databases.
* You can do search in your data.
* You can sort data.
* So this is a very simple tool to see your complete data in your browser.
* Debugging Android Database was never easier than this.


### Using Android Debug Database Library in your application
Add this in your build.gradle
```groovy
debugCompile 'com.amitshekhar.android:debug-db:0.1.0'
```

Use `debugCompile` so that it will only compile in your debug build not in release apk.

That’s all, just start the application, you will be able to see logs in the logcat like below :

* D/DebugDB: Open http://XXX.XXX.X.XXX:8080 in your browser

Now open the provided link in your browser.

Important : Your Android phone and laptop should be connected to the same Wifi.

You will be able to see like below.

<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debugdb.png >


### Found this project useful :heart:
* Support by clicking the :star: button on the upper right of this page. :v:

### TODO
* Edit shared preferences directly from your browser, at present, it is supported for database through query.
* And of course many many features and bug fixes

### Contact - Let's become friend
- [Twitter](https://twitter.com/amitiitbhu)
- [Github](https://github.com/amitshekhariitbhu)
- [Medium](https://medium.com/@amitshekhar)
- [Facebook](https://www.facebook.com/amit.shekhar.iitbhu)

### Check out [Fast Android Networking Library](https://github.com/amitshekhariitbhu/Fast-Android-Networking) for simple and easy networking in Android.

### License
```
   Copyright (C) 2016 Amit Shekhar
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
Just make pull request. You are in!