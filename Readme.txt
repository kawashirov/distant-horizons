This program is an attempt to create Level Of Details (LODs) in Minecraft.
The purpose is to increase the maximum view distance in game 


========================
source code installation
========================

See the Forge Documentation online for more detailed instructions:
http://mcforge.readthedocs.io/en/latest/gettingstarted/

Step 1: open a command line in the project folder

Step 2: run the command: "./gradlew setupDecompWorkspace"

Step 3: run the command: "./gradlew eclipse"

Step 4: Import project

Step 5: create a system variable called "JAVA_MC_HOME" with the location of the JDK 1.8.0_251 (This is needed for gradle to work correctly)
		And make sure it is used in the build.gradle file.

Step 6: In the eclipse run configuration add "-Dfml.coreMods.load=backsun.lod.LodMain"
		This lets forge know that you have a core mod you want to run.

Step 7: make sure the eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)


Other commands: 
	"gradlew --refresh-dependencies" to refresh local dependencies. 
	"gradlew clean" to reset everything (this does not affect your code) and then start the process again.



Tip:
	The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

	Current location of mcp-srg.srg:
	"C:/Users/James Seibel/.gradle/caches/minecraft/de/oceanlabs/mcp/mcp_snapshot/20171003/1.12.2/srgs/"