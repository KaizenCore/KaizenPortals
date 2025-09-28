package portals.portaltoexit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PortalListGUI {
    private static final String GUI_TITLE = ChatColor.DARK_BLUE + "Portal List";
    private static final int PORTALS_PER_PAGE = 28; // 4 rows of 7 items each

    public static void openPortalListGUI(Player player, int page, String filter) {
        List<Portal> playerPortals = getAccessiblePortals(player);

        // Apply filter if provided
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            playerPortals = playerPortals.stream()
                .filter(portal -> portal.getName().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
        }

        String title = GUI_TITLE;
        if (filter != null && !filter.isEmpty()) {
            title += " (Filter: " + filter + ")";
        }
        title += " - Page " + (page + 1);

        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Fill borders with glass panes
        fillBorders(gui);

        // Display portals
        int startIndex = page * PORTALS_PER_PAGE;
        int endIndex = Math.min(startIndex + PORTALS_PER_PAGE, playerPortals.size());

        int slot = 10; // Start from second row
        for (int i = startIndex; i < endIndex; i++) {
            Portal portal = playerPortals.get(i);
            gui.setItem(slot, createPortalItem(portal, player));

            slot++;
            if ((slot + 1) % 9 == 0) { // Skip border slots
                slot += 2;
            }
        }

        // Add control buttons
        gui.setItem(45, createSearchItem());
        gui.setItem(46, createRefreshItem());

        // Navigation buttons
        if (page > 0) {
            gui.setItem(48, createPreviousPageItem());
        }

        if (endIndex < playerPortals.size()) {
            gui.setItem(50, createNextPageItem());
        }

        // Close button
        gui.setItem(53, createCloseItem());

        player.openInventory(gui);
    }

    private static List<Portal> getAccessiblePortals(Player player) {
        Portaltoexit plugin = Portaltoexit.getInstance();

        if (player.hasPermission("portal2exit.admin")) {
            // Admins can see all portals
            return plugin.getPortalManager().getAllPortals().stream().collect(Collectors.toList());
        } else {
            // Regular players only see their own portals
            return plugin.getPortalManager().getPlayerPortals(player.getUniqueId());
        }
    }

    private static void fillBorders(Inventory gui) {
        ItemStack borderGlass = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", null);

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

    private static ItemStack createPortalItem(Portal portal, Player viewer) {
        Material material = getPortalMaterial(portal);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + portal.getName());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String createdDate = dateFormat.format(new Date(portal.getCreatedTime()));

            Location loc = portal.getLocation();
            String distance = "N/A";
            if (loc.getWorld() == viewer.getWorld()) {
                distance = String.format("%.1f", viewer.getLocation().distance(loc));
            }

            List<String> lore = new ArrayList<>(Arrays.asList(
                ChatColor.GRAY + "Type: " + ChatColor.WHITE + portal.getExitType().toString(),
                ChatColor.GRAY + "Exit Points: " + ChatColor.WHITE + portal.getExitPoints().size(),
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + (portal.getCost() > 0 ? portal.getCost() : "Free"),
                ChatColor.GRAY + "Created: " + ChatColor.WHITE + createdDate,
                "",
                ChatColor.GRAY + "Location:",
                ChatColor.WHITE + "  " + loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")",
                ChatColor.GRAY + "Distance: " + ChatColor.WHITE + distance + " blocks",
                "",
                ChatColor.YELLOW + "Left-click to manage portal",
                ChatColor.YELLOW + "Right-click to teleport to portal"
            ));

            // Add owner info if viewer is admin
            if (viewer.hasPermission("portal2exit.admin")) {
                String ownerName = Bukkit.getOfflinePlayer(portal.getOwner()).getName();
                lore.add(2, ChatColor.GRAY + "Owner: " + ChatColor.WHITE + ownerName);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static Material getPortalMaterial(Portal portal) {
        switch (portal.getExitType()) {
            case SPAWN:
                return Material.RESPAWN_ANCHOR;
            case BED:
                return Material.RED_BED;
            case CUSTOM:
                return Material.ENDER_PEARL;
            case RANDOM:
                return Material.PRISMARINE_SHARD;
            default:
                return Material.NETHER_STAR;
        }
    }

    private static ItemStack createSearchItem() {
        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Search for portals by name",
            "",
            ChatColor.YELLOW + "Click to open search menu",
            ChatColor.GRAY + "Type in chat to filter portals"
        );

        return createItem(Material.SPYGLASS, ChatColor.AQUA + "Search Portals", lore);
    }

    private static ItemStack createRefreshItem() {
        List<String> lore = Arrays.asList(
            ChatColor.GRAY + "Refresh the portal list",
            "",
            ChatColor.YELLOW + "Click to reload all portals"
        );

        return createItem(Material.OBSERVER, ChatColor.GREEN + "Refresh", lore);
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

    public static Portal getPortalFromSlot(int slot, int page, String filter, Player player) {
        List<Portal> playerPortals = getAccessiblePortals(player);

        // Apply filter if provided
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            playerPortals = playerPortals.stream()
                .filter(portal -> portal.getName().toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
        }

        int index = getPortalIndex(slot, page);
        if (index >= 0 && index < playerPortals.size()) {
            return playerPortals.get(index);
        }
        return null;
    }

    private static int getPortalIndex(int slot, int page) {
        int baseIndex = -1;

        if (slot >= 10 && slot <= 16) baseIndex = slot - 10;
        else if (slot >= 19 && slot <= 25) baseIndex = slot - 19 + 7;
        else if (slot >= 28 && slot <= 34) baseIndex = slot - 28 + 14;
        else if (slot >= 37 && slot <= 43) baseIndex = slot - 37 + 21;

        if (baseIndex >= 0) {
            return page * PORTALS_PER_PAGE + baseIndex;
        }

        return -1;
    }

    public static boolean isPortalSlot(int slot) {
        return getPortalIndex(slot, 0) != -1;
    }
}