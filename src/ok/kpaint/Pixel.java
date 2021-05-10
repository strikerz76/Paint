package ok.kpaint;

import java.awt.*;

public class Pixel {
	public int x;
	public int y;
	public Pixel(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public Pixel(Point p) {
		this.x = p.x;
		this.y = p.y;
	}
	@Override
	public boolean equals(Object other) {
		if(other instanceof Pixel) {
			Pixel pixel = (Pixel)other;
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
