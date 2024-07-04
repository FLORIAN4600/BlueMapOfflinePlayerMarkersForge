package com.technicjelle.bluemapofflineplayermarkers.forge

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.getOnEnableListener
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.logger
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.markerHandler
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.onDisableListener
import com.technicjelle.bluemapofflineplayermarkers.toOfflinePlayer
import com.technicjelle.bluemapofflineplayermarkers.writePlayerNbt
import de.bluecolored.bluemap.api.BlueMapAPI
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = BlueMapOfflinePlayerMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.DEDICATED_SERVER])
public class OPMEventHandler {

    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {

        BlueMapAPI.onEnable(getOnEnableListener(event.server))
        BlueMapAPI.onDisable(onDisableListener)

    }

    @SubscribeEvent
    public fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {

        if(event.entity !is ServerPlayer) return

        Thread { markerHandler.remove(event.entity as ServerPlayer) }.start()

    }

    @SubscribeEvent
    public fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {

        if(event.entity !is ServerPlayer) return

        val player: ServerPlayer = event.entity as ServerPlayer

        player.writePlayerNbt()

        Thread { markerHandler.add(player.server, player.toOfflinePlayer()) }.start()

    }


    @SubscribeEvent
    public fun onServerStopping(event: ServerStartedEvent) {

        BlueMapAPI.unregisterListener(getOnEnableListener(event.server))
        BlueMapAPI.unregisterListener(onDisableListener)
        logger.info("BlueMap Offline Player Markers plugin disabled!")

    }

}
