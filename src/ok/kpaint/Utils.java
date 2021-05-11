package ok.kpaint;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

public class Utils {
	
	public enum Edge {
		NORTH(Cursor.N_RESIZE_CURSOR), 
//		NORTHEAST(Cursor.NE_RESIZE_CURSOR), 
//		NORTHWEST(Cursor.NW_RESIZE_CURSOR), 
		EAST(Cursor.E_RESIZE_CURSOR), 
		SOUTH(Cursor.S_RESIZE_CURSOR), 
//		SOUTHEAST(Cursor.SE_RESIZE_CURSOR), 
//		SOUTHWEST(Cursor.SW_RESIZE_CURSOR), 
		WEST(Cursor.W_RESIZE_CURSOR), 
		INSIDE(Cursor.MOVE_CURSOR), 
		OUTSIDE(Cursor.DEFAULT_CURSOR);
		
		private int cursorType;
		private Edge(int cursorType) {
			this.cursorType = cursorType;
		}
		
		public int getCursorType() {
			return cursorType;
		}
	}
	
	public static final Edge isNearEdge(Point point, Rectangle rectangle) {
		if(rectangle.contains(point)) {
			return Edge.INSIDE;
		}
		int buffer = 10;
		Edge edge = Edge.OUTSIDE;
		if(point.y > rectangle.y - buffer && point.y < rectangle.y + rectangle.height + buffer) {
			if(point.x < rectangle.x && point.x > rectangle.x - buffer) {
				edge = Edge.WEST;
			}
			else if(point.x > rectangle.x + rectangle.width && point.x < rectangle.x + rectangle.width + buffer) {
				edge = Edge.EAST;
			}
		}
		if(point.x > rectangle.x - buffer && point.x < rectangle.x + rectangle.width + buffer) {
			if(point.y < rectangle.y && point.y > rectangle.y - buffer) {
//				if(edge == Edge.WEST) {
//					edge = Edge.NORTHWEST;
//				}
//				else if(edge == Edge.EAST) {
//					edge = Edge.NORTHEAST;
//				}
//				else {
					edge = Edge.NORTH;
//				}
			}
			if(point.y > rectangle.y + rectangle.height && point.y < rectangle.y + rectangle.height + buffer) {
//				if(edge == Edge.WEST) {
//					edge = Edge.SOUTHWEST;
//				}
//				else if(edge == Edge.EAST) {
//					edge = Edge.SOUTHEAST;
//				}
//				else {
					edge = Edge.SOUTH;
//				}
			}
		}
		return edge;
	}
	
	public static final Image getImageFromClipboard() {
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
			} catch (UnsupportedFlavorException e) {
				// handle this as desired
				e.printStackTrace();
			} catch (IOException e) {
				// handle this as desired
				e.printStackTrace();
			}
		} else {
			System.err.println("getImageFromClipboard: That wasn't an image!");
		}
		return null;
	}
	
	public static final Color getBestTextColor(Color c) {
		return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
	}
	
	public static final ImageIcon loadImageIconResource(String location) {
		URL iconURL = DriverKPaint.class.getResource(location);
		if (iconURL == null) {
			System.err.println("ERROR failed to get resource URL: \"" + location + "\"");
			return null;
		}
		return new ImageIcon(iconURL);
	}
	public static final BufferedImage loadImage(String location) {
		return toBufferedImage(loadImageIconResource(location).getImage());
	}
	
	public static final ImageIcon resizeImageIcon(ImageIcon icon, int width, int height) {
		Image image = icon.getImage(); // transform it 
		Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
		return new ImageIcon(newimg);  // transform it back
	}

	public static final BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();
		return bimage;
	}

	public static final BufferedImage copyImage(BufferedImage image) {
		BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = copy.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return copy;
	}
	
	public static final BufferedImage rotateImage(BufferedImage image, int angle) {
		BufferedImage rotated = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = rotated.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.translate(image.getWidth()/2, image.getHeight()/2);
		g.rotate(Math.toRadians(angle));
		g.translate(-image.getWidth()/2, -image.getHeight()/2);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return rotated;
	}
}
