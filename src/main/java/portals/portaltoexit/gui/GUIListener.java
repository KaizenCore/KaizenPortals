package portals.portaltoexit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {
    private final Portaltoexit plugin;
    private final Map<UUID, Portal> playerPortalContext = new HashMap<>();
    private final Map<UUID, Integer> playerPageContext = new HashMap<>();
    private final Map<UUID, String> playerFilterContext = new HashMap<>();
    private final Map<UUID, String> playerPendingAction = new HashMap<>();
    private final Map<UUID, Location> playerSelectedLocation = new HashMap<>();

    public GUIListener(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!PortalWand.isPortalWand(item)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);

            // Check if player is near a portal
            Portal nearbyPortal = plugin.getPortalManager().getPortalAtLocation(player.getLocation());
            if (nearbyPortal != null) {
                // Check permissions
                if (!nearbyPortal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to manage this portal!");
                    return;
                }

                // Check if player has a selected location to add as exit point
                Location selectedLoc = playerSelectedLocation.get(player.getUniqueId());
                if (selectedLoc != null) {
                    nearbyPortal.addExitPoint(selectedLoc);
                    plugin.getPortalManager().savePortals();
                    player.sendMessage(ChatColor.GREEN + "Exit point added to portal " + nearbyPortal.getName() + "!");
                    playerSelectedLocation.remove(player.getUniqueId());

                    // Open the exit points GUI to show the new exit point
                    playerPortalContext.put(player.getUniqueId(), nearbyPortal);
                    ExitPointsGUI.openExitPointsGUI(player, nearbyPortal, 0);
                } else {
                    // No selected location, open portal management GUI
                    playerPortalContext.put(player.getUniqueId(), nearbyPortal);
                    PortalGUI.openPortalGUI(player, nearbyPortal);
                }
            } else {
                // No nearby portal - allow portal creation
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                    // Create portal at clicked block location
                    String portalName = generatePortalName(player);
                    Location portalLoc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

                    if (plugin.getPortalManager().createPortal(player, portalName, portalLoc)) {
                        player.sendMessage(ChatColor.GREEN + "Portal '" + portalName + "' created!");
                        player.sendMessage(ChatColor.YELLOW + "Use the wand on the portal to manage it.");
                        return;
                    }
                }

                // Fallback to opening portal list GUI
                player.sendMessage(ChatColor.YELLOW + "Opening portal list. Right-click a block to create a portal, or stand within 5 blocks of a portal to manage it directly.");
                PortalListGUI.openPortalListGUI(player, 0, null);
            }

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (event.getClickedBlock() != null) {
                Location clickedLocation = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
                playerSelectedLocation.put(player.getUniqueId(), clickedLocation);
                player.sendMessage(ChatColor.GREEN + "Location selected! Use the Portal Wand on a portal to add this as an exit point.");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if this is a Portal2Exit GUI
        if (!title.contains("Portal") && !title.contains("Exit Points") && !title.contains("Kit Selection")) {
            return;
        }

        // Cancel ALL interactions in Portal GUIs
        event.setCancelled(true);

        // Get the clicked inventory
        if (event.getClickedInventory() == null) {
            return;
        }

        // Check if click is in the GUI (top inventory) or player inventory (bottom)
        boolean isTopInventory = event.getClickedInventory().equals(event.getView().getTopInventory());

        // Prevent clicking in player inventory while GUI is open
        if (!isTopInventory) {
            return;
        }

        // Additional security - prevent any item movement
        if (event.getClick().isShiftClick() ||
            event.getAction().toString().contains("HOTBAR") ||
            event.getAction().toString().contains("DROP") ||
            event.getAction().toString().contains("MOVE") ||
            event.getAction().toString().contains("PLACE") ||
            event.getAction().toString().contains("SWAP")) {
            return; // Silently ignore these actions
        }

        // Get the actual slot that was clicked
        int slot = event.getSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (title.startsWith(ChatColor.DARK_PURPLE + "Portal Management")) {
            handlePortalGUIClick(player, event);
        } else if (title.startsWith(ChatColor.DARK_GREEN + "Exit Points Management")) {
            handleExitPointsGUIClick(player, event);
        } else if (title.startsWith(ChatColor.DARK_BLUE + "Portal List")) {
            handlePortalListGUIClick(player, event);
        } else if (title.startsWith(ChatColor.DARK_AQUA + "Kit Selection")) {
            handleKitSelectionGUIClick(player, event);
        }
    }

    private void handlePortalGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        Portal portal = playerPortalContext.get(player.getUniqueId());

        if (portal == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 20: // Manage Exit Points
                // Ensure portal context is maintained when opening exit points GUI
                playerPortalContext.put(player.getUniqueId(), portal);
                ExitPointsGUI.openExitPointsGUI(player, portal, 0);
                break;

            case 21: // Exit Mode
                cycleExitMode(player, portal);
                PortalGUI.openPortalGUI(player, portal);
                break;

            case 22: // Particle Toggle
                portal.setShowParticles(!portal.isShowParticles());
                plugin.getPortalManager().savePortals();
                player.sendMessage(ChatColor.GREEN + "Particles " +
                    (portal.isShowParticles() ? "enabled" : "disabled") + " for portal " + portal.getName());
                PortalGUI.openPortalGUI(player, portal);
                break;

            case 23: // Cost Setting
                handleCostSetting(player, portal, event.getClick());
                break;

            case 24: // Kit Setting
                // Open kit selection GUI
                playerPortalContext.put(player.getUniqueId(), portal);
                KitSelectionGUI.openKitSelectionGUI(player, portal, 0);
                break;

            case 31: // Delete Portal
                if (portal.getOwner().equals(player.getUniqueId()) || player.hasPermission("portal2exit.admin")) {
                    plugin.getPortalManager().removePortal(player, portal.getName());
                    player.closeInventory();
                    playerPortalContext.remove(player.getUniqueId());
                }
                break;

            case 45: // Back
                PortalListGUI.openPortalListGUI(player, 0, null);
                break;

            case 53: // Close
                player.closeInventory();
                break;
        }
    }

    private void handleExitPointsGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        Portal portal = playerPortalContext.get(player.getUniqueId());

        if (portal == null) {
            player.closeInventory();
            return;
        }

        if (ExitPointsGUI.isExitPointSlot(slot)) {
            int exitIndex = ExitPointsGUI.getExitPointIndex(slot);
            int currentPage = playerPageContext.getOrDefault(player.getUniqueId(), 0);
            int actualIndex = currentPage * 28 + exitIndex;

            if (actualIndex < portal.getExitPoints().size()) {
                Location exitPoint = portal.getExitPoints().get(actualIndex);

                if (event.getClick() == ClickType.LEFT) {
                    // Teleport to exit point
                    player.teleport(exitPoint);
                    player.sendMessage(ChatColor.GREEN + "Teleported to exit point!");
                } else if (event.getClick() == ClickType.RIGHT) {
                    // Remove exit point
                    portal.removeExitPoint(exitPoint);
                    plugin.getPortalManager().savePortals();
                    player.sendMessage(ChatColor.GREEN + "Exit point removed!");
                    ExitPointsGUI.openExitPointsGUI(player, portal, currentPage);
                }
            }
        } else {
            switch (slot) {
                case 45: // Add Exit Point
                    Location selectedLoc = playerSelectedLocation.get(player.getUniqueId());
                    if (selectedLoc != null) {
                        portal.addExitPoint(selectedLoc);
                        plugin.getPortalManager().savePortals();
                        player.sendMessage(ChatColor.GREEN + "Exit point added!");
                        playerSelectedLocation.remove(player.getUniqueId());
                    } else {
                        portal.addExitPoint(player.getLocation());
                        plugin.getPortalManager().savePortals();
                        player.sendMessage(ChatColor.GREEN + "Current location added as exit point!");
                    }
                    ExitPointsGUI.openExitPointsGUI(player, portal, playerPageContext.getOrDefault(player.getUniqueId(), 0));
                    break;

                case 46: // Selection Mode
                    if (portal.getExitType() == Portal.ExitType.CUSTOM && !portal.getExitPoints().isEmpty()) {
                        cycleSelectionMode(player, portal);
                    } else {
                        cycleExitMode(player, portal);
                    }
                    ExitPointsGUI.openExitPointsGUI(player, portal, playerPageContext.getOrDefault(player.getUniqueId(), 0));
                    break;

                case 48: // Previous Page
                    int prevPage = Math.max(0, playerPageContext.getOrDefault(player.getUniqueId(), 0) - 1);
                    playerPageContext.put(player.getUniqueId(), prevPage);
                    ExitPointsGUI.openExitPointsGUI(player, portal, prevPage);
                    break;

                case 50: // Next Page
                    int nextPage = playerPageContext.getOrDefault(player.getUniqueId(), 0) + 1;
                    playerPageContext.put(player.getUniqueId(), nextPage);
                    ExitPointsGUI.openExitPointsGUI(player, portal, nextPage);
                    break;

                case 52: // Back
                    // Ensure portal context is maintained when going back
                    playerPortalContext.put(player.getUniqueId(), portal);
                    PortalGUI.openPortalGUI(player, portal);
                    break;

                case 53: // Close
                    player.closeInventory();
                    break;
            }
        }
    }

    private void handlePortalListGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        int currentPage = playerPageContext.getOrDefault(player.getUniqueId(), 0);
        String currentFilter = playerFilterContext.get(player.getUniqueId());

        if (PortalListGUI.isPortalSlot(slot)) {
            Portal portal = PortalListGUI.getPortalFromSlot(slot, currentPage, currentFilter, player);
            if (portal != null) {
                if (event.getClick() == ClickType.LEFT) {
                    // Open portal management
                    playerPortalContext.put(player.getUniqueId(), portal);
                    PortalGUI.openPortalGUI(player, portal);
                } else if (event.getClick() == ClickType.RIGHT) {
                    // Teleport to portal with error handling
                    try {
                        if (portal.getLocation().getWorld() == null) {
                            player.sendMessage(ChatColor.RED + "Portal world is not loaded!");
                            return;
                        }

                        if (!portal.getLocation().getWorld().equals(player.getWorld()) &&
                            !player.hasPermission("portal2exit.crossworld")) {
                            player.sendMessage(ChatColor.RED + "You don't have permission to teleport to other worlds!");
                            return;
                        }

                        player.teleport(portal.getLocation());
                        player.sendMessage(ChatColor.GREEN + "Teleported to portal " + portal.getName() + "!");
                        player.closeInventory();
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Failed to teleport to portal!");
                        plugin.getLogger().warning("Teleportation failed for " + player.getName() + " to portal " + portal.getName() + ": " + e.getMessage());
                    }
                }
            }
        } else {
            switch (slot) {
                case 45: // Search
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Type the name of the portal you want to search for:");
                    playerPendingAction.put(player.getUniqueId(), "search");
                    break;

                case 46: // Refresh
                    PortalListGUI.openPortalListGUI(player, currentPage, currentFilter);
                    break;

                case 48: // Previous Page
                    int prevPage = Math.max(0, currentPage - 1);
                    playerPageContext.put(player.getUniqueId(), prevPage);
                    PortalListGUI.openPortalListGUI(player, prevPage, currentFilter);
                    break;

                case 50: // Next Page
                    int nextPage = currentPage + 1;
                    playerPageContext.put(player.getUniqueId(), nextPage);
                    PortalListGUI.openPortalListGUI(player, nextPage, currentFilter);
                    break;

                case 53: // Close
                    player.closeInventory();
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String pendingAction = playerPendingAction.get(player.getUniqueId());

        if (pendingAction == null) {
            return;
        }

        event.setCancelled(true);
        playerPendingAction.remove(player.getUniqueId());

        String message = event.getMessage().trim();

        if (pendingAction.equals("search")) {
            if (message.equalsIgnoreCase("cancel") || message.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Search cancelled.");
                Bukkit.getScheduler().runTask(plugin, () ->
                    PortalListGUI.openPortalListGUI(player, 0, null));
                return;
            }

            playerFilterContext.put(player.getUniqueId(), message);
            playerPageContext.put(player.getUniqueId(), 0);
            player.sendMessage(ChatColor.GREEN + "Searching for portals containing: " + message);

            Bukkit.getScheduler().runTask(plugin, () ->
                PortalListGUI.openPortalListGUI(player, 0, message));
        }
    }

    private void cycleExitMode(Player player, Portal portal) {
        Portal.ExitType currentType = portal.getExitType();
        Portal.ExitType newType;

        switch (currentType) {
            case SPAWN:
                newType = Portal.ExitType.BED;
                break;
            case BED:
                newType = Portal.ExitType.CUSTOM;
                break;
            case CUSTOM:
                newType = Portal.ExitType.RANDOM;
                break;
            case RANDOM:
                newType = Portal.ExitType.SPAWN;
                break;
            default:
                newType = Portal.ExitType.SPAWN;
                break;
        }

        portal.setExitType(newType);
        plugin.getPortalManager().savePortals();
        player.sendMessage(ChatColor.GREEN + "Exit mode changed to: " + newType.toString());
    }

    private void cycleSelectionMode(Player player, Portal portal) {
        Portal.ExitSelectionMode currentMode = portal.getSelectionMode();
        Portal.ExitSelectionMode newMode;

        switch (currentMode) {
            case FIRST:
                newMode = Portal.ExitSelectionMode.RANDOM;
                break;
            case RANDOM:
                newMode = Portal.ExitSelectionMode.SEQUENTIAL;
                break;
            case SEQUENTIAL:
                newMode = Portal.ExitSelectionMode.NEAREST;
                break;
            case NEAREST:
                newMode = Portal.ExitSelectionMode.FIRST;
                break;
            default:
                newMode = Portal.ExitSelectionMode.FIRST;
                break;
        }

        portal.setSelectionMode(newMode);
        plugin.getPortalManager().savePortals();
        player.sendMessage(ChatColor.GREEN + "Exit selection mode changed to: " + newMode.toString());
    }

    private void handleCostSetting(Player player, Portal portal, ClickType clickType) {
        double currentCost = portal.getCost();
        double newCost = currentCost;

        switch (clickType) {
            case LEFT:
                newCost = currentCost + 1.0;
                break;
            case RIGHT:
                newCost = Math.max(0, currentCost - 1.0);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                newCost = 0;
                break;
        }

        portal.setCost(newCost);
        plugin.getPortalManager().savePortals();

        String costText = newCost > 0 ? String.valueOf(newCost) : "Free";
        player.sendMessage(ChatColor.GREEN + "Portal cost set to: " + costText);

        PortalGUI.openPortalGUI(player, portal);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();

        // Cancel all drag events in Portal2Exit GUIs
        if (title.contains("Portal") || title.contains("Exit Points") || title.contains("Kit Selection")) {
            event.setCancelled(true);
        }
    }

    private void handleKitSelectionGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        Portal portal = playerPortalContext.get(player.getUniqueId());

        if (portal == null) {
            player.closeInventory();
            return;
        }

        int currentPage = playerPageContext.getOrDefault(player.getUniqueId(), 0);

        if (KitSelectionGUI.isKitSlot(slot)) {
            // Player selected a kit
            String kitName = KitSelectionGUI.getKitNameFromSlot(slot, currentPage, player);
            if (kitName != null) {
                portal.setKitName(kitName);
                plugin.getPortalManager().savePortals();
                player.sendMessage(ChatColor.GREEN + "Portal kit set to: " + kitName);

                // Go back to portal GUI
                playerPortalContext.put(player.getUniqueId(), portal);
                PortalGUI.openPortalGUI(player, portal);
            }
        } else if (KitSelectionGUI.isNoKitSlot(slot)) {
            // Player selected "No Kit"
            portal.setKitName(null);
            plugin.getPortalManager().savePortals();
            player.sendMessage(ChatColor.GREEN + "Portal kit removed");

            // Go back to portal GUI
            playerPortalContext.put(player.getUniqueId(), portal);
            PortalGUI.openPortalGUI(player, portal);
        } else {
            switch (slot) {
                case 48: // Previous Page
                    if (currentPage > 0) {
                        int prevPage = currentPage - 1;
                        playerPageContext.put(player.getUniqueId(), prevPage);
                        KitSelectionGUI.openKitSelectionGUI(player, portal, prevPage);
                    }
                    break;

                case 50: // Next Page
                    int nextPage = currentPage + 1;
                    playerPageContext.put(player.getUniqueId(), nextPage);
                    KitSelectionGUI.openKitSelectionGUI(player, portal, nextPage);
                    break;

                case 52: // Back
                    // Go back to portal GUI
                    playerPortalContext.put(player.getUniqueId(), portal);
                    PortalGUI.openPortalGUI(player, portal);
                    break;

                case 53: // Close
                    player.closeInventory();
                    break;
            }
        }
    }

    // Cleanup method to remove stale data
    public void cleanupPlayerData(UUID playerId) {
        playerPortalContext.remove(playerId);
        playerPageContext.remove(playerId);
        playerFilterContext.remove(playerId);
        playerPendingAction.remove(playerId);
        playerSelectedLocation.remove(playerId);
    }

    private String generatePortalName(Player player) {
        int count = plugin.getPortalManager().getPlayerPortals(player.getUniqueId()).size();
        String baseName = player.getName() + "_portal_";
        String portalName;
        int suffix = count + 1;

        do {
            portalName = baseName + suffix;
            suffix++;
        } while (plugin.getPortalManager().getPortal(portalName) != null);

        return portalName;
    }
}