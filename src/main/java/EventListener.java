package com.yourname.pirateship;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Random;

public class EventListener implements Listener {

    private final Random random = new Random();

    // -------------------------------------------------------------
    // ЛОГИКА ОГРАНИЧЕНИЯ ПОЛЕТА (ВКЛЮЧАЯ БАЙПАС)
    // -------------------------------------------------------------
    
    // Проверка, находится ли игрок в регионе корабля
    private boolean isInShipRegion(Player player) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
        if (rm == null) return false;
        
        ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase("pirate_ship_event")) {
                return true;
            }
        }
        return false;
    }

    // Если игрок нажал пробел, чтобы взлететь
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Пропускаем игроков с правами байпаса
        if (player.hasPermission("pirateship.bypass.fly") || player.isOp()) return;

        if (event.isFlying() && isInShipRegion(player)) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage(ChatColor.RED + "☠ Проклятие пиратского корабля не даёт вам взлететь!");
        }
    }

    // Если игрок влетел в область корабля уже в режиме полёта
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("pirateship.bypass.fly") || player.isOp()) return;

        if (player.isFlying() && isInShipRegion(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage(ChatColor.RED + "☠ Ваша магия полёта рассеялась над кораблем!");
        }
    }

    // -------------------------------------------------------------
    // КЛЮЧИ И ЛУТ С БОЧЕК
    // -------------------------------------------------------------

    private ItemStack getPirateKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🗝 Ключ от пиратской бочки");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Нажмите ПКМ по бочке на корабле."));
        key.setItemMeta(meta);
        return key;
    }

    @EventHandler
    public void onPirateDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getCustomName() != null && entity.getCustomName().contains("Пират")) {
            // Шанс 35% выбить ключ
            if (random.nextInt(100) < 35) {
                event.getDrops().add(getPirateKey());
            }
        }
    }

    @EventHandler
    public void onBarrelOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BARREL) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && 
            item.getItemMeta().getDisplayName().contains("Ключ от пиратской бочки")) {
            
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1); // Забираем 1 ключ
            
            giveLoot(player);
            event.getClickedBlock().setType(Material.AIR); // Уничтожаем бочку после открытия
            player.sendMessage(ChatColor.GREEN + "💰 Вы отперли пиратскую бочку!");
        }
    }

    private void giveLoot(Player player) {
        // Броня с Protection 1-2
        ItemStack diamondChest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        diamondChest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, random.nextInt(2) + 1);

        ItemStack netheriteHelm = new ItemStack(Material.NETHERITE_HELMET);
        netheriteHelm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        // Драгоценности (без вещей из Энда)
        ItemStack diamonds = new ItemStack(Material.DIAMOND, random.nextInt(4) + 2);
        ItemStack emeralds = new ItemStack(Material.EMERALD, random.nextInt(8) + 4);
        ItemStack goldIngot = new ItemStack(Material.GOLD_INGOT, random.nextInt(12) + 6);

        ItemStack[] lootTable = {diamondChest, netheriteHelm, diamonds, emeralds, goldIngot};
        
        // Выдаем 2 случайные вещи
        player.getInventory().addItem(lootTable[random.nextInt(lootTable.length)]);
        player.getInventory().addItem(lootTable[random.nextInt(lootTable.length)]);
    }
}
