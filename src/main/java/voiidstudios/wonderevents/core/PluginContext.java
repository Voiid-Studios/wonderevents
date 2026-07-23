package voiidstudios.wonderevents.core;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.addons.WonderAddonManager;
import voiidstudios.wonderevents.core.managers.AdventureManager;
import voiidstudios.wonderevents.core.managers.CommandManager;
import voiidstudios.wonderevents.core.managers.ConfigManager;
import voiidstudios.wonderevents.core.managers.EventManager;
import voiidstudios.wonderevents.core.managers.MessagesManager;
import voiidstudios.wonderevents.core.managers.ModuleManager;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.core.platform.PaperPlatformAdapter;
import voiidstudios.wonderevents.core.platform.PlatformAdapter;
import voiidstudios.wonderevents.core.platform.SpigotPlatformAdapter;
import voiidstudios.wonderevents.core.scheduler.BukkitSchedulerAdapter;
import voiidstudios.wonderevents.core.scheduler.SchedulerAdapter;
import voiidstudios.wonderevents.expansions.WonderExpansionManager;

public final class PluginContext {
    private final WEBootstrap plugin;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private final CommandManager commandManager;
    private final EventManager eventManager;
    private final ModuleManager moduleManager;
    private final PlatformAdapter platformAdapter;
    private final SchedulerAdapter schedulerAdapter;
    private final AdventureManager adventureManager;

    private MetricsManager metricsManager;
    private WonderAddonManager addonManager;
    private WonderExpansionManager expansionManager;

    public PluginContext(WEBootstrap plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager(plugin);
        this.configManager.bootstrap();
        this.messagesManager = new MessagesManager(plugin, configManager.getLanguage(), plugin.getYALogger());
        this.commandManager = new CommandManager(this);
        this.eventManager = new EventManager(this);
        this.moduleManager = new ModuleManager(this);
        this.platformAdapter = createPlatformAdapter();
        this.schedulerAdapter = new BukkitSchedulerAdapter(plugin);
        this.adventureManager = new AdventureManager(this);
        this.adventureManager.start();
    }

    private PlatformAdapter createPlatformAdapter() {
        if (PaperPlatformAdapter.isAvailable()) {
            return new PaperPlatformAdapter(plugin);
        }
        return new SpigotPlatformAdapter(plugin);
    }

    public WEBootstrap getPlugin() { return plugin; }
    public WEBootstrap getCore() { return plugin; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public EventManager getEventManager() { return eventManager; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public PlatformAdapter getPlatform() { return platformAdapter; }
    public SchedulerAdapter getScheduler() { return schedulerAdapter; }
    public AdventureManager getAdventureManager() { return adventureManager; }

    public MetricsManager getMetricsManager() { return metricsManager; }
    public void setMetricsManager(MetricsManager metricsManager) { this.metricsManager = metricsManager; }

    public WonderAddonManager getAddonManager() { return addonManager; }
    public void setAddonManager(WonderAddonManager addonManager) { this.addonManager = addonManager; }

    public WonderExpansionManager getExpansionManager() { return expansionManager; }
    public void setExpansionManager(WonderExpansionManager expansionManager) { this.expansionManager = expansionManager; }
}
