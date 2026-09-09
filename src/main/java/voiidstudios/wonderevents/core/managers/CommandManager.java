package voiidstudios.wonderevents.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.commands.AddonsSubCommand;
import voiidstudios.wonderevents.commands.ExpansionsSubCommand;
import voiidstudios.wonderevents.commands.HelpSubCommand;
import voiidstudios.wonderevents.commands.ReloadSubCommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.manifest.WonderManifest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private final PluginContext context;
    private final List<WEACommand> commands = new ArrayList<>();
    private final Map<WEACommand, RuntimeRegistration> runtimeCommands = new IdentityHashMap<>();
    private boolean coreCommandsLoaded;

    public CommandManager(PluginContext context) {
        this.context = context;
    }

    public void loadCoreCommands() {
        if (coreCommandsLoaded) {
            return;
        }

        registerCoreCommand(new HelpSubCommand(context));
        registerCoreCommand(new ReloadSubCommand(context));
        registerCoreCommand(new AddonsSubCommand(context));
        registerCoreCommand(new ExpansionsSubCommand(context));

        coreCommandsLoaded = true;
    }

    public boolean registerAddonCommand(WEACommand command, WonderManifest manifest) {
        if (command == null) {
            return false;
        }

        if (isSubcommandSlotOccupied(command)) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] A subcommand with the name or alias '" + command.getName() + "' already exists. The command will still be available as a native command if Bukkit allows it");
        } else {
            commands.add(command);
        }

        registerRuntimeCommand(command, manifest);
        return true;
    }

    public void registerAddonCommand(WEACommand command) {
        registerAddonCommand(command, null);
    }

    public void unregisterAddonCommand(WEACommand command) {
        if (command == null) {
            return;
        }
        commands.remove(command);
        unregisterRuntimeCommand(command);
    }

    public List<WEACommand> getSubcommands() {
        return Collections.unmodifiableList(commands);
    }

    public int getLoadedCommandCount() {
        return commands.size();
    }

    public WEACommand findCommand(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        for (WEACommand command : commands) {
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
        for (WEACommand command : commands) {
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

    private void registerCoreCommand(WEACommand command) {
        if (command != null) {
            commands.add(command);
        }
    }

    private void registerRuntimeCommand(WEACommand command, WonderManifest manifest) {
        CommandMap commandMap = resolveCommandMap();
        if (commandMap == null) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] Could not get Bukkit's CommandMap to register '" + command.getName() + "'.");
            return;
        }

        WonderManifest.CommandDefinition definition = manifest == null ? null : manifest.getCommand(command.getName());
        String name = safe(command.getName(), "unknown");
        String usage = firstNonBlank(definition == null ? null : definition.getUsage(), "/" + name);
        String permission = firstNonBlank(definition == null ? null : definition.getPermission(), command.getPermission(), "");
        List<String> aliases = definition != null && !definition.getAliases().isEmpty() ? definition.getAliases() : command.getAliases();
        aliases = sanitizeAliases(name, aliases);

        RuntimeCommand runtimeCommand = new RuntimeCommand(name, usage, aliases, permission, command);
        if (definition != null && !definition.getPermissionMessage().trim().isEmpty()) {
            runtimeCommand.setPermissionMessage(definition.getPermissionMessage());
        }

        boolean registered = false;
        try {
            registered = commandMap.register(context.getPlugin().getDescription().getName(), runtimeCommand);
        } catch (Exception e) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] Could not register command '" + name + "' in Bukkit: " + e.getMessage());
        }

        if (!registered) {
            context.getPlugin().getYALogger().passiveWarning("[Commands] Bukkit rejected the registration of '" + name + "'. It will still be available internally only.");
            return;
        }

        runtimeCommands.put(command, new RuntimeRegistration(runtimeCommand, commandMap));
    }

    private void unregisterRuntimeCommand(WEACommand command) {
        RuntimeRegistration registration = runtimeCommands.remove(command);
        if (registration == null) {
            return;
        }

        try {
            registration.runtimeCommand.unregister(registration.commandMap);
        } catch (Exception ignored) {}

        removeFromKnownCommands(registration.commandMap, registration.runtimeCommand);
    }
    private boolean isSubcommandSlotOccupied(WEACommand command) {
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
            if (!(value instanceof Map<?, ?>)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) value;
            knownCommands.entrySet().removeIf(entry -> entry.getValue() == command);
        } catch (Exception ignored) {}
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
            if (value instanceof CommandMap) {
                return (CommandMap) value;
            }
        } catch (Exception ignored) {}

        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object value = field.get(Bukkit.getServer());
            if (value instanceof CommandMap) {
                return (CommandMap) value;
            }
        } catch (Exception ignored) {}

        return null;
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
            return Collections.emptyList();
        }

        List<String> sanitized = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        String primary = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) {
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

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static final class RuntimeRegistration {
        private final RuntimeCommand runtimeCommand;
        private final CommandMap commandMap;

        RuntimeRegistration(RuntimeCommand runtimeCommand, CommandMap commandMap) {
            this.runtimeCommand = runtimeCommand;
            this.commandMap = commandMap;
        }

        RuntimeCommand runtimeCommand() {
            return runtimeCommand;
        }

        CommandMap commandMap() {
            return commandMap;
        }
    }

    private static final class RuntimeCommand extends Command {
        private final WEACommand delegate;

        RuntimeCommand(String name, String usageMessage, List<String> aliases, String permission, WEACommand delegate) {
            super(name, "", usageMessage == null || usageMessage.trim().isEmpty() ? "/" + name : usageMessage, aliases == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(aliases)));
            this.delegate = delegate;
            if (permission != null && !permission.trim().isEmpty()) {
                setPermission(permission);
            }
        }

        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return delegate.execute(sender, args);
        }

        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            List<String> completions = delegate.tabComplete(sender, args);
            return completions == null ? Collections.emptyList() : completions;
        }
    }

}
