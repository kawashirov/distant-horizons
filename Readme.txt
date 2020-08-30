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

Step 5: In Eclipse go to: Expanded Run Button -> Run Configurations -> Environment, Add the variable "JAVA_HOME" with the value of "JAVA_MC_HOME" or whatever the location of the JDK version 1.8.0_251.


Other commands: 
	"gradlew --refresh-dependencies" to refresh local dependencies. 
	"gradlew clean" to reset everything (this does not affect your code) and then start the process again.



Tip:
	The Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.
