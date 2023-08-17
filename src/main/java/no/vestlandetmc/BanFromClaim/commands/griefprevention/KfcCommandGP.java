package no.vestlandetmc.BanFromClaim.commands.griefprevention;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.config.Config;
import no.vestlandetmc.BanFromClaim.config.Messages;
import no.vestlandetmc.BanFromClaim.handler.LocationFinder;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KfcCommandGP implements CommandExecutor, TabCompleter {

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageHandler.sendConsole("&cThis command can only be used in-game.");
            return true;
        }

        final Player player = (Player) sender;
        final Location loc = player.getLocation();
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);

        if (args.length == 0) {
            MessageHandler.sendMessage(player, Messages.NO_ARGUMENTS);
            return true;
        }

        if (claim == null) {
            MessageHandler.sendMessage(player, Messages.OUTSIDE_CLAIM);
            return true;
        }

        final Player kickedPlayer = Bukkit.getPlayer(args[0]);
        final String accessDenied = claim.allowGrantPermission(player);
        boolean allowBan = false;

        if (accessDenied == null) {
            allowBan = true;
        }
        if (player.hasPermission("bfc.admin")) {
            allowBan = true;
        }

        if (kickedPlayer == null) {
            MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNVALID_PLAYERNAME, args[0], player.getDisplayName(), null));
            return true;
        } else if (kickedPlayer == player) {
            MessageHandler.sendMessage(player, Messages.KICK_SELF);
            return true;
        } else if (kickedPlayer.getName().equals(claim.getOwnerName())) {
            MessageHandler.sendMessage(player, Messages.KICK_OWNER);
            return true;
        }

        if (kickedPlayer.hasPermission("bfc.bypass")) {
            MessageHandler.sendMessage(player, Messages.placeholders(Messages.PROTECTED, kickedPlayer.getDisplayName(), null, null));
            return true;
        }

        if (!allowBan) {
            MessageHandler.sendMessage(player, Messages.NO_ACCESS);
            return true;
        } else {
            final String claimOwner = claim.getOwnerName();

            final int sizeRadius = Math.max(claim.getHeight(), claim.getWidth());
            final Location greaterCorner = claim.getGreaterBoundaryCorner();
            final Location lesserCorner = claim.getLesserBoundaryCorner();

            if (GriefPrevention.instance.dataStore.getClaimAt(kickedPlayer.getLocation(), true, claim) != null) {
                if (GriefPrevention.instance.dataStore.getClaimAt(kickedPlayer.getLocation(), true, claim) == claim) {
                    final Location bannedLoc = kickedPlayer.getLocation();
                    final LocationFinder lf = new LocationFinder(greaterCorner, lesserCorner, bannedLoc.getWorld().getUID(), sizeRadius);

                    Bukkit.getScheduler().runTaskAsynchronously(BfcPlugin.getInstance(), () -> lf.IterateCircumferences(randomCircumferenceRadiusLoc -> {
                        if (randomCircumferenceRadiusLoc == null) {
                            if (Config.SAFE_LOCATION == null) {
                                kickedPlayer.teleport(bannedLoc.getWorld().getSpawnLocation());
                            } else {
                                kickedPlayer.teleport(Config.SAFE_LOCATION);
                            }
                        } else {
                            kickedPlayer.teleport(randomCircumferenceRadiusLoc);
                        }

                        MessageHandler.sendMessage(kickedPlayer, Messages.placeholders(Messages.KICKED_TARGET, kickedPlayer.getName(), player.getDisplayName(), claimOwner));

                    }));
                }
            }
        }

        MessageHandler.sendMessage(player, Messages.placeholders(Messages.KICKED, kickedPlayer.getName(), null, null));
        return true;

    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player player)) return List.of();

        if (strings.length != 1) return List.of();

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null) return List.of();

        Location l1 = claim.getGreaterBoundaryCorner();
        Location l2 = claim.getLesserBoundaryCorner().clone();
        l2.setY(l2.getWorld().getMaxHeight());

        List<Player> players = Bukkit.getOnlinePlayers().stream().filter(p -> inRegion(p, l1, l2)).collect(Collectors.toList());

        players.remove(player);

        players.removeIf(p -> p.hasPermission("redistab.vanish.see"));

        return StringUtil.copyPartialMatches(strings[0], players.stream().map(Player::getName).collect(Collectors.toList()), new ArrayList<>());
    }

    public boolean inRegion(Player player, Location location1, Location location2) {
        Location playerLocation = player.getLocation();

        double minX = Math.min(location1.getX(), location2.getX());
        double maxX = Math.max(location1.getX(), location2.getX());

        double minY = Math.min(location1.getY(), location2.getY());
        double maxY = Math.max(location1.getY(), location2.getY());

        double minZ = Math.min(location1.getZ(), location2.getZ());
        double maxZ = Math.max(location1.getZ(), location2.getZ());

        return playerLocation.getX() >= minX && playerLocation.getX() <= maxX &&
                playerLocation.getY() >= minY && playerLocation.getY() <= maxY &&
                playerLocation.getZ() >= minZ && playerLocation.getZ() <= maxZ;
    }
}
