package com.yourname.pirateship;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Vindicator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class ShipManager {

    private final Random random = new Random();
    private Location lastSpawnLocation = null;

    public void spawnShipEvent() {
        World world = Bukkit.getWorlds().get(0);
        Location spawnLoc = findSafeLocation(world);
        
        if (spawnLoc == null) {
            Bukkit.getLogger().warning("Не удалось найти безопасное место для корабля!");
            return;
        }

        // Если предыдущий корабль еще не был удален — удаляем его перед спавном нового
        if (lastSpawnLocation != null) {
            stopShipEvent();
        }

        this.lastSpawnLocation = spawnLoc;

        pasteSchematic(spawnLoc);
        createShipRegion(spawnLoc, world);
        spawnPirates(spawnLoc);

        String msg = String.format("&8[&c&lПИРАТЫ&8] &eКорабль бросил якорь на &cX: %d, Z: %d&e! Уничтожьте команду и заберите сокровища!", 
                spawnLoc.getBlockX(), spawnLoc.getBlockZ());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    // -------------------------------------------------------------
    // ПОЛНАЯ ОСТАНОВКА ИВЕНТАИ УДАЛЕНИЕ КОРАБЛЯ / МОБОВ / РЕГИОНА
    // -------------------------------------------------------------
    public boolean stopShipEvent() {
        World world = Bukkit.getWorlds().get(0);
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        
        boolean wasActive = false;

        // 1. Удаляем защитный регион WorldGuard
        if (rm != null && rm.hasRegion("pirate_ship_event")) {
            rm.removeRegion("pirate_ship_event");
            wasActive = true;
        }

        if (lastSpawnLocation != null) {
            // 2. Убиваем всех пиратов вокруг точки корабля (в радиусе 60 блоков)
            world.getNearbyEntities(lastSpawnLocation, 60, 60, 60).forEach(entity -> {
                if (entity.getCustomName() != null && entity.getCustomName().contains("Пират")) {
                    entity.remove();
                }
            });

            // 3. Очищаем блоки корабля (Стираем постройку и заполняем нижнюю часть водой)
            clearShipBlocks(lastSpawnLocation);
            
            lastSpawnLocation = null;
            wasActive = true;
        }

        return wasActive;
    }

    private void clearShipBlocks(Location loc) {
        World world = loc.getWorld();
        int locX = loc.getBlockX();
        int locY = loc.getBlockY();
        int locZ = loc.getBlockZ();

        // Проходим по кубу 60x60 блоков вокруг корабля
        for (int x = locX - 30; x <= locX + 30; x++) {
            for (int z = locZ - 30; z <= locZ + 30; z++) {
                for (int y = locY - 5; y <= locY + 45; y++) {
                    // Если блок находится ниже или на уровне океана (Y <= locY) — возвращаем ВОДУ
                    if (y <= locY) {
                        world.getBlockAt(x, y, z).setType(Material.WATER);
                    } else {
                        // Всё что выше уровня воды — очищаем ВОЗДУХОМ
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
    }

    private Location findSafeLocation(World world) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(8000) - 4000;
            int z = random.nextInt(8000) - 4000;
            
            int y = world.getHighestBlockYAt(x, z);
            Location checkLoc = new Location(world, x, y, z);
            
            while (checkLoc.getBlock().getType().name().contains("WATER") || 
                   checkLoc.getBlock().getType() == Material.AIR) {
                
                if (checkLoc.getBlock().getType() == Material.AIR && 
                    checkLoc.clone().add(0, -1, 0).getBlock().getType().name().contains("WATER")) {
                    break;
                }
                checkLoc.add(0, 1, 0);
                if (checkLoc.getBlockY() >= world.getMaxHeight() - 10) break;
            }

            if (!checkLoc.clone().add(0, -1, 0).getBlock().getType().name().contains("WATER")) {
                continue;
            }

            boolean isSafe = true;
            if (rm != null) {
                for (ProtectedRegion region : rm.getRegions().values()) {
                    BlockVector3 center = region.getMinimumPoint();
                    double distance = Math.sqrt(Math.pow(center.getX() - x, 2) + Math.pow(center.getZ() - z, 2));
                    if (distance < 700) {
                        isSafe = false;
                        break;
                    }
                }
            }
            if (isSafe) return checkLoc;
        }
        return null;
    }

    private void pasteSchematic(Location loc) {
        File schematicFile = new File(PirateShipPlugin.getInstance().getDataFolder(), "ship.schem");
        if (!schematicFile.exists()) return;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) return;

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboard = reader.read();
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(loc.getWorld()))) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        // Если нужно приподнять корабль над водой, измените y на loc.getY() + 3
                        .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(operation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createShipRegion(Location loc, World world) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rm == null) return;

        if (rm.hasRegion("pirate_ship_event")) {
            rm.removeRegion("pirate_ship_event");
        }

        BlockVector3 min = BlockVector3.at(loc.getX() - 30, loc.getY() - 5, loc.getZ() - 30);
        BlockVector3 max = BlockVector3.at(loc.getX() + 30, loc.getY() + 45, loc.getZ() + 30);
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("pirate_ship_event", min, max);
        
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);

        rm.addRegion(region);
    }

    private void spawnPirates(Location loc) {
        World world = loc.getWorld();
        
        for (int i = 0; i < 6; i++) {
            Location archerLoc = loc.clone().add(random.nextInt(10) - 5, 18, random.nextInt(10) - 5);
            Pillager p = (Pillager) world.spawnEntity(archerLoc, EntityType.PILLAGER);
            p.setCustomName(ChatColor.GOLD + "Пират-Снайпер");
            p.setCustomNameVisible(true);
        }

        for (int i = 0; i < 10; i++) {
            Location brawlerLoc = loc.clone().add(random.nextInt(14) - 7, 3, random.nextInt(14) - 7);
            Vindicator v = (Vindicator) world.spawnEntity(brawlerLoc, EntityType.VINDICATOR);
            v.setCustomName(ChatColor.RED + "Пират-Головорез");
            v.setCustomNameVisible(true);
        }
    }
}
