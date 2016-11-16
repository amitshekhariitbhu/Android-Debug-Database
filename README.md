# Android Debug Database

## Android Debug Database is a powerful library for debugging databases and shared preferences in Android applications.

## Android Debug Database is a very simple tool for viewing databases and shared preferences directly in your browser.

### How Android Debug Database helps?
* When it comes to debugging database of an android application, it is very difficult to see what's happening inside database, it's make it too simple.
* You can see all the data of shared preferences used in your application.
* You can check the current value of any of the shared preferences.
* You can do search in your data.
* So this is a very simple tool to see your complete data in your browser.
* Debugging Android Database was never easier than this.


### Using Android Debug Database Library in your application
Add this in your build.gradle
```groovy
debugCompile 'com.amitshekhar.android:debug-db:0.1.0'
```

Use `debugCompile` so that it will only compile in your debug build not in release apk.

When your android application starts, you will be able to see the logs like below :

* D/DebugDB: Open http://XXX.XXX.X.XXX:8080 in your browser

Just open the provided link in your browser, you are set to see all data of databases and shared preferences.

Important : Your Android phone and laptop should be connected to the same Wifi.

<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debugdb.png >


### Found this project useful :heart:
* Support by clicking the :star: button on the upper right of this page. :v:

### TODO
* Edit data directly from your browser
* And of course many many features and bug fixes

### Contact - Let's become friend
- [Twitter](https://twitter.com/amitiitbhu)
- [Github](https://github.com/amitshekhariitbhu)
- [Medium](https://medium.com/@amitshekhar)
- [Facebook](https://www.facebook.com/amit.shekhar.iitbhu)

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