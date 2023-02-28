# <img src="https://gitlab.com/jeseibel/distant-horizons-core/-/raw/main/_Misc%20Files/logo%20files/LOD%20logo%20flat%20-%20with%20boarder.png" width="32"> Distant Horizons

> A mod that adds a Level of Detail System to Minecraft


# What is Distant Horizons?

Distant Horizons is a Minecraft mod that adds a Level Of Detail (LOD) system to\
render simplified chunks outside the normal render distance\
allowing for an increased view distance without harming performance.

In other words: this mod lets you see farther without turning your game into a slide show.\
If you want to see a quick demo, check out a video covering the mod here:

<a href="https://youtu.be/_04BZ8W2bDM" target="_blank">![Minecraft Level Of Detail (LOD) mod - Alpha 1.6.3](https://cdn.ko-fi.com/cdn/useruploads/png_ef4d209d-50d9-462f-b31f-92e42ec3e260cover.jpg?v=c1097a5b-029c-4484-bec3-80ff58c5d239)</a>

<br>

## Minecraft and Library Versions

This branch supports the following versions of Minecraft:

> **NOTE: At the moment only 1.18.2 fabric works/build/runs**

#### 1.19 (BROKE)
Forge: 41.0.19\
Fabric: 0.14.7\
Fabric API: 0.55.3+1.19\
Modmenu: 4.0.0

#### 1.18.2 (ONLY FABRIC WORKS)
Fabric: 0.14.14\
Fabric API: 0.67.1+1.18.2\
Forge: 40.0.32\
Parchment=2022.11.06\
Modmenu: 3.2.5

#### 1.18.1, 1.18 (CEASING DEVELOPMENT SOON)
Forge: 39.1.2\
Fabric: 0.13.3\
Fabric API: 0.42.6+1.18\
Modmenu: 3.0.1

#### 1.17.1, 1.17 (BROKE)
Forge: 37.1.1\
Fabric: 0.13.2\
Fabric API: 0.46.1+1.17\
Modmenu: 2.0.14

#### 1.16.5, 1.16.5 (BROKE)
Forge: 36.2.28\
Fabric: 0.13.2\
Fabric API: 0.42.0+1.16\
Modmenu: 1.16.22
<br><br>

### Plugin and Library versions

Fabric loom: 1.1-snapshot\
Forge gradle: 5.1.67\
Sponge vanilla gradle: 0.2.1-SNAPSHOT\
Sponge mixin gradle: 0.7-SNAPSHOT\
Java Compiler plugin: Manifold Preprocessor

<br>

## Source Code Installation

### Prerequisites

* A Java Development Kit (JDK) for Java 17 (recommended) or newer. <br>
  Visit https://www.oracle.com/java/technologies/downloads/ for installers.
* Git or someway to clone git projects. <br> 
  Visit https://git-scm.com/ for installers.
* (Not required) Any Java IDE with plugins that support Manifold, for example Intellij IDEA.

**If using IntelliJ:**
1. Install the Manifold plugin
2. Open IDEA and import the build.gradle
3. Refresh the Gradle project in IDEA if required

**If using Eclipse: (Note that Eclipse doesn't support Manifold's preprocessor!)**
1. Run the command: `./gradlew geneclipseruns`
2. Run the command: `./gradlew eclipse`
3. Make sure eclipse has the JDK 17 installed. (This is needed so that eclipse can run minecraft)
4. Import the project into eclipse



## Switching Versions

To switch between different Minecraft versions, change `mcVer=1.?` in the `gradle.properties` file.

If running in an IDE, to ensure the IDE noticed the version change, run any gradle command to refresh gradle. (In IntellJ you will also need to do a gradle sync if it didn't happen automatically.)
>Note: There may be a `java.nio.file.FileSystemException` thrown when running the command after switching versions. To fix it, either restart your IDE (as your IDE is probably locking a file) or use a tool like LockHunter to unlock the linked file(s). (Generally it is a lib file under `common\build\lib`, `forge\build\lib`, or `fabric\build\lib`). \
> If anyone knows how to solve this issue please let us know here: \
> https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/233
 
<br>

## Compiling

Prerequisites:
- JDK 17 or newer

From the File Explorer:
1. Download and extract the project zip
2. Download the core from https://gitlab.com/jeseibel/distant-horizons-core and extract into a folder called `coreSubProjects`
3. Open a terminal emulator in the project folder (On Windows you can type `cmd` in the title bar)
4. Run the commands: `./gradlew assemble`
5. The compiled jar file will be in the folder `Merged`

From the command line:
1. `git clone --recurse-submodules https://gitlab.com/jeseibel/minecraft-lod-mod.git`
2. `cd minecraft-lod-mod`
3. `./gradlew assemble`
4. The compiled jar file will be in the folder `Merged`

Run tests with: `./gradlew test`

>Note: You can add the arg: `-PmcVer=?` to tell gradle to build a selected MC version instead of having to modify the `gradle.properties` file. \
> Example: `./gradlew assemble -PmcVer=1.18.2`


<Br>

## Other commands

`./gradlew --refresh-dependencies` to refresh local dependencies.

`./gradlew clean` to delete any compiled code.


## Note to self

The Minecraft source code is NOT added to your workspace in an editable way. Minecraft is treated like a normal Library. Sources are there for documentation and research purposes only.

Source code uses Mojang mappings & [Parchment](https://parchmentmc.org/) mappings.

To generate the source code run `./gradlew genSources`\
If your IDE fails to auto-detect the source jars when browsing Minecraft classes; manually select the JAR file ending with -sources.jar when prompted by your IDE. <br>
(In IntelliJ it's at the top where it says "choose sources" when browsing a Minecraft class)

<br>

## Other Useful commands

Run the standalone jar: `./gradlew run`\
Build the standalone jar: `./gradlew core:build`\
Only build Fabric: `./gradlew fabric:assemble` or `./gradlew fabric:build`\
Only build Forge: `./gradlew fabric:assemble` or `./gradlew forge:build`\
Run the Fabric client (for debugging): `./gradlew fabric:runClient`\
Run the Forge client (for debugging): `./gradlew forge:runClient`

To build all versions: `./buildAll` (all builds will end up in the `Merged` folder)

<br>

## Open Source Acknowledgements

Forgix (To merge multiple mod versions into one jar) [_Used to be_ [_DHJarMerger_](https://github.com/Ran-helo/DHJarMerger)]\
https://github.com/PacifistMC/Forgix

XZ for Java (data compression)\
https://tukaani.org/xz/java.html

Json & Toml for Java (config handling)\
https://github.com/TheElectronWill/night-config

SVG Salamander for SVG support\
https://github.com/blackears/svgSalamander

FlatLaf for theming (for development testing)\
https://www.formdev.com/flatlaf/
