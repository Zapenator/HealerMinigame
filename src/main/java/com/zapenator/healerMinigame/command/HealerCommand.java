package com.zapenator.healerMinigame.command;

import com.zapenator.healerMinigame.data.LeaderboardStore;
import com.zapenator.healerMinigame.game.ReviveMinigame;
import com.zapenator.healerMinigame.util.TimeFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles {@code /healer start|leaderboard|personalBest}.
 */
public class HealerCommand implements CommandExecutor, TabCompleter {

    private static final List<String> START = List.of("start", "play", "train");
    private static final List<String> LEADERBOARD = List.of("leaderboard", "lb", "top", "board");
    private static final List<String> PERSONAL_BEST = List.of("personalbest", "pb", "best", "mytime");

    private static final List<String> ALL_SUBCOMMANDS = new ArrayList<>();

    static {
        ALL_SUBCOMMANDS.add("start");
        ALL_SUBCOMMANDS.add("leaderboard");
        ALL_SUBCOMMANDS.add("personalBest");
    }

    private final ReviveMinigame minigame;
    private final LeaderboardStore leaderboard;

    public HealerCommand(ReviveMinigame minigame, LeaderboardStore leaderboard) {
        this.minigame = minigame;
        this.leaderboard = leaderboard;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (START.contains(sub)) {
            handleStart(sender);
        } else if (LEADERBOARD.contains(sub)) {
            handleLeaderboard(sender);
        } else if (PERSONAL_BEST.contains(sub)) {
            handlePersonalBest(sender);
        } else {
            sendUsage(sender);
        }
        return true;
    }

    private void handleStart(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can start the training minigame.", NamedTextColor.RED));
            return;
        }
        minigame.start(player);
    }

    private void handleLeaderboard(CommandSender sender) {
        List<LeaderboardStore.Entry> top = leaderboard.getTop(10);
        sender.sendMessage(Component.text("=== Healer Training Leaderboard ===", NamedTextColor.GOLD));
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("No times recorded yet. Be the first with /healer start!", NamedTextColor.GRAY));
            return;
        }
        int rank = 1;
        for (LeaderboardStore.Entry entry : top) {
            sender.sendMessage(Component.text("#" + rank + " ", NamedTextColor.YELLOW)
                    .append(Component.text(entry.name(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + TimeFormat.format(entry.millis()), NamedTextColor.AQUA)));
            rank++;
        }
    }

    private void handlePersonalBest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players have a personal best.", NamedTextColor.RED));
            return;
        }
        UUID uuid = player.getUniqueId();
        long best = leaderboard.getBestTime(uuid);
        if (best < 0) {
            sender.sendMessage(Component.text("You haven't completed a training run yet. Try /healer start!", NamedTextColor.GRAY));
            return;
        }
        int rank = leaderboard.getRank(uuid);
        player.sendMessage(Component.text("Your best time: ", NamedTextColor.GREEN)
                .append(Component.text(TimeFormat.format(best), NamedTextColor.YELLOW)));
        if (rank > 0) {
            player.sendMessage(Component.text("Leaderboard rank: #" + rank, NamedTextColor.AQUA));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Healer Training commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/healer start", NamedTextColor.YELLOW)
                .append(Component.text(" - begin a timed training run", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/healer leaderboard", NamedTextColor.YELLOW)
                .append(Component.text(" - view the fastest times", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/healer personalBest", NamedTextColor.YELLOW)
                .append(Component.text(" - view your best time", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String sub : ALL_SUBCOMMANDS) {
                if (sub.toLowerCase().startsWith(partial)) {
                    matches.add(sub);
                }
            }
            return matches;
        }
        return List.of();
    }
}
