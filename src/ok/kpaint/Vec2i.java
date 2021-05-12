package ok.kpaint;

import java.awt.*;

public class Vec2i {
	public int x;
	public int y;
	public Vec2i() {
		this(0, 0);
	}
	public Vec2i(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public Vec2i(Point p) {
		this(p.x, p.y);
	}

	public Vec2i add(int xx, int yy) {
		return new Vec2i(x + xx, y + yy);
	}
	public Vec2i add(Vec2i other) {
		return new Vec2i(x + other.x, y + other.y);
	}
	
	public Vec2i subtract(Vec2i other) {
		return new Vec2i(x - other.x, y - other.y);
	}
	
	public double distanceTo(Vec2i other) {
		int dx = Math.abs(x - other.x);
		int dy = Math.abs(y - other.y);
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	
	@Override
	public boolean equals(Object other) {
		if(other instanceof Vec2i) {
			Vec2i pixel = (Vec2i)other;
			return this.x == pixel.x && this.y == pixel.y;
		}
		return false;
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	@Override
	public String toString() {
		return x + "," + y;
	}
}
