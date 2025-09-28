package portals.portaltoexit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PortalGUI {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Portal Management";

    public static void openPortalGUI(Player player, Portal portal) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill borders with glass panes
        fillBorders(gui);

        // Portal info item
        gui.setItem(13, createPortalInfoItem(portal));

        // Management buttons
        gui.setItem(20, createExitManagementItem(portal));
        gui.setItem(21, createExitModeItem(portal));
        gui.setItem(22, createParticleToggleItem(portal));
        gui.setItem(23, createCostSettingItem(portal));
        gui.setItem(24, createKitSettingItem(portal));

        // Owner/admin only options
        if (portal.getOwner().equals(player.getUniqueId()) || player.hasPermission("portal2exit.admin")) {
            gui.setItem(31, createDeletePortalItem());
        }

        // Navigation
        gui.setItem(45, createBackItem());
        gui.setItem(53, createCloseItem());

        player.openInventory(gui);
    }

    private static void fillBorders(Inventory gui) {
        ItemStack borderGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderGlass);
            gui.setItem(45 + i, borderGlass);
        }

        // Left and right columns
        for (int i = 1; i < 5; i++) {
            gui.setItem(i * 9, borderGlass);
            gui.setItem(i * 9 + 8, borderGlass);
        }
    }

    private static ItemStack createPortalInfoItem(Portal portal) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(portal.getOwner());
            meta.setOwningPlayer(owner);
            meta.setDisplayName(ChatColor.GOLD + portal.getName());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String createdDate = dateFormat.format(new Date(portal.getCreatedTime()));

            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Owner: " + ChatColor.WHITE + owner.getName(),
                ChatColor.GRAY + "Type: " + ChatColor.WHITE + portal.getExitType().toString(),
                ChatColor.GRAY + "Exit Points: " + ChatColor.WHITE + portal.getExitPoints().size(),
                ChatColor.GRAY + "Created: " + ChatColor.WHITE + createdDate,
                "",
                ChatColor.GRAY + "Location:",
                ChatColor.WHITE + "  World: " + portal.getLocation().getWorld().getName(),
                ChatColor.WHITE + "  X: " + portal.getLocation().getBlockX(),
                ChatColor.WHITE + "  Y: " + portal.getLocation().getBlockY(),
                ChatColor.WHITE + "  Z: " + portal.getLocation().getBlockZ(),
                "",
                ChatColor.YELLOW + "Portal Information"
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createExitManagementItem(Portal portal) {
        Material material = portal.getExitType() == Portal.ExitType.CUSTOM ?
            Material.ENDER_PEARL : Material.COMPASS;

        String exitCount = String.valueOf(portal.getExitPoints().size());

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Current exit points: " + ChatColor.WHITE + exitCount,
            "",
            ChatColor.YELLOW + "Click to manage exit points",
            ChatColor.GRAY + "Add, remove, or modify portal exits"
        );

        return createItem(material, ChatColor.GREEN + "Manage Exit Points", lore);
    }

    private static ItemStack createExitModeItem(Portal portal) {
        Material material = Material.REDSTONE_TORCH;
        String modeName = portal.getExitType().toString();

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Current mode: " + ChatColor.WHITE + modeName,
            "",
            ChatColor.YELLOW + "Available modes:",
            ChatColor.WHITE + "SPAWN - Always teleport to spawn",
            ChatColor.WHITE + "BED - Teleport to player's bed",
            ChatColor.WHITE + "CUSTOM - Use custom exit points",
            ChatColor.WHITE + "RANDOM - Random custom exit point",
            "",
            ChatColor.YELLOW + "Click to cycle through modes"
        );

        return createItem(material, ChatColor.BLUE + "Exit Mode: " + modeName, lore);
    }

    private static ItemStack createParticleToggleItem(Portal portal) {
        Material material = portal.isShowParticles() ? Material.GLOWSTONE_DUST : Material.GUNPOWDER;
        String status = portal.isShowParticles() ? "Enabled" : "Disabled";
        ChatColor color = portal.isShowParticles() ? ChatColor.GREEN : ChatColor.RED;

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Current status: " + color + status,
            "",
            ChatColor.YELLOW + "Click to toggle particle effects",
            ChatColor.GRAY + "Shows ambient particles around portal"
        );

        return createItem(material, color + "Particles: " + status, lore);
    }

    private static ItemStack createCostSettingItem(Portal portal) {
        Material material = portal.getCost() > 0 ? Material.GOLD_INGOT : Material.IRON_INGOT;
        String costText = portal.getCost() > 0 ? String.valueOf(portal.getCost()) : "Free";

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Current cost: " + ChatColor.WHITE + costText,
            "",
            ChatColor.YELLOW + "Left-click to increase cost",
            ChatColor.YELLOW + "Right-click to decrease cost",
            ChatColor.YELLOW + "Shift+click to set to zero"
        );

        return createItem(material, ChatColor.GOLD + "Usage Cost: " + costText, lore);
    }

    private static ItemStack createKitSettingItem(Portal portal) {
        Material material = portal.getKitName() != null ? Material.CHEST : Material.BARREL;
        String kitName = portal.getKitName() != null ? portal.getKitName() : "None";

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Current kit: " + ChatColor.WHITE + kitName,
            "",
            ChatColor.YELLOW + "Click to select a kit",
            ChatColor.GRAY + "Players will receive this kit when using portal"
        );

        return createItem(material, ChatColor.LIGHT_PURPLE + "Portal Kit: " + kitName, lore);
    }

    private static ItemStack createDeletePortalItem() {
        List<String> lore = Arrays.asList(
            ChatColor.RED + "WARNING: This action cannot be undone!",
            "",
            ChatColor.YELLOW + "Click to delete this portal",
            ChatColor.GRAY + "All data will be permanently lost"
        );

        return createItem(Material.TNT, ChatColor.RED + "Delete Portal", lore);
    }

    private static ItemStack createBackItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go back"
        );

        return createItem(Material.ARROW, ChatColor.GRAY + "Back", lore);
    }

    private static ItemStack createCloseItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to close this menu"
        );

        return createItem(Material.BARRIER, ChatColor.RED + "Close", lore);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}