package com.technicjelle.bluemapofflineplayermarkers

import com.technicjelle.BMUtils
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.logger
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapWorld
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*
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

    fun remove(player: ServerPlayer) {
        val optionalApi = BlueMapAPI.getInstance()

        if (optionalApi.isEmpty) {
            logger.warn("Tried to remove a marker, but BlueMap wasn't loaded!")
            return
        }

        optionalApi.get().maps.forEach { map ->
            map.markerSets[ConfigManager.read().markerSetName]?.remove(player.uuid.toString())
        }

        logger.info("Marker for ${player.name.string} removed")
    }

    fun loadOfflineMarkers(server: MinecraftServer) {
        server.getOfflinePlayers().forEach {

            val timeSinceLastPlayed: Long = System.currentTimeMillis() - it.lastTimeOnline

            logger.info("Player " + it.name + " was last seen " + timeSinceLastPlayed + "ms ago")

            if (ConfigManager.read().expireTimeInHours > 0 && timeSinceLastPlayed > ConfigManager.read().expireTimeInHours * 60 * 60 * 1000) {
                logger.info("Player " + it.name + " was last seen too long ago, skipping")
                return@forEach
            }

            add(server, it)

        }
    }
}