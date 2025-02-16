package com.technicjelle.bluemapofflineplayermarkers

import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.nbt.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.FastBufferedInputStream
import net.minecraft.world.level.storage.LevelResource
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun MinecraftServer.getOfflinePlayers(): List<OfflinePlayer> = mutableListOf<OfflinePlayer>().apply {

    val playersOnServer: List<String> = playerList.players.map { it.stringUUID }

    val offlineFiles: List<File> =
        File(getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID).listFiles()?.filter {
            !playersOnServer.contains(it.name.removeSuffix(".dat"))
        } ?: emptyList()

    if (offlineFiles.isEmpty()) return@apply

    offlineFiles.forEach { file ->
        add(getDataFromFiles(file))
    }
}

fun ServerPlayer.toOfflinePlayer(): OfflinePlayer = getDataFromFiles(
    File(
        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(),
        "${BlueMapOfflinePlayerMarkers.MOD_ID}${File.separator}${stringUUID}.dat"
    )
)


fun getDataFromFiles(offlineData: File): OfflinePlayer {
    FileInputStream(offlineData).use { stream ->
        readCompressedNbt(stream).let { nbt ->
            return OfflinePlayer(
                UUID.fromString(offlineData.name.removeSuffix(".dat")),
                nbt.getString("username"),
                nbt.getLong("lastOnline"),
                nbt.getList("position", 6).map { tag -> (tag as DoubleTag).asDouble },
                nbt.getString("dimension"),
                nbt.getInt("gameMode")
            )
        }
    }
}

fun ServerPlayer.writePlayerNbt() {
    File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), BlueMapOfflinePlayerMarkers.MOD_ID).let {
        if (!it.exists()) it.mkdir()
        if (!it.exists()) it.createNewFile()
        writeCompressedNbt(CompoundTag().apply {
            putString("username", name.string)
            putLong("lastOnline", System.currentTimeMillis())
            put("position", ListTag().apply {
                add(DoubleTag.valueOf(position().x))
                add(DoubleTag.valueOf(position().y))
                add(DoubleTag.valueOf(position().z))
            })
            putString("dimension", level().dimension().location().toString())
            putInt("gameMode", gameMode.gameModeForPlayer.id)
        }, File(it, "$stringUUID.dat"))
    }
}


@Throws(IOException::class)
fun writeCompressedNbt(tag: CompoundTag, file: File) {
    FileOutputStream(file).use { outputstream ->
        DataOutputStream(BufferedOutputStream(GZIPOutputStream(outputstream))).use { dataoutputstream ->
            writeTag(tag, dataoutputstream)
        }
    }
}

@Throws(IOException::class)
fun writeTag(tag: Tag?, dataOutput: DataOutput) {

    if(tag == null) return

    dataOutput.writeByte(tag.id.toInt())
    if (tag.id.toInt() != 0) {
        dataOutput.writeUTF("")
        tag.write(dataOutput)
    }
}

@Throws(IOException::class)
fun readCompressedNbt(stream: InputStream?): CompoundTag {

    try {
        return readNbt(DataInputStream(FastBufferedInputStream(GZIPInputStream(stream))),
            NbtAccounter.unlimitedHeap()
        )
    }catch(exception: IOException) {
        throw exception
    }

}


@Throws(IOException::class)
fun readNbt(dataInput: DataInput?, nbtAccounter: NbtAccounter?): CompoundTag {
    val tag = dataInput?.let { nbtAccounter?.let { it1 -> readUnnamedTag(it, it1) } }
    if (tag is CompoundTag) {
        return tag
    } else {
        throw IOException("Root tag must be a named compound tag")
    }
}


@Throws(IOException::class)
private fun readUnnamedTag(dataInput: DataInput, nbtAccounter: NbtAccounter): Any? {
    val b0 = dataInput.readByte()
    nbtAccounter.accountBytes(1L)
    return if (b0.toInt() == 0) {
        EndTag.INSTANCE
    } else {
        readUTF(nbtAccounter, dataInput.readUTF())
        nbtAccounter.accountBytes(4L)
        try {
            TagTypes.getType(b0.toInt()).load(dataInput, nbtAccounter)
        } catch (ioexception: IOException) {
            val crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data")
            val crashreportcategory = crashreport.addCategory("NBT Tag")
            crashreportcategory.setDetail("Tag type", b0)
            throw ReportedException(crashreport)
        }
    }
}

fun readUTF(nbtAccounter: NbtAccounter, data: String?): String? {
    nbtAccounter.accountBytes(16L)
    if (data == null) {
        return data
    } else {
        val len = data.length
        var utflen = 0

        for (i in 0 until len) {
            val c = data[i].code
            if (c in 1..127) {
                ++utflen
            } else if (c > 2047) {
                utflen += 3
            } else {
                utflen += 2
            }
        }

        nbtAccounter.accountBytes((8 * utflen).toLong())
        return data
    }
}