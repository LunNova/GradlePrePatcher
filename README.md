GradlePrePatcher [![Build Status](http://nallar.me/buildservice/job/GradlePrePatcher/badge/icon)](http://nallar.me/buildservice/job/GradlePrePatcher/)
==========
Tool for prepatching minecraft source files. Intended for use with [ModPatcher](https://github.com/nallar/ModPatcher).

Copyright &copy; 2014, nallar <rallan.pcl+gt@gmail.com>
GradlePrePatcher is licensed under the MIT license.

Usage
--------
Add a dependency on GradlePrePatcher to your build.gradle: 

```gradle
buildscript {
    repositories {
        // ... more repositories here
        maven {
            name = 'nallar'
            url = 'http://repo.nallar.me/'
        }
    }
    dependencies {
        // ... more deps here
        classpath 'me.nallar:GradlePrePatcher:1.0-SNAPSHOT'
    }
}
```

Add these hooks to the end of your build.gradle:

```gradle
import me.nallar.gradleprepatcher.Main;

// Change these
def sourceRoot = "src/main/java/" 
def patchPackageName = "org.example.patched"

def prepatchInputs = {
    // Prepatching must run if patch code is updated,
    inputs.dir file(sourceRoot + patchPackageName.replace(".", "/"))
}

project.tasks.deobfBinJar.setDirty()

afterEvaluate {
    Main.loadPatches(file(sourceRoot), file(sourceRoot + patchPackageName.replace(".", "/")))

    deobfBinJar prepatchInputs
    extractMinecraftSrc prepatchInputs
    project.tasks.deobfBinJar.doLast { task ->
        Main.onTaskEnd(task.getOutDirtyJar(), false)
    }
    project.tasks.extractMinecraftSrc.doLast { task ->
        Main.onTaskEnd(file("./build/tmp/recompSrc"), true)
    }
}
```

Download
---------
Download the latest builds from [Jenkins].

Compiling
---------
LogSpamMustDie is built using Gradle.

* Install the java development kit
* Run `./gradlew jar` 


Coding and Pull Request Formatting
----------------------------------
* Generally follows the Oracle coding standards.
* Tabs, no spaces.
* Pull requests must compile and work.
* Pull requests must be formatted properly.

Please follow the above conventions if you want your pull requests accepted.

Donations
----------------------------------

Bitcoin: [1BUjvwxxGH23Fkj7vdGtbrgLEF91u8npQu](bitcoin:1BUjvwxxGH23Fkj7vdGtbrgLEF91u8npQu)

Paypal: rossallan3+pp@googlemail.com

Contributors
----------------------------------

* [nallar](https://github.com/nallar/ "Ross Allan")
* Everyone who's helped with testing and reporting bugs :)

Acknowledgements
----------------------------------

YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products: [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).

[Jenkins]: http://nallar.me/buildservice
