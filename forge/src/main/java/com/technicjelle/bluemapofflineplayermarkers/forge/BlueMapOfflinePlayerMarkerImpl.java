package com.technicjelle.bluemapofflineplayermarkers.forge;

import com.mojang.authlib.GameProfile;
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(BlueMapOfflinePlayerMarkers.MOD_ID)
public class BlueMapOfflinePlayerMarkerImpl {

    public  BlueMapOfflinePlayerMarkerImpl() {

        try {
            Class.forName("net.minecraftforge.common.util.FakePlayer"); // Why Forge ?? Why remove it ??? Even NeoForge kept it
            FakeMarkerCommand.createFakePlayer = ((ServerLevel level, GameProfile profile) ->
                    (ServerPlayer) Class.forName("net.minecraftforge.common.util.FakePlayer").getConstructor(level.getClass(), profile.getClass()).newInstance(level, profile)
            );
        }catch (Exception e) {
            FakeMarkerCommand.createFakePlayer = DummyServerPlayer::new;
        }

        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            MinecraftForge.EVENT_BUS.register(new OPMEventHandlerImpl());
        }

        BlueMapOfflinePlayerMarkers.initiate();

    }

}
