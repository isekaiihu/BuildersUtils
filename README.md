# BuildersUtils

A client-side Fabric mod for Minecraft 1.21.11 that adds WorldEdit-style block
selection with a wooden axe, plus automated, player-like area/wall mining.

**Author:** ISekai

## Features

### Selection (wooden axe)
- **Left-click** a block while holding a **wooden axe** sets **Position 1**.
- **Right-click** a block while holding a **wooden axe** sets **Position 2**.
- A **green wireframe outline** is drawn around the live selection so you always
  see what is selected (a single green block shows after only pos1 is set).
- These clicks are swallowed client-side, so they won't break/strip blocks even
  on a multiplayer server.
- While mining, the mod turns the camera smoothly, swaps to the best tool in
  your hotbar for each block, walks/jumps toward out-of-reach blocks, and breaks
  them one at a time, driven through the real movement/attack input.

## Commands
| Command        | Description                                                           |
|----------------|------------------------------------------------------------------------|
| `//mine area`  | Walk around and mine every block in the selection until cleared.       |
| `//mine walls` | Mine only the four vertical side walls of the selection.               |
| `//mine off`   | Cancel the active mining task and release all keys.                    |
| `//desel`      | Clear the current selection (and stop any mining).                     |

## Dependencies
- Minecraft `1.21.11`
- Yarn mappings `1.21.11+build.6`
- Fabric Loader `>=0.16.0` (built/tested against `0.19.3`)
- Fabric API `0.141.4+1.21.11`
- Fabric Loom `1.17.12`, Gradle `9.6.0`
- Java 21

## Installation
Download the jar from Releases, drop it into your `.minecraft/mods` folder
along with Fabric API, and launch with Fabric Loader 0.19.3 or newer.

## Building from source
Minecraft 1.21.11 + Loom 1.17 require **Java 21** to run Gradle itself (not
just to compile). Point `JAVA_HOME` at a JDK 21 installation before building.

```bash
./gradlew build
```

Compiled jar lands in `build/libs/`.

### Open in IntelliJ IDEA
1. *File -> Open* -> select this folder, let Gradle import.
2. *Settings -> Build Tools -> Gradle* -> set **Gradle JVM = JDK 21**
   (and *Project Structure -> SDKs* -> JDK 21).
3. Run the generated **Minecraft Client** run configuration.

## Notes / limitations
- The mining "pathfinder" is a pragmatic greedy digger (nearest-block-first,
  top-down, walk + jump + dig-through). It clears open volumes reliably and moves
  like a player, but it is not a full A* solver — it won't place scaffolding to
  cross large gaps or solve complex mazes.
- Selection outline uses depth-tested lines, so it is hidden behind solid blocks
  (same as a normal block outline).
- On servers with anti-cheat, fully automated mining may be flagged - use on
  singleplayer or servers where automation is allowed.

## License
See [LICENSE](LICENSE). All rights reserved - see terms above.
