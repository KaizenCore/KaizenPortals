package portals.portaltoexit.listeners;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalInteractionListener implements Listener {
    private final Portaltoexit plugin;
    private final Map<UUID, Long> lastPortalCheck;

    public PortalInteractionListener(Portaltoexit plugin) {
        this.plugin = plugin;
        this.lastPortalCheck = new HashMap<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player actually moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Throttle checks to once per 250ms per player
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastPortalCheck.get(player.getUniqueId());
        if (lastCheck != null && currentTime - lastCheck < 250) {
            return;
        }
        lastPortalCheck.put(player.getUniqueId(), currentTime);

        // Check if player has permission
        if (!player.hasPermission("portal2exit.use")) {
            return;
        }

        // Check if player is near a portal
        Location playerLoc = player.getLocation();
        Portal nearbyPortal = plugin.getPortalManager().getPortalAtLocation(playerLoc);

        if (nearbyPortal != null) {
            // Play ambient portal effects
            if (plugin.getConfigManager().areParticlesEnabled()) {
                playAmbientEffects(nearbyPortal.getLocation());
            }

            // Check if player is close enough to use the portal (within 1 block)
            if (playerLoc.distance(nearbyPortal.getLocation()) <= 1.0) {
                // Teleport the player
                plugin.getTeleportationManager().teleportPlayer(player, nearbyPortal);
            }
        }
    }

    private void playAmbientEffects(Location location) {
        // Spawn subtle ambient particles around active portals
        location.getWorld().spawnParticle(Particle.PORTAL, location, 5, 0.3, 0.5, 0.3, 0.01);
    }
}