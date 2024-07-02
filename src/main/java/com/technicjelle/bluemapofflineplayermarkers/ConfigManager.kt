package com.technicjelle.bluemapofflineplayermarkers

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths

object ConfigManager {
    private val configDir: File = Paths.get("", "config", BlueMapOfflinePlayerMarkers.MOD_ID).toFile()
    private val configFile = File(configDir, "config.json")

    private val clientConfigJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        if (!configDir.exists()) configDir.mkdirs()
        if (!configFile.exists()) configFile.writeText(clientConfigJson.encodeToString(Config()))
    }

    fun read() = clientConfigJson.decodeFromString<Config>(configFile.readText())

    @Serializable
    data class Config(
        val markerSetName: String = "Offline Players",
        val toggleable: Boolean = true,
        val defaultHidden: Boolean = false,
        val expireTimeInHours: Int = 0,
        val hiddenGameModes: Set<GameMode> = setOf(
            GameMode.SPECTATOR
        )
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
