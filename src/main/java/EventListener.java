package com.yourname.pirateship;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EventListener implements Listener {

    private final Random random = new Random();
    // Список открытых бочек на текущем ивенте (чтобы нельзя было открывать их бесконечно)
    private final Set<Location> openedBarrels = new HashSet<>();

    // -------------------------------------------------------------
    // ПРОВЕРКА РЕГИОНА И ПОЛЕТА
    // -------------------------------------------------------------
    
    private boolean isInShipRegion(Location loc) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if (rm == null) return false;
        
        ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase("pirate_ship_event")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pirateship.bypass.fly") || player.isOp()) return;

        if (event.isFlying() && isInShipRegion(player.getLocation())) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage(ChatColor.RED + "☠ Проклятие пиратского корабля не даёт вам взлететь!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("pirateship.bypass.fly") || player.isOp()) return;

        if (player.isFlying() && isInShipRegion(player.getLocation())) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.sendMessage(ChatColor.RED + "☠ Ваша магия полёта рассеялась над кораблем!");
        }
    }

    // -------------------------------------------------------------
    // ВЫПАДЕНИЕ КЛЮЧЕЙ С МОБОВ
    // -------------------------------------------------------------

    private ItemStack getPirateKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🗝 Ключ от пиратской бочки");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Нажмите ПКМ по бочке на пиратском корабле."));
        key.setItemMeta(meta);
        return key;
    }

    @EventHandler
    public void onPirateDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getCustomName() != null && entity.getCustomName().contains("Пират")) {
            // Шанс 40% выбить ключ
            if (random.nextInt(100) < 40) {
                event.getDrops().add(getPirateKey());
            }
        }
    }

    // -------------------------------------------------------------
    // ЛОГИКА ОТКРЫТИЯ БОЧЕК
    // -------------------------------------------------------------

    @EventHandler
    public void onBarrelInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BARREL) return;

        Location blockLoc = event.getClickedBlock().getLocation();

        // Проверяем, находится ли бочка в регионе пиратского корабля
        if (!isInShipRegion(blockLoc)) return;

        Player player = event.getPlayer();
        ItemStack itemHand = player.getInventory().getItemInMainHand();

        // Если бочка уже была взломана
        if (openedBarrels.contains(blockLoc)) {
            player.sendMessage(ChatColor.RED + "Эта бочка уже пуста и разграблена!");
            event.setCancelled(true);
            return;
        }

        // Проверяем, есть ли у игрока ключ в руке
        boolean isKey = itemHand.getType() == Material.TRIPWIRE_HOOK && 
                        itemHand.hasItemMeta() && 
                        itemHand.getItemMeta().getDisplayName().contains("Ключ от пиратской бочки");

        if (!isKey) {
            // Если ключа нет — замок не поддается
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "🔒 Бочка заперта на массивный замок! Нужен 🗝 Ключ от пиратской бочки.");
            return;
        }

        // --- У ИГРОКА ЕСТЬ КЛЮЧ ---
        event.setCancelled(true); // Отменяем дефолтное открытие, чтобы забить инвентарь кастомным лутом

        // Забираем 1 ключ
        itemHand.setAmount(itemHand.getAmount() - 1);

        // Помечаем бочку как открытую
        openedBarrels.add(blockLoc);

        // Заполняем бочку разнообразным лутом
        Barrel barrelState = (Barrel) event.getClickedBlock().getState();
        Inventory barrelInv = barrelState.getInventory();
        barrelInv.clear(); // Очищаем от случайных вещей схематики

        fillBarrelWithLoot(barrelInv);

        // Открываем инвентарь бочки игроку
        player.openInventory(barrelInv);
        player.sendMessage(ChatColor.GREEN + "🔓 Вы отперли пиратскую бочку ключом!");
    }

    // -------------------------------------------------------------
    // РАЗНООБРАЗНЫЙ ЛУТ И ЗАЧАРОВАНИЯ
    // -------------------------------------------------------------

    private void fillBarrelWithLoot(Inventory inv) {
        // Создаем пул вещей
        List<ItemStack> lootPool = new ArrayList<>();

        // 1. БРОНЯ (Различные вариации)
        lootPool.add(createEnchantedItem(Material.DIAMOND_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 1, Enchantment.DURABILITY, 2));
        lootPool.add(createEnchantedItem(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.DURABILITY, 1));
        lootPool.add(createEnchantedItem(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION_FIRE, 2, Enchantment.DURABILITY, 2));
        lootPool.add(createEnchantedItem(Material.DIAMOND_BOOTS, Enchantment.PROTECTION_FALL, 2, Enchantment.PROTECTION_ENVIRONMENTAL, 1));

        lootPool.add(createEnchantedItem(Material.NETHERITE_HELMET, Enchantment.PROTECTION_ENVIRONMENTAL, 1, Enchantment.WATER_WORKER, 1));
        lootPool.add(createEnchantedItem(Material.NETHERITE_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 2, Enchantment.PROTECTION_PROJECTILE, 2));
        lootPool.add(createEnchantedItem(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION_FIRE, 2, Enchantment.DURABILITY, 1));

        // 2. ОРУЖИЕ И ИНСТРУМЕНТЫ
        lootPool.add(createEnchantedItem(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 2, Enchantment.DURABILITY, 2));
        lootPool.add(createEnchantedItem(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 1, Enchantment.FIRE_ASPECT, 1));
        lootPool.add(createEnchantedItem(Material.DIAMOND_PICKAXE, Enchantment.DIG_SPEED, 3, Enchantment.DURABILITY, 2));
        lootPool.add(createEnchantedItem(Material.CROSSBOW, Enchantment.QUICK_CHARGE, 1, Enchantment.DURABILITY, 2));

        // 3. ДРАГОЦЕННОСТИ И РЕСУРСЫ
        lootPool.add(new ItemStack(Material.DIAMOND, random.nextInt(4) + 2)); // 2-5 алмазов
        lootPool.add(new ItemStack(Material.NETHERITE_INGOT, 1));             // 1 незеритовый слиток (редко!)
        lootPool.add(new ItemStack(Material.NETHERITE_SCRAP, random.nextInt(2) + 1)); // 1-2 обломка незерита
        lootPool.add(new ItemStack(Material.GOLD_BLOCK, random.nextInt(3) + 1));       // 1-3 золотых блока
        lootPool.add(new ItemStack(Material.EMERALD_BLOCK, random.nextInt(2) + 1));   // 1-2 изумрудных блока
        lootPool.add(new ItemStack(Material.IRON_INGOT, random.nextInt(12) + 6));     // 6-17 железа

        // 4. ПИРАТСКИЕ И ПОЛЕЗНЫЕ ПРИПАСЫ
        lootPool.add(new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(12) + 5)); // Бутылочки опыта
        lootPool.add(new ItemStack(Material.GOLDEN_APPLE, random.nextInt(3) + 1));        // Золотые яблоки
        lootPool.add(new ItemStack(Material.TNT, random.nextInt(6) + 2));                 // Динамит
        lootPool.add(new ItemStack(Material.FIREWORK_ROCKET, random.nextInt(16) + 8));    // Фейерверки для элитр
        lootPool.add(new ItemStack(Material.SPYGLASS, 1));                                // Подзорная труба Капитана

        // Выбираем от 3 до 6 случайных предметов из пула и раскладываем по слотам бочки (всего 27 слотов)
        int itemsToPlace = random.nextInt(4) + 3; // 3-6 предметов в бочке
        
        for (int i = 0; i < itemsToPlace; i++) {
            ItemStack randomLoot = lootPool.get(random.nextInt(lootPool.size()));
            int slot = random.nextInt(inv.getSize()); // Рандомный слот бочки
            inv.setItem(slot, randomLoot);
        }
    }

    // Вспомогательный метод для удобного создания зачарованных вещей
    private ItemStack createEnchantedItem(Material material, Enchantment ench1, int level1, Enchantment ench2, int level2) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(ench1, level1, true);
            if (ench2 != null) {
                meta.addEnchant(ench2, level2, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
