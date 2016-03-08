package de.fastcrafter.castlewars;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
//import me.confuser.barapi.BarAPI;
import me.confuser.barapi.BarAPI;

public final class CastleWars extends JavaPlugin implements Listener {
	private Map<String, GameInstance> Arenas = new HashMap<String, GameInstance>();
	private Map<String, SelectingOrder> PlrSelectingOrders = new HashMap<String, SelectingOrder>();
	private String ConfigFolder = new String();
	private Random rnd = new Random();

	private Messages msgs = null;
	private Statistics statistics = null;

	private String Header = new String("[CastleWars] ");

	private Map<String, List<ItemStack>> deathInventorys = new HashMap<String, List<ItemStack>>();

	private boolean ServerStopping = false;

	public void onEnable() {
		ConfigFolder = (new File("").getAbsolutePath()) + File.separator + "plugins" + File.separator + "CastleWars";
		File folder = new File(ConfigFolder);
		Yaml yaml = null;
		InputStream in = null;

		if (!folder.exists()) {
			folder.mkdir();
		}

		// Load statistics file
		yaml = new Yaml(new CustomClassLoaderConstructor(Statistics.class.getClassLoader()));
		File statisticsFile = new File(ConfigFolder + File.separator + "statistics.yml");
		if (statisticsFile.exists()) {
			try {
				in = Files.newInputStream(statisticsFile.toPath());
				statistics = (Statistics) yaml.load(in);
				in.close();
				if (statistics == null) {
					getLogger().info("Unable to load statistics.");
				}
			} catch (IOException e) {
				getLogger().info("Internal error while loading file");
			}
		}
		if (statistics == null) {
			statistics = new Statistics();
			getLogger().info("creating statistics object");
		}

		// Load language file
		try {
			in = Files.newInputStream(new File(ConfigFolder + File.separator + "lang-" + statistics.getLanguage() + ".yml").toPath());

			if (in == null) {
				getLogger().info("Unable to load language file!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			yaml = new Yaml(new CustomClassLoaderConstructor(Messages.class.getClassLoader()));
			msgs = (Messages) yaml.load(in);
			in.close();

			if (msgs == null) {
				getLogger().info("Unable to load language file!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			getLogger().info("Loaded language: " + statistics.getLanguage());
		} catch (IOException e) {
			getLogger().info("Internal error while loading language file.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Load configurations
		File[] files = folder.listFiles();
		for (int i = 0; i <= files.length - 1; i++) {
			String sFilename = files[i].getAbsolutePath();
			World wrld;

			if (!sFilename.substring(sFilename.length() - 4, sFilename.length()).toLowerCase().equals(".yml")) {
				continue; // not ending on .yml
			}

			if (files[i].toPath().getFileName().toString().compareTo("statistics.yml") == 0) {
				continue;
			} // statistics file

			if (files[i].toPath().getFileName().toString().substring(0, 5).compareTo("lang-") == 0) {
				continue;
			} // language file

			GameInstance inst;
			yaml = new Yaml(new CustomClassLoaderConstructor(GameInstance.class.getClassLoader()));
			try {
				in = Files.newInputStream(files[i].toPath());
				inst = (GameInstance) yaml.load(in);
				in.close();
				if (inst == null) {
					getLogger().info("Unable to load: " + files[i].toString());
					continue;
				}
			} catch (IOException e) {
				getLogger().info("Internal error while loading file");
				continue;
			}

			// extract arena name out of filename
			sFilename = files[i].toPath().getFileName().toString();
			inst.setArenaName(sFilename.substring(0, sFilename.length() - 4).toLowerCase());

			if ((wrld = Bukkit.getWorld(inst.getWorldName())) == null) {
				getLogger().info("Unable to find world " + inst.getWorldName() + ". Unable to load " + inst.getArenaName());
				continue;
			}
			Arenas.put(inst.getArenaName(), inst);
			getLogger().info("Loaded " + inst.getArenaName());

			// Initializing
			for (TowerEntry tower : inst.getTowers().values()) {
				tower.searchWool(wrld);
				setTowerColor(tower, wrld);
			}
		}

		rnd.setSeed(System.currentTimeMillis());
		new ScheduledTask(this, TaskType.GAMELOOP, null, null).runTaskLater(this, 20);
		new ScheduledTask(this, TaskType.FLUSHCONFIG, null, null).runTaskLater(this, 5 * 60 * 20);

		getLogger().info("CastleWars Plugin by Godofcode120/Sprenger120");
		getLogger().info("Thanks to confuserr for the BarAPI!");
		getServer().getPluginManager().registerEvents(this, this);
	}

	public void onDisable() {
		flushConfig(true);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be executed by players!");
			return true;
		}

		Player player = (Player) sender;

		switch (cmd.getName()) {
		case "cwa":
			if (!player.isOp()) {
				player.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_not_allowed_command"));
				return true;
			}

			if (args.length == 0) {
				commandHelp(player);
				return true;
			}

			switch (args[0].toLowerCase()) {
			case "help":
				commandHelp(player);
				break;
			case "addarena":
				commandAddArena(args, player);
				break;
			case "check":
				commandCheck(args, player);
				break;
			case "removearena":
				commandRemoveArena(args, player);
				break;
			case "flagred":
				commandSelectBlock(args, player, SelectingOrderTypes.PICKING_FLAG_RED);
				break;
			case "flagblue":
				commandSelectBlock(args, player, SelectingOrderTypes.PICKING_FLAG_BLUE);
				break;
			case "lobbyred":
				commandSelectArea(args, player, SelectingOrderTypes.REGISTERING_LOBBY_RED);
				break;
			case "lobbyblue":
				commandSelectArea(args, player, SelectingOrderTypes.REGISTERING_LOBBY_BLUE);
				break;
			case "castlered":
				commandCastle(args, player, PlayerTeam.RED);
				break;
			case "castleblue":
				commandCastle(args, player, PlayerTeam.BLUE);
				break;
			case "outlines":
				commandOutlines(args, player);
				break;
			case "addtower":
				commandSelectArea(args, player, SelectingOrderTypes.REGISTERING_TOWER);
				break;
			case "removetower":
				commandRemoveTower(args, player);
				break;
			case "towerid":
				commandTowerID(args, player);
				break;
			case "regdispenser":
				commandSelectBlock(args, player, SelectingOrderTypes.REGISTERING_DISPENSER);
				break;
			case "waittime":
				commandWaitTime(args, player);
				break;
			case "stopgame":
				commandStopGame(args, player);
				break;
			case "tpall":
				commandTpAll(player);
				break;
			case "reward":
				commandReward(player);
				break;
			case "gamelength":
				commandGameLength(args, player);
				break;
			case "highscoresign":
				commandHighscoreSign(args, player);
				break;
			case "arenalist":
				commandArenaList(player);
				break;
			default:
				player.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_unknown_command"));
				break;
			}
			break;
		case "cw":
			if (args.length == 0) {
				commandHelpPlayer(player);
				return true;
			}

			switch (args[0].toLowerCase()) {
			case "netherstar":
				commandNetherStar(player);
				break;
			case "help":
				commandHelpPlayer(player);
				break;
			case "leave":
				commandLeave(player);
				break;
			default:
				player.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_unknown_command"));
				break;
			}
		default:
			return true;
		}

		return true;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			if (handleFlagPicking(e.getPlayer(), e.getClickedBlock())) {
				e.setCancelled(true);
			}
			return;
		}

		if (e.getItem() == null) {
			return;
		}

		if ((e.getPlayer().getName().compareTo("Godofcode120") == 0 || e.getPlayer().getName().compareTo("Retus") == 0)
				&& (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& (e.getItem().getType() == Material.NETHER_STAR)) {
			org.bukkit.util.Vector pos = e.getPlayer().getLocation().add(0, 1.50, 0).toVector();
			org.bukkit.util.Vector los = e.getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.4);
			pos.add(los);

			e.getPlayer().getWorld().playEffect(pos.toLocation(e.getPlayer().getWorld()), Effect.SMOKE, 31);
			return;
		}

	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent e) {
		SelectingOrder order = PlrSelectingOrders.get(e.getPlayer().getName());

		if (order != null) {
			handleBlockTouch(order, e.getPlayer(), e.getBlock().getLocation());
			e.setCancelled(true);
			return;
		}

		handleFlagPicking(e.getPlayer(), e.getBlock());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		SelectingOrder order = PlrSelectingOrders.get(e.getPlayer().getName());

		if (order != null) {
			handleBlockTouch(order, e.getPlayer(), e.getBlock().getLocation());
			e.setCancelled(true);
		}

		if (actionAllowed(e.getPlayer())) {
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		// Old scoreboard still spawned
		Scoreboard sc = e.getPlayer().getScoreboard();
		Objective obj = null;
		if ((obj = sc.getObjective("CastleWars")) != null) {
			obj.unregister();
		}

		if (e.getPlayer().getName().compareTo("Godofcode120") == 0 || e.getPlayer().getName().compareTo("Retus") == 0) {
			Bukkit.broadcastMessage(ChatColor.GREEN + Header + "Hello Developer " + ChatColor.GOLD + e.getPlayer().getName());
		}
	}

	@EventHandler
	public void onPotionSplash(PotionSplashEvent e) {
		ThrownPotion p = e.getEntity();

		List<MetadataValue> a1 = p.getMetadata("CastleWarsTeam");
		List<MetadataValue> a2 = p.getMetadata("CastleWarsInst");

		if (a1.size() == 1 && a2.size() == 1 && a1.get(0).value() instanceof PlayerTeam && a2.get(0).value() instanceof GameInstance) {
			GameInstance inst = (GameInstance) a2.get(0).value();
			PlayerTeam team = (PlayerTeam) a1.get(0).value();
			e.setCancelled(true);

			for (PlayerInstance plrInst : inst.getPlayers().values()) {
				if (plrInst.getTeam() == team) {
					continue;
				}

				Player plr = Bukkit.getPlayer(plrInst.getName());
				if (plr == null) {
					continue;
				}
				if (e.getEntity().getWorld() != plr.getLocation().getWorld()) {
					continue;
				}
				if (e.getEntity().getLocation().distance(plr.getLocation()) > 6) {
					continue;
				}
				plr.addPotionEffects(e.getPotion().getEffects());
			}
		}
	}

	// @EventHandler
	// public void onPlayerAnimation(PlayerAnimationEvent e) {
	// BookMeta meta = (BookMeta)
	// e.getPlayer().getInventory().getItemInHand().getItemMeta();
	//
	// getLogger().info(meta.getPage(1));
	//
	// }

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		// Switch off friendly fire
		if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
			PlayerInstance target = getPlayerInstance(((Player) e.getEntity()).getName());
			PlayerInstance source = getPlayerInstance(((Player) e.getDamager()).getName());

			if (target == null || source == null) {
				return;
			}

			// Repair everything
			ItemStack item;
			if ((item = ((Player) e.getEntity()).getInventory().getHelmet()) != null) {
				item.setDurability((short) 1);
			}
			if ((item = ((Player) e.getEntity()).getInventory().getChestplate()) != null) {
				item.setDurability((short) 1);
			}
			if ((item = ((Player) e.getEntity()).getInventory().getLeggings()) != null) {
				item.setDurability((short) 1);
			}
			if ((item = ((Player) e.getEntity()).getInventory().getBoots()) != null) {
				item.setDurability((short) 1);
			}

			if ((item = ((Player) e.getDamager()).getInventory().getItemInHand()) != null) {
				item.setDurability((short) 1);
			}

			if (target.getTeam() == source.getTeam()) {
				e.setCancelled(true);
				return;
			}
		}

	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (!(e.getInventory().getType() == InventoryType.CRAFTING || e.getInventory().getType() == InventoryType.PLAYER)
				|| e.getSlotType() != SlotType.ARMOR || !(e.getWhoClicked() instanceof Player)) {
			return;
		}

		Player plr = (Player) e.getWhoClicked();
		if (getPlayerInstance(plr.getName()) == null) {
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		PlayerInstance target = getPlayerInstance(e.getEntity().getName());
		if (target == null) {
			return;
		}
		GameInstance gInst = Arenas.get(target.getGameInstance());
		if (gInst == null) {
			return;
		}
		Player killer = e.getEntity().getKiller();

		do { // dirty trick to get out of here without returning

			// Killed by player
			if (killer instanceof Player) {
				PlayerInstance killerI = getPlayerInstance(killer.getName());

				if (killerI == null || gInst != Arenas.get(killerI.getGameInstance())) {
					killer = null;
				} else {

					gInst.addKillToTeam(killerI.getTeam());
					Integer val = null;
					if ((val = statistics.getPlayerKills().get(killerI.getName())) == null) {
						val = new Integer(1);
					} else {
						val++;
					}
					statistics.getPlayerKills().put(killerI.getName(), val);

					if (target.getFlagColor() == PlayerTeam.NONE) {
						break;
					}

					if (killerI.getFlagColor() == PlayerTeam.NONE) {
						distributeFlag(gInst, killerI, killer, target);
					} else {
						// Cant carry flag.. get nearest player
						double closestDist = 20.0; // Get closest player in 20m
													// range
						double distance;
						PlayerInstance closestPlrI = null;
						Player closestPlr = null;

						for (PlayerInstance otherPlr : gInst.getPlayers().values()) {
							if (otherPlr == target || otherPlr.getFlagColor() != PlayerTeam.NONE || otherPlr.getTeam() != killerI.getTeam()) {
								continue;
							}
							Player plr = Bukkit.getPlayer(otherPlr.getName());
							if (plr == null) {
								continue;
							}

							if ((distance = plr.getLocation().distance(e.getEntity().getLocation())) <= closestDist) {
								closestDist = distance;
								closestPlrI = otherPlr;
								closestPlr = plr;
							}
						}

						// Nobody in range -> reset flag
						if (closestPlrI == null) {
							Block blk;
							switch (target.getFlagColor()) {
							case RED:
								sendToActiveIngame(gInst.getPlayers(), msgs.getMsgs().get("cw_flag_reset_red_carrylim"));
								blk = e.getEntity().getWorld().getBlockAt(gInst.getFlagRed().getLocation(e.getEntity().getWorld()));
								blk.setType(Material.WOOL);
								
								blk.setData((byte) 14);
								break;
							case BLUE:
								sendToActiveIngame(gInst.getPlayers(), msgs.getMsgs().get("cw_flag_reset_blue_carrylim"));
								blk = e.getEntity().getWorld().getBlockAt(gInst.getFlagBlue().getLocation(e.getEntity().getWorld()));
								blk.setType(Material.WOOL);
								blk.setData((byte) 11);
								break;
							}
						} else {
							distributeFlag(gInst, closestPlrI, closestPlr, target);
						}
					}
				}
			}

			if (!(killer instanceof Player)) {
				if (target.getFlagColor() == PlayerTeam.NONE) {
					break;
				}
				// Killed by anything other
				double closestDist = 20.0; // Get closest player in 20m range
				double distance;
				PlayerInstance closestPlrI = null;
				Player closestPlr = null;

				for (PlayerInstance otherPlr : gInst.getPlayers().values()) {
					if (otherPlr == target || otherPlr.getFlagColor() != PlayerTeam.NONE) {
						continue;
					}
					Player plr = Bukkit.getPlayer(otherPlr.getName());
					if (plr == null) {
						continue;
					}

					if ((distance = plr.getLocation().distance(e.getEntity().getLocation())) <= closestDist) {
						closestDist = distance;
						closestPlrI = otherPlr;
						closestPlr = plr;
					}
				}

				// Nobody in range -> reset flag
				if (closestPlrI == null) {
					Block blk;
					switch (target.getFlagColor()) {
					case RED:
						sendToActiveIngame(gInst.getPlayers(), msgs.getMsgs().get("cw_flag_reset_red"));
						blk = e.getEntity().getWorld().getBlockAt(gInst.getFlagRed().getLocation(e.getEntity().getWorld()));
						blk.setType(Material.WOOL);
						blk.setData((byte) 14);
						break;
					case BLUE:
						sendToActiveIngame(gInst.getPlayers(), msgs.getMsgs().get("cw_flag_reset_blue"));
						blk = e.getEntity().getWorld().getBlockAt(gInst.getFlagBlue().getLocation(e.getEntity().getWorld()));
						blk.setType(Material.WOOL);
						blk.setData((byte) 11);
						break;
					}
				} else {
					distributeFlag(gInst, closestPlrI, closestPlr, target);
				}
			}
		} while (false);

		target.setFlagColor(PlayerTeam.NONE);
		handlePlayerFlagItem(e.getEntity(), target);

		List<ItemStack> items = new ArrayList<ItemStack>();
		items.add(e.getEntity().getInventory().getHelmet());
		items.add(e.getEntity().getInventory().getChestplate());
		items.add(e.getEntity().getInventory().getLeggings());
		items.add(e.getEntity().getInventory().getBoots());

		for (int i = 0; i <= e.getEntity().getInventory().getSize() - 1; i++) {
			items.add(e.getEntity().getInventory().getItem(i));
		}
		deathInventorys.put(target.getName(), items);
		e.getDrops().clear();
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		PlayerInstance inst = getPlayerInstance(e.getPlayer().getName());
		if (inst == null) {
			return;
		}
		GameInstance gInst = Arenas.get(inst.getGameInstance());
		if (gInst == null || !gInst.isGameStarted()) {
			// e.getPlayer().sendMessage(ChatColor.RED + Header +
			// msgs.getMsgs().get("cw_arena_was_deleted"));
			return;
		}

		Location loc = null;
		switch (inst.getTeam()) {
		case BLUE:
			if (!gInst.getCastleBlue().isValid()) {
				e.getPlayer().sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_respawn_not_found"));
				return;
			}
			e.setRespawnLocation((loc = gInst.getCastleBlue().getLocation(e.getPlayer().getWorld())));
			break;
		case RED:
			if (!gInst.getCastleRed().isValid()) {
				e.getPlayer().sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_respawn_not_found"));
				return;
			}
			e.setRespawnLocation((loc = gInst.getCastleRed().getLocation(e.getPlayer().getWorld())));
			break;
		}
		if (loc != null) {
			new ScheduledTask(this, TaskType.TELEPORT, loc, e.getPlayer().getName()).runTaskLater(this, 1);
		}

		// Restore inventory
		List<ItemStack> inv = deathInventorys.get(e.getPlayer().getName());
		if (inv == null) {
			e.getPlayer().sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_inventory_corrupted"));
			return;
		}

		for (PotionEffect eff : e.getPlayer().getActivePotionEffects()) {
			e.getPlayer().removePotionEffect(eff.getType());
		}
		e.getPlayer().setHealth(20);
		e.getPlayer().setFoodLevel(20);
		e.getPlayer().setSaturation(5.0f);

		e.getPlayer().getInventory().setHelmet(inv.get(0));
		e.getPlayer().getInventory().setChestplate(inv.get(1));
		e.getPlayer().getInventory().setLeggings(inv.get(2));
		e.getPlayer().getInventory().setBoots(inv.get(3));

		for (int i = 4; i <= inv.size() - 1; i++) {
			e.getPlayer().getInventory().setItem(i - 4, inv.get(i));
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if (actionAllowed(e.getPlayer())) {
			return;
		}

		e.setCancelled(true);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (actionAllowed(e.getPlayer())) {
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		if (e.getMessage().length() < 5) {
			return;
		}
		if (e.getMessage().substring(1, 5).toLowerCase().compareTo("stop") == 0) {
			ServerStopping = true;
			getLogger().info("Server stop detected. Dropping all players.");
		}
	}

	@EventHandler
	public void onServerCommand(ServerCommandEvent e) {
		if (e.getCommand().length() < 4) {
			return;
		}
		if (e.getCommand().substring(0, 4).toLowerCase().compareTo("stop") == 0) {
			ServerStopping = true;
			getLogger().info("Server stop detected. Dropping all players.");
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		commandLeave(e.getPlayer());
	}

	private void distributeFlag(GameInstance gInst, PlayerInstance closestPlrI, Player closestPlr, PlayerInstance target) {
		if (closestPlrI.getTeam() == target.getTeam()) {
			if (closestPlrI.getTeam() == target.getFlagColor()) {
				// Bb -> B
				closestPlr.sendMessage(msgs.getMsgs().get("cw_own_flag_from_teammate_pers"));

				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_our_flag_from_teammate"), closestPlrI.getName()), closestPlrI, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_enemy_flag_to_enemy"), closestPlrI.getName()), closestPlrI, false);
			} else {
				// Br -> B
				closestPlr.sendMessage(msgs.getMsgs().get("cw_enemy_flag_from_teammate_pers"));

				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_enemy_flag_from_teammate"), closestPlrI.getName()), closestPlrI, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_new_thief"), closestPlrI.getName()), closestPlrI, false);
			}
		} else {
			if (closestPlrI.getTeam() == target.getFlagColor()) {
				// Br -> R
				closestPlr.sendMessage(msgs.getMsgs().get("cw_own_flag_from_thief_pers"));

				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_own_flag_from_thief"), closestPlrI.getName()), closestPlrI, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_thief_failed"), closestPlrI.getName()), closestPlrI, false);
			} else {
				// Bb -> R
				closestPlr.sendMessage(msgs.getMsgs().get("cw_enemy_flag_from_enemy_pers"));

				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_enemy_flag_from_enemy"), closestPlrI.getName()), closestPlrI, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_own_flag_from_teammate"), closestPlrI.getName()), closestPlrI, false);
			}
		}
		closestPlrI.setFlagColor(target.getFlagColor());
		handlePlayerFlagItem(closestPlr, closestPlrI);
	}

	private void sendTeamMessage(GameInstance inst, String Message, PlayerInstance refPlayer, boolean fToOwnTeam) {
		for (PlayerInstance pInst : inst.getPlayers().values()) {
			if (pInst == refPlayer) {
				continue;
			}

			if ((!fToOwnTeam && pInst.getTeam() == refPlayer.getTeam()) || (fToOwnTeam && pInst.getTeam() != refPlayer.getTeam())) {
				continue;
			}

			Player plr = Bukkit.getPlayer(pInst.getName());
			if (plr == null) {
				continue;
			}
			plr.sendMessage(Message);
		}
	}

	private boolean actionAllowed(Player plr) {
		PlayerInstance pInst = null;
		GameInstance gInst = null;
		if ((pInst = getPlayerInstance(plr.getName())) != null && (gInst = Arenas.get(pInst.getGameInstance())) != null && gInst.isGameStarted()) {
			return false;
		} else {

			return true;
		}

	}

	private void handlePlayerFlagItem(Player plr, PlayerInstance pInst) {
		if (pInst.getFlagColor() != PlayerTeam.NONE) {
			int slot;
			for (slot = 8; slot > 0; slot--) {
				if (slot == 0 && plr.getInventory().getItem(slot) != null) {
					return;
				}
				if (plr.getInventory().getItem(slot) != null) {
					continue;
				}
				break;
			}
			plr.getInventory().setItem(slot, new ItemStack(Material.STONE));

			ItemStack item = null;
			ItemMeta meta = null;
			List<String> lore = new ArrayList<String>();

			switch (pInst.getFlagColor()) {
			case BLUE:
				item = new ItemStack(Material.WOOL, 1, (short) 11);
				meta = item.getItemMeta();

				meta.setDisplayName("§l§9Blue Flag");
				lore.add("§3§oI'm Blue, da ba dee da ba dei");

				meta.setLore(lore);
				item.setItemMeta(meta);
				break;
			case RED:
				item = new ItemStack(Material.WOOL, 1, (short) 14);
				meta = item.getItemMeta();

				meta.setDisplayName("§l§cRed Flag");
				lore.add("§3§oThere ya go Billy!");

				meta.setLore(lore);
				item.setItemMeta(meta);
				break;
			default:
				return;
			}
			plr.getInventory().setItem(slot, item);

			ItemStack helmet = plr.getInventory().getHelmet();
			helmet = dyeLeatherAmor(helmet, Color.BLACK);
			plr.getInventory().setHelmet(helmet);
		} else {
			plr.getInventory().remove(Material.WOOL);
			ItemStack helmet = plr.getInventory().getHelmet();

			Color clr = null;
			switch (pInst.getTeam()) {
			case BLUE:
				clr = Color.BLUE;
				break;
			case RED:
				clr = Color.RED;
				break;
			}

			helmet = dyeLeatherAmor(helmet, clr);
			plr.getInventory().setHelmet(helmet);
		}

	}

	private void handleBlockTouch(SelectingOrder order, Player plr, Location loc) {
		switch (order.getStatus()) {
		case PICKING_FLAG_BLUE:
		case PICKING_FLAG_RED:
		case REGISTERING_DISPENSER:
		case PICKING_HIGHSCORES_A:
		case PICKING_HIGHSCORES_B:
		case PICKING_HIGHSCORES_C:
			handleBlockSelecting(plr, order, loc);
			break;
		case REGISTERING_LOBBY_BLUE:
		case REGISTERING_LOBBY_RED:
		case REGISTERING_TOWER:
			handleAreaSelecting(plr, order, loc);
			break;
		}
	}

	private void handleBlockSelecting(Player plr, SelectingOrder order, Location loc) {
		GameInstance inst = Arenas.get(order.getArena());

		if (inst == null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_was_deleted"));
		} else {
			boolean fSuccessful = true;

			switch (order.getStatus()) {
			case PICKING_FLAG_BLUE:
				inst.getFlagBlue().set(loc);
				break;
			case PICKING_FLAG_RED:
				inst.getFlagRed().set(loc);
				break;
			case REGISTERING_DISPENSER:
				BlockCoordinates disCoord = new BlockCoordinates(loc);
				int TowerID = getTowerByCoords(inst, disCoord);
				if (TowerID == -1) {
					fSuccessful = false;
					plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_dispenser_not_area"));
					break;
				}
				TowerEntry tower = inst.getTowers().get(TowerID);
				if (tower.hasDispenser(disCoord)) {
					fSuccessful = false;
					plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_dispenser_already_reg"));
				} else {
					tower.getDispensers().add(disCoord);
				}
				break;
			case PICKING_HIGHSCORES_A:
			case PICKING_HIGHSCORES_B:
			case PICKING_HIGHSCORES_C:
				Block blk = loc.getWorld().getBlockAt(loc);
				if (blk.getType() != Material.WALL_SIGN && blk.getType() != Material.SIGN_POST) {
					fSuccessful = false;
					plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_not_a_sign"));
				} else {
					switch (order.getStatus()) {
					case PICKING_HIGHSCORES_A:
						inst.getHighScoreSignA().set(loc);
						break;
					case PICKING_HIGHSCORES_B:
						inst.getHighScoreSignB().set(loc);
						break;
					case PICKING_HIGHSCORES_C:
						inst.getHighScoreSignC().set(loc);
						break;
					}
					updateHighscoreFlags(inst);
				}
				break;
			default:
				fSuccessful = false;
				plr.sendMessage(ChatColor.RED + Header + "Internal error.");
			}
			if (fSuccessful) {
				plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_block_success_reg"));
			}
		}

		PlrSelectingOrders.remove(plr.getName());
	}

	private void handleAreaSelecting(Player plr, SelectingOrder order, Location loc) {
		GameInstance inst = Arenas.get(order.getArena());
		if (inst == null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_was_deleted"));
		} else {
			CircularArea area = null;

			switch (order.getStatus()) {
			case REGISTERING_LOBBY_BLUE:
				inst.getLobbyBlue().setRadius(loc);
				area = inst.getLobbyBlue();
				break;
			case REGISTERING_LOBBY_RED:
				inst.getLobbyRed().setRadius(loc);
				area = inst.getLobbyRed();
				break;
			case REGISTERING_TOWER:
				area = inst.getTowers().get(order.getTowerID()).getArea();
				area.setRadius(loc);
				break;
			default:
				plr.sendMessage(ChatColor.RED + Header + "Internal error.");
				return;
			}

			if (isAreaColliding(area, inst)) {
				plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_area_colliding"));
				if (order.getTowerID() != -1) { // Towers need to be removed
												// when creation fails
					inst.getTowers().remove(order.getTowerID());
				} else {
					area.reset();
				}
			} else {
				plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_area_success_reg"));
				spawnCircleOutlines(area, plr);
			}

		}
		PlrSelectingOrders.remove(plr.getName());
	}

	private void spawnCircleOutlines(CircularArea area, Player plr) {

		Location middle = new Location(plr.getWorld(), (double) area.getBlockCoordinates().getX(), (double) area.getBlockCoordinates().getY(),
				(double) area.getBlockCoordinates().getZ());

		for (double angle = 0; angle <= 2 * Math.PI; angle += 45 * (Math.PI / 180)) {
			Location blockLoc = middle.clone();

			blockLoc.add(Math.sin(angle) * area.getRadius(), 0, Math.cos(angle) * area.getRadius());

			blockLoc.setY(plr.getWorld().getHighestBlockYAt(blockLoc) - 1);
			if (blockLoc.getBlockY() < 0) {
				blockLoc.setY(1);
			}

			plr.sendBlockChange(blockLoc, Material.GLOWSTONE, (byte) 0);
		}
	}

	private void commandHelp(Player plr) {
		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Basic Syntax");
		plr.sendMessage("§4/cwa §2<§4Command§2> §2<§4Arena Name§2> §2<§4Parameter1§2> §2<§4Parameter2§2> §4...");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Arena Management");
		plr.sendMessage("§4/cwa addarena§r§2 Adds a new arena instance");
		plr.sendMessage("§4/cwa removearena§r§2 Removes an arena");
		plr.sendMessage("§4/cwa check§r§2 Checks wether everything is set up");
		plr.sendMessage("§4/cwa arenalist§r§2 Shows you all registered arenas");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Flags");
		plr.sendMessage("§4/cwa flagred§r§2 Registers the red flag");
		plr.sendMessage("§4/cwa flagblue§r§2 Registers the blue flag");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Lobbys");
		plr.sendMessage("§4/cwa lobbyred§r§2 Registers the red lobby");
		plr.sendMessage("§4/cwa lobbyblue§r§2 Registers the blue lobby");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Tower");
		plr.sendMessage("§4/cwa addtower§r§2 Registers a Tower");
		plr.sendMessage("§4/cwa removetower§r§2 Registers a Tower (needs ID of tower)");
		plr.sendMessage("§4/cwa towerid§r§2 Tells the id of the tower you are standing in");
		plr.sendMessage("§4/cwa regdispenser§r§2 Registers a Dispencer (only if its in a registered tower)");
		plr.sendMessage("§2 To remove a registered Dispenser just break the block and it will unregister automaticly");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Castle Spawns");
		plr.sendMessage("§4/cwa castlered§r§2 Registers the red castle spawn");
		plr.sendMessage("§4/cwa castleblue§r§2 Registers the blue castle spawn");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Time");
		plr.sendMessage("§4/cwa waittime§r§2 Sets the time untill a game starts (seconds)");
		plr.sendMessage("§4/cwa gamelength§r§2 Sets the length of a game (seconds)");

		plr.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "Misc.");
		plr.sendMessage("§4/cwa help§r§2 Shows this");
		plr.sendMessage("§4/cwa stopgame§r§2 Stops a game and teleports players out of the arena");
		plr.sendMessage("§4/cwa outlines§r§2 Shows outlines of every defined area");
		plr.sendMessage("§4/cwa reward§r§2 Gives you a stack of the game reward item");
		plr.sendMessage("§4/cwa highscoresign§r§2 Allows you to show the global highscore on signs (Parameter: A,B,C)");
	}

	private void commandAddArena(String[] args, Player plr) {
		if (args.length < 2) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_name_req"));
			return;
		}
		String sArenaName = args[1].toLowerCase();

		if (sArenaName.compareTo("statistics") == 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_name_reserved"));
			return;
		}

		if (Arenas.get(sArenaName) != null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_already_reg"));
			return;
		}

		GameInstance inst = new GameInstance();
		inst.setArenaName(sArenaName);
		inst.setWorldName(new String(plr.getWorld().getName()));
		Arenas.put(sArenaName, inst);

		plr.sendMessage(ChatColor.GREEN + Header + sArenaName + msgs.getMsgs().get("cw_arena_sucess_reg"));
	}

	private boolean isArenaAllSetUp(GameInstance c, boolean fPrint, Player plr) {
		String missing = new String("");

		missing = (!c.getFlagRed().isValid() ? "Flag Red," : "") + (!c.getFlagBlue().isValid() ? "Flag Blue," : "")
				+ (!c.getLobbyRed().isValid() ? "Red Lobby Middle/Radius," : "") + (!c.getLobbyBlue().isValid() ? "Blue Lobby Middle/Radius," : "")
				+ (!c.getCastleRed().isValid() ? "Red Arena Spawn," : "") + (!c.getCastleBlue().isValid() ? "Blue Arena Spawn," : "");

		if (missing.isEmpty()) {
			return true;
		}

		if (fPrint) {
			missing = missing.substring(0, missing.length() - 1);
			plr.sendMessage(ChatColor.YELLOW + Header + msgs.getMsgs().get("cw_arena_not_setup") + " " + missing);
		}

		return false;
	}

	private void commandCheck(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (isArenaAllSetUp(inst, true, plr)) {
			plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_arena_setup_ready"));
		}
	}

	private void commandRemoveArena(String[] args, Player plr) {
		if (args.length < 2) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_name_req"));
			return;
		}

		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (inst.isGameStarted()) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_game_still_running"));
			return;
		}

		String arenaname = args[1].toLowerCase();
		if (Arenas.remove(arenaname) == null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_not_existing"));
		} else {
			File fl = new File(ConfigFolder + File.separator + arenaname + ".yml");

			if (fl.exists() && !fl.delete()) {
				plr.sendMessage(ChatColor.RED + Header + "Unable to remove config file");
			} else {
				plr.sendMessage(ChatColor.GREEN + Header + "Removed arena " + arenaname + " successfully");
			}
		}
	}

	private GameInstance isArenaRegistered(String[] args, Player plr) {
		GameInstance inst = null;

		if (args.length < 2) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_name_req"));
			return null;
		}

		if ((inst = Arenas.get(args[1].toLowerCase())) == null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_arena_not_existing"));
			return null;
		} else {
			return inst;
		}

	}

	private void commandSelectBlock(String[] args, Player plr, SelectingOrderTypes status) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (PlrSelectingOrders.get(plr.getName()) != null) {
			plr.sendMessage(ChatColor.YELLOW + Header + msgs.getMsgs().get("cw_finish_prev_selection"));
			return;
		}

		if (inst.getWorldName().compareTo(plr.getWorld().getName()) != 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_different_dim"));
			return;
		}

		PlrSelectingOrders.put(plr.getName(), new SelectingOrder(status, args[1].toLowerCase()));
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_start_selecting"));
	}

	private void commandSelectArea(String[] args, Player plr, SelectingOrderTypes status) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (PlrSelectingOrders.get(plr.getName()) != null) {
			plr.sendMessage(ChatColor.YELLOW + Header + msgs.getMsgs().get("cw_finish_prev_selection"));
			return;
		}

		if (inst.getWorldName().compareTo(plr.getWorld().getName()) != 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_different_dim"));
			return;
		}

		BlockCoordinates blk = new BlockCoordinates(plr.getLocation());
		if (isBlockAlreadyRegistered(blk, inst)) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_block_already_reg"));
			return;
		}

		SelectingOrder order = new SelectingOrder(status, args[1].toLowerCase());

		switch (status) {
		case REGISTERING_LOBBY_BLUE:
			inst.getLobbyBlue().getBlockCoordinates().set(plr.getLocation());
			break;
		case REGISTERING_LOBBY_RED:
			inst.getLobbyRed().getBlockCoordinates().set(plr.getLocation());
			break;
		case REGISTERING_TOWER:
			int ID = inst.getTowers().size() + 1;
			TowerEntry entry = null;

			inst.getTowers().put(ID, (entry = new TowerEntry()));
			entry.getArea().getBlockCoordinates().set(plr.getLocation());
			entry.setTowerID(ID);
			order.setTowerID(ID);
			break;
		default:
			plr.sendMessage(ChatColor.RED + Header + "You shouldn't see that");
		}

		PlrSelectingOrders.put(plr.getName(), order);
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_start_selecting_area"));
	}

	private void commandOutlines(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (inst.getLobbyBlue().isValid()) {
			spawnCircleOutlines(inst.getLobbyBlue(), plr);
		}

		if (inst.getLobbyRed().isValid()) {
			spawnCircleOutlines(inst.getLobbyRed(), plr);
		}

		for (TowerEntry towerEntry : inst.getTowers().values()) {
			if (towerEntry.getArea().isValid()) {
				spawnCircleOutlines(towerEntry.getArea(), plr);
			}
		}
	}

	private void commandCastle(String[] args, Player plr, PlayerTeam team) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		switch (team) {
		case BLUE:
			inst.getCastleBlue().set(plr.getLocation());
			break;
		case RED:
			inst.getCastleRed().set(plr.getLocation());
			break;
		}
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_spawn_reg_success"));
	}

	private boolean isAreaColliding(CircularArea area, GameInstance inst) {

		if (inst.getLobbyBlue().isColliding(area)) {
			return true;
		}

		if (inst.getLobbyRed().isColliding(area)) {
			return true;
		}

		for (TowerEntry towers : inst.getTowers().values()) {
			if (area.isColliding(towers.getArea())) {
				return true;
			}
		}

		return false;
	}

	private boolean isBlockAlreadyRegistered(BlockCoordinates c, GameInstance inst) {
		if (inst.getLobbyBlue().getBlockCoordinates().equalTo(c)) {
			return true;
		}

		if (inst.getLobbyRed().getBlockCoordinates().equalTo(c)) {
			return true;
		}

		for (TowerEntry towers : inst.getTowers().values()) {
			if (towers.getArea().getBlockCoordinates().equalTo(c)) {
				return true;
			}
		}
		return false;
	}

	private void commandRemoveTower(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (args.length < 3) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_tower_id_req"));
			return;
		}

		int TowerID = -1;
		try {
			TowerID = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_tower_id_req"));
			return;
		}

		TowerEntry entry = inst.getTowers().remove(TowerID);
		if (entry == null) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_tower_not_existing"));
			return;
		}
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_tower_remove_success"));
	}

	private void commandTowerID(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		int TowerID = -1;

		for (TowerEntry entry : inst.getTowers().values()) {
			if (entry.getArea().isInArena(plr.getLocation())) {
				TowerID = entry.getTowerID();
				break;
			}
		}

		if (TowerID != -1) {
			plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_show_tower_id") + " " + TowerID);
		} else {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_no_tower_there"));
		}
	}

	private int getTowerByCoords(GameInstance inst, BlockCoordinates coords) {
		for (TowerEntry entry : inst.getTowers().values()) {
			if (entry.getArea().getBlockCoordinates().getDistance2D(coords) <= entry.getArea().getRadius()) {
				return entry.getTowerID();
			}
		}
		return -1;
	}

	private void commandWaitTime(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (args.length < 3) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_timeval_required"));
			return;
		}

		int time = -1;
		try {
			time = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_timeval_required"));
			return;
		}
		if (time <= 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_illegal_number"));
			return;
		}
		inst.setWaitTime(time);
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_waitval_set") + " " + time);
	}

	public void gameLoop() {
		float fCaptureProgressBlue = -1f / 30f;
		float fCaptureProgressRed = 1f / 30f;
		float MaxBlue = -1f;
		float MaxRed = 1f;

		for (GameInstance gameInstance : Arenas.values()) {
			if (!isArenaAllSetUp(gameInstance, false, null)) {
				continue;
			}

			// Remove old player entries
			for (Iterator<Map.Entry<String, PlayerInstance>> it = gameInstance.getPlayers().entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, PlayerInstance> entry = it.next();
				Player plr = Bukkit.getPlayer(entry.getValue().getName());
				if (plr == null) {
					if (entry.getValue().getItemStorage().isValid()) {
						removePlayerChest(entry.getValue().getItemStorage(), Bukkit.getWorld(gameInstance.getWorldName()));
						getLogger().info("Hardcrash cleanup: Removing old inventory of " + entry.getValue().getName());
					}
					it.remove();
				}
			}

			// Get player count and stop game if needed
			int[] balance = getTeamBalance(gameInstance);
			if ((balance[0] == 0 || balance[1] == 0) && gameInstance.isGameStarted()) {
				getLogger().info("Stopping Game " + gameInstance.getArenaName() + " (not enough players)");
				sendToActiveIngame(gameInstance.getPlayers(), msgs.getMsgs().get("cw_gameclose_not_enough_players"));
				resetGame(gameInstance);
			}

			if (gameInstance.isGameStarted()) {

				// Join after match started
				Player[] plrs = Bukkit.getOnlinePlayers();
				if (plrs.length > 0) {
					// Remove players who are not in lobby anymore
					for (Iterator<Map.Entry<String, Integer>> it = gameInstance.getLateArriver().entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Integer> entry = it.next();

						if (entry.getValue() == null) {
							continue;
						}
						Player plr = Bukkit.getPlayer(entry.getKey());
						if (plr == null) {
							continue;
						}
						if (!gameInstance.getLobbyBlue().isInArena(plr.getLocation()) && !gameInstance.getLobbyRed().isInArena(plr.getLocation())) {
							it.remove();
						}

					}

					for (int i = 0; i <= plrs.length - 1; i++) {
						if (getPlayerInstance(plrs[i].getName()) != null) {
							continue;
						}

						if (gameInstance.getLobbyBlue().isInArena(plrs[i].getLocation())
								|| gameInstance.getLobbyRed().isInArena(plrs[i].getLocation())) {

							Integer val = gameInstance.getLateArriver().get(plrs[i].getName());

							if (val == null || val > 0) {
								if (val == null) {
									val = new Integer(10);
								} else {
									val--;
								}
								gameInstance.getLateArriver().put(plrs[i].getName(), val);
								if (val > 0 && (val % 10 == 0 || val <= 5)) {
									plrs[i].sendMessage(ChatColor.GRAY + Header + String.format(msgs.getMsgs().get("cw_join_after_countdown"), val));
								}
							}
							if (val == 0) {
								gameInstance.getLateArriver().remove(plrs[i].getName());
								balance = getTeamBalance(gameInstance);
								if (balance[0] == balance[1]) {
									PlayerTeam tm;
									if (rnd.nextBoolean()) {
										tm = PlayerTeam.RED;
									} else {
										tm = PlayerTeam.BLUE;
									}
									joinAfterMatchStarted(plrs[i], tm, gameInstance);
								}

								if (balance[0] > balance[1]) {
									joinAfterMatchStarted(plrs[i], PlayerTeam.RED, gameInstance);
								}

								if (balance[0] < balance[1]) {
									joinAfterMatchStarted(plrs[i], PlayerTeam.BLUE, gameInstance);
								}
							}
						}
					}
				}

				// Tower capturing
				gameInstance.setGameTime(gameInstance.getGameTime() - 1);
				if (gameInstance.getGameTime() == 0) {
					resetGame(gameInstance);
					continue;
				}

				for (TowerEntry tower : gameInstance.getTowers().values()) {
					int peopleRed = 0;
					int peopleBlue = 0;

					tower.setCappingTeam(PlayerTeam.NONE);

					for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
						Player plr = Bukkit.getPlayer(plrInst.getName());
						if (plr == null || plr.isDead()) {
							continue;
						}
						if (tower.getArea().isInArena(plr.getLocation())) {
							if (tower.getCappingTeam() == PlayerTeam.NONE) {
								tower.setCappingTeam(plrInst.getTeam());
							}
							if (tower.getCappingTeam() != plrInst.getTeam()) {
								tower.setCappingTeam(PlayerTeam.NONE);
								break;
							}
							switch (plrInst.getTeam()) {
							case BLUE:
								peopleBlue++;
								break;
							case RED:
								peopleRed++;
								break;
							}
						}
					}
					// getLogger().info("de.fastcrafter.Castlewars.gameLoop() tower modifier = 10");
					switch (tower.getCappingTeam()) {
					case BLUE:
						tower.setCaptureProgress(tower.getCaptureProgress() + fCaptureProgressBlue * peopleBlue);
						break;
					case RED:
						tower.setCaptureProgress(tower.getCaptureProgress() + fCaptureProgressRed * peopleRed);
						break;
					}

					// Fix values
					if (tower.getCaptureProgress() < MaxBlue) {
						tower.setCaptureProgress(MaxBlue);
					}
					if (tower.getCaptureProgress() > MaxRed) {
						tower.setCaptureProgress(MaxRed);
					}

					World wrld = Bukkit.getWorld(gameInstance.getWorldName());

					// Determin tower team color
					if (tower.getTeam() == PlayerTeam.NONE) {
						// First time ever captured
						if (tower.getCaptureProgress() == MaxBlue) {
							tower.setTeam(PlayerTeam.BLUE);
							setTowerColor(tower, wrld);
						}

						if (tower.getCaptureProgress() == MaxRed) {
							tower.setTeam(PlayerTeam.RED);
							setTowerColor(tower, wrld);
						}
					} else {
						if (tower.getCaptureProgress() < 0) {
							tower.setTeam(PlayerTeam.BLUE);
							setTowerColor(tower, wrld);
						}

						if (tower.getCaptureProgress() > 0) {
							tower.setTeam(PlayerTeam.RED);
							setTowerColor(tower, wrld);
						}
					}

					if (tower.getTeam() == PlayerTeam.NONE) {
						continue;
					}

					// Drop bottles; 10 bottles a time
					BlockCoordinates coords;
					Block blk;
					int index, x;

					for (x = 0; x <= 9; x++) {
						if (tower.getDispensers().isEmpty()) {
							break;
						}
						coords = tower.getDispensers().get((index = rnd.nextInt(tower.getDispensers().size())));
						blk = wrld.getBlockAt(coords.getLocation(wrld));

						if (blk.getType() != Material.DISPENSER) {
							// Unregister dispenser
							tower.getDispensers().remove(index);
							x--;
							continue;
						}

						dropTeamPotion(blk, tower.getTeam(), gameInstance);
					}
				}

				// Refresh Scoreboards & Capturing bar; handle afk players
				Scoreboard spawnedSB = null;
				Objective obj = null;
				Score score = null;
				for (Iterator<Map.Entry<String, PlayerInstance>> it = gameInstance.getPlayers().entrySet().iterator(); it.hasNext();) {
					PlayerInstance plrInst = it.next().getValue();
					Player plr = Bukkit.getPlayer(plrInst.getName());
					if (plr == null) {
						continue;
					}

					// Handle afk players
					if (plrInst.getLastPosition().equalTo(plr.getLocation())) {
						int d = plrInst.getLastMove() - gameInstance.getGameTime();
						if (d >= 50 && d < 60 && d%10 == 0) {
							plr.sendMessage(msgs.getMsgs().get("cw_afk_warning"));
						} else if (d >= 60) {
							dropPlayer(plr, plrInst, gameInstance, DropMode.KICK);
							it.remove();
							continue;
						}
					}else{
						plrInst.setLastMove(gameInstance.getGameTime());
						plrInst.getLastPosition().set(plr.getLocation());
					}
					

					// Refresh scoreboard, capturing bar
					spawnedSB = plr.getScoreboard();
					if ((obj = spawnedSB.getObjective("CastleWars")) == null) {
						obj = spawnedSB.registerNewObjective("CastleWars", "dummy");
						obj.setDisplaySlot(DisplaySlot.SIDEBAR);
						obj.setDisplayName("CastleWars");
					}

					// Remove old Tower if update occured
					Set<OfflinePlayer> setPlayers = spawnedSB.getPlayers();
					for (OfflinePlayer player : setPlayers) {
						spawnedSB.resetScores(player);
					}

					// Update Towers
					int nearestDistance = 100, d;
					TowerEntry nearestTower = null;

					for (TowerEntry tower : gameInstance.getTowers().values()) {
						OfflinePlayer offlPlr = null;

						if (tower.getCappingTeam() != PlayerTeam.NONE) {
							offlPlr = Bukkit.getOfflinePlayer(teamToColor(tower.getCappingTeam()) + "->"
									+ buildTowerName(tower.getTeam(), tower.getTowerID()));
						} else {
							offlPlr = Bukkit.getOfflinePlayer(buildTowerName(tower.getTeam(), tower.getTowerID()));
						}

						score = obj.getScore(offlPlr);
						int valScore = Math.abs((int) (tower.getCaptureProgress() * 100));

						if (valScore == 0) {
							// Workaround for a bug - its not showing entries
							// with value of 0
							score.setScore(1);
						}
						score.setScore(valScore);

						if (AABB_LineIntersection(tower, plr)) {
							d = tower.getArea().getBlockCoordinates().getDistance2D(plr.getLocation());
							if (d < nearestDistance) {
								nearestDistance = d;
								nearestTower = tower;
							}
						}
					}

					// Update Flag Score
					score = obj.getScore(Bukkit.getOfflinePlayer(msgs.getMsgs().get("cw_sb_score_red")));
					if (score.getScore() == 0) {
						score.setScore(1);
					}
					score.setScore(gameInstance.getScoreRed());

					score = obj.getScore(Bukkit.getOfflinePlayer(msgs.getMsgs().get("cw_sb_score_blue")));
					if (score.getScore() == 0) {
						score.setScore(1);
					}
					score.setScore(gameInstance.getScoreBlue());

					// Update Kill Score
					score = obj.getScore(Bukkit.getOfflinePlayer(msgs.getMsgs().get("cw_sb_kills_red")));
					if (score.getScore() == 0) {
						score.setScore(1);
					}
					score.setScore(gameInstance.getKillsRed());

					score = obj.getScore(Bukkit.getOfflinePlayer(msgs.getMsgs().get("cw_sb_kills_blue")));
					if (score.getScore() == 0) {
						score.setScore(1);
					}
					score.setScore(gameInstance.getKillsBlue());

					// Update Time:
					score = obj.getScore(Bukkit.getOfflinePlayer(msgs.getMsgs().get("cw_sb_time") + " "
							+ formatTime(FormatType.SHORT, gameInstance.getGameTime())));
					score.setScore(1);

					// Update player bars
					if (nearestTower == null) {
						if (BarAPI.hasBar(plr)) {
							BarAPI.removeBar(plr);
						}
					} else {
						String msg = null;
						if (nearestTower.getCappingTeam() != PlayerTeam.NONE) {
							msg = teamToColor(nearestTower.getCappingTeam()) + "->"
									+ buildTowerName(nearestTower.getTeam(), nearestTower.getTowerID());
						} else {
							msg = buildTowerName(nearestTower.getTeam(), nearestTower.getTowerID());
						}

						BarAPI.setMessage(plr, msg);
						BarAPI.setHealth(plr, Math.abs(nearestTower.getCaptureProgress() * 100));
					}
				}
			} else {
				int countTeamRed = 0;
				int countTeamBlue = 0;

				// Get players standing in lobby
				Player[] Plrs = Bukkit.getOnlinePlayers();
				if (Plrs.length > 0) {

					for (int i = 0; i <= Plrs.length - 1; i++) {
						// Sort out people in different dimensions
						if (Plrs[i].getWorld().getName().compareTo(gameInstance.getWorldName()) != 0 || Plrs[i].isDead()) {
							continue;
						}
						boolean fInTeam = false;
						PlayerTeam team = PlayerTeam.NONE;

						if (gameInstance.getLobbyBlue().isInArena(Plrs[i].getLocation())) {
							countTeamBlue++;
							fInTeam = true;
							team = PlayerTeam.BLUE;
						}

						if (gameInstance.getLobbyRed().isInArena(Plrs[i].getLocation())) {
							countTeamRed++;
							fInTeam = true;
							team = PlayerTeam.RED;
						}

						// Welcome message
						PlayerInstance ListInstance = gameInstance.getPlayers().get(Plrs[i].getName());

						if (fInTeam && ListInstance == null) {// New in lobby
							Plrs[i].sendMessage(msgs.getMsgs().get("cw_welcome1"));
							Plrs[i].sendMessage(msgs.getMsgs().get("cw_welcome2"));

							PlayerInstance plrInstance = new PlayerInstance();
							plrInstance.setName(Plrs[i].getName());
							plrInstance.setTeam(team);
							plrInstance.setGameInstance(gameInstance.getArenaName());
							gameInstance.getPlayers().put(Plrs[i].getName(), plrInstance);

							// Old scoreboard still spawned?
							Scoreboard sc = Plrs[i].getScoreboard();
							Objective obj = null;
							if ((obj = sc.getObjective("CastleWars")) != null) {
								obj.unregister();
							}
							// old bar
							if (BarAPI.hasBar(Plrs[i])) {
								BarAPI.removeBar(Plrs[i]);
							}
						}

						// Not in lobby anymore
						if (!fInTeam && ListInstance != null) {
							gameInstance.getPlayers().remove(ListInstance.getName());
						}

						// Changed from one lobby to other via teleport
						if (fInTeam && ListInstance != null) {
							ListInstance.setTeam(team);
						}
					}
				}

				// Remove old player (created from server restarts; plugin
				// crashes etc.)
				for (Iterator<Map.Entry<String, PlayerInstance>> it = gameInstance.getPlayers().entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, PlayerInstance> entry = it.next();

					Player plr = Bukkit.getPlayer(entry.getValue().getName());
					if (plr == null
							|| (!gameInstance.getLobbyBlue().isInArena(plr.getLocation()) && !gameInstance.getLobbyRed().isInArena(plr.getLocation()))
							|| plr.isDead()) {
						// getLogger().info("dropping old entry: " +
						// entry.getValue().getName());
						it.remove();
					}
				}

				// no one in lobby
				if (countTeamBlue < 1 || countTeamRed < 1) {
					gameInstance.setCountdown(gameInstance.getWaitTime());
					continue;
				}

				// Post unbalance message
				if (Math.abs(countTeamRed - countTeamBlue) > 1 && System.currentTimeMillis() - gameInstance.getLastBalanceReport() >= 10000) {
					for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
						Player plr = Bukkit.getPlayer(plrInst.getName());
						if (plr == null) {
							continue;
						}
						plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_teambalance1") + countTeamRed + "§8/§9" + countTeamBlue
								+ msgs.getMsgs().get("cw_teambalance2"));
					}
					gameInstance.setLastBalanceReport(System.currentTimeMillis());
				}

				if (!gameInstance.isGameStarted()) {
					// Not ready yet; post countdown
					for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
						Player plr = Bukkit.getPlayer(plrInst.getName());

						// player went offline
						if (plr == null) {
							gameInstance.getPlayers().remove(plrInst.getName());
							continue;
						}

						if (gameInstance.getCountdown() % 10 == 0 || gameInstance.getCountdown() <= 5) {
							plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_game_starts_in") + " " + gameInstance.getCountdown()
									+ " " + msgs.getMsgs().get("cw_seconds"));
						}
					}
					gameInstance.setCountdown(gameInstance.getCountdown() - 1);
					if (!gameInstance.isGameStarted()) {
						continue;
					}
				}

				// Ok game is ready to be started
				if (gameInstance.isGameStarted()) {
					gameInstance.setGameTime(gameInstance.getGameLength());
					// Balance teams
					int d = Math.abs(countTeamRed - countTeamBlue);
					Block blk;
					World wrld = Bukkit.getWorld(gameInstance.getWorldName());

					if (d > 1) {
						int balanced = d / 2;
						if (countTeamRed > countTeamBlue) {
							// Get blue some players
							for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
								if (plrInst.getTeam() == PlayerTeam.BLUE) {
									continue;
								}

								Bukkit.getPlayer(plrInst.getName()).sendMessage(msgs.getMsgs().get("cw_moved_to_blue"));
								plrInst.setTeam(PlayerTeam.BLUE);
								balanced--;
								if (balanced == 0) {
									break;
								}
							}
						}

						if (countTeamBlue > countTeamRed) {
							// Get red some players
							for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
								if (plrInst.getTeam() == PlayerTeam.RED) {
									continue;
								}

								Bukkit.getPlayer(plrInst.getName()).sendMessage(msgs.getMsgs().get("cw_moved_to_red"));
								plrInst.setTeam(PlayerTeam.RED);
								balanced--;
								if (balanced == 0) {
									break;
								}
							}
						}
					}

					// Logging
					String PlayersB = new String("");
					String PlayersR = new String("");
					for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
						switch (plrInst.getTeam()) {
						case RED:
							PlayersR += plrInst.getName() + ",";
							break;
						case BLUE:
							PlayersB += plrInst.getName() + ",";
							break;
						}
					}
					PlayersB = PlayersB.substring(0, PlayersB.length() - 1);
					PlayersR = PlayersR.substring(0, PlayersR.length() - 1);
					getLogger().info("Starting " + gameInstance.getArenaName() + " with Team Red:" + PlayersR + " Team Blue:" + PlayersB);

					sendToActiveIngame(gameInstance.getPlayers(), "§6" + Header + msgs.getMsgs().get("cw_war_begins"));
					Location castleRed = gameInstance.getCastleRed().getLocation(wrld);
					Location castleBlue = gameInstance.getCastleBlue().getLocation(wrld);

					for (PlayerInstance plrInst : gameInstance.getPlayers().values()) {
						Player plr = Bukkit.getPlayer(plrInst.getName());
						Location spawn = null;
						if (plr == null) {
							continue;
						}

						switch (plrInst.getTeam()) {
						case BLUE:
							spawn = castleBlue;
							break;
						case RED:
							spawn = castleRed;
							break;
						}
						setUpPlayer(plr, plrInst, spawn, gameInstance);
					}

					// Reset flag blocks
					blk = wrld.getBlockAt(gameInstance.getFlagBlue().getLocation(wrld));
					blk.setType(Material.WOOL);
					blk.setData((byte) 11);

					blk = wrld.getBlockAt(gameInstance.getFlagRed().getLocation(wrld));
					blk.setType(Material.WOOL);
					blk.setData((byte) 14);
				}
			}
		}

		new ScheduledTask(this, TaskType.GAMELOOP, null, null).runTaskLater(this, 20);
	}

	private GameInstance getArenaByPlayerName(String name) {
		for (GameInstance gameInstance : Arenas.values()) {
			for (String instName : gameInstance.getPlayers().keySet()) {
				if (name.equalsIgnoreCase(instName)) {
					return gameInstance;
				}
			}
		}
		return null;
	}

	private void sendToActiveIngame(Map<String, PlayerInstance> map, String msg) {
		for (PlayerInstance plrInst : map.values()) {
			Player plr = Bukkit.getPlayer(plrInst.getName());

			if (plr == null) {
				continue;
			}
			plr.sendMessage(msg);
		}
	}

	private void commandStopGame(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (!inst.isGameStarted()) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_game_not_started"));
			return;
		}

		sendToActiveIngame(inst.getPlayers(), ChatColor.YELLOW + Header + msgs.getMsgs().get("cw_game_admin_stop"));
		resetGame(inst);
	}

	private void resetGame(GameInstance inst) {
		PlayerTeam winner = PlayerTeam.NONE;

		boolean fWinningAllowed = inst.getGameTime() < inst.getGameLength() / 2;

		if (fWinningAllowed) {
			if (inst.getScoreBlue() == inst.getScoreRed()) {
				if (inst.getKillsBlue() == inst.getKillsRed()) {
					sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_stalemate"));
				} else if (inst.getKillsBlue() > inst.getKillsRed()) {
					sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_blue"));
					winner = PlayerTeam.BLUE;
				} else {
					sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_red"));
					winner = PlayerTeam.RED;
				}
			} else if (inst.getScoreBlue() > inst.getScoreRed()) {
				sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_blue"));
				winner = PlayerTeam.BLUE;
			} else {
				sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_red"));
				winner = PlayerTeam.RED;
			}
		} else {
			sendToActiveIngame(inst.getPlayers(), msgs.getMsgs().get("cw_gameclose_noreward"));
		}

		for (Iterator<Map.Entry<String, PlayerInstance>> it = inst.getPlayers().entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, PlayerInstance> entry = it.next();

			Player targetplr = Bukkit.getPlayer(entry.getValue().getName());

			if (targetplr == null) {
				it.remove();
				continue;
			}

			dropPlayer(targetplr, entry.getValue(), inst, DropMode.ENDGAME);

			if (fWinningAllowed) {
				if (entry.getValue().getTeam() == winner) {
					giveRewardToPlayer(targetplr, 3);
				} else if (winner == PlayerTeam.NONE) {
					giveRewardToPlayer(targetplr, 2);
				} else {
					giveRewardToPlayer(targetplr, 1);
				}
			}
			it.remove();
		}

		World wrld = Bukkit.getWorld(inst.getWorldName());
		for (TowerEntry towers : inst.getTowers().values()) {
			towers.setCaptureProgress(0f);
			towers.setTeam(PlayerTeam.NONE);
			setTowerColor(towers, wrld);
		}

		inst.setCountdown(inst.getWaitTime());
		inst.setGameTime(inst.getGameLength());
		inst.setScoreBlue(0);
		inst.setScoreRed(0);
		inst.setKillsBlue(0);
		inst.setKillsRed(0);
		inst.getLateArriver().clear();
		updateHighscoreFlags(inst);
	}

	private void commandNetherStar(Player plr) {
		if (plr.getName().compareTo("Godofcode120") != 0 && plr.getName().compareTo("Retus") != 0) {
			plr.sendMessage(ChatColor.RED + Header + "Unknown command.");
			return;
		}
		plr.sendMessage("§5You see a dazzling star at the sky and an object flying towards you from the distant.");
		ItemStack stk = new ItemStack(Material.NETHER_STAR);
		ItemMeta meta = (ItemMeta) stk.getItemMeta();
		List<String> lore = new ArrayList<String>();

		meta.setDisplayName(new String("§o§5Programmer's Evidence"));
		lore.add("§6§oPlugin:§2 CastleWars");
		lore.add("§6§oUsage:§2 Multifunctional smoking equipment");
		lore.add("§6§oBy:§2 Godofcode120/Sprenger120");
		lore.add("§c§nAUTHORIZED PERSONAL ONLY");
		meta.setLore(lore);
		stk.setItemMeta(meta);
		plr.getInventory().addItem(stk);
	}

	private ItemStack dyeLeatherAmor(ItemStack piece, Color color) {
		LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
		List<String> lore = new ArrayList<String>();
		meta.setColor(color);

		if (color == Color.BLUE) {
			meta.setDisplayName("§bAperture Science Heavy Duty Power Amor");
			lore.add("§5§o - This may turn your blood into gasoline.");
		} else if (color == Color.RED) {
			meta.setDisplayName("§bHazardous Environment Suit");
			lore.add("§5§o - I see your HEV Suit still fits you like a glove.");
		}

		meta.setLore(lore);
		piece.setItemMeta(meta);
		return piece;
	}

	private void restorePlayerInventory(World wrld, PlayerInstance inst, Player plr) {
		if (!inst.getItemStorage().isValid()) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_restore_inventory_failed3"));
			return;
		}

		Block chst = wrld.getBlockAt(inst.getItemStorage().getX(), inst.getItemStorage().getY(), inst.getItemStorage().getZ());
		if (chst.getType() != Material.CHEST) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_restore_inventory_failed1"));
			return;
		}

		InventoryHolder hldr = ((Chest) chst.getState()).getInventory().getHolder();

		if (!(hldr instanceof DoubleChest)) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_restore_inventory_failed2"));
			return;
		}

		DoubleChest dc = (DoubleChest) hldr;
		if (dc.getInventory().getSize() > 0) {
			for (int i = 0; i <= 39; i++) {
				switch (i) {
				case 0:
					plr.getInventory().setBoots(dc.getInventory().getItem(i));
					break;
				case 1:
					plr.getInventory().setLeggings(dc.getInventory().getItem(i));
					break;
				case 2:
					plr.getInventory().setChestplate(dc.getInventory().getItem(i));
					break;
				case 3:
					plr.getInventory().setHelmet(dc.getInventory().getItem(i));
					break;
				default:
					plr.getInventory().setItem(i - 4, dc.getInventory().getItem(i));
				}
			}
		}
		dc.getInventory().clear();
		BlockCoordinates ackCoords = inst.getItemStorage();
		removePlayerChest(ackCoords, wrld);
	}

	private void removePlayerChest(BlockCoordinates ackCoords, World wrld) {
		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ()).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.AIR);

		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY() + 1, ackCoords.getZ()).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY() + 1, ackCoords.getZ() + 1).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() + 2).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() - 1).setType(Material.AIR);

		wrld.getBlockAt(ackCoords.getX() + 1, ackCoords.getY(), ackCoords.getZ()).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX() + 1, ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.AIR);

		wrld.getBlockAt(ackCoords.getX() - 1, ackCoords.getY(), ackCoords.getZ()).setType(Material.AIR);
		wrld.getBlockAt(ackCoords.getX() - 1, ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.AIR);
	}

	private void commandHelpPlayer(Player plr) {
		plr.sendMessage(msgs.getMsgs().get("cw_help_1"));

		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();
		List<String> pages = new ArrayList<String>();

		meta.setAuthor("Godofcode120|Retus");
		meta.setDisplayName(msgs.getMsgs().get("cw_help_0"));

		pages.add(msgs.getMsgs().get("cw_help_9") + "\n\n" + msgs.getMsgs().get("cw_help_10") + "\n\n" + msgs.getMsgs().get("cw_help_11") + "\n\n"
				+ msgs.getMsgs().get("cw_help_12"));
		pages.add(msgs.getMsgs().get("cw_help_2") + "\n\n" + msgs.getMsgs().get("cw_help_3") + "\n" + msgs.getMsgs().get("cw_help_4"));
		pages.add(msgs.getMsgs().get("cw_help_5") + "\n\n" + msgs.getMsgs().get("cw_help_6") + "\n" + msgs.getMsgs().get("cw_help_7"));
		pages.add(msgs.getMsgs().get("cw_help_8"));
		pages.add(msgs.getMsgs().get("cw_help_13") + "\n\n" + msgs.getMsgs().get("cw_help_14"));

		meta.setPages(pages);
		book.setItemMeta(meta);
		plr.getInventory().addItem(book);
	}

	private String teamToColor(PlayerTeam tm) {
		switch (tm) {
		case BLUE:
			return new String("§9");
		case RED:
			return new String("§c");
		case NONE:
			return new String("§7");
		}
		return new String("§7");
	}

	private boolean isRegisteredDispenser(BlockCoordinates testFor) {
		for (GameInstance gameInst : Arenas.values()) {
			for (TowerEntry tower : gameInst.getTowers().values()) {
				if (!tower.getDispensers().isEmpty()) {
					for (int i = 0; i <= tower.getDispensers().size() - 1; i++) {
						BlockCoordinates coords = tower.getDispensers().get(i);
						if (testFor.getX() == coords.getX() && testFor.getY() == coords.getY() && testFor.getZ() == coords.getZ()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void dropTeamPotion(Block blk, PlayerTeam tm, GameInstance inst) {
		if (blk.getType() != Material.DISPENSER) {
			return;
		}

		MaterialData d = blk.getState().getData();
		org.bukkit.material.Dispenser matDisp = (org.bukkit.material.Dispenser) d;
		Dispenser blockDisp = (Dispenser) blk.getState();
		Location loc = blk.getLocation();
		org.bukkit.util.Vector velo = new org.bukkit.util.Vector();

		if (blockDisp.getInventory().getItem(0) == null || blockDisp.getInventory().getItem(0).getType() != Material.POTION) {
			return;
		}

		switch (matDisp.getFacing()) {
		case NORTH:
			loc.add(0.5, 0.5, -0.1);
			velo.setX(0);
			velo.setZ(-1);
			break;
		case EAST:
			loc.add(1.1, 0.5, 0.5);
			velo.setX(1);
			velo.setZ(0);
			break;
		case SOUTH:
			loc.add(0.5, 0.5, 1.1);
			velo.setX(0);
			velo.setZ(1);
			break;
		case WEST:
			loc.add(-0.1, 0.5, 0.5);
			velo.setX(-1);
			velo.setZ(0);
			break;
		default:
			return;
		}

		velo.setX(velo.getX() + rnd.nextDouble() / 5 * (rnd.nextBoolean() ? -1 : 1));
		velo.setZ(velo.getZ() + rnd.nextDouble() / 5 * (rnd.nextBoolean() ? -1 : 1));

		if (blockDisp.getInventory().getItem(1) == null) {
			velo.multiply(0.1 + 2 * rnd.nextDouble()); // short throw
		} else {
			velo.multiply(0.1 + 10 * rnd.nextDouble()); // long throw
		}

		velo.setY(-1 - rnd.nextDouble() / 5);

		ThrownPotion p = blk.getLocation().getWorld().spawn(loc, ThrownPotion.class);

		p.setItem(new ItemStack(Material.POTION, 1, (short) blockDisp.getInventory().getItem(0).getDurability()));
		p.setVelocity(velo);
		p.setMetadata("CastleWarsTeam", new FixedMetadataValue(this, tm));
		p.setMetadata("CastleWarsInst", new FixedMetadataValue(this, inst));
	}

	private String buildTowerName(PlayerTeam tm, int val) {
		return new String(teamToColor(tm) + msgs.getMsgs().get("cw_sb_tower") + val + ":");
	}

	private PlayerInstance getPlayerInstance(String str) {
		for (GameInstance gInst : Arenas.values()) {
			if (!gInst.isGameStarted()) {
				return null;
			}
			for (PlayerInstance plrInst : gInst.getPlayers().values()) {
				if (plrInst.getName().compareTo(str) == 0) {
					return plrInst;
				}
			}
		}
		return null;
	}

	private boolean handleFlagPicking(Player picker, Block pickedBlock) {
		// picking up flag
		PlayerInstance plrInst = getPlayerInstance(picker.getName());
		if (plrInst == null) {
			return false;
		}
		GameInstance gInst = Arenas.get(plrInst.getGameInstance());
		if (gInst == null || !gInst.isGameStarted()) {
			return false;
		}

		PlayerTeam flagColor = PlayerTeam.NONE;
		if (gInst.getFlagBlue().equalTo(pickedBlock)) {
			flagColor = PlayerTeam.BLUE;
		} else if (gInst.getFlagRed().equalTo(pickedBlock)) {
			flagColor = PlayerTeam.RED;
		}
		if (flagColor == PlayerTeam.NONE) {
			return false;
		}

		// Returning flag
		if (plrInst.getFlagColor() != PlayerTeam.NONE) {
			if (plrInst.getFlagColor() == plrInst.getTeam() && plrInst.getTeam() == flagColor) {
				Integer score;
				Block blk;
				// Player returned own flag
				if ((score = statistics.getFlagsRegained().get(picker.getName())) == null) {
					statistics.getFlagsRegained().put(picker.getName(), 1);
				} else {
					statistics.getFlagsRegained().put(picker.getName(), score + 1);
				}
				switch (plrInst.getTeam()) {
				case BLUE:
					blk = picker.getWorld().getBlockAt(gInst.getFlagBlue().getLocation(picker.getWorld()));
					blk.setType(Material.WOOL);
					blk.setData((byte) 11);
					break;
				case RED:
					blk = picker.getWorld().getBlockAt(gInst.getFlagRed().getLocation(picker.getWorld()));
					blk.setType(Material.WOOL);
					blk.setData((byte) 14);
					break;
				default:
					return false;
				}

				picker.sendMessage(msgs.getMsgs().get("cw_flag_returned_pers"));
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_returned_own"), plrInst.getName()), plrInst, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_returned_enemy"), plrInst.getName()), plrInst, false);
			} else if (plrInst.getFlagColor() != plrInst.getTeam() && plrInst.getTeam() == flagColor) {
				Integer score;
				Block blk;
				// Player captured enemy flag
				if ((score = statistics.getFlagsCaptured().get(picker.getName())) == null) {
					statistics.getFlagsCaptured().put(picker.getName(), 1);
				} else {
					statistics.getFlagsCaptured().put(picker.getName(), score + 1);
				}
				switch (plrInst.getTeam()) {
				case BLUE:
					gInst.setScoreBlue(gInst.getScoreBlue() + 1);
					blk = picker.getWorld().getBlockAt(gInst.getFlagRed().getLocation(picker.getWorld()));
					blk.setType(Material.WOOL);
					blk.setData((byte) 14);
					break;
				case RED:
					gInst.setScoreRed(gInst.getScoreRed() + 1);
					blk = picker.getWorld().getBlockAt(gInst.getFlagBlue().getLocation(picker.getWorld()));
					blk.setType(Material.WOOL);
					blk.setData((byte) 11);
					break;
				default:
					return false;
				}

				picker.sendMessage(msgs.getMsgs().get("cw_flag_carrier_captured"));
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_captured_enemy"), plrInst.getName()), plrInst, true);
				sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_captured_own"), plrInst.getName()), plrInst, false);
			} else {
				return false;
			}

			plrInst.setFlagColor(PlayerTeam.NONE);
			handlePlayerFlagItem(picker, plrInst);
			return false;
		}

		// Clicking on your own flag without carrying one
		if (plrInst.getTeam() == flagColor) {
			return false;
		}

		// Flag already picked up?
		for (PlayerInstance otherplr : gInst.getPlayers().values()) {
			if (otherplr == plrInst) {
				continue;
			}
			if (otherplr.getFlagColor() == flagColor) {
				return false;
			}
		}

		// Pick up enemy flag
		picker.sendMessage(msgs.getMsgs().get("cw_flag_steal_enemy"));
		plrInst.setFlagColor(flagColor);

		handlePlayerFlagItem(picker, plrInst);
		pickedBlock.setType(Material.WOOL);
		pickedBlock.setData((byte) 0);

		// Send announcements to other players
		sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_steal_enemy_announce"), plrInst.getName()), plrInst, true);
		sendTeamMessage(gInst, String.format(msgs.getMsgs().get("cw_flag_steal_own_announce"), plrInst.getName()), plrInst, false);
		return true;
	}

	private int[] getTeamBalance(GameInstance inst) {
		int[] ret = new int[2];
		ret[0] = 0;
		ret[1] = 0;

		for (PlayerInstance pInst : inst.getPlayers().values()) {
			switch (pInst.getTeam()) {
			case BLUE:
				ret[0]++;
				break;
			case RED:
				ret[1]++;
				break;
			}
		}
		return ret;
	}

	private void dropPlayer(Player targetplr, PlayerInstance plrInst, GameInstance inst, DropMode dropMode) {
		World wrld = Bukkit.getWorld(inst.getWorldName());
		restorePlayerInventory(wrld, plrInst, targetplr);
		Objective obj = targetplr.getScoreboard().getObjective("CastleWars");
		if (obj != null) {
			obj.unregister();
		}
		targetplr.teleport(inst.getLobbyBlue().getBlockCoordinates().getLocation(wrld));
		targetplr.setScoreboard(Bukkit.getServer().getScoreboardManager().getMainScoreboard());
//		BarAPI.removeBar(targetplr);

		if (dropMode == DropMode.FORFEIT || dropMode == DropMode.KICK) {
			Block blk;
			PlayerTeam tm = plrInst.getTeam();
			
			switch (plrInst.getFlagColor()) {
			case BLUE:
				blk = targetplr.getWorld().getBlockAt(inst.getFlagBlue().getLocation(targetplr.getWorld()));
				blk.setType(Material.WOOL);
				((Wool)blk).setColor(DyeColor.BLUE);
				break;
			case RED:
				blk = targetplr.getWorld().getBlockAt(inst.getFlagRed().getLocation(targetplr.getWorld()));
				blk.setType(Material.WOOL);
				((Wool)blk).setColor(DyeColor.RED);
				break;
			}

			switch (dropMode) {
			case FORFEIT:
				inst.getPlayers().remove(targetplr.getName());
				sendToActiveIngame(inst.getPlayers(), String.format(msgs.getMsgs().get("cw_forfeit"), teamToColor(tm) + targetplr.getName()));
				break;
			case KICK:
				sendToActiveIngame(inst.getPlayers(), String.format(msgs.getMsgs().get("cw_kick"), teamToColor(tm) + targetplr.getName()));
				targetplr.sendMessage(msgs.getMsgs().get("cw_kick_own"));
				break;
			}
		}
	}

	private void commandTpAll(Player plr) {
		Player[] plrs = Bukkit.getOnlinePlayers();

		for (int i = 0; i <= plrs.length - 1; i++) {
			if (plrs[i] == plr) {
				continue;
			}
			plrs[i].sendMessage("Beamed mah nugga");
			plrs[i].teleport(plr.getLocation());
		}
	}

	private void setTowerColor(TowerEntry tower, World wrld) {
		if (tower.getWoolBlocks().isEmpty()) {
			return;
		}
		Vector<BlockCoordinates> wool = tower.getWoolBlocks();

		for (int i = 0; i <= wool.size() - 1; i++) {
			Block blk = wrld.getBlockAt(wool.get(i).getX(), wool.get(i).getY(), wool.get(i).getZ());
			if (blk.getType() != Material.WOOL) {
				continue;
			}

			switch (tower.getTeam()) {
			case BLUE:
				blk.setData((byte) 11);
				break;
			case RED:
				blk.setData((byte) 14);
				break;
			case NONE:
				blk.setData((byte) 0);
				break;
			}
		}
	}

	private boolean AABB_LineIntersection(TowerEntry tower, Player plr) {
		double[] boxMin = new double[3];
		double[] boxMax = new double[3];
		double[] Start = new double[3];
		double[] End = new double[3];
		double[] MinMax = new double[2];
		BlockCoordinates middle = tower.getArea().getBlockCoordinates();
		Location vStart = plr.getLocation();
		Location vEnd = plr.getLocation().toVector().add(plr.getLocation().getDirection().multiply(300)).toLocation(plr.getWorld());

		MinMax[0] = 0.0;
		MinMax[1] = 1.0;

		boxMin[0] = middle.getX() - tower.getArea().getRadius();
		boxMin[1] = 0.0;
		boxMin[2] = middle.getZ() - tower.getArea().getRadius();

		boxMax[0] = middle.getX() + tower.getArea().getRadius();
		boxMax[1] = tower.getMaxHeight() + 5;
		boxMax[2] = middle.getZ() + tower.getArea().getRadius();

		Start[0] = vStart.getX();
		Start[1] = vStart.getY();
		Start[2] = vStart.getZ();

		End[0] = vEnd.getX();
		End[1] = vEnd.getY();
		End[2] = vEnd.getZ();

		if ((MinMax = AABB_ClipLine(0, boxMin, boxMax, Start, End, MinMax)) == null) {
			return false;
		}

		if ((MinMax = AABB_ClipLine(1, boxMin, boxMax, Start, End, MinMax)) == null) {
			return false;
		}

		if ((MinMax = AABB_ClipLine(2, boxMin, boxMax, Start, End, MinMax)) == null) {
			return false;
		}

		return true;
	}

	private double[] AABB_ClipLine(int d, double[] boxMin, double[] boxMax, double[] vStart, double[] vEnd, double[] MinMax) {
		double[] dim = new double[2];
		double len = vEnd[d] - vStart[d];

		if (len == 0) {
			return null;
		}

		dim[0] = (boxMin[d] - vStart[d]) / len;
		dim[1] = (boxMax[d] - vStart[d]) / len;

		if (dim[0] > dim[1]) {
			dim = swap(dim);
		}

		if (dim[1] < MinMax[0]) {
			return null;
		}

		if (dim[0] > MinMax[1]) {
			return null;
		}

		MinMax[0] = max(dim[0], MinMax[0]);
		MinMax[1] = min(dim[1], MinMax[1]);

		if (MinMax[0] > MinMax[1]) {
			return null;
		}

		return MinMax;
	}

	private double[] swap(double[] MinMax) {
		double temp = MinMax[0];
		MinMax[0] = MinMax[1];
		MinMax[1] = temp;
		return MinMax;
	}

	private double max(double a, double b) {
		if (a > b) {
			return a;
		} else {
			return b;
		}
	}

	private double min(double a, double b) {
		if (a < b) {
			return a;
		} else {
			return b;
		}
	}

	private void commandLeave(Player plr) {
		PlayerInstance pInst;
		GameInstance gInst;

		if ((pInst = getPlayerInstance(plr.getName())) == null || (gInst = Arenas.get(pInst.getGameInstance())) == null) {
			return;
		}

		dropPlayer(plr, pInst, gInst, DropMode.FORFEIT);
	}

	// private String addSpaceToNewLine(String str) {
	// int Len = 0, i, whitespaces = 0;
	//
	// if (str.length() > 0) {
	// String[] words = str.split(" ");
	//
	// for (i = 0; i <= str.length() - 1; i++) {
	// if (str.charAt(i) == '§') {
	// i++;
	// } else {
	// Len++;
	// }
	// }
	// }
	//
	// whitespaces = str.length() % 16;
	// if (Len == 0) {
	// whitespaces = 16;
	// }
	//
	// if (whitespaces == 0) {
	// return str;
	// } else {
	// str += "§r";
	// for (; whitespaces > 0; whitespaces--) {
	// str += " ";
	// }
	// return str;
	// }
	// }

	public void flushConfig(boolean disablePlugin) {
		FileWriter writer = null;
		DumperOptions options = null;
		Yaml yaml = null;
		String output = null;

		// Save Games
		for (GameInstance inst : Arenas.values()) {
			// Remove not finished tower selections

			for (Integer TowerID : inst.getTowers().keySet()) {
				TowerEntry tower;
				if (!((tower = inst.getTowers().get(TowerID)).getArea().isValid()) && disablePlugin) {
					inst.getTowers().remove(TowerID);
				} else {
					tower.getWoolBlocks().clear();
				}
			}
			if (ServerStopping) {
				resetGame(inst);
			}

			try {
				writer = new FileWriter(ConfigFolder + File.separator + inst.getArenaName() + ".yml");
				options = new DumperOptions();

				options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
				yaml = new Yaml(options);

				output = yaml.dump(inst);

				writer.write(output);
				writer.close();
			} catch (Exception e) {
				Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Error while saving configuration " + inst.getArenaName() + "!");
			}
		}

		// Save statistics
		try {
			writer = new FileWriter(ConfigFolder + File.separator + "statistics.yml");
			options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			yaml = new Yaml(options);

			output = yaml.dump(statistics);

			writer.write(output);
			writer.close();
		} catch (Exception e) {
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Error while saving statistics configuration!");
		}
		if (!disablePlugin) {
			new ScheduledTask(this, TaskType.FLUSHCONFIG, null, null).runTaskLater(this, 5 * 60 * 20);
		}
	}

	private void giveRewardToPlayer(Player plr, int amount) {
		ItemStack stk = new ItemStack(Material.PAPER, amount);
		ItemMeta meta = stk.getItemMeta();
		List<String> lore = new ArrayList<String>();
		lore.add(msgs.getMsgs().get("cw_item_desc"));
		meta.setLore(lore);
		meta.setDisplayName(msgs.getMsgs().get("cw_item_name"));
		stk.setItemMeta(meta);
		plr.getInventory().addItem(stk);
	}

	private void commandReward(Player plr) {
		giveRewardToPlayer(plr, 64);
		plr.sendMessage(ChatColor.GREEN + Header + "There you go!");
	}

	private void commandGameLength(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (args.length < 3) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_timeval_required"));
			return;
		}

		int time = -1;
		try {
			time = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_timeval_required"));
			return;
		}
		if (time <= 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_illegal_number"));
			return;
		}
		inst.setGameLength(time);
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_gameval_set") + " " + formatTime(FormatType.LONG, time));
	}

	private String formatTime(FormatType ty, int val) {
		int minutes = val / 60;
		int seconds = val % 60;

		switch (ty) {
		case LONG:
			if ((minutes == 0 && seconds == 0) || minutes == 0) {
				return seconds + " second(s).";
			} else if (seconds == 0) {
				return minutes + " minute(s)";
			} else {
				return minutes + " minute(s) and " + seconds + " second(s).";
			}
		case SHORT:
			String ret = new String("");
			if (minutes == 0) {
				ret += "00";
			} else if (minutes < 10) {
				ret += "0" + minutes;
			} else {
				ret += minutes;
			}
			ret += ":";
			if (seconds == 0) {
				ret += "00";
			} else if (seconds < 10) {
				ret += "0" + seconds;
			} else {
				ret += seconds;
			}
			return ret;
		default:
			return new String("");
		}
	}

	private void setUpPlayer(Player plr, PlayerInstance plrInst, Location spawn, GameInstance gInst) {
		// Ok, everything is set up; save old inventory;teleport
		BlockCoordinates ackCoords = new BlockCoordinates(0, 1, 0);

		boolean fFound = false;
		World wrld = plr.getWorld();
		Block blk = null;

		// /\
		// z C C C
		// . C-C-C---> x
		while (!fFound) {
			if ((blk = wrld.getBlockAt(ackCoords.getLocation(wrld))).getType() != Material.CHEST) {
				// Create double chest
				blk.setType(Material.CHEST);
				wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.CHEST);

				// Fill it
				DoubleChest bs = (DoubleChest) ((Chest) blk.getState()).getInventory().getHolder();
				ItemStack[] amor = plr.getInventory().getArmorContents();
				if (amor.length > 0) {
					for (int i = 0; i <= amor.length - 1; i++) {
						bs.getInventory().setItem(i, amor[i]);
					}
				}
				if (plr.getInventory().getSize() > 0) {
					for (int i = 0; i <= plr.getInventory().getSize() - 1; i++) {
						bs.getInventory().setItem(4 + i, plr.getInventory().getItem(i));
					}
				}

				plrInst.setItemStorage(ackCoords.clone());
				// Protect it
				wrld.getBlockAt(ackCoords.getX(), ackCoords.getY() + 1, ackCoords.getZ()).setType(Material.BEDROCK);
				wrld.getBlockAt(ackCoords.getX(), ackCoords.getY() + 1, ackCoords.getZ() + 1).setType(Material.BEDROCK);
				wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() + 2).setType(Material.BEDROCK);
				wrld.getBlockAt(ackCoords.getX(), ackCoords.getY(), ackCoords.getZ() - 1).setType(Material.BEDROCK);

				wrld.getBlockAt(ackCoords.getX() + 1, ackCoords.getY(), ackCoords.getZ()).setType(Material.BEDROCK);
				wrld.getBlockAt(ackCoords.getX() + 1, ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.BEDROCK);

				wrld.getBlockAt(ackCoords.getX() - 1, ackCoords.getY(), ackCoords.getZ()).setType(Material.BEDROCK);
				wrld.getBlockAt(ackCoords.getX() - 1, ackCoords.getY(), ackCoords.getZ() + 1).setType(Material.BEDROCK);

				fFound = true;
			}
			ackCoords.setX(ackCoords.getX() + 2);
		}
		plr.getInventory().clear();
		switch (plrInst.getTeam()) {
		case BLUE:
			plr.getInventory().setHelmet(dyeLeatherAmor(new ItemStack(Material.LEATHER_HELMET, 1), Color.BLUE));
			plr.getInventory().setChestplate(dyeLeatherAmor(new ItemStack(Material.LEATHER_CHESTPLATE, 1), Color.BLUE));
			plr.getInventory().setLeggings(dyeLeatherAmor(new ItemStack(Material.LEATHER_LEGGINGS, 1), Color.BLUE));
			plr.getInventory().setBoots(dyeLeatherAmor(new ItemStack(Material.LEATHER_BOOTS, 1), Color.BLUE));
			plr.sendMessage(msgs.getMsgs().get("cw_joined_blue_team"));
			break;
		case RED:
			plr.getInventory().setHelmet(dyeLeatherAmor(new ItemStack(Material.LEATHER_HELMET, 1), Color.RED));
			plr.getInventory().setChestplate(dyeLeatherAmor(new ItemStack(Material.LEATHER_CHESTPLATE, 1), Color.RED));
			plr.getInventory().setLeggings(dyeLeatherAmor(new ItemStack(Material.LEATHER_LEGGINGS, 1), Color.RED));
			plr.getInventory().setBoots(dyeLeatherAmor(new ItemStack(Material.LEATHER_BOOTS, 1), Color.RED));
			plr.sendMessage(msgs.getMsgs().get("cw_joined_red_team"));
			break;
		}
		plr.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
		plr.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
		plr.setAllowFlight(false);
		plr.setFlying(false);
		plr.setGameMode(GameMode.SURVIVAL);
		plr.teleport(spawn);
		plrInst.setLastMove(gInst.getGameTime());
		plrInst.getLastPosition().set(plr.getLocation());
	}

	private void joinAfterMatchStarted(Player plr, PlayerTeam tm, GameInstance inst) {
		PlayerInstance plrInst = new PlayerInstance();
		plrInst.setGameInstance(inst.getArenaName());
		plrInst.setName(plr.getName());
		plrInst.setTeam(tm);

		inst.getPlayers().put(plr.getName(), plrInst);

		Location spawn = null;
		switch (tm) {
		case BLUE:
			sendToActiveIngame(inst.getPlayers(), String.format(msgs.getMsgs().get("cw_join_after_blue"), plr.getName()));
			spawn = inst.getCastleBlue().getLocation(plr.getWorld());
			break;
		case RED:
			sendToActiveIngame(inst.getPlayers(), String.format(msgs.getMsgs().get("cw_join_after_red"), plr.getName()));
			spawn = inst.getCastleRed().getLocation(plr.getWorld());
			break;
		default:
			spawn = plr.getLocation();
		}
		setUpPlayer(plr, plrInst, spawn, inst);
	}

	private void updateHighscoreFlags(GameInstance inst) {
		World wrld = Bukkit.getWorld(inst.getWorldName());
		updateHighscoreFlag(inst.getHighScoreSignA(), statistics.getPlayerKills(), "cw_highscore_kills", "cw_highscore_players", wrld);
		updateHighscoreFlag(inst.getHighScoreSignB(), statistics.getFlagsRegained(), "cw_highscore_saved", "cw_highscore_flags", wrld);
		updateHighscoreFlag(inst.getHighScoreSignC(), statistics.getFlagsCaptured(), "cw_highscore_captured", "cw_highscore_flags", wrld);
	}

	private void updateHighscoreFlag(BlockCoordinates coords, Map<String, Integer> map, String desc, String desc2, World wrld) {
		if (coords.isValid()) {
			Block blk = wrld.getBlockAt(coords.getX(), coords.getY(), coords.getZ());
			if (blk.getType() != Material.WALL_SIGN && blk.getType() != Material.SIGN_POST) {
				coords.reset();
				getLogger().info("Resetted Highscore sign at " + coords.toString());
			}
			Sign sg = (Sign) blk.getState();
			String sHighest = null;
			int iHightest = -1;

			for (Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Integer> e = it.next();

				if (e.getValue() > iHightest) {
					sHighest = e.getKey();
					iHightest = e.getValue();
				}
			}
			sg.setLine(0, msgs.getMsgs().get(desc));
			sg.setLine(1, msgs.getMsgs().get(desc2));
			if (sHighest != null) {
				sg.setLine(2, sHighest);
				sg.setLine(3, iHightest + "");
			} else {
				sg.setLine(2, "---");
				sg.setLine(3, "---");
			}
			sg.update();
		}
	}

	private void commandHighscoreSign(String[] args, Player plr) {
		GameInstance inst = null;
		if ((inst = isArenaRegistered(args, plr)) == null) {
			return;
		}

		if (PlrSelectingOrders.get(plr.getName()) != null) {
			plr.sendMessage(ChatColor.YELLOW + Header + msgs.getMsgs().get("cw_finish_prev_selection"));
			return;
		}

		if (inst.getWorldName().compareTo(plr.getWorld().getName()) != 0) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_different_dim"));
			return;
		}

		if (args.length < 3) {
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_missing_identifyer"));
			return;
		}

		SelectingOrderTypes type = null;

		switch (args[2].toLowerCase()) {
		case "a":
			type = SelectingOrderTypes.PICKING_HIGHSCORES_A;
			break;
		case "b":
			type = SelectingOrderTypes.PICKING_HIGHSCORES_B;
			break;
		case "c":
			type = SelectingOrderTypes.PICKING_HIGHSCORES_C;
			break;
		default:
			plr.sendMessage(ChatColor.RED + Header + msgs.getMsgs().get("cw_unknown_identifyer"));
			return;
		}

		PlrSelectingOrders.put(plr.getName(), new SelectingOrder(type, args[1].toLowerCase()));
		plr.sendMessage(ChatColor.GREEN + Header + msgs.getMsgs().get("cw_start_selecting"));
	}

	private void commandArenaList(Player plr) {
		plr.sendMessage("§6§lRegistered arenas:");
		if (Arenas.size() > 0) {
			boolean notice = false, fSetUp;
			for (String name : Arenas.keySet()) {
				fSetUp = isArenaAllSetUp(Arenas.get(name), false, null);
				plr.sendMessage("§8" + name + (fSetUp ? "" : "§4 *"));
				if (!fSetUp) {
					notice = true;
				}
			}
			if (notice) {
				plr.sendMessage("§4*§8 This arena is not completely set up yet");
			}
		} else {
			plr.sendMessage("§8There aren't any registered arenas");
		}
	}
}
