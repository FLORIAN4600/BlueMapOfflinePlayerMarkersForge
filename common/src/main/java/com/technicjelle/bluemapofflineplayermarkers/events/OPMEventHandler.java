package com.technicjelle.bluemapofflineplayermarkers.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.technicjelle.bluemapofflineplayermarkers.Bukkit2Forge;
import com.technicjelle.bluemapofflineplayermarkers.ConfigManager;
import com.technicjelle.bluemapofflineplayermarkers.MarkerHandler;
import com.technicjelle.bluemapofflineplayermarkers.commands.DeleteMarkerCommand;
import com.technicjelle.bluemapofflineplayermarkers.commands.FakeMarkerCommand;
import com.technicjelle.bluemapofflineplayermarkers.debug.OPMTests;
import de.bluecolored.bluemap.api.BlueMapAPI;
import fr.florian4600.serverutils.Thrower;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.*;

public class OPMEventHandler {

    private static final Set<String> shouldNotAdd = new HashSet<>();

    public static void onServerStarted(MinecraftServer server) {

        if (ENVIRONMENT.equalsIgnoreCase("DEBUG")) {
            OPMTests.runAllTests(server);
        } else {
            OPMTests.runMandatoryTests(server);
        }

        ConfigManager.checkForLanguageChange(server, true);

        BlueMapAPI.onEnable(getOnEnableListener(server));
        BlueMapAPI.onDisable(getOnDisableListener());

    }

    public static void onPlayerLogin(ServerPlayer player) {

        new Thread (() -> MarkerHandler.remove(player)).start();

    }

    public static void onPlayerLogout(ServerPlayer player) {

        Bukkit2Forge.writePlayerNbt(player);

        if (shouldNotAdd.contains(player.getName().getString())) {
            shouldNotAdd.remove(player.getName().getString());
            if (ENVIRONMENT.equalsIgnoreCase("DEBUG"))
                logger.info(translator.translate("bluemapofflineplayermarkers.debug.hidden.ban", player.getName().getString()));
            return ;
        }

        new Thread(() ->
                MarkerHandler.add(player.server, Bukkit2Forge.toOfflinePlayer(player))
        ).start();

    }

    public static void onServerStopping(MinecraftServer server) {

        BlueMapAPI.unregisterListener(getOnEnableListener(server));
        BlueMapAPI.unregisterListener(getOnDisableListener());
        ConfigManager.write(ConfigManager.read());
        logger.info(translator.translate("bluemapofflineplayermarkers.info.disabled"));

    }

    public static void onCommandEvent(ParseResults<CommandSourceStack> parsedResult) {

        ArrayList<String> tempArgs = new ArrayList<>();
        ArrayList<String> commandArgs = new ArrayList<>();

        for (String argumentPart : parsedResult.getReader().getString().split("\"")) {
            Collections.addAll(tempArgs, argumentPart.split("\'"));
        }

        for (int i = 0; i < tempArgs.size(); i++) {
            String argument = tempArgs.get(i);
            if (i % 2 == 0) {
                Collections.addAll(commandArgs, Arrays.stream(argument.split(" ")).filter(string -> !string.isEmpty()).toArray(String[]::new));
            }
            if (argument.isEmpty())
                continue;
            commandArgs.add(argument);
        }

        if (commandArgs.isEmpty())
            return;

        if (StringUtils.equalsAny(commandArgs.getFirst(), "fakeOfflineMarker", "deleteOfflineMarker", "reload")) {

            if (parsedResult.getContext().getSource() != null) {
                ConfigManager.checkForLanguageChange(parsedResult.getContext().getSource().getServer());
            }
            return;

        }

        if (commandArgs.size() < 2)
            return;

        if (StringUtils.equalsAny(commandArgs.getFirst(), "ban", "ban-ip")) {
            shouldNotAdd.add(commandArgs.get(1));
        }

    }

    public static void onServerTicked() {

        if(ENVIRONMENT.equalsIgnoreCase("DEBUG")) Thrower.checkThrowables(logger);

    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        DeleteMarkerCommand.register(dispatcher);

        if(ConfigManager.read().advancedMode) {
            FakeMarkerCommand.register(dispatcher);
        }

    }

}
