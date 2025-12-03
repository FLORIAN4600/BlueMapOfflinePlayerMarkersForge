package com.technicjelle.bluemapofflineplayermarkers.neoforge;


import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import com.technicjelle.bluemapofflineplayermarkers.events.OPMEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = BlueMapOfflinePlayerMarkers.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = {Dist.DEDICATED_SERVER})
public class OPMEventHandlerImpl {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {

        OPMEventHandler.onServerStarted(event.getServer());

    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {

        OPMEventHandler.onPlayerLogin((ServerPlayer) ((EntityEvent)event).getEntity());

    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {

        OPMEventHandler.onPlayerLogout((ServerPlayer) ((EntityEvent)event).getEntity());

    }


    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {

        OPMEventHandler.onServerStopping(event.getServer());

    }

    @SubscribeEvent
    public static void onCommandEvent(CommandEvent event) {

        OPMEventHandler.onCommandEvent(event.getParseResults());

    }

    @SubscribeEvent
    public static void onServerTicked(ServerTickEvent.Post event) {

        OPMEventHandler.onServerTicked();

    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        OPMEventHandler.registerCommands(event.getDispatcher());

    }

}
