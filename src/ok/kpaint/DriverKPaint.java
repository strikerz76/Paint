package ok.kpaint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import ok.kpaint.gui.*;
import ok.kpaint.gui.layers.*;

public class DriverKPaint {
	public static final boolean NEW_VERSION = true;
	
	public static final Font MAIN_FONT = new Font("Comic Sans MS", Font.PLAIN, 15);
	public static final Font MAIN_FONT_BIG = new Font("Cooper Black", Font.PLAIN, 16);
	public static final boolean DEBUG = false;
	
	public static final String TITLE = "KPaint 1.1";
	
	private JFrame frame;
	private ImagePanel imagePanel;
	private ImagePanelInterface imagePanelInterface;

	private GUIInterface guiInterface;
	private ControllerInterface controllerInterface;
	

	final JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
	
	private Layers layers = new Layers();;

	public DriverKPaint() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		controllerInterface = new ControllerInterface() {
			@Override
			public void open() {
				int returnVal = fc.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					openImage(file.getAbsolutePath());
				}
			}
			
			@Override
			public void save() {
				saveImage();
			}
		};
		
		frame = new JFrame(TITLE);
		frame.setSize((int)(Toolkit.getDefaultToolkit().getScreenSize().width*0.9), (int)(Toolkit.getDefaultToolkit().getScreenSize().height*0.9));
		frame.setMinimumSize(new Dimension(670, 500));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);

		frame.setIconImage(Utils.loadImageIconResource("resources/icon.png").getImage());
		frame.setVisible(true);
		
		imagePanel = new ImagePanel(layers);
		
		GUIPanel guiPanel;
		frame.add(imagePanel, BorderLayout.CENTER);
		imagePanelInterface = imagePanel.getInterface();
		
		guiPanel = new GUIPanel(controllerInterface, imagePanelInterface, layers);
		guiInterface = guiPanel.getInterface();
		guiInterface.switchLayout(true);
		frame.add(guiPanel, BorderLayout.WEST);

		imagePanelInterface.resetView();
		frame.repaint();
		imagePanel.requestFocus();
		
		
		imagePanel.setGUIInterface(guiInterface);
		imagePanel.setControllerInterface(controllerInterface);
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException ex) {
				}
			}
		});
		
		frame.repaint();
		frame.revalidate();
		imagePanelInterface.resetView();
	}
	
	private String getExtension(String filename) {
		int lastDot = filename.lastIndexOf(".");
		if(lastDot == -1 || lastDot == filename.length() - 1) {
			return "";
		}
		return filename.substring(lastDot + 1);
	}

	private void openImage(String path) {
		BufferedImage image = loadImage(path);
		if (image != null) {
			imagePanel.addImageLayer(image);
			imagePanelInterface.resetView();
			updateTitle(path);
		}
	}
	
	private void saveImage() {
		int returnVal = fc.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			String path = file.getAbsolutePath();
			String ext = getExtension(path);
			if(!ext.toLowerCase().equals("png")) {
				ext = "png";
				file = new File(file.getAbsolutePath() + "." + ext);
				JOptionPane.showMessageDialog(frame, "ERROR! Can only write png files.\nSaving to " + file.getAbsolutePath());
			}
			BufferedImage current = imagePanel.getCurrentImage();
			try {
				ImageIO.write(current, ext, file);
				updateTitle(file.getAbsolutePath());
			} catch (IOException e1) {
				System.err.println("FileName = " + path);
				e1.printStackTrace();
			}
		}
	}
	
	private void updateTitle(String filename) {
		frame.setTitle(TITLE + "     " + filename);
	}

	public BufferedImage loadImage(String fileName) {
		File file = new File(fileName);
		try {
			BufferedImage read = ImageIO.read(file);
			fc.setCurrentDirectory(file.getParentFile());
			fc.setSelectedFile(file);
			return read;
		} catch (IOException e) {
			System.err.println("File name = " + fileName);
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		DriverKPaint p = new DriverKPaint();
		if (args.length > 0) {
			p.openImage(args[0]);
			p.imagePanelInterface.resetView();
		}
		else {
			p.layers.add();
			p.imagePanelInterface.resetView();
		}
	}

}
