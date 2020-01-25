# MDP-Android
Written in Kotlin.

Interopable with Java if necessary.

## Project Requirements (Updated 26 Jan 2020)
| Stuff          | Minimum Requirement |	
| -------------- | ------------------- |     
| Android Studio | 3.6 Canary 11+ |
| Gradle Plugin  | 3.6.0-alpha12  | 
| Gradle Version | 5.6.4          | 

Gradle version may be modified under File --> Project Structure... --> Project.

Gradle plugin version may be modified in build.gradle (Project: MDP Test Kotlin).
```gradle
dependencies {
	classpath 'com.android.tools.build:gradle:3.6.0-alpha12'
    ...
}
```

#### [IMPORTANT] Make sure view binding is enabled in build.gradle (Module: app): 
```gradle
android {
    ...
    viewBinding {
        enabled = true
    }
}
```

Reference: https://developer.android.com/topic/libraries/view-binding
