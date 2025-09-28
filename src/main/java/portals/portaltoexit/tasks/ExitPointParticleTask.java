package portals.portaltoexit.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;
import portals.portaltoexit.utils.VersionCompatibility;

/**
 * Task to display visual particles at portal exit points
 */
public class ExitPointParticleTask extends BukkitRunnable {
    private final Portaltoexit plugin;

    public ExitPointParticleTask(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Only show particles if enabled
        if (!plugin.getConfigManager().areParticlesEnabled()) {
            return;
        }

        for (Portal portal : plugin.getPortalManager().getAllPortals()) {
            // Only show exit particles for custom exit portals
            if (portal.getExitType() != Portal.ExitType.CUSTOM && portal.getExitType() != Portal.ExitType.RANDOM) {
                continue;
            }

            // Show particles at all exit points
            for (Location exitLocation : portal.getExitPoints()) {
                if (exitLocation == null || exitLocation.getWorld() == null) {
                    continue;
                }

                // Check if any player is nearby to see the particles
                boolean hasNearbyPlayer = false;
                for (Player player : exitLocation.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(exitLocation) < 1024) { // 32 blocks
                        hasNearbyPlayer = true;
                        break;
                    }
                }

                if (hasNearbyPlayer) {
                    showExitParticles(exitLocation);
                }
            }
        }
    }

    private void showExitParticles(Location location) {
        // Create a beacon-like effect at exit points
        Location particleLocation = location.clone().add(0, 0.5, 0);

        // Green particles to distinguish from purple portal entrance
        VersionCompatibility.spawnParticle(particleLocation, "VILLAGER_HAPPY", 3, 0.3, 0.5, 0.3, 0.01);

        // Ring effect at ground level
        double radius = 1.0;
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location ringLoc = location.clone().add(x, 0.1, z);
            VersionCompatibility.spawnParticle(ringLoc, "END_ROD", 1, 0, 0, 0, 0);
        }

        // Upward beam effect every few ticks
        if (System.currentTimeMillis() % 3000 < 100) { // Show beam briefly every 3 seconds
            for (double y = 0; y < 3; y += 0.5) {
                Location beamLoc = location.clone().add(0, y, 0);
                VersionCompatibility.spawnParticle(beamLoc, "VILLAGER_HAPPY", 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}