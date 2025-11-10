package com.technicjelle.bluemapofflineplayermarkers.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.markerHandler
import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.translator
import com.technicjelle.bluemapofflineplayermarkers.toOfflinePlayer
import com.technicjelle.bluemapofflineplayermarkers.writePlayerNbt
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.CompletableFuture

class FakeMarkerCommand {

    companion object {

        lateinit var createFakePlayer: (ServerLevel, GameProfile) -> ServerPlayer

        fun register(commandDispatcher: CommandDispatcher<CommandSourceStack?>) {
            commandDispatcher.register(
                Commands.literal("fakeOfflineMarker")
                    .requires { sourceStack: CommandSourceStack -> sourceStack.hasPermission(2) }
                    .then(
                        Commands.argument("target", StringArgumentType.string())
                            .executes { context: CommandContext<CommandSourceStack> -> createMarker(context.source, StringArgumentType.getString(context, "target"))}
                            .suggests { context, builder ->
                                context.source.server.playerList.players.forEach {
                                    builder.suggest(it.name.string)
                                }
                                CompletableFuture.completedFuture(builder.build())
                            }
                            .then(
                                Commands.argument("destination", Vec3Argument.vec3())
                                    .executes { context: CommandContext<CommandSourceStack> -> createMarker(context.source, StringArgumentType.getString(context, "target"), Vec3Argument.getVec3(context, "destination"))}
                                    .then(
                                        Commands.argument("uuid", UuidArgument.uuid())
                                            .executes { context: CommandContext<CommandSourceStack> -> createMarker(context.source, StringArgumentType.getString(context, "target"), Vec3Argument.getVec3(context, "destination"), UuidArgument.getUuid(context, "uuid"))}
                                            .suggests { context, builder ->
                                                val input: String = context.input.split(" ").reversed()[0]
                                                builder.suggest(input+UUID.randomUUID().toString().removeRange(0, input.length))
                                                CompletableFuture.completedFuture(builder.build())
                                            }
                                    )
                            )
                    )
            )
        }

        @Throws(CommandSyntaxException::class)
        private fun createMarker(stack: CommandSourceStack, target: String, position: Vec3 = stack.position, uuid: UUID = UUID.randomUUID()): Int {

            val fake: ServerPlayer = createFakePlayer(stack.level, GameProfile(uuid, target))

            fake.setPos(position)

            fake.writePlayerNbt()

            Thread {
                markerHandler.add(stack.server, fake.toOfflinePlayer())
            }.start()

            stack.sendSuccess({ translator.translateToColoredComponent("bluemapofflineplayermarkers.fake.succeeded", ChatFormatting.GOLD, target, stack.level.dimension().location(), position)}, true)

            return 1

        }

    }

}