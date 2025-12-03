package com.technicjelle.bluemapofflineplayermarkers;

import com.technicjelle.bluemapofflineplayermarkers.struct.OfflinePlayer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Bukkit2Forge {


    public static List<OfflinePlayer> getOfflinePlayers(MinecraftServer server) {

        List<OfflinePlayer> offlinePlayers = new ArrayList<>();
        List<String> playersOnServer = server.getPlayerList().getPlayers().stream().map(Entity::getStringUUID).toList();
        File[] files = new File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID).listFiles();

        if (files == null)
                files = new File[0];
        else
            files = Arrays.stream(files).filter(playerFile -> !playersOnServer.contains(playerFile.getName().replaceFirst("(\\.dat)$", ""))).toArray(File[]::new);

        for (File file : files) {
            offlinePlayers.add(getDataFromFiles(file));
        }

        return offlinePlayers;

    }

    public static OfflinePlayer toOfflinePlayer(ServerPlayer player) {
        return getDataFromFiles(
                new File(
                        player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(),
                        BlueMapOfflinePlayerMarkers.MOD_ID + File.separator + player.getStringUUID() + ".dat"
                )
        );
    }


    public static OfflinePlayer getDataFromFiles(File playerData) {

        try (FileInputStream stream = new FileInputStream(playerData)) {

            CompoundTag nbt = readCompressedNbt(stream);

            return new OfflinePlayer(
                    UUID.fromString(playerData.getName().replaceFirst("(\\.dat)$", "")),
                    nbt.getString("username"),
                    nbt.getLong("lastOnline"),
                    nbt.getList("position", 6).stream().map( tag -> (tag instanceof DoubleTag doubleTag) ? doubleTag.getAsDouble() : 0.0d).toList(),
                    nbt.getString("dimension"),
                    nbt.getInt("gameMode")
            );

        } catch (Exception ignored) {} // EXCEPTION IGNORED FOR NOW

        // It should already be broken by the time it gets here
        return null;

    }

    public static void writePlayerNbt(ServerPlayer player) {

        File offlinePlayers = new File(player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID);

        if (!offlinePlayers.exists()) {
            offlinePlayers.mkdir();
        }

        if (!offlinePlayers.exists())
            return ; // ERROR

        CompoundTag nbt = new CompoundTag();

        nbt.putString("username", player.getName().getString());
        nbt.putLong("lastOnline", System.currentTimeMillis());
        nbt.putString("dimension", player.level().dimension().location().toString());
        nbt.putInt("gameMode", player.gameMode.getGameModeForPlayer().getId());

        ListTag position = new ListTag();

        position.add(DoubleTag.valueOf(player.position().x));
        position.add(DoubleTag.valueOf(player.position().y));
        position.add(DoubleTag.valueOf(player.position().z));

        nbt.put("position", position);

        try {
            writeCompressedNbt(nbt, new File(offlinePlayers, player.getStringUUID() + ".dat"));
        } catch (IOException ignored) {} // EXCEPTION IGNORED FOR NOW

    }

    public static void writeCompressedNbt(CompoundTag tag, File file) throws IOException {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)))) {
                writeTag(tag, dataOutputStream);
            }
        }

    }

    public static void writeTag(@Nullable Tag tag, DataOutput dataOutput) throws IOException {

        if(tag == null)
            return ;

        dataOutput.write(tag.getId());

        if (tag.getId() != 0) {
            dataOutput.writeUTF("");
            tag.write(dataOutput);
        }

    }

    public static CompoundTag readCompressedNbt(@Nullable InputStream stream) throws IOException {

        if (stream == null)
            return null; // May break things

        return readNbt(new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(stream))), NbtAccounter.unlimitedHeap());

    }

    public static CompoundTag readNbt(@Nullable DataInput dataInput, @Nullable NbtAccounter nbtAccounter) throws IOException {

        if (dataInput == null || nbtAccounter == null)
            return null;

        Tag tag = readUnnamedTag(dataInput, nbtAccounter);

        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }

    }

    private static Tag readUnnamedTag(DataInput dataInput, NbtAccounter nbtAccounter) throws IOException {

        byte b0 = dataInput.readByte();
        nbtAccounter.accountBytes(1L);

        if (b0 == 0)
            return EndTag.INSTANCE;

        readUTF(nbtAccounter, dataInput.readUTF());
        nbtAccounter.accountBytes(4L);

        try {
            return TagTypes.getType(b0).load(dataInput, nbtAccounter);
        } catch (IOException exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Loading NBT data");
            CrashReportCategory category = crashreport.addCategory("NBT Tag");
            category.setDetail("Tag type", b0);
            throw new ReportedException(crashreport);
        }
    }

    @Nullable
    public static String readUTF(NbtAccounter nbtAccounter, @Nullable String data) {

        nbtAccounter.accountBytes(16L);

        if (data == null)
            return data;

        int len = data.length();
        int utfLen = 0;

        for (int i = 0; i < len; i++) {

            int c = data.charAt(i);

            if (c >= 1 && c <= 127) {
                utfLen++;
            } else if (c > 2047) {
                utfLen += 3;
            } else {
                utfLen += 2;
            }

        }

        nbtAccounter.accountBytes((8L * utfLen));

        return data;

    }

}
