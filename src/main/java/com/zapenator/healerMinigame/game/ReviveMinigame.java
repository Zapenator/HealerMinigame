package com.zapenator.healerMinigame.game;

import com.zapenator.healerMinigame.HealerMinigame;
import com.zapenator.healerMinigame.data.LeaderboardStore;
import com.zapenator.healerMinigame.util.TimeFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The healer revive minigame, extracted from CivLabs' MiniEvents specialization
 * plugin so it can run standalone as a training exercise.
 * <p>
 * The minigame mechanics are unchanged from the original: a 54-slot inventory is
 * seeded with a shuffled set of "injury" and "healthy" items. Clicking an injury
 * converts it to a bandage; clicking a healthy organ spawns three fresh injuries.
 * Clearing every injury completes the revive. The only difference is how it is
 * accessed — instead of right-clicking a downed player with a bandage, a player
 * starts it on themselves via {@code /healer start}, and the run is timed.
 */
public class ReviveMinigame implements Listener {

    // --- Injury and healthy item tables (unchanged from the original minigame) ---
    private static final List<InjuryItem> INJURIES = List.of(
            new InjuryItem("Tumor", Material.SPIDER_EYE),
            new InjuryItem("Blood Clout", Material.REDSTONE),
            new InjuryItem("Severe Bleeding", Material.RED_DYE),
            new InjuryItem("Damaged Muscle", Material.BEEF),
            new InjuryItem("Infected Injury", Material.NETHER_WART),
            new InjuryItem("Brain Tumor", Material.NETHER_WART_BLOCK),
            new InjuryItem("Broken Bone", Material.BONE_MEAL)
    );
    private static final List<HealthyItem> HEALTHY_ITEMS = List.of(
            new HealthyItem("Healthy Liver", Material.FERMENTED_SPIDER_EYE),
            new HealthyItem("Healthy Stomach", Material.RABBIT_FOOT),
            new HealthyItem("Healthy Heart", Material.BEETROOT),
            new HealthyItem("Healthy Kidneys", Material.SWEET_BERRIES),
            new HealthyItem("Healthy Brain", Material.RED_GLAZED_TERRACOTTA),
            new HealthyItem("Healthy Bone", Material.BONE)
    );

    /** Title used to identify (and display on) the minigame inventory. */
    private static final String TITLE_PREFIX = "Healer Training";

    private final LeaderboardStore leaderboard;
    private final NamespacedKey injuryKey;

    private final Map<UUID, Session> sessions = new HashMap<>();

    public ReviveMinigame(HealerMinigame plugin, LeaderboardStore leaderboard) {
        this.leaderboard = leaderboard;
        this.injuryKey = new NamespacedKey(plugin, "revive_injury");
    }

    // -------------------------------
    // START
    // -------------------------------
    public boolean start(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a training run!", NamedTextColor.RED));
            return false;
        }

        Inventory inv = createReviveInventory();

        BossBar bar = Bukkit.createBossBar("Revive Progress", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(0.0);

        Session session = new Session(bar, System.currentTimeMillis());
        sessions.put(player.getUniqueId(), session);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        return true;
    }

    private boolean isMinigameView(String plainTitle) {
        return plainTitle.startsWith(TITLE_PREFIX);
    }

    // -------------------------------
    // CREATE INVENTORY
    // -------------------------------
    private Inventory createReviveInventory() {
        Inventory inv = Bukkit.createInventory(
                null, 54,
                Component.text(TITLE_PREFIX, NamedTextColor.BLACK)
        );

        // shuffle & place injuries
        List<InjuryItem> injuries = new ArrayList<>(INJURIES);
        Collections.shuffle(injuries);
        for (InjuryItem inj : injuries) {
            if (hasItemInInventory(inv, inj.mat())) continue; // skip if already present
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createInjuryItem(inj));
        }

        // place healthy items
        List<HealthyItem> healthyList = new ArrayList<>(HEALTHY_ITEMS);
        Collections.shuffle(healthyList);
        for (HealthyItem h : healthyList) {
            if (hasItemInInventory(inv, h.mat())) continue;
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createHealthyItem(h));
        }

        return inv;
    }

    private boolean hasItemInInventory(Inventory inv, Material mat) {
        return Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .anyMatch(i -> i.getType() == mat);
    }

    private Integer getRandomEmptySlot(Inventory inv) {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) {
                empty.add(i);
            }
        }
        if (empty.isEmpty()) return null;
        return empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
    }

    // -------------------------------
    // CREATE ITEMS
    // -------------------------------
    private ItemStack createInjuryItem(InjuryItem inj) {
        ItemStack item = new ItemStack(inj.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(inj.name(), NamedTextColor.RED));
        meta.getPersistentDataContainer().set(injuryKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHealthyItem(HealthyItem h) {
        ItemStack item = new ItemStack(h.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(h.name(), NamedTextColor.GREEN));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBandageItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text("Bandage", NamedTextColor.WHITE));
        meta.setEnchantmentGlintOverride(true);
        paper.setItemMeta(meta);
        return paper;
    }

    // -------------------------------
    // REROLL INJURIES
    // -------------------------------
    private void addRandomInjuries(Inventory inv, int amount) {
        List<InjuryItem> shuffled = new ArrayList<>(INJURIES);
        Collections.shuffle(shuffled);

        int placed = 0;
        for (InjuryItem inj : shuffled) {
            if (placed >= amount) break;
            if (hasItemInInventory(inv, inj.mat())) continue;
            Integer slot = getRandomEmptySlot(inv);
            if (slot == null) break;
            inv.setItem(slot, createInjuryItem(inj));
            placed++;
        }
    }

    // -------------------------------
    // BOSSBAR PROGRESS
    // -------------------------------
    private void updateBossBarProgress(Session session, Inventory inv) {
        BossBar bar = session.bar();
        if (bar == null) return;

        long totalInjuries = countInjuries(inv);
        double progress = 1.0 - ((double) totalInjuries / INJURIES.size());
        progress = Math.min(1.0, Math.max(0.0, progress));
        bar.setProgress(progress);
    }

    private long countInjuries(Inventory inv) {
        return Arrays.stream(inv.getContents())
                .filter(Objects::nonNull)
                .filter(ItemStack::hasItemMeta)
                .filter(item -> item.getItemMeta().getPersistentDataContainer().has(injuryKey, PersistentDataType.BYTE))
                .count();
    }

    private boolean allInjuriesCleared(Inventory inv) {
        return countInjuries(inv) == 0;
    }

    // -------------------------------
    // CLICK HANDLER
    // -------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!isMinigameView(title)) return;

        // Cancel all interaction while the minigame is open, so items can't be taken.
        e.setCancelled(true);

        // Only act on clicks landing inside the minigame (top) inventory.
        Inventory inv = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= inv.getSize()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        Material type = clicked.getType();

        // Clicked healthy item -> spawn more injuries
        boolean isHealthy = HEALTHY_ITEMS.stream().anyMatch(h -> h.mat() == type);
        if (isHealthy) {
            addRandomInjuries(inv, 3);
            updateBossBarProgress(session, inv);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1, 0.8f);
            return;
        }

        // Injury clicked -> replace with bandage
        boolean isInjury = clicked.hasItemMeta()
                && clicked.getItemMeta().getPersistentDataContainer().has(injuryKey, PersistentDataType.BYTE);
        if (!isInjury) return;

        inv.setItem(raw, createBandageItem());
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1.4f);
        updateBossBarProgress(session, inv);

        if (allInjuriesCleared(inv)) {
            complete(player, session);
        }
    }

    // -------------------------------
    // COMPLETE
    // -------------------------------
    private void complete(Player player, Session session) {
        long elapsed = System.currentTimeMillis() - session.startMillis();

        cleanup(player.getUniqueId(), session);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);

        boolean isBest = leaderboard.recordTime(player.getUniqueId(), player.getName(), elapsed);

        player.sendMessage(Component.text("Revive complete! ", NamedTextColor.GREEN)
                .append(Component.text("Time: " + TimeFormat.format(elapsed), NamedTextColor.YELLOW)));

        if (isBest) {
            player.sendMessage(Component.text("New personal best!", NamedTextColor.GOLD));
            int rank = leaderboard.getRank(player.getUniqueId());
            if (rank > 0) {
                player.sendMessage(Component.text("You are now #" + rank + " on the leaderboard.", NamedTextColor.AQUA));
            }
        } else {
            long best = leaderboard.getBestTime(player.getUniqueId());
            player.sendMessage(Component.text("Your best: " + TimeFormat.format(best), NamedTextColor.GRAY));
        }
    }

    // -------------------------------
    // CLOSE = CANCEL UNLESS FINISHED
    // -------------------------------
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!isMinigameView(title)) return;

        // If the inventory closed while injuries remain, the run is abandoned.
        if (!allInjuriesCleared(e.getInventory())) {
            cleanup(player.getUniqueId(), session);
            player.sendMessage(Component.text("Training run cancelled.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Session session = sessions.get(e.getPlayer().getUniqueId());
        if (session != null) {
            cleanup(e.getPlayer().getUniqueId(), session);
        }
    }

    private void cleanup(UUID uuid, Session session) {
        sessions.remove(uuid);
        if (session.bar() != null) {
            session.bar().removeAll();
        }
    }

    public void shutdown() {
        for (Session session : sessions.values()) {
            if (session.bar() != null) session.bar().removeAll();
        }
        sessions.clear();
    }

    // --- Records ---
    private record Session(BossBar bar, long startMillis) {
    }

    public record InjuryItem(String name, Material mat) {
    }

    public record HealthyItem(String name, Material mat) {
    }
}
