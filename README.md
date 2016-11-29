# Android Debug Database

## Android Debug Database is a powerful library for debugging databases and shared preferences in Android applications.

### Android Debug Database allows you to view databases and shared preferences directly in your browser in a very simple way.

### What can Android Debug Database do?
* See all the databases.
* See all the data in the shared preferences used in your application.
* Run any sql query on the given database to update and delete your data.
* Directly edit the database values.
* Search in your data.
* Sort data.

### Check out another awesome library for fast and simple networking in Android.
* [Fast Android Networking Library](https://github.com/amitshekhariitbhu/Fast-Android-Networking)

### Using Android Debug Database Library in your application
Add this to your app's build.gradle
```groovy
debugCompile 'com.amitshekhar.android:debug-db:0.4.0'
```

Use `debugCompile` so that it will only compile in your debug build and not in your release apk.

That’s all, just start the application, you will see in the logcat an entry like follows :

* D/DebugDB: Open http://XXX.XXX.X.XXX:8080 in your browser

* You can also always get the debug address url from your code by calling the method `DebugDB.getAddressLog();`

Now open the provided link in your browser.

Important : Your Android phone and laptop should be connected to the same Network (Wifi or LAN).

Note      : If you want use different port other than 8080. 
            In the app build.gradle file under buildTypes do the following change

```groovy
debug {
    resValue("string", "PORT_NUMBER", "8081")
}
```




You will see something like this :

<img src=https://raw.githubusercontent.com/amitshekhariitbhu/Android-Debug-Database/master/assets/debugdb.png >


### Find this project useful ? :heart:
* Support it by clicking the :star: button on the upper right of this page. :v:

### TODO
* Edit shared preferences directly from the browser. At present, it is only possible to edit database values.
* And of course many more features and bug fixes.

### Contact - Let's become friends
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
Just make pull request. You're in!