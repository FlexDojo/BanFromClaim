package no.vestlandetmc.BanFromClaim.listener;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.objects.GetUpReason;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.config.ClaimData;
import no.vestlandetmc.BanFromClaim.config.Config;
import no.vestlandetmc.BanFromClaim.config.Messages;
import no.vestlandetmc.BanFromClaim.handler.LocationFinder;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import no.vestlandetmc.BanFromClaim.handler.ParticleHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GPListener implements Listener {

    private final BfcPlugin plugin;
    private final ClaimData claimData;
    private final Map<UUID, Location> lastLocation;

    public GPListener(BfcPlugin plugin) {
        this.plugin = plugin;
        this.claimData = ClaimData.getInstance();
        this.lastLocation = new HashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Location loc = e.getPlayer().getLocation();

        checkBan(e.getPlayer(), loc, loc, true);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Location loc = e.getTo();
        Player player = e.getPlayer();
        boolean check = checkBan(player, e.getFrom(), loc, false);
        if (check) {
            e.setCancelled(true);
        }
    }

    private boolean checkBan(Player player, Location locFrom, Location locTo, boolean tp) {
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(locTo, true, null);

        if (claim == null) return false;

        final ParticleHandler ph = new ParticleHandler(locTo);

        if (player.hasPermission("bfc.bypass") || player.getGameMode().equals(GameMode.SPECTATOR)) {
            return false;
        }

        final UUID ownerUUID = claim.ownerID;
        final String claimID = claim.getID().toString();
        boolean hasAttacked = false;

        if (CombatMode.attackerContains(player.getUniqueId()))
            hasAttacked = CombatMode.getAttacker(player.getUniqueId()).equals(ownerUUID);

        if ((claimData.isAllBanned(claimID) || playerBanned(player, claimID)) && !hasAttacked && !hasTrust(player, claim)) {
            if (claim.contains(locFrom, true, false)) {
                if (playerBanned(player, claimID) || claimData.isAllBanned(claimID)) {
                    final int sizeRadius = Math.max(claim.getHeight(), claim.getWidth());

                    if (tp) {
                        final LocationFinder lf = new LocationFinder(claim.getGreaterBoundaryCorner(), claim.getLesserBoundaryCorner(), player.getWorld().getUID(), sizeRadius);
                        Bukkit.getScheduler().runTaskAsynchronously(BfcPlugin.getInstance(), () -> lf.IterateCircumferences(randomCircumferenceRadiusLoc -> {
                            player.teleport(Objects.requireNonNullElseGet(randomCircumferenceRadiusLoc, () -> Objects.requireNonNullElseGet(Config.SAFE_LOCATION, () -> player.getWorld().getSpawnLocation())));

                        }));
                    }
                } else {
                    if (tp) {
                        final Location tpLoc = player.getLocation().add(locFrom.toVector().subtract(locTo.toVector()).normalize().multiply(3));

                        if (tpLoc.getBlock().getType().equals(Material.AIR)) {
                            player.teleport(tpLoc);
                        } else {
                            final Location safeLoc = tpLoc.getWorld().getHighestBlockAt(tpLoc).getLocation().add(0D, 1D, 0D);
                            player.teleport(safeLoc);
                        }

                        ph.drawCircle(1, locTo.getBlockX() == locFrom.getBlockX());
                    }
                }

            } else {
                if(tp) {
                    final Location tpLoc = player.getLocation().add(locFrom.toVector().subtract(locTo.toVector()).normalize().multiply(3));
                    if (tpLoc.getBlock().getType().equals(Material.AIR)) {
                        player.teleport(tpLoc);
                    } else {
                        final Location safeLoc = tpLoc.getWorld().getHighestBlockAt(tpLoc).getLocation().add(0D, 1D, 0D);
                        player.teleport(safeLoc);
                    }

                    ph.drawCircle(1, locTo.getBlockX() == locFrom.getBlockX());
                }
            }


            if (!MessageHandler.spamMessageClaim.contains(player.getUniqueId().toString())) {
                MessageHandler.sendTitle(player, Messages.TITLE_MESSAGE, Messages.SUBTITLE_MESSAGE);
                MessageHandler.spamMessageClaim.add(player.getUniqueId().toString());

                Bukkit.getScheduler().runTaskLater(BfcPlugin.getInstance(), () -> {
                    MessageHandler.spamMessageClaim.remove(player.getUniqueId().toString());
                }, 5L * 20L);
            }
            return true;
        }
        if (GSitAPI.isSitting(player)) {
            GSitAPI.removeSeat(player, GetUpReason.PLUGIN);
        }

        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerEnterClaim(PlayerMoveEvent e) {
        final Location locFrom = e.getFrom();
        final Location locTo = e.getTo();

        if (locTo == null || locTo.getWorld() == null) return;

        if (locTo.getBlockY() > locTo.getWorld().getMaxHeight() + 10) return;

        if (lastLocation.getOrDefault(e.getPlayer().getUniqueId(), locFrom).equals(locTo)) return;
        else lastLocation.put(e.getPlayer().getUniqueId(), locTo);

        if (locFrom.getBlock().equals(locTo.getBlock())) {
            return;
        }

        final Player player = e.getPlayer();

        checkBan(player, locFrom, locTo, true);
    }


    private boolean playerBanned(Player player, String claimID) {
        final ClaimData claimData = ClaimData.getInstance();
        if (claimData.checkClaim(claimID)) {
            if (claimData.bannedPlayers(claimID) != null) {
                for (final String bp : claimData.bannedPlayers(claimID)) {
                    if (bp.equals(player.getUniqueId().toString())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean hasTrust(Player player, Claim claim) {
        final String accessDenied = claim.allowGrantPermission(player);
        final String buildDenied = claim.allowBuild(player, Material.DIRT);

        if (accessDenied == null || buildDenied == null || player.getUniqueId().equals(claim.getOwnerID())) {
            return true;
        } else {
            return false;
        }
    }
}
