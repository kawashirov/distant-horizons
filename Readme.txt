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

Step 6: make sure the eclipse has the JDK 1.8.0_251 installed. (This is needed so that eclipse can run minecraft)


Other commands: 
	"gradlew --refresh-dependencies" to refresh local dependencies. 
	"gradlew clean" to reset everything (this does not affect your code) and then start the process again.



Tip:
	The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.
