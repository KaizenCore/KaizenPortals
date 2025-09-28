package portals.portaltoexit.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import portals.portaltoexit.Portaltoexit;
import portals.portaltoexit.data.Portal;
import portals.portaltoexit.gui.PortalWand;
import portals.portaltoexit.gui.PortalListGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PortalCommand implements CommandExecutor, TabCompleter {
    private final Portaltoexit plugin;

    public PortalCommand(Portaltoexit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("portal2exit.use")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // Open GUI directly when no arguments provided
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("portal2exit.gui")) {
                    PortalListGUI.openPortalListGUI(player, 0, null);
                } else {
                    sendHelp(sender);
                }
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "remove":
            case "delete":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "tp":
            case "teleport":
                return handleTeleport(sender, args);
            case "setexit":
                return handleSetExit(sender, args);
            case "addexit":
                return handleAddExit(sender, args);
            case "removeexit":
            case "delexit":
                return handleRemoveExit(sender, args);
            case "listexits":
                return handleListExits(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "removeall":
                return handleRemoveAll(sender, args);
            case "wand":
                return handleWand(sender, args);
            case "gui":
                return handleGUI(sender, args);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (!sender.hasPermission("portal2exit.create")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid-args", "{usage}", "/portal create <name>"));
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];
        Location location = player.getLocation();

        plugin.getPortalManager().createPortal(player, portalName, location);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid-args", "{usage}", "/portal remove <name>"));
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];

        plugin.getPortalManager().removePortal(player, portalName);
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        List<Portal> portalsToList;

        // Check if admin wants to list all portals
        if (args.length > 1 && args[1].equalsIgnoreCase("all") && sender.hasPermission("portal2exit.admin")) {
            portalsToList = new ArrayList<>(plugin.getPortalManager().getAllPortals());
            sender.sendMessage("§6--- All Portals ---");
        } else {
            // Regular player listing their own portals
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
                return true;
            }
            Player player = (Player) sender;
            portalsToList = plugin.getPortalManager().getPlayerPortals(player.getUniqueId());
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.portal-list-header"));
        }

        if (portalsToList.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.portal-list-empty"));
        } else {
            for (Portal portal : portalsToList) {
                Location loc = portal.getLocation();
                String locString = String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
                String ownerName = plugin.getServer().getOfflinePlayer(portal.getOwner()).getName();

                if (sender.hasPermission("portal2exit.admin") && args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    // Show owner for admin listing
                    sender.sendMessage("§7- §b" + portal.getName() + " §7(Owner: §e" + ownerName + "§7) at §e" + locString);
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("commands.portal-list-item",
                            "{name}", portal.getName(),
                            "{location}", locString));
                }
            }
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid-args", "{usage}", "/portal info <name>"));
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];
        Portal portal = plugin.getPortalManager().getPortal(portalName);

        if (portal == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
            return true;
        }

        // Display portal information
        player.sendMessage("§6--- Portal Info ---");
        player.sendMessage("§eName: §f" + portal.getName());
        Location loc = portal.getLocation();
        player.sendMessage("§eLocation: §f" + String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ()));
        player.sendMessage("§eWorld: §f" + loc.getWorld().getName());
        player.sendMessage("§eExit Type: §f" + portal.getExitType().toString());
        player.sendMessage("§eOwner: §f" + plugin.getServer().getOfflinePlayer(portal.getOwner()).getName());

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid-args", "{usage}", "/portal tp <name>"));
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];
        Portal portal = plugin.getPortalManager().getPortal(portalName);

        if (portal == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
            return true;
        }

        // Check ownership or admin permission
        if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getTeleportationManager().teleportPlayer(player, portal);
        return true;
    }

    private boolean handleSetExit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /portal setexit <name> <spawn|bed|custom>");
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];
        String exitType = args[2].toUpperCase();

        Portal portal = plugin.getPortalManager().getPortal(portalName);
        if (portal == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
            return true;
        }

        // Check ownership
        if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        try {
            Portal.ExitType type = Portal.ExitType.valueOf(exitType);
            portal.setExitType(type);

            if (type == Portal.ExitType.CUSTOM) {
                // Only add current location if no exit points exist
                if (portal.getExitPoints().isEmpty()) {
                    portal.addExitPoint(player.getLocation());
                    player.sendMessage("§aPortal exit type set to CUSTOM and added your current location!");
                } else {
                    player.sendMessage("§aPortal exit type set to CUSTOM (keeping existing exit points)");
                }
                player.sendMessage("§7Use §e/portal addexit " + portal.getName() + "§7 to add more exit points");
            } else {
                player.sendMessage("§aPortal exit type set to: " + type.toString());
            }

            plugin.getPortalManager().savePortals();
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid exit type! Use: spawn, bed, or custom");
        }

        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal2exit.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getConfigManager().reload();
        plugin.getPortalManager().loadPortals();
        sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
        return true;
    }

    private boolean handleRemoveAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal2exit.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Safety confirmation
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§cThis will remove ALL portals on the server!");
            sender.sendMessage("§cTo confirm, use: §e/portal removeall confirm");
            return true;
        }

        int count = plugin.getPortalManager().getAllPortals().size();
        plugin.getPortalManager().removeAllPortals();
        sender.sendMessage("§aRemoved §e" + count + "§a portals from the server.");
        return true;
    }

    private boolean handleAddExit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /portal addexit <name>");
            return true;
        }

        Player player = (Player) sender;
        String portalName = args[1];
        Portal portal = plugin.getPortalManager().getPortal(portalName);

        if (portal == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
            return true;
        }

        // Check ownership
        if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Add current location as exit point
        portal.addExitPoint(player.getLocation());
        plugin.getPortalManager().savePortals();

        player.sendMessage("§aAdded exit point at your current location!");
        player.sendMessage("§7Portal now has §e" + portal.getExitPoints().size() + "§7 exit point(s).");
        return true;
    }

    private boolean handleRemoveExit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            // Remove nearest exit point
            sender.sendMessage("§7Looking for nearby exit points...");

            for (Portal portal : plugin.getPortalManager().getAllPortals()) {
                Location nearest = portal.getNearestExitPoint(player.getLocation());
                if (nearest != null && nearest.distance(player.getLocation()) < 5) {
                    // Check ownership
                    if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
                        player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        return true;
                    }

                    portal.removeExitPoint(nearest);
                    plugin.getPortalManager().savePortals();
                    player.sendMessage("§aRemoved nearby exit point from portal §e" + portal.getName());
                    return true;
                }
            }
            player.sendMessage("§cNo exit points found within 5 blocks!");
        } else {
            // Remove by portal name
            String portalName = args[1];
            Portal portal = plugin.getPortalManager().getPortal(portalName);

            if (portal == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
                return true;
            }

            // Check ownership
            if (!portal.getOwner().equals(player.getUniqueId()) && !player.hasPermission("portal2exit.admin")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            // Remove nearest exit point to player
            Location nearest = portal.getNearestExitPoint(player.getLocation());
            if (nearest != null && nearest.distance(player.getLocation()) < 10) {
                portal.removeExitPoint(nearest);
                plugin.getPortalManager().savePortals();
                player.sendMessage("§aRemoved exit point from portal §e" + portal.getName());
            } else {
                player.sendMessage("§cNo exit points found within 10 blocks for this portal!");
            }
        }

        return true;
    }

    private boolean handleListExits(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /portal listexits <name>");
            return true;
        }

        String portalName = args[1];
        Portal portal = plugin.getPortalManager().getPortal(portalName);

        if (portal == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("portal-not-found", "{name}", portalName));
            return true;
        }

        List<Location> exitPoints = portal.getExitPoints();
        sender.sendMessage("§6--- Exit Points for " + portal.getName() + " ---");

        if (exitPoints.isEmpty()) {
            sender.sendMessage("§7No custom exit points set.");
        } else {
            int index = 1;
            for (Location exit : exitPoints) {
                String locString = String.format("§7%d. World: §e%s §7at §e%.0f, %.0f, %.0f",
                    index++,
                    exit.getWorld().getName(),
                    exit.getX(),
                    exit.getY(),
                    exit.getZ());
                sender.sendMessage(locString);
            }
            sender.sendMessage("§7Exit type: §e" + portal.getExitType());
        }

        return true;
    }

    private boolean handleWand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (!sender.hasPermission("portal2exit.wand")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        PortalWand.giveWand(player);
        return true;
    }

    private boolean handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.player-only"));
            return true;
        }

        if (!sender.hasPermission("portal2exit.gui")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Player player = (Player) sender;
        PortalListGUI.openPortalListGUI(player, 0, null);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6--- Portal2Exit Commands ---");
        sender.sendMessage("§e/portal §7- Open the portal management GUI");
        sender.sendMessage("§e/portal create <name> §7- Create a portal at your location");
        sender.sendMessage("§e/portal remove <name> §7- Remove a portal");
        sender.sendMessage("§e/portal list §7- List your portals");
        sender.sendMessage("§e/portal info <name> §7- Get portal information");
        sender.sendMessage("§e/portal tp <name> §7- Teleport using a portal");
        sender.sendMessage("§e/portal setexit <name> <type> §7- Set portal exit type");
        sender.sendMessage("§7  Exit types: §aspawn§7, §abed§7, or §acustom §7(multiple locations)");
        sender.sendMessage("§e/portal addexit <name> §7- Add current location as additional exit point");
        sender.sendMessage("§e/portal removeexit [name] §7- Remove nearby exit point");
        sender.sendMessage("§e/portal listexits <name> §7- List all exit points for a portal");
        sender.sendMessage("§e/portal wand §7- Get a Portal Wand for GUI management");
        sender.sendMessage("§e/portal gui §7- Open the Portal List GUI");

        if (sender.hasPermission("portal2exit.admin")) {
            sender.sendMessage("§6--- Admin Commands ---");
            sender.sendMessage("§e/portal list all §7- List ALL portals on server");
            sender.sendMessage("§e/portal removeall confirm §7- Remove ALL portals");
            sender.sendMessage("§e/portal reload §7- Reload configuration");
            sender.sendMessage("§7Note: Admins can remove any portal with §e/portal remove <name>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "remove", "list", "info", "tp", "setexit", "addexit", "removeexit", "listexits", "wand", "gui", "help");
            if (sender.hasPermission("portal2exit.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reload");
                subCommands.add("removeall");
            }
            return filterStartsWith(subCommands, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("remove") || subCommand.equals("delete") ||
                subCommand.equals("info") || subCommand.equals("tp") ||
                subCommand.equals("teleport") || subCommand.equals("setexit") ||
                subCommand.equals("addexit") || subCommand.equals("removeexit") ||
                subCommand.equals("listexits")) {

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<String> portalNames = plugin.getPortalManager().getPlayerPortals(player.getUniqueId())
                            .stream()
                            .map(Portal::getName)
                            .collect(Collectors.toList());
                    return filterStartsWith(portalNames, args[1]);
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setexit")) {
            return filterStartsWith(Arrays.asList("spawn", "bed", "custom"), args[2]);
        }

        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}