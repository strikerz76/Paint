package ok.kpaint;

import java.awt.*;

/**
 * Actions that can occur on the from the gui that need to have effect on the image panel.
 */
public interface ImagePanelInterface {

	public void undo();
	public void redo();
	public void resetView();
	public void applySelection();
	public void clearSelection();
	public void pasteFromClipboard();

	public void showTiling(boolean enabled);

	public Color getMainColor();
	public Color getAltColor();
	public void setMainColor(Color color1);
	public void setAltColor(Color color2);
	public void swapColors();

	public void newCanvas();
	
	public void setBrushSize(int size);
	public void setBrushShape(BrushShape shape);
	public void setBrushMode(BrushMode mode);

}
