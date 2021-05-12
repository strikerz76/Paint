package ok.kpaint.gui.layers;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

import ok.kpaint.*;
import ok.kpaint.gui.*;

public class Layer {
	
	private static final int DEFAULT_SIZE = 256;
	
	private static int idCounter = 0;
	private final int id;

	private BufferedImage image;
	private Vec2i position = new Vec2i();
	
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
	public Layer(Layer layer) {
		this.id = idCounter++;
		this.image = layer.image;
		this.shown = layer.shown;
		this.position = layer.position;
	}
	
	public void translate(Vec2i delta) {
		position.x += delta.x;
		position.y += delta.y;
	}
	
	public void centerAround(Vec2i center) {
		position.x = center.x - w()/2;
		position.y = center.y - h()/2;
	}
	
	public void setPosition(Vec2i position) {
		this.position = position;
	}
	
	/**
	 * 	this is called when state changes on a command
	 */
	public void updateCommand(Command inprogressCommand) {
		Vec2i delta = inprogressCommand.mouseEndPixel.subtract(inprogressCommand.mouseStartPixel);
		if(inprogressCommand.handle.type == HandleType.MOVE) {
			translate(delta);
			inprogressCommand.mouseStartPixel = inprogressCommand.mouseEndPixel;
		}
	}
	
	public Rectangle bounds() {
		return new Rectangle(position.x, position.y, image.getWidth(), image.getHeight());
	}
	public Rectangle getBoundsAfterCommand(Command command) {
		Vec2i delta = command.mouseEndPixel.subtract(command.mouseStartPixel);
		Rectangle newSize = new Rectangle(position.x, position.y, image.getWidth(), image.getHeight());
		if(command.handle.direction == Direction.NORTH
				|| command.handle.direction == Direction.NORTHEAST
				|| command.handle.direction == Direction.NORTHWEST) {
			newSize.y += delta.y;
			newSize.height -= delta.y;
		}
		if(command.handle.direction == Direction.EAST
				|| command.handle.direction == Direction.NORTHEAST
				|| command.handle.direction == Direction.SOUTHEAST) {
			newSize.width += delta.x;
		}
		if(command.handle.direction == Direction.SOUTH
				|| command.handle.direction == Direction.SOUTHEAST
				|| command.handle.direction == Direction.SOUTHWEST) {
			newSize.height += delta.y;
		}
		if(command.handle.direction == Direction.WEST
				|| command.handle.direction == Direction.NORTHWEST
				|| command.handle.direction == Direction.SOUTHWEST) {
			newSize.x += delta.x;
			newSize.width -= delta.x;
		}
		return newSize;
	}
	/**
	 * 	this is called when a command is completed (usually when mouse is released)
	 */
	public void applyCommand(Command command, Color altColor) {
		Vec2i delta = command.mouseEndPixel.subtract(command.mouseStartPixel);
		if(command.handle.type == HandleType.MOVE) {
			translate(delta);
		}
		else {
			Rectangle newSize = getBoundsAfterCommand(command);
			if(command.handle.type == HandleType.RESIZE) {
				resize(newSize, altColor);
			}
			else if(command.handle.type == HandleType.STRETCH) {
				stretch(newSize);
			}
		}
	}
	
	public void stretch(Rectangle newSize) {
		BufferedImage newImage = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = newImage.createGraphics();
		g.drawImage(image, 0, 0, newSize.width, newSize.height, null);
		g.dispose();
		this.image = newImage;
		position.x = newSize.x;
		position.y = newSize.y;
	}
	
	public void resize(Rectangle newSize, Color altColor) {
		BufferedImage newImage = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = newImage.createGraphics();
		g.setColor(altColor);
		Vec2i deltaPos = position.subtract(new Vec2i(newSize.x, newSize.y));
		g.fillRect(0, 0, deltaPos.x, newImage.getHeight());
		g.fillRect(0, 0, newImage.getWidth(), deltaPos.y);
		g.fillRect(deltaPos.x + image.getWidth(), 0, newImage.getWidth() - image.getWidth(), newImage.getHeight());
		g.fillRect(0, deltaPos.y + image.getHeight(), newImage.getWidth(), newImage.getHeight());
		g.drawImage(image, deltaPos.x, deltaPos.y, null);
		g.dispose();
		this.image = newImage;
		position.x = newSize.x;
		position.y = newSize.y;
	}
	
	public void reflectImage(boolean horizontal) {
		image = Utils.createFlipped(image, !horizontal);
	}
	
	public void draw(Vec2i pixel, Brush brush) {
		
		Vec2i drawAt = pixel.subtract(position);//new Vec2i(pixel.x - position.x, pixel.y - position.y);
		
		if (brush.getMode() == BrushMode.ALL_MATCHING_COLOR) {
			matchColorDraw(drawAt, brush);
		} 
		else if (brush.getMode() == BrushMode.FILL) {
			fill(drawAt, brush);
		}
		else if (brush.getMode() == BrushMode.BRUSH) {
			brush(drawAt, brush);
		}
	}
	
	private void brush(Vec2i center, Brush brush) {
		Point lowerBound = new Point(center.x - brush.getSize()/2, center.y - brush.getSize()/2);
		Point upperBound = new Point(lowerBound.x + brush.getSize() - 1, lowerBound.y + brush.getSize() - 1);
		lowerBound.x = Math.max(lowerBound.x, 0);
		lowerBound.y = Math.max(lowerBound.y, 0);
		upperBound.x = Math.min(upperBound.x, image.getWidth()-1);
		upperBound.y = Math.min(upperBound.y, image.getHeight()-1);
		
		int radius = brush.getSize()/2;
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
	private void fill(Vec2i center, Brush brush) {
		Point lowerBound = new Point(center.x - brush.getSize()/2, center.y - brush.getSize()/2);
		Point upperBound = new Point(lowerBound.x + brush.getSize() - 1, lowerBound.y + brush.getSize() - 1);
		lowerBound.x = Math.max(lowerBound.x, 0);
		lowerBound.y = Math.max(lowerBound.y, 0);
		upperBound.x = Math.min(upperBound.x, image.getWidth()-1);
		upperBound.y = Math.min(upperBound.y, image.getHeight()-1);
		
		HashSet<Integer> colors = new HashSet<>();
		HashSet<Vec2i> visited = new HashSet<>();
		LinkedList<Vec2i> search = new LinkedList<>();

		int radius = brush.getSize()/2;
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
				
				Vec2i start = new Vec2i(i, j);
				search.add(start);
				colors.add(image.getRGB(i, j));
				visited.add(start);
			}
		}
		
		while (!search.isEmpty()) {
			Vec2i pixel = search.removeFirst();
			image.setRGB(pixel.x, pixel.y, brush.getColor().getRGB());
			
			Vec2i[] neighbors = new Vec2i[] {
					new Vec2i(pixel.x, pixel.y - 1),
					new Vec2i(pixel.x, pixel.y + 1),
					new Vec2i(pixel.x - 1, pixel.y),
					new Vec2i(pixel.x + 1, pixel.y)
			};
			for(Vec2i neighbor : neighbors) {
				if(!visited.contains(neighbor) && neighbor.x >= 0 && neighbor.y >= 0 && neighbor.x < image.getWidth() && neighbor.y < image.getHeight()) {
					visited.add(neighbor);
					if (colors.contains(image.getRGB(neighbor.x, neighbor.y))) {
						search.add(neighbor);
					}
				}
			}
		}
	}

	private void matchColorDraw(Vec2i center, Brush brush) {
		Point lowerBound = new Point(center.x - brush.getSize()/2, center.y - brush.getSize()/2);
		Point upperBound = new Point(lowerBound.x + brush.getSize() - 1, lowerBound.y + brush.getSize() - 1);
		lowerBound.x = Math.max(lowerBound.x, 0);
		lowerBound.y = Math.max(lowerBound.y, 0);
		upperBound.x = Math.min(upperBound.x, image.getWidth()-1);
		upperBound.y = Math.min(upperBound.y, image.getHeight()-1);
		
		HashSet<Integer> colors = new HashSet<>();
		for(int i = lowerBound.x; i <= upperBound.x; i++) {
			for(int j = lowerBound.y; j <= upperBound.y; j++) {
				colors.add(image.getRGB(i, j));
			}
		}
		if(colors.isEmpty()) {
			return;
		}
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				if(colors.contains(image.getRGB(i, j))) {
					image.setRGB(i, j, brush.getColor().getRGB());
				}
			}
		}
	}
	
	public void erase(Rectangle rect, Color color) {
		Vec2i lowerBound = new Vec2i(rect.x, rect.y).subtract(position);
		Vec2i upperBound = new Vec2i(lowerBound.x + rect.width, lowerBound.y + rect.height);
		lowerBound.x = Math.max(lowerBound.x, 0);
		lowerBound.y = Math.max(lowerBound.y, 0);
		upperBound.x = Math.min(upperBound.x, image.getWidth());
		upperBound.y = Math.min(upperBound.y, image.getHeight());
		for (int i = lowerBound.x; i < upperBound.x; i++) {
			for (int j = lowerBound.y; j < upperBound.y; j++) {
				image.setRGB(i, j, color.getRGB());
			}
		}
	}
	
	public void toggleShown() {
		shown = !shown;
	}
	public void setShown(boolean shown) {
		this.shown = shown;
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
		return position.x;
	}
	
	public int y() {
		return position.y;
	}
	
	public int id() {
		return id;
	}
	
	@Override
	public String toString() {
		return "L" + id() + ",x:" + x() + ",y:" + y() + ",w:" + w() + ",h:" + h();
	}
}
