package com.technicjelle.bluemapofflineplayermarkers.events

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.MOD_ID
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.getOnEnableListener
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.logger
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.markerHandler
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.onDisableListener
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.translator
import com.technicjelle.bluemapofflineplayermarkers.ConfigManager
import com.technicjelle.bluemapofflineplayermarkers.commands.DeleteMarkerCommand
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand
import com.technicjelle.bluemapofflineplayermarkers.toOfflinePlayer
import com.technicjelle.bluemapofflineplayermarkers.writePlayerNbt
import de.bluecolored.bluemap.api.BlueMapAPI
import fr.florian4600.serverutils.Translatable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer


public class OPMEventHandler {

    companion object {

        private val shouldNotAdd: MutableSet<String> = mutableSetOf()

        fun onServerStarted(server: MinecraftServer) {

            translator = Translatable(logger, server, MOD_ID, "en_us")

            BlueMapAPI.onEnable(getOnEnableListener(server))
            BlueMapAPI.onDisable(onDisableListener)

        }

        public fun onPlayerLogin(player: ServerPlayer) {

            Thread { markerHandler.remove(player) }.start()

        }

        public fun onPlayerLogout(player: ServerPlayer) {

            player.writePlayerNbt()

            if(shouldNotAdd.contains(player.name.string)) {
                shouldNotAdd.remove(player.name.string)
                return
            }

            Thread {
                markerHandler.add(player.server, player.toOfflinePlayer())
            }.start()

        }


        public fun onServerStopping(server: MinecraftServer) {

            BlueMapAPI.unregisterListener(getOnEnableListener(server))
            BlueMapAPI.unregisterListener(onDisableListener)
            logger.info("BlueMap Offline Player Markers plugin disabled!")

        }

        public fun onCommandEvent(parsedResult: ParseResults<CommandSourceStack?>) {

            val tempArgs: ArrayList<String> = arrayListOf()
            val commandArgs: ArrayList<String> = arrayListOf()

            parsedResult.reader.string.split("\"").forEach {
                it.split("\'").forEach { arg ->
                    tempArgs.add(arg)
                }
            }

            tempArgs.forEachIndexed { index, arg ->
                if(index%2 == 0) {
                    arg.split(" ").forEach {
                        if(it.isEmpty()) return@forEach
                        commandArgs.add(it)
                    }
                    return@forEachIndexed
                }
                if(arg.isEmpty()) return@forEachIndexed
                commandArgs.add(arg)
            }

            if(commandArgs.size < 2) return;

            if(commandArgs[0] == "ban-ip" || commandArgs[0] == "ban") {

                shouldNotAdd.add(commandArgs[1])

            }

        }

        public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack?>) {
            DeleteMarkerCommand.register(dispatcher)

            if(ConfigManager.read().advancedMode) {
                FakeMarkerCommand.register(dispatcher)
            }

        }

    }

}
