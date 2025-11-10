package com.technicjelle.bluemapofflineplayermarkers.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.markerHandler
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.translator
import com.technicjelle.bluemapofflineplayermarkers.getOfflinePlayers
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString

class DeleteMarkerCommand {

    companion object {

        private val ERROR: DynamicCommandExceptionType = DynamicCommandExceptionType { playerName: Any? ->
            translator.translateToComponent("bluemapofflineplayermarkers.delete.failed.error", playerName)
        }

        fun register(commandDispatcher: CommandDispatcher<CommandSourceStack?>) {
            commandDispatcher.register(
                Commands.literal("deleteOfflineMarker")
                    .requires { sourceStack: CommandSourceStack -> sourceStack.hasPermission(2) }
                    .then(
                        Commands.argument("target", StringArgumentType.string())
                            .executes { context: CommandContext<CommandSourceStack> -> deleteMarker(context.source, StringArgumentType.getString(context, "target"))}
                            .suggests { context, builder ->
                                context.source.server.getOfflinePlayers().forEach {
                                    builder.suggest(it.name)
                                }
                                CompletableFuture.completedFuture(builder.build())
                            }
                    )
            )
        }

        @Throws(CommandSyntaxException::class)
        private fun deleteMarker(stack: CommandSourceStack, target: String, ): Int {

            var uuid: UUID = UUID.randomUUID()

            if(!stack.server.getOfflinePlayers().any {
                    if(it.name == target) uuid = it.uuid
                    it.name == target
                }) throw ERROR.create(target)

            if(Paths.get(stack.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).pathString, BlueMapOfflinePlayerMarkers.MOD_ID, "$uuid.dat").deleteIfExists()) {
                markerHandler.remove(uuid, target)
                stack.sendSuccess({ translator.translateToColoredComponent("bluemapofflineplayermarkers.delete.succeeded", ChatFormatting.BLUE, target)}, true)
            }else throw ERROR.create(target)

            return 1

        }

    }

}