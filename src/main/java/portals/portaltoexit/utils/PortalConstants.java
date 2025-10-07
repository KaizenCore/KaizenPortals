package portals.portaltoexit.utils;

/**
 * Constants used throughout the Portal2Exit plugin
 * Centralizes all magic numbers and configuration values
 */
public final class PortalConstants {

    // Prevent instantiation
    private PortalConstants() {}

    // Portal Detection
    public static final double PORTAL_DETECTION_RADIUS = 5.0;
    public static final double PORTAL_WAND_RANGE = 50.0;
    public static final double EXIT_POINT_DETECTION_RADIUS = 2.0;

    // Search and Safety
    public static final int MAX_SAFE_LOCATION_SEARCH_RADIUS = 10;
    public static final int DEFAULT_SAFE_LOCATION_RADIUS = 5;
    public static final int MAX_SEARCH_ATTEMPTS = 100;

    // Particle Effects
    public static final double PORTAL_PARTICLE_RADIUS = 2.0;
    public static final double PORTAL_PARTICLE_HEIGHT = 3.0;
    public static final int PORTAL_PARTICLE_DENSITY = 10;
    public static final int PORTAL_PARTICLE_COUNT = 5;
    public static final double PORTAL_PARTICLE_SPEED = 0.0;

    public static final double EXIT_PARTICLE_RADIUS = 1.5;
    public static final double EXIT_PARTICLE_HEIGHT = 2.0;
    public static final int EXIT_PARTICLE_DENSITY = 8;
    public static final int EXIT_PARTICLE_COUNT = 3;

    // Task Intervals (in ticks)
    public static final long PORTAL_PARTICLE_TASK_INTERVAL = 10L;  // Every 0.5 seconds
    public static final long EXIT_PARTICLE_TASK_INTERVAL = 20L;    // Every 1 second
    public static final long AUTO_SAVE_TICKS_PER_MINUTE = 1200L;  // 20 ticks/second * 60 seconds

    // GUI Settings
    public static final int GUI_ROWS_PORTAL_LIST = 6;
    public static final int GUI_ROWS_PORTAL_MANAGEMENT = 4;
    public static final int GUI_ROWS_EXIT_POINTS = 5;
    public static final int GUI_ROWS_KIT_SELECTION = 3;

    public static final int PORTALS_PER_PAGE = 45;
    public static final int EXITS_PER_PAGE = 36;
    public static final int KITS_PER_PAGE = 18;

    // GUI Slot Positions
    public static final int SLOT_PREVIOUS_PAGE = 48;
    public static final int SLOT_NEXT_PAGE = 50;
    public static final int SLOT_SEARCH = 51;
    public static final int SLOT_CREATE = 52;
    public static final int SLOT_BACK = 53;

    // Portal Management GUI Slots
    public static final int SLOT_TELEPORT = 10;
    public static final int SLOT_RENAME = 12;
    public static final int SLOT_DELETE = 14;
    public static final int SLOT_EXIT_POINTS = 16;
    public static final int SLOT_EXIT_TYPE = 19;
    public static final int SLOT_SELECTION_MODE = 21;
    public static final int SLOT_PARTICLES = 23;
    public static final int SLOT_SOUNDS = 25;
    public static final int SLOT_PORTAL_INFO = 31;

    // Exit Points GUI Slots
    public static final int SLOT_ADD_EXIT = 36;
    public static final int SLOT_CLEAR_EXITS = 38;
    public static final int SLOT_EXIT_BACK = 44;

    // Limits
    public static final int MAX_PORTAL_NAME_LENGTH = 32;
    public static final int MIN_PORTAL_NAME_LENGTH = 3;
    public static final int MAX_PORTAL_DESCRIPTION_LENGTH = 128;
    public static final int MAX_KIT_NAME_LENGTH = 32;

    // Default Values
    public static final int DEFAULT_PORTAL_LIMIT = 10;
    public static final int DEFAULT_COOLDOWN_SECONDS = 3;
    public static final double DEFAULT_PORTAL_COST = 100.0;
    public static final double DEFAULT_USAGE_COST = 10.0;

    // Distance Constants
    public static final double MAX_TELEPORT_DISTANCE = 30000.0;  // 30,000 blocks
    public static final double VIEW_DISTANCE_SQUARED = 256.0;    // 16 blocks squared

    // Permissions
    public static final String PERM_USE = "portal2exit.use";
    public static final String PERM_CREATE = "portal2exit.create";
    public static final String PERM_REMOVE = "portal2exit.remove";
    public static final String PERM_WAND = "portal2exit.wand";
    public static final String PERM_GUI = "portal2exit.gui";
    public static final String PERM_ADMIN = "portal2exit.admin";
    public static final String PERM_BYPASS_LIMIT = "portal2exit.unlimited";
    public static final String PERM_BYPASS_COST = "portal2exit.free";
    public static final String PERM_BYPASS_COOLDOWN = "portal2exit.bypass.cooldown";
    public static final String PERM_BYPASS_REQUIREMENTS = "portal2exit.bypass.requirements";
    public static final String PERM_REMOVE_OTHERS = "portal2exit.remove.others";
}