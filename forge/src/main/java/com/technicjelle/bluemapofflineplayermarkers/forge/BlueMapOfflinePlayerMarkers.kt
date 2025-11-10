package com.technicjelle.bluemapofflineplayermarkers.forge

import com.mojang.authlib.GameProfile
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.runForDist

@Mod(BlueMapOfflinePlayerMarkers.MOD_ID)
object BlueMapOfflinePlayerMarkers {
    init {

        FakeMarkerCommand.createFakePlayer = {level: ServerLevel, profile: GameProfile -> DummyServerPlayer(level, profile) }

        val obj = runForDist(
            clientTarget = {},
            serverTarget = {
                FORGE_BUS.register(OPMEventHandler())
            }
        )

        BlueMapOfflinePlayerMarkers

    }
}