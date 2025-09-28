package portals.portaltoexit.managers;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import portals.portaltoexit.Portaltoexit;

public class ConfigManager {
    private final Portaltoexit plugin;

    public ConfigManager(Portaltoexit plugin) {
        this.plugin = plugin;
        plugin.reloadConfig();
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("plugin.debug", false);
    }

    public int getAutoSaveInterval() {
        return plugin.getConfig().getInt("plugin.auto-save-interval", 5);
    }

    public int getMaxPortalsPerPlayer() {
        return plugin.getConfig().getInt("portals.max-portals-per-player", 10);
    }

    public int getCooldownSeconds() {
        return plugin.getConfig().getInt("portals.cooldown", 3);
    }

    public boolean areSoundsEnabled() {
        return plugin.getConfig().getBoolean("portals.sounds-enabled", true);
    }

    public boolean areParticlesEnabled() {
        return plugin.getConfig().getBoolean("portals.particles-enabled", true);
    }

    public String getRequiredItem() {
        return plugin.getConfig().getString("portals.creation.required-item", "minecraft:ender_eye");
    }

    public String getRequiredBaseBlock() {
        return plugin.getConfig().getString("portals.creation.required-base-block", "minecraft:obsidian");
    }

    public boolean shouldConsumeItem() {
        return plugin.getConfig().getBoolean("portals.creation.consume-item", true);
    }

    public String getDefaultExitType() {
        return plugin.getConfig().getString("exit-portals.default-exit-type", "spawn");
    }

    public Location getCustomExitLocation() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("exit-portals.custom-exit");
        if (section == null) return null;

        String worldName = section.getString("world", "world");
        double x = section.getDouble("x", 0);
        double y = section.getDouble("y", 64);
        double z = section.getDouble("z", 0);
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
    }

    public boolean isSafetyCheckEnabled() {
        return plugin.getConfig().getBoolean("teleportation.safety-checks", true);
    }

    public boolean shouldFindSafeLocation() {
        return plugin.getConfig().getBoolean("teleportation.find-safe-location", true);
    }

    public int getSafeLocationSearchRadius() {
        return plugin.getConfig().getInt("teleportation.safe-location-search-radius", 5);
    }

    public String getMessage(String key) {
        // Check if key already has a full path
        if (key.startsWith("commands.") || key.startsWith("messages.")) {
            return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(key, "Message not found: " + key));
        }
        // Default to messages prefix
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("messages." + key, "Message not found: " + key));
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    // Portal particle effects configuration
    public boolean arePortalParticlesEnabled() {
        return plugin.getConfig().getBoolean("portal-effects.ambient-particles.enabled", true);
    }

    public double getPortalParticleRadius() {
        return plugin.getConfig().getDouble("portal-effects.ambient-particles.radius", 2.0);
    }

    public int getPortalParticleDensity() {
        return plugin.getConfig().getInt("portal-effects.ambient-particles.density", 10);
    }

    public double getPortalParticleHeight() {
        return plugin.getConfig().getDouble("portal-effects.ambient-particles.height", 3.0);
    }

    // Economy configuration
    public boolean isEconomyEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", false);
    }

    public double getDefaultPortalCost() {
        return plugin.getConfig().getDouble("economy.default-cost", 100.0);
    }

    public double getOwnerRevenuePercentage() {
        return plugin.getConfig().getDouble("economy.owner-gets-percentage", 0.1);
    }

    // Kit configuration
    public boolean areKitsEnabled() {
        return plugin.getConfig().getBoolean("kits.enabled", false);
    }

    public boolean shouldClearInventoryForKits() {
        return plugin.getConfig().getBoolean("kits.clear-inventory", false);
    }
}