package portals.portaltoexit.managers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import portals.portaltoexit.Portaltoexit;

import java.util.UUID;

/**
 * Manages economy integration through Vault API
 */
public class EconomyManager {
    private final Portaltoexit plugin;
    private Economy economy = null;
    private boolean vaultEnabled = false;

    public EconomyManager(Portaltoexit plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Setup Vault economy integration
     */
    private boolean setupEconomy() {
        // Check if Vault is installed
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found - Economy features disabled");
            return false;
        }

        // Get economy service
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy plugin found - Economy features disabled");
            return false;
        }

        economy = rsp.getProvider();
        vaultEnabled = (economy != null);

        if (vaultEnabled) {
            plugin.getLogger().info("Successfully hooked into economy: " + economy.getName());
        }

        return vaultEnabled;
    }

    /**
     * Check if economy is enabled
     */
    public boolean isEnabled() {
        return vaultEnabled && economy != null;
    }

    /**
     * Check if player has enough money
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) return true;  // No economy = free
        return economy.has(player, amount);
    }

    /**
     * Get player's balance
     */
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Withdraw money from player
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEnabled()) return true;  // No economy = success
        if (amount <= 0) return true;  // Free portal

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Give money to player
     */
    public boolean depositPlayer(Player player, double amount) {
        if (!isEnabled()) return true;
        if (amount <= 0) return true;

        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Give money to offline player by UUID
     */
    public boolean depositPlayer(UUID playerId, double amount) {
        if (!isEnabled()) return true;
        if (amount <= 0) return true;

        // Get offline player
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }

    /**
     * Process portal usage payment
     */
    public boolean processPortalPayment(Player user, UUID portalOwner, double cost, double ownerPercentage) {
        if (!isEnabled()) return true;
        if (cost <= 0) return true;

        // Check balance
        if (!hasBalance(user, cost)) {
            return false;
        }

        // Withdraw from user
        if (!withdrawPlayer(user, cost)) {
            return false;
        }

        // Calculate owner's cut
        if (ownerPercentage > 0 && !user.getUniqueId().equals(portalOwner)) {
            double ownerCut = cost * ownerPercentage;
            depositPlayer(portalOwner, ownerCut);

            // Log transaction
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info(String.format("Portal payment: %s paid %.2f, owner received %.2f",
                    user.getName(), cost, ownerCut));
            }
        }

        return true;
    }

    /**
     * Format money amount
     */
    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    /**
     * Get currency name (plural)
     */
    public String getCurrencyNamePlural() {
        if (!isEnabled()) return "coins";
        return economy.currencyNamePlural();
    }

    /**
     * Get currency name (singular)
     */
    public String getCurrencyNameSingular() {
        if (!isEnabled()) return "coin";
        return economy.currencyNameSingular();
    }

    /**
     * Calculate the cost to create a portal based on player's existing portal count
     * @param portalCount The player's current portal count
     * @return The cost to create a new portal
     */
    public double calculatePortalCreationCost(int portalCount) {
        if (!isEnabled()) return 0;

        double baseCost = plugin.getConfig().getDouble("economy.creation-cost", 1000.0);
        double scalingMultiplier = plugin.getConfig().getDouble("economy.scaling-multiplier", 0.5);

        if (scalingMultiplier <= 0) {
            return baseCost;
        }

        // Formula: creation-cost * (1 + (portal-count * multiplier))
        return baseCost * (1 + (portalCount * scalingMultiplier));
    }

    /**
     * Process portal creation payment
     * @param player The player creating the portal
     * @param portalCount The player's current portal count
     * @return The amount paid (0 if economy disabled or insufficient funds)
     */
    public double processPortalCreationPayment(Player player, int portalCount) {
        if (!isEnabled()) return 0;

        double cost = calculatePortalCreationCost(portalCount);
        if (cost <= 0) return 0;

        // Check if player has enough money
        if (!hasBalance(player, cost)) {
            return -1; // Indicate insufficient funds
        }

        // Withdraw the money
        if (withdrawPlayer(player, cost)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info(String.format("Portal creation payment: %s paid %.2f",
                    player.getName(), cost));
            }
            return cost;
        }

        return -1; // Payment failed
    }

    /**
     * Calculate refund amount for removing a portal
     * @param creationCost The original cost paid to create the portal
     * @return The refund amount
     */
    public double calculatePortalRefund(double creationCost) {
        if (!isEnabled() || creationCost <= 0) return 0;

        double refundPercentage = plugin.getConfig().getDouble("economy.removal-refund-percentage", 50) / 100.0;
        return creationCost * refundPercentage;
    }

    /**
     * Process portal removal refund
     * @param playerId The UUID of the portal owner
     * @param creationCost The original creation cost
     * @return The amount refunded
     */
    public double processPortalRemovalRefund(UUID playerId, double creationCost) {
        if (!isEnabled()) return 0;

        double refundAmount = calculatePortalRefund(creationCost);
        if (refundAmount <= 0) return 0;

        if (depositPlayer(playerId, refundAmount)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info(String.format("Portal removal refund: %s received %.2f",
                    playerId.toString(), refundAmount));
            }
            return refundAmount;
        }

        return 0; // Refund failed
    }

    /**
     * Get the default portal usage cost
     * @return Default usage cost
     */
    public double getDefaultUsageCost() {
        return plugin.getConfig().getDouble("economy.default-usage-cost", 10.0);
    }

    /**
     * Check if economy features are enabled in config
     * @return true if economy is enabled in config
     */
    public boolean isEconomyEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", true);
    }

    /**
     * Process portal usage payment with owner revenue sharing
     * @param user The player using the portal
     * @param portalOwner The UUID of the portal owner
     * @param cost The cost to use the portal
     * @return true if payment was successful
     */
    public boolean processPortalUsagePayment(Player user, UUID portalOwner, double cost) {
        if (!isEnabled() || cost <= 0) return true;

        // Check balance
        if (!hasBalance(user, cost)) {
            return false;
        }

        // Withdraw from user
        if (!withdrawPlayer(user, cost)) {
            return false;
        }

        // For now, no revenue sharing with portal owners
        // This could be added as a feature later if needed

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(String.format("Portal usage payment: %s paid %.2f",
                user.getName(), cost));
        }

        return true;
    }

    /**
     * Reload economy (in case economy plugin was loaded after this plugin)
     */
    public void reload() {
        setupEconomy();
    }
}