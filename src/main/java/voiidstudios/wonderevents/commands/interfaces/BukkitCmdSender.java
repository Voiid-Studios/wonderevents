package voiidstudios.wonderevents.commands.interfaces;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class BukkitCmdSender implements CmdSender {
    private final CommandSender sender;

    public BukkitCmdSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMsg(String msg, Object... args) {
        String formatted = args.length > 0 ? String.format(msg, args) : msg;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }
}