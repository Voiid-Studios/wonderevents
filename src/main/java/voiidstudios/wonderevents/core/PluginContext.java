package voiidstudios.wonderevents.core;

import voiidstudios.wonderevents.WEBootstrap;
import voiidstudios.wonderevents.addons.MagicAddonManager;
import voiidstudios.wonderevents.core.managers.CommandManager;
import voiidstudios.wonderevents.core.managers.ConfigManager;
import voiidstudios.wonderevents.core.managers.EventManager;
import voiidstudios.wonderevents.core.managers.ModuleManager;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.core.platform.PaperPlatformAdapter;
import voiidstudios.wonderevents.core.platform.PlatformAdapter;
import voiidstudios.wonderevents.core.platform.SpigotPlatformAdapter;
import voiidstudios.wonderevents.core.scheduler.BukkitSchedulerAdapter;
import voiidstudios.wonderevents.core.scheduler.SchedulerAdapter;
import voiidstudios.wonderevents.expansions.ExpansionManager;

public final class PluginContext {
    private final WEBootstrap plugin;
    private final ConfigManager configManager;
    private final CommandManager commandManager;
    private final EventManager eventManager;
    private final ModuleManager moduleManager;
    private final PlatformAdapter platformAdapter;
    private final SchedulerAdapter schedulerAdapter;

    private MetricsManager metricsManager;
    private MagicAddonManager addonManager;
    private ExpansionManager expansionManager;

    public PluginContext(WEBootstrap plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager(plugin);
        this.commandManager = new CommandManager(this);
        this.eventManager = new EventManager(this);
        this.moduleManager = new ModuleManager(this);
        this.platformAdapter = createPlatformAdapter();
        this.schedulerAdapter = new BukkitSchedulerAdapter(plugin);
    }

    private PlatformAdapter createPlatformAdapter() {
        if (PaperPlatformAdapter.isAvailable()) {
            return new PaperPlatformAdapter(plugin);
        }
        return new SpigotPlatformAdapter(plugin);
    }

    public WEBootstrap getPlugin() { return plugin; }
    public ConfigManager getConfigManager() { return configManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public EventManager getEventManager() { return eventManager; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public PlatformAdapter getPlatform() { return platformAdapter; }
    public SchedulerAdapter getScheduler() { return schedulerAdapter; }

    public MetricsManager getMetricsManager() { return metricsManager; }
    public void setMetricsManager(MetricsManager metricsManager) { this.metricsManager = metricsManager; }

    public MagicAddonManager getAddonManager() { return addonManager; }
    public void setAddonManager(MagicAddonManager addonManager) { this.addonManager = addonManager; }

    public ExpansionManager getExpansionManager() { return expansionManager; }
    public void setExpansionManager(ExpansionManager expansionManager) { this.expansionManager = expansionManager; }
}
