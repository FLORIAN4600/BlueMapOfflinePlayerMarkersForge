package com.technicjelle.bluemapofflineplayermarkers.commands;


import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.technicjelle.bluemapofflineplayermarkers.Bukkit2Forge;
import com.technicjelle.bluemapofflineplayermarkers.MarkerHandler;
import fr.florian4600.serverutils.Thrower;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.FailableBiFunction;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.translator;

public class FakeMarkerCommand {

    public static FailableBiFunction<ServerLevel, GameProfile, ServerPlayer, Exception> createFakePlayer;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fakeOfflineMarker").requires( stack -> stack.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .executes(context -> createMarker(context.getSource(), StringArgumentType.getString(context, "target")))
                        .suggests((context, builder) -> {
                            context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> builder.suggest(player.getName().getString()));
                            return CompletableFuture.completedFuture(builder.build());
                        })
                        .then(Commands.argument("destination", Vec3Argument.vec3())
                                .executes(context -> createMarker(context.getSource(), StringArgumentType.getString(context, "target"), Vec3Argument.getVec3(context, "destination")))
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .executes(context -> createMarker(context.getSource(), StringArgumentType.getString(context, "target"), Vec3Argument.getVec3(context, "destination"), UuidArgument.getUuid(context, "uuid")))
                                        .suggests((context, builder) -> {
                                            String input = Lists.reverse(Arrays.stream(context.getInput().split(" ")).toList()).get(0);
                                            builder.suggest(input+ UUID.randomUUID().toString().replaceFirst(String.format(".{%s}", input.length()+1), ""));
                                            return CompletableFuture.completedFuture(builder.build());
                                        })
                                )
                        )
                )
        );
    }

    private static int createMarker(CommandSourceStack stack, String target) {
        return createMarker(stack, target, stack.getPosition());
    }

    private static int createMarker(CommandSourceStack stack, String target, Vec3 position) {
        return createMarker(stack, target, position, UUID.randomUUID());
    }

    private static int createMarker(CommandSourceStack stack, String target, Vec3 position, UUID uuid)  {

        try {
            ServerPlayer fake = createFakePlayer.apply(stack.getLevel(), new GameProfile(uuid, target));

            fake.setPos(position);

            Bukkit2Forge.writePlayerNbt(fake);

            new Thread(
                    () -> MarkerHandler.add(stack.getServer(), Bukkit2Forge.toOfflinePlayer(fake))
            ).start();

            stack.sendSuccess(() -> translator.translateToColoredComponent("bluemapofflineplayermarkers.command.fake.succeeded", ChatFormatting.GOLD, target, stack.getLevel().dimension().location(), position), true);

        }catch (Exception e) {
            Thrower.appendError(e);
        }

        return 1;

    }

}