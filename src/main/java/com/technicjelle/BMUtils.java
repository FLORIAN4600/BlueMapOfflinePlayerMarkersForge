package com.technicjelle;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BMUtils {
    static final String FALLBACK_ICON = "assets/steve.png";

    private BMUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Copies any stream to the BlueMap asset folder.<br>
     * If the resource is a script or style, it will be registered with BlueMap.<br>
     * <b>This function should be called directly inside {@link BlueMapAPI#onEnable(Consumer)}, not in a separate thread.</b>
     *
     * @param blueMapAPI The BlueMapAPI instance
     * @param in         The input stream to copy from
     * @param toAsset    The asset to copy to, relative to BlueMap's asset folder (<code>bluemap/web/assets</code>)
     * @param overwrite  Whether to overwrite the asset if it already exists
     * @throws IOException If the resource could not be copied
     */
    public static void copyStreamToBlueMap(@NotNull BlueMapAPI blueMapAPI, @NotNull InputStream in, @NotNull String toAsset, boolean overwrite) throws IOException {
        Path toPath = blueMapAPI.getWebApp().getWebRoot().resolve("assets").resolve(toAsset);

        //Register script or style
        String assetPath = "assets/" + toAsset;
        if (toAsset.endsWith(".js")) blueMapAPI.getWebApp().registerScript(assetPath);
        if (toAsset.endsWith(".css")) blueMapAPI.getWebApp().registerStyle(assetPath);

        //Copy resource
        if (Files.exists(toPath) && !overwrite) return;
        Files.createDirectories(toPath.getParent());
        try (
                OutputStream out = Files.newOutputStream(toPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            in.transferTo(out);
        }
    }

    /**
     * Copies any file to the BlueMap asset folder.<br>
     * If the resource is a script or style, it will be registered with BlueMap.<br>
     * <b>This function should be called directly inside {@link BlueMapAPI#onEnable(Consumer)}, not in a separate thread.</b>
     *
     * @param blueMapAPI The BlueMapAPI instance
     * @param from       The file to copy
     * @param toAsset    The asset to copy to, relative to BlueMap's asset folder (<code>bluemap/web/assets</code>)
     * @param overwrite  Whether to overwrite the asset if it already exists
     * @throws IOException If the resource could not be found or copied
     */
    public static void copyFileToBlueMap(@NotNull BlueMapAPI blueMapAPI, @NotNull Path from, @NotNull String toAsset, boolean overwrite) throws IOException {
        try (
                InputStream in = Files.newInputStream(from)
        ) {
            copyStreamToBlueMap(blueMapAPI, in, toAsset, overwrite);
        }
    }

    /**
     * Copies a resource from the jar to the BlueMap asset folder.<br>
     * If the resource is a script or style, it will be registered with BlueMap.<br>
     * <b>This function should be called directly inside {@link BlueMapAPI#onEnable(Consumer)}, not in a separate thread.</b>
     *
     * @param blueMapAPI   The BlueMapAPI instance
     * @param classLoader  The class loader to get the resource from the correct jar
     * @param fromResource The resource to copy from the jar
     * @param toAsset      The asset to copy to, relative to BlueMap's asset folder (<code>bluemap/web/assets</code>)
     * @param overwrite    Whether to overwrite the asset if it already exists
     * @throws IOException If the resource could not be found or copied
     */
    public static void copyJarResourceToBlueMap(@NotNull BlueMapAPI blueMapAPI, @NotNull ClassLoader classLoader, @NotNull String fromResource, @NotNull String toAsset, boolean overwrite) throws IOException {
        try (
                InputStream in = classLoader.getResourceAsStream(fromResource)
        ) {
            if (in == null) throw new IOException("Resource not found: " + fromResource);
            copyStreamToBlueMap(blueMapAPI, in, toAsset, overwrite);
        }
    }

    /**
     * Gets the URL to a player head icon for a specific map.<br>
     * If the icon doesn't exist yet, it will be created.
     *
     * @param blueMapAPI The BlueMapAPI instance
     * @param playerUUID The player to get the head of
     * @param blueMapMap The map to get the head for (each map has its own playerheads folder)
     * @return The URL to the player head, relative to BlueMap's web root,<br>or a Steve head if the head couldn't be found
     */
    public static String getPlayerHeadIconAddress(@NotNull BlueMapAPI blueMapAPI, @NotNull UUID playerUUID, @NotNull BlueMapMap blueMapMap) {
        final String assetName = "playerheads/" + playerUUID + ".png";

        try {
            if (!blueMapMap.getAssetStorage().assetExists(assetName)) {
                createPlayerHead(blueMapAPI, playerUUID, assetName, blueMapMap);
            }
        } catch (IOException e) {
            return FALLBACK_ICON;
        }

        return blueMapMap.getAssetStorage().getAssetUrl(assetName);
    }

    /**
     * For when BlueMap doesn't have an icon for this player yet, so we need to make it create one.
     */
    private static void createPlayerHead(@NotNull BlueMapAPI blueMapAPI, @NotNull UUID playerUUID, @NotNull String assetName, @NotNull BlueMapMap map) throws IOException {
        SkinProvider skinProvider = blueMapAPI.getPlugin().getSkinProvider();
        try {
            Optional<BufferedImage> oImgSkin = skinProvider.load(playerUUID);
            if (oImgSkin.isEmpty()) {
                throw new IOException(playerUUID + " doesn't have a skin");
            }

            try (OutputStream out = map.getAssetStorage().writeAsset(assetName)) {
                BufferedImage head = blueMapAPI.getPlugin().getPlayerMarkerIconFactory()
                        .apply(playerUUID, oImgSkin.get());
                ImageIO.write(head, "png", out);
            } catch (IOException e) {
                throw new IOException("Failed to write " + playerUUID + "'s head to asset-storage", e);
            }
        } catch (IOException e) {
            throw new IOException("Failed to load skin for player " + playerUUID, e);
        }
    }
}