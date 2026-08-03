package com.yourname.pirateship;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Vindicator;

import java.util.Random;

public class ShipManager {

    private final Random random = new Random();

    public void spawnShipEvent() {
        World world = Bukkit.getWorlds().get(0); // Обычно это world
        Location spawnLoc = findSafeLocation(world);
        
        if (spawnLoc == null) {
            Bukkit.getLogger().warning("Не удалось найти место для корабля!");
            return;
        }

        // 1. Вставка схематики (псевдокод, нужно адаптировать под вашу версию WE)
        // Здесь должен быть код загрузки ship.schem через WorldEdit Clipboard
        
        // 2. Создание региона WorldGuard
        createShipRegion(spawnLoc, world);
        
        // 3. Спавн пиратов
        spawnPirates(spawnLoc);

        // 4. Оповещение
        String msg = String.format("&8[&c&lПИРАТЫ&8] &eКорабль бросил якорь! Координаты: &cX: %d, Z: %d&e. Убейте команду, чтобы получить ключи от сокровищ!", 
                spawnLoc.getBlockX(), spawnLoc.getBlockZ());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private Location findSafeLocation(World world) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        
        for (int i = 0; i < 50; i++) { // 50 попыток найти место
            int x = random.nextInt(10000) - 5000;
            int z = random.nextInt(10000) - 5000;
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            // Проверка воды (корабль должен спавниться в океане)
            if (!loc.getBlock().getType().name().contains("WATER")) continue;

            // Проверка регионов (минимум 700 блоков)
            boolean safe = true;
            for (ProtectedRegion region : rm.getRegions().values()) {
                // Вычисляем примерное расстояние от центра региона
                double dx = region.getMinimumPoint().getX() - x;
                double dz = region.getMinimumPoint().getZ() - z;
                if (Math.sqrt(dx*dx + dz*dz) < 700) {
                    safe = false;
                    break;
                }
            }
            if (safe) return loc;
        }
        return null; // Если не нашли за 50 попыток
    }

    private void createShipRegion(Location loc, World world) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        
        // Примерный размер корабля (например 50x50)
        var min = BukkitAdapter.asBlockVector(loc.clone().add(-25, -10, -25));
        var max = BukkitAdapter.asBlockVector(loc.clone().add(25, 40, 25));
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("pirate_ship_event", min, max);
        
        // Флаги региона
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        // Запрет полета реализуется через сторонние плагины или кастомный флаг, 
        // но базово можно запретить команду /fly или использовать флаги из WG Extra Flags.
        
        rm.addRegion(region);
    }

    private void spawnPirates(Location loc) {
        World world = loc.getWorld();
        
        // Спавн Поборников (с топорами в трюмах и на палубе)
        for (int i = 0; i < 10; i++) {
            Vindicator v = (Vindicator) world.spawnEntity(loc.clone().add(random.nextInt(10), 2, random.nextInt(10)), EntityType.VINDICATOR);
            v.setCustomName(ChatColor.RED + "Пират-головорез");
            v.setCustomNameVisible(true);
        }

        // Спавн Разбойников (с арбалетами на мачтах, добавляем высоту Y)
        for (int i = 0; i < 5; i++) {
            Pillager p = (Pillager) world.spawnEntity(loc.clone().add(random.nextInt(5), 15, random.nextInt(5)), EntityType.PILLAGER);
            p.setCustomName(ChatColor.GOLD + "Пират-стрелок");
            p.setCustomNameVisible(true);
        }
    }
}
