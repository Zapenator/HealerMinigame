package com.zapenator.healerMinigame.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists each player's best (lowest) completion time for the healer training
 * minigame to {@code leaderboard.yml} in the plugin's data folder.
 * <p>
 * Times are stored in milliseconds; a lower time is better.
 */
public class LeaderboardStore {

    private final Plugin plugin;
    private final File file;
    private FileConfiguration config;

    public LeaderboardStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboard.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create leaderboard.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save leaderboard.yml", e);
        }
    }

    /**
     * Records a completion time. Only stored if it beats the player's existing best.
     *
     * @return true if this became the player's new best time
     */
    public boolean recordTime(UUID uuid, String name, long millis) {
        String base = uuid.toString();
        config.set(base + ".name", name);

        long previous = config.getLong(base + ".bestMillis", Long.MAX_VALUE);
        if (millis < previous) {
            config.set(base + ".bestMillis", millis);
            save();
            return true;
        }
        // Still persist the (possibly updated) name.
        save();
        return false;
    }

    /**
     * @return the player's best time in millis, or -1 if they have none
     */
    public long getBestTime(UUID uuid) {
        return config.getLong(uuid + ".bestMillis", -1L);
    }

    /**
     * @return top entries sorted fastest-first, at most {@code limit} of them
     */
    public List<Entry> getTop(int limit) {
        List<Entry> entries = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            if (!config.contains(key + ".bestMillis")) continue;
            String name = config.getString(key + ".name", "Unknown");
            long millis = config.getLong(key + ".bestMillis");
            entries.add(new Entry(key, name, millis));
        }
        entries.sort(Comparator.comparingLong(Entry::millis));
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    /**
     * @return 1-based rank of the player on the leaderboard, or -1 if unranked
     */
    public int getRank(UUID uuid) {
        List<Entry> all = getTop(Integer.MAX_VALUE);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).uuid().equals(uuid.toString())) {
                return i + 1;
            }
        }
        return -1;
    }

    public record Entry(String uuid, String name, long millis) {
    }
}
