package voiidstudios.wonderevents.commands;

import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.core.PluginContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ReloadSubCommand implements WEACommand {
    private final PluginContext context;

    public ReloadSubCommand(PluginContext context) {
        this.context = context;
    }

    public String getName() {
        return "reload";
    }

    public String getPermission() {
        return "wonderevents.admin.reload";
    }

    public boolean execute(CommandSender sender, String[] args) {
        WEBootstrap plugin = context.getPlugin();

        if (args.length == 0) {
            plugin.reloadWonderEvents(sender, WEBootstrap.ReloadScope.ALL);
            return true;
        }

        String scopeArg = args[0].toLowerCase(Locale.ROOT);
        WEBootstrap.ReloadScope scope = switch (scopeArg) {
            case "all" -> WEBootstrap.ReloadScope.ALL;
            case "configs", "config" -> WEBootstrap.ReloadScope.CONFIGS;
            case "expansions", "expansion" -> WEBootstrap.ReloadScope.EXPANSIONS;
            case "addons", "addon" -> WEBootstrap.ReloadScope.ADDONS;
            default -> null;
        };

        if (scope == null) {
            return false;
        }

        plugin.reloadWonderEvents(sender, scope);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new java.util.ArrayList<>();
            for (String option : Arrays.asList("all", "configs", "expansions", "addons")) {
                if (option.startsWith(prefix)) {
                    result.add(option);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
