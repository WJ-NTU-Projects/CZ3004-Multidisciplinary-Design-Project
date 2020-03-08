# MDP Android / Arduino Unmodular Repository
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/101011101001010/MDP/blob/master/LICENSE)

Project files for the Android and Arduino subsystems, MDP AY1920 Sem 2. 

Files for any other subsystems are mainly used for testing purposes.


## Arduino
[Sketch Rundowns and Module Documentations](https://github.com/101011101001010/MDP/wiki)

### External Libraries
* [digitalWriteFast (external download only)](https://code.google.com/archive/p/digitalwritefast/downloads)
* [EnableInterrupt](https://github.com/GreyGnome/EnableInterrupt)


## Android
[![Download](https://img.shields.io/badge/APK%20Download-1.0.0-blue)](https://github.com/101011101001010/MDP/releases/tag/1.0.0)

[Full Changelog](https://github.com/101011101001010/MDP/wiki/Android-Changelog)

### IDE Setup (Updated 02 Mar 2020)
The Android project uses features from recent builds of Android Studio, particularly: [View Binding](https://developer.android.com/topic/libraries/view-binding).

The table below lists the minimum requirements needed to build and run the project files on your PC.

| Stuff          | Minimum Requirement |	
| -------------- | ------------------- |     
| Android Studio | 3.6 RC 3            |
| Gradle Plugin  | 3.6.0-rc03          | 
| Gradle Version | 6.1.1               | 

Gradle version may be modified under File --> Project Structure... --> Project.


## Algorithms
For stripped down testing of ongoing issues only. 

Many functionalities were copied over from Android and may have been stripped down as well.

### IDE Setup (Updated 02 Mar 2020)
| Stuff          | Minimum Requirement |	
| -------------- | ------------------- |     
| IntelliJ IDEA  | 2019.3.3            |  
| JDK            | 8 ONLY              |

### External Libraries (If not using IDEA)
* [TornadoFX - Lightweight JavaFX Framework for Kotlin](https://github.com/edvin/tornadofx)
* [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [Gradle Shadow - For building a fat JAR (optional)](https://github.com/johnrengelman/shadow)
