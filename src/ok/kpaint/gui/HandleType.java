package ok.kpaint.gui;

import java.awt.image.*;

import ok.kpaint.*;

public enum HandleType {

	RESIZE("/resize_north.png", "/resize_north.png"),
	STRETCH("/stretch_north.png", "/stretch_north.png"),
	MOVE("/resize_north.png", "/resize_north.png")
	;
	
	BufferedImage sideIcon;
	BufferedImage cornerIcon;
	
	HandleType(String sideIconFile, String cornerIconFile) {
		sideIcon = Utils.loadImage(sideIconFile);
		cornerIcon = Utils.loadImage(cornerIconFile);
	}
}
