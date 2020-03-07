# MDP Group 16 Android / Arduino Repository
Project files for the Android and Arduino subsystems, MDP AY1920 Sem 2. 

Files for any other subsystems are mainly used for testing purposes.

For detailed documentations (if any), [please visit the repository wiki](https://github.com/101011101001010/MDP/wiki).


## Android
[![Download](https://img.shields.io/badge/APK%20Download-1.0.0-blue)](https://github.com/GreyGnome/EnableInterrupt)

### IDE Setup (Updated 02 Mar 2020)
The Android project uses features from recent builds of Android Studio, particularly: [View Binding](https://developer.android.com/topic/libraries/view-binding).

The table below lists the minimum requirements needed to build and run the project files on your PC.

| Stuff          | Minimum Requirement |	
| -------------- | ------------------- |     
| Android Studio | 3.6 RC 3            |
| Gradle Plugin  | 3.6.0-rc03          | 
| Gradle Version | 6.1.1               | 

Gradle version may be modified under File --> Project Structure... --> Project.


## Arduino
### External Libraries
* [digitalWriteFast (external download only)](https://code.google.com/archive/p/digitalwritefast/downloads)
* [EnableInterrupt](https://github.com/GreyGnome/EnableInterrupt)


## Algorithms
For stripped down testing of ongoing issues only. 

Many functionalities were copied over from Android and may have been stripped down as well.

### IDE Setup (Updated 02 Mar 2020)
| Stuff          | Minimum Requirement |	
| -------------- | ------------------- |     
| IntelliJ IDEA  | 2019.3.3            |  
| JDK            | 8 ONLY              |

If not using IDEA, Gradle plugin and external libraries may need to be configured separately. All libraries are available on Maven Central.

### External Libraries (If not using IDEA)
* [TornadoFX - Lightweight JavaFX Framework for Kotlin](https://github.com/edvin/tornadofx)
* [Gradle Shadow - For building a fat JAR (optional)](https://github.com/johnrengelman/shadow)
* [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
