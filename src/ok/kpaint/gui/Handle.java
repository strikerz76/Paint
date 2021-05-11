package ok.kpaint.gui;

import java.awt.*;
import java.awt.image.*;

import ok.kpaint.*;

public class Handle {
	private static final Toolkit toolkit = Toolkit.getDefaultToolkit();

	public static final Handle MOVE_NORTH = new Handle(HandleType.MOVE, Direction.ALL);	
	
	public static final Handle RESIZE_NORTH = new Handle(HandleType.RESIZE, Direction.NORTH);	
	public static final Handle RESIZE_SOUTH = new Handle(HandleType.RESIZE, Direction.SOUTH);
	public static final Handle RESIZE_EAST = new Handle(HandleType.RESIZE, Direction.EAST);
	public static final Handle RESIZE_WEST = new Handle(HandleType.RESIZE, Direction.WEST);
	
	public static final Handle STRETCH_NORTH = new Handle(HandleType.STRETCH, Direction.NORTH);
	public static final Handle STRETCH_SOUTH = new Handle(HandleType.STRETCH, Direction.SOUTH);
	public static final Handle STRETCH_EAST = new Handle(HandleType.STRETCH, Direction.EAST);
	public static final Handle STRETCH_WEST = new Handle(HandleType.STRETCH, Direction.WEST);
	
	
	public final HandleType type;
	public final Direction direction;
	public final BufferedImage image;
	public final Cursor cursor;
	public Handle(HandleType type, Direction direction) {
		this.type = type;
		this.direction = direction;
		image = pickImage();
		cursor = toolkit.createCustomCursor(image, new Point(image.getWidth()/2, image.getHeight()/2), type + "_" + direction);
	}
	
	private BufferedImage pickImage() {
		if(direction == Direction.NORTH) {
			return type.sideIcon;
		}
		else if(direction == Direction.EAST) {
			return Utils.rotateImage(type.sideIcon, 90);
		}
		else if(direction == Direction.SOUTH) {
			return Utils.rotateImage(type.sideIcon, 180);
		}
		else if(direction == Direction.WEST) {
			return Utils.rotateImage(type.sideIcon, 270);
		}
		return type.sideIcon;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Handle other = (Handle) obj;
		if (direction != other.direction)
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	
}
