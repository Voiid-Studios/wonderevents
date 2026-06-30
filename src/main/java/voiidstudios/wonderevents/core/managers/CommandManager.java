
package voiidstudios.wonderevents.core.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import voiidstudios.wonderevents.api.ZFCommand;
import voiidstudios.wonderevents.core.PluginContext;

public class CommandManager {
    private final PluginContext context;
    private final List<ZFCommand> addonCommands = new ArrayList<>();

    public CommandManager(PluginContext context) {
        this.context = context;
    }

    public void loadCoreCommands() {
        // Admin commands only.
    }

    public void registerAddonCommand(ZFCommand command) {
        if (command != null) addonCommands.add(command);
    }

    public void unregisterAddonCommand(ZFCommand command) {
        addonCommands.remove(command);
    }

    public List<ZFCommand> getSubcommands() {
        return Collections.unmodifiableList(addonCommands);
    }

    public int getLoadedCommandCount() {
        return addonCommands.size();
    }

    public PluginContext getContext() {
        return context;
    }
}
