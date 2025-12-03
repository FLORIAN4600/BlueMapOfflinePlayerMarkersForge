package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.BMUtils;
import com.technicjelle.bluemapofflineplayermarkers.struct.OfflinePlayer;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.*;

public class MarkerHandler {

    public static void add(MinecraftServer server, OfflinePlayer player) {
        add(
                server,
                player.uuid,
                player.name,
                new BlockPos(player.position.getFirst().intValue(), player.position.getFirst().intValue(), player.position.getFirst().intValue()),
                player.dimension,
                player.gameMode,
                System.currentTimeMillis()
        );
    }

    public static void add(MinecraftServer server, OfflinePlayer player, BlockPos pos, String dimension, int gameMode) {
        add(
                server,
                player.uuid,
                player.name,
                pos,
                dimension,
                gameMode,
                player.lastTimeOnline
        );
    }

    public static void add(
            MinecraftServer server,
            UUID uuid,
            String playerName,
            BlockPos pos,
            String dimension,
            int gameMode,
            long lastPlayed
    ) {
        Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();

        if (optionalApi.isEmpty()) {
            logger.warn(translator.translate("bluemapofflineplayermarkers.info.added.fail.bluemap"));
            return ;
        }

        BlueMapAPI api = optionalApi.get();

        if (!api.getWebApp().getPlayerVisibility(uuid)) {
            logger.debug(translator.translate("bluemapofflineplayermarkers.debug.playerhidden.bluemap", playerName, TelemetryProperty.GameMode.values()[gameMode]));
            return;
        }

        if (ConfigManager.read().hiddenGameModes.stream().anyMatch(hiddenMode -> hiddenMode.ordinal() == gameMode)) {
            if (ENVIRONMENT.equalsIgnoreCase("DEBUG"))
                logger.info(translator.translate("bluemapofflineplayermarkers.debug.playerhidden.gamemode", playerName));
            return;
        }

        Optional<BlueMapWorld> blueMapWorld = api.getWorld(dimension);

        if (blueMapWorld.isEmpty())
        {
            logger.warn(translator.translate("bluemapofflineplayermarkers.info.added.fail.dimension", dimension, playerName));
            return ;
        }

                POIMarker.Builder markerBuilder = POIMarker.builder().label(playerName).detail(
                playerName + " <i>(offline)</i><br><bmopm-datetime data-timestamp=" + lastPlayed + "></bmopm-datetime>"
        ).styleClasses("bmopm-offline-player").position(pos.getX(), pos.getY() + 1.8d, pos.getZ());

        for (BlueMapMap map : blueMapWorld.get().getMaps()) {

            markerBuilder.icon(BMUtils.getPlayerHeadIconAddress(api, uuid, map), 0, 0);
            ConfigManager.Config config = ConfigManager.read();

            map.getMarkerSets().computeIfAbsent(config.markerSetName, key ->
                MarkerSet.builder().label(key).toggleable(config.toggleable).defaultHidden(config.defaultHidden).build()
            ).put(uuid.toString(), markerBuilder.build());

        }

        logger.info(translator.translate("bluemapofflineplayermarkers.info.added.success", playerName));
    }

    public static void remove(ServerPlayer player) {
        remove(player.getUUID(), player.getName().getString());
    }

    public static void remove(UUID uuid, @Nullable String name) {
        Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();

        if (optionalApi.isEmpty()) {
            logger.warn(translator.translate("bluemapofflineplayermarkers.info.removed.fail.bluemap"));
            return ;
        }

        for (BlueMapMap map : optionalApi.get().getMaps()) {
            MarkerSet set = map.getMarkerSets().getOrDefault(ConfigManager.read().markerSetName, null);
            if (set != null)
                set.remove(uuid.toString());
        }

        logger.info(translator.translate("bluemapofflineplayermarkers.info.removed.success", (name == null) ? uuid.toString() : name));
    }

    public static void loadOfflineMarkers(MinecraftServer server) {

        ConfigManager.Config config = ConfigManager.read();

        for (OfflinePlayer offlinePlayer : Bukkit2Forge.getOfflinePlayers(server)) {

            long timeSinceLastPlayed = System.currentTimeMillis() - offlinePlayer.lastTimeOnline;

            logger.info(translator.translate("bluemapofflineplayermarkers.info.lastseen", offlinePlayer.name, timeSinceLastPlayed));

            // Avoids unnecessary file access + list check
            if ((config.removeBannedPlayerMarkers || config.hideBannedPlayers)) {

                if(ConfigManager.readBannedPlayers().stream().anyMatch(bannedPlayer -> (bannedPlayer.name).equals(offlinePlayer.name) || UUID.fromString(bannedPlayer.uuid) == offlinePlayer.uuid)) {

                    if(config.removeBannedPlayerMarkers) {

                        if (deleteByUUID(server, offlinePlayer.uuid))
                            logger.info(translator.translate("bluemapofflineplayermarkers.info.removed.ban", offlinePlayer.name));
                        else
                            logger.warn(translator.translate("bluemapofflineplayermarkers.info.removed.filefail", offlinePlayer.name, offlinePlayer.uuid));

                    }

                    continue ;

                }

            }

            if (config.expireTimeInHours > 0 && timeSinceLastPlayed > (long) ConfigManager.read().expireTimeInHours * 60 * 60 * 1000) {

                if (deleteByUUID(server, offlinePlayer.uuid))
                    logger.info(translator.translate("bluemapofflineplayermarkers.info.removed.expire", offlinePlayer.name));
                else
                    logger.warn(translator.translate("bluemapofflineplayermarkers.info.removed.filefail", offlinePlayer.name, offlinePlayer.uuid));

                continue ;

            }

            add(server, offlinePlayer);

        }
    }

    public static boolean deleteByUUID(MinecraftServer server, UUID uuid) {

        File offlinePlayerSave = Paths.get(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toAbsolutePath().toString(), BlueMapOfflinePlayerMarkers.MOD_ID, uuid.toString() + ".dat").toFile();
        if (offlinePlayerSave.exists())
            return offlinePlayerSave.delete();

        return true;

    }

}
