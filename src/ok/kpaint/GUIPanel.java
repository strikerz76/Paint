package ok.kpaint;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.imageio.plugins.tiff.*;
import javax.swing.*;

import ok.kpaint.gui.*;
import ok.kpaint.gui.layers.*;
import ok.kui.*;

public class GUIPanel extends JPanel {

	private ControllerInterface controllerInterface;
	private ImagePanelInterface imagePanelInterface;
	private HashMap<BrushMode, KRadioButton> modeButtons = new HashMap<>();
	

	private KButton openFile;
	private KButton saveFile;
//	private KButton newFile;
	private KButton undoButton;
	private KButton redoButton;
//	private KButton applyButton;
	private JToggleButton toggleTiling;
	private JToggleButton toggleDarkMode;
	private KSlider brushSize2;
	private JButton brushColor1;
	private JButton brushColor2;
	private JButton transparentButton;
	private ColorSwatches swatchesPanel;
	private JComboBox<BrushShape> brushShape;
	private JPanel fillerPanel;
	
	private LayersPanel layersPanel;
	private Layers layers;
	
	private GUIInterface guiInterface = new GUIInterface() {
		@Override
		public void finishedSelection() {
//			clickModeButton(BrushMode.MOVE);
		}
		@Override
		public void changedColor(Color newColor) {
			brushColor1.revalidate();
			brushColor2.repaint();
			swatchesPanel.choseColor(newColor);
			clickModeButton(BrushMode.BRUSH);
			repaint();
		}
		@Override
		public void changeModeHotkey(BrushMode mode) {
			clickModeButton(mode);
		}
		@Override
		public void switchLayout(boolean withTitles) {
			if(withTitles) {
				setupWithTitles();
			}
			else {
				setupCompact();
			}
		}
	};
	
	public GUIPanel(ControllerInterface controllerInterface, ImagePanelInterface imagePanelInterface, Layers layers) {
		this.controllerInterface = controllerInterface;
		this.imagePanelInterface = imagePanelInterface;
		this.setLayout(new GridBagLayout());
		this.layers = layers;
	}
	
	public GUIInterface getInterface() {
		return guiInterface;
	}
	
	private void clickModeButton(BrushMode mode) {
		modeButtons.get(mode).doClick();
	}
	
	private ButtonGroup setupModeButtons(boolean withTitles) {
		ButtonGroup group = new ButtonGroup();
		for(BrushMode mode : BrushMode.values()) {
			KRadioButton modeButton = KUI.setupKRadioButton(withTitles ? mode.toString() : "", mode.getTooltipText(), mode.getImageIcon());
			modeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					imagePanelInterface.setBrushMode(mode);
				}
			});
			group.add(modeButton);
			if(mode == Brush.DEFAULT_BRUSH.getMode()) {
				modeButton.setSelected(true);
				imagePanelInterface.setBrushMode(mode);
			}
			modeButtons.put(mode, modeButton);
		}
		return group;
	}
	
	private void createGUIElements(boolean withTitles) {
		setupModeButtons(withTitles);

		openFile = KUI.setupKButton(withTitles ? "Open" : "", "Open File", "/open.png");
		openFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				controllerInterface.open();
			}
		});

		saveFile = KUI.setupKButton(withTitles ? "Save" : "", "(Ctrl S) Save File", "/save.png");
		saveFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controllerInterface.save();
			}
		});


//		newFile = KUI.setupKButton(withTitles ? "New Canvas" : "", "(Ctrl N) New Canvas", "/new_canvas.png");
//		newFile.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				imagePanelInterface.newCanvas();
//			}
//		});
		
		
		undoButton = KUI.setupKButton("", "(Ctrl Z) Undo", "/undo.png");
		undoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagePanelInterface.undo();
			}
		});

		redoButton = KUI.setupKButton("", "(Ctrl Shift Z) Redo", "/redo.png");
		redoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagePanelInterface.redo();
			}
		});

		toggleTiling = KUI.setupKToggleButton(withTitles ? "Tiling" : "", "Tiling: enables tiling view which draws copies of the image around it for working on seemless textures", "/tiling_icon.png");
		toggleTiling.addActionListener(e -> {
			imagePanelInterface.showTiling(toggleTiling.isSelected());
		});
		
		toggleDarkMode = KUI.setupKToggleButton(withTitles ? "Dark Mode" : "", "Toggles dark/light canvas background.", "/darkmode.png");
		toggleDarkMode.addActionListener(e -> {
			imagePanelInterface.enableDarkMode(toggleDarkMode.isSelected());
		});
		toggleDarkMode.setSelected(true);
		imagePanelInterface.enableDarkMode(true);
		
		brushSize2 = new KSlider(1, 20);
		brushSize2.setValue(Brush.DEFAULT_BRUSH.getSize());
		brushSize2.addChangeListener(e -> {
			imagePanelInterface.setBrushSize(brushSize2.getValue());
		});

		brushColor1 = KUI.setupColorButton("Main", new HasColor() {
			@Override
			public Color getColor() {
				return imagePanelInterface.getMainColor();
			}
			@Override
			public void setColor(Color color) {
				imagePanelInterface.setMainColor(color);
			}
		});

		brushColor2 = KUI.setupColorButton("Shift", new HasColor() {
			@Override
			public Color getColor() {
				return imagePanelInterface.getAltColor();
			}
			@Override
			public void setColor(Color color) {
				imagePanelInterface.setAltColor(color);
			}
		});
		
		swatchesPanel = new ColorSwatches(imagePanelInterface);
		swatchesPanel.setFocusable(false);
		
		brushShape = new JComboBox<>(BrushShape.values());
		KUI.setupJComponent(brushShape, "Changes brush shape between square and circle", Utils.resizeImageIcon(Utils.loadImageIconResource("/brush_shape.png"), 32, 32));
		brushShape.setSelectedItem(Brush.DEFAULT_BRUSH.getShape());
		brushShape.addActionListener(e -> {
			imagePanelInterface.setBrushShape((BrushShape)brushShape.getSelectedItem());
		});
		
		if(DriverKPaint.NEW_VERSION) {
			layersPanel = new LayersPanel(layers);
		}

		fillerPanel = new JPanel();
		fillerPanel.setOpaque(false);
	}
	
	private JPanel getSeparator(int height, Color color) {
		JPanel sep = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
//				g.setColor(Color.black);
//				g.drawLine(0, getHeight()/2, getWidth()-1, getHeight()/2);
			}
		};
		sep.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		sep.setBackground(color);
		sep.setPreferredSize(new Dimension(100, height));
		return sep;
	}
	
	private void setupWithTitles() {
		this.removeAll();
		createGUIElements(true);
		
		int row = 0;
		
		int sepHeight = 10;
		Color sepColor = Color.LIGHT_GRAY;
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 2;
		c.weightx = 1;
		
		// ############ ROW 0 ################## 
		c.gridx = 0; c.gridy = row++; c.weighty = 1;
		this.add(getSeparator(sepHeight, sepColor), c);
		c.weighty = 0;
		c.gridx = 0; c.gridy = row;
		c.gridwidth = 1;
		this.add(openFile, c);
		c.gridx = 1; c.gridy = row++;
		this.add(saveFile, c);
		c.gridwidth = 2;
//		c.gridx = 0; c.gridy = row++;
//		this.add(newFile, c);
		
//		// ############ ROW 1 ################## 
//		c.gridx = 0; c.gridy = row++; c.weighty = 1;
//		this.add(getSeparator(sepHeight, sepColor), c);
//		c.weighty = 0;
//		c.gridx = 0; c.gridy = row; c.gridwidth = 1;
//		this.add(undoButton, c);
//		c.gridx = 1; c.gridy = row++;
//		this.add(redoButton, c);
//		c.gridwidth = 2;
		
		// ############ ROW 2 ################## 
		c.gridx = 0; c.gridy = row++; c.weighty = 1;
		this.add(getSeparator(sepHeight, sepColor), c);
		c.weighty = 0;
		c.gridx = 0; c.gridy = row++;
		this.add(modeButtons.get(BrushMode.EXTRACT), c);
//		c.gridx = 0; c.gridy = row++;
//		this.add(applyButton, c);
		c.gridwidth = 1;
		c.gridx = 0; c.gridy = row;
		this.add(toggleTiling, c);
		c.gridx = 1; c.gridy = row++;
		this.add(toggleDarkMode, c);
		c.gridwidth = 2;
		
		// ############ ROW 3 ################## 
		c.gridx = 0; c.gridy = row++; c.weighty = 1;
		this.add(getSeparator(sepHeight, sepColor), c);
		c.weighty = 0;
//		c.gridx = 0; c.gridy = row++;
//		this.add(modeButtons.get(BrushMode.MOVE), c);
		c.gridx = 0; c.gridy = row++;
		this.add(modeButtons.get(BrushMode.BRUSH), c);
		c.gridx = 0; c.gridy = row++;
		this.add(modeButtons.get(BrushMode.FILL), c);
		c.gridx = 0; c.gridy = row++;
		this.add(modeButtons.get(BrushMode.ALL_MATCHING_COLOR), c);

		// ############ ROW 4 ################## 
		c.gridx = 0; c.gridy = row++;
		this.add(brushSize2, c);
		c.gridx = 0; c.gridy = row++;
		this.add(brushShape, c);

		// ############ ROW 5 ################## 
		c.gridx = 0; c.gridy = row++; c.weighty = 1;
		this.add(getSeparator(sepHeight, sepColor), c);
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridx = 0; c.gridy = row;
		this.add(brushColor1, c);
		c.gridx = 1; c.gridy = row++;
		this.add(brushColor2, c);
		c.gridwidth = 2;
		c.gridx = 0; c.gridy = row++;
		this.add(modeButtons.get(BrushMode.COLOR_PICKER), c);
		c.gridx = 0; c.gridy = row++;
		this.add(swatchesPanel, c);
		
		// ############ ROW 6 ################## 
		c.gridx = 0; c.gridy = row++; c.weighty = 1;
		this.add(getSeparator(sepHeight, sepColor), c);
		c.weightx = 0;
		c.gridx = 0; c.gridy = row++;
		this.add(layersPanel.getPanel(), c);
		

		// ############ FILLER ################## 
		c.gridx = 0; c.gridy = row++; c.weightx = 1; c.weighty = 5;
		c.fill = GridBagConstraints.BOTH;
		this.add(getSeparator(sepHeight, sepColor), c);
		
		this.revalidate();
	}
	
	private void setupCompact() {
		this.removeAll();
		createGUIElements(false);
		

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		// ############ ROW 0 ################## 
		c.gridx = 0; c.gridy = 0;
		this.add(openFile, c);
		c.gridx = 1; c.gridy = 0;
		this.add(saveFile, c);
//		c.gridx = 2; c.gridy = 0;
//		this.add(newFile, c);
		
		

		// ############ ROW 1 ################## 
		c.gridx = 0; c.gridy = 1;
		this.add(undoButton, c);
		c.gridx = 1; c.gridy = 1;
		this.add(redoButton, c);
		
		// ############ ROW 2 ################## 
		c.gridx = 0; c.gridy = 2;
		this.add(modeButtons.get(BrushMode.EXTRACT), c);
//		c.gridx = 1; c.gridy = 2;
//		this.add(applyButton, c);
		c.gridx = 2; c.gridy = 2;
		this.add(toggleTiling, c);
		
		// ############ ROW 3 ################## 
//		c.gridx = 0; c.gridy = 3;
//		this.add(modeButtons.get(BrushMode.MOVE), c);
		c.gridx = 1; c.gridy = 3;
		this.add(modeButtons.get(BrushMode.BRUSH), c);
		c.gridx = 2; c.gridy = 3;
		this.add(modeButtons.get(BrushMode.FILL), c);
		c.gridx = 3; c.gridy = 3;
		this.add(modeButtons.get(BrushMode.ALL_MATCHING_COLOR), c);

		// ############ ROW 4 ################## 
		c.gridx = 0; c.gridy = 4; c.gridwidth = 5;
		this.add(brushSize2, c);
		c.gridwidth = 1;
		

		// ############ ROW 5 ################## 
		c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
		this.add(brushColor1, c);
		c.gridx = 2; c.gridy = 5; c.gridwidth = 2;
		this.add(brushColor2, c);
		c.gridwidth = 1;
		c.gridx = 4; c.gridy = 5;
		this.add(modeButtons.get(BrushMode.COLOR_PICKER), c);
		
		c.gridx = 0; c.gridy = 6; c.gridwidth = 5;
		this.add(swatchesPanel, c);


		c.gridx = 0; c.gridy = 6; c.gridwidth = 4;
		this.add(new JSeparator(), c);

		
		// ############ FILLER ################## 
		c.gridx = 0; c.gridy = 7; c.weightx = 1; c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		this.add(fillerPanel, c);
		
		this.revalidate();
	}
}
