package com.technicjelle.bluemapofflineplayermarkers

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.nio.file.Paths

object ConfigManager {
    private val bannedPlayersFile: File = Paths.get("", "banned-players.json").toFile();
    private val configDir: File = Paths.get("", "config", BlueMapOfflinePlayerMarkers.MOD_ID).toFile()
    private val configFile = File(configDir, "config.json")

    private val clientConfigJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        if (!configDir.exists()) configDir.mkdirs()
        configFile.writeText(clientConfigJson.encodeToString(if(configFile.exists()) read() else Config()))
    }

    fun read() = clientConfigJson.decodeFromJsonElement<Config>(Json.parseToJsonElement(configFile.readText()))

    fun readBannedPlayers() = clientConfigJson.decodeFromJsonElement<List<BannedPlayer>>(Json.parseToJsonElement(bannedPlayersFile.readText()))


    @Serializable
    data class BannedPlayer(
        val uuid: String,
        val name: String,
        val created: String,
        val source: String,
        val expires: String,
        val reason: String
    )

    @Serializable
    data class Config(
        val advancedMode: Boolean = false,
        val markerSetName: String = "Offline Players",
        val toggleable: Boolean = true,
        val defaultHidden: Boolean = false,
        val expireTimeInHours: Int = 0,
        val hiddenGameModes: Set<GameMode> = setOf(
            GameMode.SPECTATOR
        ),
        val hideBannedPlayers: Boolean = true,
        val removeBannedPlayerMarkers: Boolean = true
    ) {

        @Serializable
        enum class GameMode(val id: Int) {
            SURVIVAL(0),
            CREATIVE(1),
            ADVENTURE(2),
            SPECTATOR(3)
        }

    }

}
