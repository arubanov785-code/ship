package com.yourname.pirateship;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PirateShipPlugin extends JavaPlugin {

    private static PirateShipPlugin instance;
    private ShipManager shipManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Автоматически сохраняем ship.schem из ресурсов плагина, если его нет в папке
        if (!new File(getDataFolder(), "ship.schem").exists()) {
            saveResource("ship.schem", false);
        }

        this.shipManager = new ShipManager();
        
        // Регистрация событий
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        // Регистрация команды /pirateship (или /ps)
        if (getCommand("pirateship") != null) {
            getCommand("pirateship").setExecutor(new ShipCommand(shipManager));
        }

        startEventTimer();
        getLogger().info("Плагин PirateShipEvent успешно запущен!");
    }

    public static PirateShipPlugin getInstance() {
        return instance;
    }

    private void startEventTimer() {
        // 6 часов = 432 000 тиков
        long sixHoursInTicks = 432000L;
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            
            // Оповещение за 1 час (за 72000 тиков до спавна)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&8[&c&lПИРАТЫ&8] &eКапитан Черная Борода поднимает паруса! Пиратский корабль прибудет через &c1 час&e!"));
            }, sixHoursInTicks - 72000L);

            // Оповещение за 5 минут (за 6000 тиков до спавна)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&8[&c&lПИРАТЫ&8] &eНа горизонте виднеются черные паруса! Корабль бросит якорь через &c5 минут&e!"));
            }, sixHoursInTicks - 6000L);

            // Спавн корабля
            Bukkit.getScheduler().runTaskLater(this, () -> {
                shipManager.spawnShipEvent();
            }, sixHoursInTicks);

        }, 0L, sixHoursInTicks);
    }
}
