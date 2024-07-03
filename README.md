# BlueMap Offline Players Marker for forge 1.20.1
Thanks to TechnicJell for publishing the source code of the mod on GitHub, which allowed me to port it to forge for my own server\
A huge thanks to syorito-hatsuki without whom I would have not been able to do this port, as I based myself on his Fabric source code
- See https://github.com/TechnicJelle/BlueMapOfflinePlayerMarkers/tree/main for the original source code
- See https://github.com/syorito-hatsuki/BlueMapOfflinePlayerMarkersFabric for the Fabric source code

# BlueMapOfflinePlayerMarkersForge
A port version of the TechnicJelle's BlueMap Offline Player Markers mod (1.20.1) that was in my files for quite some time.\
I hope it can help the author to port it to forge, as I've seen it was one of the things on his TODO list

## Will this mod be updated?
- **Short answer:** It depends on my needs\
I am actually used to porting mods from one version of Minecraft to another or from fabric to forge / from forge to fabric\
But I only do this to small mods when I need to\
So this mod might go on without updates or with updates, it will depend on my needs and my schedule\
But as this mod seems really simple to maintain, I will try my best to keep it updated, at least for 1-2 years\
Do feel free to update or fork or modify it to your needs (be aware that the [**license**](https://github.com/FLORIAN4600/BlueMapOfflinePlayerMarkersForge/blob/main/LICENSE) requires you to do things like: credit the publishers of the source code (TechnicJell, syorito-hatsuki and FLORIAN4600), put a link to their original code, and copy the license as it was provided by the first repo).

### Edit:
I am actually trying to do a modfile that can support multiple Forge versions\
This is a really bad idea of mine, as I am stuck trying to compare some method parameters with an array of elements, to see if it would fit\
**I have two ways to go:**
- either target specific functions that must not have the same amout of parameters with the same name and return type
- or continue scratching my head and trying a lot of things until I find a way
- *(or just do a file per group of minor versions (like one for 1.20-1.20.2, one for 1.20.3-1.20.4, one for 1.20.5, one for 1.20.6, one for 1.21, and so on and so forth from 1.18 to 1.21)
