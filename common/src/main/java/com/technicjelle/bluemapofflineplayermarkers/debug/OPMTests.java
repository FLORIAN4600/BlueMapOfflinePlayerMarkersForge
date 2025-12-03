package com.technicjelle.bluemapofflineplayermarkers.debug;

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import fr.florian4600.compatutils.MinecraftCrossJava;
import fr.florian4600.serverutils.Translatable;
import net.minecraft.server.MinecraftServer;

public class OPMTests {

    public static void runMandatoryTests(MinecraftServer server) {

        languageTest(server);

    }

    public static void runAllTests(MinecraftServer server) {

        BlueMapOfflinePlayerMarkers.logger.error("THIS IS A DEBUG ENVIRONMENT, IF YOU AREN'T SURE OF WHAT IT MEANS, PLEASE CONTACT FLORIAN4600");

        MinecraftCrossJava.checkAll();

        runMandatoryTests(server);

    }

    public static void languageTest(MinecraftServer server) {

        Translatable.testAllModLanguages(server, BlueMapOfflinePlayerMarkers.MOD_ID);

    }

}
