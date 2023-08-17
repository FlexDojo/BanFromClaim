package no.vestlandetmc.BanFromClaim.commands.griefprevention;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.config.ClaimData;
import no.vestlandetmc.BanFromClaim.config.Messages;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class UnbfcCommand implements CommandExecutor, TabCompleter {

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageHandler.sendConsole("&cThis command can only be used in-game.");
            return true;
        }

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

        final String accessDenied = claim.allowGrantPermission(player);
        boolean allowBan = accessDenied == null;

        if (player.hasPermission("bfc.admin")) {
            allowBan = true;
        }


        if (!allowBan) {
            MessageHandler.sendMessage(player, Messages.NO_ACCESS);
            return true;

        } else {
            final String claimOwner = claim.getOwnerName();
            final String claimID = claim.getID().toString();

            List<String> bannedPlayers = listPlayers(claimID);

            if (bannedPlayers == null) {
                player.sendMessage("§cNon ci sono giocatori bannati in questa claim");
                return true;
            }

            List<UUID> uuids = bannedPlayers.stream().map(UUID::fromString).toList();

//			List<String> names = uuids.stream().map(ClaimData.getInstance()::getName).filter(Optional::isPresent).map(Optional::get).toList();
            Map<String, UUID> names = new HashMap<>();

            for (UUID uuid : uuids) {
                ClaimData.getInstance().getName(uuid).ifPresent(name -> names.put(name, uuid));
            }

            Optional<String> optional = names.keySet().stream().filter(name -> name.equalsIgnoreCase(args[0])).findFirst();

            if (optional.isPresent()) {
                UUID uuid = names.get(optional.get());

                if (setClaimData(player, claimID, uuid.toString(), false)) {
                    MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNBANNED, optional.get(), player.getDisplayName(), claimOwner));
                    Player bPlayer = Bukkit.getPlayer(uuid);
                    if (bPlayer != null) {
                        MessageHandler.sendMessage(bPlayer, Messages.placeholders(Messages.UNBANNED_TARGET, bPlayer.getName(), player.getDisplayName(), claimOwner));
                    }
                    return true;
                }
            } else {
                MessageHandler.sendMessage(player, Messages.placeholders(Messages.NOT_BANNED, args[0], player.getDisplayName(), null));
            }

//			if(listPlayers(claim.getID().toString()) != null) {
//				for(final String bp : listPlayers(claim.getID().toString())) {
//					final OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(bp));
//					if(bannedPlayer.getName().equalsIgnoreCase(args[0])) {
//						bPlayer = bannedPlayer;
//						if(setClaimData(player, claimID, bp, false)) {
//							MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNBANNED, bannedPlayer.getName(), player.getDisplayName(), claimOwner));
//							if(bannedPlayer.isOnline()) {
//								MessageHandler.sendMessage(bannedPlayer.getPlayer(), Messages.placeholders(Messages.UNBANNED_TARGET, bannedPlayer.getName(), player.getDisplayName(), claimOwner));
//							}
//							return true;
//						}
//					}
//				}
//			}
        }

        return true;
    }

    private List<String> listPlayers(String claimID) {
        final ClaimData claimData = ClaimData.getInstance();

        return claimData.bannedPlayers(claimID);
    }

    private boolean setClaimData(Player player, String claimID, String bannedUUID, boolean add) {
        final ClaimData claimData = ClaimData.getInstance();

        return claimData.setClaimData(player, claimID, bannedUUID, add);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return List.of();

        if (strings.length != 1) return List.of();

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null) return List.of();

        List<String> bannedPlayers = listPlayers(claim.getID().toString());

        if (bannedPlayers == null) return List.of();

        List<UUID> uuids = bannedPlayers.stream().map(UUID::fromString).toList();

        List<String> names = uuids.stream().map(ClaimData.getInstance()::getName).filter(Optional::isPresent).map(Optional::get).toList();

        return StringUtil.copyPartialMatches(strings[0], names, new ArrayList<>());
    }
}
