package ok.kpaint.gui.layers;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import ok.kpaint.*;

public class LayersPanel implements LayersListener {
	
	private static final ImageIcon TRASH_ICON = Utils.resizeImageIcon(
	               Utils.loadImageIconResource("/trash.png"), 16, 16);
	private static final ImageIcon HIDDEN_ICON = Utils.resizeImageIcon(
	               Utils.loadImageIconResource("/hidden.png"), 16, 16);
	private static final ImageIcon SHOWN_ICON = Utils.resizeImageIcon(
	               Utils.loadImageIconResource("/shown.png"), 16, 16);
	
	private HashMap<Integer, JPanel> layerPanels = new HashMap<>();
	private Layers layers;
	
	private JPanel panel;
	private JButton addLayerButton;
	
	public LayersPanel(Layers layers) {
		this.layers = layers;
		
		panel = new JPanel();
		panel.setFocusable(false);
		layers.addListener(this);
		
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Layers"));
	}
	
	public JPanel getPanel() {
		return panel;
	}

	private void updateUI() {
		panel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		int row = 0;

		addLayerButton = new JButton("new layer");
		addLayerButton.setFocusable(false);
		addLayerButton.addActionListener(e -> {
			Vec2i newLayerSize = Utils.queryNewLayerSize(addLayerButton,
			                                             new Vec2i(layers.active().w(), layers.active().h()));
			if(newLayerSize != null) {
				BufferedImage newImage = new BufferedImage(newLayerSize.x, newLayerSize.y, BufferedImage.TYPE_4BYTE_ABGR);
				layers.add(newImage);
			}
		});
		c.gridx = 0; c.gridy = row++; c.weightx = 1;;
		panel.add(addLayerButton, c);
		

		for(int i = layers.getLayers().size() - 1; i >= 0; i--) {
			Layer layer = layers.getLayers().get(i);
			JPanel layerPanel = layerPanels.get(layer.id());
			c.gridx = 0; c.gridy = row++; c.weightx = 1; c.gridwidth = 2;
			c.fill = GridBagConstraints.HORIZONTAL;
			panel.add(layerPanel, c);
		}

		panel.revalidate();
		panel.repaint();
	}
	@Override
	public void update() {
		HashMap<Integer, JPanel> newLayerPanels = new HashMap<>();
		for(Layer layer : layers.getLayers()) {
			JPanel layerPanel = layerPanels.get(layer.id());
			if(layerPanel == null) {
				layerPanel = makeLayerPanel(layer);
			}
			newLayerPanels.put(layer.id(), layerPanel);
		}
		layerPanels = newLayerPanels;
		updateUI();
	}
	
	private JPanel makeLayerPanel(Layer layer) {
		JPanel layerPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if(layers.active() == layer) {
					g.setColor(Color.LIGHT_GRAY);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
			}
		};
		layerPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		layerPanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {
				layers.setActive(layer);
			}
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
		});
		JLabel name = new JLabel("L" + layer.id());
		JButton deleteButton = new JButton(TRASH_ICON);
		deleteButton.setMargin(new Insets(0, 0, 0, 0));
		deleteButton.setFocusable(false);
		deleteButton.addActionListener(e -> {
			layers.delete(layer);
		});
		JButton moveUpButton = new JButton("^");
		moveUpButton.setMargin(new Insets(0, 5, 0, 5));
		moveUpButton.setFocusable(false);
		moveUpButton.addActionListener(e -> {
			layers.move(layer, 1);
		});
		JButton moveDownButton = new JButton("v");
		moveDownButton.setMargin(new Insets(0, 5, 0, 5));
		moveDownButton.setFocusable(false);
		moveDownButton.addActionListener(e -> {
			layers.move(layer, -1);
		});
		JToggleButton showhideButton = new JToggleButton(SHOWN_ICON);
		showhideButton.setMargin(new Insets(0, 0, 0, 0));
		showhideButton.setFocusable(false);
		showhideButton.addActionListener(e -> {
			layers.toggleShown(layer);
			showhideButton.setIcon(showhideButton.isSelected() ? HIDDEN_ICON : SHOWN_ICON);
		});
		
		layerPanel.add(name);
		layerPanel.add(deleteButton);
		layerPanel.add(moveUpButton);
		layerPanel.add(moveDownButton);
		layerPanel.add(showhideButton);
		
		LayerContextMenu contextMenu = new LayerContextMenu(layers, layer);
		layerPanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3) {
					contextMenu.show(layerPanel, e.getX(), e.getY());
				}
			}
			@Override
			public void mousePressed(MouseEvent e) { }
			@Override
			public void mouseExited(MouseEvent e) { }
			@Override
			public void mouseEntered(MouseEvent e) { }
			@Override
			public void mouseClicked(MouseEvent e) { }
		});
		return layerPanel;
	}

}
