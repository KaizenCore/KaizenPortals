package portals.portaltoexit.managers;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import portals.portaltoexit.Portaltoexit;

import java.util.*;

/**
 * Manages permission-based portal limits and world restrictions
 */
public class PermissionManager {
    private final Portaltoexit plugin;
    private final Map<String, Integer> permissionLimits;
    private final Map<String, List<String>> worldRestrictions;
    private boolean worldRestrictionsEnabled;

    public PermissionManager(Portaltoexit plugin) {
        this.plugin = plugin;
        this.permissionLimits = new HashMap<>();
        this.worldRestrictions = new HashMap<>();
        loadConfiguration();
    }

    /**
     * Load permission configuration from config.yml
     */
    public void loadConfiguration() {
        permissionLimits.clear();
        worldRestrictions.clear();

        ConfigurationSection permSection = plugin.getConfig().getConfigurationSection("permissions");
        if (permSection == null) {
            plugin.getLogger().warning("No permissions section found in config.yml");
            return;
        }

        // Load portal limits
        ConfigurationSection limitsSection = permSection.getConfigurationSection("portal-limits");
        if (limitsSection != null) {
            for (String permission : limitsSection.getKeys(false)) {
                int limit = limitsSection.getInt(permission, 0);
                permissionLimits.put(permission, limit);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Loaded permission limit: " + permission + " -> " + limit);
                }
            }
        }

        // Load world restrictions
        ConfigurationSection worldSection = permSection.getConfigurationSection("world-restrictions");
        if (worldSection != null) {
            worldRestrictionsEnabled = worldSection.getBoolean("enabled", true);

            ConfigurationSection allowedWorldsSection = worldSection.getConfigurationSection("allowed-worlds");
            if (allowedWorldsSection != null) {
                for (String permission : allowedWorldsSection.getKeys(false)) {
                    List<String> worlds = allowedWorldsSection.getStringList(permission);
                    worldRestrictions.put(permission, worlds);
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Loaded world restriction: " + permission + " -> " + worlds);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + permissionLimits.size() + " permission limits and " +
                               worldRestrictions.size() + " world restrictions");
    }

    /**
     * Get the maximum number of portals a player can create based on their permissions
     * @param player The player to check
     * @return Maximum portal limit (0 = unlimited)
     */
    public int getMaxPortals(Player player) {
        int defaultLimit = plugin.getConfigManager().getMaxPortalsPerPlayer();
        int highestLimit = defaultLimit;

        // Check all permission-based limits and use the highest one
        for (Map.Entry<String, Integer> entry : permissionLimits.entrySet()) {
            String permission = entry.getKey();
            int limit = entry.getValue();

            if (player.hasPermission(permission)) {
                if (limit == 0) {
                    return 0; // Unlimited
                }
                if (highestLimit == 0 || limit > highestLimit) {
                    highestLimit = limit;
                }
            }
        }

        return highestLimit;
    }

    /**
     * Check if a player can create a portal in their current world
     * @param player The player to check
     * @return true if allowed, false if restricted
     */
    public boolean canCreateInWorld(Player player) {
        if (!worldRestrictionsEnabled) {
            return true;
        }

        String worldName = player.getWorld().getName();

        // Check each world restriction permission
        for (Map.Entry<String, List<String>> entry : worldRestrictions.entrySet()) {
            String permission = entry.getKey();
            List<String> allowedWorlds = entry.getValue();

            if (player.hasPermission(permission)) {
                // Check if "all" is in the allowed worlds list
                if (allowedWorlds.contains("all")) {
                    return true;
                }

                // Check if current world is in the allowed list
                if (allowedWorlds.contains(worldName)) {
                    return true;
                }
            }
        }

        // If no permissions match, check if player has a basic permission
        // This prevents players with no permissions from being completely blocked
        if (worldRestrictions.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Check if a player can use a portal in their current world
     * @param player The player to check
     * @return true if allowed, false if restricted
     */
    public boolean canUseInWorld(Player player) {
        // For now, using the same logic as creation
        // This could be expanded to have separate use vs create restrictions
        return canCreateInWorld(player);
    }

    /**
     * Check if a player has reached their portal limit
     * @param player The player to check
     * @param currentPortalCount The player's current number of portals
     * @return true if they can create more portals, false if at limit
     */
    public boolean canCreateMorePortals(Player player, int currentPortalCount) {
        int maxPortals = getMaxPortals(player);

        // 0 means unlimited
        if (maxPortals == 0) {
            return true;
        }

        return currentPortalCount < maxPortals;
    }

    /**
     * Get a list of worlds the player can create portals in
     * @param player The player to check
     * @return List of allowed world names
     */
    public List<String> getAllowedWorlds(Player player) {
        List<String> allowed = new ArrayList<>();

        if (!worldRestrictionsEnabled) {
            // If restrictions are disabled, all worlds are allowed
            plugin.getServer().getWorlds().forEach(world -> allowed.add(world.getName()));
            return allowed;
        }

        Set<String> worldSet = new HashSet<>();

        // Check each permission the player has
        for (Map.Entry<String, List<String>> entry : worldRestrictions.entrySet()) {
            String permission = entry.getKey();
            List<String> allowedWorlds = entry.getValue();

            if (player.hasPermission(permission)) {
                if (allowedWorlds.contains("all")) {
                    // Player has access to all worlds
                    plugin.getServer().getWorlds().forEach(world -> worldSet.add(world.getName()));
                    break;
                } else {
                    worldSet.addAll(allowedWorlds);
                }
            }
        }

        return new ArrayList<>(worldSet);
    }

    /**
     * Get a user-friendly message about portal limits for a player
     * @param player The player to check
     * @param currentPortalCount The player's current portal count
     * @return Formatted message about their limits
     */
    public String getPortalLimitMessage(Player player, int currentPortalCount) {
        int maxPortals = getMaxPortals(player);

        if (maxPortals == 0) {
            return "§aUnlimited portals (Current: " + currentPortalCount + ")";
        } else {
            return "§e" + currentPortalCount + "/" + maxPortals + " portals used";
        }
    }

    /**
     * Get a user-friendly message about world restrictions for a player
     * @param player The player to check
     * @return Formatted message about world access
     */
    public String getWorldAccessMessage(Player player) {
        if (!worldRestrictionsEnabled) {
            return "§aAll worlds accessible";
        }

        List<String> allowedWorlds = getAllowedWorlds(player);

        if (allowedWorlds.isEmpty()) {
            return "§cNo world access configured";
        }

        if (allowedWorlds.size() > 10) {
            return "§aAccess to " + allowedWorlds.size() + " worlds";
        }

        return "§aAllowed worlds: §e" + String.join(", ", allowedWorlds);
    }

    /**
     * Check if player has a specific permission with debug logging
     * @param player The player to check
     * @param permission The permission to check
     * @return true if player has permission
     */
    public boolean hasPermission(Player player, String permission) {
        boolean hasPermission = player.hasPermission(permission);

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Permission check: " + player.getName() +
                                  " -> " + permission + " = " + hasPermission);
        }

        return hasPermission;
    }

    /**
     * Reload permission configuration
     */
    public void reload() {
        loadConfiguration();
    }

    /**
     * Check if world restrictions are enabled
     * @return true if enabled
     */
    public boolean areWorldRestrictionsEnabled() {
        return worldRestrictionsEnabled;
    }

    /**
     * Get all configured permission limits
     * @return Map of permission to limit
     */
    public Map<String, Integer> getPermissionLimits() {
        return new HashMap<>(permissionLimits);
    }

    /**
     * Get all configured world restrictions
     * @return Map of permission to allowed worlds
     */
    public Map<String, List<String>> getWorldRestrictions() {
        return new HashMap<>(worldRestrictions);
    }
}