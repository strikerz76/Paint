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
	
	public Vec2i subtract(Vec2i other) {
		return new Vec2i(x - other.x, y - other.y);
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
