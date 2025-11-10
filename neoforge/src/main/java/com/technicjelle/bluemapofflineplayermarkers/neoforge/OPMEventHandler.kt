package com.technicjelle.bluemapofflineplayermarkers.neoforge

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.events.OPMEventHandler
import net.minecraft.server.level.ServerPlayer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.CommandEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent


@EventBusSubscriber(modid = BlueMapOfflinePlayerMarkers.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.DEDICATED_SERVER])
public class OPMEventHandler {

    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {

        OPMEventHandler.onServerStarted(event.server)

    }

    @SubscribeEvent
    public fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {

        OPMEventHandler.onPlayerLogin(event.entity as ServerPlayer)

    }

    @SubscribeEvent
    public fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {

        OPMEventHandler.onPlayerLogout(event.entity as ServerPlayer)

    }


    @SubscribeEvent
    public fun onServerStopping(event: ServerStartedEvent) {

        OPMEventHandler.onServerStopping(event.server)

    }

    @SubscribeEvent
    public fun onCommandEvent(event: CommandEvent) {

        OPMEventHandler.onCommandEvent(event.parseResults)

    }

    @SubscribeEvent
    public fun registerCommands(event: RegisterCommandsEvent) {

        OPMEventHandler.registerCommands(event.dispatcher)

    }

}
