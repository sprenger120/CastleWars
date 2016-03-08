package de.fastcrafter.castlewars;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockCoordinates {
	private int X = 0;
	private int Y = -1;
	private int Z = 0;

	public BlockCoordinates() {
	}

	public BlockCoordinates(int x, int y, int z) {
		X = x;
		Y = y;
		Z = z;
	}

	public BlockCoordinates(Location loc) {
		set(loc);
	}

	public void set(Location loc) {
		X = loc.getBlockX();
		Y = loc.getBlockY();
		Z = loc.getBlockZ();
	}

	public int getX() {
		return X;
	}

	public void setX(int x) {
		X = x;
	}

	public int getY() {
		return Y;
	}

	public void setY(int y) {
		Y = y;
	}

	public int getZ() {
		return Z;
	}

	public void setZ(int z) {
		Z = z;
	}

	public int getDistance2D(Location loc) {
		return (int) Math.sqrt((loc.getX() - (double) X) * (loc.getX() - (double) X) + (loc.getZ() - (double) Z) * (loc.getZ() - (double) Z));
	}

	public int getDistance2D(BlockCoordinates loc) {
		return (int) Math.sqrt((double) ((loc.getX() - X) * (loc.getX() - X) + (loc.getZ() - Z) * (loc.getZ() - Z)));
	}

	public int getDistance2D(int x, int z) {
		return (int) Math.sqrt((double) (x - X) * (x - X) + (z - Z) * (z - Z));
	}

	public boolean isValid() {
		return Y >= 0;
	}

	public boolean equalTo(BlockCoordinates c) {
		if (c.getX() == X && c.getY() == Y && c.getZ() == Z) {
			return true;
		} else {
			return false;
		}
	}

	public boolean equalTo(Block blk) {
		if (blk.getX() == X && blk.getY() == Y && blk.getZ() == Z) {
			return true;
		} else {
			return false;
		}
	}

	public boolean equalTo(Location loc) {
		return loc.getBlockX() == X && loc.getBlockY() == Y && loc.getBlockZ() == Z;
	}
	
	public Location getLocation(World wrld) {
		return new Location(wrld, (double) X, (double) Y, (double) Z);
	}

	public BlockCoordinates clone() {
		return new BlockCoordinates(X, Y, Z);
	}

	public void reset() {
		X = 0;
		Y = -1;
		Z = 0;
	}
	
	public String toString() {
		return "X:" + X + " Y:" + Y + " Z:" + Z;
	}
}
