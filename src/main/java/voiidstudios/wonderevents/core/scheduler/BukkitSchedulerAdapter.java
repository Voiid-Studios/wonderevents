package voiidstudios.wonderevents.core.scheduler;

import org.bukkit.plugin.Plugin;

public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    public void runTask(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public void runTaskLater(Runnable runnable, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public void runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public void runAsync(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
