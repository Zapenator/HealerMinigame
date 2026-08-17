package com.zapenator.healerMinigame;

import com.zapenator.healerMinigame.command.HealerCommand;
import com.zapenator.healerMinigame.data.LeaderboardStore;
import com.zapenator.healerMinigame.game.ReviveMinigame;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HealerMinigame extends JavaPlugin {

    private LeaderboardStore leaderboard;
    private ReviveMinigame minigame;

    @Override
    public void onEnable() {
        this.leaderboard = new LeaderboardStore(this);
        this.minigame = new ReviveMinigame(this, leaderboard);

        getServer().getPluginManager().registerEvents(minigame, this);

        HealerCommand handler = new HealerCommand(minigame, leaderboard);
        PluginCommand command = getCommand("healer");
        if (command != null) {
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().severe("Could not register /healer command — is it declared in plugin.yml?");
        }

        getLogger().info("HealerMinigame enabled.");
    }

    @Override
    public void onDisable() {
        if (minigame != null) {
            minigame.shutdown();
        }
    }
}
