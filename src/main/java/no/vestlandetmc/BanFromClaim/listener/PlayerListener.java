package no.vestlandetmc.BanFromClaim.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import no.vestlandetmc.BanFromClaim.BfcPlugin;
import no.vestlandetmc.BanFromClaim.handler.MessageHandler;

public class PlayerListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent p) {
		final Player player = p.getPlayer();


	}

}
