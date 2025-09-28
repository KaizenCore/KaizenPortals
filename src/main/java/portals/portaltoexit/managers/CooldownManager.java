package portals.portaltoexit.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Long> cooldowns;
    private final ConfigManager configManager;

    public CooldownManager(ConfigManager configManager) {
        this.cooldowns = new HashMap<>();
        this.configManager = configManager;
    }

    public boolean isOnCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }

        long lastUsed = cooldowns.get(playerId);
        long cooldownMs = configManager.getCooldownSeconds() * 1000L;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUsed >= cooldownMs) {
            cooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public int getRemainingCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) {
            return 0;
        }

        long lastUsed = cooldowns.get(playerId);
        long cooldownMs = configManager.getCooldownSeconds() * 1000L;
        long currentTime = System.currentTimeMillis();
        long remainingMs = cooldownMs - (currentTime - lastUsed);

        if (remainingMs <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }

        return (int) Math.ceil(remainingMs / 1000.0);
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }
}