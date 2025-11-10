package com.technicjelle.bluemapofflineplayermarkers.forge

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.events.OPMEventHandler
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.CommandEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod


@Mod.EventBusSubscriber(modid = BlueMapOfflinePlayerMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.DEDICATED_SERVER])
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
