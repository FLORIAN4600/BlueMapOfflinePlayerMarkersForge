package fr.florian4600.serverutils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.florian4600.compatutils.MinecraftCrossJava;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <b>ANY USE OF THIS CLASS ON UNSUPPORTED MC VERSION MIGHT THROW RUNTIME EXCEPTIONS</b><p>
 * - Current support: <b>Minecraft 1.18.2 to 1.21.4</b><p>
 */
public class Translatable {

    public HashMap<String, String> translations = new HashMap<>();

    public Translatable(MinecraftServer server, String modId, String lang) {

        MinecraftCrossJava.checkAll();

        String langFile = String.format(Locale.ROOT, "lang/%s.json", lang);
        ResourceManager assetsResources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, server.getResourceManager().listPacks().toList());

        List<Resource> resourceStack = MinecraftCrossJava.ResourceUtils.getResourceStack(assetsResources, MinecraftCrossJava.ResourceUtils.fromNamespaceAndPath(modId, langFile));

        for (Resource resource : resourceStack) {

            try (InputStream stream = MinecraftCrossJava.ResourceUtils.getInputStream(resource)) {

                JsonElement jsonelement = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonElement.class);
                JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "strings");

                jsonobject.entrySet().forEach(entry -> {
                    String parsedEntry = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]").matcher(GsonHelper.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
                    translations.put(entry.getKey(), parsedEntry);
                });

                IOUtils.closeQuietly(stream);

            } catch(Exception e) {
                throw new RuntimeException("Failed to read locale data "+resource.toString(), e);
            }
        }

    }

    public Translatable() {
        // Dummy Instance
    }

    public String translate(String key, Object ...args) {

        String out = translations.getOrDefault(key, key);
        Matcher matcher = Pattern.compile("(%s|%un|%b|%c|%d|%e|%f|%g|%h|%n|%o|%t|%x)").matcher(out);

        int groupCount = 0;

        while (matcher.find()) groupCount++;

        if(groupCount > args.length) {

            StringBuilder newOut = new StringBuilder();
            ArrayList<Object> newArgs = new ArrayList<>(Arrays.stream(args).toList());
            int lastEnd = 0;
            int replaceCount = 0;

            while (matcher.find()) {
                replaceCount++;
                if(replaceCount > args.length) {
                    newOut.append(out, lastEnd, matcher.start());
                    newOut.append("%s");
                    newArgs.add(out.substring(matcher.start(), matcher.end()));
                    lastEnd = matcher.end();
                }
            }

            out = newOut.toString();
            args = newArgs.toArray();
        }

        return out.formatted(args);
    }

    public <T extends Component> T translateToComponent(String key, Object ...args) {

        return MinecraftCrossJava.ChatUtils.literal(translate(key, args));

    }

    public <T extends Component> T translateToColoredComponent(String key, ChatFormatting color, Object ...args) {
        return MinecraftCrossJava.ChatUtils.withStyle(translateToComponent(key, args), style -> style.withColor(color));
    }


    /**
     * This function fetches all the loaded language files for your mod (could be by you, other mods, or datapacks)
     * @param server  minecraft server, must be non null
     * @param namespace you mod id
     * @return A <code>String[]</code> of all the loaded languages
     */
    @NotNull
    public static String[] listLanguages(@NotNull MinecraftServer server, @NotNull String namespace) {

        ResourceManager assetsResources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, server.getResourceManager().listPacks().toList());

        Set<ResourceLocation> langSet = MinecraftCrossJava.ResourceUtils.listResources(assetsResources, "lang", resourceLocation -> resourceLocation.getNamespace().equals(namespace)).keySet();

        return langSet.stream().map(location -> location.getPath().replaceFirst("lang/", "").replaceFirst("(?s)(.*)(\\.json)", "$1")).toArray(String[]::new);

    }


    /**
     * This is a basic test to check if all the necessary languages work<p>
     * <b>There should be for every</b> <code>lang</code> of <code>languages</code><b>:</b><p>
     * <ul>
     *     <li><b>a file</b> (<code>-modid-/assets/-lang-</code>)
     *     <ul><li><b>containing</b> <code>"-modid-.debug.langtest": "-lang- Translator test: %s %s, %s"</code></li></ul>
     *     </li>
     * </ul><br>
     * @param server minecraft server, must be non null
     * @param namespace your mod id
     * @param languages the languages you want to test
     * @throws RuntimeException on wrongly set up Translatable class or language file
     */
    public static void testLanguages(@NotNull MinecraftServer server, @NotNull String namespace, @NotNull String[] languages) {

        for(String lang: languages) {

            String[] testingArgs = {"Hi, I'm", "FLORIAN4600", "Nice to meet you!!"};
            String expectedOutput = String.format(lang+" Translator test: %s %s, %s", (Object[]) testingArgs);

            Translatable translatable = new Translatable(server, namespace, lang);
            String translation = translatable.translate(namespace+".debug.langtest", (Object[]) testingArgs);


            if(!translation.equals(expectedOutput)) {
                String error = String.format("Mismatched strings, program was expecting: {(file): \"%s.debug.langtest\": \"%s Translator test: %s\"; (result): \"%s\"}, but found instead: {(file): \"%s.debug.langtest\": \"%s\"; (result): \"%s\"}",
                        namespace, lang, "%s %s, %s", // expected json
                        expectedOutput, // expected output
                        namespace, translatable.translate(namespace+".debug.langtest"), // resulted json
                        translation // resulted output
                );
                throw new RuntimeException("Translator could not initialize "+lang, new RuntimeException(error));
            }

        }

    }


    /**
     * This is a basic test to check if all the loaded language files are properly set up.<p>
     * <b>There should be for every language file</b> (for the language: <code>lang</code>)<b>:</b>
     * <ul>
     *     <li><b>a file</b> (<code>-modid-/assets/-lang-</code>)
     *     <ul><li><b>containing</b> <code>"-modid-.debug.langtest": "-lang- Translator test: %s %s, %s"</code></li></ul>
     *     </li>
     * </ul><br>
     * @param server minecraft server, must be non null
     * @param namespace your mod id
     * @throws RuntimeException on wrongly set up Translatable class or language file
     */
    public static void testAllModLanguages(@NotNull MinecraftServer server, @NotNull String namespace) {

        testLanguages(server, namespace, listLanguages(server, namespace));

    }

}
