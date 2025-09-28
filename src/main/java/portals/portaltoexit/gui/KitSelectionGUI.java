package portals.portaltoexit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Kit;
import portals.portaltoexit.data.Portal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KitSelectionGUI {
    private static final String GUI_TITLE = ChatColor.DARK_AQUA + "Kit Selection";
    private static final int ITEMS_PER_PAGE = 28;

    public static void openKitSelectionGUI(Player player, Portal portal, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill borders with glass panes
        fillBorders(gui);

        // Get kits from KitManager
        Portaltoexit plugin = Portaltoexit.getInstance();
        Map<String, Kit> allKits = plugin.getKitManager().getKits();

        // Convert to array for pagination
        String[] kitNames = allKits.keySet().toArray(new String[0]);

        // If no kits available, show message
        if (kitNames.length == 0) {
            gui.setItem(22, createItem(Material.PAPER, ChatColor.YELLOW + "No Kits Available",
                Arrays.asList(ChatColor.GRAY + "No kits are configured",
                             ChatColor.GRAY + "Contact an administrator")));
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) kitNames.length / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, kitNames.length);

        // Add kit items
        int slot = 10; // Start at row 1, column 1
        for (int i = startIndex; i < endIndex; i++) {
            String kitName = kitNames[i];
            Kit kit = allKits.get(kitName);

            if (kit != null) {
                gui.setItem(slot, createKitItem(kit, portal.getKitName()));

                // Move to next slot (skip border slots)
                slot++;
                if ((slot + 1) % 9 == 0) { // End of row
                    slot += 2; // Skip to next row, column 1
                }
                if (slot >= 44) break; // Don't go into bottom border
            }
        }

        // Add "No Kit" option
        gui.setItem(45, createNoKitItem(portal.getKitName()));

        // Navigation buttons
        if (page > 0) {
            gui.setItem(48, createPreviousPageItem());
        }

        if (page < totalPages - 1) {
            gui.setItem(50, createNextPageItem());
        }

        // Back and close buttons
        gui.setItem(52, createBackItem());
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

    private static ItemStack createKitItem(Kit kit, String currentKitName) {
        boolean isSelected = kit.getName().equals(currentKitName);
        Material material = isSelected ? Material.CHEST : Material.BARREL;

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Items: " + ChatColor.WHITE + kit.getItems().size(),
            "",
            isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to select this kit"
        );

        String displayName = (isSelected ? ChatColor.GREEN : ChatColor.AQUA) + kit.getName();

        return createItem(material, displayName, lore);
    }

    private static ItemStack createNoKitItem(String currentKitName) {
        boolean isSelected = currentKitName == null;
        Material material = isSelected ? Material.BARRIER : Material.STRUCTURE_VOID;

        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Remove kit assignment from portal",
            "",
            isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to select"
        );

        String displayName = (isSelected ? ChatColor.GREEN : ChatColor.RED) + "No Kit";

        return createItem(material, displayName, lore);
    }

    private static ItemStack createPreviousPageItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go to previous page"
        );

        return createItem(Material.ARROW, ChatColor.GOLD + "Previous Page", lore);
    }

    private static ItemStack createNextPageItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go to next page"
        );

        return createItem(Material.ARROW, ChatColor.GOLD + "Next Page", lore);
    }

    private static ItemStack createBackItem() {
        List<String> lore = Arrays.asList(
            ChatColor.YELLOW + "Click to go back to portal settings"
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
                // Filter out empty descriptions
                List<String> filteredLore = lore.stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
                meta.setLore(filteredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Check if a slot contains a kit item
     */
    public static boolean isKitSlot(int slot) {
        // Check if slot is in the kit area (rows 1-4, columns 1-7)
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7;
    }

    /**
     * Get kit name from slot position and page
     */
    public static String getKitNameFromSlot(int slot, int page, Player player) {
        if (!isKitSlot(slot)) {
            return null;
        }

        Portaltoexit plugin = Portaltoexit.getInstance();
        Map<String, Kit> allKits = plugin.getKitManager().getKits();
        String[] kitNames = allKits.keySet().toArray(new String[0]);

        // Calculate which kit this slot represents
        int row = slot / 9;
        int col = slot % 9;
        int slotIndex = (row - 1) * 7 + (col - 1); // Convert to 0-based index within kit area
        int kitIndex = page * ITEMS_PER_PAGE + slotIndex;

        if (kitIndex >= 0 && kitIndex < kitNames.length) {
            return kitNames[kitIndex];
        }

        return null;
    }

    /**
     * Check if slot is the "No Kit" option
     */
    public static boolean isNoKitSlot(int slot) {
        return slot == 45;
    }
}