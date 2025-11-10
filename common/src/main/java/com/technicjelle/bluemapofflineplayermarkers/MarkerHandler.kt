package com.technicjelle.bluemapofflineplayermarkers

import com.technicjelle.BMUtils
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.logger
import com.technicjelle.bluemapofflineplayermarkers.struct.OfflinePlayer
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.pathString
import kotlin.math.roundToInt


class MarkerHandler {
    fun add(server: MinecraftServer, player: OfflinePlayer) {
        add(
            server,
            player.uuid,
            player.name,
            BlockPos(player.position[0].roundToInt(), player.position[1].roundToInt(), player.position[2].roundToInt()),
            player.dimension,
            player.gameMode,
            System.currentTimeMillis()
        )
    }

    fun add(server: MinecraftServer, player: OfflinePlayer, location: BlockPos, dimension: String, gameMode: Int) {
        add(server, player.uuid, player.name, location, dimension, gameMode, player.lastTimeOnline)
    }

    private fun add(
        server: MinecraftServer,
        uuid: UUID,
        playerName: String,
        location: BlockPos,
        dimension: String,
        gameMode: Int,
        lastPlayed: Long
    ) {
        val optionalApi: Optional<BlueMapAPI> = BlueMapAPI.getInstance()

        if (optionalApi.isEmpty) {
            logger.warn("Tried to add a marker, but BlueMap wasn't loaded!")
            return
        }

        val api: BlueMapAPI = optionalApi.get()

        if (!api.webApp.getPlayerVisibility(uuid)) return

        if (ConfigManager.read().hiddenGameModes.any { it.id == gameMode }) return

        val blueMapWorld: BlueMapWorld = api.getWorld(server.levelKeys().find {
            it.location().toString() == dimension
        }).orElse(null) ?: return

        val markerBuilder: POIMarker.Builder = POIMarker.builder().label(playerName).detail(
            "$playerName <i>(offline)</i><br><bmopm-datetime data-timestamp=$lastPlayed></bmopm-datetime>"
        ).styleClasses("bmopm-offline-player").position(location.x.toDouble(), location.y + 1.8, location.z.toDouble())

        blueMapWorld.maps.forEach {
            markerBuilder.icon(BMUtils.getPlayerHeadIconAddress(api, uuid, it), 0, 0) // centered with CSS instead

            it.markerSets.computeIfAbsent(ConfigManager.read().markerSetName) {
                MarkerSet.builder().label(ConfigManager.read().markerSetName)
                    .toggleable(ConfigManager.read().toggleable).defaultHidden(ConfigManager.read().defaultHidden)
                    .build()
            }.put(uuid.toString(), markerBuilder.build())

        }

        logger.info("Marker for $playerName added")
    }

    fun remove(player: ServerPlayer) = remove(player.uuid, player.name.string)

    fun remove(uuid: UUID, name: String?) {
        val optionalApi = BlueMapAPI.getInstance()

        if (optionalApi.isEmpty) {
            logger.warn("Tried to hide a marker, but BlueMap wasn't loaded!")
            return
        }

        optionalApi.get().maps.forEach { map ->
            map.markerSets[ConfigManager.read().markerSetName]?.remove(uuid.toString())
        }

        logger.info("Marker for ${name ?: uuid.toString()} hidden")
    }

    fun loadOfflineMarkers(server: MinecraftServer) {

        val config: ConfigManager.Config = ConfigManager.read()

        server.getOfflinePlayers().forEach {

            val timeSinceLastPlayed: Long = System.currentTimeMillis() - it.lastTimeOnline

            logger.info("Player " + it.name + " was last seen " + timeSinceLastPlayed + "ms ago")

            val bannedPlayers: List<ConfigManager.BannedPlayer> = ConfigManager.readBannedPlayers()

            // Avoids unnecessary file access + list check
            if ((config.removeBannedPlayerMarkers || config.hideBannedPlayers) && bannedPlayers.any { bannedPlayer -> bannedPlayer.name == it.name || UUID.fromString(bannedPlayer.uuid) == it.uuid }) {

                if(config.removeBannedPlayerMarkers) {

                    // Removing the player's offline marker
                    Paths.get(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toAbsolutePath().pathString, BlueMapOfflinePlayerMarkers.MOD_ID, "${it.uuid}.dat").toFile().delete()

                    logger.info(it.name + "'s Offline Marker has been removed, cause: Player has been Banned ")

                }

                return@forEach

            }

            if (config.expireTimeInHours > 0 && timeSinceLastPlayed > ConfigManager.read().expireTimeInHours * 60 * 60 * 1000) {
                logger.info("Player " + it.name + " was last seen too long ago, skipping")
                return@forEach
            }

            add(server, it)

        }
    }
}