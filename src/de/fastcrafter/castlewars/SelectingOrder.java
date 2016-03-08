package de.fastcrafter.castlewars;

public class SelectingOrder {
	private SelectingOrderTypes Status;
	private String Arena = new String("");
	private int TowerID  = -1;
	
	public SelectingOrder(SelectingOrderTypes stat,String arena) {
		Status = stat;
		Arena = arena;
	}

	public SelectingOrderTypes getStatus() {
		return Status;
	}

	public void setStatus(SelectingOrderTypes status) {
		Status = status;
	}

	public String getArena() {
		return Arena;
	}

	public void setArena(String arena) {
		Arena = arena;
	}

	public int getTowerID() {
		return TowerID;
	}

	public void setTowerID(int towerID) {
		TowerID = towerID;
	}
}
