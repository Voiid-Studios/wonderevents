package voiidstudios.wonderevents.utils;

import org.bukkit.plugin.Plugin;

import voiidstudios.wonderevents.WEBootstrap;

import java.lang.reflect.Array;

public class UniversalFormatter {

    private final Plugin plugin;
    private Boolean hasMiniMessage;
    private Boolean hasLegacySerializer;
    private boolean warned;

    public UniversalFormatter(Plugin plugin) {
        this.plugin = plugin;
    }

    public Object format(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (!hasMiniMessage()) {
            return TextUtils.toLegacy(text);
        }

        try {
            Object component = tryLegacyToComponent(text);
            String miniMessageText = component == null ? text : serializeMiniMessage(component);
            String cleaned = miniMessageText == null ? text : miniMessageText.replace("\\<", "<").replace("\\\\", "");
            Object miniMessageComponent = deserializeMiniMessage(cleaned);

            if (miniMessageComponent != null) {
                return miniMessageComponent;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            warnFallback("El formateador universal no está disponible", exception);
        }

        return TextUtils.toLegacy(text);
    }

    private Object tryLegacyToComponent(String text) throws ReflectiveOperationException {
        if (!hasLegacySerializer()) {
            return null;
        }

        Class<?> legacyClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
        Object serializer = createLegacySerializer(legacyClass);
        return legacyClass.getMethod("deserialize", String.class).invoke(serializer, text.replace('\u00A7', '&'));
    }

    private Object createLegacySerializer(Class<?> legacyClass) throws ReflectiveOperationException {
        Class<?> builderClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer$Builder");
        Object builder = legacyClass.getMethod("builder").invoke(null);

        builderClass.getMethod("character", char.class).invoke(builder, '&');
        invokeIfExists(builderClass, builder, "hexCharacter", char.class, '#');
        invokeIfExists(builderClass, builder, "useUnusualXRepeatedCharacterHexFormat");
        invokeIfExists(builderClass, builder, "hexColors");

        return builderClass.getMethod("build").invoke(builder);
    }

    private String serializeMiniMessage(Object component) throws ReflectiveOperationException {
        Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
        Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
        Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);

        return (String) miniMessageClass.getMethod("serialize", componentClass).invoke(miniMessage, component);
    }

    private Object deserializeMiniMessage(String text) throws ReflectiveOperationException {
        Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
        Object miniMessage = miniMessageClass.getMethod("miniMessage").invoke(null);

        try {
            return miniMessageClass.getMethod("deserialize", String.class).invoke(miniMessage, text);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            return miniMessageClass.getMethod("deserialize", CharSequence.class).invoke(miniMessage, text);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Class<?> tagResolverClass = Class.forName("net.kyori.adventure.text.minimessage.tag.resolver.TagResolver");
            Object emptyResolvers = Array.newInstance(tagResolverClass, 0);
            return miniMessageClass.getMethod("deserialize", String.class, emptyResolvers.getClass())
                    .invoke(miniMessage, text, emptyResolvers);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }

        return null;
    }

    private void invokeIfExists(Class<?> type, Object instance, String methodName) throws ReflectiveOperationException {
        try {
            type.getMethod(methodName).invoke(instance);
        } catch (NoSuchMethodException ignored) {
        }
    }

    private void invokeIfExists(Class<?> type, Object instance, String methodName, Class<?> parameterType, Object value)
            throws ReflectiveOperationException {
        try {
            type.getMethod(methodName, parameterType).invoke(instance, value);
        } catch (NoSuchMethodException ignored) {
        }
    }

    private boolean hasMiniMessage() {
        if (hasMiniMessage == null) {
            hasMiniMessage = classExists("net.kyori.adventure.text.minimessage.MiniMessage")
                    && classExists("net.kyori.adventure.text.Component");
        }
        return hasMiniMessage;
    }

    private boolean hasLegacySerializer() {
        if (hasLegacySerializer == null) {
            hasLegacySerializer = classExists("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
        }
        return hasLegacySerializer;
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private void warnFallback(String message, Throwable throwable) {
        if (warned) {
            return;
        }

        warned = true;
        String warning = message + ", así que uso el formato clásico: "
                + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

        if (plugin instanceof WEBootstrap we && we.getYALogger() != null) {
            we.getYALogger().warning(warning);
        } else {
            plugin.getLogger().warning(warning);
        }
    }
}
