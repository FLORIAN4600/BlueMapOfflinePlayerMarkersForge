package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.BMUtils;
import de.bluecolored.bluemap.api.BlueMapAPI;
import fr.florian4600.serverutils.Translatable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.Consumer;

public class BlueMapOfflinePlayerMarkers {

    public static final String MOD_ID = "bluemapofflineplayermarkers";
    public static String ENVIRONMENT;

    public static final Logger logger = LogManager.getLogger(MOD_ID);
    public static Translatable translator = new Translatable();

    public record Test(ServerPlayer player) {}

    public static void initiate() {

        try (InputStream stream = BlueMapOfflinePlayerMarkers.class.getResourceAsStream("/META-INF/ENVIRONMENT")) {
            ENVIRONMENT = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream))).readLine();
        }catch (Exception e) {
            throw new RuntimeException("COULD NOT LOAD BMOPM ENVIRONMENT", e);
        }

        ConfigManager.initiate();

    }

    public static Consumer<BlueMapAPI> getOnEnableListener(MinecraftServer server) {

        return api -> {
            logger.info(translator.translate("bluemapofflineplayermarkers.info.enabled"));

            try {
                BMUtils.copyJarResourceToBlueMap(api, BlueMapOfflinePlayerMarkers.class.getClassLoader(), "assets/technicjelle/style.css", "bmopm.css", false);
                BMUtils.copyJarResourceToBlueMap(api, BlueMapOfflinePlayerMarkers.class.getClassLoader(), "assets/technicjelle/script.js", "bmopm.js", false);
            }catch (Exception e) {
                logger.trace("Failed to copy resources to BlueMap webapp!", e);
            }

            new Thread(() -> MarkerHandler.loadOfflineMarkers(server)).start();
        };

    }

    public static Consumer<BlueMapAPI> getOnDisableListener() {
        return api -> logger.info(translator.translate("bluemapofflineplayermarkers.info.shuttingdown"));
    }
}
