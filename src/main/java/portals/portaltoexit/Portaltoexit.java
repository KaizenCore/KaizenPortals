package portals.portaltoexit;

import org.bukkit.plugin.java.JavaPlugin;
import portals.portaltoexit.commands.PortalCommand;
import portals.portaltoexit.listeners.PortalCreationListener;
import portals.portaltoexit.listeners.PortalInteractionListener;
import portals.portaltoexit.gui.GUIListener;
import portals.portaltoexit.managers.*;
import portals.portaltoexit.tasks.PortalParticleTask;
import portals.portaltoexit.tasks.ExitPointParticleTask;
import portals.portaltoexit.utils.VersionCompatibility;

public class Portaltoexit extends JavaPlugin {

    private static Portaltoexit instance;
    private ConfigManager configManager;
    private PortalManager portalManager;
    private CooldownManager cooldownManager;
    private TeleportationManager teleportationManager;
    private EconomyManager economyManager;
    private KitManager kitManager;
    private PermissionManager permissionManager;
    private ActivationManager activationManager;
    private PortalParticleTask particleTask;
    private ExitPointParticleTask exitParticleTask;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("[KaizenPortals] Loading...");
        getLogger().info("[KaizenPortals] Server version: " + VersionCompatibility.getServerVersion());
        getLogger().info("[KaizenPortals] Running on: " +
            (VersionCompatibility.isPaper() ? "Paper" :
             VersionCompatibility.isSpigot() ? "Spigot" : "Bukkit"));

        try {
            // Register serializable classes
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(portals.portaltoexit.data.Portal.class);
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(portals.portaltoexit.data.Kit.class);
            org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass(portals.portaltoexit.data.Portal.RequiredItem.class);

            // Load configuration
            saveDefaultConfig();
            configManager = new ConfigManager(this);

            // Initialize managers
            cooldownManager = new CooldownManager(configManager);
            portalManager = new PortalManager(this);
            teleportationManager = new TeleportationManager(this);

            // Initialize economy (soft dependency)
            economyManager = new EconomyManager(this);

            // Initialize kit manager
            kitManager = new KitManager(this);

            // Initialize permission manager
            permissionManager = new PermissionManager(this);

            // Initialize activation manager
            activationManager = new ActivationManager(this);

            // Load portal data
            portalManager.loadPortals();

            // Register events
            registerEvents();

            // Register commands
            registerCommands();

            // Start particle task if enabled
            if (configManager.arePortalParticlesEnabled()) {
                particleTask = new PortalParticleTask(this);
                particleTask.start();
                getLogger().info("[KaizenPortals] Particle effects enabled");
            }

            // Start exit point particle task
            if (configManager.areParticlesEnabled()) {
                exitParticleTask = new ExitPointParticleTask(this);
                exitParticleTask.runTaskTimer(this, 40L, 20L); // Every second
                getLogger().info("[KaizenPortals] Exit point particles enabled");
            }

            // Start auto-save task
            portalManager.startAutoSave();

            getLogger().info("[KaizenPortals] Enabled successfully!");
            getLogger().info("[KaizenPortals] Loaded " + portalManager.getPortalCount() + " portals");
            if (economyManager.isEnabled()) {
                getLogger().info("[KaizenPortals] Economy integration active");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to enable KaizenPortals: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[KaizenPortals] Saving data...");

        // Cancel particle tasks
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (exitParticleTask != null) {
            exitParticleTask.cancel();
        }

        // Save portal data
        if (portalManager != null) {
            portalManager.savePortals();
        }

        // Save kit data
        if (kitManager != null) {
            kitManager.saveKits();
        }

        getLogger().info("[KaizenPortals] Disabled!");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PortalCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }

    private void registerCommands() {
        getCommand("portal").setExecutor(new PortalCommand(this));
        getCommand("portal").setTabCompleter(new PortalCommand(this));
    }

    public static Portaltoexit getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public TeleportationManager getTeleportationManager() {
        return teleportationManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ActivationManager getActivationManager() {
        return activationManager;
    }
}
