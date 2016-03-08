package de.fastcrafter.castlewars;

import org.bukkit.Location;

public class CircularArea {
	private BlockCoordinates coords = new BlockCoordinates();
	private int Radius;
	
	public CircularArea() { 
		coords.setX(0);
		coords.setY(-1);
		coords.setZ(0);
		Radius = -1;
	}
	
	public CircularArea(int x,int y,int z,int r) { 
		coords.setX(x);
		coords.setY(y);
		coords.setZ(z);
		Radius = r;
	}

	public int getX() {
		return coords.getX();
	}

	public void setX(int x) {
		coords.setX(x);
	}

	public int getY() {
		return coords.getY();
	}

	public void setY(int y) {
		coords.setY(y);
	}

	public int getZ() {
		return coords.getZ();
	}

	public void setZ(int z) {
		coords.setZ(z);
	}

	public int getRadius() {
		return Radius;
	}

	public void setRadius(int radius) {
		Radius = radius;
	}
	
	public void setRadius(Location loc) {
		Radius = coords.getDistance2D(loc);
	}
	
	public boolean isInArena(Location loc) { 
		return coords.getDistance2D(loc) <= Radius;
	}
	
	
	public boolean isInArena(BlockCoordinates loc) { 
		return coords.getDistance2D(loc) <= Radius;
	}
	
	public boolean isValid() {
		return coords.isValid() && Radius > 0;
	}
	
	public BlockCoordinates getBlockCoordinates() { 
		return coords;
	}
	
	public boolean isColliding(CircularArea are) {
		if (!are.isValid() || !isValid()) {
			return false;
		}
		
		if (coords.equalTo(are.getBlockCoordinates()) && Radius == are.getRadius()) {
			return false; //dont match same areas
		}
		
		
		if (coords.getDistance2D(are.getBlockCoordinates()) - are.getRadius() <= Radius) {
			return true;
		}else{
			return false;
		}
	}
	
	public void reset() { 
		coords.setX(0);
		coords.setY(-1);
		coords.setZ(0);
		Radius = -1;
	}
}
