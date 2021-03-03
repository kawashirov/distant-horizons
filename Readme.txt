This mod adds a Level Of Detail (LOD) system to Minecraft.
This implementation renders simplified chunks outside the normal render distance
allowing for an increased view distance without harming performance.

Or in other words: this mod let's you see farther without turning your game into a slide show.


Forge version: 1.16.4-35.1.4

Notes:
This version has been confirmed to work in Eclipse and retail Minecraft.
(retail running forge 1.16.4-35.1.37)
That being said only singleplayer is currently supported; connecting
to servers (local or otherwise) will cause no LODs to be drawn and
may cause instibility.


========================
source code installation
========================

See the Forge Documentation online for more detailed instructions:
http://mcforge.readthedocs.io/en/latest/gettingstarted/

Step 1: Create a system variable called "JAVA_MC_HOME" with the location of the JDK 1.8.0_251 (This is needed for gradle to work correctly)

Step 2: replace JAVA_HOME with JAVA_MC_HOME in gradle.bat

Step 3: open a command line in the project folder

Step 4: run the command: "./gradlew geneclipseruns"

Step 5: run the command: "./gradlew eclipse"

Step 6: Make sure the eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)

Step 7: Import the project into eclipse



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

