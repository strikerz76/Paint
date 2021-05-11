package ok.kpaint.gui;

import java.awt.image.*;

import ok.kpaint.*;

public enum HandleType {

	RESIZE("/resize_north.png", "/resize_northeast.png"),
	STRETCH("/stretch_north.png", "/stretch_northeast.png"),
	MOVE("/move.png", "/move.png")
	;
	
	BufferedImage sideIcon;
	BufferedImage cornerIcon;
	
	HandleType(String sideIconFile, String cornerIconFile) {
		sideIcon = Utils.loadImage(sideIconFile);
		cornerIcon = Utils.loadImage(cornerIconFile);
	}
}
