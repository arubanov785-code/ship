package com.yourname.pirateship;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class PirateShipPlugin extends JavaPlugin {

    private static PirateShipPlugin instance;
    private ShipManager shipManager;

    @Override
    public void onEnable() {
        instance = this;
        this.shipManager = new ShipManager();
        
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        startEventTimer();
        getLogger().info("Плагин PirateShipEvent успешно запущен!");
    }

    public static PirateShipPlugin getInstance() {
        return instance;
    }

    private void startEventTimer() {
        // 6 часов = 21 600 секунд
        // Планируем таймеры относительно цикла в 6 часов
        long sixHoursInTicks = 6 * 60 * 60 * 20L;
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Оповещение за 1 час
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&8[&c&lПИРАТЫ&8] &eКапитан Черная Борода поднимает паруса! Пиратский корабль с сокровищами прибудет через &c1 час&e!"));
            }, sixHoursInTicks - (60 * 60 * 20L));

            // Оповещение за 5 минут
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&8[&c&lПИРАТЫ&8] &eНа горизонте виднеются черные паруса! Корабль бросит якорь через &c5 минут&e!"));
            }, sixHoursInTicks - (5 * 60 * 20L));

            // Спавн корабля
            Bukkit.getScheduler().runTaskLater(this, () -> {
                shipManager.spawnShipEvent();
            }, sixHoursInTicks);

        }, 0L, sixHoursInTicks);
    }
}
