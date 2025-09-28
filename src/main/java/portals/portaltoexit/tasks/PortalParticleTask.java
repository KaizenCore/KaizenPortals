package portals.portaltoexit.tasks;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;
import portals.portaltoexit.utils.VersionCompatibility;

/**
 * Task to display ambient particles at portal locations
 */
public class PortalParticleTask extends BukkitRunnable {
    private final Portaltoexit plugin;
    private int tickCounter = 0;

    public PortalParticleTask(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCounter++;

        // Only show particles every 5 ticks (4 times per second) to reduce lag
        if (tickCounter % 5 != 0) {
            return;
        }

        for (Portal portal : plugin.getPortalManager().getAllPortals()) {
            if (!portal.isShowParticles()) {
                continue;
            }

            Location loc = portal.getLocation();

            // Check if any players are nearby (within 32 blocks)
            if (loc.getWorld() == null || loc.getWorld().getPlayers().stream()
                    .noneMatch(p -> p.getLocation().distanceSquared(loc) <= 1024)) {
                continue;
            }

            // Display portal particles
            displayPortalParticles(loc);
        }
    }

    private void displayPortalParticles(Location location) {
        // Create a spiral effect
        double radius = plugin.getConfigManager().getPortalParticleRadius();
        int density = plugin.getConfigManager().getPortalParticleDensity();
        double height = plugin.getConfigManager().getPortalParticleHeight();

        for (int i = 0; i < density; i++) {
            double angle = (2 * Math.PI * i) / density + (tickCounter * 0.1);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            double y = (height * i / density) + (Math.sin(tickCounter * 0.05) * 0.3);

            Location particleLoc = location.clone().add(x, y, z);

            // Use version-compatible particle spawning
            VersionCompatibility.spawnParticle(
                particleLoc,
                VersionCompatibility.getPortalParticle(),
                2,  // count
                0.1, 0.1, 0.1,  // offset
                0.01  // speed
            );
        }

        // Add some magical particles occasionally
        if (tickCounter % 20 == 0) {  // Every second
            VersionCompatibility.spawnParticle(
                location.clone().add(0, 1, 0),
                VersionCompatibility.getMagicParticle(),
                10,
                0.5, 0.5, 0.5,
                0.05
            );

            // Add end rod particles for extra effect
            VersionCompatibility.spawnParticle(
                location.clone().add(0, 2, 0),
                "END_ROD",
                5,
                0.3, 0.3, 0.3,
                0.02
            );
        }
    }

    /**
     * Start the particle task
     */
    public void start() {
        // Run every tick (20 times per second)
        this.runTaskTimer(plugin, 20L, 1L);
    }
}