package no.vestlandetmc.BanFromClaim;

import no.vestlandetmc.BanFromClaim.commands.SafeSpot;
import no.vestlandetmc.BanFromClaim.commands.griefprevention.*;
import no.vestlandetmc.BanFromClaim.config.ClaimData;
import no.vestlandetmc.BanFromClaim.config.Config;
import no.vestlandetmc.BanFromClaim.config.Messages;
import no.vestlandetmc.BanFromClaim.handler.CombatScheduler;
import no.vestlandetmc.BanFromClaim.handler.Hooks;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import no.vestlandetmc.BanFromClaim.listener.CombatMode;
import no.vestlandetmc.BanFromClaim.listener.GPListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class BfcPlugin extends JavaPlugin {

    private static BfcPlugin instance;

    private File dataFile;
    private FileConfiguration data;

    public static BfcPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        MessageHandler.sendConsole("&2 ___ ___ ___");
        MessageHandler.sendConsole("&2| _ ) __/ __|        &8" + getDescription().getName() + " v" + getDescription().getVersion());
        MessageHandler.sendConsole("&2| _ \\ _| (__         &8Author: " + getDescription().getAuthors().toString().replace("[", "").replace("]", ""));
        MessageHandler.sendConsole("&2|___/_| \\___|");
        MessageHandler.sendConsole("");

        createDatafile();

        Config.initialize();

        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            MessageHandler.sendConsole("&2[" + getDescription().getPrefix() + "] &7Successfully hooked into &eGriefPrevention");
            MessageHandler.sendConsole("");

            Hooks.setGP();
            Hooks.setGSIT();

            this.getServer().getPluginManager().registerEvents(new GPListener(this), this);
            BfcCommand gpCommand = new BfcCommand();
            this.getCommand("banfromclaim").setExecutor(gpCommand);
            this.getCommand("banfromclaim").setTabCompleter(gpCommand);

            UnbfcCommand unbfcCommand = new UnbfcCommand();
            this.getCommand("unbanfromclaim").setExecutor(unbfcCommand);
            this.getCommand("unbanfromclaim").setTabCompleter(unbfcCommand);

            this.getCommand("banfromclaimlist").setExecutor(new BfclistCommand());
            this.getCommand("banfromclaimall").setExecutor(new BfcAllCommand());

            if (Config.KICKMODE) {
                KfcCommandGP kfcCommandGP = new KfcCommandGP();
                this.getCommand("kickfromclaim").setExecutor(kfcCommandGP);
                this.getCommand("kickfromclaim").setTabCompleter(kfcCommandGP);
            }

        } else {
            MessageHandler.sendConsole("&2[" + getDescription().getPrefix() + "] &cNo supported claimsystem was found.");
            MessageHandler.sendConsole("");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.getCommand("bfcsafespot").setExecutor(new SafeSpot());


        Messages.initialize();
        ClaimData.createSection();

        if (Config.COMBAT_ENABLED) {
            this.getServer().getPluginManager().registerEvents(new CombatMode(), this);
            new CombatScheduler().runTaskTimer(this, 0L, 20L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                ClaimData.cleanDatafile();
            }

        }.runTaskTimer(this, 30 * 20L, 3600 * 20L);

    }

    @Override
    public void onDisable() {

    }

    public FileConfiguration getDataFile() {
        return this.data;
    }

    public void createDatafile() {
        dataFile = new File(this.getDataFolder(), "data.dat");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        data = new YamlConfiguration();
        try {
            data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
