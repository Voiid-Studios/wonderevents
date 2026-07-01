package voiidstudios.wonderevents;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.addons.MagicAddonManager;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.ConsoleBox;
import voiidstudios.wonderevents.core.log.JavaLoggerImpl;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.managers.MainCommandManager;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.expansions.ExpansionManager;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class WEBootstrap extends JavaPlugin {
    public String version = getDescription().getVersion();

    private static final String WE_LOADED_PROPERTY = "wonderevents.jvm.loaded";
    private static final long UPDATE_CHECK_INTERVAL = 8L * 60L * 60L * 20L; // 8 horas

    private final String serverName = Bukkit.getServer().getName();
    private final String bukkitVersion = Bukkit.getBukkitVersion();
    private final String cleanVersion = bukkitVersion.split("-")[0];
    private final String serverId = Bukkit.getVersion();
    private final String cleanId = serverId.split("-", 2)[1].split(" ")[0];

    private YALogger yaLogger;
    private PluginContext context;
    private ExpansionManager expansionManager;
    private MagicAddonManager addonManager;
    private MetricsManager metricsManager;
    private MainCommandManager mainCommandManager;

    @Override
    public void onEnable() {
        long pluginStart = System.nanoTime();

        yaLogger = new YALogger(new JavaLoggerImpl(Bukkit.getServer().getLogger()), true);

        yaLogger.process("Warming up...");

        sendConsoleInformationMessage();

        ensureFolders();
        ensureCoreConfig();

        context = new PluginContext(this);
        metricsManager = new MetricsManager(context);
        expansionManager = new ExpansionManager(context);
        addonManager = new MagicAddonManager(context);
        mainCommandManager = new MainCommandManager(context);

        context.setMetricsManager(metricsManager);
        context.setExpansionManager(expansionManager);
        context.setAddonManager(addonManager);

        context.getCommandManager().loadCoreCommands();
        registerMainCommand();

        if (context.getConfigManager().isBstatsMetricsEnabled()) {
            metricsManager.start();
        }

        expansionManager.loadExpansions();
        addonManager.loadAddons();
        addonManager.enableAddons();

        long totalMs = elapsedMs(pluginStart);
        yaLogger.success("§aAll set! WonderEvents is set up correctly §7(" + totalMs + "ms)");
    }

    @Override
    public void onDisable() {
        if (addonManager != null) addonManager.disableAddons();
        if (expansionManager != null) expansionManager.disableExpansions();
        if (metricsManager != null) metricsManager.stop();
    }

    public void reloadWonderEvents() {
        if (yaLogger == null || context == null) {
            return;
        }

        long start = System.nanoTime();
        yaLogger.process("Reloading WonderEvents...");

        if (addonManager != null) {
            addonManager.disableAddons();
        }
        if (expansionManager != null) {
            expansionManager.disableExpansions();
        }

        context.getConfigManager().reload();

        if (metricsManager != null) {
            metricsManager.stop();
            if (context.getConfigManager().isBstatsMetricsEnabled()) {
                metricsManager.start();
            }
        }

        int loadedExpansions = 0;
        int loadedAddons = 0;

        if (expansionManager != null) {
            loadedExpansions = expansionManager.loadExpansions();
        }
        if (addonManager != null) {
            loadedAddons = addonManager.loadAddons();
            addonManager.enableAddons();
        }

        long totalMs = elapsedMs(start);
        yaLogger.success("§aWonderEvents reloaded §7(" + totalMs + "ms) §8| §fExpansions: §d" + loadedExpansions + " §8| §fAddons: §d" + loadedAddons);
    }

    private void registerMainCommand() {
        PluginCommand command = getCommand("wonderevents");
        if (command == null) {
            yaLogger.passiveWarning("Could not register /wonderevents because it was missing from plugin.yml");
            return;
        }

        command.setExecutor(mainCommandManager);
        command.setTabCompleter(mainCommandManager);
    }

    private long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    public void sendConsoleInformationMessage(){
        List<String> box = ConsoleBox.builder()
                .borderColor("§d")
                .title("§bWonderEvents")
                .line("§fVersion: §b" + version)
                .line("§fRunning on: §b" + serverName + " §7(ID: " + cleanId + ", MC: " + cleanVersion + ")")
                .footer(dateText())
                .build();

        box.forEach(yaLogger::info);
    }

    private String dateText() { // totally useless, but cute :3
        LocalDate date = LocalDate.now();

        switch (date.getMonth()) {
            case JANUARY:
                if (date.getDayOfMonth() == 1) {
                    return "§fNew year, new bugs... uh, I mean, adventures!! <3";
                }
                break;
            case FEBRUARY:
                if (date.getDayOfMonth() == 29) {
                    return "§fFebruary 29. I guess that's better than seeing a friendly creeper.";
                }
                break;
            case MARCH:
                if (date.getDayOfMonth() == 13) {
                    return "§fToday is my dev's birthday. Happy birthday, MaxxVoiid! :D";
                }
                break;
            case APRIL:
                if (date.getDayOfMonth() == 1) {
                    return "§fToday is April 1. If something explodes... maybe it was on purpose.";
                }
                break;
            case JUNE:
                String[] PRIDE_MESSAGES = {
                        "§cHap§6py P§erid§ae Mo§9nth§d! <3",
                        "§cJune i§6s here§e. Time §ato bri§bng out §9the co§dlors ;)",
                        "§cHave §6a won§ederf§aul Pr§bide M§9onth§d! <3"
                };

                return PRIDE_MESSAGES[ThreadLocalRandom.current().nextInt(PRIDE_MESSAGES.length)];
            case OCTOBER:
                if (date.getDayOfMonth() == 31) {
                    return "§fTonight, even the bugs are scarier... Happy Halloween!";
                }
                break;
            case DECEMBER:
                if (date.getDayOfMonth() > 23 && date.getDayOfMonth() < 27) {
                    return "§fMerry Christmas! Hopefully no plugins will give you any errors today <3";
                }
                break;
            default:
                break;
        }

        int hour = LocalTime.now().getHour();
        if (hour >= 6 && hour < 12) {
            return "§fGood morning! Hope your server has a great day :)";
        } else if (hour >= 12 && hour < 19) {
            return "§fGood afternoon! Keep up the good work :D";
        } else if (hour >= 19 && hour < 22) {
            return "§fGood evening! Wrapping up for the day? :b";
        } else {
            return "§fLate night gaming session? Don't forget to sleep! :p";
        }
    }

    private void ensureFolders() {
        File data = getDataFolder();
        if (!data.exists()) data.mkdirs();
        new File(data, "expansions").mkdirs();
        new File(data, "addons").mkdirs();
        new File(data, "configs").mkdirs();
    }

    private void ensureCoreConfig() {
        File cfg = new File(new File(getDataFolder(), "configs"), "magic-config.yml");
        if (!cfg.exists()) {
            saveResource("configs/magic-config.yml", false);
        }
    }

    public YALogger getYALogger() {
        return yaLogger;
    }

    public PluginContext getPluginContext() {
        return context;
    }
}
