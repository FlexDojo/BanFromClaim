package no.vestlandetmc.BanFromClaim.commands.griefprevention;

import com.nextdevv.metacoin.api.GettoniAPI;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.config.ClaimData;
import no.vestlandetmc.BanFromClaim.config.Config;
import no.vestlandetmc.BanFromClaim.config.Messages;
import no.vestlandetmc.BanFromClaim.handler.LocationFinder;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BfcCommand implements CommandExecutor, TabCompleter {

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			MessageHandler.sendConsole("&cThis command can only be used in-game.");
			return true;
		}

		final Player player = (Player) sender;
		final Location loc = player.getLocation();
		final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);

		if(args.length == 0) {
			MessageHandler.sendMessage(player, Messages.NO_ARGUMENTS);
			return true;
		}

		if(claim == null) {
			MessageHandler.sendMessage(player, Messages.OUTSIDE_CLAIM);
			return true;
		}

		GettoniAPI.getInstance().getName(args[0]).thenAccept(u -> {


			if(u==null) {
				MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNVALID_PLAYERNAME, args[0], player.getDisplayName(), null));
				return;
			}

			Bukkit.getScheduler().runTask(BfcPlugin.getInstance(), () -> {
				final OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(u);
				final String accessDenied = claim.allowGrantPermission(player);
				boolean allowBan = accessDenied == null;

				if(player.hasPermission("bfc.admin")) { allowBan = true; }

				if(!bannedPlayer.isOnline()) {
					if(!bannedPlayer.hasPlayedBefore()) {
						MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNVALID_PLAYERNAME, args[0], player.getDisplayName(), null));
						return;
					} else if(bannedPlayer == player) {
						MessageHandler.sendMessage(player, Messages.BAN_SELF);
						return;
					} else if(bannedPlayer.getName().equals(claim.getOwnerName())) {
						MessageHandler.sendMessage(player, Messages.BAN_OWNER);
						return;
					}
				} else {
					if(bannedPlayer.getPlayer().hasPermission("bfc.bypass")) {
						MessageHandler.sendMessage(player, Messages.placeholders(Messages.PROTECTED, bannedPlayer.getPlayer().getDisplayName(), null, null));
						return;
					}
				}

				if(!allowBan) {
					MessageHandler.sendMessage(player, Messages.NO_ACCESS);
					return;
				} else {
					final String claimOwner = claim.getOwnerName();

					final int sizeRadius = Math.max(claim.getHeight(), claim.getWidth());
					final Location greaterCorner = claim.getGreaterBoundaryCorner();
					final Location lesserCorner = claim.getLesserBoundaryCorner();

					if(setClaimData(player, claim.getID().toString(), bannedPlayer.getUniqueId().toString(), true)) {
						if(bannedPlayer.isOnline()) {
							if(GriefPrevention.instance.dataStore.getClaimAt(bannedPlayer.getPlayer().getLocation(), true, claim) != null) {
								if(GriefPrevention.instance.dataStore.getClaimAt(bannedPlayer.getPlayer().getLocation(), true, claim) == claim) {
									final Location bannedLoc = bannedPlayer.getPlayer().getLocation();
									final LocationFinder lf = new LocationFinder(greaterCorner, lesserCorner, bannedLoc.getWorld().getUID(), sizeRadius);

									Bukkit.getScheduler().runTaskAsynchronously(BfcPlugin.getInstance(), () -> lf.IterateCircumferences(randomCircumferenceRadiusLoc -> {
                                        bannedPlayer.getPlayer().teleport(Objects.requireNonNullElseGet(randomCircumferenceRadiusLoc, () -> Objects.requireNonNullElseGet(Config.SAFE_LOCATION, () -> bannedLoc.getWorld().getSpawnLocation())));

										MessageHandler.sendMessage(bannedPlayer.getPlayer(), Messages.placeholders(Messages.BANNED_TARGET, bannedPlayer.getName(), player.getDisplayName(), claimOwner));

									}));
								}
							}
						}

						MessageHandler.sendMessage(player, Messages.placeholders(Messages.BANNED, bannedPlayer.getName(), null, null));

					} else {
						MessageHandler.sendMessage(player, Messages.ALREADY_BANNED);
					}
				}
			});
		}).exceptionally(e -> {
			MessageHandler.sendMessage(player, Messages.placeholders(Messages.UNVALID_PLAYERNAME, args[0], player.getDisplayName(), null));
			BfcPlugin.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "An error occurred while trying to ban a player from a claim.", e);
			return null;
		});


		return true;
	}

	private boolean setClaimData(Player player, String claimID, String bannedUUID, boolean add) {
		final ClaimData claimData = ClaimData.getInstance();

		return claimData.setClaimData(player, claimID, bannedUUID, add);
	}

	@Override
	public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
		return StringUtil.copyPartialMatches(strings[0], GettoniAPI.getInstance().getAllNames(), new ArrayList<>());
	}

}
