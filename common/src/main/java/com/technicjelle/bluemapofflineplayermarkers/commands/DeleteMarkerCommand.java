package com.technicjelle.bluemapofflineplayermarkers.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.technicjelle.bluemapofflineplayermarkers.Bukkit2Forge;
import com.technicjelle.bluemapofflineplayermarkers.MarkerHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.MOD_ID;
import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.translator;

public class DeleteMarkerCommand {

    private static final DynamicCommandExceptionType ERROR = new DynamicCommandExceptionType(playerName ->
            translator.translateToComponent("bluemapofflineplayermarkers.command.delete.failed.error", playerName)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deleteOfflineMarker").requires(stack -> stack.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .executes(context -> deleteMarker(context.getSource(), StringArgumentType.getString(context, "target")))
                        .suggests((context, builder) -> {
                            Bukkit2Forge.getOfflinePlayers(context.getSource().getServer()).forEach(player -> builder.suggest(player.name));
                            return CompletableFuture.completedFuture(builder.build());
                        })
                )
        );
    }

    private static int deleteMarker(CommandSourceStack stack, String target) throws CommandSyntaxException {

        AtomicReference<UUID> uuid = new AtomicReference<>(UUID.randomUUID());

        if(Bukkit2Forge.getOfflinePlayers(stack.getServer()).stream().noneMatch(player -> {
            if(player.name.equals(target)) uuid.set(player.uuid);
            return player.name.equals(target);
        })) throw ERROR.create(target);

        try {

            if(!Files.deleteIfExists(Paths.get(String.valueOf(stack.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR)), MOD_ID,  uuid.get()+".dat"))) throw new Exception();

            MarkerHandler.remove(uuid.get(), target);

            stack.sendSuccess(() -> translator.translateToColoredComponent("bluemapofflineplayermarkers.command.delete.succeeded", ChatFormatting.BLUE, target), true);

        } catch (Exception ignored) {
            throw ERROR.create(target);
        }

        return 1;

    }

}