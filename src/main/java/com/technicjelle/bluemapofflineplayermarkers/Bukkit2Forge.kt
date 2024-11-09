package com.technicjelle.bluemapofflineplayermarkers

import net.minecraft.CrashReport
import net.minecraft.ReportedException
import net.minecraft.nbt.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.FastBufferedInputStream
import net.minecraft.world.level.storage.LevelResource
import org.apache.logging.log4j.Level
import java.io.*
import java.lang.reflect.Method
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
            putString("dimension", level.dimension().location().toString())
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

    debugLogClassStruct(Class.forName("net.minecraft.nbt.NbtAccounter"))


    return if(Class.forName("net.minecraft.nbt.NbtAccounter").declaredFields.any { v -> v.name == "f_128917_" }) {
        readNbt(DataInputStream(FastBufferedInputStream(GZIPInputStream(stream))),
            Class.forName("net.minecraft.nbt.NbtAccounter").getDeclaredField("f_128917_").get(null) as NbtAccounter? // Forge 46 has a static final to get an unlimited NbtAccounter
        )
    }else {
        readNbt(DataInputStream(FastBufferedInputStream(GZIPInputStream(stream))),
            Class.forName("net.minecraft.nbt.NbtAccounter").declaredConstructors[0].newInstance(Long.MAX_VALUE, 512) as NbtAccounter? // Forge 49, on the other hand, uses a function. But I decided to use its constructor instead
        )
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
    accountBytes(nbtAccounter, 1L)
    return if (b0.toInt() == 0) {
        EndTag.INSTANCE
    } else {
        nbtAccounter.readUTF(dataInput.readUTF())
        accountBytes(nbtAccounter, 4L)
        try {
            tryInvokeOrDefault(TagTypes.getType(b0.toInt()), Class.forName("net.minecraft.nbt.Tag"), arrayOf("m_7300_", "load"),  arrayOf(arrayOf(dataInput, 0, nbtAccounter), arrayOf(dataInput, nbtAccounter))) // Forge >= 1.20.2 requires an additional argument, as forge < 1.20.2 does not (+ it is not yet calling load)
        } catch (ioexception: IOException) {
            val crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data")
            val crashreportcategory = crashreport.addCategory("NBT Tag")
            crashreportcategory.setDetail("Tag type", b0)
            throw ReportedException(crashreport)
        }
    }
}

fun accountBytes(nbtAccounter: NbtAccounter, bytes: Long) {
    tryInvokeOrDefault(nbtAccounter, Void.TYPE, arrayOf("m_6800_", "m_128926_", "m_263468_"), arrayOf(arrayOf(bytes))) // Will try to account for bytes. First = forge 43, second = forge 46, third = forge 49
}

fun tryInvokeOrDefault(obj: Any, rType: Class<*>, methods: Array<String>, argsList: Array<Array<*>>, local: Boolean = true, forceAccess: Boolean = true): Any? { // Tries to call (on runtime) not yet defined (on compile time) functions

    for((i, method) in methods.withIndex()) {

        val args: Array<*> = if(i < argsList.size) argsList[i] else argsList[0]

        var filteredMethods = if(local) {obj.javaClass.declaredMethods} else {obj.javaClass.methods} // declaredMethods are only the one the actual class has set publicly. While as methods is each and every functions the class has either inherited or declared (public and private)

        filteredMethods = filteredMethods.filter { m ->
            printDebugMethodInfos(m, method, args, rType)
            m.name == method && m.parameterCount == args.size && (m.returnType.isInstance(rType) || rType.isInstance(m.returnType) || rType == m.returnType || m.returnType.interfaces.contains(rType))
        }.toTypedArray() // filter the class functions to see if any matches what I asked

        if(filteredMethods.isNotEmpty()) {
            printDebugMethod(i, obj, filteredMethods, args)
            if(forceAccess) filteredMethods[0].isAccessible = true else filteredMethods[0].trySetAccessible() // Try to gently force the function call if asked. If not, brute force its way (probably in a safe manner too)
            return filteredMethods[0].invoke(obj, *args) // call the function on the given object, with the given arguments
        }

        BlueMapOfflinePlayerMarkers.logDebug("Method ${i}: no match")

    }

    val sb: StringBuilder = StringBuilder()

    sb.append("Methods not found: {")

    for((i, method) in methods.withIndex()) {

        sb.append("${method}(${(if(i < argsList.size) argsList[i] else argsList[0]).contentToString()})")

        if(i < methods.size-1) {
            sb.append(" ; ")
        }

    }

    sb.append("}")


    BlueMapOfflinePlayerMarkers.logError("Could not invoke required functions on object: "+obj.javaClass.name)
    throw NoSuchMethodException(sb.toString())
}


// Debug functions to easily spot the right functions to use & fix the different mismatch errors

fun printDebugMethodInfos(m: Method, methodName: String, args: Array<*>, returnType: Class<*>) {

    if(!BlueMapOfflinePlayerMarkers.IS_DEBUG) return

    BlueMapOfflinePlayerMarkers.logger.log(Level.ERROR, "${m.name}   ${m.parameterCount}   ${m.returnType}   ${m.parameters.contentToString()}")
    BlueMapOfflinePlayerMarkers.logger.log(Level.WARN, "$methodName   ${args.size}   $returnType")
    BlueMapOfflinePlayerMarkers.logger.log(Level.WARN, "${m.name == methodName}   ${m.parameterCount == args.size}   (${m.returnType.isInstance(returnType)} || ${returnType.isInstance(m.returnType)} || ${returnType == m.returnType} || ${m.returnType.interfaces.contains(returnType)})")
}

fun printDebugMethod(methodIndex: Int, obj: Any, filteredMethods: Array<Method>, args: Array<*>) {

    if(!BlueMapOfflinePlayerMarkers.IS_DEBUG) return

    BlueMapOfflinePlayerMarkers.logInfo("Method $methodIndex chosen")
    BlueMapOfflinePlayerMarkers.logInfo("${filteredMethods[0].name}   ${filteredMethods[0].parameterCount}   ${filteredMethods[0].returnType}")
    BlueMapOfflinePlayerMarkers.logError("${filteredMethods[0]}   ${filteredMethods.contentToString()}   ${obj.javaClass.name}   $obj   ${args.contentToString()}")

}

fun debugLogClassStruct(obj: Class<*>) {

    BlueMapOfflinePlayerMarkers.logDebug("Constructors")

    for(c in obj.constructors) {

        val sb = StringBuilder()

        sb.append(c.name)
        sb.append("   ")

        for((i, p) in c.parameters.withIndex()) {

            sb.append("[${i}] ")
            sb.append(p.name)
            sb.append(";")
            sb.append(p.type)
            sb.append(";")
            sb.append(p.javaClass)
            sb.append(";")
            sb.append(p.declaringExecutable)
            sb.append(";")
            sb.append(p.annotatedType)

        }

        BlueMapOfflinePlayerMarkers.logDebug(sb.toString())
    }

    BlueMapOfflinePlayerMarkers.logDebug("Static Variables")

    for(p in obj.declaredFields) {
        BlueMapOfflinePlayerMarkers.logDebug(p.name+"   "+p.declaringClass+"   "+p.javaClass+"   "+p.type+"   "+p.annotatedType)
    }

}