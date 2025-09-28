package portals.portaltoexit.managers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;
import portals.portaltoexit.data.Kit;

import java.util.*;

/**
 * Manages portal activation requirements including items, permissions, and kits
 */
public class ActivationManager {
    private final Portaltoexit plugin;
    private boolean itemRequirementsEnabled;
    private boolean permissionRequirementsEnabled;
    private boolean kitRequirementsEnabled;
    private List<Portal.RequiredItem> defaultRequiredItems;
    private String defaultRequiredPermission;
    private boolean checkKitReceived;

    public ActivationManager(Portaltoexit plugin) {
        this.plugin = plugin;
        this.defaultRequiredItems = new ArrayList<>();
        loadConfiguration();
    }

    /**
     * Load activation configuration from config.yml
     */
    public void loadConfiguration() {
        defaultRequiredItems.clear();

        ConfigurationSection activationSection = plugin.getConfig().getConfigurationSection("activation");
        if (activationSection == null) {
            plugin.getLogger().warning("No activation section found in config.yml");
            return;
        }

        // Load item requirements
        ConfigurationSection itemSection = activationSection.getConfigurationSection("item-requirements");
        if (itemSection != null) {
            itemRequirementsEnabled = itemSection.getBoolean("enabled", true);

            List<Map<?, ?>> defaultItems = itemSection.getMapList("default-items");
            for (Map<?, ?> itemData : defaultItems) {
                try {
                    String materialName = (String) itemData.get("material");
                    Material material = Material.valueOf(materialName);
                    int amount = (Integer) itemData.get("amount");
                    boolean consume = (Boolean) itemData.get("consume");
                    String name = (String) itemData.get("name");
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) itemData.get("lore");

                    Portal.RequiredItem requiredItem = new Portal.RequiredItem(material, amount, consume, name, lore);
                    defaultRequiredItems.add(requiredItem);

                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Loaded default required item: " + requiredItem);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid default required item configuration: " + e.getMessage());
                }
            }
        }

        // Load permission requirements
        ConfigurationSection permSection = activationSection.getConfigurationSection("permission-requirements");
        if (permSection != null) {
            permissionRequirementsEnabled = permSection.getBoolean("enabled", true);
            defaultRequiredPermission = permSection.getString("default-permission", "portal2exit.use");
        }

        // Load kit requirements
        ConfigurationSection kitSection = activationSection.getConfigurationSection("kit-requirements");
        if (kitSection != null) {
            kitRequirementsEnabled = kitSection.getBoolean("enabled", true);
            checkKitReceived = kitSection.getBoolean("check-kit-received", true);
        }

        plugin.getLogger().info("Loaded activation requirements - Items: " + itemRequirementsEnabled +
                               ", Permissions: " + permissionRequirementsEnabled +
                               ", Kits: " + kitRequirementsEnabled);
    }

    /**
     * Check if a player can activate a portal based on all requirements
     * @param player The player attempting to use the portal
     * @param portal The portal being used
     * @return ActivationResult with success status and failure reason
     */
    public ActivationResult checkActivationRequirements(Player player, Portal portal) {
        // Check permission requirements
        if (permissionRequirementsEnabled) {
            String requiredPermission = portal.getRequiredPermission();
            if (requiredPermission == null) {
                requiredPermission = defaultRequiredPermission;
            }

            if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
                return new ActivationResult(false, "missing-permission",
                    plugin.getConfigManager().getMessage("missing-permission", "{permission}", requiredPermission));
            }
        }

        // Check kit requirements
        if (kitRequirementsEnabled && checkKitReceived) {
            String requiredKit = portal.getRequiredKit();
            if (requiredKit != null) {
                if (!hasReceivedKit(player, requiredKit)) {
                    return new ActivationResult(false, "missing-kit",
                        plugin.getConfigManager().getMessage("missing-required-kit", "{kit}", requiredKit));
                }
            }
        }

        // Check item requirements
        if (itemRequirementsEnabled) {
            List<Portal.RequiredItem> requiredItems = portal.getRequiredItems();
            if (requiredItems.isEmpty()) {
                requiredItems = defaultRequiredItems;
            }

            if (!requiredItems.isEmpty()) {
                ActivationResult itemResult = checkItemRequirements(player, requiredItems);
                if (!itemResult.isSuccess()) {
                    return itemResult;
                }
            }
        }

        return new ActivationResult(true, null, null);
    }

    /**
     * Process portal activation - consume items, give kits, etc.
     * @param player The player using the portal
     * @param portal The portal being used
     * @return true if processing was successful
     */
    public boolean processActivation(Player player, Portal portal) {
        try {
            // Consume required items
            if (itemRequirementsEnabled) {
                List<Portal.RequiredItem> requiredItems = portal.getRequiredItems();
                if (requiredItems.isEmpty()) {
                    requiredItems = defaultRequiredItems;
                }

                if (!consumeRequiredItems(player, requiredItems)) {
                    return false;
                }
            }

            // Give kit if specified
            String kitToGive = portal.getKitToGive();
            if (kitToGive == null) {
                kitToGive = portal.getKitName(); // Backward compatibility
            }

            if (kitToGive != null && plugin.getKitManager() != null) {
                // Use KitManager to apply the kit (handles cooldowns, permissions, etc.)
                plugin.getKitManager().applyKit(player, kitToGive);
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing portal activation for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if player has the required items
     * @param player The player to check
     * @param requiredItems List of required items
     * @return ActivationResult with success status
     */
    private ActivationResult checkItemRequirements(Player player, List<Portal.RequiredItem> requiredItems) {
        List<String> missingItems = new ArrayList<>();

        for (Portal.RequiredItem requiredItem : requiredItems) {
            if (!hasRequiredItem(player, requiredItem)) {
                missingItems.add(requiredItem.getAmount() + "x " +
                               formatMaterialName(requiredItem.getMaterial()));
            }
        }

        if (!missingItems.isEmpty()) {
            String itemsText = String.join(", ", missingItems);
            return new ActivationResult(false, "missing-items",
                plugin.getConfigManager().getMessage("missing-required-items", "{items}", itemsText));
        }

        return new ActivationResult(true, null, null);
    }

    /**
     * Check if player has a specific required item
     * @param player The player to check
     * @param requiredItem The required item
     * @return true if player has the item
     */
    private boolean hasRequiredItem(Player player, Portal.RequiredItem requiredItem) {
        int foundAmount = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != requiredItem.getMaterial()) {
                continue;
            }

            // Check display name if specified
            if (requiredItem.getDisplayName() != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !requiredItem.getDisplayName().equals(meta.getDisplayName())) {
                    continue;
                }
            }

            // Check lore if specified
            if (!requiredItem.getLore().isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null ||
                    !meta.getLore().equals(requiredItem.getLore())) {
                    continue;
                }
            }

            foundAmount += item.getAmount();
            if (foundAmount >= requiredItem.getAmount()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Consume required items from player's inventory
     * @param player The player
     * @param requiredItems List of items to consume
     * @return true if all items were consumed successfully
     */
    private boolean consumeRequiredItems(Player player, List<Portal.RequiredItem> requiredItems) {
        // First, verify we can consume all items
        for (Portal.RequiredItem requiredItem : requiredItems) {
            if (requiredItem.shouldConsume() && !hasRequiredItem(player, requiredItem)) {
                return false;
            }
        }

        // Then consume the items
        for (Portal.RequiredItem requiredItem : requiredItems) {
            if (requiredItem.shouldConsume()) {
                consumeItem(player, requiredItem);
            }
        }

        return true;
    }

    /**
     * Consume a specific item from player's inventory
     * @param player The player
     * @param requiredItem The item to consume
     */
    private void consumeItem(Player player, Portal.RequiredItem requiredItem) {
        int amountToConsume = requiredItem.getAmount();

        for (int i = 0; i < player.getInventory().getSize() && amountToConsume > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != requiredItem.getMaterial()) {
                continue;
            }

            // Check display name and lore matching (same logic as hasRequiredItem)
            if (requiredItem.getDisplayName() != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !requiredItem.getDisplayName().equals(meta.getDisplayName())) {
                    continue;
                }
            }

            if (!requiredItem.getLore().isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || meta.getLore() == null ||
                    !meta.getLore().equals(requiredItem.getLore())) {
                    continue;
                }
            }

            int consumeFromThisStack = Math.min(amountToConsume, item.getAmount());
            if (consumeFromThisStack >= item.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - consumeFromThisStack);
            }
            amountToConsume -= consumeFromThisStack;
        }
    }

    /**
     * Check if player has received a specific kit (placeholder implementation)
     * This would need to be integrated with a kit tracking system
     * @param player The player to check
     * @param kitName The kit name
     * @return true if player has received the kit
     */
    private boolean hasReceivedKit(Player player, String kitName) {
        // For now, check if the kit exists and player has permission
        // This could be expanded to track actual kit receipts
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            return false;
        }

        return kit.hasPermission(player);
    }

    /**
     * Format material name for display
     * @param material The material
     * @return Formatted name
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(Character.toUpperCase(word.charAt(0)))
                     .append(word.substring(1));
        }

        return formatted.toString();
    }

    /**
     * Reload activation configuration
     */
    public void reload() {
        loadConfiguration();
    }

    /**
     * Check if item requirements are enabled
     * @return true if enabled
     */
    public boolean areItemRequirementsEnabled() {
        return itemRequirementsEnabled;
    }

    /**
     * Check if permission requirements are enabled
     * @return true if enabled
     */
    public boolean arePermissionRequirementsEnabled() {
        return permissionRequirementsEnabled;
    }

    /**
     * Check if kit requirements are enabled
     * @return true if enabled
     */
    public boolean areKitRequirementsEnabled() {
        return kitRequirementsEnabled;
    }

    /**
     * Get default required items
     * @return List of default required items
     */
    public List<Portal.RequiredItem> getDefaultRequiredItems() {
        return new ArrayList<>(defaultRequiredItems);
    }

    /**
     * Get default required permission
     * @return Default permission string
     */
    public String getDefaultRequiredPermission() {
        return defaultRequiredPermission;
    }

    /**
     * Result of an activation check
     */
    public static class ActivationResult {
        private final boolean success;
        private final String failureReason;
        private final String failureMessage;

        public ActivationResult(boolean success, String failureReason, String failureMessage) {
            this.success = success;
            this.failureReason = failureReason;
            this.failureMessage = failureMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public String getFailureMessage() {
            return failureMessage;
        }
    }
}