
package voiidstudios.wonderevents.core.managers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import voiidstudios.wonderevents.core.PluginContext;

import java.util.Collections;
import java.util.List;

public class MainCommandManager implements CommandExecutor, TabCompleter {
    protected final PluginContext context;

    public MainCommandManager(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§eMagicEvents admin command framework.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
