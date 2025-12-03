package com.technicjelle.bluemapofflineplayermarkers.neoforge;

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@Mod(BlueMapOfflinePlayerMarkers.MOD_ID)
public class BlueMapOfflinePlayerMarkerImpl {

    public BlueMapOfflinePlayerMarkerImpl() {

        FakeMarkerCommand.createFakePlayer = FakePlayerFactory::get;

        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            NeoForge.EVENT_BUS.register(OPMEventHandlerImpl.class);
        }

        BlueMapOfflinePlayerMarkers.initiate();

    }

}
