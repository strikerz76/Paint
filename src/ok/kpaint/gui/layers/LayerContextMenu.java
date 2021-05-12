package ok.kpaint.gui.layers;

import javax.swing.*;

import ok.kpaint.*;

public class LayerContextMenu extends JPopupMenu {
	
	public LayerContextMenu(Layers layers, Layer layer) {
		
		JMenuItem deleteButton = new JMenuItem("delete");
		deleteButton.addActionListener(e -> {
			layers.delete(layer);
		});
		JMenuItem selectButton = new JMenuItem("select");
		selectButton.addActionListener(e -> {
			layers.setActive(layer);
		});
		JMenuItem clipBoardButton = new JMenuItem("copy this layer");
		clipBoardButton.addActionListener(e -> {
			ClipboardImage.setClipboard(layer.image());
		});
		JMenuItem hideLayersButton = new JMenuItem("hide other layers");
		hideLayersButton.addActionListener(e -> {
			for(Layer l : layers.getLayers()) {
				if(l != layer) {
					l.setShown(true);
				}
			}
		});
		JMenuItem duplicateButton = new JMenuItem("duplicate");
		duplicateButton.addActionListener(e -> {
			layers.add(Utils.copyImage(layer.image()));
		});
		JMenuItem flipHorizontalButton = new JMenuItem("horizontal reflect");
		flipHorizontalButton.addActionListener(e -> {
			layer.reflectImage(true);
		});
		JMenuItem flipVerticalButton = new JMenuItem("vertical reflect");
		flipVerticalButton.addActionListener(e -> {
			layer.reflectImage(false);
		});
		JMenuItem applyButton = new JMenuItem("apply onto layer below");
		applyButton.addActionListener(e -> {
			layers.applyLayer(layer);
		});
		
		if(layers.active() != layer) {
			this.add(selectButton);
		}
		this.add(deleteButton);
		this.add(clipBoardButton);
		this.add(duplicateButton);
		this.add(flipHorizontalButton);
		this.add(flipVerticalButton);
		this.add(applyButton);
		// TODO when hiding other layer it doesnt update the UI
//		this.add(hideLayersButton);
	}
}
