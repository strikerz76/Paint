package ok.kpaint;

import java.awt.*;

public class Brush {
	
	private int brushSize;
	private BrushShape shape;
	private BrushMode brushMode;
	private Color color;
	
	public Brush(int brushSize, BrushShape shape, BrushMode mode, Color color) {
		this.brushSize = brushSize;
		this.shape = shape;
		this.brushMode = mode;
		this.color = color;
	}

	public BrushShape getShape() {
		return shape;
	}

	public void setShape(BrushShape shape) {
		this.shape = shape;
	}

	public int getBrushSize() {
		return brushSize;
	}

	public void setBrushSize(int brushSize) {
		this.brushSize = brushSize;
	}

	public BrushMode getMode() {
		return brushMode;
	}

	public void setMode(BrushMode brushMode) {
		this.brushMode = brushMode;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
}
