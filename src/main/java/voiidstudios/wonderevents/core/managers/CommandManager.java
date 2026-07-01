package voiidstudios.wonderevents.core.managers;

import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.api.ZFCommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.expansions.ExpansionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CommandManager {
    private final PluginContext context;
    private final List<ZFCommand> commands = new ArrayList<>();
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

    public void registerAddonCommand(ZFCommand command) {
        if (command != null) {
            commands.add(command);
        }
    }

    public void unregisterAddonCommand(ZFCommand command) {
        commands.remove(command);
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

    private static String getPrimaryName(ZFCommand command) {
        return command.getName() == null ? "unknown" : command.getName();
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
