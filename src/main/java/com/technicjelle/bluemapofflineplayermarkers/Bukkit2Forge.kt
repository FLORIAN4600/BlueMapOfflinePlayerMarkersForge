package com.technicjelle.bluemapofflineplayermarkers

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource
import java.io.File
import java.io.FileInputStream
import java.util.*

fun MinecraftServer.getOfflinePlayers(): List<OfflinePlayer> = mutableListOf<OfflinePlayer>().apply {

    val playersOnServer: List<String> = playerList.players.map { it.uuid.toString() }

    val offlineFiles: List<File> =
        File(getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID).listFiles()?.filter {
            !playersOnServer.contains(it.name.removeSuffix(".dat"))
        } ?: emptyList()

    if (offlineFiles.isEmpty()) return@apply

    offlineFiles.forEach { file ->
        add(getDataFromFiles(file))
    }
}

fun ServerPlayer.toOfflinePlayer(): OfflinePlayer = getDataFromFiles(
    File(
        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(),
        "${BlueMapOfflinePlayerMarkers.MOD_ID}${File.separator}${stringUUID}.dat"
    )
)

fun getDataFromFiles(offlineData: File): OfflinePlayer {
    FileInputStream(offlineData).use { stream ->
        NbtIo.readCompressed(stream).let { nbt ->
            return OfflinePlayer(
                UUID.fromString(offlineData.name.removeSuffix(".dat")),
                nbt.getString("username"),
                nbt.getLong("lastOnline"),
                nbt.getList("position", 6).map { tag -> (tag as DoubleTag).asDouble },
                nbt.getString("dimension"),
                nbt.getInt("gameMode")
            )
        }
    }
}

fun ServerPlayer.writePlayerNbt() {
    File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID).let {
        if (!it.exists()) it.mkdir()
        if (!it.exists()) it.createNewFile()
        NbtIo.writeCompressed(CompoundTag().apply {
            putString("username", name.string)
            putLong("lastOnline", System.currentTimeMillis())
            put("position", ListTag().apply {
                add(DoubleTag.valueOf(position().x))
                add(DoubleTag.valueOf(position().y))
                add(DoubleTag.valueOf(position().z))
            })
            putString("dimension", level().dimension().location().toString())
            putInt("gameMode", gameMode.gameModeForPlayer.id)
        }, File(it, "$stringUUID.dat"))
    }
}