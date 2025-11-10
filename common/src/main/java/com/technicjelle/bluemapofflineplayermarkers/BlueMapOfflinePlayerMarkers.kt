package com.technicjelle.bluemapofflineplayermarkers

import com.technicjelle.BMUtils
import de.bluecolored.bluemap.api.BlueMapAPI
import fr.florian4600.compatutils.CompatibilityUtilities
import fr.florian4600.serverutils.Translatable
import net.minecraft.server.MinecraftServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.function.Consumer

object BlueMapOfflinePlayerMarkers {

    const val MOD_ID = "bluemapofflineplayermarkers"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    // Both booleans should be false in release state, please do tell me if I forgot to switch them back in any release source code
    val compatUtils: CompatibilityUtilities = CompatibilityUtilities(logger, false, false)

    var translator: Translatable = Translatable();

    public val markerHandler = MarkerHandler()

    init {

        ConfigManager

    }

    public fun getOnEnableListener(server: MinecraftServer) = Consumer<BlueMapAPI> { api ->
        logger.info("API Ready! BlueMap Offline Player Markers plugin enabled!")

        runCatching {
            BMUtils.copyJarResourceToBlueMap(
                api, javaClass.classLoader, "assets/technicjelle/style.css", "bmopm.css", false
            )
            BMUtils.copyJarResourceToBlueMap(
                api, javaClass.classLoader, "assets/technicjelle/script.js", "bmopm.js", false
            )
        }.onFailure { logger.trace("Failed to copy resources to BlueMap webapp!", it) }

        Thread { markerHandler.loadOfflineMarkers(server) }.start()
    }

    val onDisableListener = Consumer<BlueMapAPI> {
        logger.info("API disabled! BlueMap Offline Player Markers shutting down...")
    }
}