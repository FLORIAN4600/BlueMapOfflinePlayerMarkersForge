package fr.florian4600.compatutils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"unused", "DeprecatedIsStillUsed"})
public class CompatibilityTests {

    public static class TestBuilder {

        private final ArrayList<TestInstance<?>> tests = new ArrayList<>();
        private Logger logger = LoggerFactory.getLogger(CompatibilityTests.class);

        public TestBuilder() {}

        public static TestBuilder make() {return new TestBuilder();}

        public TestBuilder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public TestBuilder withTest(TestInstance<?> test) {
            tests.add(test);
            return this;
        }

        public void start(Logger logger) {
            this.logger = logger;
            start();
        }

        public void start() {

            for(TestInstance<?> test : tests) {
                test.run(logger);
            }

        }

    }

    public static abstract class TestInstance<T extends TestInstance<T>> {

        protected String message;
        private final ArrayList<TestInstance<?>> orList = new ArrayList<>();
        private final ArrayList<TestInstance<?>> andList = new ArrayList<>();

        public T or(TestInstance<?> test) {
            orList.add(test);
            return getThis();
        }

        public T and(TestInstance<?> test) {
            andList.add(test);
            return getThis();
        }

        protected abstract T getThis();

        protected abstract void writeToFile();

        @Deprecated // DO NOT USE OUTSIDE THE CLASS
        protected abstract boolean run();

        private boolean runRecursive(AtomicReference<ArrayList<String>> successes, AtomicReference<ArrayList<TestInstance<?>>> failures) {

            // Check Test and Sub-Tests
            {
                if (run()) {

                    successes.get().add(message);

                    boolean allWorks = true;

                    for (TestInstance<?> test : andList) {

                        if (test.runRecursive(successes, failures)) {
                            successes.get().add(test.message);
                        } else {
                            failures.get().add(test);
                            allWorks = false;
                        }

                    }

                    if (allWorks) return true;

                } else {
                    failures.get().add(getThis());
                }
            }

            // Check for Alternatives when a failure happens
            return orList.stream().anyMatch(test -> test.runRecursive(successes, failures));

        }

        public void run(Logger logger) {

            AtomicReference<ArrayList<String>> successes = new AtomicReference<>(new ArrayList<>());
            AtomicReference<ArrayList<TestInstance<?>>> failures = new AtomicReference<>(new ArrayList<>());

            if(runRecursive(successes, failures)) {
                successes.get().forEach(logger::debug);
                return;
            }

            failures.get().forEach(failure -> {

                logger.error(failure.message);
                failure.writeToFile();

            });

        }

    }

    public static class MethodTest extends TestInstance<MethodTest> {

        private Class<?> objClass;
        private String className;
        private final String operationName;
        private final ArrayList<String> methodNames;
        private final ArrayList<List<Class<?>>> parameterTypes;
        private final ArrayList<List<String>> parameterTypesName;
        private final ArrayList<String> notFoundClasses;

        public MethodTest(String operationName, List<String> methodNames, List<List<Class<?>>> parameterTypes, List<List<String>> parameterTypesName) {
            this.operationName = operationName;
            this.methodNames = new ArrayList<>(methodNames);
            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.parameterTypesName = new ArrayList<>(parameterTypesName);
            this.notFoundClasses = new ArrayList<>();
        }

        @Deprecated // Needs to have either a class name or a class set, prefer fromNameAndClassName or fromNameAndClass
        public static MethodTest fromNameOnly(String operationName) {
            return new MethodTest(operationName, List.of(), List.of(), List.of());
        }

        public static MethodTest fromNameAndClassName(String operationName, String className) {
            return new MethodTest(operationName, List.of(), List.of(), List.of()).withClassName(className);
        }

        public static MethodTest fromNameAndClass(String operationName, Class<?> objClass) {
            return new MethodTest(operationName, List.of(), List.of(), List.of()).withClass(objClass);
        }

        public MethodTest withClass(Class<?> objClass) {
            this.objClass = objClass;
            return getThis();
        }

        public MethodTest withClassName(String className) {
            this.className = className;
            return getThis();
        }

        public MethodTest withMethodNames(String ...methodNames) {

            this.methodNames.addAll(List.of(methodNames));
            return getThis();

        }

        @SafeVarargs
        public final MethodTest withMultipleParameterTypes(List<Class<?>>... parameterTypes) {

            this.parameterTypes.addAll(List.of(parameterTypes));
            return getThis();

        }

        public MethodTest withParameterTypes(Class<?>... parameterTypes) {

            this.parameterTypes.add(List.of(parameterTypes));
            return getThis();

        }

        @SafeVarargs
        public final MethodTest withMultipleParameterTypesName(List<String>... parameterTypes) {

            this.parameterTypesName.addAll(List.of(parameterTypes));
            return getThis();

        }

        public MethodTest withParameterTypesName(String... parameterTypes) {

            this.parameterTypesName.add(List.of(parameterTypes));
            return getThis();

        }

        @Override
        protected MethodTest getThis() {
            return this;
        }

        @Override
        public boolean run() {

            // ClassName To Class
            if(objClass == null) {

                if(className == null) {
                    message = String.format("%s:   No class defined, MethodTest built wrongly", operationName);
                    return false;
                }

                try {
                    objClass = Class.forName(className);
                }catch (Exception ignored) {
                    message = String.format("%s:   No class found for %s", operationName, className);
                    return false;
                }
            }

            className = objClass.getName();

            // Parameter Names to Types
            for(List<String> typeNames : parameterTypesName) {

                ArrayList<Class<?>> types = new ArrayList<>(typeNames.size());

                for(String typeName : typeNames) {

                    try {
                        types.add(Class.forName(typeName));
                    }catch (Exception ignored) {
                        notFoundClasses.add(typeName);
                    }

                }

                parameterTypes.add(types);

            }

            Method method = null;

            // Method Gathering
            {
                if(parameterTypes.isEmpty()) {
                    method = CompatibilityUtilities.getClassMethod(objClass, methodNames);
                }else {
                    for(List<Class<?>> types : parameterTypes) {

                        try {
                            method = CompatibilityUtilities.getClassMethod(objClass, methodNames, types.toArray(Class[]::new));
                        }catch (Exception ignored) {}

                    }
                }
            }

            // Error checking
            if(method == null) {

                ArrayList<String> parameterNamesList = new ArrayList<>();

                for(List<Class<?>> types : parameterTypes) {
                    parameterNamesList.add(Arrays.toString(types.toArray(Class[]::new)));
                }

                message = String.format("%s:   Failed to find %s(%s) for %s", operationName, Arrays.toString(methodNames.toArray(String[]::new)), Arrays.toString(parameterNamesList.toArray(String[]::new)), objClass.getName());

                if(!notFoundClasses.isEmpty()) {
                    message = String.format("%s\n%s:   Parameter types not found: %s", message, operationName, Arrays.toString(notFoundClasses.toArray()));
                }

                return false;
            }

            // Success
            message = String.format("%s:  Found %s for %s", operationName, method, objClass.getName());
            return true;

        }

        @Override
        protected void writeToFile() {

            File dumpFile = Paths.get("", "logs", "compat-tests", className+".txt").toFile();

            try {

                FileUtils.writeStringToFile(
                        dumpFile,
                        String.format("\n\nOperation:  %s\n  Type: %s\n  Time: %s\n\n", operationName, getThis().getClass().getName(), new Date()),
                        StandardCharsets.UTF_8,
                        true
                );

                // Annoying Errors Printing
                {
                    if(objClass == null) {

                        FileUtils.writeStringToFile(
                                dumpFile,
                                String.format("Error: Class not found: %s\n", className),
                                StandardCharsets.UTF_8,
                                true
                        );

                        return;

                    }

                    for(String notFoundClass : notFoundClasses) {

                        FileUtils.writeStringToFile(
                                dumpFile,
                                String.format("Error: Class not found: %s\n", notFoundClass),
                                StandardCharsets.UTF_8,
                                true
                        );

                    }
                }

                // Alternative Methods Printing
                {
                    String introduction = "Did you meant: \n";

                    for (List<Class<?>> types : parameterTypes) {

                        for (Method method : objClass.getMethods()) {

                            if (ClassUtils.isAssignable(types.toArray(Class[]::new), method.getParameterTypes())) {
                                FileUtils.writeStringToFile(
                                        dumpFile,
                                        String.format("%s  - %s\n", introduction, method),
                                        StandardCharsets.UTF_8,
                                        true
                                );
                                introduction = "";
                            }

                        }

                    }

                    for (String methodName : methodNames) {

                        for (Method method : objClass.getMethods()) {

                            if (method.getName().equals(methodName)) {
                                FileUtils.writeStringToFile(
                                        dumpFile,
                                        String.format("%s  - %s\n", introduction, method),
                                        StandardCharsets.UTF_8,
                                        true
                                );
                                introduction = "";
                            }

                        }

                    }
                }

                // All Methods Printing
                {
                    FileUtils.writeStringToFile(
                            dumpFile,
                            "\nAll Methods: \n",
                            StandardCharsets.UTF_8,
                            true
                    );

                    for(Method method : objClass.getMethods()) {

                        FileUtils.writeStringToFile(
                                dumpFile,
                                String.format("  - %s\n", method),
                                StandardCharsets.UTF_8,
                                true
                        );

                    }
                }

            }catch (IOException e) {
                throw new RuntimeException("Could not write to: "+dumpFile.getAbsolutePath(), e);
            }

        }

    }

    public static class ConstructorTest extends TestInstance<ConstructorTest> {

        private Class<?> objClass;
        private String className;
        private final String operationName;
        private final ArrayList<List<Class<?>>> parameterTypes;

        public ConstructorTest(String operationName, List<List<Class<?>>> parameterTypes) {
            this.operationName = operationName;
            this.parameterTypes = new ArrayList<>(parameterTypes);
        }

        @Override
        protected ConstructorTest getThis() {
            return this;
        }

        @Deprecated // Needs to have either a class name or a class set, prefer fromNameAndClassName or fromNameAndClass
        public static ConstructorTest fromNameOnly(String operationName) {
            return new ConstructorTest(operationName, List.of());
        }

        public static ConstructorTest fromNameAndClassName(String operationName, String className) {
            return new ConstructorTest(operationName, List.of()).withClassName(className);
        }

        public static ConstructorTest fromNameAndClass(String operationName, Class<?> objClass) {
            return new ConstructorTest(operationName, List.of()).withClass(objClass);
        }

        public ConstructorTest withClass(Class<?> objClass) {
            this.objClass = objClass;
            return getThis();
        }

        public ConstructorTest withClassName(String className) {
            this.className = className;
            return getThis();
        }

        public ConstructorTest withParameterTypes(Class<?>... parameterTypes) {

            this.parameterTypes.add(List.of(parameterTypes));
            return getThis();

        }

        public boolean run() {

            if(objClass == null) {

                if(className == null) {
                    message = "No class defined, ConstructorTest built wrongly";
                    return false;
                }
                try {
                    objClass = Class.forName(className);
                }catch (Exception ignored) {
                    message = String.format("No class found for %s", className);
                    return false;
                }

            }

            className = objClass.getName();


            Constructor<?> constructor = null;

            for(List<Class<?>> type : parameterTypes) {

                try {
                    constructor = objClass.getConstructor(type.toArray(Class[]::new));
                }catch (Exception ignored) {}

            }

            if(constructor == null) {

                ArrayList<String> parameterNamesList = new ArrayList<>();

                for(List<Class<?>> type : parameterTypes) {
                    parameterNamesList.add(Arrays.toString(type.toArray(Class[]::new)));
                }

                message = String.format("%s:   Failed to find %s(%s) for %s", operationName, objClass.getName(), Arrays.toString(parameterNamesList.toArray(String[]::new)), objClass.getName());
                return false;
            }

            message = String.format("%s:  Found %s for %s", operationName, constructor, objClass.getName());
            return true;
        }

        @Override
        protected void writeToFile() {

            File dumpFile = Paths.get("", "logs", "compat-tests", className+".txt").toFile();

            try {

                FileUtils.writeStringToFile(
                        dumpFile,
                        String.format("\n\nOperation:  %s\n  Type: %s\n  Time: %s\n\n", operationName, getThis().getClass().getName(), new Date()),
                        StandardCharsets.UTF_8,
                        true
                );

                if(objClass == null) {

                    FileUtils.writeStringToFile(
                            dumpFile,
                            String.format("Error: Class not found: %s\n", className),
                            StandardCharsets.UTF_8,
                            true
                    );

                    return;

                }

                // All Constructors Printing
                {
                    String introduction = "Did you meant: \n";

                    for(Constructor<?> constructor : objClass.getConstructors()) {

                        FileUtils.writeStringToFile(
                                dumpFile,
                                String.format("%s  - %s\n", introduction, constructor),
                                StandardCharsets.UTF_8,
                                true
                        );
                        introduction = "";

                    }

                }

            }catch (IOException e) {
                throw new RuntimeException("Could not write to: "+dumpFile.getAbsolutePath(), e);
            }

        }

    }

}
