package portals.portaltoexit.data;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Represents a kit that can be given to players when using portals
 */
public class Kit implements ConfigurationSerializable {
    private final String name;
    private final List<ItemStack> items;
    private final Map<String, ItemStack> armor;  // helmet, chestplate, leggings, boots
    private final boolean clearInventory;
    private final int cooldown;  // In seconds
    private final double cost;  // Additional cost for the kit
    private final String permission;  // Optional permission required

    public Kit(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.armor = new HashMap<>();
        this.clearInventory = false;
        this.cooldown = 0;
        this.cost = 0;
        this.permission = null;
    }

    public Kit(String name, List<ItemStack> items, Map<String, ItemStack> armor,
               boolean clearInventory, int cooldown, double cost, String permission) {
        this.name = name;
        this.items = items != null ? items : new ArrayList<>();
        this.armor = armor != null ? armor : new HashMap<>();
        this.clearInventory = clearInventory;
        this.cooldown = cooldown;
        this.cost = cost;
        this.permission = permission;
    }

    /**
     * Apply this kit to a player
     */
    public void applyToPlayer(Player player) {
        PlayerInventory inventory = player.getInventory();

        // Clear inventory if configured
        if (clearInventory) {
            inventory.clear();
            inventory.setHelmet(null);
            inventory.setChestplate(null);
            inventory.setLeggings(null);
            inventory.setBoots(null);
        }

        // Apply armor
        if (armor.containsKey("helmet") && armor.get("helmet") != null) {
            inventory.setHelmet(armor.get("helmet").clone());
        }
        if (armor.containsKey("chestplate") && armor.get("chestplate") != null) {
            inventory.setChestplate(armor.get("chestplate").clone());
        }
        if (armor.containsKey("leggings") && armor.get("leggings") != null) {
            inventory.setLeggings(armor.get("leggings").clone());
        }
        if (armor.containsKey("boots") && armor.get("boots") != null) {
            inventory.setBoots(armor.get("boots").clone());
        }

        // Add items
        for (ItemStack item : items) {
            if (item != null) {
                HashMap<Integer, ItemStack> leftover = inventory.addItem(item.clone());
                // Drop items that don't fit
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        player.updateInventory();
    }

    /**
     * Check if player has permission for this kit
     */
    public boolean hasPermission(Player player) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    /**
     * Create kit from configuration section
     */
    public static Kit fromConfig(String name, Map<String, Object> config) {
        List<ItemStack> items = new ArrayList<>();
        Map<String, ItemStack> armor = new HashMap<>();

        // Parse items
        if (config.containsKey("items")) {
            List<?> itemList = (List<?>) config.get("items");
            if (itemList != null) {
                for (Object itemObj : itemList) {
                    ItemStack item;
                    if (itemObj instanceof Map) {
                        // New config format with Map/ConfigurationSection
                        item = parseItemConfig((Map<String, Object>) itemObj);
                    } else {
                        // Legacy string format
                        item = parseItemString(itemObj.toString());
                    }
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        }

        // Parse armor
        if (config.containsKey("armor")) {
            Object armorObj = config.get("armor");
            if (armorObj instanceof ConfigurationSection) {
                ConfigurationSection armorSection = (ConfigurationSection) armorObj;
                for (String slot : armorSection.getKeys(false)) {
                    Object itemData = armorSection.get(slot);
                    ItemStack item = null;

                    if (itemData instanceof ConfigurationSection) {
                        ConfigurationSection itemSection = (ConfigurationSection) itemData;
                        Map<String, Object> itemMap = new HashMap<>();
                        for (String key : itemSection.getKeys(false)) {
                            itemMap.put(key, itemSection.get(key));
                        }
                        item = parseItemConfig(itemMap);
                    } else if (itemData instanceof Map) {
                        item = parseItemConfig((Map<String, Object>) itemData);
                    } else if (itemData instanceof String) {
                        item = parseItemString((String) itemData);
                    }

                    if (item != null) {
                        armor.put(slot.toLowerCase(), item);
                    }
                }
            } else if (armorObj instanceof Map) {
                Map<?, ?> armorMap = (Map<?, ?>) armorObj;
                for (Map.Entry<?, ?> entry : armorMap.entrySet()) {
                    String slot = entry.getKey().toString().toLowerCase();
                    ItemStack item = null;
                    if (entry.getValue() instanceof Map) {
                        item = parseItemConfig((Map<String, Object>) entry.getValue());
                    } else if (entry.getValue() instanceof String) {
                        item = parseItemString(entry.getValue().toString());
                    }
                    if (item != null) {
                        armor.put(slot, item);
                    }
                }
            }
        }

        boolean clearInventory = false;
        if (config.containsKey("clear-inventory")) {
            clearInventory = (Boolean) config.get("clear-inventory");
        }

        int cooldown = 0;
        if (config.containsKey("cooldown")) {
            cooldown = ((Number) config.get("cooldown")).intValue();
        }

        double cost = 0;
        if (config.containsKey("cost")) {
            cost = ((Number) config.get("cost")).doubleValue();
        }

        String permission = null;
        if (config.containsKey("permission")) {
            permission = config.get("permission").toString();
        }

        return new Kit(name, items, armor, clearInventory, cooldown, cost, permission);
    }

    /**
     * Parse item string format: "MATERIAL AMOUNT" or "MATERIAL:DATA AMOUNT"
     */
    private static ItemStack parseItemString(String itemString) {
        if (itemString == null || itemString.isEmpty()) {
            return null;
        }

        String[] parts = itemString.trim().split(" ");
        if (parts.length == 0) {
            return null;
        }

        String materialName = parts[0];
        int amount = 1;

        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        // Parse material directly
        try {
            Material material = Material.valueOf(materialName.toUpperCase().replace("MINECRAFT:", ""));
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            // Material not found
            return null;
        }
    }

    /**
     * Parse item from configuration map format
     */
    private static ItemStack parseItemConfig(Map<String, Object> itemConfig) {
        if (itemConfig == null || !itemConfig.containsKey("material")) {
            return null;
        }

        // Get material (required)
        String materialName = itemConfig.get("material").toString();
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase().replace("MINECRAFT:", ""));
        } catch (IllegalArgumentException e) {
            // Material not found
            return null;
        }

        // Get amount (default 1)
        int amount = 1;
        if (itemConfig.containsKey("amount")) {
            try {
                amount = ((Number) itemConfig.get("amount")).intValue();
            } catch (Exception e) {
                amount = 1;
            }
        }

        // Create ItemStack
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        // Set display name (optional)
        if (itemConfig.containsKey("name")) {
            String name = itemConfig.get("name").toString();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        // Set lore (optional)
        if (itemConfig.containsKey("lore")) {
            Object loreObj = itemConfig.get("lore");
            List<String> lore = new ArrayList<>();

            if (loreObj instanceof List) {
                List<?> loreList = (List<?>) loreObj;
                for (Object line : loreList) {
                    if (line != null) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line.toString()));
                    }
                }
            } else if (loreObj instanceof String) {
                // Single line lore
                lore.add(ChatColor.translateAlternateColorCodes('&', loreObj.toString()));
            }

            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
        }

        // Apply ItemMeta
        itemStack.setItemMeta(meta);

        // Add enchantments (optional)
        if (itemConfig.containsKey("enchantments")) {
            Object enchObj = itemConfig.get("enchantments");
            if (enchObj instanceof Map) {
                Map<?, ?> enchantments = (Map<?, ?>) enchObj;
                for (Map.Entry<?, ?> entry : enchantments.entrySet()) {
                    try {
                        String enchantName = entry.getKey().toString().toUpperCase();
                        int level = ((Number) entry.getValue()).intValue();

                        // Try to get enchantment by name
                        Enchantment enchantment = Enchantment.getByName(enchantName);
                        if (enchantment != null) {
                            itemStack.addUnsafeEnchantment(enchantment, level);
                        }
                    } catch (Exception e) {
                        // Skip invalid enchantments
                    }
                }
            }
        }

        return itemStack;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);

        // Serialize items
        List<String> itemStrings = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                itemStrings.add(item.getType().name() + " " + item.getAmount());
            }
        }
        map.put("items", itemStrings);

        // Serialize armor
        Map<String, String> armorStrings = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : armor.entrySet()) {
            if (entry.getValue() != null) {
                armorStrings.put(entry.getKey(), entry.getValue().getType().name());
            }
        }
        map.put("armor", armorStrings);

        map.put("clear-inventory", clearInventory);
        map.put("cooldown", cooldown);
        map.put("cost", cost);
        if (permission != null) {
            map.put("permission", permission);
        }

        return map;
    }

    // Getters
    public String getName() { return name; }
    public List<ItemStack> getItems() { return items; }
    public Map<String, ItemStack> getArmor() { return armor; }
    public boolean isClearInventory() { return clearInventory; }
    public int getCooldown() { return cooldown; }
    public double getCost() { return cost; }
    public String getPermission() { return permission; }
}