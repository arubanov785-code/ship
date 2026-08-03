package com.yourname.pirateship;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Random;

public class EventListener implements Listener {

    private final Random random = new Random();

    private ItemStack getPirateKey() {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Ключ от пиратской бочки");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Используйте для открытия", ChatColor.GRAY + "бочек на пиратском корабле."));
        key.setItemMeta(meta);
        return key;
    }

    @EventHandler
    public void onPirateDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getCustomName() != null && entity.getCustomName().contains("Пират")) {
            // Шанс 30% на выпадение ключа
            if (random.nextInt(100) < 30) {
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

        // Проверяем, находится ли бочка в регионе корабля
        // (Для упрощения проверяем название ключа)
        if (item != null && item.getType() == Material.TRIPWIRE_HOOK && item.hasItemMeta() && 
            item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Ключ от пиратской бочки")) {
            
            event.setCancelled(true); // Отменяем обычное открытие бочки
            
            // Забираем ключ
            item.setAmount(item.getAmount() - 1);
            
            // Выдаем лут
            grantLoot(player);
            
            // Убираем бочку (или заменяем на сундук, чтобы нельзя было фармить)
            event.getClickedBlock().setType(Material.AIR);
            player.sendMessage(ChatColor.GREEN + "Вы успешно открыли пиратскую бочку!");
        }
    }

    private void grantLoot(Player player) {
        // Создаем массив возможного лута
        ItemStack diamondChest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        diamondChest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2);

        ItemStack netheriteSword = new ItemStack(Material.NETHERITE_SWORD);
        netheriteSword.addEnchantment(Enchantment.DAMAGE_ALL, 1);

        ItemStack diamonds = new ItemStack(Material.DIAMOND, random.nextInt(5) + 1);
        ItemStack emeralds = new ItemStack(Material.EMERALD, random.nextInt(10) + 1);
        ItemStack goldBlock = new ItemStack(Material.GOLD_BLOCK, random.nextInt(3) + 1);

        ItemStack[] possibleLoot = {diamondChest, netheriteSword, diamonds, emeralds, goldBlock};
        
        // Выдаем 2-3 случайных предмета
        int itemsCount = random.nextInt(2) + 2;
        for (int i = 0; i < itemsCount; i++) {
            ItemStack loot = possibleLoot[random.nextInt(possibleLoot.length)];
            player.getInventory().addItem(loot);
        }
    }
}
