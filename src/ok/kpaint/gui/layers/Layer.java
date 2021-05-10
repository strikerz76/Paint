package ok.kpaint.gui.layers;

import java.awt.*;
import java.awt.image.*;

import ok.kpaint.*;

public class Layer {
	
	private static final int DEFAULT_SIZE = 256;
	
	private static int idCounter = 0;
	private final int id;

	private BufferedImage image;
	private int x, y;
	
	private boolean shown;
	
	public Layer() {
		this.id = idCounter++;
		image = new BufferedImage(DEFAULT_SIZE, DEFAULT_SIZE, BufferedImage.TYPE_INT_ARGB);
		shown = true;
	}
	public Layer(BufferedImage image) {
		this.id = idCounter++;
		this.image = image;
		shown = true;
	}
	
	public void centerAround(Pixel center) {
		x = center.x - w()/2;
		y = center.y - h()/2;
	}
	
	public void resize(Rectangle newSize, Color altColor) {
		BufferedImage newImage = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = newImage.createGraphics();
		g.setColor(altColor);
		g.fillRect(0, 0, x-newSize.x, newImage.getHeight());
		g.fillRect(0, 0, newImage.getWidth(), y-newSize.y);
		g.fillRect(-newSize.x + image.getWidth(), 0, newImage.getWidth(), newImage.getHeight());
		g.fillRect(0, -newSize.y + image.getHeight(), newImage.getWidth(), newImage.getHeight());
		g.drawImage(image, -newSize.x, -newSize.y, null);
		g.dispose();
//		if(selectedRectangle != null) {
//			selectedRectangle.x -= newSize.x;
//			selectedRectangle.y -= newSize.y;
//		}
//		xOffset += newSize.x*pixelSize;
//		yOffset += newSize.y*pixelSize;
	}
	
	public void draw(Pixel pixel, Brush brush) {
		
		Pixel drawAt = new Pixel(pixel.x - x, pixel.y - y);
		
//		if (brush.getMode() == BrushMode.ALL_MATCHING_COLOR) {
//			matchColorDraw(lowerBound, upperBound, color);
//		} 
//		else if (brush.getMode() == BrushMode.FILL) {
//			fill(lowerBound, upperBound, color);
//		}
//		else if (brush.getMode() == BrushMode.BRUSH) {
			brush(drawAt, brush);
//		}
	}
	
	private void brush(Pixel center, Brush brush) {
		Point lowerBound = new Point(center.x - brush.getBrushSize()/2, center.y - brush.getBrushSize()/2);
		Point upperBound = new Point(lowerBound.x + brush.getBrushSize() - 1, lowerBound.y + brush.getBrushSize() - 1);

		lowerBound.x = Math.max(lowerBound.x, 0);
		lowerBound.y = Math.max(lowerBound.y, 0);
		
		upperBound.x = Math.min(upperBound.x, image.getWidth()-1);
		upperBound.y = Math.min(upperBound.y, image.getHeight()-1);
		
		int radius = brush.getBrushSize()/2;
		int maxdistance = radius*radius;
		for(int i = lowerBound.x; i <= upperBound.x; i++) {
			for(int j = lowerBound.y; j <= upperBound.y; j++) {
				if(brush.getShape() == BrushShape.CIRCLE) {
					double distance = (i - center.x)*(i - center.x) 
							+ (j - center.y)*(j - center.y);
					if(distance > maxdistance) {
						continue;
					}
				}
				image.setRGB(i, j, brush.getColor().getRGB());
			}
		}
	}
	
	public void toggleShown() {
		shown = !shown;
	}
	
	public boolean shown() {
		return shown;
	}
	
	public BufferedImage image() {
		return image;
	}
	
	public int w() {
		return image.getWidth();
	}
	
	public int h() {
		return image.getHeight();
	}
	
	public int x() {
		return x;
	}
	
	public int y() {
		return y;
	}
	
	public int id() {
		return id;
	}
	
	@Override
	public String toString() {
		return "L" + id() + ",x:" + x() + ",y:" + y() + ",w:" + w() + ",h:" + h();
	}
}
