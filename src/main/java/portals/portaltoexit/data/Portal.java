package portals.portaltoexit.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class Portal implements ConfigurationSerializable {
    private final String name;
    private final Location location;
    private final UUID owner;
    private ExitType exitType;
    private ExitSelectionMode selectionMode;  // How to select from multiple exit points
    private Location customExit;  // Single exit for backward compatibility
    private List<Location> exitPoints;  // Multiple exit points for CUSTOM type
    private final long createdTime;
    private String kitName;  // Optional kit to give on portal use
    private double cost;  // Cost to use this portal
    private boolean showParticles;  // Whether to show ambient particles

    // New fields for activation requirements
    private List<RequiredItem> requiredItems;  // Items required to activate portal
    private String requiredPermission;  // Permission required to use portal
    private String requiredKit;  // Kit that must have been received to use portal
    private String kitToGive;  // Kit to give when using portal (different from kitName for backward compatibility)
    private double creationCost;  // Cost that was paid to create this portal (for refunds)

    private static final Random random = new Random();

    public Portal(String name, Location location, UUID owner) {
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.exitType = ExitType.SPAWN;
        this.selectionMode = ExitSelectionMode.RANDOM;
        this.customExit = null;
        this.exitPoints = new ArrayList<>();
        this.createdTime = System.currentTimeMillis();
        this.kitName = null;
        this.cost = 0;
        this.showParticles = true;
        this.requiredItems = new ArrayList<>();
        this.requiredPermission = null;
        this.requiredKit = null;
        this.kitToGive = null;
        this.creationCost = 0;
    }

    public Portal(String name, Location location, UUID owner, ExitType exitType, Location customExit, long createdTime) {
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.exitType = exitType;
        this.selectionMode = ExitSelectionMode.RANDOM;
        this.customExit = customExit;
        this.exitPoints = new ArrayList<>();
        if (customExit != null && exitType == ExitType.CUSTOM) {
            this.exitPoints.add(customExit);
        }
        this.createdTime = createdTime;
        this.kitName = null;
        this.cost = 0;
        this.showParticles = true;
        this.requiredItems = new ArrayList<>();
        this.requiredPermission = null;
        this.requiredKit = null;
        this.kitToGive = null;
        this.creationCost = 0;
    }

    public Portal(String name, Location location, UUID owner, ExitType exitType, Location customExit,
                  long createdTime, String kitName, double cost, boolean showParticles) {
        this.name = name;
        this.location = location;
        this.owner = owner;
        this.exitType = exitType;
        this.selectionMode = ExitSelectionMode.RANDOM;
        this.customExit = customExit;
        this.exitPoints = new ArrayList<>();
        if (customExit != null && exitType == ExitType.CUSTOM) {
            this.exitPoints.add(customExit);
        }
        this.createdTime = createdTime;
        this.kitName = kitName;
        this.cost = cost;
        this.showParticles = showParticles;
        this.requiredItems = new ArrayList<>();
        this.requiredPermission = null;
        this.requiredKit = null;
        this.kitToGive = null;
        this.creationCost = 0;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwner() {
        return owner;
    }

    public ExitType getExitType() {
        return exitType;
    }

    public void setExitType(ExitType exitType) {
        this.exitType = exitType;
        if (exitType != ExitType.CUSTOM && exitType != ExitType.RANDOM) {
            // Clear exit points if not using custom exits
            this.exitPoints.clear();
        }
    }

    public ExitSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(ExitSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public Location getCustomExit() {
        // Use selection mode to determine which exit point to return
        if (!exitPoints.isEmpty()) {
            switch (selectionMode) {
                case RANDOM:
                    return getRandomExitPoint();
                case SEQUENTIAL:
                    return getSequentialExitPoint();
                case NEAREST:
                    // Note: For nearest, we need a reference location which we don't have here
                    // This method will be used primarily for backward compatibility
                    return exitPoints.get(0);
                case FIRST:
                default:
                    return exitPoints.get(0);
            }
        }
        return customExit;
    }

    public void setCustomExit(Location customExit) {
        this.customExit = customExit;
        // Clear existing exit points and add the new one
        this.exitPoints.clear();
        if (customExit != null) {
            this.exitPoints.add(customExit);
        }
    }

    // New methods for managing multiple exit points
    public void addExitPoint(Location exit) {
        if (exit != null && !exitPoints.contains(exit)) {
            exitPoints.add(exit);
            // Update customExit for backward compatibility
            if (customExit == null) {
                customExit = exit;
            }
        }
    }

    public void removeExitPoint(Location exit) {
        exitPoints.remove(exit);
        // Update customExit if it was removed
        if (exit != null && exit.equals(customExit)) {
            customExit = exitPoints.isEmpty() ? null : exitPoints.get(0);
        }
    }

    public List<Location> getExitPoints() {
        return new ArrayList<>(exitPoints);
    }

    public void clearExitPoints() {
        exitPoints.clear();
        customExit = null;
    }

    public Location getRandomExitPoint() {
        if (exitPoints.isEmpty()) {
            return customExit;
        }
        return exitPoints.get(random.nextInt(exitPoints.size()));
    }

    public Location getNearestExitPoint(Location from) {
        Location nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Location exit : exitPoints) {
            if (exit.getWorld() == from.getWorld()) {
                double distance = exit.distance(from);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = exit;
                }
            }
        }

        return nearest;
    }

    // Sequential exit point selection (cycles through exits in order)
    private int lastUsedExitIndex = 0;

    public Location getSequentialExitPoint() {
        if (exitPoints.isEmpty()) {
            return customExit;
        }

        Location selectedExit = exitPoints.get(lastUsedExitIndex);
        lastUsedExitIndex = (lastUsedExitIndex + 1) % exitPoints.size();
        return selectedExit;
    }

    public void resetSequentialIndex() {
        lastUsedExitIndex = 0;
    }

    // Get exit point based on current exit type and selection mode
    public Location getExitPointByType(Location fromLocation) {
        switch (exitType) {
            case SPAWN:
                return null; // Will be handled by teleportation manager
            case BED:
                return null; // Will be handled by teleportation manager
            case CUSTOM:
                if (exitPoints.isEmpty()) {
                    return customExit;
                }
                switch (selectionMode) {
                    case RANDOM:
                        return getRandomExitPoint();
                    case SEQUENTIAL:
                        return getSequentialExitPoint();
                    case NEAREST:
                        return getNearestExitPoint(fromLocation);
                    case FIRST:
                    default:
                        return exitPoints.get(0);
                }
            case RANDOM:
                return getRandomExitPoint();
            default:
                return customExit;
        }
    }

    // Check if portal has valid exit points for its type
    public boolean hasValidExitPoints() {
        switch (exitType) {
            case SPAWN:
            case BED:
                return true; // These don't require custom exit points
            case CUSTOM:
            case RANDOM:
                return !exitPoints.isEmpty() || customExit != null;
            default:
                return false;
        }
    }

    // Get a description of the exit configuration
    public String getExitDescription() {
        switch (exitType) {
            case SPAWN:
                return "Teleports to spawn point";
            case BED:
                return "Teleports to player's bed";
            case CUSTOM:
                if (exitPoints.isEmpty()) {
                    return "No exit points set";
                }
                String modeDesc = selectionMode.getDescription();
                return modeDesc + " from " + exitPoints.size() + " exit point" + (exitPoints.size() > 1 ? "s" : "");
            case RANDOM:
                return exitPoints.isEmpty() ? "No exit points set" :
                       "Randomly selects from " + exitPoints.size() + " exit points";
            default:
                return "Unknown exit type";
        }
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public String getKitName() {
        return kitName;
    }

    public void setKitName(String kitName) {
        this.kitName = kitName;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean isShowParticles() {
        return showParticles;
    }

    public void setShowParticles(boolean showParticles) {
        this.showParticles = showParticles;
    }

    // New getter and setter methods for activation requirements
    public List<RequiredItem> getRequiredItems() {
        return new ArrayList<>(requiredItems);
    }

    public void setRequiredItems(List<RequiredItem> requiredItems) {
        this.requiredItems = requiredItems != null ? new ArrayList<>(requiredItems) : new ArrayList<>();
    }

    public void addRequiredItem(RequiredItem item) {
        if (item != null) {
            this.requiredItems.add(item);
        }
    }

    public void removeRequiredItem(RequiredItem item) {
        this.requiredItems.remove(item);
    }

    public void clearRequiredItems() {
        this.requiredItems.clear();
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredKit() {
        return requiredKit;
    }

    public void setRequiredKit(String requiredKit) {
        this.requiredKit = requiredKit;
    }

    public String getKitToGive() {
        return kitToGive;
    }

    public void setKitToGive(String kitToGive) {
        this.kitToGive = kitToGive;
    }

    public double getCreationCost() {
        return creationCost;
    }

    public void setCreationCost(double creationCost) {
        this.creationCost = creationCost;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("location", location);
        map.put("owner", owner.toString());
        map.put("exitType", exitType.toString());
        map.put("selectionMode", selectionMode.toString());
        if (customExit != null) {
            map.put("customExit", customExit);
        }
        // Save all exit points
        if (!exitPoints.isEmpty()) {
            map.put("exitPoints", exitPoints);
        }
        map.put("createdTime", createdTime);
        if (kitName != null) {
            map.put("kitName", kitName);
        }
        map.put("cost", cost);
        map.put("showParticles", showParticles);

        // Save new fields
        if (!requiredItems.isEmpty()) {
            List<Map<String, Object>> itemsData = new ArrayList<>();
            for (RequiredItem item : requiredItems) {
                itemsData.add(item.serialize());
            }
            map.put("requiredItems", itemsData);
        }
        if (requiredPermission != null) {
            map.put("requiredPermission", requiredPermission);
        }
        if (requiredKit != null) {
            map.put("requiredKit", requiredKit);
        }
        if (kitToGive != null) {
            map.put("kitToGive", kitToGive);
        }
        map.put("creationCost", creationCost);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Portal deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        Location location = (Location) map.get("location");
        UUID owner = UUID.fromString((String) map.get("owner"));
        ExitType exitType = ExitType.valueOf((String) map.get("exitType"));
        ExitSelectionMode selectionMode = map.containsKey("selectionMode") ?
            ExitSelectionMode.valueOf((String) map.get("selectionMode")) : ExitSelectionMode.RANDOM;
        Location customExit = map.containsKey("customExit") ? (Location) map.get("customExit") : null;
        long createdTime = map.containsKey("createdTime") ? ((Number) map.get("createdTime")).longValue() : System.currentTimeMillis();
        String kitName = map.containsKey("kitName") ? (String) map.get("kitName") : null;
        double cost = map.containsKey("cost") ? ((Number) map.get("cost")).doubleValue() : 0;
        boolean showParticles = map.containsKey("showParticles") ? (Boolean) map.get("showParticles") : true;

        Portal portal = new Portal(name, location, owner, exitType, customExit, createdTime, kitName, cost, showParticles);
        portal.setSelectionMode(selectionMode);

        // Load multiple exit points if available
        if (map.containsKey("exitPoints")) {
            List<Location> exitPoints = (List<Location>) map.get("exitPoints");
            for (Location exit : exitPoints) {
                portal.addExitPoint(exit);
            }
        }

        // Load new fields
        if (map.containsKey("requiredItems")) {
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) map.get("requiredItems");
            for (Map<String, Object> itemData : itemsData) {
                portal.addRequiredItem(RequiredItem.deserialize(itemData));
            }
        }
        if (map.containsKey("requiredPermission")) {
            portal.setRequiredPermission((String) map.get("requiredPermission"));
        }
        if (map.containsKey("requiredKit")) {
            portal.setRequiredKit((String) map.get("requiredKit"));
        }
        if (map.containsKey("kitToGive")) {
            portal.setKitToGive((String) map.get("kitToGive"));
        }
        if (map.containsKey("creationCost")) {
            portal.setCreationCost(((Number) map.get("creationCost")).doubleValue());
        }

        return portal;
    }

    public enum ExitType {
        SPAWN,
        BED,
        CUSTOM,
        RANDOM  // New type for random selection from multiple exits
    }

    public enum ExitSelectionMode {
        FIRST("Uses first exit point"),
        RANDOM("Randomly selects"),
        SEQUENTIAL("Cycles through in order"),
        NEAREST("Uses nearest exit point");

        private final String description;

        ExitSelectionMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents an item required to activate a portal
     */
    public static class RequiredItem implements ConfigurationSerializable {
        private final Material material;
        private final int amount;
        private final boolean consume;
        private final String displayName;
        private final List<String> lore;

        public RequiredItem(Material material, int amount, boolean consume, String displayName, List<String> lore) {
            this.material = material;
            this.amount = amount;
            this.consume = consume;
            this.displayName = displayName;
            this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        }

        public RequiredItem(Material material, int amount, boolean consume) {
            this(material, amount, consume, null, null);
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public boolean shouldConsume() {
            return consume;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return new ArrayList<>(lore);
        }

        @Override
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("material", material.name());
            map.put("amount", amount);
            map.put("consume", consume);
            if (displayName != null) {
                map.put("displayName", displayName);
            }
            if (!lore.isEmpty()) {
                map.put("lore", lore);
            }
            return map;
        }

        @SuppressWarnings("unchecked")
        public static RequiredItem deserialize(Map<String, Object> map) {
            Material material = Material.valueOf((String) map.get("material"));
            int amount = ((Number) map.get("amount")).intValue();
            boolean consume = (Boolean) map.get("consume");
            String displayName = map.containsKey("displayName") ? (String) map.get("displayName") : null;
            List<String> lore = map.containsKey("lore") ? (List<String>) map.get("lore") : null;
            return new RequiredItem(material, amount, consume, displayName, lore);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequiredItem that = (RequiredItem) o;
            return amount == that.amount &&
                   consume == that.consume &&
                   material == that.material &&
                   java.util.Objects.equals(displayName, that.displayName) &&
                   java.util.Objects.equals(lore, that.lore);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(material, amount, consume, displayName, lore);
        }

        @Override
        public String toString() {
            return String.format("%dx %s%s", amount, material.name(), consume ? " (consumed)" : "");
        }
    }
}