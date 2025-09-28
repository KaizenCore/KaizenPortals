package portals.portaltoexit.listeners;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import portals.portaltoexit.Portaltoexit;

public class PortalCreationListener implements Listener {
    private final Portaltoexit plugin;

    public PortalCreationListener(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check for right-click on block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (block == null || item == null) {
            return;
        }

        // Check if player has permission
        if (!player.hasPermission("portal2exit.create")) {
            return;
        }

        // Check if the required item is being used
        String requiredItem = plugin.getConfigManager().getRequiredItem();
        Material requiredMaterial = Material.matchMaterial(requiredItem);

        if (requiredMaterial == null || item.getType() != requiredMaterial) {
            return;
        }

        // Check if the block is the required base block
        String requiredBlock = plugin.getConfigManager().getRequiredBaseBlock();
        Material requiredBlockMaterial = Material.matchMaterial(requiredBlock);

        if (requiredBlockMaterial == null || block.getType() != requiredBlockMaterial) {
            return;
        }

        // Cancel the default interaction
        event.setCancelled(true);

        // Check if player can create more portals
        if (!plugin.getPortalManager().canCreatePortal(player.getUniqueId())) {
            int max = plugin.getConfigManager().getMaxPortalsPerPlayer();
            player.sendMessage(plugin.getConfigManager().getMessage("max-portals-reached", "{max}", String.valueOf(max)));
            return;
        }

        // Generate portal name
        String portalName = generatePortalName(player);

        // Create the portal at the block location (add 1 to Y for standing on top)
        if (plugin.getPortalManager().createPortal(player, portalName, block.getLocation().add(0.5, 1, 0.5))) {
            // Consume item if configured
            if (plugin.getConfigManager().shouldConsumeItem()) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
            }

            // Play creation effects
            playCreationEffects(block.getLocation().add(0.5, 1, 0.5));

            // Send additional instructions
            player.sendMessage("§7Tip: Use §e/portal setexit " + portalName + " <type>§7 to set the exit location!");
            player.sendMessage("§7Types: §aspawn§7, §abed§7, or §acustom§7 (current location)");
        }
    }

    private String generatePortalName(Player player) {
        // Generate a unique name for the portal
        int count = plugin.getPortalManager().getPlayerPortals(player.getUniqueId()).size();
        String baseName = player.getName() + "_portal_";
        String portalName;
        int suffix = count + 1;

        do {
            portalName = baseName + suffix;
            suffix++;
        } while (plugin.getPortalManager().getPortal(portalName) != null);

        return portalName;
    }

    private void playCreationEffects(org.bukkit.Location location) {
        if (plugin.getConfigManager().areParticlesEnabled()) {
            // Spawn creation particles using version compatibility
            portals.portaltoexit.utils.VersionCompatibility.spawnParticle(
                location, "PORTAL", 100, 0.5, 1, 0.5, 0.5);
            portals.portaltoexit.utils.VersionCompatibility.spawnParticle(
                location, portals.portaltoexit.utils.VersionCompatibility.getMagicParticle(), 50, 0.5, 1, 0.5, 0.1);
            portals.portaltoexit.utils.VersionCompatibility.spawnParticle(
                location, "END_ROD", 30, 0.3, 0.5, 0.3, 0.05);
        }

        if (plugin.getConfigManager().areSoundsEnabled()) {
            // Play creation sound using version compatibility
            portals.portaltoexit.utils.VersionCompatibility.playSound(
                location, "BLOCK_END_PORTAL_FRAME_FILL", 1.0f, 1.0f);
            // Also try alternate sound for effect layering
            portals.portaltoexit.utils.VersionCompatibility.playSound(
                location, "ENTITY_ENDERMAN_TELEPORT", 0.5f, 1.5f);
        }
    }
}