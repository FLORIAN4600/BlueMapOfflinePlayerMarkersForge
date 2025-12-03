package com.technicjelle.bluemapofflineplayermarkers.forge;


import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import com.technicjelle.bluemapofflineplayermarkers.events.OPMEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlueMapOfflinePlayerMarkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = {Dist.DEDICATED_SERVER})
public class OPMEventHandlerImpl {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {

        OPMEventHandler.onServerStarted(event.getServer());

    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {

        OPMEventHandler.onPlayerLogin((ServerPlayer) ((EntityEvent)event).getEntity());

    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {

        OPMEventHandler.onPlayerLogout((ServerPlayer) ((EntityEvent)event).getEntity());

    }


    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {

        OPMEventHandler.onServerStopping(event.getServer());

    }

    @SubscribeEvent
    public void onCommandEvent(CommandEvent event) {

        OPMEventHandler.onCommandEvent(event.getParseResults());

    }

    @SubscribeEvent
    public void onServerTicked(TickEvent event) {

        OPMEventHandler.onServerTicked();

    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {

        OPMEventHandler.registerCommands(event.getDispatcher());

    }

}
