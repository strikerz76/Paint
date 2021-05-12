# KPaint 1.2 alpha

## Controls
- Left mouse to draw, do actions, or use layer handles.
- Right click to open layer context menu
- Right or middle mouse drag to move the camera
- Mouse wheel to zoom in and out

- CTRL+N makes a new layer
- CTRL+S saves to file system
- CTRL+V pastes clipboard contents on a new layer
- CTRL+C copies entire canvas to clipboard
- CTRL+D duplicates selected layer
- SPACEBAR resets the camera view
- SHIFT swaps main color with alt color
- P switches to color picker
- E switches to extract mode
- B switches to brush
- F switches to fill
- A switches to matching color mode


## Removed Features
- UNDO/REDO

## New Features
- Added Layers feature
	- when you open a new image, paste an image, or make a new canvas, KPaint will create a new layer for it.
	- Layers GUI: 
		- select
		- delete
		- show/hide
		- change ordering
	- Right click on a layer in the Layers GUI or directly on the canvas:
		- delete
		- copy
		- duplicate
		- reflect horizontal or vertical
		- apply to layer below
	- Use the canvas handles to:
		- resize
		- stretch
		- move
	- Extract mode will extract a section of all currently visible layers into a new layer

- Added toggleable dark mode

## Changed Features
- There is now only a main brush color that can be used with left mouse. Pressing shift swaps the main color with the alternate color.
- CTRL+C now copies composed image of all the layers. To copy a single layer, use the right click menu or hide all other layers.
