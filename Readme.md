# ![DH x MiA](readme/logo.png "DH x MiA")
A kawashirov's customized version of **Distant Horizons** mod for **Mine in Abyss** server.
Fixes some server-specific errors and minor optimizations.

Check out the mod's main GitLab page here:<br>
https://gitlab.com/jeseibel/minecraft-lod-mod

Check out Mine in Abyss here:<br>
https://mineinabyss.com/

Forum thread on Mine in Abyss Discord:<br>
https://discord.com/channels/385252444734619680/1181222588169588746/1181222588169588746

### Differences from original version
- **Fixed issues that causes `LodDataBuilder` to crash constantly (and spam errors in chat)**, especially on most of tall chunks on MiA. Should build LODs for all the chunks properly.
- - [Not using `getLightBlockingHeightMapValue` anymore](https://github.com/kawashirov/distant-horizons-core/commit/a5fdd224283938ca53542a24503875b94b121a7b), as it sometimes returns invalid data for some reason (especially on MiA).
- - [Added workaround](https://github.com/kawashirov/distant-horizons-core/commit/4ca91f9c0535adeedf08cf32e36a5fd7bfc3d3f2) for `NullPointerException` in `ChunkLightStorage.get()`
- [Disabled `glIsProgram` check](https://github.com/kawashirov/distant-horizons-core/commit/48cb52f2a375aa97c194b51f41b391560814ea6e), as it does nothing but slows rendering. Should boost FPS on most machines.
- [Disabled auto-updates feature](https://github.com/kawashirov/distant-horizons-core/commit/c23495cbe8cf1500e06e10dbadfa32de9ab41d57), to not owervrite this version with official ones.

Please note, that all changes made not in this repo, but in "core" repo as original project split into parts:<br>
https://github.com/kawashirov/distant-horizons-core/compare/main...main-kawashirov

### How to migrate
You don't need to do anything special.

Just delete `.jar` of original mod version and replace with this one.
You can download it on Releases page or build yourself (check original repo above for instructions).

Save file of original version works with this version,
but you need to re-visit parts of the world that was broken,
to re-generate broken LODs.
