package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class LogBot extends Module
{
    public static final LogBot INSTANCE = new LogBot();

    private static final List<LogDeletionTime> LOG_DELETION_TIMES = Stream.of(new LogDeletionTime("plugins/AntiCheatAddition/logs", ".AntiCheatAddition"),
                                                                              new LogDeletionTime("logs", ".Server"))
                                                                          // Actually active.
                                                                          .filter(LogDeletionTime::isActive)
                                                                          .toList();

    private BukkitTask task;

    private LogBot()
    {
        super("LogBot");
    }

    @Override
    public void enable()
    {
        // Start a daily executed task to clean up the logs.
        task = Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), () -> {
            final long currentTime = System.currentTimeMillis();
            for (LogDeletionTime logDeletionTime : LOG_DELETION_TIMES) logDeletionTime.handleLog(currentTime);
        }, 1, TimeUtil.toTicks(1, TimeUnit.DAYS));
    }

    @Override
    public void disable()
    {
        task.cancel();
    }

    private record LogDeletionTime(File logFolder, long timeToDelete)
    {
        private LogDeletionTime(String filePath, String configPath)
        {
            this(new File(filePath), TimeUnit.DAYS.toMillis(AntiCheatAddition.getInstance().getConfig().getLong("LogBot" + configPath, 10)));
        }

        public boolean isActive()
        {
            return timeToDelete > 0;
        }

        public void handleLog(final long currentTime)
        {
            // The folder exists.
            if (!logFolder.exists()) {
                Log.severe(() -> "Could not find log folder " + logFolder.getName());
                return;
            }

            final File[] files = logFolder.listFiles();
            if (files == null) return;

            for (File file : files) {
                final String fileName = file.getName();
                // Be sure it is a log file of AntiCheatAddition (.log) or a log file of the server (.log.gz)
                if ((fileName.endsWith(".log") || fileName.endsWith(".log.gz")) && currentTime - file.lastModified() > timeToDelete) {
                    if (file.delete()) Log.info(() -> "Deleted " + fileName);
                    else Log.severe(() -> "Could not delete old file " + fileName);
                }
            }
        }
    }
}