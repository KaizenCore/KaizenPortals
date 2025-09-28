package portals.portaltoexit.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PortalManager {
    private final Portaltoexit plugin;
    private final Map<String, Portal> portals;
    private final File dataFile;

    public PortalManager(Portaltoexit plugin) {
        this.plugin = plugin;
        this.portals = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "portals.yml");
    }

    public void loadPortals() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (config.contains("portals")) {
            for (String key : config.getConfigurationSection("portals").getKeys(false)) {
                Portal portal = (Portal) config.get("portals." + key);
                if (portal != null) {
                    portals.put(portal.getName().toLowerCase(), portal);
                }
            }
        }

        plugin.getLogger().info("Loaded " + portals.size() + " portals.");
    }

    public void savePortals() {
        YamlConfiguration config = new YamlConfiguration();

        for (Portal portal : portals.values()) {
            config.set("portals." + portal.getName(), portal);
        }

        try {
            config.save(dataFile);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Saved " + portals.size() + " portals.");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save portals: " + e.getMessage());
        }
    }

    public boolean createPortal(Player player, String name, Location location) {
        String lowerName = name.toLowerCase();

        // Check if portal already exists
        if (portals.containsKey(lowerName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-already-exists", "{name}", name));
            return false;
        }

        // Check portal limit
        if (!canCreatePortal(player.getUniqueId())) {
            int max = plugin.getConfigManager().getMaxPortalsPerPlayer();
            player.sendMessage(plugin.getConfigManager().getMessage("max-portals-reached", "{max}", String.valueOf(max)));
            return false;
        }

        // Create the portal
        Portal portal = new Portal(name, location, player.getUniqueId());
        portals.put(lowerName, portal);

        player.sendMessage(plugin.getConfigManager().getMessage("portal-created", "{name}", name));

        // Auto-save if configured
        if (plugin.getConfigManager().getAutoSaveInterval() > 0) {
            savePortals();
        }

        return true;
    }

    public boolean removePortal(Player player, String name) {
        String lowerName = name.toLowerCase();
        Portal portal = portals.get(lowerName);

        if (portal == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", name));
            return false;
        }

        // Check if player owns the portal or has admin permission
        if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return false;
        }

        portals.remove(lowerName);
        player.sendMessage(plugin.getConfigManager().getMessage("portal-removed", "{name}", name));

        // Auto-save if configured
        if (plugin.getConfigManager().getAutoSaveInterval() > 0) {
            savePortals();
        }

        return true;
    }

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public Portal getPortalAtLocation(Location location) {
        for (Portal portal : portals.values()) {
            if (isNearPortal(location, portal.getLocation(), 5.0)) { // Increased range from 2 to 5
                return portal;
            }
        }
        return null;
    }

    private boolean isNearPortal(Location loc1, Location loc2, double radius) {
        if (loc1.getWorld() != loc2.getWorld()) {
            return false;
        }
        return loc1.distance(loc2) <= radius;
    }

    public List<Portal> getPlayerPortals(UUID playerId) {
        return portals.values().stream()
                .filter(portal -> portal.getOwner().equals(playerId))
                .collect(Collectors.toList());
    }

    public boolean canCreatePortal(UUID playerId) {
        int maxPortals = plugin.getConfigManager().getMaxPortalsPerPlayer();
        if (maxPortals == 0) {
            return true; // Unlimited
        }

        long playerPortalCount = portals.values().stream()
                .filter(portal -> portal.getOwner().equals(playerId))
                .count();

        return playerPortalCount < maxPortals;
    }

    public Collection<Portal> getAllPortals() {
        return portals.values();
    }

    public int getPortalCount() {
        return portals.size();
    }

    public void startAutoSave() {
        int interval = plugin.getConfigManager().getAutoSaveInterval();
        if (interval > 0) {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::savePortals,
                    interval * 60 * 20L, interval * 60 * 20L); // Convert minutes to ticks
        }
    }

    public void removeAllPortals() {
        // Clear all portals
        portals.clear();
        savePortals();

        // The particle task will automatically stop showing particles for removed portals
        // since it checks if portals exist in the collection
    }
}