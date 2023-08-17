package no.vestlandetmc.BanFromClaim.config;

import com.nextdevv.metacoin.api.GettoniAPI;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimData {

    private static File file;
    private static ClaimData instance;
    private final FileConfiguration cfg = BfcPlugin.getInstance().getDataFile();
    private final String prefix = "bfc_claim_data";
    private final Map<UUID, String> namesCache;

    public static ClaimData getInstance() {
        if (instance == null) {
            instance = new ClaimData();
        }
        return instance;
    }

    private ClaimData() {
        namesCache = new ConcurrentHashMap<>();
        Bukkit.getScheduler().runTaskTimerAsynchronously(BfcPlugin.getInstance(), this::loadNames, 0L, 20L * 30);
    }

    private void loadNames() {
        for (String claimID : cfg.getConfigurationSection(prefix).getKeys(false)) {
            for (String uuid : cfg.getStringList(prefix + "." + claimID)) {
                UUID u = UUID.fromString(uuid);
                GettoniAPI.getInstance().getName(u).thenAccept(name -> namesCache.put(u, name));
            }
        }
    }

    public Optional<String> getName(UUID uuid) {
        return Optional.ofNullable(namesCache.get(uuid));
    }

    public boolean setClaimData(Player player, String claimID, String bannedUUID, boolean add) {
        if (add) {
            if (!existData(claimID, bannedUUID)) {
                addData(claimID, bannedUUID);
                return true;
            } else {
                return false;
            }
        } else {
            if (existData(claimID, bannedUUID)) {
                removeData(claimID, bannedUUID);
                return true;
            } else {
                return false;
            }
        }
    }

    private void addData(String claimID, String bannedUUID) {
        final List<String> uuid = new ArrayList<>();

        if (!cfg.contains(prefix + "." + claimID)) {
            cfg.createSection(prefix + "." + claimID);
        }

        if (!cfg.getStringList(prefix + "." + claimID).isEmpty()) {
            uuid.addAll(cfg.getStringList(prefix + "." + claimID));
        }
        uuid.add(bannedUUID);
        cfg.set(prefix + "." + claimID, uuid);
        saveDatafile();
    }

    public void banAll(String claimID) {
        if (cfg.contains("claims-ban-all" + "." + claimID + ".ban-all")) {
            if (cfg.getBoolean("claims-ban-all" + "." + claimID + ".ban-all")) {
                cfg.set("claims-ban-all" + "." + claimID + ".ban-all", false);
            } else {
                cfg.set("claims-ban-all" + "." + claimID + ".ban-all", true);
            }
        } else {
            cfg.set("claims-ban-all" + "." + claimID + ".ban-all", true);
        }

        saveDatafile();
    }

    public boolean isAllBanned(String claimID) {
        if (cfg.contains("claims-ban-all" + "." + claimID + ".ban-all")) {
            return cfg.getBoolean("claims-ban-all" + "." + claimID + ".ban-all");
        } else {
            return false;
        }
    }

    private void removeData(String claimID, String bannedUUID) {
        final List<String> uuid = new ArrayList<>();

        if (!cfg.getStringList(prefix + "." + claimID).isEmpty()) {
            uuid.addAll(cfg.getStringList(prefix + "." + claimID));
            if (uuid.contains(bannedUUID)) {
                uuid.remove(bannedUUID);
                cfg.set(prefix + "." + claimID, uuid);

                if (cfg.getStringList(prefix + "." + claimID).isEmpty()) {
                    cfg.set(prefix + "." + claimID, null);
                }
                saveDatafile();
            }
        }
    }

    private boolean existData(String claimID, String bannedUUID) {

        if (cfg.contains(prefix + "." + claimID)) {
            if (cfg.getStringList(prefix + "." + claimID).isEmpty()) {
                return false;

            } else {
                final List<String> uuid = new ArrayList<>(cfg.getStringList(prefix + "." + claimID));
                return uuid.contains(bannedUUID);
            }
        }

        return false;
    }

    public boolean checkClaim(String claimID) {
        return cfg.contains(prefix + "." + claimID);
    }

    public List<String> bannedPlayers(String claimID) {
        if (cfg.contains(prefix + "." + claimID)) {
            if (!cfg.getStringList(prefix + "." + claimID).isEmpty()) {
                return cfg.getStringList(prefix + "." + claimID);
            }
        }

        return null;
    }

    private static void saveDatafile() {
        try {
            file = new File(BfcPlugin.getInstance().getDataFolder(), "data.dat");
            BfcPlugin.getInstance().getDataFile().save(file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void createSection() {
        if (!BfcPlugin.getInstance().getDataFile().contains("bfc_claim_data")) {
            BfcPlugin.getInstance().getDataFile().createSection("bfc_claim_data");
        }
        if (!BfcPlugin.getInstance().getDataFile().contains("claims-ban-all")) {
            BfcPlugin.getInstance().getDataFile().createSection("claims-ban-all");
        }
        saveDatafile();
    }

    public static void cleanDatafile() {
        boolean clean = false;
        final String prefix = BfcPlugin.getInstance().getDescription().getPrefix();

        if (!BfcPlugin.getInstance().getDataFile().getKeys(false).isEmpty()) {
            if (!BfcPlugin.getInstance().getDataFile().getConfigurationSection("bfc_claim_data").getKeys(false).isEmpty()) {
                for (final String claimID : BfcPlugin.getInstance().getDataFile().getConfigurationSection("bfc_claim_data").getKeys(false)) {
                    if (BfcPlugin.getInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
                        if (GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimID)) == null) {
                            BfcPlugin.getInstance().getDataFile().set("bfc_claim_data." + claimID, null);
                            clean = true;
                        }
                    }
                }
            }

            if (!BfcPlugin.getInstance().getDataFile().getConfigurationSection("claims-ban-all").getKeys(false).isEmpty()) {
                for (final String claimID : BfcPlugin.getInstance().getDataFile().getConfigurationSection("bfc_claim_data").getKeys(false)) {
                    if (BfcPlugin.getInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
                        if (GriefPrevention.instance.dataStore.getClaim(Long.parseLong(claimID)) == null) {
                            BfcPlugin.getInstance().getDataFile().set("claims-ban-all." + claimID, null);
                            clean = true;
                        }
                    }
                }

                if (clean) {
                    saveDatafile();
                    MessageHandler.sendConsole("&2[" + prefix + "] &eData storage has been cleared of old removed claims...");
                }
            }
        }
    }
}
