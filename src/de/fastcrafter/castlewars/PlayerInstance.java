package de.fastcrafter.castlewars;


public class PlayerInstance {
	private String Name = new String("");
	private PlayerTeam Team = PlayerTeam.NONE;
	private PlayerTeam FlagColor = PlayerTeam.NONE;
	private BlockCoordinates ItemStorage = new BlockCoordinates();
	private String gameInstance = new String("");
	private int LastMove = 0;
	private BlockCoordinates LastPosition = new BlockCoordinates();
	
	public PlayerInstance() { 
		Team = PlayerTeam.NONE; 
		FlagColor = PlayerTeam.NONE; 
	}
	
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public PlayerTeam getTeam() {
		return Team;
	}
	public void setTeam(PlayerTeam team) {
		Team = team;
	}

	public PlayerTeam getFlagColor() {
		return FlagColor;
	}

	public void setFlagColor(PlayerTeam flagColor) {
		FlagColor = flagColor;
	}

	public BlockCoordinates getItemStorage() {
		return ItemStorage;
	}

	public void setItemStorage(BlockCoordinates itemStorage) {
		ItemStorage = itemStorage;
	}

	public String getGameInstance() {
		return gameInstance;
	}

	public void setGameInstance(String gameInstance) {
		this.gameInstance = gameInstance;
	}

	public int getLastMove() {
		return LastMove;
	}

	public void setLastMove(int lastMove) {
		LastMove = lastMove;
	}

	public BlockCoordinates getLastPosition() {
		return LastPosition;
	}

	public void setLastPosition(BlockCoordinates lastPosition) {
		LastPosition = lastPosition;
	}
}