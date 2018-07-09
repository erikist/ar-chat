# AR Chat [![Build Status](https://www.bitrise.io/app/610e7fa57fe13251/status.svg?token=SEQQEFc-va9jbjqe_p8Q3w)](https://www.bitrise.io/app/610e7fa57fe13251)

This [project](https://github.com/erikist/ar-chat/tree/master) will hopefully help explain how to build an AR chat application. The concept is simple: One user initiates a call and will host a video stream for their friends to get in on. People who join the call afterwards can then draw in their friend's worldspace. 

For our tools we are going to be leveraging ARCore, Google's proprietary AR engine for Android, and Twilio's Programmable Video API. Twilio's API gives us a mechanism to stream the video to other devices. You'll be in the driver's seat writing this in Kotlin because we are not savages.

> sav·age /ˈsavij/ -- A person who uses Java when Kotlin is available or Objective-C when Swift is available. 

### Installation

Let's begin by adding our dependencies. Alter your `app/build.gradle` file, by adding the following dependencies:

```
    // The Twilio Programmable Video API
    implementation 'com.twilio:video-android:2.0.2'
    // ARCore library
    implementation 'com.google.ar:core:1.3.0'
``` 

These are provided in the `jcenter()` and `google()` repositories; both are added by default in Android Studio. These contain the APIs for both programmable video and ARChat, and we will be exploring a little of what can be done with them in this blog post.

In addition, if you want to follow along, we will be [using databinding](https://developer.android.com/topic/libraries/data-binding/start) for this tutorial. _We are not savages._

### API Keys

First off, we are going to need a Twilio API key, which you can get via signing up for the [Twilio service](https://www.twilio.com/) and getting access to their programmable video API, where they will provide your key. This key ties your twilio account and your application together so that Twilio can get paid, and your application's use can be identified. 

This brings us our first challenge: how do we provide this key and keep it secure. _In reality,_ you can't keep anything too private with JVM based languages. There's always risk of exposing your API key, but let's keep it managed and do it in a way that can be replicated happily in your projects. First off putting your key into some field is a big no-no. If you can download your project from source control and see your key then _you are doing it wrong, you savage_.

Create a file called `release-environment.sh` in your top level application directory and add it to your `.gitignore`. Inside of it, add your API key as follows: 

```
#!/bin/bash

export TWILIO_AR_CHAT_KEY="heylookijustmadeupmyowntwiliokeybutactuallyputyourshere"
```

Run this by simply calling `source release-environment.sh` and your environment variables will now contain your Twilio API key. Now, let's go let gradle know about the environment variable. Alter your `build.gradle` as such: 

```
android {
    //...
    buildTypes {
        debug {
            buildConfigField("String", "TWILIO_API_KEY", "\"${System.getenv("TWILIO_AR_CHAT_KEY")}\"")
        }
        release {
            buildConfigField("String", "TWILIO_API_KEY", "\"${System.getenv("TWILIO_AR_CHAT_KEY")}\"")
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    //...
}
```

Android build tools will generate a file from your `build.gradle` called `BuildConfig.java` which is available to your application. The function `buildConfigField` allows us to specify a variable (in this case, a `String` called `TWILIO_API_KEY` with the environment variable `TWILIO_AR_CHAT_KEY`) and use it in app. We can specify different keys for release and debug, if you have a sandbox environment for instance. You now can reference your API key programmatically by calling `BuildConfig.TWILIO_API_KEY`.

### Architecture

For our application, we are going to have two main activities. We are going to have a the `RoomActivity` which is responsible for providing a UI for our users to choose what "Room" they would like to join. A "Room" is a concept for Twilio which is basically an abstraction of the notion of a chatroom. Multiple users can join this so long as they know the Room name. In a more realistic application, you would probably generate this in some way, but for our purposes we are just going to have the user tell us which room they want to either join or create. Additionally, we need to have the user identify themselves, so we will prompt them for a name. In addition, we will have a `CallActivity` which is responsible for connecting to the call, displaying the video and audio and metadata about our Room.

### RoomActivity

