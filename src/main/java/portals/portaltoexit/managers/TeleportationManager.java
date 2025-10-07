package portals.portaltoexit.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

public class TeleportationManager {
    private final Portaltoexit plugin;

    public TeleportationManager(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    public void teleportPlayer(Player player, Portal portal) {
        // Check activation requirements first
        if (plugin.getActivationManager() != null) {
            ActivationManager.ActivationResult result = plugin.getActivationManager().checkActivationRequirements(player, portal);
            if (!result.isSuccess()) {
                player.sendMessage(result.getFailureMessage());
                return;
            }
        }

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId())) {
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-active", "{seconds}", String.valueOf(remaining)));
            return;
        }

        // Check economy cost
        double cost = portal.getCost() > 0 ? portal.getCost() : plugin.getConfigManager().getDefaultPortalCost();
        if (cost > 0 && plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
            // Check if player has bypass permission
            if (!player.hasPermission("portal2exit.bypass.cost")) {
                if (!plugin.getEconomyManager().hasBalance(player, cost)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds",
                        "{amount}", plugin.getEconomyManager().format(cost)));
                    return;
                }

                // Process payment
                double ownerPercentage = plugin.getConfigManager().getOwnerRevenuePercentage();
                if (!plugin.getEconomyManager().processPortalPayment(player, portal.getOwner(), cost, ownerPercentage)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("economy-error"));
                    return;
                }
            }
        }

        // Determine exit location
        Location exitLocation = getExitLocation(player, portal);

        if (exitLocation == null) {
            player.sendMessage("§cUnable to find a valid exit location!");
            return;
        }

        // Ensure safety if configured
        if (plugin.getConfigManager().isSafetyCheckEnabled()) {
            exitLocation = ensureSafeLocation(exitLocation);
        }

        // Play effects at departure
        if (plugin.getConfigManager().areParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
        }

        if (plugin.getConfigManager().areSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 1.0f);
        }

        // Teleport the player
        player.teleport(exitLocation);

        // Process activation after successful teleportation (consume items, give kits)
        if (plugin.getActivationManager() != null) {
            plugin.getActivationManager().processActivation(player, portal);
        }

        // Play effects at arrival
        if (plugin.getConfigManager().areParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.PORTAL, exitLocation, 50, 0.5, 1, 0.5, 0.1);
        }

        if (plugin.getConfigManager().areSoundsEnabled()) {
            player.getWorld().playSound(exitLocation, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);
        }

        // Set cooldown
        plugin.getCooldownManager().setCooldown(player.getUniqueId());

        // Send message
        player.sendMessage(plugin.getConfigManager().getMessage("portal-used", "{name}", portal.getName()));
    }

    /**
     * Gets exit location based on the portal's selection mode (FIRST, RANDOM, SEQUENTIAL, NEAREST)
     */
    private Location getExitBySelectionMode(Player player, Portal portal) {
        if (portal.getExitPoints().isEmpty()) {
            return portal.getCustomExit();
        }

        switch (portal.getSelectionMode()) {
            case FIRST:
                return portal.getExitPoints().get(0);

            case RANDOM:
                return portal.getRandomExitPoint();

            case SEQUENTIAL:
                return portal.getSequentialExitPoint();

            case NEAREST:
                Location nearest = portal.getNearestExitPoint(player.getLocation());
                return nearest != null ? nearest : portal.getExitPoints().get(0);

            default:
                return portal.getExitPoints().get(0);
        }
    }

    private Location getExitLocation(Player player, Portal portal) {
        // Add null safety for world
        if (player.getWorld() == null) {
            plugin.getLogger().warning("Player world is null for " + player.getName());
            return null;
        }

        switch (portal.getExitType()) {
            case SPAWN:
                Location spawnLoc = player.getWorld().getSpawnLocation();
                return spawnLoc != null ? spawnLoc : player.getLocation();

            case BED:
                Location bedLocation = player.getBedSpawnLocation();
                if (bedLocation != null) {
                    return bedLocation;
                } else {
                    // Fall back to spawn if no bed
                    Location spawn = player.getWorld().getSpawnLocation();
                    return spawn != null ? spawn : player.getLocation();
                }

            case CUSTOM:
                // Use the specific selection mode for CUSTOM type
                Location customExit = getExitBySelectionMode(player, portal);
                if (customExit != null) {
                    return customExit.clone();
                } else {
                    // Fall back to default custom exit from config
                    Location configExit = plugin.getConfigManager().getCustomExitLocation();
                    if (configExit != null) {
                        return configExit.clone();
                    } else {
                        Location spawn = player.getWorld() != null ? player.getWorld().getSpawnLocation() : null;
                        return spawn != null ? spawn : player.getLocation();
                    }
                }

            case RANDOM:
                // RANDOM type always uses random selection regardless of selection mode
                Location randomExit = portal.getRandomExitPoint();
                if (randomExit != null) {
                    return randomExit.clone();
                } else {
                    // Fall back to default custom exit from config
                    Location configExit = plugin.getConfigManager().getCustomExitLocation();
                    if (configExit != null) {
                        return configExit.clone();
                    } else {
                        Location spawn = player.getWorld() != null ? player.getWorld().getSpawnLocation() : null;
                        return spawn != null ? spawn : player.getLocation();
                    }
                }

            default:
                return player.getWorld().getSpawnLocation();
        }
    }

    public Location ensureSafeLocation(Location location) {
        // First, check if the current location is safe
        if (isSafeLocation(location)) {
            return location;
        }

        // If finding safe location is disabled, return the original
        if (!plugin.getConfigManager().shouldFindSafeLocation()) {
            return location;
        }

        // Search for a safe location nearby
        int searchRadius = plugin.getConfigManager().getSafeLocationSearchRadius();
        Location safeLocation = findSafeLocation(location, searchRadius);

        return safeLocation != null ? safeLocation : location;
    }

    private Location findSafeLocation(Location center, int radius) {
        // Search in a spiral pattern for a safe location
        for (int y = 0; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location testLoc = center.clone().add(x, y, z);
                    if (isSafeLocation(testLoc)) {
                        return testLoc;
                    }
                }
            }
        }

        // Search downward if no safe location found above
        for (int y = -1; y >= -radius; y--) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location testLoc = center.clone().add(x, y, z);
                    if (isSafeLocation(testLoc)) {
                        return testLoc;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        // Check that feet and head are air (or passable)
        if (!isPassable(feet) || !isPassable(head)) {
            return false;
        }

        // Check that ground is solid
        if (!ground.getType().isSolid()) {
            return false;
        }

        // Check for dangerous blocks
        if (isDangerous(ground.getType())) {
            return false;
        }

        return true;
    }

    private boolean isPassable(Block block) {
        Material type = block.getType();
        // Check for air differently for 1.12.2 compatibility
        if (type == Material.AIR) return true;
        if (type.isSolid()) return false;

        // Use material names that exist in all versions
        String typeName = type.name();
        return typeName.equals("WATER") ||
               typeName.contains("GRASS") ||
               typeName.equals("VINE") ||
               typeName.contains("TALL_GRASS") ||
               !type.isSolid();
    }

    private boolean isDangerous(Material material) {
        String name = material.name();
        return material == Material.LAVA || material == Material.FIRE ||
               material == Material.CACTUS ||
               name.contains("MAGMA") ||  // MAGMA_BLOCK in newer versions
               name.contains("WITHER") ||  // WITHER_ROSE in newer versions
               name.contains("BERRY");  // SWEET_BERRY_BUSH in newer versions
    }
}