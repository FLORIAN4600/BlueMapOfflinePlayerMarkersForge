package fr.florian4600.compatutils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class CompatibilityUtilities {

    private final Logger logger;

    private final boolean IS_DEBUG;
    private final boolean ADVANCED_DEBUG;

    /**
     *
     * @param logger log4j logger to log errors, debug infos and warnings
     * @param isDebug prints out some debugging infos
     * @param advancedDebug defines if we print out only necessary infos (in case of error) or some useful infos that can quickly cram up all your debug file
     */
    public CompatibilityUtilities(Logger logger, boolean isDebug, boolean advancedDebug) {

        IS_DEBUG = isDebug;
        ADVANCED_DEBUG = advancedDebug;
        this.logger = logger;

    }


    public void logDebug(Object message) {
        if(IS_DEBUG) logger.log(Level.DEBUG, message.toString());
    }

    public void logInfo(Object message) {
        logger.log(Level.INFO, message.toString());
    }

    public void logError(Object message) {
        logger.log(Level.ERROR, message.toString());
    }

    public void logWarning(Object message) {
        logger.log(Level.WARN, message.toString());
    }

    /**
     * Invoke methods called by method name.<br>
     * Useful if you are using a method that changes name or parameters from version A to version B.<br>
     *
     * @param obj The Object on which we should call the function/method
     * @param returnType The return type of the function
     * @param methodNames A list of the methods names you want to call
     * @param argsList  A list of the arguments (as a list) you want to pass out to the function (if length is lower than methods length, the last one will be reused)
     * @param local Defines if we check in the direct public methods, or all methods including private, protected and inherited ones
     * @param forceAccess Defines if we try to force invoke it if we have no access (private, protected) over the function/method
     * @throws NoSuchMethodException If none of the given method exists
     * @throws InvocationTargetException If the method called throws an exception
     */
    public Object tryInvokeOrDefault(Object obj, Class<?> returnType, List<String> methodNames, List<List<Object>> argsList, boolean local, boolean forceAccess) throws NoSuchMethodException, InvocationTargetException { // Tries to call (on runtime) not yet defined (on compile time) functions

        for(int i = 0; i < methodNames.size(); i++) {

            String method = methodNames.get(i);

            List<Object> args = i < argsList.size() ? argsList.get(i) : argsList.getFirst();

            Method[] methods = local ? obj.getClass().getDeclaredMethods() : obj.getClass().getMethods(); // declaredMethods are only the one the actual class has set publicly. While as methods is each and every functions the class has either inherited or declared (public and private)

            Method[] filteredMethods = (Method[]) Arrays.stream(methods).filter(m -> {
                printDebugMethodInfos(m, method, args, returnType);
                return m.getName().equals(method) && m.getParameterCount() == args.size() && (m.getReturnType().isInstance(returnType) || returnType.isInstance(m.getReturnType()) || returnType == m.getReturnType() || Arrays.stream(m.getReturnType().getInterfaces()).anyMatch(typeInterface -> returnType == typeInterface));
            }).toArray(); // filter the class functions to see if any matches what I asked

            if(filteredMethods.length > 0) {
                try {
                    if(forceAccess) {
                        filteredMethods[0].setAccessible(true);
                    } else filteredMethods[0].trySetAccessible(); // Try to gently force the function call if asked. If not, brute force its way (probably in a safe manner too)
                    return filteredMethods[0].invoke(obj, args.toArray()); // call the function on the given object, with the given arguments
                }catch (IllegalAccessException exception) {
                    logDebug("Chosen method is inaccessible:");
                    printDebugMethod(i, obj, filteredMethods, args);
                }
            }

            logDebug("Method ${i}: no match");
            logDebug("Did you meant?:");

            for(Method m : methods) {
                if(m.getName().equals(method) || m.getParameterCount() == args.size() && (m.getReturnType().isInstance(returnType) || returnType.isInstance(m.getReturnType()) || returnType == m.getReturnType() || Arrays.stream(m.getReturnType().getInterfaces()).anyMatch(typeInterface -> returnType == typeInterface))) {
                    logDebug("- name: {"+m.getName()+"}, return_type"+m.getReturnType()+"}, args_number: {"+m.getParameterCount()+"}, args_type: {"+ Arrays.toString(m.getParameterTypes()) +"}");
                }
            }

            logDebug("Rest of the List:");

            for(Method m : methods) {
                if(!(m.getName().equals(method) || m.getParameterCount() == args.size() && (m.getReturnType().isInstance(returnType) || returnType.isInstance(m.getReturnType()) || returnType == m.getReturnType() || Arrays.stream(m.getReturnType().getInterfaces()).anyMatch(typeInterface -> returnType == typeInterface)))) {
                    logDebug("- name: {"+m.getName()+"}, return_type"+m.getReturnType()+"}, args_number: {"+m.getParameterCount()+"}, args_type: {"+ Arrays.toString(m.getParameterTypes()) +"}");
                }
            }

        }

        StringBuilder sb = new StringBuilder();

        sb.append("Methods not found: {");

        for(int i = 0; i < methodNames.size(); i++) {

            String method = methodNames.get(i);

            sb.append(method).append("(").append(Arrays.toString((i < argsList.size() ? argsList.get(i) : argsList.getFirst()).toArray())).append(")");

            if(i < methodNames.size()-1) {
                sb.append(" ; ");
            }

        }

        sb.append("}");


        logError("Could not invoke required functions on object: "+obj.getClass().getName());
        throw new NoSuchMethodException(sb.toString());
    }

    public Object tryInvokeOrDefault(Object obj, Class<?> returnType, List<String> methodNames, List<List<Object>> argsList, boolean local) throws NoSuchMethodException, InvocationTargetException {
        return tryInvokeOrDefault(obj, returnType, methodNames, argsList, local, true);
    }

    public Object tryInvokeOrDefault(Object obj, Class<?> returnType, List<String> methodNames, List<List<Object>> argsList) throws NoSuchMethodException, InvocationTargetException {
        return tryInvokeOrDefault(obj, returnType, methodNames, argsList, true, true);
    }


    // Debug functions to easily spot the right functions to use & fix the different mismatch errors
    public void printDebugMethodInfos(Method m, String methodName, List<?> args, Class<?> returnType) {

        if(!IS_DEBUG || !ADVANCED_DEBUG) return;

        logError(m.getName()+"   "+m.getParameterCount()+"   "+m.getReturnType()+"   "+Arrays.toString(m.getParameters()));
        logWarning(methodName+"   "+args.size()+"   "+returnType);
        logWarning(Objects.equals(m.getName(), methodName)+"   "+(m.getParameterCount() == args.size())+"   "+(m.getReturnType().isInstance(returnType) || returnType.isInstance(m.getReturnType()) || returnType == m.getReturnType() || Arrays.stream(m.getReturnType().getInterfaces()).anyMatch(typeInterface -> returnType == typeInterface)));
    }

    public void printDebugMethod(int methodIndex, Object obj, Method[] filteredMethods, List<?> args) {

        if(!IS_DEBUG) return;

        Method chosenMethod = filteredMethods[methodIndex];

        logInfo("Method "+methodIndex+" chosen");
        logInfo(chosenMethod.getName()+"   "+chosenMethod.getParameterCount()+"   "+chosenMethod.getReturnType());
        logError(chosenMethod+"   "+ Arrays.toString(filteredMethods)+"   "+obj.getClass().getName()+"   "+obj+"   "+Arrays.toString(args.toArray()));

    }

    public void debugLogClassStruct(Class<?> obj) {

        logDebug("Constructors:");

        for (Constructor<?> c : obj.getConstructors()) {

            StringBuilder sb = new StringBuilder();

            sb.append(c.getName());
            sb.append("   ");

            for (int i = 0; i < c.getParameterCount(); i++) {

                Parameter p = c.getParameters()[i];

                sb.append("[${i}] ");
                sb.append(p.getName());
                sb.append(";");
                sb.append(p.getType());
                sb.append(";");
                sb.append(p.getClass());
                sb.append(";");
                sb.append(p.getDeclaringExecutable());
                sb.append(";");
                sb.append(p.getAnnotatedType());

            }

            logDebug(sb.toString());
        }

        logDebug("Static Variables");

        for (Field p : obj.getDeclaredFields()) {
            logDebug(p.getName() + "   " + p.getDeclaringClass() + "   " + p.getClass() + "   " + p.getType() + "   " + p.getAnnotatedType());
        }

    }

}
