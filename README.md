# AR Chat

This [project](https://github.com/erikist/ar-chat/tree/master) will hopefully help explain how to build an AR chat application. The concept is simple: One user initiates a call and will host a video stream for their friends to get in on. People who join the call afterwards can then draw in their friend's worldspace. 

For our tools we are going to be leveraging ARCore, Google's proprietary AR engine for Android, and Twilio's Programmable Video API. Twilio's API gives us a mechanism to stream the video to other devices. You'll be in the driver's seat writing this in Kotlin because we are not savages.

> sav·age /ˈsavij/ -- A person who uses Java when Kotlin is available or Objective-C when Swift is available. 

Let's begin by adding our dependencies. Alter your `app/build.gradle` file, by adding the following dependencies:

```
    // The Twilio Programmable Video API
    implementation 'com.twilio:video-android:2.0.2'
    // ARCore library
    implementation 'com.google.ar:core:1.2.0'
``` 

These are provided in the `jcenter()` and `google()` repositories, and both are added by default in Android Studio. These contain the APIs for both programmable video and ARChat, and we will be exploring a little of what can be done with them in this blog post.

