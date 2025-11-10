package com.technicjelle.bluemapofflineplayermarkers.neoforge

import com.mojang.authlib.GameProfile
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand
import net.minecraft.server.level.ServerLevel
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.util.FakePlayerFactory
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(value = BlueMapOfflinePlayerMarkers.MOD_ID, dist = [Dist.DEDICATED_SERVER])
object BlueMapOfflinePlayerMarkers {
    init {

        FakeMarkerCommand.createFakePlayer = { level: ServerLevel, profile: GameProfile -> FakePlayerFactory.get(level, profile) }

        val obj = runForDist(
            clientTarget = {},
            serverTarget = {
                FORGE_BUS.register(OPMEventHandler())
            }
        )

        BlueMapOfflinePlayerMarkers

    }
}