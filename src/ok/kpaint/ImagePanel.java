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
	
	public static final BrushMode DEFAULT_BRUSHMODE = BrushMode.BRUSH;
	
	private LinkedList<String> infoStrings = new LinkedList<>();
	private int xOffset;
	private int yOffset;
	private Point previousMousePosition = new Point(0, 0);
	private boolean movingCanvas;
	private Edge resizingCanvas;
	private Rectangle targetCanvasSize = new Rectangle();
	private HashSet<Integer> mouseButtonsPressed = new HashSet<>();

	/** AKA zoom level */
	private double pixelSize = 1;
	private Brush brush = new Brush(1, BrushShape.SQUARE, DEFAULT_BRUSHMODE, Color.white);
	private Layers layers;
	
	private Color altColor;
	
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
			brush.setBrushSize(size);
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

	public void resetImage(int w, int h) {
//		BufferedImage defaultImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
//		Graphics g = defaultImage.getGraphics();
//		g.setColor(altColor);
//		g.fillRect(0, 0, defaultImage.getWidth(), defaultImage.getHeight());
//		g.dispose();
//		addImageLayer(defaultImage);
		resetView();
	}
	public ImagePanel(Layers layers) {
		this.layers = layers;
		layers.addListener(this);
		altColor = Color.black;
		resetImage(512, 512);
		this.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				Point pixelPosition = getPixelPosition(e.getPoint());
				pixelPosition.x = Math.max(0, Math.min(pixelPosition.x, layers.active().w()));
				pixelPosition.y = Math.max(0, Math.min(pixelPosition.y, layers.active().h()));
				double oldPixelSize = pixelSize;
				if (e.getWheelRotation() > 0) {
					pixelSize = pixelSize * 0.9;
					if(pixelSize < 0.01) { 
						pixelSize = 0.01;
					}
				} else {
					pixelSize = pixelSize*1.1 + 0.1;
				}
				double deltaPixelSize = pixelSize - oldPixelSize;
				xOffset = (int)(xOffset - deltaPixelSize * pixelPosition.x);
				yOffset = (int)(yOffset - deltaPixelSize * pixelPosition.y);
				repaint();
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
//					Edge edge = Edge.OUTSIDE;
//					if(selectedRectangle != null) {
//						Rectangle sel = getSelectionScreenRectangle();
//						if(sel != null) {
//							edge = Utils.isNearEdge(e.getPoint(), sel);
//						}
//					}
//					if(edge == Edge.OUTSIDE) {
//						startMovingCanvas(e.getPoint());
//					}
//					else {
//						startMovingSelection(edge);
//					}
				}
				else if(brush.getMode() == BrushMode.SELECT) {
					resetSelection();
//					startedSelection = e.getPoint();
					updateSelectionRectangle(e.getPoint());
				}
				else if(brush.getMode() == BrushMode.COLOR_PICKER) {
					colorPicker(getPixelPosition(e.getPoint()), e.isShiftDown());
				}
				else {
					draw(getPixelPosition(e.getPoint()), e.isShiftDown());
				}
				previousMousePosition = e.getPoint();
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
				previousMousePosition = e.getPoint();
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
				if (movingCanvas || resizingCanvas != null) {
					updateCanvasMove(previousMousePosition, e.getPoint());
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
					draw(getPixelPosition(e.getPoint()), e.isShiftDown());
				}
				previousMousePosition = e.getPoint();
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
				previousMousePosition = e.getPoint();
				repaint();
			}
		});
	}
	
	private void updateCanvasMove(Point previousScreenPos, Point newScreenPos) {
		if(movingCanvas) {
			xOffset += newScreenPos.x - previousScreenPos.x;
			yOffset += newScreenPos.y - previousScreenPos.y;
//			if(startedSelection != null) {
//				startedSelection.x += newScreenPos.x - previousScreenPos.x;
//				startedSelection.y += newScreenPos.y - previousScreenPos.y;
//			}
		}
		else if(resizingCanvas != null) {
			Point previousPos = getPixelPosition(previousScreenPos);
			Point newPos = getPixelPosition(newScreenPos);
			if(resizingCanvas == Edge.EAST) {
				targetCanvasSize.width += newPos.x - previousPos.x;
				targetCanvasSize.width = Math.max(targetCanvasSize.width, 1);
			}
			else if(resizingCanvas == Edge.WEST) {
				int deltaWidth = previousPos.x - newPos.x;
				deltaWidth = Math.max(deltaWidth, -(targetCanvasSize.width) + 1);
				targetCanvasSize.width += deltaWidth;
				targetCanvasSize.x -= deltaWidth;
			}
			else if(resizingCanvas == Edge.SOUTH) {
				targetCanvasSize.height += newPos.y - previousPos.y;
				targetCanvasSize.height = Math.max(targetCanvasSize.height, 1);
			}
			else if(resizingCanvas == Edge.NORTH) {
				int deltaHeight = previousPos.y - newPos.y;
				deltaHeight = Math.max(deltaHeight, -(targetCanvasSize.height) + 1);
				targetCanvasSize.height += deltaHeight;
				targetCanvasSize.y -= deltaHeight;
			}
		}
	}
	
//	private void updateSelectionMove(Point previousPos, Point newPos) {
//		if(movingSelection == Edge.INSIDE) {
//			selectedRectangle.x += newPos.x - previousPos.x;
//			selectedRectangle.y += newPos.y - previousPos.y;
//		}
//		else if(movingSelection == Edge.EAST) {
//			if(newPos.x > selectedRectangle.x) {
//				selectedRectangle.width = newPos.x - selectedRectangle.x;
//			}
//			else {
//				selectedImage = createFlipped(selectedImage, false);
//				movingSelection = Edge.WEST;
//				selectedRectangle.width = selectedRectangle.x - newPos.x - 1;
//				selectedRectangle.x = newPos.x;
//			}
//		}
//		else if(movingSelection == Edge.WEST) {
//			if(newPos.x < selectedRectangle.x + selectedRectangle.width) {
//				selectedRectangle.width = (selectedRectangle.x + selectedRectangle.width) - newPos.x;
//				selectedRectangle.x = newPos.x;
//			}
//			else {
//				selectedImage = createFlipped(selectedImage, false);
//				movingSelection = Edge.EAST;
//				int newx = selectedRectangle.x + selectedRectangle.width + 1;
//				selectedRectangle.width = newPos.x - (selectedRectangle.x + selectedRectangle.width) - 1;
//				selectedRectangle.x = newx;
//			}
//		}
//		
//		if(movingSelection == Edge.SOUTH) {
//			if(newPos.y > selectedRectangle.y) {
//				selectedRectangle.height = newPos.y - selectedRectangle.y;
//			}
//			else {
//				selectedImage = createFlipped(selectedImage, true);
//				movingSelection = Edge.NORTH;
//				selectedRectangle.height = selectedRectangle.y - newPos.y - 1;
//				selectedRectangle.y = newPos.y;
//			}
//		}
//		else if(movingSelection == Edge.NORTH) {
//			if(newPos.y < selectedRectangle.y + selectedRectangle.height) {
//				selectedRectangle.height = (selectedRectangle.y + selectedRectangle.height) - newPos.y;
//				selectedRectangle.y = newPos.y;
//			}
//			else {
//				selectedImage = createFlipped(selectedImage, true);
//				movingSelection = Edge.SOUTH;
//				int newy = selectedRectangle.y + selectedRectangle.height + 1;
//				selectedRectangle.height = newPos.y - (selectedRectangle.y + selectedRectangle.height) - 1;
//				selectedRectangle.y = newy;
//			}
//		}
//		
//	}

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
	
	private void startMovingSelection(Edge edge) {
//		movingSelection = edge;
	}
	private void finishMovingSelection() {
//		movingSelection = null;
	}
	
	private void startMovingCanvas(Point mousePosition) {

		Rectangle canvas = getCanvasScreenRectangle();
		Edge edge = Utils.isNearEdge(mousePosition, canvas);
		if(edge == Edge.INSIDE || edge == Edge.OUTSIDE) {
			movingCanvas = true;
		}
		else {
			targetCanvasSize.x = layers.active().x();
			targetCanvasSize.y = layers.active().y();
			targetCanvasSize.width = layers.active().w();
			targetCanvasSize.height = layers.active().h();
			resizingCanvas = edge;
		}
	}
	
	private void finishMovingCanvas() {
		movingCanvas = false;
		if(resizingCanvas != null) {
			layers.active().resize(targetCanvasSize, altColor);
//			resizeCanvas(targetCanvasSize);
//			history.pushVersion();
			repaint();
		}
		resizingCanvas = null;
	}
	
	public void setGUIInterface(GUIInterface guiInterface) {
		this.guiInterface = guiInterface;
	}
	public void setControllerInterface(ControllerInterface controllerInterface) {
		this.controllerInterface = controllerInterface;
	}
	
	public void colorPicker(Point pixel, boolean shiftDown) {
		Color selected = new Color(getCurrentImage().getRGB(pixel.x, pixel.y), true);
		if(shiftDown) {
			ipInterface.setAltColor(selected);
		}
		else {
			ipInterface.setMainColor(selected);
		}
		repaint();
	}
	public void draw(Point currentPixel, boolean shiftDown) {
		Point previousPixel = getPixelPosition(previousMousePosition);
		int deltax = currentPixel.x - previousPixel.x;
		int deltay = currentPixel.y - previousPixel.y;
		if(Math.abs(deltax) <= 1 && Math.abs(deltay) <= 1) {
			drawOnPixel(currentPixel, shiftDown);
			return;
		}
		if(Math.abs(deltax) > Math.abs(deltay)) {
			if(currentPixel.x < previousPixel.x) {
				Point temp = currentPixel;
				currentPixel = previousPixel;
				previousPixel = temp;
			}
			for(int x = previousPixel.x; x <= currentPixel.x; x++) {
				double ratio = (double)(x - previousPixel.x) / (currentPixel.x - previousPixel.x);
				int yy = (int) (previousPixel.y + (currentPixel.y - previousPixel.y) * ratio);
				drawOnPixel(new Point(x, yy), shiftDown);
			}
		}
		else {
			if(currentPixel.y < previousPixel.y) {
				Point temp = currentPixel;
				currentPixel = previousPixel;
				previousPixel = temp;
			}
			for(int y = previousPixel.y; y <= currentPixel.y; y++) {
				double ratio = (double)(y - previousPixel.y) / (currentPixel.y - previousPixel.y);
				int xx = (int)(previousPixel.x + (currentPixel.x - previousPixel.x) * ratio);
				drawOnPixel(new Point(xx, y), shiftDown);
			}
		}
	}
	public void drawOnPixel(Point pixel, boolean shiftDown) {
		if(!DriverKPaint.NEW_VERSION) {
//			history.modified();
			Color setTo = brush.getColor();
			if(shiftDown) {
				setTo = altColor;
			}
			Point lowerBound = new Point(pixel.x - brush.getBrushSize()/2, pixel.y - brush.getBrushSize()/2);
			Point upperBound = new Point(lowerBound.x + brush.getBrushSize() - 1, lowerBound.y + brush.getBrushSize() - 1);
			lowerBound.x = Math.max(lowerBound.x, 0);
			lowerBound.y = Math.max(lowerBound.y, 0);
			
//			upperBound.x = Math.min(upperBound.x, history.getCurrent().getWidth()-1);
//			upperBound.y = Math.min(upperBound.y, history.getCurrent().getHeight()-1);
			
			if (brush.getMode() == BrushMode.ALL_MATCHING_COLOR) {
				matchColorDraw(lowerBound, upperBound, setTo);
			} 
			else if (brush.getMode() == BrushMode.FILL) {
				fill(lowerBound, upperBound, setTo);
			}
			else if (brush.getMode() == BrushMode.BRUSH) {
				brush(lowerBound, upperBound, brush.getShape(), setTo);
			}
		}
		else {
			layers.draw(new Pixel(pixel), brush);
		}
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
				(int)(xOffset + layers.active().x()*pixelSize), 
				(int)(yOffset + layers.active().y() * pixelSize), 
				(int)(layers.active().w()*pixelSize), 
				(int)(layers.active().h()*pixelSize));
		
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
	
	public Point getPixelPosition(Point screenPos) {
		Point pixel = new Point();
		pixel.x = (int) ((screenPos.x - xOffset)/pixelSize);
		pixel.y = (int) ((screenPos.y - yOffset)/pixelSize);
		if(screenPos.x - xOffset < 0) {
			pixel.x -= 1;
		}
		if(screenPos.y - yOffset < 0) {
			pixel.y -= 1;
		}
		return pixel;
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
		pixelSize = Math.min(xfit, yfit) * 0.95;
		xOffset = (int) (getWidth()/2 - pixelSize * bounds.width/2 - pixelSize*bounds.x);
		yOffset = (int) (getHeight()/2 - pixelSize * bounds.height/2 - pixelSize*bounds.y);
		repaint();
	}
	
	private LinkedList<Pixel> getNeighbors(Pixel pixel) {
		LinkedList<Pixel> neighbors = new LinkedList<>();
		neighbors.add(new Pixel(pixel.x - 1, pixel.y));
		neighbors.add(new Pixel(pixel.x + 1, pixel.y));
		neighbors.add(new Pixel(pixel.x, pixel.y - 1));
		neighbors.add(new Pixel(pixel.x, pixel.y + 1));
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
	
	public Point pixelPositionToDrawingPosition(Point pixel) {
		Point drawingPosition = new Point((int)(pixel.x * pixelSize), (int)(pixel.y * pixelSize));
		return drawingPosition;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(Color.white);
		g.drawLine(xOffset, 0, xOffset, getHeight());
		g.drawString("0", xOffset + 2, getHeight() - 2);
		g.drawLine(0, yOffset, getWidth(), yOffset);
		g.drawString("0", getWidth() - 10, yOffset - 2);
		
		Graphics2D g2d = (Graphics2D)g;
		
		int strokeSize = 2;
		g2d.setStroke(new BasicStroke(strokeSize));
		
		g.translate(xOffset, yOffset);

		if(DriverKPaint.NEW_VERSION) {
			Rectangle bounds = layers.getBoundingRect();
			Rectangle scaledBounds = new Rectangle(
					(int)(bounds.x*pixelSize), 
					(int)(bounds.y*pixelSize), 
					(int)(bounds.width*pixelSize),
					(int)(bounds.height*pixelSize));
			g.drawImage(layers.compose(), scaledBounds.x, scaledBounds.y, scaledBounds.width, scaledBounds.height, null);
			for(Layer layer : layers.getLayers()) {
				if(layer.shown()) {
					int x = (int) (pixelSize*layer.x());
					int y = (int) (pixelSize*layer.y());
					int w = (int) (pixelSize*layer.w());
					int h = (int) (pixelSize*layer.h());
					g.setColor(Color.white);
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
		
		if(resizingCanvas != null) {
			g.setColor(Color.red);
			g.drawRect((int) (targetCanvasSize.x*pixelSize), (int) (targetCanvasSize.y*pixelSize), (int) (targetCanvasSize.width*pixelSize), (int) (targetCanvasSize.height*pixelSize));
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
		int indicatorBrushSize = brush.getBrushSize();
		if(brush.getMode() == BrushMode.SELECT || brush.getMode() == BrushMode.COLOR_PICKER) {
			indicatorBrushSize = 1;
		}
		if(previousMousePosition != null && (brush.getMode() == BrushMode.BRUSH || brush.getMode() == BrushMode.FILL || brush.getMode() == BrushMode.ALL_MATCHING_COLOR || brush.getMode() == BrushMode.COLOR_PICKER || brush.getMode() == BrushMode.SELECT)) {
			Point pixelPosition = getPixelPosition(previousMousePosition);
			int minx = (int) ((pixelPosition.x - indicatorBrushSize/2) * pixelSize);
			int miny = (int) ((pixelPosition.y - indicatorBrushSize/2) * pixelSize);
			int maxx = (int) ((pixelPosition.x - indicatorBrushSize/2 + indicatorBrushSize) * pixelSize) - 1;
			int maxy = (int) ((pixelPosition.y - indicatorBrushSize/2 + indicatorBrushSize) * pixelSize) - 1;
			g.setColor(Color.black);
			g.drawRect(minx, miny, maxx-minx, maxy-miny);
			g.setColor(Color.white);
			g.drawRect(minx + strokeSize, miny + strokeSize, maxx-minx - strokeSize*2, maxy-miny - strokeSize*2);
			if(DriverKPaint.DEBUG) {
				g.setColor(Color.green);
				g.drawString(pixelSize + "", 10, getHeight() - 70);
				g.drawString(xOffset + "," + yOffset, 10, getHeight() - 50);
				g.drawString(previousMousePosition.x + "," + previousMousePosition.y, 10, getHeight() - 30);
			}
			infoStrings.add("Mouse Position: " + pixelPosition.x + ", " + pixelPosition.y);
		}

		
		infoStrings.add("Brush Size: " + brush.getBrushSize());
		infoStrings.add("Canvas Size: " + getCurrentImage().getWidth() + ", " + getCurrentImage().getHeight());
		if(resizingCanvas != null) {
			infoStrings.add("New Canvas Size: " + targetCanvasSize.width + ", " + targetCanvasSize.height);
		}
//		if(selectedRectangle != null) {
//			infoStrings.add("Selection Dims: " + selectedRectangle.x + ", " + selectedRectangle.y + ", " + (selectedRectangle.width+1) + ", " + (selectedRectangle.height+1));
//		}
		g.translate(-xOffset, -yOffset);
		g.setColor(Color.green);
		g.setFont(DriverKPaint.MAIN_FONT);
		int y = 25;
		for(String s : infoStrings) {
			g.drawString(s, 10, y);
			y += DriverKPaint.MAIN_FONT.getSize() + 3;
		}
		infoStrings.clear();

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
