ScreenTimeoutDemo
=================

Android screen timeout demo.

This demo resets the screen timeout when a 'user selected' app is in the
foreground. The timeout returns to the default setting when the app is in
the background.

How it works:

I created a Service class to run a TimerTask. The TimerTask gets the list of
running tasks and examines it to see if the 'user selected' app is running in the
forground or background. The screen timeout is set depending on if and where the
app is found.

I've also added a mini tutorial using the ShowcaseView library. You'll need to 
include this library if you want to build and run the demo. More information can 
be found about ShowcaseView at http://espiandev.github.io/ShowcaseView/.

I added a PreferenceActivity class to give the user the ability to set the timeout 
length.
