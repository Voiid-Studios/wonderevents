package voiidstudios.wonderevents.commands;

import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.managers.MessagesManager;

import java.util.HashMap;
import java.util.Map;

public final class HelpSubCommand implements WEACommand {
    private final PluginContext context;

    public HelpSubCommand(PluginContext context) {
        this.context = context;
    }

    public String getName() {
        return "help";
    }

    public String getPermission() {
        return "wonderevents.admin";
    }

    public boolean execute(CommandSender sender, String[] args) {
        MessagesManager messages = context.getMessagesManager();

        Map<String, String> header = new HashMap<>();
        header.put("%VERSION%", context.getPlugin().getDescription().getVersion());
        messages.sendPrefixed(sender, "command.help.header", header);
        messages.sendList(sender, "command.help.lines", null);

        return true;
    }
}
