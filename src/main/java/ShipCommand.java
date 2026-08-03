package com.yourname.pirateship;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ShipCommand implements CommandExecutor {

    private final ShipManager shipManager;

    public ShipCommand(ShipManager shipManager) {
        this.shipManager = shipManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка прав (админ или консоль)
        if (!sender.hasPermission("pirateship.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) {
            sender.sendMessage(ChatColor.YELLOW + "Запуск принудительного спавна пиратского корабля...");
            
            // Вызываем спавн корабля
            shipManager.spawnShipEvent();
            
            sender.sendMessage(ChatColor.GREEN + "Ивент успешно запущен!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Использование: /pirateship spawn");
        return true;
    }
}
