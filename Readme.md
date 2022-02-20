# <img src="https://gitlab.com/jeseibel/distant-horizons-core/-/raw/main/_logo%20files/LOD%20logo%20flat%20-%20with%20boarder.png" width="32"> Distant Horizons

> A mod that add a Level of Detail System to Minecraft


# What is Distant Horizons?

This mod adds a Level Of Detail (LOD) system to Minecraft.\
This implementation renders simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

Or in other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

<a href="https://www.youtube.com/watch?v=H2tnvEVbO1c" target="_blank">![Minecraft Level Of Detail (LOD) mod - Alpha 1.5](https://i.ytimg.com/vi_webp/H2tnvEVbO1c/mqdefault.webp)</a>

Architectury version: 3.4-SNAPSHOT\
Forge version: 39.0.5 and 38.0.14\
Fabric version: 0.12.12\
Fabric API version: 0.44.0+1.18

Modmenu version: 3.0.0\
Sodium version: mc1.18-0.4.0-alpha5

Notes:\
This version has been confirmed to work in Eclipse and Retail Minecraft.\
(Retail running forge version 1.18.1-39.0.5 and fabric version 1.18-0.12.12 and 1.18.1-0.12.12)


## Source Code Installation

See the Fabric Documentation online for more detailed instructions:\
https://fabricmc.net/wiki/tutorial:setup

### Prerequisites

* A Java Development Kit (JDK) for Java 17 (recommended) or newer. Visit https://www.oracle.com/java/technologies/downloads/ for installers.
* Git or someway to clone git projects. Visit https://git-scm.com/ for installers.
* (Not required) Any Java IDE, for example Intellij IDEA and Eclipse. You may also use any other code editors, such as Visual Studio Code. (Optional)
It's better to use IntelliJ IDEA since Eclipse is not supported by Architectury, but it still works.

**If using IntelliJ:**
1. open IDEA and import the build.gradle
2. refresh the Gradle project in IDEA if required

**If using Ecplise:**
1. run the command: `./gradlew geneclipseruns`
2. run the command: `./gradlew eclipse`
3. Make sure eclipse has the JDK 17 installed. (This is needed so that eclipse can run minecraft)
4. Import the project into eclipse

## Compiling

**Using GUI**
1. Open a command line in the project folder
2. Run the command: `./gradlew build`
3. The compiled jar file will be in the folder `fabric/build/libs/` and `forge/build/libs/`

**If in terminal:**
1. `git clone -b 1.18.X --recurse-submodules https://gitlab.com/jeseibel/minecraft-lod-mod.git`
2. `cd minecraft-lod-mod`
3. `./gradlew assemble` or `./gradlew build`
4. The build should be in `fabric/build/libs/` and `forge/build/libs/`


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
