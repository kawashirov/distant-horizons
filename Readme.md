# Distant Horizons

This mod adds a Level Of Detail (LOD) system to Minecraft.\
This implementation renders simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

Or in other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

<a href="https://www.youtube.com/watch?v=H2tnvEVbO1c" target="_blank">![Minecraft Level Of Detail (LOD) mod - Alpha 1.5](https://i.ytimg.com/vi_webp/H2tnvEVbO1c/mqdefault.webp)</a>

Forge version: 38.0.14\
Fabric version: 0.12.6\
Fabric API version: 0.43.1+1.18

Notes:\
This version has been confirmed to work in Eclipse and Retail Minecraft.\
(Retail running forge version 1.18-38.0.14 and fabric version 1.18-0.12.6)


## source code installation

See the Fabric Documentation online for more detailed instructions:\
https://fabricmc.net/wiki/tutorial:setup

### Prerequisites

* A Java Development Kit (JDK) for Java 17 (recommended) or newer. Visit https://adoptium.net/releases.html for installers.
* Any Java IDE, for example Intellij IDEA and Eclipse. You may also use any other code editors, such as Visual Studio Code.

**If using Ecplise:**
1. run the command: `./gradlew geneclipseruns`
2. run the command: `./gradlew eclipse`
3. Make sure eclipse has the JDK 17 installed. (This is needed so that eclipse can run minecraft)
4. Import the project into eclipse

**If using IntelliJ:**
1. open IDEA and import the build.gradle
2. run the command: `./gradlew genIntellijRuns`
3. refresh the Gradle project in IDEA if required

## Compiling

**Using GUI**
1. open a command line in the project folder
2. run the command: `./gradlew build`
3. the compiled jar file will be in the folder `fabric/build/libs/` and `forge/build/libs/`

**If in terminal:**
1. `git clone -b 1.18 --recurse-submodules https://gitlab.com/jeseibel/minecraft-lod-mod.git`
2. `cd minecraft-lod-mod`
3. `./gradlew build` (must be run as root on linux)
4. The build should be in `fabric/build/libs/` and `forge/build/libs/`


## Other commands

`./gradlew --refresh-dependencies` to refresh local dependencies.

`./gradlew clean` to reset everything (this does not affect your code) and then start the process again.


## Note to self

The Minecraft source code is NOT added to your workspace in an editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings.

The source code can be 'created' with the `./eclipse` command and can be found in the following path:\
`minecraft-lod-mod\build\fg_cache\mcp\ VERSION \joined\ RANDOM_STRING \patch\output.jar`


## Open Source Acknowledgements

XZ for Java (data compression)\
https://tukaani.org/xz/java.html
