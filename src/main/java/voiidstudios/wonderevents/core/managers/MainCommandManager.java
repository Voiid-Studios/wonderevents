package voiidstudios.wonderevents.core.managers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.core.PluginContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainCommandManager implements CommandExecutor, TabCompleter {
    protected final PluginContext context;

    public MainCommandManager(PluginContext context) {
        this.context = context;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            WEACommand help = context.getCommandManager().findCommand("help");
            if (help != null) {
                help.execute(sender, new String[0]);
            }
            return true;
        }

        WEACommand subcommand = context.getCommandManager().findCommand(args[0]);
        if (subcommand == null) {
            context.getMessagesManager().send(sender, "command.unknown");
            return true;
        }

        if (!hasPermission(sender, subcommand.getPermission())) {
            context.getMessagesManager().send(sender, "command.no_permissions");
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        boolean handled = subcommand.execute(sender, subArgs);
        if (!handled) {
            context.getMessagesManager().send(sender, "command.unknown");
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            List<String> suggestions = new ArrayList<>();
            for (WEACommand subcommand : context.getCommandManager().getSubcommands()) {
                if (hasPermission(sender, subcommand.getPermission())) {
                    addIfMatches(suggestions, subcommand.getName(), prefix);
                    for (String aliasName : subcommand.getAliases()) {
                        addIfMatches(suggestions, aliasName, prefix);
                    }
                }
            }
            return suggestions;
        }

        WEACommand subcommand = context.getCommandManager().findCommand(args[0]);
        if (subcommand == null) {
            return Collections.emptyList();
        }

        if (!hasPermission(sender, subcommand.getPermission())) {
            return Collections.emptyList();
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        List<String> completions = subcommand.tabComplete(sender, subArgs);
        return completions == null ? Collections.emptyList() : completions;
    }

    private static void addIfMatches(List<String> suggestions, String value, String prefix) {
        if (value == null) return;
        
        String normalized = value.toLowerCase(Locale.ROOT);
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isEmpty() || normalized.startsWith(normalizedPrefix)) {
            if (!suggestions.contains(value)) {
                suggestions.add(value);
            }
        }
    }

    private static boolean hasPermission(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }
}
