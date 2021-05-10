package ok.kpaint.gui.layers;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class LayersPanel implements LayersListener {
	
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
			layers.add(0);
		});
		c.gridx = 0; c.gridy = row++; c.weightx = 1;;
		panel.add(addLayerButton, c);
		

		for(int i = layers.getLayers().size() - 1; i >= 0; i--) {
			Layer layer = layers.getLayers().get(i);
			JPanel layerPanel = layerPanels.get(layer.id());
			c.gridx = 0; c.gridy = row++; c.weightx = 1; c.gridwidth = 2;
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
					g.setColor(Color.gray);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
			}
		};
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
		JButton deleteButton = new JButton("d");
		deleteButton.setFocusable(false);
		deleteButton.addActionListener(e -> {
			layers.delete(layer);
		});
		JButton moveUpButton = new JButton("^");
		moveUpButton.setFocusable(false);
		moveUpButton.addActionListener(e -> {
			layers.move(layer, 1);
		});
		JButton moveDownButton = new JButton("v");
		moveDownButton.setFocusable(false);
		moveDownButton.addActionListener(e -> {
			layers.move(layer, -1);
		});
		JToggleButton showhideButton = new JToggleButton("h");
		showhideButton.setFocusable(false);
		showhideButton.addActionListener(e -> {
			layers.toggleShown(layer);
		});
		
		layerPanel.add(name);
		layerPanel.add(deleteButton);
		layerPanel.add(moveUpButton);
		layerPanel.add(moveDownButton);
		layerPanel.add(showhideButton);
		return layerPanel;
	}

}
