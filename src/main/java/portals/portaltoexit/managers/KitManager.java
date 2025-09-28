package portals.portaltoexit.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Kit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages kits and their application to players
 */
public class KitManager {
    private final Portaltoexit plugin;
    private final Map<String, Kit> kits;
    private final Map<UUID, Map<String, Long>> kitCooldowns;  // Player -> Kit -> Last use time
    private final File kitsFile;

    public KitManager(Portaltoexit plugin) {
        this.plugin = plugin;
        this.kits = new HashMap<>();
        this.kitCooldowns = new HashMap<>();
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    /**
     * Load kits from configuration
     */
    public void loadKits() {
        kits.clear();

        // Load from main config first - check for available-kits section
        ConfigurationSection kitsSection = plugin.getConfig().getConfigurationSection("kits.available-kits");
        if (kitsSection != null) {
            for (String kitName : kitsSection.getKeys(false)) {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
                if (kitSection != null) {
                    Map<String, Object> kitConfig = kitSection.getValues(true);
                    Kit kit = Kit.fromConfig(kitName, kitConfig);
                    kits.put(kitName.toLowerCase(), kit);
                }
            }
        }

        // Load from separate kits file if it exists
        if (kitsFile.exists()) {
            YamlConfiguration kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
            for (String kitName : kitsConfig.getKeys(false)) {
                ConfigurationSection kitSection = kitsConfig.getConfigurationSection(kitName);
                if (kitSection != null) {
                    Map<String, Object> kitConfig = kitSection.getValues(true);
                    Kit kit = Kit.fromConfig(kitName, kitConfig);
                    kits.put(kitName.toLowerCase(), kit);
                }
            }
        }

        plugin.getLogger().info("Loaded " + kits.size() + " kits");
    }

    /**
     * Save kits to file
     */
    public void saveKits() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Kit> entry : kits.entrySet()) {
            config.set(entry.getKey(), entry.getValue().serialize());
        }

        try {
            config.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save kits: " + e.getMessage());
        }
    }

    /**
     * Apply a kit to a player
     */
    public boolean applyKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            return false;
        }

        // Check permission
        if (!kit.hasPermission(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return false;
        }

        // Check cooldown
        if (isOnCooldown(player, kitName)) {
            int remaining = getRemainingCooldown(player, kitName);
            player.sendMessage(plugin.getConfigManager().getMessage("kit-cooldown",
                "{kit}", kit.getName(),
                "{time}", String.valueOf(remaining)));
            return false;
        }

        // Check economy cost
        if (kit.getCost() > 0 && plugin.getEconomyManager() != null && plugin.getEconomyManager().isEnabled()) {
            if (!plugin.getEconomyManager().hasBalance(player, kit.getCost())) {
                player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds",
                    "{amount}", plugin.getEconomyManager().format(kit.getCost())));
                return false;
            }

            if (!plugin.getEconomyManager().withdrawPlayer(player, kit.getCost())) {
                player.sendMessage(plugin.getConfigManager().getMessage("economy-error"));
                return false;
            }
        }

        // Apply the kit
        kit.applyToPlayer(player);

        // Set cooldown
        setCooldown(player, kitName, kit.getCooldown());

        // Send success message
        player.sendMessage(plugin.getConfigManager().getMessage("kit-received",
            "{kit}", kit.getName()));

        return true;
    }

    /**
     * Check if player is on cooldown for a kit
     */
    public boolean isOnCooldown(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null || kit.getCooldown() <= 0) {
            return false;
        }

        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return false;
        }

        Long lastUse = playerCooldowns.get(kitName.toLowerCase());
        if (lastUse == null) {
            return false;
        }

        long cooldownMs = kit.getCooldown() * 1000L;
        return System.currentTimeMillis() - lastUse < cooldownMs;
    }

    /**
     * Get remaining cooldown in seconds
     */
    public int getRemainingCooldown(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null || kit.getCooldown() <= 0) {
            return 0;
        }

        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }

        Long lastUse = playerCooldowns.get(kitName.toLowerCase());
        if (lastUse == null) {
            return 0;
        }

        long cooldownMs = kit.getCooldown() * 1000L;
        long remainingMs = cooldownMs - (System.currentTimeMillis() - lastUse);

        if (remainingMs <= 0) {
            playerCooldowns.remove(kitName.toLowerCase());
            return 0;
        }

        return (int) Math.ceil(remainingMs / 1000.0);
    }

    /**
     * Set cooldown for a player and kit
     */
    private void setCooldown(Player player, String kitName, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }

        kitCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(kitName.toLowerCase(), System.currentTimeMillis());
    }

    /**
     * Get a kit by name
     */
    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    /**
     * Add or update a kit
     */
    public void addKit(String name, Kit kit) {
        kits.put(name.toLowerCase(), kit);
    }

    /**
     * Remove a kit
     */
    public boolean removeKit(String name) {
        return kits.remove(name.toLowerCase()) != null;
    }

    /**
     * Get all kits
     */
    public Map<String, Kit> getKits() {
        return new HashMap<>(kits);
    }

    /**
     * Check if kit exists
     */
    public boolean kitExists(String name) {
        return kits.containsKey(name.toLowerCase());
    }

    /**
     * Clear player cooldowns (for cleanup)
     */
    public void clearPlayerCooldowns(UUID playerId) {
        kitCooldowns.remove(playerId);
    }

    /**
     * Reload kits
     */
    public void reload() {
        loadKits();
    }
}