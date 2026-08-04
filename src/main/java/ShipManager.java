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

        if (lastSpawnLocation != null) {
            stopShipEvent();
        }

        this.lastSpawnLocation = spawnLoc;

        pasteSchematic(spawnLoc);
        createShipRegion(spawnLoc, world);
        
        Bukkit.getScheduler().runTaskLater(PirateShipPlugin.getInstance(), () -> {
            spawnPirates(spawnLoc);
        }, 40L);

        String msg = String.format("&8[&c&lПИРАТЫ&8] &eКорабль бросил якорь на &cX: %d, Z: %d&e! Уничтожьте команду и заберите сокровища!", 
                spawnLoc.getBlockX(), spawnLoc.getBlockZ());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public boolean stopShipEvent() {
        World world = Bukkit.getWorlds().get(0);
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        
        boolean wasActive = false;

        if (rm != null && rm.hasRegion("pirate_ship_event")) {
            rm.removeRegion("pirate_ship_event");
            wasActive = true;
        }

        if (lastSpawnLocation != null) {
            // Убиваем мобов в радиусе 120 блоков
            world.getNearbyEntities(lastSpawnLocation, 120, 120, 120).forEach(entity -> {
                if (entity.getCustomName() != null && entity.getCustomName().contains("Пират")) {
                    entity.remove();
                }
            });

            clearShipBlocks(lastSpawnLocation);
            
            lastSpawnLocation = null;
            wasActive = true;
        }

        return wasActive;
    }

    private void clearShipBlocks(Location loc) {
        World world = loc.getWorld();

        world.getNearbyEntities(loc, 120, 120, 120).forEach(entity -> {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                entity.remove();
            }
        });

        // УВЕЛИЧЕННЫЙ РАДИУС: 100 блоков во все стороны (200x200 зона)
        int radius = 100;
        int minX = loc.getBlockX() - radius;
        int maxX = loc.getBlockX() + radius;
        int minZ = loc.getBlockZ() - radius;
        int maxZ = loc.getBlockZ() + radius;
        
        int waterY = loc.getBlockY() - 1;

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            
            BlockVector3 minWater = BlockVector3.at(minX, waterY - 15, minZ);
            BlockVector3 maxWater = BlockVector3.at(maxX, waterY, maxZ);
            com.sk89q.worldedit.regions.CuboidRegion waterRegion = new com.sk89q.worldedit.regions.CuboidRegion(minWater, maxWater);
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) waterRegion, BukkitAdapter.adapt(Material.WATER.createBlockData()));

            // Высота мачт очищается вплоть до +80 блоков вверх
            BlockVector3 minAir = BlockVector3.at(minX, waterY + 1, minZ);
            BlockVector3 maxAir = BlockVector3.at(maxX, waterY + 80, maxZ);
            com.sk89q.worldedit.regions.CuboidRegion airRegion = new com.sk89q.worldedit.regions.CuboidRegion(minAir, maxAir);
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) airRegion, BukkitAdapter.adapt(Material.AIR.createBlockData()));

            Operations.complete(editSession.commit());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bukkit.getScheduler().runTaskLater(PirateShipPlugin.getInstance(), () -> {
            world.getNearbyEntities(loc, 120, 120, 120).forEach(entity -> {
                if (entity.getType() == EntityType.DROPPED_ITEM) {
                    entity.remove();
                }
            });
        }, 20L);
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

        // Регион привата тоже увеличен до радиуса 100
        BlockVector3 min = BlockVector3.at(loc.getX() - 100, loc.getY() - 15, loc.getZ() - 100);
        BlockVector3 max = BlockVector3.at(loc.getX() + 100, loc.getY() + 80, loc.getZ() + 100);
        
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("pirate_ship_event", min, max);
        
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);

        rm.addRegion(region);
    }

    private void spawnPirates(Location loc) {
        World world = loc.getWorld();
        
        for (int i = 0; i < 6; i++) {
            int rx = loc.getBlockX() + random.nextInt(14) - 7;
            int rz = loc.getBlockZ() + random.nextInt(14) - 7;
            
            int highestY = world.getHighestBlockYAt(rx, rz);
            Location archerLoc = new Location(world, rx, highestY + 3, rz);
            Pillager p = (Pillager) world.spawnEntity(archerLoc, EntityType.PILLAGER);
            p.setCustomName(ChatColor.GOLD + "Пират-Снайпер");
            p.setCustomNameVisible(true);
        }

        for (int i = 0; i < 10; i++) {
            int rx = loc.getBlockX() + random.nextInt(20) - 10;
            int rz = loc.getBlockZ() + random.nextInt(20) - 10;
            
            int highestY = world.getHighestBlockYAt(rx, rz);
            Location brawlerLoc = new Location(world, rx, highestY + 2, rz);
            Vindicator v = (Vindicator) world.spawnEntity(brawlerLoc, EntityType.VINDICATOR);
            v.setCustomName(ChatColor.RED + "Пират-Головорез");
            v.setCustomNameVisible(true);
        }
    }
}
