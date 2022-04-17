# THIS VERSION IS NO LONGER MAINTAINED! ALL DEVELOPMENT HAVE BEEN MOVED OVER TO THE MAIN BRANCH

# <img src="https://gitlab.com/jeseibel/distant-horizons-core/-/raw/main/_logo%20files/LOD%20logo%20flat%20-%20with%20boarder.png" width="32"> Distant Horizons

> A mod that adds a Level of Detail System to Minecraft


# What is Distant Horizons?

This mod adds a Level Of Detail (LOD) system to Minecraft.\
This implementation renders simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

Or in other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

<a href="https://www.youtube.com/watch?v=H2tnvEVbO1c" target="_blank">![Minecraft Level Of Detail (LOD) mod - Alpha 1.5](https://i.ytimg.com/vi_webp/H2tnvEVbO1c/mqdefault.webp)</a>

### Versions

Architectury version: 3.4-SNAPSHOT\
Java Compiler plugin: Manifold Preprocessor

#### 1.17.1 mods
Forge version: 37.1.1\
Fabric version: 0.13.2\
Fabric API version: 0.46.1+1.17\
Modmenu version: 2.0.14


## Source Code Installation

See the Fabric Documentation online for more detailed instructions:\
https://fabricmc.net/wiki/tutorial:setup

### Prerequisites

* A Java Development Kit (JDK) for Java 17 (recommended) or newer. Visit https://www.oracle.com/java/technologies/downloads/ for installers.
* Git or someway to clone git projects. Visit https://git-scm.com/ for installers.
* (Not required) Any Java IDE with plugins that support Manifold, for example Intellij IDEA.

**If using IntelliJ:**
0. Install Manifold plugin
1. open IDEA and import the build.gradle
2. refresh the Gradle project in IDEA if required

**If using Ecplise: (Note that Eclispe currently doesn't support Manifold's preprocessor!)**
1. run the command: `./gradlew geneclipseruns`
2. run the command: `./gradlew eclipse`
3. Make sure eclipse has the JDK 17 installed. (This is needed so that eclipse can run minecraft)
4. Import the project into eclipse

## Switching Versions
This branch support 1 built versions:
- 1.17.1 (which also runs on 1.17)

To switch between active versions, change `mcVer=1.17.?` in `gradle.properties` file.

If running on IDE, to ensure IDE pickup the changed versions, you will need to run a gradle command again to allow gradle to update all the libs. (In IntellJ you will also need to do a gradle sync again if it didn't start it automatically.)
>Note: There may be a `java.nio.file.FileSystemException` thrown on running the command after switching versions. To fix it, either restart your IDE (as your IDE is locking up a file) or use tools like LockHunter to unlock the linked file. (Often a lib file under `common\build\lib` or `forge\build\lib` or `fabric\build\lib`). If anyone knows how to solve this issue please comment to this issue: https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/233

## Compiling

**Using GUI**
1. Download the zip of the project and extract it
2. Download the core from https://gitlab.com/jeseibel/distant-horizons-core and extract into a folder called `core`
3. Open a command line in the project folder
4. Run the command: `./gradlew assemble`
5. Then run command: `./gradlew mergeJars`
6. The compiled jar file will be in the folder `Merged`

**If in terminal:**
1. `git clone -b preprocessor_test --recurse-submodules https://gitlab.com/jeseibel/minecraft-lod-mod.git`
2. `cd minecraft-lod-mod`
3. `./gradlew assemble`
4. `./gradlew mergeJars`
5. The compiled jar file will be in the folder `Merged`
>Note: You can add the arg: `-PmcVer=1.17.?` to tell gradle to build a selected MC version instead of having to manually modify the `gradle.properties` file.


## Other commands

`./gradlew --refresh-dependencies` to refresh local dependencies.

`./gradlew clean` to reset everything (this does not affect your code) and then start the process again.


## Note to self

The Minecraft source code is NOT added to your workspace in an editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings.

## Useful commands

Build only Fabric: `./gradlew fabric:assemble` or `./gradlew fabric:build`\
Build only Forge: `./gradlew fabric:assemble` or `./gradlew forge:build`\
Run the Fabric client (for debugging): `./gradlew fabric:runClient`\
Run the Forge client (for debugging): `./gradlew forge:runClient`

## Open Source Acknowledgements

XZ for Java (data compression)\
https://tukaani.org/xz/java.html

DHJarMerger (To merge multiple mod versions into one jar)\
https://github.com/Ran-helo/DHJarMerger
