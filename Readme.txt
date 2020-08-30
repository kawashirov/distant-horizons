source installation
==============================

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
When using Decomp workspace, the Minecraft source code is NOT added to your workspace in a editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes and usually can be accessed under the 'referenced libraries' section of your IDE.

Forge source installation
=========================
MinecraftForge ships with this code and installs it as part of the forge
installation process, no further action is required on your part.

LexManos' Install Video
=======================
https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be

For more details update more often refer to the Forge Forums:
http://www.minecraftforge.net/forum/index.php/topic,14048.0.html
