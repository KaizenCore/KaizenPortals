package portals.portaltoexit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import portals.portaltoexit.data.Portal;

import java.util.Arrays;
import java.util.List;

public class ExitPointsGUI {
    private static final String GUI_TITLE = ChatColor.DARK_GREEN + "Exit Points Management";
    private static final int ITEMS_PER_PAGE = 28; // 4 rows of 7 items each

    public static void openExitPointsGUI(Player player, Portal portal, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE + " - Page " + (page + 1));

        // Fill borders with glass panes
        fillBorders(gui);

        // Add current exit points
        List<Location> exitPoints = portal.getExitPoints();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, exitPoints.size());

        // Display exit points
        int slot = 10; // Start from second row
        for (int i = startIndex; i < endIndex; i++) {
            Location exit = exitPoints.get(i);
            gui.setItem(slot, createExitPointItem(exit, i + 1));

            slot++;
            if ((slot + 1) % 9 == 0) { // Skip border slots
                slot += 2;
            }
        }

        // Add new exit point button
        gui.setItem(45, createAddExitPointItem());

        // Selection mode button
        gui.setItem(46, createSelectionModeItem(portal));

        // Navigation buttons
        if (page > 0) {
            gui.setItem(48, createPreviousPageItem());
        }

        if (endIndex < exitPoints.size()) {
            gui.setItem(50, createNextPageItem());
        }

        // Back and close buttons
        gui.setItem(52, createBackItem());
        gui.setItem(53, createCloseItem());

        player.openInventory(gui);
    }

    private static void fillBorders(Inventory gui) {
        ItemStack borderGlass = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);

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

    private static ItemStack createExitPointItem(Location location, int index) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Exit Point #" + index);

            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "World: " + ChatColor.WHITE + location.getWorld().getName(),
                ChatColor.GRAY + "X: " + ChatColor.WHITE + location.getBlockX(),
                ChatColor.GRAY + "Y: " + ChatColor.WHITE + location.getBlockY(),
                ChatColor.GRAY + "Z: " + ChatColor.WHITE + location.getBlockZ(),
                "",
                ChatColor.YELLOW + "Left-click to teleport here",
                ChatColor.RED + "Right-click to remove"
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createAddExitPointItem() {
        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Add a new exit point to this portal",
            "",
            ChatColor.YELLOW + "Click to add your current location",
            ChatColor.GRAY + "Or use the Portal Wand to select a location"
        );

        return createItem(Material.EMERALD, ChatColor.GREEN + "Add Exit Point", lore);
    }

    private static ItemStack createSelectionModeItem(Portal portal) {
        Material material;
        String modeName;
        ChatColor color;

        if (portal.getExitType() == Portal.ExitType.CUSTOM && !portal.getExitPoints().isEmpty()) {
            // Show exit selection mode for custom portals with exit points
            switch (portal.getSelectionMode()) {
                case FIRST:
                    material = Material.COMPASS;
                    modeName = "First Exit Point";
                    color = ChatColor.BLUE;
                    break;
                case RANDOM:
                    material = Material.PRISMARINE_SHARD;
                    modeName = "Random Selection";
                    color = ChatColor.LIGHT_PURPLE;
                    break;
                case SEQUENTIAL:
                    material = Material.REPEATER;
                    modeName = "Sequential (Round-Robin)";
                    color = ChatColor.GREEN;
                    break;
                case NEAREST:
                    material = Material.RECOVERY_COMPASS;
                    modeName = "Nearest Exit Point";
                    color = ChatColor.AQUA;
                    break;
                default:
                    material = Material.STONE;
                    modeName = "Unknown";
                    color = ChatColor.GRAY;
                    break;
            }

            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Current selection mode: " + color + modeName,
                "",
                ChatColor.YELLOW + "Available selection modes:",
                ChatColor.WHITE + "FIRST - Always use first exit point",
                ChatColor.WHITE + "RANDOM - Randomly select from exit points",
                ChatColor.WHITE + "SEQUENTIAL - Cycle through in order",
                ChatColor.WHITE + "NEAREST - Use closest exit to player",
                "",
                ChatColor.YELLOW + "Click to cycle through selection modes"
            );

            return createItem(material, color + "Exit Selection: " + modeName, lore);
        } else {
            // Show exit type for non-custom portals or custom portals without exit points
            switch (portal.getExitType()) {
                case SPAWN:
                    material = Material.RESPAWN_ANCHOR;
                    modeName = "Spawn Point";
                    color = ChatColor.YELLOW;
                    break;
                case BED:
                    material = Material.RED_BED;
                    modeName = "Player Bed";
                    color = ChatColor.RED;
                    break;
                case CUSTOM:
                    material = Material.COMPASS;
                    modeName = "Custom Exit Points";
                    color = ChatColor.BLUE;
                    break;
                case RANDOM:
                    material = Material.PRISMARINE_SHARD;
                    modeName = "Random Exit Point";
                    color = ChatColor.LIGHT_PURPLE;
                    break;
                default:
                    material = Material.STONE;
                    modeName = "Unknown";
                    color = ChatColor.GRAY;
                    break;
            }

            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Current exit type: " + color + modeName,
                "",
                ChatColor.YELLOW + "Available exit types:",
                ChatColor.WHITE + "SPAWN - Always use spawn point",
                ChatColor.WHITE + "BED - Use player's bed location",
                ChatColor.WHITE + "CUSTOM - Use custom exit points",
                ChatColor.WHITE + "RANDOM - Legacy random mode",
                "",
                ChatColor.YELLOW + "Click to cycle through exit types"
            );

            return createItem(material, color + "Exit Type: " + modeName, lore);
        }
    }

    private static ItemStack createPreviousPageItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go to previous page"
        );

        return createItem(Material.ARROW, ChatColor.GRAY + "Previous Page", lore);
    }

    private static ItemStack createNextPageItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go to next page"
        );

        return createItem(Material.ARROW, ChatColor.GRAY + "Next Page", lore);
    }

    private static ItemStack createBackItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to return to portal management"
        );

        return createItem(Material.ARROW, ChatColor.GRAY + "Back to Portal", lore);
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

    public static int getExitPointIndex(int slot) {
        // Convert GUI slot to exit point index
        if (slot >= 10 && slot <= 16) return slot - 10;
        if (slot >= 19 && slot <= 25) return slot - 19 + 7;
        if (slot >= 28 && slot <= 34) return slot - 28 + 14;
        if (slot >= 37 && slot <= 43) return slot - 37 + 21;
        return -1;
    }

    public static boolean isExitPointSlot(int slot) {
        return getExitPointIndex(slot) != -1;
    }
}