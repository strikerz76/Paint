package ok.kpaint;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import ok.kpaint.Utils.*;
import ok.kpaint.gui.layers.*;

public class ImagePanel extends JPanel implements LayersListener {
	
	private LinkedList<String> infoStrings = new LinkedList<>();

	private double zoom = 1;
	private Vec2i cameraOffset = new Vec2i();
	private Vec2i previousMousePosition = new Vec2i();
	
	private boolean movingCamera;
	private boolean movingLayer;
	private Edge resizingLayer;
	private Rectangle targetLayerSize = new Rectangle();
	private HashSet<Integer> mouseButtonsPressed = new HashSet<>();

	private Brush brush = new Brush(Brush.DEFAULT_BRUSH);
	private Color altColor = new Color(0, 0, 0, 0);
	private Layers layers;
	
	private boolean showTiling;
	
	private GUIInterface guiInterface;
	private ControllerInterface controllerInterface;
	private ImagePanelInterface ipInterface = new ImagePanelInterface() {
		@Override
		public void undo() {
//			history.rewindVersion();
			repaint();
		}
		@Override
		public void redo() {
//			history.upwindVersion();
			repaint();
		}
		@Override
		public void resetView() {
			ImagePanel.this.resetView();
		}
		@Override
		public void applySelection() {
			ImagePanel.this.applySelection();
		}
		@Override
		public void clearSelection() {
			ImagePanel.this.clearSelection();
		}
		@Override
		public void pasteFromClipboard() {
			ImagePanel.this.pasteFromClipboard();
		}
		@Override
		public Color getMainColor() {
			return brush.getColor();
		}
		@Override
		public Color getAltColor() {
			return altColor;
		}
		@Override
		public void setMainColor(Color mainColor) {
			brush.setColor(mainColor);
			guiInterface.changedColor(mainColor);
		}
		@Override
		public void setAltColor(Color altColor) {
			ImagePanel.this.altColor = altColor;
			guiInterface.changedColor(altColor);
		}
		@Override
		public void swapColors() {
			Color temp = brush.getColor();
			brush.setColor(altColor);
			altColor = temp;
			guiInterface.changedColor(altColor);
		}
		@Override
		public void newCanvas() {
			JPanel chooseSize = new JPanel();
			chooseSize.add(new JLabel("Width:"));
			JTextField widthField = new JTextField("" + getCurrentImage().getWidth(), 6);
			chooseSize.add(widthField);
			chooseSize.add(new JLabel("Height:"));
			JTextField heightField = new JTextField("" + getCurrentImage().getHeight(), 6);
			chooseSize.add(heightField);
			for(Component c : chooseSize.getComponents()) {
				c.setFont(DriverKPaint.MAIN_FONT);
			}
			int result = JOptionPane.showConfirmDialog(ImagePanel.this, chooseSize, "New Canvas", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if(result == JOptionPane.OK_OPTION) {
				try {
					int width = Integer.parseInt(widthField.getText());
					int height = Integer.parseInt(heightField.getText());
					resetImage(width, height);
				}
				catch(NumberFormatException e) {
					JLabel l = new JLabel("Width and height must be integers.");
					l.setFont(DriverKPaint.MAIN_FONT);
					JOptionPane.showMessageDialog(ImagePanel.this, l, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		@Override
		public void showTiling(boolean enabled) {
			showTiling = enabled;
		}
		@Override
		public void setBrushSize(int size) {
			brush.setSize(size);
			repaint();
		}
		@Override
		public void setBrushShape(BrushShape shape) {
			brush.setShape(shape);
		}
		@Override
		public void setBrushMode(BrushMode mode) {
			brush.setMode(mode);
		}
	};
	
	public ImagePanelInterface getInterface() {
		return ipInterface;
	}
	public ImagePanel(Layers layers) {
		this.layers = layers;
		layers.addListener(this);
		this.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoomCamera(e.getWheelRotation(), screenToPixel(e.getPoint()));
			}
		});
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.isControlDown()) {
					if(e.getKeyCode() == KeyEvent.VK_1) {
						guiInterface.switchLayout(true);
					}
					if(e.getKeyCode() == KeyEvent.VK_2) {
						guiInterface.switchLayout(false);
					}
					if(e.getKeyCode() == KeyEvent.VK_N) {
						ipInterface.newCanvas();
					}
					if(e.getKeyCode() == KeyEvent.VK_S) {
						controllerInterface.save();
					}
					if(e.getKeyCode() == KeyEvent.VK_V) {
						ipInterface.pasteFromClipboard();
					}
					else if(e.getKeyCode() == KeyEvent.VK_C) {
						ClipboardImage.setClipboard(getCurrentImage());
					}
					else if(e.getKeyCode() == KeyEvent.VK_D) {
						duplicateLayer();
					}
					if(e.getKeyCode() == KeyEvent.VK_Z) {
						if(e.isShiftDown()) {
							ipInterface.redo();
						}
						else {
							ipInterface.undo();
						}
					}
					if(e.getKeyCode() == KeyEvent.VK_A) {
						selectAll();
						updateSelection();
						guiInterface.finishedSelection();
					}
				}
				else {
					if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						ipInterface.applySelection();
					}
					else if(e.getKeyCode() == KeyEvent.VK_DELETE) {
						ipInterface.clearSelection();
					}
					else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
						ipInterface.resetView();
					}
					else if(e.getKeyCode() == KeyEvent.VK_SHIFT) {
						ipInterface.swapColors();
					}
					else if(e.getKeyCode() == KeyEvent.VK_P) {
						guiInterface.changeModeHotkey(BrushMode.COLOR_PICKER);
					}
					else if(e.getKeyCode() == KeyEvent.VK_S) {
						guiInterface.changeModeHotkey(BrushMode.SELECT);
					}
					else if(e.getKeyCode() == KeyEvent.VK_B) {
						guiInterface.changeModeHotkey(BrushMode.BRUSH);
					}
					else if(e.getKeyCode() == KeyEvent.VK_F) {
						guiInterface.changeModeHotkey(BrushMode.FILL);
					}
					else if(e.getKeyCode() == KeyEvent.VK_A) {
						guiInterface.changeModeHotkey(BrushMode.ALL_MATCHING_COLOR);
					}
				}
			}
		});
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
//				mousePosition = e.getPoint();
				mouseButtonsPressed.add(e.getButton());
				if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					startMovingCanvas(e.getPoint());
				}
				else if(brush.getMode() == BrushMode.SELECT) {
					resetSelection();
//					startedSelection = e.getPoint();
					updateSelectionRectangle(e.getPoint());
				}
				else if(brush.getMode() == BrushMode.COLOR_PICKER) {
					colorPicker(screenToPixel(e.getPoint()), e.isShiftDown());
				}
				else {
					draw(screenToPixel(e.getPoint()), e.isShiftDown());
				}
				previousMousePosition = new Vec2i(e.getPoint());
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
//				mousePosition = e.getPoint();
				mouseButtonsPressed.remove(e.getButton());
				if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					System.out.println("finishMovingCanvas");
					finishMovingCanvas();
				}
//				else if(brush.getMode() == BrushMode.MOVE) {
//					System.out.println("finishMovingSelection & finishMovingCanvas");
//					finishMovingSelection();
//					finishMovingCanvas();
//				}
				else if(brush.getMode() == BrushMode.SELECT) {
					System.out.println("finish selection");
					updateSelectionRectangle(e.getPoint());
					updateSelection();
					guiInterface.finishedSelection();
				}
				else {
//					history.pushVersion();
					repaint();
				}
				previousMousePosition = new Vec2i(e.getPoint());
			}
			@Override
			public void mouseEntered(MouseEvent e) {
//				mousePosition = e.getPoint();
				repaint();
			}
			@Override
			public void mouseExited(MouseEvent e) {
//				mousePosition = null;
				repaint();
			}
		});
		this.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (movingCamera || movingLayer || resizingLayer != null) {
					updateCanvasMove(previousMousePosition, new Vec2i(e.getPoint()));
				}
//				else if(movingSelection != null && movingSelection != Edge.OUTSIDE) {
//					Point previousPos = getPixelPosition(previousMousePosition);
//					Point newPos = getPixelPosition(e.getPoint());
//					updateSelectionMove(previousPos, newPos);
//				}
				else if(brush.getMode() == BrushMode.SELECT) {
					updateSelectionRectangle(e.getPoint());
				}
				else {
					draw(screenToPixel(e.getPoint()), e.isShiftDown());
				}
				previousMousePosition = new Vec2i(e.getPoint());
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				int newCursorType = Cursor.DEFAULT_CURSOR;
				if(brush.getMode() == BrushMode.SELECT) {
					newCursorType = Cursor.CROSSHAIR_CURSOR;
				}
				Rectangle canvasRect = getCanvasScreenRectangle();
//				if(brush.getMode() == BrushMode.MOVE) {
					Edge canvasEdge = Utils.isNearEdge(e.getPoint(), canvasRect);
					if(canvasEdge != Edge.OUTSIDE && canvasEdge != Edge.INSIDE) {
						newCursorType = canvasEdge.getCursorType();
					}
//				}
//				if(brush.getMode() == BrushMode.MOVE && selectedRectangle != null) {
//				if(brush.getMode() == BrushMode.MOVE && selectedRectangle != null) {
//					Rectangle selectionRect = getSelectionScreenRectangle();
//					Edge selectionEdge = Utils.isNearEdge(e.getPoint(), selectionRect);
//					if(selectionEdge != Edge.OUTSIDE) {
//						newCursorType = selectionEdge.getCursorType();
//					}
//				}
//				}
				ImagePanel.this.setCursor(new Cursor(newCursorType));
				previousMousePosition = new Vec2i(e.getPoint());
				repaint();
			}
		});
	}

	private static BufferedImage createFlipped(BufferedImage image, boolean northsouth) {
		AffineTransform at = new AffineTransform();
		if(northsouth) {
			at.concatenate(AffineTransform.getScaleInstance(1, -1));
			at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		}
		else {
			at.concatenate(AffineTransform.getScaleInstance(-1, 1));
			at.concatenate(AffineTransform.getTranslateInstance(-image.getWidth(), 0));
		}
		return createTransformed(image, at);
	}

	private static BufferedImage createRotated(BufferedImage image) {
		AffineTransform at = AffineTransform.getRotateInstance(Math.PI, image.getWidth() / 2, image.getHeight() / 2.0);
		return createTransformed(image, at);
	}

	private static BufferedImage createTransformed(BufferedImage image, AffineTransform at) {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImage.createGraphics();
		g.transform(at);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}
	
	private void startMovingCanvas(Point mousePosition) {
		Rectangle canvas = getCanvasScreenRectangle();
		Edge edge = Utils.isNearEdge(mousePosition, canvas);
		if(edge == Edge.OUTSIDE || edge == Edge.INSIDE) {
			movingCamera = true;
			ImagePanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
//		else if(edge == Edge.INSIDE) {
//			movingLayer = true;
//			ImagePanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
//		}
		else {
			targetLayerSize.x = layers.active().x();
			targetLayerSize.y = layers.active().y();
			targetLayerSize.width = layers.active().w();
			targetLayerSize.height = layers.active().h();
			resizingLayer = edge;
		}
	}
	
	private void updateCanvasMove(Vec2i previousMouse, Vec2i newMouse) {
		Vec2i previousPixel = screenToPixel(previousMouse);
		Vec2i newPixel = screenToPixel(newMouse);
		Vec2i deltaPixel = newPixel.subtract(previousPixel);
		if(movingCamera) {
			cameraOffset.x += newMouse.x - previousMouse.x;
			cameraOffset.y += newMouse.y - previousMouse.y;
		}
		else if(movingLayer) {
			layers.active().translate(deltaPixel);
		}
		else if(resizingLayer != null) {
			if(resizingLayer == Edge.EAST) {
				targetLayerSize.width += newPixel.x - previousPixel.x;
				targetLayerSize.width = Math.max(targetLayerSize.width, 1);
			}
			else if(resizingLayer == Edge.WEST) {
				int deltaWidth = previousPixel.x - newPixel.x;
				deltaWidth = Math.max(deltaWidth, -(targetLayerSize.width) + 1);
				targetLayerSize.width += deltaWidth;
				targetLayerSize.x -= deltaWidth;
			}
			else if(resizingLayer == Edge.SOUTH) {
				targetLayerSize.height += newPixel.y - previousPixel.y;
				targetLayerSize.height = Math.max(targetLayerSize.height, 1);
			}
			else if(resizingLayer == Edge.NORTH) {
				int deltaHeight = previousPixel.y - newPixel.y;
				deltaHeight = Math.max(deltaHeight, -(targetLayerSize.height) + 1);
				targetLayerSize.height += deltaHeight;
				targetLayerSize.y -= deltaHeight;
			}
		}
	}
	
	private void finishMovingCanvas() {
		movingCamera = false;
		movingLayer = false;
		if(resizingLayer != null) {
			layers.active().resize(targetLayerSize, altColor);
//			resizeCanvas(targetCanvasSize);
//			history.pushVersion();
			repaint();
		}
		resizingLayer = null;
	}
	
	public void setGUIInterface(GUIInterface guiInterface) {
		this.guiInterface = guiInterface;
	}
	public void setControllerInterface(ControllerInterface controllerInterface) {
		this.controllerInterface = controllerInterface;
	}
	
	public void colorPicker(Vec2i pixel, boolean shiftDown) {
		Color selected = new Color(getCurrentImage().getRGB(pixel.x, pixel.y), true);
		if(shiftDown) {
			ipInterface.setAltColor(selected);
		}
		else {
			ipInterface.setMainColor(selected);
		}
		repaint();
	}
	public void draw(Vec2i currentPixel, boolean shiftDown) {
		Vec2i previousPixel = screenToPixel(previousMousePosition);
		int deltax = currentPixel.x - previousPixel.x;
		int deltay = currentPixel.y - previousPixel.y;
		if(Math.abs(deltax) <= 1 && Math.abs(deltay) <= 1) {
			drawOnPixel(currentPixel, shiftDown);
			return;
		}
		if(Math.abs(deltax) > Math.abs(deltay)) {
			if(currentPixel.x < previousPixel.x) {
				Vec2i temp = currentPixel;
				currentPixel = previousPixel;
				previousPixel = temp;
			}
			for(int x = previousPixel.x; x <= currentPixel.x; x++) {
				double ratio = (double)(x - previousPixel.x) / (currentPixel.x - previousPixel.x);
				int yy = (int) (previousPixel.y + (currentPixel.y - previousPixel.y) * ratio);
				drawOnPixel(new Vec2i(x, yy), shiftDown);
			}
		}
		else {
			if(currentPixel.y < previousPixel.y) {
				Vec2i temp = currentPixel;
				currentPixel = previousPixel;
				previousPixel = temp;
			}
			for(int y = previousPixel.y; y <= currentPixel.y; y++) {
				double ratio = (double)(y - previousPixel.y) / (currentPixel.y - previousPixel.y);
				int xx = (int)(previousPixel.x + (currentPixel.x - previousPixel.x) * ratio);
				drawOnPixel(new Vec2i(xx, y), shiftDown);
			}
		}
	}
	public void drawOnPixel(Vec2i pixel, boolean shiftDown) {
		layers.draw(pixel, brush);
		repaint();
	}
	
	public void updateSelectionRectangle(Point mousePosition) {
//		Point one = getPixelPosition(mousePosition);
//		Point two = getPixelPosition(startedSelection);
//		int minx = Math.max(Math.min(one.x, two.x), 0);
//		int miny = Math.max(Math.min(one.y, two.y), 0);
//		int maxx = Math.min(Math.max(one.x, two.x), layers.active().w()-1);
//		int maxy = Math.min(Math.max(one.y, two.y), layers.active().h()-1);
//		selectedRectangle = new Rectangle(minx, miny, maxx-minx, maxy-miny);
	}
	public void selectAll() {
//		selectedRectangle = new Rectangle(0, 0, getCurrentImage().getWidth()-1, getCurrentImage().getHeight()-1);
	}
	
	public void updateSelection() {
//		history.modified();
//		BufferedImage subimage = history.getCurrent().getSubimage(selectedRectangle.x, selectedRectangle.y, selectedRectangle.width + 1, selectedRectangle.height + 1);
//		selectedImage = Utils.copyImage(subimage);
//		brush(new Point(selectedRectangle.x, selectedRectangle.y), new Point(selectedRectangle.x+selectedRectangle.width, selectedRectangle.y + selectedRectangle.height), BrushShape.SQUARE, color2);
//		history.pushVersion();
//		repaint();
	}

//	public void resizeCanvas(Rectangle newSize) {
////		BufferedImage newImage = new BufferedImage(newSize.width, newSize.height, BufferedImage.TYPE_4BYTE_ABGR);
////		Graphics g = newImage.getGraphics();
////		g.setColor(color2);
////		g.fillRect(0, 0, -newSize.x, newImage.getHeight());
////		g.fillRect(0, 0, newImage.getWidth(), -newSize.y);
////		BufferedImage currentImage = history.getCurrent();
////		g.fillRect(-newSize.x + currentImage.getWidth(), 0, newImage.getWidth(), newImage.getHeight());
////		g.fillRect(0, -newSize.y + currentImage.getHeight(), newImage.getWidth(), newImage.getHeight());
////		g.drawImage(currentImage, -newSize.x, -newSize.y, null);
////		g.dispose();
////		history.setCurrentImage(newImage);
////		if(selectedRectangle != null) {
////			selectedRectangle.x -= newSize.x;
////			selectedRectangle.y -= newSize.y;
////		}
////		xOffset += newSize.x*pixelSize;
////		yOffset += newSize.y*pixelSize;
//	}
	
	private void pasteFromClipboard() {
//		Image image = Utils.getImageFromClipboard();
//		if(image != null) {
//			ipInterface.applySelection();
//			selectedImage = Utils.toBufferedImage(image); 
//			selectedRectangle = new Rectangle((int)((getWidth()/2-xOffset)/pixelSize - selectedImage.getWidth()/2), (int)((getHeight()/2-yOffset)/pixelSize - selectedImage.getHeight()/2), selectedImage.getWidth()-1, selectedImage.getHeight()-1);
//			repaint();
//		}
	}
	
	private void clearSelection() {
//		selectedImage = null;
//		selectedRectangle = null;
//		repaint();
	}
	
	private Rectangle getSelectionScreenRectangle() {
//		if(selectedRectangle != null) {
//			return new Rectangle((int) (selectedRectangle.x*pixelSize+xOffset), (int) (selectedRectangle.y*pixelSize+yOffset), (int) ((selectedRectangle.width+1)*pixelSize)-1, (int) ((selectedRectangle.height+1)*pixelSize)-1);
//		}
		return null;
	}
	
	private Rectangle getCanvasScreenRectangle() {
		return new Rectangle(
				(int)(cameraOffset.x + layers.active().x()*zoom), 
				(int)(cameraOffset.y + layers.active().y() * zoom), 
				(int)(layers.active().w()*zoom), 
				(int)(layers.active().h()*zoom));
		
//		return new Rectangle(xOffset, yOffset, (int) (getCurrentImage().getWidth()*pixelSize), (int) (getCurrentImage().getHeight()*pixelSize));
	}
	
	private Rectangle getCanvasSizeWithSelection() {
//		if(selectedRectangle == null || selectedImage == null) {
//			return new Rectangle(0, 0, getCurrentImage().getWidth(), getCurrentImage().getHeight());
//		}
//		int minx = Math.min(selectedRectangle.x, 0);
//		int miny = Math.min(selectedRectangle.y, 0);
//		int maxx = Math.max(selectedRectangle.x + selectedImage.getWidth(), getCurrentImage().getWidth());
//		int maxy = Math.max(selectedRectangle.y + selectedImage.getHeight(), getCurrentImage().getHeight());
//		int x = 0;
//		int y = 0;
//		if(selectedRectangle.x < 0) {
//			x = selectedRectangle.x;
//		}
//		if(selectedRectangle.y < 0) {
//			y = selectedRectangle.y;
//		}
//		return new Rectangle(x, y, maxx - minx, maxy - miny);
		return getCanvasScreenRectangle();
	}
	
	private void applySelection() {
//		if(selectedRectangle == null || selectedImage == null) {
//			return;
//		}
////		history.modified();
//		Rectangle newCanvasSize = getCanvasSizeWithSelection();
//		
//		if(newCanvasSize.width != getCurrentImage().getWidth() || newCanvasSize.height != getCurrentImage().getHeight()) {
//			System.out.println("resizing");
////			resizeCanvas(newCanvasSize);
//		}
//		Graphics g = getCurrentImage().getGraphics();
//		g.drawImage(selectedImage, selectedRectangle.x, selectedRectangle.y, selectedRectangle.width+1, selectedRectangle.height+1, null);
//		g.dispose();
//		resetSelection();
////		history.pushVersion();
//		repaint();
	}
	
	public void resetSelection() {
//		startedSelection = null;
//		selectedImage = null;
//		selectedRectangle = null;
	}
	
	public Vec2i screenToPixel(Point screenPos) {
		return screenToPixel(new Vec2i(screenPos));
	}
	public Vec2i screenToPixel(Vec2i screenPos) {
		Vec2i pixel = new Vec2i();
		pixel.x = (int) ((screenPos.x - cameraOffset.x)/zoom);
		pixel.y = (int) ((screenPos.y - cameraOffset.y)/zoom);
		if(screenPos.x - cameraOffset.x < 0) {
			pixel.x -= 1;
		}
		if(screenPos.y - cameraOffset.y < 0) {
			pixel.y -= 1;
		}
		return pixel;
	}
	
	public Vec2i pixelToScreen(Vec2i pixel) {
		return new Vec2i((int) (pixel.x * zoom + cameraOffset.x), (int) (pixel.y * zoom + cameraOffset.y));
	}

	public BufferedImage getCurrentImage() {
		return layers.compose();
	}

	public void addImageLayer(BufferedImage image) {
		layers.add(image);
		repaint();
	}
	
	private void resetView() {
		Rectangle bounds = layers.getBoundingRect();
		
		double xfit = 1.0*getWidth()/bounds.width;
		double yfit = 1.0*getHeight()/bounds.height;
		zoom = Math.min(xfit, yfit) * 0.95;
		cameraOffset.x = (int) (getWidth()/2 - zoom * bounds.width/2 - zoom*bounds.x);
		cameraOffset.y = (int) (getHeight()/2 - zoom * bounds.height/2 - zoom*bounds.y);
		repaint();
	}
	private void zoomCamera(int direction, Vec2i center) {
		double oldPixelSize = zoom;
		if (direction > 0) {
			zoom = zoom * 0.9;
			zoom = zoom < 0.01 ? 0.01 : zoom;
		} else {
			zoom = zoom*1.1 + 0.1;
		}
		double deltaPixelSize = zoom - oldPixelSize;
		cameraOffset.x = (int)(cameraOffset.x - deltaPixelSize * center.x);
		cameraOffset.y = (int)(cameraOffset.y - deltaPixelSize * center.y);
		repaint();
	}

	public void resetImage(int w, int h) {
		layers.deleteAll();
		BufferedImage defaultImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = defaultImage.getGraphics();
		g.setColor(altColor);
		g.fillRect(0, 0, defaultImage.getWidth(), defaultImage.getHeight());
		g.dispose();
		addImageLayer(defaultImage);
		resetView();
	}
	private void duplicateLayer() {
		layers.add(Utils.copyImage(layers.active().image()));
	}
	
	private LinkedList<Vec2i> getNeighbors(Vec2i pixel) {
		LinkedList<Vec2i> neighbors = new LinkedList<>();
		neighbors.add(new Vec2i(pixel.x - 1, pixel.y));
		neighbors.add(new Vec2i(pixel.x + 1, pixel.y));
		neighbors.add(new Vec2i(pixel.x, pixel.y - 1));
		neighbors.add(new Vec2i(pixel.x, pixel.y + 1));
		return neighbors;
	}
	
	private void brush(Point lowerBound, Point upperBound, BrushShape shape, Color setTo) {
//		int centerx = (upperBound.x + lowerBound.x)/2;
//		int centery = (upperBound.y + lowerBound.y)/2;
//		int radius = (upperBound.x - lowerBound.x)/2;
//		int maxdistance = radius*radius;
//		for(int i = lowerBound.x; i <= upperBound.x; i++) {
//			for(int j = lowerBound.y; j <= upperBound.y; j++) {
//				if(shape == BrushShape.CIRCLE) {
//					double distance = (i - centerx)*(i - centerx) 
//							+ (j - centery)*(j - centery);
//					if(distance > maxdistance) {
//						continue;
//					}
//				}
//				history.getCurrent().setRGB(i, j, setTo.getRGB());
//			}
//		}
	}
	private void fill(Point lowerBound, Point upperBound, Color setTo) {
//		HashSet<Integer> colors = new HashSet<>();
//		HashSet<Pixel> visited = new HashSet<>();
//		LinkedList<Pixel> search = new LinkedList<Pixel>();
//		for(int i = lowerBound.x; i <= upperBound.x; i++) {
//			for(int j = lowerBound.y; j <= upperBound.y; j++) {
//				Pixel start = new Pixel(i, j);
//				search.add(start);
//				colors.add(history.getCurrent().getRGB(i, j));
//				visited.add(start);
//			}
//		}
//		while (!search.isEmpty()) {
//			Pixel pixel = search.removeFirst();
//			history.getCurrent().setRGB(pixel.x, pixel.y, setTo.getRGB());
////			setSelected(pixel.x, pixel.y, setTo);
//			for(Pixel neighbor : getNeighbors(pixel)) {
//				if(!visited.contains(neighbor) && neighbor.x >= 0 && neighbor.y >= 0 && neighbor.x < history.getCurrent().getWidth() && neighbor.y < history.getCurrent().getHeight()) {
//					visited.add(neighbor);
//					if (colors.contains(history.getCurrent().getRGB(neighbor.x, neighbor.y))) {
//						search.add(neighbor);
//					}
//				}
//			}
//		}
	}

	private void matchColorDraw(Point lowerBound, Point upperBound, Color setTo) {
//		HashSet<Integer> colors = new HashSet<>();
//		for(int i = lowerBound.x; i <= upperBound.x; i++) {
//			for(int j = lowerBound.y; j <= upperBound.y; j++) {
//				colors.add(history.getCurrent().getRGB(i, j));
//			}
//		}
//		if(colors.isEmpty()) {
//			return;
//		}
//		for (int i = 0; i < history.getCurrent().getWidth(); i++) {
//			for (int j = 0; j < history.getCurrent().getHeight(); j++) {
//				if(colors.contains(history.getCurrent().getRGB(i, j))) {
//					history.getCurrent().setRGB(i, j, setTo.getRGB());
//				}
//			}
//		}
	}
	
	public Color getMainColor() {
		return brush.getColor();
	}
	public Color getAltColor() {
		return altColor;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(Color.white);
		g.drawLine(cameraOffset.x, 0, cameraOffset.x, getHeight());
		g.drawString("0", cameraOffset.x + 2, getHeight() - 2);
		g.drawLine(0, cameraOffset.y, getWidth(), cameraOffset.y);
		g.drawString("0", getWidth() - 10, cameraOffset.y - 2);
		
		Graphics2D g2d = (Graphics2D)g;
		
		int strokeSize = 2;
		g2d.setStroke(new BasicStroke(strokeSize));
		
		g.translate(cameraOffset.x, cameraOffset.y);

		if(DriverKPaint.NEW_VERSION) {
			Rectangle bounds = layers.getBoundingRect();
			Rectangle scaledBounds = new Rectangle(
					(int)(bounds.x*zoom), 
					(int)(bounds.y*zoom), 
					(int)(bounds.width*zoom),
					(int)(bounds.height*zoom));
			g.drawImage(layers.compose(), scaledBounds.x, scaledBounds.y, scaledBounds.width, scaledBounds.height, null);
			for(Layer layer : layers.getLayers()) {
				if(layer.shown()) {
					int x = (int) (zoom*layer.x());
					int y = (int) (zoom*layer.y());
					int w = (int) (zoom*layer.w());
					int h = (int) (zoom*layer.h());
					g.setColor(layer == layers.active() ? Color.white : Color.LIGHT_GRAY);
					g.drawRect(x-1, y-1, w+1, h+1);
				}
			}
			
		}
		else {
//			int canvasWidth = (int)(history.getCurrent().getWidth()*pixelSize);
//			int canvasHeight = (int)(history.getCurrent().getHeight()*pixelSize);
//			int stripeWidth = 10;
//			for (int i = 0; i < canvasWidth; i += stripeWidth) {
//				int c = Math.min(Math.max(i + xOffset + i%(i%10 + 1), 0), getWidth());
//				g.setColor(new Color((int) (c * 255 / getWidth()),
//						(int) (c * 255 / getWidth() ),
//						(int) (c * 255 / getWidth())));
//				int width = i + stripeWidth > canvasWidth ? canvasWidth - i : stripeWidth;
//				g.fillRect(i, 0, width, canvasHeight);
//			}
//	
//			if(showTiling) {
//				g.drawImage(history.getCurrent(), 0, -canvasHeight, canvasWidth, canvasHeight, null);
//				g.drawImage(history.getCurrent(), 0, canvasHeight, canvasWidth, canvasHeight, null);
//				g.drawImage(history.getCurrent(), -canvasWidth, -canvasHeight/2, canvasWidth, canvasHeight, null);
//				g.drawImage(history.getCurrent(), -canvasWidth, +canvasHeight/2, canvasWidth, canvasHeight, null);
//				g.drawImage(history.getCurrent(), +canvasWidth, -canvasHeight/2, canvasWidth, canvasHeight, null);
//				g.drawImage(history.getCurrent(), +canvasWidth, +canvasHeight/2, canvasWidth, canvasHeight, null);
//			}
//			g.drawImage(history.getCurrent(), 0, 0, canvasWidth, canvasHeight, null);
//			
//			int borderStrokeSize = 1;
//			g2d.setStroke(new BasicStroke(borderStrokeSize));
//			g.setColor(Color.white);
//			g.drawRect(-borderStrokeSize*2, -borderStrokeSize*2, canvasWidth + borderStrokeSize*4, canvasHeight + borderStrokeSize*4);
//			g.setColor(Color.black);
//			g.drawRect(-borderStrokeSize, -borderStrokeSize, canvasWidth + borderStrokeSize*2, canvasHeight + borderStrokeSize*2);

		}
		
		g2d.setStroke(new BasicStroke(strokeSize));
		
		if(resizingLayer != null) {
			g.setColor(Color.red);
			g.drawRect((int) (targetLayerSize.x*zoom), (int) (targetLayerSize.y*zoom), (int) (targetLayerSize.width*zoom), (int) (targetLayerSize.height*zoom));
		}

//		if(selectedImage != null) {
//			g.drawImage(selectedImage, (int) (selectedRectangle.x*pixelSize)+1, (int) (selectedRectangle.y*pixelSize)+1, (int) ((selectedRectangle.width+1)*pixelSize)-1, (int) ((selectedRectangle.height+1)*pixelSize)-1, null);
//			g.setColor(new Color(255, 0, 0, 30));
//			g.fillRect((int) (selectedRectangle.x*pixelSize)+1, (int) (selectedRectangle.y*pixelSize)+1, (int) ((selectedRectangle.width+1)*pixelSize)-1, (int) ((selectedRectangle.height+1)*pixelSize)-1);
//		}
//		if(selectedRectangle != null) {
//			g.setColor(Color.red);
//			g.drawRect((int) (selectedRectangle.x*pixelSize), (int) (selectedRectangle.y*pixelSize), (int) ((selectedRectangle.width+1)*pixelSize)-1, (int) ((selectedRectangle.height+1)*pixelSize)-1);
//		}
		int indicatorBrushSize = brush.getSize();
		if(brush.getMode() == BrushMode.SELECT || brush.getMode() == BrushMode.COLOR_PICKER) {
			indicatorBrushSize = 1;
		}
		if(previousMousePosition != null && (brush.getMode() == BrushMode.BRUSH || brush.getMode() == BrushMode.FILL || brush.getMode() == BrushMode.ALL_MATCHING_COLOR || brush.getMode() == BrushMode.COLOR_PICKER || brush.getMode() == BrushMode.SELECT)) {
			Vec2i pixelPosition = screenToPixel(previousMousePosition);
			if(!movingCamera && !movingLayer && resizingLayer == null) {
				int minx = (int) ((pixelPosition.x - indicatorBrushSize/2) * zoom);
				int miny = (int) ((pixelPosition.y - indicatorBrushSize/2) * zoom);
				int maxx = (int) ((pixelPosition.x - indicatorBrushSize/2 + indicatorBrushSize) * zoom) - 1;
				int maxy = (int) ((pixelPosition.y - indicatorBrushSize/2 + indicatorBrushSize) * zoom) - 1;
				g.setColor(Color.black);
				g.drawRect(minx, miny, maxx-minx, maxy-miny);
				g.setColor(Color.white);
				g.drawRect(minx + strokeSize, miny + strokeSize, maxx-minx - strokeSize*2, maxy-miny - strokeSize*2);
			}
			if(DriverKPaint.DEBUG) {
				g.setColor(Color.green);
				g.drawString(zoom + "", 10, getHeight() - 70);
				g.drawString(cameraOffset.toString(), 10, getHeight() - 50);
				g.drawString(previousMousePosition.toString(), 10, getHeight() - 30);
			}
			infoStrings.add("Mouse Position: " + pixelPosition.x + ", " + pixelPosition.y);
		}

		
		infoStrings.add("Brush Size: " + brush.getSize());
		infoStrings.add("Canvas Size: " + getCurrentImage().getWidth() + ", " + getCurrentImage().getHeight());
		if(resizingLayer != null) {
			infoStrings.add("New Canvas Size: " + targetLayerSize.width + ", " + targetLayerSize.height);
		}
//		if(selectedRectangle != null) {
//			infoStrings.add("Selection Dims: " + selectedRectangle.x + ", " + selectedRectangle.y + ", " + (selectedRectangle.width+1) + ", " + (selectedRectangle.height+1));
//		}
		g.translate(-cameraOffset.x, -cameraOffset.y);
		g.setColor(Color.green);
		g.setFont(DriverKPaint.MAIN_FONT);
		int y = 25;
		for(String s : infoStrings) {
			g.drawString(s, 10, y);
			y += DriverKPaint.MAIN_FONT.getSize() + 3;
		}
		infoStrings.clear();

		if(layers.active().shown()) {
			Vec2i activeScreenTopLeft = pixelToScreen(new Vec2i(layers.active().x(), layers.active().y()));
			Vec2i activeScreenBotRight = pixelToScreen(new Vec2i(layers.active().x() + layers.active().w(),
			                                                     layers.active().y() + layers.active().h()));
			Vec2i boundedTopLeft = new Vec2i(Math.max(activeScreenTopLeft.x, 0), Math.max(activeScreenTopLeft.y, 0));
			Vec2i boundedBotRight = new Vec2i(Math.min(activeScreenBotRight.x, getWidth()), Math.min(activeScreenBotRight.y, getHeight()));
			Vec2i boundedCenter = new Vec2i((boundedTopLeft.x + boundedBotRight.x)/2, (boundedTopLeft.y + boundedBotRight.y)/2);
			
			int radius = 32;
			int padding = 5;
			int distance = 10;
			g.setColor(Color.white);
			g.drawOval(activeScreenTopLeft.x-distance - radius, boundedCenter.y - radius/2 - padding - radius, radius, radius);
			g.drawOval(activeScreenTopLeft.x-distance - radius, boundedCenter.y - radius/2, radius, radius);
			g.drawOval(activeScreenTopLeft.x-distance - radius, boundedCenter.y + radius/2 + padding, radius, radius);
			
			g.drawOval(activeScreenBotRight.x+distance, boundedCenter.y - radius/2 - padding - radius, radius, radius);
			g.drawOval(activeScreenBotRight.x+distance, boundedCenter.y - radius/2, radius, radius);
			g.drawOval(activeScreenBotRight.x+distance, boundedCenter.y + radius/2 + padding, radius, radius);
	
	
			g.drawOval(boundedCenter.x - radius/2 - padding - radius, activeScreenTopLeft.y - distance - radius, radius, radius);
			g.drawOval(boundedCenter.x - radius/2, activeScreenTopLeft.y - distance - radius, radius, radius);
			g.drawOval(boundedCenter.x + radius/2 + padding, activeScreenTopLeft.y - distance - radius, radius, radius);
			
			g.drawOval(boundedCenter.x - radius/2 - padding - radius, activeScreenBotRight.y + distance, radius, radius);
			g.drawOval(boundedCenter.x - radius/2, activeScreenBotRight.y + distance, radius, radius);
			g.drawOval(boundedCenter.x + radius/2 + padding, activeScreenBotRight.y + distance, radius, radius);
	
			g.drawOval(activeScreenTopLeft.x - distance - radius, activeScreenTopLeft.y - distance - radius, radius, radius);
			g.drawOval(activeScreenTopLeft.x - distance - radius, activeScreenBotRight.y + distance, radius, radius);
			g.drawOval(activeScreenBotRight.x + distance, activeScreenBotRight.y + distance, radius, radius);
			g.drawOval(activeScreenBotRight.x + distance, activeScreenTopLeft.y - distance - radius, radius, radius);
		}
//		int historyPreviewSize = 70;
//		int historyPreviewOffset = 10;
//		for(int i = 0; i < history.getHistory().size(); i++) {
//			g.drawImage(history.getHistory().get(i), getWidth() - historyPreviewOffset - historyPreviewSize, historyPreviewOffset + i*(historyPreviewOffset + historyPreviewSize), historyPreviewSize, historyPreviewSize, null);
//			g.setColor(Color.white);
//			g2d.setStroke(new BasicStroke(1));
//			g.drawRect(getWidth() - historyPreviewOffset - historyPreviewSize, historyPreviewOffset + i*(historyPreviewOffset + historyPreviewSize), historyPreviewSize, historyPreviewSize);
//
//			if(i == history.getCursor()) {
//				g.setColor(Color.green);
//				g2d.setStroke(new BasicStroke(3));
//				g.drawRect(getWidth() - historyPreviewOffset*3/2 - historyPreviewSize, historyPreviewOffset/2 + i*(historyPreviewOffset + historyPreviewSize), historyPreviewSize + historyPreviewOffset, historyPreviewSize + historyPreviewOffset);
//			}
//		}
	}

	@Override
	public void update() {
		repaint();
	}

}
