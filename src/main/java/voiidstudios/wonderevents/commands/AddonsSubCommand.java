package voiidstudios.wonderevents.commands;

import org.bukkit.command.CommandSender;

import voiidstudios.wonderevents.addons.WonderAddonDescriptor;
import voiidstudios.wonderevents.addons.WonderAddonManager;
import voiidstudios.wonderevents.api.WEACommand;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.managers.MessagesManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AddonsSubCommand implements WEACommand {
    private final PluginContext context;

    public AddonsSubCommand(PluginContext context) {
        this.context = context;
    }

    public String getName() {
        return "addons";
    }

    public String getPermission() {
        return "wonderevents.admin.addons";
    }

    public boolean execute(CommandSender sender, String[] args) {
        MessagesManager messages = context.getMessagesManager();

        if (args.length == 0) {
            return handleList(sender, messages);
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "enable":
                return handleEnable(sender, args, messages);
            case "disable":
                return handleDisable(sender, args, messages);
            case "list":
                return handleList(sender, messages);
            default:
                return false;
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("enable", "disable", "list"), args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable"))) {
            WonderAddonManager manager = context.getAddonManager();
            if (manager == null) {
                return Collections.emptyList();
            }

            List<String> ids = new java.util.ArrayList<>();
            for (WonderAddonDescriptor descriptor : manager.getLoadedDescriptors()) {
                ids.add(descriptor.getId());
            }
            return filter(ids, args[1]);
        }

        return Collections.emptyList();
    }

    private boolean handleEnable(CommandSender sender, String[] args, MessagesManager messages) {
        if (args.length < 2) {
            messages.send(sender, "command.addons.enable.usage");
            return true;
        }

        String id = args[1];
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%ID%", id);

        WonderAddonManager manager = context.getAddonManager();
        WonderAddonManager.AddonToggleResult result = manager == null
                ? WonderAddonManager.AddonToggleResult.NOT_FOUND
                : manager.enableAddon(id);

        switch (result) {
            case NOT_FOUND:
                messages.send(sender, "command.addons.enable.not_found", placeholders);
                break;
            case ALREADY:
                messages.send(sender, "command.addons.enable.already", placeholders);
                break;
            case SUCCESS:
                messages.send(sender, "command.addons.enable.success", placeholders);
                break;
        }
        return true;
    }

    private boolean handleDisable(CommandSender sender, String[] args, MessagesManager messages) {
        if (args.length < 2) {
            messages.send(sender, "command.addons.disable.usage");
            return true;
        }

        String id = args[1];
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%ID%", id);

        WonderAddonManager manager = context.getAddonManager();
        WonderAddonManager.AddonToggleResult result = manager == null
                ? WonderAddonManager.AddonToggleResult.NOT_FOUND
                : manager.disableAddon(id);

        switch (result) {
            case NOT_FOUND:
                messages.send(sender, "command.addons.disable.not_found", placeholders);
                break;
            case ALREADY:
                messages.send(sender, "command.addons.disable.already", placeholders);
                break;
            case SUCCESS:
                messages.send(sender, "command.addons.disable.success", placeholders);
                break;
        }
        return true;
    }

    private boolean handleList(CommandSender sender, MessagesManager messages) {
        WonderAddonManager manager = context.getAddonManager();
        List<WonderAddonDescriptor> descriptors = manager == null
                ? Collections.emptyList()
                : manager.getLoadedDescriptors();

        if (descriptors.isEmpty()) {
            messages.send(sender, "command.addons.list.empty");
            return true;
        }

        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("%ADDONS%", String.valueOf(descriptors.size()));
        messages.send(sender, "command.addons.list.title", titlePlaceholders);

        String separator = messages.get("command.addons.list.separator");
        String enabledColor = messages.get("command.addons.list.enabled");
        String disabledColor = messages.get("command.addons.list.disabled");

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < descriptors.size(); i++) {
            WonderAddonDescriptor descriptor = descriptors.get(i);
            boolean enabled = manager != null && manager.isEnabled(descriptor.getId());

            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("%STATUSCOLOR%", enabled ? enabledColor : disabledColor);
            entryPlaceholders.put("%NAME%", descriptor.getName());
            entryPlaceholders.put("%VERSION%", descriptor.getVersion());

            line.append(messages.get("command.addons.list.entry", entryPlaceholders));
            if (i < descriptors.size() - 1) {
                line.append(separator);
            }
        }

        sender.sendMessage(line.toString());
        return true;
    }

    private List<String> filter(List<String> options, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new java.util.ArrayList<>();
        for (String option : options) {
            if (option != null && option.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(option);
            }
        }
        return result;
    }
}
