package ok.kpaint;

import javax.swing.*;

public enum BrushMode {
//	MOVE("Move", "(m) Move: use the mouse to move and resize canvas", "resources/move.png"), 
	SELECT("Select", "(s) Select: draw a rectangle to select a section of the image", "resources/select.png"), 
	BRUSH("Brush", "(b) Brush: draw with the mouse", "resources/brush.png"), 
	FILL("Fill", "(f) Fill: paints all of the adjacent pixels that have matching color", "resources/fill.png"), 
	ALL_MATCHING_COLOR("All Matching Color", "(a) All Matching Color: paints all pixels on the image that match the color", "resources/color.png"),
	COLOR_PICKER("Color Picker", "(p) Color Picker: choose a color from the image. (Hold shift to assign the alternate color)", "resources/color_picker.png"),
	;
	private String name;
	private String tooltipText;
	private ImageIcon image;
	BrushMode(String name, String tooltipText, String imageLocation) {
		this.name = name;
		this.tooltipText = tooltipText;
		this.image = Utils.resizeImageIcon(Utils.loadImageIconResource(imageLocation), 32, 32);
	}
	public ImageIcon getImageIcon() {
		return image;
	}
	public String getTooltipText() {
		return tooltipText;
	}
	@Override
	public String toString() {
		return name;
	}
}
