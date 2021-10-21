# Distant Horizons

This mod adds a Level Of Detail (LOD) system to Minecraft.\
This implementation renders simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

Or in other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

[![Minecraft Level Of Detail (LOD) mod - Alpha 1.4](https://i.ytimg.com/vi_webp/MDWcEvdUGUE/mqdefault.webp)](https://www.youtube.com/watch?v=MDWcEvdUGUE)


Forge version: 1.16.5-36.1.0

Notes:\
This version has been confirmed to work in Eclipse and Retail Minecraft.\
(Retail running forge version 1.16.5-36.1.0)


## source code installation

See the Forge Documentation online for more detailed instructions:\
http://mcforge.readthedocs.io/en/latest/gettingstarted/

1. Create a system variable called "JAVA_MC_HOME" with the location of the JDK 1.8.0_251 (This is needed for gradle to work correctly)
2. replace JAVA_HOME with JAVA_MC_HOME in gradle.bat
3. open a command line in the project folder

**If using Ecplise:**
1. run the command: `./gradlew geneclipseruns`
2. run the command: `./gradlew eclipse`
3. Make sure eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)
4. Import the project into eclipse

**If using IntelliJ:**
1. open IDEA and import the build.gradle
2. run the command: `./gradlew genIntellijRuns`
3. refresh the Gradle project in IDEA if required


## Compiling

1. open a command line in the project folder
2. run the command: `./gradlew build`
3. the compiled jar file will be in the folder `build\libs`


## Other commands

`./gradlew --refresh-dependencies` to refresh local dependencies.

`./gradlew clean` to reset everything (this does not affect your code) and then start the process again.


## Note to self

The Minecraft source code is NOT added to your workspace in an editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings.

The source code can be 'created' with the `./eclipse` command and can be found in the following path:\
`minecraft-lod-mod\build\fg_cache\mcp\ VERSION \joined\ RANDOM_STRING \patch\output.jar`


## Open Source Acknowledgements

XZ for Java (data compression)
https://tukaani.org/xz/java.html