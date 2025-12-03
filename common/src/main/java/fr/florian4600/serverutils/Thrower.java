package fr.florian4600.serverutils;

import com.technicjelle.bluemapofflineplayermarkers.BlueMapOfflinePlayerMarkers;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class Thrower {

    public static ArrayList<Throwable> errors = new ArrayList<>();

    public static void appendError(Throwable t) {
        if(!BlueMapOfflinePlayerMarkers.ENVIRONMENT.equalsIgnoreCase("DEBUG"))
            throw new RuntimeException(t);
        errors.add(t);
    }

    public static void checkThrowables(Logger logger) {

        if(errors.isEmpty()) return;

        errors.forEach(t -> logger.error("Caught an unhandled error: ", t));
        logger.warn("Throwing unhandled errors...");

        RuntimeException[] errorList = errors.stream().map(RuntimeException::new).toArray(RuntimeException[]::new);

        throw new RuntimeException(Arrays.toString(errorList));

    }

}
