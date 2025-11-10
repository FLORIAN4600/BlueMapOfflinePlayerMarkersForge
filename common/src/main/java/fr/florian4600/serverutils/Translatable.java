package fr.florian4600.serverutils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class Translatable {

    public HashMap<String, String> translations = new HashMap<>();

    public Translatable(Logger logger, MinecraftServer server, String modId, String lang) {

        String langFile = String.format(Locale.ROOT, "lang/%s.json", lang);
        ResourceManager assetsResources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, server.getResourceManager().listPacks().toList());
        List<Resource> resourceStack = assetsResources.getResourceStack(ResourceLocation.fromNamespaceAndPath(modId, langFile));

        for (Resource resource : resourceStack) {
            try (InputStream stream = resource.open()) {

                JsonElement jsonelement = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonElement.class);
                JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "strings");

                jsonobject.entrySet().forEach(entry -> {
                    String parsedEntry = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]").matcher(GsonHelper.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
                    translations.put(entry.getKey(), parsedEntry);
                });

                IOUtils.closeQuietly(stream);

            } catch(IOException e) {
                logger.error("Failed to read locale data {}", resource.toString(), e);
            }
        }

    }

    public Translatable() {
        // Dummy Translatable
    }

    public String translate(String key, Object ...args) {
        return translations.getOrDefault(key, key).formatted(args);
    }

    public MutableComponent translateToComponent(String key, Object ...args) {
        return Component.literal(translate(key, args));
    }

    public MutableComponent translateToColoredComponent(String key, ChatFormatting color, Object ...args) {
        return translateToComponent(key, args).withStyle(style -> style.withColor(color));
    }

}
