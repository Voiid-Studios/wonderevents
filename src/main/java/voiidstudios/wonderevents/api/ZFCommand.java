package voiidstudios.wonderevents.api;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface ZFCommand {

    String getName();

    String getDescription();

    String getPermission();

    boolean execute(CommandSender sender, String[] args);

    default List<String> getAliases() {
        return Collections.emptyList();
    }

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
