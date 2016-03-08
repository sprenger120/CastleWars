package de.fastcrafter.castlewars;

import java.util.Vector;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class TowerEntry {
	private CircularArea Area = new CircularArea();
	private int TowerID = -1;
	private Vector<BlockCoordinates> Dispensers = new Vector<BlockCoordinates>();
	private Vector<BlockCoordinates> WoolBlocks = new Vector<BlockCoordinates>();
	private float CaptureProgress = 0;  //0 = Neutral ;  -1  = blue  ;  1 = red
	private PlayerTeam Team = PlayerTeam.NONE;
	private PlayerTeam CappingTeam = PlayerTeam.NONE;
	private int MaxHeight = 0;
	
	public CircularArea getArea() {
		return Area;
	}
	public void setArea(CircularArea area) {
		Area = area;
	}
	public int getTowerID() {
		return TowerID;
	}
	public void setTowerID(int towerID) {
		TowerID = towerID;
	}
	public Vector<BlockCoordinates> getDispensers() {
		return Dispensers;
	}
	public void setDispensers(Vector<BlockCoordinates> dispensers) {
		Dispensers = dispensers;
	}

	public boolean hasDispenser(BlockCoordinates disCoord) {
		if (Dispensers.size() == 0) {return false;}
		for (int x = 0; x <= Dispensers.size() - 1; x++) {
			if (Dispensers.get(x).equalTo(disCoord)) {
				return true;
			}
		}
		return false;
	}
	
	public float getCaptureProgress() {
		return CaptureProgress;
	}
	public void setCaptureProgress(float captureProgress) {
		CaptureProgress = captureProgress;
	}
	public PlayerTeam getTeam() {
		return Team;
	}
	public void setTeam(PlayerTeam team) {
		Team = team;
	}
	public PlayerTeam getCappingTeam() {
		return CappingTeam;
	}
	public void setCappingTeam(PlayerTeam cappingTeam) {
		CappingTeam = cappingTeam;
	}
	public Vector<BlockCoordinates> getWoolBlocks() {
		return WoolBlocks;
	}
	public void setWoolBlocks(Vector<BlockCoordinates> woolBlocks) {
		WoolBlocks = woolBlocks;
	}
	
	public void searchWool(World wrld) {
		if (!Area.isValid()) {
			return;
		}
		Block blk = null;
		
		for (int x = Area.getBlockCoordinates().getX() - Area.getRadius();x<= Area.getBlockCoordinates().getX() + Area.getRadius();x++) {
			for (int z = Area.getBlockCoordinates().getZ() - Area.getRadius();z<= Area.getBlockCoordinates().getZ() + Area.getRadius();z++) {
			int highest = wrld.getHighestBlockYAt(x,z);
			if (highest > MaxHeight) {
				MaxHeight = highest;
			}
				for (int y=0;y<=highest;y++) {
					blk = wrld.getBlockAt(x, y, z);
					if (blk.getType() == Material.WOOL) {
						WoolBlocks.add(new BlockCoordinates(x,y,z));
					}
				}
			}
		}	
	}
	public int getMaxHeight() {
		return MaxHeight;
	}
	public void setMaxHeight(int maxHeight) {
		MaxHeight = maxHeight;
	}
}
