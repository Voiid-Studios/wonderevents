package voiidstudios.wonderevents;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import voiidstudios.wonderevents.addons.WonderAddonManager;
import voiidstudios.wonderevents.core.PluginContext;
import voiidstudios.wonderevents.core.log.ConsoleBox;
import voiidstudios.wonderevents.core.log.JavaLoggerImpl;
import voiidstudios.wonderevents.core.log.YALogger;
import voiidstudios.wonderevents.core.managers.MainCommandManager;
import voiidstudios.wonderevents.core.metrics.MetricsManager;
import voiidstudios.wonderevents.expansions.WonderExpansionManager;
import voiidstudios.wonderevents.utils.DownloadSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

public final class WEBootstrap extends JavaPlugin {
    public String version = getDescription().getVersion();

    private static final String WE_LOADED_PROPERTY = "wonderevents.jvm.loaded";
    private static final long UPDATE_CHECK_INTERVAL = 8L * 60L * 60L * 20L; // 8 hours

    private YALogger yaLogger;
    private PluginContext context;
    private WonderExpansionManager expansionManager;
    private WonderAddonManager addonManager;
    private MetricsManager metricsManager;
    private MainCommandManager mainCommandManager;

    public void onEnable() {
        long pluginStart = System.nanoTime();

        yaLogger = new YALogger(new JavaLoggerImpl(Bukkit.getServer().getLogger()), true);
        yaLogger.process("Warming up...");

        sendConsoleBanner();

        sendConsoleInformationMessage();

        yaLogger.process("Getting everything ready for you...");

        if (Boolean.getBoolean(WE_LOADED_PROPERTY)) {
            sendConsoleReloadWarning();
        } else {
            System.setProperty(WE_LOADED_PROPERTY, "true");
        }

        context = new PluginContext(this);
        metricsManager = new MetricsManager(context);
        expansionManager = new WonderExpansionManager(context);
        addonManager = new WonderAddonManager(context);
        mainCommandManager = new MainCommandManager(context);

        context.setMetricsManager(metricsManager);
        context.setExpansionManager(expansionManager);
        context.setAddonManager(addonManager);

        yaLogger.process("Looking for commands...");

        long cmdsStart = System.nanoTime();
        context.getCommandManager().loadCoreCommands();
        registerMainCommand();
        long cmdsMs = elapsedMs(cmdsStart);

        int cmdsCount = context.getCommandManager().getLoadedCommandCount();
        yaLogger.success("§bRegistered " + cmdsCount + " commands. They're all ears! §7(" + cmdsMs + "ms)");

        yaLogger.process("Looking for expansions...");

        long exStart = System.nanoTime();
        expansionManager.loadExpansions();
        expansionManager.enableExpansions();
        long exMs = elapsedMs(exStart);

        int exCount = expansionManager.getLoadedCount();
        if (exCount > 0) {
            yaLogger.success("§bLoaded " + exCount + " expansions. They're ready for action! §7(" + exMs + "ms)");
        } else {
            yaLogger.passiveInfo("[Expansions] §9Where did they go? I haven't found them in /expansions...");
        }

        yaLogger.process("Looking for addons...");

        long adStart = System.nanoTime();
        addonManager.loadAddons();
        addonManager.enableAddons();
        long adMs = elapsedMs(adStart);

        int adCount = addonManager.getLoadedCount();
        if (adCount > 0) {
            yaLogger.success("§bLoaded " + adCount + " addons. They're ready for action! §7(" + adMs + "ms)");
        } else {
            yaLogger.passiveInfo("[Addons] §9Hmm, it's so quiet... there's no one on /addons...");
        }

        if (context.getConfigManager().isMetricsEnabled()) {
            yaLogger.process("Connecting the satellites...");
            metricsManager.start();
        }

        long totalMs = elapsedMs(pluginStart);
        yaLogger.success("§aAll set! WonderEvents is set up correctly §7(" + totalMs + "ms)");
    }

    public void onDisable() {
        yaLogger.process("Time to rest... wrapping up everything");

        if (addonManager != null) addonManager.disableAddons();
        if (expansionManager != null) expansionManager.disableExpansions();
        if (metricsManager != null) metricsManager.stop();
        if (context != null) context.getAdventureManager().stop();

        yaLogger.success("Everything's tucked in. See you next time! zzz...");
    }

    public enum ReloadScope {
        ALL,
        CONFIGS,
        EXPANSIONS,
        ADDONS
    }

    public void reloadWonderEvents() {
        reloadWonderEvents(Bukkit.getConsoleSender(), ReloadScope.ALL);
    }

    public void reloadWonderEvents(CommandSender sender) {
        reloadWonderEvents(sender, ReloadScope.ALL);
    }

    public void reloadWonderEvents(CommandSender sender, ReloadScope scope) {
        if (yaLogger == null || context == null) {
            return;
        }

        if (scope == null) {
            scope = ReloadScope.ALL;
        }

        long start = System.nanoTime();
        var messages = context.getMessagesManager();
        messages.sendPrefixed(sender, "command.reload.process");

        boolean reloadConfigs = scope == ReloadScope.ALL || scope == ReloadScope.CONFIGS;
        boolean reloadExpansions = scope == ReloadScope.ALL || scope == ReloadScope.EXPANSIONS;
        boolean reloadAddons = scope == ReloadScope.ALL || scope == ReloadScope.ADDONS;

        if (reloadConfigs) {
            context.getConfigManager().reload();
            context.getMessagesManager().reload(context.getConfigManager().getLanguage());

            Map<String, String> reloadPlaceholders = new java.util.HashMap<>();
            reloadPlaceholders.put("%CONFIG%", context.getConfigManager().getConfigFile().getName());
            messages.sendPrefixed(sender, "command.reload.success_config", reloadPlaceholders);
            messages.sendPrefixed(sender, "command.reload.success_messages");
        }

        if (reloadExpansions && expansionManager != null) {
            int loadedExpansions = expansionManager.reloadExpansions();

            Map<String, String> expPlaceholders = new java.util.HashMap<>();
            expPlaceholders.put("%EXPANSIONS%", String.valueOf(loadedExpansions));
            messages.sendPrefixed(sender, "command.reload.success_expansions", expPlaceholders);
        }

        if (reloadAddons && addonManager != null) {
            if (metricsManager != null) {
                metricsManager.stop();
            }

            int loadedAddons = addonManager.reloadAddons();

            if (metricsManager != null && context.getConfigManager().isMetricsEnabled()) {
                metricsManager.start();
            }

            Map<String, String> addonPlaceholders = new java.util.HashMap<>();
            addonPlaceholders.put("%ADDONS%", String.valueOf(loadedAddons));
            messages.sendPrefixed(sender, "command.reload.success_addons", addonPlaceholders);
        }

        Map<String, String> donePlaceholders = new java.util.HashMap<>();
        donePlaceholders.put("%MS%", String.valueOf(elapsedMs(start)));
        messages.sendPrefixed(sender, "command.reload.done", donePlaceholders);
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

    private void sendConsoleBanner(){
        yaLogger.info("");
        yaLogger.info("          ::::::");
        yaLogger.info("      ::::::::");
        yaLogger.info("    ::::::::        ::");
        yaLogger.info("  ::::::::      ::  ::  ::");
        yaLogger.info("  ::::::::        ::::::");
        yaLogger.info("::::::::::    ::::::::::::::");
        yaLogger.info("::::::::::        ::::::");
        yaLogger.info("::::::::::      ::  ::  ::    ::");
        yaLogger.info("::::::::::          ::        ::");
        yaLogger.info("::::::::::::                ::::");
        yaLogger.info("::::::::::::::            ::::::");
        yaLogger.info("::::::::::::::::::::::::::::::::");
        yaLogger.info("  ::::::::::::::::::::::::::::");
        yaLogger.info("    ::::::::::::::::::::::::");
        yaLogger.info("      ::::::::::::::::::::");
        yaLogger.info("          ::::::::::::");
        yaLogger.info("");
    }

    public void sendConsoleInformationMessage() {
        List<String> box = ConsoleBox.builder()
                .borderColor("§d")
                .title("§bWonderEvents")
                .line("§fVersion: §b" + version)
                .line("§fRunning on: " + platformInfo())
                .line("§fDownloaded from: §b" + DownloadSource.detect(this))
                .footer(dateText())
                .build();

        box.forEach(yaLogger::info);
    }

    private void sendConsoleReloadWarning() {
        final String[] RELOAD_PREFIXES = {
            "WHAT ARE YOU DOING?!",
            "OH HELL NO.",
            "...seriously?",
            "bro.",
            "nope. nope. nope.",
            "have you tried NOT doing that?",
            "the council does not approve.",
            "skill issue.",
            "i am so tired of you.",
            "do you feel powerful? does this make you feel powerful?",
            "i will not stand for this.",
            "i will not tolerate such ingratitude.",
            "i love you very much, but this time you've really gone too far."
        };

        String prefix = RELOAD_PREFIXES[ThreadLocalRandom.current().nextInt(RELOAD_PREFIXES.length)];

        List<String> box = ConsoleBox.builder()
                .title("⚠ " + prefix + " ⚠")
                .line("Server reload detected by WonderEvents.")
                .line("This usually happens when you use /bukkit:reload, PlugMan, or similar.")
                .blank()
                .line("This action IS NOT SUPPORTED and may cause SERIOUS PROBLEMS WITH YOUR")
                .line("EXPANSIONS AND ADD-ONS!!!")
                .blank()
                .line("YOU WILL GET NO SUPPORT FOR THE PLUGIN FOR ANY ISSUES YOU ENCOUNTER")
                .line("AFTER THE SERVER RELOAD!")
                .blank()
                .line("More info: https://madelinemiller.dev/blog/problem-with-reload/")
                .footer("#RestartYourServerAndNeverReloadIt")
                .build();

        box.forEach(yaLogger::warning);
    }

    public static String platformInfo(){
        try{
            Class.forName("io.papermc.paper.ServerBuildInfo");
            return modernInfo();
        }catch(ClassNotFoundException ignored){
            return legacyInfo();
        }
    }
    
    private static String modernInfo(){
        io.papermc.paper.ServerBuildInfo info = io.papermc.paper.ServerBuildInfo.buildInfo();
        
        String name = info.brandName();
        String mcVersion = info.minecraftVersionName();

        OptionalInt build = info.buildNumber();
        if(build.isEmpty())
            return String.format("§b%s §7(MC: %s)", name, mcVersion);
        
        return String.format("§b%s §7(MC: %s, Build: %s)", name, mcVersion, build.getAsInt());
    }
    
    private static String legacyInfo(){
        String bukkitVersion = Bukkit.getBukkitVersion(); // "1.8.8-R0.1-SNAPSHOT"
        String mcVersion = bukkitVersion.split("-")[0];
        
        String raw = Bukkit.getVersion(); // "git-Paper-1618 (MC: 1.16.5)"
        String name = extractBrand(raw);
        
        return String.format("§b%s §7(MC: %s)", name, mcVersion);
    }
    
    private static String extractBrand(String rawVersion){
        if(rawVersion != null && rawVersion.startsWith("git-")){
            String stripped = rawVersion.substring(4);
            int dash = stripped.indexOf('-');
            if(dash > 0)
                return stripped.substring(0, dash);
        }
        
        return Bukkit.getName();
    }

    private String dateText() {
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

        String[] VS_MESSAGES = {
                "§fMade with <3 from Voiid Studios",
                "§fThe Voiid Studios Team says hello ;)",
                "§fVoiid Studios on top! <3",
                "§fVoiid Studios was here :D"
        };

        return VS_MESSAGES[ThreadLocalRandom.current().nextInt(VS_MESSAGES.length)];
    }

    public YALogger getYALogger() {
        return yaLogger;
    }

    public PluginContext getPluginContext() {
        return context;
    }

    public WEBootstrap getCore() {
        return this;
    }
}
