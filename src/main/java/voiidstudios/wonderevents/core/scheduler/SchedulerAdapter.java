package voiidstudios.wonderevents.core.scheduler;

public interface SchedulerAdapter {
    void runTask(Runnable runnable);

    void runTaskLater(Runnable runnable, long delayTicks);

    void runTaskTimer(Runnable runnable, long delayTicks, long periodTicks);

    void runAsync(Runnable runnable);
}
