package voiidstudios.wonderevents.core.managers;

import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.api.ZFCommand;
import voiidstudios.wonderevents.core.PluginContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
                    sender.sendMessage("§d§lWonderEvents §7- §fAvailable commands:");
                    for (ZFCommand command : commands) {
                        sender.sendMessage(" §8• §d/" + getPrimaryName(command) + " §7- §f" + command.getDescription());
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
                    sender.sendMessage("§d§lWonderEvents §7- §fRuntime status:");
                    sender.sendMessage(" §8• §fExpansions: §d" + context.getExpansionManager().getLoadedDescriptors().size());
                    sender.sendMessage(" §8• §fAddons: §d" + context.getAddonManager().getLoadedCount());
                    sender.sendMessage(" §8• §fCommands: §d" + getLoadedCommandCount());
                    sender.sendMessage(" §8• §fModules: §d" + context.getModuleManager().getLoadedModuleCount());
                    sender.sendMessage(" §8• §fListeners: §d" + context.getEventManager().getRegisteredListenerCount());
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "reload",
                "Reloads configs, expansions and addons.",
                "wonderevents.admin.reload",
                (sender, args) -> {
                    context.getPlugin().reloadWonderEvents();
                    sender.sendMessage("§aWonderEvents reload requested.");
                    return true;
                },
                (sender, args) -> List.of()
        ));

        registerCoreCommand(new BaseCommand(
                "addons",
                "Lists all loaded addons.",
                "wonderevents.admin.addons",
                (sender, args) -> {
                    var addonManager = context.getAddonManager();
                    var descriptors = addonManager.getLoadedDescriptors();
                    if (descriptors.isEmpty()) {
                        sender.sendMessage("§eNo addons are currently loaded.");
                        return true;
                    }

                    sender.sendMessage("§d§lWonderEvents §7- §fLoaded addons:");
                    for (var descriptor : descriptors) {
                        sender.sendMessage(" §8• §d" + descriptor.getName() + " §7v" + descriptor.getVersion() +
                                (descriptor.getAuthor().isBlank() ? "" : " §8by §f" + descriptor.getAuthor()));
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
                    var expansionManager = context.getExpansionManager();
                    var descriptors = expansionManager.getLoadedDescriptors();
                    if (descriptors.isEmpty()) {
                        sender.sendMessage("§eNo expansions are currently loaded.");
                        return true;
                    }

                    sender.sendMessage("§d§lWonderEvents §7- §fLoaded expansions:");
                    for (var descriptor : descriptors) {
                        sender.sendMessage(" §8• §d" + descriptor.getId() + " §7- §f" + descriptor.getName() + " §8(" + descriptor.getVersion() + ")");
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

        String normalized = name.toLowerCase(Locale.ROOT);
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
        return Objects.requireNonNullElse(command.getName(), "unknown");
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
