package fr.florian4600.compatutils;

import fr.florian4600.compatutils.CompatibilityTests.MethodTest;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.lang3.ClassUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static fr.florian4600.compatutils.CompatibilityTests.ConstructorTest;
import static fr.florian4600.compatutils.CompatibilityTests.TestBuilder;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public class MinecraftCrossJava {


    /**
     * <b>You should use it if:</b>
     * <ul>
     *     <li><b>you are trying to add more compatibility to this lib</b></li>
     *     <li><b>if you are getting errors</b></li>
     * </ul>
     * <p>
     * Will check if it can run all functions without issues
     */
    public static void checkAll() {

        TestBuilder.make().withTest(
                ConstructorTest.fromNameAndClassName("TextComponent::new", "net.minecraft.network.chat.TextComponent")
                        .withParameterTypes(String.class)
                        .or(
                                MethodTest.fromNameAndClassName("MutableComponent::create", "net.minecraft.network.chat.MutableComponent")
                                        .withMethodNames("create", "m_237204_")
                                        .withParameterTypesName("net.minecraft.network.chat.contents.PlainTextContents")
                                        .and(
                                                MethodTest.fromNameAndClassName("PlainTextContents::create", "net.minecraft.network.chat.contents.PlainTextContents")
                                                        .withMethodNames("create", "m_307377_")
                                                        .withParameterTypes(String.class)
                                        )
                        )
        ).withTest(
                MethodTest.fromNameAndClassName("MutableComponent::withStyle", "net.minecraft.network.chat.MutableComponent")
                        .withMethodNames("withStyle", "m_130938_")
                        .withParameterTypes(UnaryOperator.class)
        ).withTest(
                MethodTest.fromNameAndClassName("MutableComponent::getStyle", "net.minecraft.network.chat.MutableComponent")
                        .withMethodNames("getStyle", "m_7383_")
        ).withTest(
                MethodTest.fromNameAndClassName("MutableComponent::setStyle", "net.minecraft.network.chat.MutableComponent")
                        .withMethodNames("setStyle", "m_6270_", "m_130948_")
                        .withParameterTypes(Style.class)
        ).withTest(
                ConstructorTest.fromNameAndClass("ResourceLocation::new", ResourceLocation.class)
                        .withParameterTypes(String.class, String.class)
                        .or(
                                MethodTest.fromNameAndClass("ResourceLocation::fromNamespaceAndPath", ResourceLocation.class)
                                        .withMethodNames("fromNamespaceAndPath")
                                        .withParameterTypes(String.class, String.class)
                        )
        ).withTest(
                MethodTest.fromNameAndClass("ResourceManager::getResourceStack", ResourceManager.class)
                        .withMethodNames("getResourceStack", "getResources", "m_213829_", "m_7396_")
                        .withParameterTypes(ResourceLocation.class)
        ).withTest(
                MethodTest.fromNameAndClass("ResourceManager::listResources", ResourceManager.class)
                        .withMethodNames("listResources", "m_6540_", "m_214159_") //TODO: IS IT m_214159_ OR m_214160_?
                        .withParameterTypes(String.class, Predicate.class)
        ).withTest(
                MethodTest.fromNameAndClass("ResourceManager::getResource", ResourceManager.class)
                        .withMethodNames("getResource", "m_142591_")
                        .withParameterTypes(ResourceLocation.class)
        ).withTest(
                MethodTest.fromNameAndClass("Resource::open", Resource.class)
                        .withMethodNames("open", "m_6679_", "m_215507_")
        ).start();


    }

    public static class ChatUtils {

        public static <T extends Component> T literal(String content) {

            return CompatibilityUtilities.tryMultiple(
                    () -> (T) Class.forName("net.minecraft.network.chat.TextComponent").getDeclaredConstructor(String.class).newInstance(content),
                    () -> (T) CompatibilityUtilities.getClassMethodAndInvoke(Class.forName("net.minecraft.network.chat.MutableComponent"), null, List.of("create", "m_237204_"),
                            CompatibilityUtilities.getClassMethodAndInvoke(Class.forName("net.minecraft.network.chat.contents.PlainTextContents"), null, List.of("create", "m_307377_"), content))
            );

        }

        public static <T extends Component> T withStyle(T component, UnaryOperator<Style> styleOperator) {

            return CompatibilityUtilities.tryMultiple(
                    () -> setStyle(component, styleOperator.apply(getStyle(component))),
                    () -> (T) CompatibilityUtilities.getClassMethodAndInvoke(Class.forName("net.minecraft.network.chat.MutableComponent"), component, List.of("withStyle", "m_130938_"), styleOperator)
            );

        }

        public static <T extends Component> Style getStyle(T component) {

            try {
                return (Style) CompatibilityUtilities.getClassMethodAndInvoke(Class.forName("net.minecraft.network.chat.MutableComponent"), component, List.of("getStyle", "m_7383_"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        public static <T extends Component> T setStyle(T component, Style style) {

            try {
                return (T) CompatibilityUtilities.getClassMethodAndInvoke(Class.forName("net.minecraft.network.chat.MutableComponent"), component, List.of("setStyle", "m_6270_", "m_130948_"), style);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public static class ResourceUtils {

        public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {

            return CompatibilityUtilities.tryMultiple(
                    () -> ResourceLocation.class.getDeclaredConstructor(String.class, String.class).newInstance(namespace, path),
                    () -> (ResourceLocation) CompatibilityUtilities.getClassMethodAndInvoke(ResourceLocation.class, null, List.of("fromNamespaceAndPath"), namespace, path)
            );

        }

        public static ResourceLocation parse(String location) {

            int colonIndex = location.indexOf(":");

            return fromNamespaceAndPath(colonIndex == -1 ? "minecraft" : location.substring(0, colonIndex-1), location.substring(colonIndex+1));

        }

        public static List<Resource> getResourceStack(ResourceManager manager, ResourceLocation location) {

            return (List<Resource>) CompatibilityUtilities.getClassMethodAndInvoke(ResourceManager.class, manager, List.of("getResourceStack", "getResources", "m_213829_", "m_7396_"), location);

        }

        public static Map<ResourceLocation, Resource> listResources(ResourceManager manager, String string, Predicate<ResourceLocation> filter) {

            Object resources = CompatibilityUtilities.tryMultiple(
                    () -> CompatibilityUtilities.getClassMethodAndInvoke(ResourceManager.class, manager, List.of("listResources", "m_6540_", "m_214159_"), string, filter),
                    () -> CompatibilityUtilities.getClassMethodAndInvoke(ResourceManager.class, manager, List.of("listResources", "m_6540_", "m_214159_"), string, (Predicate<String>) str -> filter.test(parse(string)))
            );

            if(ClassUtils.isAssignable(resources.getClass(), Map.class)) {
                return (Map<ResourceLocation, Resource>) resources;
            }

            if(ClassUtils.isAssignable(resources.getClass(), Collection.class)) {

                HashMap<ResourceLocation, Resource> resourceMap = new HashMap<>();

                for(ResourceLocation location : (Collection<ResourceLocation>) resources) {
                    resourceMap.put(location, getResource(manager, location));
                }

                return resourceMap;

            }

            throw new RuntimeException("listResources is returning an object of unsupported type: "+resources.getClass().getTypeName());

        }

        public static Resource getResource(ResourceManager manager, ResourceLocation location) {

            return (Resource) CompatibilityUtilities.getClassMethodAndInvoke(ResourceManager.class, manager, List.of("getResource", "m_142591_"), location);

        }

        public static InputStream getInputStream(Resource resource) {

            return (InputStream) CompatibilityUtilities.getClassMethodAndInvoke(Resource.class, resource, List.of("open", "m_6679_", "m_215507_"));

        }

    }

}
