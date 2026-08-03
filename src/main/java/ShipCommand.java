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
        if (!sender.hasPermission("pirateship.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length > 0) {
            // Спавн ивента
            if (args[0].equalsIgnoreCase("spawn")) {
                sender.sendMessage(ChatColor.YELLOW + "Запуск спавна пиратского корабля...");
                shipManager.spawnShipEvent();
                sender.sendMessage(ChatColor.GREEN + "Корабль успешно заспавнен!");
                return true;
            }

            // Остановка ивента
            if (args[0].equalsIgnoreCase("stop")) {
                boolean stopped = shipManager.stopShipEvent();
                if (stopped) {
                    sender.sendMessage(ChatColor.GREEN + "☠ Ивент остановлен: корабль убран, пираты уничтожены, приват снят!");
                } else {
                    sender.sendMessage(ChatColor.RED + "В данный момент активный ивент не найден.");
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Использование: /ps spawn или /ps stop");
        return true;
    }
}
