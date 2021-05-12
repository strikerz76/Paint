package ok.kpaint;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.Map.*;

import javax.swing.*;

import ok.kpaint.Utils.*;
import ok.kpaint.gui.*;
import ok.kpaint.gui.layers.*;

public class ImagePanel extends JPanel implements LayersListener, ComponentListener {
	
	private static final Color TRANSLUCENT_BORDER = new Color(255, 255, 255, 100);
	private static final Color SELECTED_BORDER = new Color(255, 255, 255, 150);
//	private static final Cursor colorPickerCursor = Toolkit.getDefaultToolkit().createCustomCursor(Utils.loadImage("/color_picker.png"), new Point(0, 0), "colorpicker");
	
	
	private LinkedList<String> infoStrings = new LinkedList<>();

	private double zoom = 1;
	private Vec2i cameraOffset = new Vec2i();
	private Vec2i previousMousePosition = new Vec2i();
	
	
	private Command inprogressCommand;
	private boolean movingCamera;
	private HashSet<Integer> mouseButtonsPressed = new HashSet<>();
	private boolean mouseOverHandle = false;

	private Brush brush = new Brush(Brush.DEFAULT_BRUSH);
	private Color altColor = new Color(0, 0, 0, 0);
	private Layers layers;
	private BufferedImage background;
	
	private boolean showTiling;
	private boolean darkMode;
	
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
		public void newLayer() {
			Vec2i newLayerSize = Utils.queryNewLayerSize(ImagePanel.this,
			                                             new Vec2i(layers.active().w(), layers.active().h()));
			if(newLayerSize != null) {
				BufferedImage newImage = new BufferedImage(newLayerSize.x, newLayerSize.y, BufferedImage.TYPE_4BYTE_ABGR);
				layers.add(newImage);
			}
		}
		@Override
		public void showTiling(boolean enabled) {
			showTiling = enabled;
		}
		@Override
		public void enableDarkMode(boolean enabled) {
			darkMode = enabled;
			updateBackground();
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
		updateBackground();
		this.layers = layers;
		layers.addListener(this);
		this.addComponentListener(this);
		Thread repaintThread = new Thread(() -> {
			try {
				while(true) {
					repaint();
					Thread.sleep(100);
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		});
		repaintThread.start();
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
//					if(e.getKeyCode() == KeyEvent.VK_1) {
//						guiInterface.switchLayout(true);
//					}
//					if(e.getKeyCode() == KeyEvent.VK_2) {
//						guiInterface.switchLayout(false);
//					}
					if(e.getKeyCode() == KeyEvent.VK_N) {
						ipInterface.newLayer();
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
				}
				else {
					if(e.getKeyCode() == KeyEvent.VK_SPACE) {
						ipInterface.resetView();
					}
					else if(e.getKeyCode() == KeyEvent.VK_SHIFT) {
						ipInterface.swapColors();
					}
					else if(e.getKeyCode() == KeyEvent.VK_P) {
						guiInterface.changeModeHotkey(BrushMode.COLOR_PICKER);
					}
					else if(e.getKeyCode() == KeyEvent.VK_E) {
						guiInterface.changeModeHotkey(BrushMode.EXTRACT);
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
				Vec2i mousePos = new Vec2i(e.getPoint());
				mouseButtonsPressed.add(e.getButton());
				if(e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					
				}
				else {
					Handle handle = isMouseInHandle(mousePos);
					if(brush.getMode() == BrushMode.EXTRACT) {
						startExtraction(mousePos);
					}
					else if(brush.getMode() == BrushMode.COLOR_PICKER) {
						colorPicker(screenToPixel(mousePos), e.isShiftDown());
					}
					else if(handle != null) {
						inprogressCommand = new Command(layers.active(), handle, screenToPixel(mousePos));
					}
					else {
						draw(screenToPixel(e.getPoint()), e.isShiftDown());
					}
				}
				previousMousePosition = mousePos;
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				Vec2i mousePos = new Vec2i(e.getPoint());
				mouseButtonsPressed.remove(e.getButton());
				if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
					if(movingCamera) {
						finishMovingCanvas();
					}
					else {
						Layer layer = layers.hitScan(screenToPixel(mousePos));
						if(layer != null) {
							LayerContextMenu contextMenu = new LayerContextMenu(layers, layer);
							contextMenu.show(ImagePanel.this, e.getX(), e.getY());
						}
					}
				}
				else {
					if(inprogressCommand != null) {
						inprogressCommand.layer.applyCommand(inprogressCommand, altColor);
						// TODO add to history here
						inprogressCommand = null;
					}
					else if(brush.getMode() == BrushMode.EXTRACT) {
						finishExtraction();
						guiInterface.finishedSelection();
					}
					else {
					}
				}
				previousMousePosition = mousePos;
				repaint();
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
				Vec2i mousePos = new Vec2i(e.getPoint());
				if(mouseButtonsPressed.contains(MouseEvent.BUTTON2)
						|| mouseButtonsPressed.contains(MouseEvent.BUTTON3)) {
					startMovingCanvas(previousMousePosition);
				}
				if (movingCamera) {
					updateCanvasMove(previousMousePosition, mousePos);
				}
				else {
					if(inprogressCommand != null) {
						inprogressCommand.mouseEndPixel = screenToPixel(mousePos);
						inprogressCommand.layer.updateCommand(inprogressCommand);
					}
					else if(brush.getMode() == BrushMode.EXTRACT) {
						updateExtraction(mousePos);
					}
					else {
						draw(screenToPixel(mousePos), e.isShiftDown());
					}
				}
				previousMousePosition = mousePos;
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				Vec2i mousePos = new Vec2i(e.getPoint());
//				int newCursorType = Cursor.DEFAULT_CURSOR;
//				if(brush.getMode() == BrushMode.SELECT) {
//					newCursorType = Cursor.CROSSHAIR_CURSOR;
//				}
//				ImagePanel.this.setCursor(Cursor.getPredefinedCursor(newCursorType));
				updateCursor(mousePos);
				previousMousePosition = mousePos;
				repaint();
			}
		});
	}
	
	@Override
	public void componentResized(ComponentEvent e) {
		if(background.getWidth() != getWidth() || background.getHeight() != getHeight()) {
			updateBackground();
		}
	}
	@Override
	public void componentMoved(ComponentEvent e) {
		
	}
	@Override
	public void componentShown(ComponentEvent e) {
		
	}
	@Override
	public void componentHidden(ComponentEvent e) {
		
	}
	
	private void updateBackground() {
		background = Utils.makeBackgroundImage(getWidth(), getHeight(), darkMode);
	}
	
	private void startMovingCanvas(Vec2i mousePosition) {
		Rectangle canvas = getCanvasScreenRectangle();
		Edge edge = Utils.isNearEdge(mousePosition, canvas);
		if(edge == Edge.OUTSIDE || edge == Edge.INSIDE) {
			movingCamera = true;
			ImagePanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
	}
	
	private void finishMovingCanvas() {
		movingCamera = false;
	}
	
	private Vec2i extractionStart;
	private Vec2i extractionCurrent;
	private void startExtraction(Vec2i screen) {
		extractionStart = screenToPixel(screen);
		extractionCurrent = extractionStart.add(1, 1);
	}
	private void updateExtraction(Vec2i screen) {
		extractionCurrent = screenToPixel(screen).add(1, 1);
	}
	private void finishExtraction() {
		Rectangle extraction = Utils.makeRectangle(extractionStart, extractionCurrent);
		layers.extract(extraction, altColor);
		guiInterface.changeModeHotkey(BrushMode.BRUSH);
		extractionStart = null;
		extractionCurrent = null;
	}
	
	public void setGUIInterface(GUIInterface guiInterface) {
		this.guiInterface = guiInterface;
	}
	public void setControllerInterface(ControllerInterface controllerInterface) {
		this.controllerInterface = controllerInterface;
	}
	
	public void colorPicker(Vec2i pixel, boolean shiftDown) {
		Rectangle bounds = layers.getBoundingRect();
		Color selected = new Color(getCurrentImage().getRGB(pixel.x - bounds.x, pixel.y - bounds.y), true);
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
	
	
	private void pasteFromClipboard() {
		Image image = Utils.getImageFromClipboard();
		if(image != null) {
			layers.add(Utils.copyImage(Utils.toBufferedImage(image)));
			repaint();
		}
	}
	
	private Rectangle getCanvasScreenRectangle() {
		return new Rectangle(
				(int)(cameraOffset.x + layers.active().x()*zoom), 
				(int)(cameraOffset.y + layers.active().y() * zoom), 
				(int)(layers.active().w()*zoom), 
				(int)(layers.active().h()*zoom));
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
		zoom = Math.min(xfit, yfit) * 0.9;
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
	
	public Color getMainColor() {
		return brush.getColor();
	}
	public Color getAltColor() {
		return altColor;
	}
	
	private HashMap<Handle, Vec2i> handles = new HashMap<>();
	int radius = 16;
	private void updateHandlePositions() {
		Vec2i activeScreenTopLeft = pixelToScreen(new Vec2i(layers.active().x(), layers.active().y()));
		Vec2i activeScreenBotRight = pixelToScreen(new Vec2i(layers.active().x() + layers.active().w(),
		                                                     layers.active().y() + layers.active().h()));
		Vec2i boundedTopLeft = new Vec2i(Math.max(activeScreenTopLeft.x, 0), Math.max(activeScreenTopLeft.y, 0));
		Vec2i boundedBotRight = new Vec2i(Math.min(activeScreenBotRight.x, getWidth()), Math.min(activeScreenBotRight.y, getHeight()));
		Vec2i boundedCenter = new Vec2i((boundedTopLeft.x + boundedBotRight.x)/2, (boundedTopLeft.y + boundedBotRight.y)/2);

		int radius = 16;
		int padding = 2;
		int distance = 10;
		int offset = padding + radius;
		handles.put(Handle.MOVE_NORTH, new Vec2i((boundedCenter.x + activeScreenTopLeft.x)/2, activeScreenTopLeft.y - distance - radius));
		handles.put(Handle.MOVE_SOUTH, new Vec2i(activeScreenBotRight.x + distance + radius, (boundedCenter.y + activeScreenBotRight.y)/2));
		
		handles.put(Handle.RESIZE_NORTH, new Vec2i(boundedCenter.x - offset, activeScreenTopLeft.y - distance - radius));
		handles.put(Handle.RESIZE_SOUTH, new Vec2i(boundedCenter.x - offset, activeScreenBotRight.y + distance + radius));
		handles.put(Handle.RESIZE_EAST, new Vec2i(activeScreenBotRight.x + distance + radius, boundedCenter.y - offset));
		handles.put(Handle.RESIZE_WEST, new Vec2i(activeScreenTopLeft.x - distance - radius, boundedCenter.y - offset));

		handles.put(Handle.RESIZE_NORTHEAST, new Vec2i(activeScreenBotRight.x, activeScreenTopLeft.y - distance - radius));
		handles.put(Handle.RESIZE_SOUTHEAST, new Vec2i(activeScreenBotRight.x, activeScreenBotRight.y + distance + radius));
		handles.put(Handle.RESIZE_SOUTHWEST, new Vec2i(activeScreenTopLeft.x, activeScreenBotRight.y + distance + radius));
		handles.put(Handle.RESIZE_NORTHWEST, new Vec2i(activeScreenTopLeft.x, activeScreenTopLeft.y - distance - radius));
		
		handles.put(Handle.STRETCH_NORTH, new Vec2i(boundedCenter.x + offset, activeScreenTopLeft.y - distance - radius));
		handles.put(Handle.STRETCH_SOUTH, new Vec2i(boundedCenter.x + offset, activeScreenBotRight.y + distance + radius));
		handles.put(Handle.STRETCH_EAST, new Vec2i(activeScreenBotRight.x + distance + radius, boundedCenter.y + offset));
		handles.put(Handle.STRETCH_WEST, new Vec2i(activeScreenTopLeft.x - distance - radius, boundedCenter.y + offset));

		handles.put(Handle.STRETCH_NORTHEAST, new Vec2i(activeScreenBotRight.x + distance + radius, activeScreenTopLeft.y));
		handles.put(Handle.STRETCH_SOUTHEAST, new Vec2i(activeScreenBotRight.x + distance + radius, activeScreenBotRight.y));
		handles.put(Handle.STRETCH_SOUTHWEST, new Vec2i(activeScreenTopLeft.x - distance - radius, activeScreenBotRight.y));
		handles.put(Handle.STRETCH_NORTHWEST, new Vec2i(activeScreenTopLeft.x - distance - radius, activeScreenTopLeft.y));
	}
	
	private void updateCursor(Vec2i mousePos) {
		Handle handle = isMouseInHandle(mousePos);
		if(handle != null) {
			this.mouseOverHandle = true;
			setCursor(handle.cursor);
		}
		else {
			this.mouseOverHandle = false;
			if(brush.getMode() == BrushMode.EXTRACT) {
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
			else {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
	private Handle isMouseInHandle(Vec2i mousePos) {
		for(Entry<Handle, Vec2i> entry : handles.entrySet()) {
			Vec2i pos = entry.getValue();
			Handle handle = entry.getKey();
			if(mousePos.distanceTo(pos) <= radius) {
				return handle;
			}
		}
		return null;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.black);
//		g.fillRect(0, 0, getWidth(), getHeight());
		g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
		
//		g.setColor(Color.white);
//		g.drawLine(cameraOffset.x, 0, cameraOffset.x, getHeight()/16);
//		g.drawLine(cameraOffset.x, getHeight()*15/16, cameraOffset.x, getHeight());
//		g.drawString("0", cameraOffset.x + 2, getHeight() - 2);
//		g.drawLine(0, cameraOffset.y, getWidth()/16, cameraOffset.y);
//		g.drawLine(getWidth()*15/16, cameraOffset.y, getWidth(), cameraOffset.y);
//		g.drawString("0", getWidth() - 10, cameraOffset.y - 2);
		
		Graphics2D g2d = (Graphics2D)g;
		
		int strokeSize = 1;
		BasicStroke basicStroke = new BasicStroke(strokeSize);
		BasicStroke dashed = new BasicStroke(1.0f,
                                             BasicStroke.CAP_SQUARE,
                                             BasicStroke.JOIN_MITER,
                                             1.0f, new float[] {10}, (System.currentTimeMillis()%2000)/100f);
		g2d.setStroke(basicStroke);
		
		g.translate(cameraOffset.x, cameraOffset.y);

		for(Layer layer : layers.getLayers()) {
			if(layer.shown()) {
				int x = (int) (zoom*layer.x());
				int y = (int) (zoom*layer.y());
				int w = (int) (zoom*layer.w());
				int h = (int) (zoom*layer.h());
				g.drawImage(layer.image(), x, y, w, h, null);
				if(showTiling && layer == layers.active()) {
					g.drawImage(layer.image(), x, y - h, w, h, null);
					g.drawImage(layer.image(), x, y + h, w, h, null);
					g.drawImage(layer.image(), x + w, y - h/2, w, h, null);
					g.drawImage(layer.image(), x - w, y - h/2, w, h, null);
					g.drawImage(layer.image(), x + w, y + h/2, w, h, null);
					g.drawImage(layer.image(), x - w, y + h/2, w, h, null);
				}
				g.setColor(TRANSLUCENT_BORDER);
				g2d.setStroke(basicStroke);
				g.drawRect(x-1, y-1, w+1, h+1);
			}
		}
		if(layers.active().shown()) {
			int x = (int) (zoom*layers.active().x());
			int y = (int) (zoom*layers.active().y());
			int w = (int) (zoom*layers.active().w());
			int h = (int) (zoom*layers.active().h());
			g.setColor(SELECTED_BORDER);
			g2d.setStroke(dashed);
			g.drawRect(x-1, y-1, w+1, h+1);
		}
		g2d.setStroke(basicStroke);
		
		if(inprogressCommand != null) {
			g.setColor(Color.red);
			Rectangle bounds = inprogressCommand.layer.getBoundsAfterCommand(inprogressCommand);
			g.drawRect((int) (bounds.x*zoom)-1, (int) (bounds.y*zoom)-1, (int) (bounds.width*zoom)+1, (int) (bounds.height*zoom)+1);
		}
		
		if(extractionStart != null) {
			g.setColor(Color.red);
			Rectangle extraction = Utils.makeRectangle(extractionStart, extractionCurrent);
			g.drawRect((int) (extraction.x*zoom)-1, (int) (extraction.y*zoom)-1, (int) (extraction.width*zoom)+1, (int) (extraction.height*zoom)+1);
		}

		int indicatorBrushSize = brush.getSize();
		if(brush.getMode() == BrushMode.EXTRACT || brush.getMode() == BrushMode.COLOR_PICKER) {
			indicatorBrushSize = 1;
		}
		if(previousMousePosition != null && (brush.getMode() == BrushMode.BRUSH || brush.getMode() == BrushMode.FILL || brush.getMode() == BrushMode.ALL_MATCHING_COLOR || brush.getMode() == BrushMode.COLOR_PICKER || brush.getMode() == BrushMode.EXTRACT)) {
			Vec2i pixelPosition = screenToPixel(previousMousePosition);
			if(!mouseOverHandle && !movingCamera) {
				int minx = (int) ((pixelPosition.x - indicatorBrushSize/2) * zoom);
				int miny = (int) ((pixelPosition.y - indicatorBrushSize/2) * zoom);
				int maxx = (int) ((pixelPosition.x - indicatorBrushSize/2 + indicatorBrushSize) * zoom) - 1;
				int maxy = (int) ((pixelPosition.y - indicatorBrushSize/2 + indicatorBrushSize) * zoom) - 1;
				
				if(brush.getShape() == BrushShape.CIRCLE) {
					g.setColor(Color.black);
					g.drawOval(minx, miny, maxx-minx, maxy-miny);
					g.setColor(Color.white);
					g.drawOval(minx + strokeSize, miny + strokeSize, maxx-minx - strokeSize*2, maxy-miny - strokeSize*2);
				}
				else if(brush.getShape() == BrushShape.SQUARE) {
					g.setColor(Color.black);
					g.drawRect(minx, miny, maxx-minx, maxy-miny);
					g.setColor(Color.white);
					g.drawRect(minx + strokeSize, miny + strokeSize, maxx-minx - strokeSize*2, maxy-miny - strokeSize*2);
				}
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
		if(inprogressCommand != null) {
			Rectangle newBounds = inprogressCommand.layer.getBoundsAfterCommand(inprogressCommand);
			infoStrings.add("New Canvas Dims: " + newBounds.x + ", " + newBounds.x + ", " + 
												  newBounds.width + ", " + newBounds.height);
		}
		g.translate(-cameraOffset.x, -cameraOffset.y);
		g.setColor(Color.green);
		g.setFont(DriverKPaint.MAIN_FONT);
		int y = 25;
		for(String s : infoStrings) {
			g.drawString(s, 10, y);
			y += DriverKPaint.MAIN_FONT.getSize() + 3;
		}
		infoStrings.clear();

		if(layers.active().shown() && inprogressCommand == null) {
			updateHandlePositions();
			g.setColor(Color.DARK_GRAY);
//			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
//			g2d.setStroke(KUI.dashed);
			for(Entry<Handle, Vec2i> entry : handles.entrySet()) {
				Vec2i pos = entry.getValue();
				Handle handle = entry.getKey();
				g.fillOval(pos.x - radius, pos.y - radius, radius*2, radius*2);
				g.drawImage(handle.image, pos.x - radius, pos.y - radius, radius*2, radius*2, null);
			}
		}
	}

	@Override
	public void update() {
		repaint();
	}

}
