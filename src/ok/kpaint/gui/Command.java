package ok.kpaint.gui;

import ok.kpaint.*;
import ok.kpaint.gui.layers.*;

public class Command {

	public final Layer layer;
	public final Handle handle;
	public Vec2i mouseStartPixel;
	public Vec2i mouseEndPixel;
	
	public Command(Layer layer, Handle handle, Vec2i mouseStartPixel) {
		this.layer = layer;
		this.handle = handle;
		this.mouseStartPixel = mouseStartPixel;
		this.mouseEndPixel = mouseStartPixel;
	}
}
