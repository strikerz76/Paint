package ok.kui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import ok.kpaint.*;

public class KUI {
	public final static BasicStroke dashed = new BasicStroke(1.0f,
	                                                  BasicStroke.CAP_BUTT,
	                                                  BasicStroke.JOIN_MITER,
	                                                  10.0f, new float[] {1, 4}, 0.0f);
	
	public static void setupJComponent(JComponent component, String tooltip, ImageIcon icon) {
		component.setToolTipText(tooltip);
		component.setFocusable(false);
		component.setBackground(Color.black);
		component.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		if(component instanceof AbstractButton) {
			AbstractButton button = (AbstractButton)component;
			button.setMargin(new Insets(0, 0, 0, 0));
			button.setFocusPainted(false);
			button.setBorderPainted(true);
			button.setIcon(icon);
			button.setHorizontalAlignment(SwingConstants.CENTER);
		}
	}

	public static KButton setupKButton(String title, String tooltip, String iconPath) {
		KButton button = new KButton(title);
		ImageIcon icon = Utils.resizeImageIcon(Utils.loadImageIconResource(iconPath), 32, 32);
		setupJComponent(button, tooltip, icon);
		return button;
	}
	

	public static KToggleButton setupKToggleButton(String title, String tooltip, String iconPath) {
		KToggleButton button = new KToggleButton(title);
		ImageIcon icon = Utils.resizeImageIcon(Utils.loadImageIconResource(iconPath), 32, 32);
		setupJComponent(button, tooltip, icon);
		return button;
	}
	
	public static KRadioButton setupKRadioButton(String title, String tooltip, ImageIcon icon) {
		KRadioButton modeButton = new KRadioButton(title);
		setupJComponent(modeButton, tooltip, icon);
		return modeButton;
	}


	public static JButton setupColorButton(String text, HasColor c) {
		int width = 80;
		int height = 40;
		Image background = Utils.resizeImageIcon(
				Utils.loadImageIconResource("/transparentBackground.png"), width, height).getImage();
		JButton chooseColorButton = new KBrushColorButton(text, c, background);
		chooseColorButton.setOpaque(false);
		chooseColorButton.setContentAreaFilled(false);
		chooseColorButton.setPreferredSize(new Dimension(width, height));
		chooseColorButton.setFocusable(false);
		chooseColorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose Color", c.getColor());
				if(newColor != null) {
					c.setColor(newColor);
				}
			}
		});
		return chooseColorButton;
	}
}
