package portals.portaltoexit.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;


/**
 * Utility class for handling cross-version compatibility
 * Supports Minecraft 1.12.2 through 1.21+
 */
public class VersionCompatibility {

    private static final String SERVER_VERSION;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        SERVER_VERSION = version.substring(version.lastIndexOf('.') + 1);

        // Parse version numbers (e.g., v1_12_R1 -> 1.12)
        String[] parts = SERVER_VERSION.split("_");
        int tempMajor = 1;
        int tempMinor = 12;

        if (parts.length >= 2) {
            try {
                tempMajor = Integer.parseInt(parts[0].substring(1));
                tempMinor = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Keep defaults
            }
        }

        MAJOR_VERSION = tempMajor;
        MINOR_VERSION = tempMinor;
    }

    /**
     * Check if server is running 1.13 or newer (The Flattening)
     */
    public static boolean isPostFlattening() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 13);
    }

    /**
     * Get compatible material
     */
    public static Material getMaterial(String name) {
        // Clean up the name
        String cleanName = name.toUpperCase()
            .replace("MINECRAFT:", "")
            .replace(" ", "_");

        // Direct material lookup
        try {
            return Material.valueOf(cleanName);
        } catch (IllegalArgumentException e) {
            // Try fallback names for common materials
        }

        // Fallback attempts for common materials
        if (cleanName.equals("ENDER_EYE")) {
            try {
                return Material.valueOf("ENDER_EYE");
            } catch (IllegalArgumentException e) {
                try {
                    return Material.valueOf("EYE_OF_ENDER");
                } catch (IllegalArgumentException e2) {
                    // Not available
                }
            }
        }

        if (cleanName.equals("OBSIDIAN")) {
            return Material.OBSIDIAN;
        }

        // Handle grass compatibility across versions
        if (cleanName.equals("GRASS") || cleanName.equals("SHORT_GRASS")) {
            if (isPostFlattening()) {
                // 1.20.3+ uses SHORT_GRASS, 1.13-1.20.2 uses GRASS
                try {
                    return Material.valueOf("SHORT_GRASS");
                } catch (IllegalArgumentException e) {
                    try {
                        return Material.valueOf("GRASS");
                    } catch (IllegalArgumentException e2) {
                        // Not available
                    }
                }
            } else {
                // Pre-1.13 uses LONG_GRASS with data value 1
                try {
                    return Material.valueOf("LONG_GRASS");
                } catch (IllegalArgumentException e) {
                    // Not available
                }
            }
        }

        return null;
    }

    /**
     * Play particle effect compatible with all versions
     */
    public static void spawnParticle(Location location, String particleType, int count,
                                    double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null || location.getWorld() == null) return;

        try {
            // Use Bukkit's native particle system
            Particle particle = Particle.valueOf(particleType.toUpperCase());
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (IllegalArgumentException e) {
            // Particle type not available in this version
            // Try alternative particle names for older versions
            String alternativeName = particleType;
            if (particleType.equals("WITCH") && !isPostFlattening()) {
                alternativeName = "SPELL_WITCH";
            } else if (particleType.equals("SPELL_WITCH") && isPostFlattening()) {
                alternativeName = "WITCH";
            }

            if (!alternativeName.equals(particleType)) {
                try {
                    Particle particle = Particle.valueOf(alternativeName.toUpperCase());
                    location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
                } catch (IllegalArgumentException ignored) {
                    // Alternative also not available
                }
            }
        } catch (Exception e) {
            // Other error, ignore
        }
    }

    /**
     * Play sound compatible with all versions
     */
    public static void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null) return;

        try {
            // Direct sound playing using Bukkit API
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (Exception e) {
            // Sound not available in this version
            // Try some common fallbacks
            if (soundName.contains("PORTAL") || soundName.contains("ENDERMAN")) {
                try {
                    Sound fallback = Sound.ENTITY_ENDERMAN_TELEPORT;
                    location.getWorld().playSound(location, fallback, volume, pitch);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Get safe particle name for portal effects
     */
    public static String getPortalParticle() {
        if (isPostFlattening()) {
            return "PORTAL";
        } else {
            return "PORTAL";  // Same in most versions
        }
    }

    /**
     * Get safe particle name for witch/magic effects
     */
    public static String getMagicParticle() {
        if (isPostFlattening()) {
            return "WITCH";
        } else {
            return "SPELL_WITCH";  // Pre-1.13 name
        }
    }

    /**
     * Get server version string
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Get major version number
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * Get minor version number
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * Check if running on Paper
     */
    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    /**
     * Check if running on Spigot
     */
    public static boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}