package portals.portaltoexit.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import portals.portaltoexit.Portaltoexit;

import java.util.Arrays;
import java.util.List;

public class PortalWand {
    private static final String WAND_NAME = ChatColor.AQUA + "Portal Wand";
    private static final String WAND_IDENTIFIER = "portal_wand_v1";

    public static ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(WAND_NAME);

            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Right-click a portal to manage it",
                ChatColor.GRAY + "Left-click to select location for new exit",
                ChatColor.DARK_GRAY + "Portal2Exit Tool",
                ChatColor.DARK_GRAY + WAND_IDENTIFIER
            );

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            wand.setItemMeta(meta);
        }

        return wand;
    }

    public static boolean isPortalWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }

        List<String> lore = meta.getLore();
        return lore != null && lore.contains(ChatColor.DARK_GRAY + WAND_IDENTIFIER);
    }

    public static void giveWand(Player player) {
        // Check if player already has a wand
        for (ItemStack item : player.getInventory().getContents()) {
            if (isPortalWand(item)) {
                player.sendMessage(ChatColor.YELLOW + "You already have a Portal Wand!");
                return;
            }
        }

        ItemStack wand = createWand();
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(wand);
            player.sendMessage(ChatColor.GREEN + "You received a Portal Wand! Right-click portals to manage them.");
        } else {
            player.getWorld().dropItem(player.getLocation(), wand);
            player.sendMessage(ChatColor.YELLOW + "Your inventory is full! The Portal Wand was dropped on the ground.");
        }
    }
}