This mod adds a Level Of Detail (LOD) system to Minecraft.
This implementation renders simplified chunks outside the normal render distance
allowing for an increased view distance without harming performance.

Or in other words: this mod let's you see farther without turning your game into a slide show.
If you want to see a quick demo, check out the video I made here:
https://youtu.be/CCT-3s02tYA


Forge version: 1.16.5-36.1.0

Notes:
This version has been confirmed to work in Eclipse and Retail Minecraft.
(Retail running forge version 1.16.5-36.1.0)


========================
source code installation
========================

See the Forge Documentation online for more detailed instructions:
http://mcforge.readthedocs.io/en/latest/gettingstarted/

Step 1: Create a system variable called "JAVA_MC_HOME" with the location of the JDK 1.8.0_251 (This is needed for gradle to work correctly)
Step 2: replace JAVA_HOME with JAVA_MC_HOME in gradle.bat
Step 3: open a command line in the project folder

If using Ecplise:
Step e-1: run the command: "./gradlew geneclipseruns"
Step e-2: run the command: "./gradlew eclipse"
Step e-3: Make sure eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)
Step e-4: Import the project into eclipse

If using IntelliJ
step i-1: open IDEA and import the build.gradle
step i-2: run the command: "./gradlew genIntellijRuns"
step i-3: refresh the Gradle project in IDEA if required





=========
compiling
=========

Step 1: open a command line in the project folder
Step 2: run the command: "./gradlew build"
Step 3: the compiled jar file will be in the folder "build\libs"



==============
Other commands
==============

"./gradlew --refresh-dependencies" to refresh local dependencies. 
"./gradlew clean" to reset everything (this does not affect your code) and then start the process again.



============
Note to self
============

The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings.

The source code can be 'created' with the ./eclipse command and can be found in the following path:
minecraft-lod-mod\build\fg_cache\mcp\ VERSION \joined\ RANDOM_STRING \patch\output.jar
