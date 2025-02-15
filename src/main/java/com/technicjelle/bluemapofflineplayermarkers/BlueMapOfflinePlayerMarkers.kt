package com.technicjelle.bluemapofflineplayermarkers

import com.technicjelle.BMUtils
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.MOD_ID
import com.technicjelle.bluemapofflineplayermarkers.forge.OPMEventHandler
import de.bluecolored.bluemap.api.BlueMapAPI
import fr.florian4600.compatutils.CompatibilityUtilities
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.runForDist
import java.util.function.Consumer

@Mod(MOD_ID)
object BlueMapOfflinePlayerMarkers {

    const val MOD_ID = "bluemapofflineplayermarkers"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val compatUtils: CompatibilityUtilities = CompatibilityUtilities(BlueMapOfflinePlayerMarkers.logger)

    public val markerHandler = MarkerHandler()

    init {

        val obj = runForDist(
            clientTarget = {},
            serverTarget = {
                FORGE_BUS.register(OPMEventHandler())
            }
        )

        ConfigManager

    }

    public fun logInfo(message: Any) {
        logger.log(Level.INFO, message.toString())
    }

    public fun logError(message: Any) {
        logger.log(Level.ERROR, message.toString())
    }

    public fun getOnEnableListener(server: MinecraftServer) = Consumer<BlueMapAPI> { api ->
        logInfo("API Ready! BlueMap Offline Player Markers plugin enabled!")

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
        logInfo("API disabled! BlueMap Offline Player Markers shutting down...")
    }
}