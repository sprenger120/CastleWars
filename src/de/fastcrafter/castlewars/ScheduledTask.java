package de.fastcrafter.castlewars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScheduledTask extends BukkitRunnable {
	private CastleWars cw = null;
	private TaskType tType = null;
	private Location toTeleport = null;
	private String playerName = null;
	
	public ScheduledTask(CastleWars para,TaskType type,Location loc,String splr) {
		cw = para;
		tType = type;
		toTeleport = loc;
		playerName = splr;
	}
	
	@Override
	public void run() {
		switch (tType) {
		case GAMELOOP:
			cw.gameLoop();
			break;
		case FLUSHCONFIG:
			cw.flushConfig(false);
			break;
		case TELEPORT:
			Player plr = Bukkit.getPlayer(playerName);
			if (plr != null && toTeleport != null) {
				plr.teleport(toTeleport);
			}
			break;
		}
	}
}
