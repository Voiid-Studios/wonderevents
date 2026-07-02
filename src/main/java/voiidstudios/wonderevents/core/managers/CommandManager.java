package voiidstudios.wonderevents.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import voiidstudios.wonderevents.api.ZFCommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;
import voiidstudios.wonderevents.expansions.ExpansionDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CommandManager {
    private final PluginContext context;
    private final List<ZFCommand> commands = new ArrayList<>();
    private final Map<ZFCommand, RuntimeRegistration> runtimeCommands = new IdentityHashMap<>();
    private boolean coreCommandsLoaded;

    public CommandManager(PluginContext context) {
        this.context = context;
    }

    public void loadCoreCommands() {
        if (coreCommandsLoaded) {
            return;
        }

        registerCoreCommand(new BaseCommand(
                "help",
                "Shows the available WonderEvents commands.",
                "wonderevents.admin",
                (sender, args) -> {
                    MessagesManager messages = context.getMessagesManager();
                    Map<String, String> header = new HashMap<>();
                    header.put("%VERSION%", context.getPlugin().getDescription().getVersion());
                    messages.send(sender, "command.help.header", header);

                    for (ZFCommand command : commands) {
                        Map<String, String> line = new HashMap<>();
                        line.put("%COMMAND%", getPrimaryName(command));
                        line.put("%DESCRIPTION%", Objects.toString(command.getDescription(), ""));
                        messages.send(sender, "command.help.line", line);
                    }
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "status",
                "Shows a small runtime summary of the core.",
                "wonderevents.admin.status",
                (sender, args) -> {
                    MessagesManager messages = context.getMessagesManager();
                    messages.send(sender, "command.status.header");

                    Map<String, String> values = new HashMap<>();
                    values.put("%EXPANSIONS%", String.valueOf(getExpansionCount()));
                    values.put("%ADDONS%", String.valueOf(getAddonCount()));
                    values.put("%COMMANDS%", String.valueOf(getLoadedCommandCount()));
                    values.put("%MODULES%", String.valueOf(context.getModuleManager().getLoadedModuleCount()));
                    values.put("%LISTENERS%", String.valueOf(context.getEventManager().getRegisteredListenerCount()));

                    messages.send(sender, "command.status.expansions", values);
                    messages.send(sender, "command.status.addons", values);
                    messages.send(sender, "command.status.commands", values);
                    messages.send(sender, "command.status.modules", values);
                    messages.send(sender, "command.status.listeners", values);
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "reload",
                "Reloads configs, expansions and addons.",
                "wonderevents.admin.reload",
                (sender, args) -> {
                    context.getPlugin().reloadWonderEvents(sender);
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "addons",
                "Lists all loaded addons.",
                "wonderevents.admin.addons",
                (sender, args) -> {
                    MessagesManager messages = context.getMessagesManager();
                    List<?> descriptors = context.getAddonManager() == null ? Collections.emptyList() : context.getAddonManager().getLoadedDescriptors();
                    if (descriptors.isEmpty()) {
                        messages.send(sender, "command.addons.empty");
                        return true;
                    }

                    messages.send(sender, "command.addons.header");
                    for (Object object : descriptors) {
                        voiidstudios.wonderevents.addons.MagicAddonDescriptor descriptor =
                                (voiidstudios.wonderevents.addons.MagicAddonDescriptor) object;
                        Map<String, String> line = new HashMap<>();
                        line.put("%NAME%", descriptor.getName());
                        line.put("%VERSION%", descriptor.getVersion());
                        line.put("%AUTHOR%", descriptor.getAuthor().isBlank() ? "Unknown" : descriptor.getAuthor());
                        messages.send(sender, "command.addons.line", line);
                    }
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "expansions",
                "Lists all loaded expansions.",
                "wonderevents.admin.expansions",
                (sender, args) -> {
                    MessagesManager messages = context.getMessagesManager();
                    List<ExpansionDescriptor> descriptors = context.getExpansionManager() == null ? Collections.emptyList() : context.getExpansionManager().getLoadedDescriptors();
                    if (descriptors.isEmpty()) {
                        messages.send(sender, "command.expansions.empty");
                        return true;
                    }

                    messages.send(sender, "command.expansions.header");
                    for (ExpansionDescriptor descriptor : descriptors) {
                        Map<String, String> line = new HashMap<>();
                        line.put("%ID%", descriptor.getId());
                        line.put("%NAME%", descriptor.getName());
                        line.put("%VERSION%", descriptor.getVersion());
                        messages.send(sender, "command.expansions.line", line);
                    }
                    return true;
                },
                (sender, args) -> List.of()
        ));

        coreCommandsLoaded = true;
    }

    public boolean registerAddonCommand(ZFCommand command, WonderManifest manifest) {
        if (command == null) {
            return false;
        }

        if (isSubcommandSlotOccupied(command)) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] Ya existe un subcomando con el nombre o alias '" + command.getName() + "'. El comando seguirá disponible como comando nativo si Bukkit lo permite.");
        } else {
            commands.add(command);
        }

        registerRuntimeCommand(command, manifest);
        return true;
    }

    public void registerAddonCommand(ZFCommand command) {
        registerAddonCommand(command, null);
    }

    public void unregisterAddonCommand(ZFCommand command) {
        if (command == null) {
            return;
        }
        commands.remove(command);
        unregisterRuntimeCommand(command);
    }

    public List<ZFCommand> getSubcommands() {
        return Collections.unmodifiableList(commands);
    }

    public int getLoadedCommandCount() {
        return commands.size();
    }

    public ZFCommand findCommand(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        for (ZFCommand command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
            for (String alias : command.getAliases()) {
                if (alias != null && alias.equalsIgnoreCase(name)) {
                    return command;
                }
            }
        }
        return null;
    }

    public List<String> getCommandSuggestions(String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (ZFCommand command : commands) {
            addIfMatches(suggestions, command.getName(), normalized);
            for (String alias : command.getAliases()) {
                addIfMatches(suggestions, alias, normalized);
            }
        }
        return suggestions;
    }

    public PluginContext getContext() {
        return context;
    }

    private void registerCoreCommand(ZFCommand command) {
        if (command != null) {
            commands.add(command);
        }
    }

    private void registerRuntimeCommand(ZFCommand command, WonderManifest manifest) {
        CommandMap commandMap = resolveCommandMap();
        if (commandMap == null) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] No pude obtener el CommandMap de Bukkit para registrar '" + command.getName() + "'.");
            return;
        }

        WonderManifest.CommandDefinition definition = manifest == null ? null : manifest.getCommand(command.getName());
        String name = safe(command.getName(), "unknown");
        String description = firstNonBlank(definition == null ? null : definition.getDescription(), command.getDescription(), "");
        String usage = firstNonBlank(definition == null ? null : definition.getUsage(), "/" + name);
        String permission = firstNonBlank(definition == null ? null : definition.getPermission(), command.getPermission(), "");
        List<String> aliases = definition != null && !definition.getAliases().isEmpty() ? definition.getAliases() : command.getAliases();
        aliases = sanitizeAliases(name, aliases);

        RuntimeCommand runtimeCommand = new RuntimeCommand(name, description, usage, aliases, permission, command);
        if (definition != null && !definition.getPermissionMessage().isBlank()) {
            runtimeCommand.setPermissionMessage(definition.getPermissionMessage());
        }

        boolean registered = false;
        try {
            registered = commandMap.register(context.getPlugin().getDescription().getName(), runtimeCommand);
        } catch (Exception e) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] No pude registrar el comando '" + name + "' en Bukkit: " + e.getMessage());
        }

        if (!registered) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] Bukkit rechazo el registro de '" + name + "'. Seguirá disponible solo internamente.");
            return;
        }

        runtimeCommands.put(command, new RuntimeRegistration(runtimeCommand, commandMap));
    }

    private void unregisterRuntimeCommand(ZFCommand command) {
        RuntimeRegistration registration = runtimeCommands.remove(command);
        if (registration == null) {
            return;
        }

        try {
            registration.runtimeCommand.unregister(registration.commandMap);
        } catch (Exception ignored) {
            // fallback below
        }

        removeFromKnownCommands(registration.commandMap, registration.runtimeCommand);
    }
    private boolean isSubcommandSlotOccupied(ZFCommand command) {
        if (command == null) {
            return true;
        }

        if (findCommand(command.getName()) != null) {
            return true;
        }

        for (String alias : command.getAliases()) {
            if (alias != null && findCommand(alias) != null) {
                return true;
            }
        }

        return false;
    }


    private void removeFromKnownCommands(CommandMap commandMap, Command command) {
        try {
            Field knownCommandsField = findKnownCommandsField(commandMap.getClass());
            if (knownCommandsField == null) {
                return;
            }
            knownCommandsField.setAccessible(true);
            Object value = knownCommandsField.get(commandMap);
            if (!(value instanceof Map<?, ?> rawMap)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) rawMap;
            knownCommands.entrySet().removeIf(entry -> entry.getValue() == command);
        } catch (Exception ignored) {
            // Best-effort cleanup.
        }
    }

    private Field findKnownCommandsField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType()) && field.getName().toLowerCase(Locale.ROOT).contains("known")) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private CommandMap resolveCommandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof CommandMap commandMap) {
                return commandMap;
            }
        } catch (Exception ignored) {
        }

        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object value = field.get(Bukkit.getServer());
            if (value instanceof CommandMap commandMap) {
                return commandMap;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private int getExpansionCount() {
        return context.getExpansionManager() == null ? 0 : context.getExpansionManager().getLoadedDescriptors().size();
    }

    private int getAddonCount() {
        return context.getAddonManager() == null ? 0 : context.getAddonManager().getLoadedCount();
    }

    private static void addIfMatches(List<String> suggestions, String value, String prefix) {
        if (value == null) {
            return;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (prefix.isEmpty() || normalized.startsWith(prefix)) {
            if (!suggestions.contains(value)) {
                suggestions.add(value);
            }
        }
    }

    private static List<String> sanitizeAliases(String name, List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        String primary = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            String normalized = alias.toLowerCase(Locale.ROOT);
            if (normalized.equals(primary) || seen.contains(normalized)) {
                continue;
            }
            seen.add(normalized);
            sanitized.add(alias);
        }
        return sanitized;
    }

    private static String getPrimaryName(ZFCommand command) {
        return command.getName() == null ? "unknown" : command.getName();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record RuntimeRegistration(RuntimeCommand runtimeCommand, CommandMap commandMap) {
    }

    private static final class RuntimeCommand extends Command {
        private final ZFCommand delegate;

        RuntimeCommand(String name, String description, String usageMessage, List<String> aliases, String permission, ZFCommand delegate) {
            super(name, description == null ? "" : description, usageMessage == null || usageMessage.isBlank() ? "/" + name : usageMessage, aliases == null ? List.of() : List.copyOf(aliases));
            this.delegate = delegate;
            if (permission != null && !permission.isBlank()) {
                setPermission(permission);
            }
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return delegate.execute(sender, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            List<String> completions = delegate.tabComplete(sender, args);
            return completions == null ? Collections.emptyList() : completions;
        }
    }

    private record BaseCommand(
            String name,
            String description,
            String permission,
            CommandAction executor,
            TabAction completer
    ) implements ZFCommand {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getPermission() {
            return permission;
        }

        @Override
        public boolean execute(CommandSender sender, String[] args) {
            return executor.execute(sender, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return completer.complete(sender, args);
        }
    }

    @FunctionalInterface
    private interface CommandAction {
        boolean execute(CommandSender sender, String[] args);
    }

    @FunctionalInterface
    private interface TabAction {
        List<String> complete(CommandSender sender, String[] args);
    }
}
