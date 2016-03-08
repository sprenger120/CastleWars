package de.fastcrafter.castlewars;

import java.util.HashMap;
import java.util.Map;

public class GameInstance {
	private String ArenaName = new String("<theonewithoutname>");

	private BlockCoordinates FlagRed = new BlockCoordinates(0, -1, 0);
	private BlockCoordinates FlagBlue = new BlockCoordinates(0, -1, 0);
	
	private BlockCoordinates CastleBlue = new BlockCoordinates(0, -1, 0);
	private BlockCoordinates CastleRed = new BlockCoordinates(0, -1, 0);
	
	private CircularArea LobbyBlue = new CircularArea(0, -1, 0, -1);
	private CircularArea LobbyRed = new CircularArea(0, -1, 0, -1);

	private int WaitTime = 120;
	private String WorldName = null;
	private Map<Integer,TowerEntry> Towers = new  HashMap<Integer,TowerEntry>();
	
	private Map<String,PlayerInstance> Players = new HashMap<String,PlayerInstance>();
	private Map<String,Integer> LateArriver = new HashMap<String,Integer>();
	private int Countdown = WaitTime;
	private long lastBalanceReport = 0L;
	
	private int GameLength = 600;
	private int GameTime = GameLength;
	
	private int ScoreBlue = 0;
	private int ScoreRed = 0;
	
	private int KillsBlue = 0;
	private int KillsRed = 0;
	
	private BlockCoordinates HighScoreSignA = new BlockCoordinates();
	private BlockCoordinates HighScoreSignB = new BlockCoordinates();
	private BlockCoordinates HighScoreSignC = new BlockCoordinates();
	
	public BlockCoordinates getFlagRed() {
		return FlagRed;
	}
	public void setFlagRed(BlockCoordinates flagRed) {
		FlagRed = flagRed;
	}
	public BlockCoordinates getFlagBlue() {
		return FlagBlue;
	}
	public void setFlagBlue(BlockCoordinates flagBlue) {
		FlagBlue = flagBlue;
	}

	public CircularArea getLobbyBlue() {
		return LobbyBlue;
	}
	public void setLobbyBlue(CircularArea lobbyBlue) {
		LobbyBlue = lobbyBlue;
	}
	public CircularArea getLobbyRed() {
		return LobbyRed;
	}
	public void setLobbyRed(CircularArea lobbyRed) {
		LobbyRed = lobbyRed;
	}
	public int getWaitTime() {
		return WaitTime;
	}
	public void setWaitTime(int waitTime) {
		WaitTime = waitTime;
	}
	public Map<Integer, TowerEntry> getTowers() {
		return Towers;
	}
	public void setTowers(Map<Integer, TowerEntry> towers) {
		Towers = towers;
	}
	public BlockCoordinates getCastleBlue() {
		return CastleBlue;
	}
	public void setCastleBlue(BlockCoordinates castleBlue) {
		CastleBlue = castleBlue;
	}
	public BlockCoordinates getCastleRed() {
		return CastleRed;
	}
	public void setCastleRed(BlockCoordinates castleRed) {
		CastleRed = castleRed;
	}	
	
	public String getArenaName() {
		return ArenaName;
	}
	public void setArenaName(String arenaName) {
		ArenaName = arenaName;
	}
	public int getCountdown() {
		return Countdown;
	}
	public void setCountdown(int countdown) {
		Countdown = countdown;
	}
	public Map<String, PlayerInstance> getPlayers() {
		return Players;
	}
	public void setPlayers(Map<String, PlayerInstance> players) {
		Players = players;
	}
	
	public boolean isGameStarted() { 
		return Countdown == 0;
	}
	public String getWorldName() {
		return WorldName;
	}
	public void setWorldName(String worldName) {
		WorldName = worldName;
	}
	public long getLastBalanceReport() {
		return lastBalanceReport;
	}
	public void setLastBalanceReport(long lastBalanceReport) {
		this.lastBalanceReport = lastBalanceReport;
	}
	public int getGameLength() {
		return GameLength;
	}
	public void setGameLength(int gameLength) {
		GameLength = gameLength;
	}
	public int getGameTime() {
		return GameTime;
	}
	public void setGameTime(int gameTime) {
		GameTime = gameTime;
	}
	public int getScoreBlue() {
		return ScoreBlue;
	}
	public void setScoreBlue(int scoreBlue) {
		ScoreBlue = scoreBlue;
	}
	public int getScoreRed() {
		return ScoreRed;
	}
	public void setScoreRed(int scoreRed) {
		ScoreRed = scoreRed;
	}
	public int getKillsBlue() {
		return KillsBlue;
	}
	public void setKillsBlue(int killsBlue) {
		KillsBlue = killsBlue;
	}
	public int getKillsRed() {
		return KillsRed;
	}
	public void setKillsRed(int killsRed) {
		KillsRed = killsRed;
	}
	public void addKillToTeam(PlayerTeam tm) {
		switch(tm) {
		case BLUE:
			KillsBlue++;
			break;
		case RED:
			KillsRed++;
			break;
		}
	}
	public Map<String, Integer> getLateArriver() {
		return LateArriver;
	}
	public void setLateArriver(Map<String, Integer> lateArriver) {
		LateArriver = lateArriver;
	}
	public BlockCoordinates getHighScoreSignA() {
		return HighScoreSignA;
	}
	public void setHighScoreSignA(BlockCoordinates highScoreSignA) {
		HighScoreSignA = highScoreSignA;
	}
	public BlockCoordinates getHighScoreSignB() {
		return HighScoreSignB;
	}
	public void setHighScoreSignB(BlockCoordinates highScoreSignB) {
		HighScoreSignB = highScoreSignB;
	}
	public BlockCoordinates getHighScoreSignC() {
		return HighScoreSignC;
	}
	public void setHighScoreSignC(BlockCoordinates highScoreSignC) {
		HighScoreSignC = highScoreSignC;
	}
	
}
