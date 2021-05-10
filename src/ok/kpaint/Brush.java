package ok.kpaint;

import java.awt.*;

public class Brush {

	public static final Brush DEFAULT_BRUSH = new Brush(10, BrushShape.CIRCLE, BrushMode.BRUSH, Color.white);
	
	private int size;
	private BrushShape shape;
	private BrushMode mode;
	private Color color;
	
	public Brush(int brushSize, BrushShape shape, BrushMode mode, Color color) {
		this.size = brushSize;
		this.shape = shape;
		this.mode = mode;
		this.color = color;
	}
	public Brush(Brush other) {
		this(other.size, other.shape, other.mode, other.color);
	}

	public BrushShape getShape() {
		return shape;
	}

	public void setShape(BrushShape shape) {
		this.shape = shape;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int brushSize) {
		this.size = brushSize;
	}

	public BrushMode getMode() {
		return mode;
	}

	public void setMode(BrushMode brushMode) {
		this.mode = brushMode;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
}
