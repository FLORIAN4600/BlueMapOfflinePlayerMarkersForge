package com.technicjelle.bluemapofflineplayermarkers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import fr.florian4600.serverutils.Translatable;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers.*;

public class ConfigManager {

    private static final File bannedPlayersFile = Paths.get("", "banned-players.json").toFile();
    private static final File configDir = Paths.get("", "config", MOD_ID).toFile();
    private static final File configFile = new File(configDir, "config.json");

    private static String loadedLanguage = "en_us";
    private static final Gson clientConfigGson = new GsonBuilder().serializeNulls().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.PROTECTED).create();

    private static Config cache = null;

    public static String getLoadedLanguage() {
        return loadedLanguage;
    }

    public static void initiate() {
        if (!configDir.exists())
            configDir.mkdirs();
        write(read());
    }

    public static void write(@Nullable Config config) {

        try {
            Files.write(Path.of(configFile.getAbsolutePath()), clientConfigGson.toJson(config == null ? new Config() : config).getBytes());
        } catch (Exception ignored) {} // EXCEPTION IGNORED FOR NOW

    }

    public static Config read() {

        if (configFile.exists()) {
            try {
                long lastModified = configFile.lastModified();
                if (cache == null || cache.getModifiedTime() < lastModified) {
                    cache = clientConfigGson.fromJson(Files.readString(Path.of(configFile.getAbsolutePath())), Config.class);
                    cache.setModifiedTime(lastModified);
                }
            } catch (Exception ignored) {} // EXCEPTION IGNORED FOR NOW
        }

        if (cache == null) {
            cache = new Config();
            cache.setModifiedTime(new Date().getTime());
            write(cache);
        }

        return cache;

    }

    public static List<BannedPlayer> readBannedPlayers() {

        try {
            return clientConfigGson.fromJson(Files.readString(Path.of(bannedPlayersFile.getAbsolutePath())), TypeToken.getParameterized(List.class, BannedPlayer.class).getType());
        } catch (Exception ignored) {} // EXCEPTION IGNORED FOR NOW

        return List.of();

    }

    public static void checkForLanguageChange(MinecraftServer server) {
        checkForLanguageChange(server, false);
    }

    public static void checkForLanguageChange(MinecraftServer server, Boolean initializeAnyway) {

        Config newConfig = read();

        if (!Objects.equals(newConfig.language, loadedLanguage)) {

            try {

                Translatable.testLanguages(server, MOD_ID, List.of(newConfig.language).toArray(String[]::new));

                Translatable newTranslator = new Translatable(server, MOD_ID, loadedLanguage);

                loadedLanguage = newConfig.language;
                translator = newTranslator;

                logger.error(translator.translate("bluemapofflineplayermarkers.info.language.changed.success", loadedLanguage));

            } catch (Exception e) {

                logger.error(translator.translate("bluemapofflineplayermarkers.info.language.changed.error", newConfig.language, loadedLanguage));
                logger.error("Caused by: {}", e.getMessage());

                newConfig.language = loadedLanguage;
                write(newConfig);

            }

        } else if (initializeAnyway)
            translator = new Translatable(server, MOD_ID, loadedLanguage);

    }

    public static class BannedPlayer {

        public String uuid;
        public String name;
        public String created;
        public String source;
        public String expires;
        public String reason;

        public BannedPlayer(String uuid, String name, String created, String source, String expires, String reason) {
            this.uuid = uuid;
            this.name = name;
            this.created = created;
            this.source = source;
            this.expires = expires;
            this.reason = reason;
        }

    }

    public static class Config {

        protected long lastModified = 0L;
        public boolean advancedMode;
        public String language;
        public String markerSetName;
        public boolean toggleable;
        public boolean defaultHidden;
        public int expireTimeInHours;
        public boolean hideHiddenPlayerMarker;
        public List<GameType> hiddenGameModes;
        public boolean hideBannedPlayers;
        public boolean removeBannedPlayerMarkers;

        public Config() {
            this.advancedMode = false;
            this.language = "en_us";
            this.markerSetName = "Offline Players";
            this.toggleable = true;
            this.defaultHidden = false;
            this.expireTimeInHours = 0;
            this.hideHiddenPlayerMarker = true;
            this.hiddenGameModes = List.of(GameType.SPECTATOR);
            this.hideBannedPlayers = true;
            this.removeBannedPlayerMarkers = true;
        }

        public Config(boolean advancedMode, String markerSetName, boolean toggleable, boolean defaultHidden, int expireTimeInHours, boolean hideHiddenPlayerMarker, List<GameType> hiddenGameModes, boolean hideBannedPlayers, boolean removeBannedPlayerMarkers) {
            this.advancedMode = advancedMode;
            this.markerSetName = markerSetName;
            this.toggleable = toggleable;
            this.defaultHidden = defaultHidden;
            this.expireTimeInHours = expireTimeInHours;
            this.hideHiddenPlayerMarker = hideHiddenPlayerMarker;
            this.hiddenGameModes = hiddenGameModes;
            this.hideBannedPlayers = hideBannedPlayers;
            this.removeBannedPlayerMarkers = removeBannedPlayerMarkers;
        }

        public void setModifiedTime(long time) {
            this.lastModified = time;
        }

        public long getModifiedTime() {
            return this.lastModified;
        }

    }

}
