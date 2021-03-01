This mod adds a Level Of Detail (LOD) system to Minecraft.
This implementation renders simplified chunks outside the normal render distance
allowing for an increased view distance without harming performance.


Forge version: 1.16.4-35.1.4

Notes:
This version has been confirmed to work on eclipse and retail Minecraft.
(retail running forge 1.16.4-35.1.37)


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


Other commands: 
	"gradlew --refresh-dependencies" to refresh local dependencies. 
	"gradlew clean" to reset everything (this does not affect your code) and then start the process again.



Tip:
	The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

	Current location of mcp-srg.srg:
	"C:/Users/James Seibel/.gradle/caches/minecraft/de/oceanlabs/mcp/mcp_snapshot/20171003/1.12.2/srgs/"
