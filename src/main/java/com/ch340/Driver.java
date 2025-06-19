package com.ch340;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.github.sarxos.webcam.*;
import com.github.sarxos.webcam.ds.dummy.WebcamDummyDriver;

public class Driver {

	public static final String TIME_LAPSE_SAVES = "./TimeLapses/";
	public static final DimensionOption[] VIEW_SIZE_OPTIONS = new DimensionOption[] {
			new DimensionOption(WebcamResolution.QVGA.getSize()), new DimensionOption(WebcamResolution.NHD.getSize()),
			new DimensionOption(WebcamResolution.VGA.getSize()), new DimensionOption(WebcamResolution.SVGA.getSize()),
			new DimensionOption(WebcamResolution.XGA.getSize()), new DimensionOption(WebcamResolution.HD.getSize()),
			new DimensionOption(new Dimension(1280, 960)) ,
            new DimensionOption(new Dimension(1920, 1080)) };
	public static final Dimension[] VIEW_SIZE_DIMENSIONS;
	static {
		VIEW_SIZE_DIMENSIONS = new Dimension[VIEW_SIZE_OPTIONS.length];
		for (int i = 0; i < VIEW_SIZE_OPTIONS.length; i++) {
			VIEW_SIZE_DIMENSIONS[i] = VIEW_SIZE_OPTIONS[i].get();
		}
	}

	JFrame frame;
	JComboBox<Webcam> webcamSelect;
	JComboBox<DimensionOption> viewSizeSelect;
	JTextField delaySelect;
	JToggleButton startButton;
	JButton resetButton;
	JPanel imagePanelContainer;
	JTextField currentStatus;

	Webcam selectedWebcam;

	volatile BufferedImage currentImage;
	String newFolderName;
	volatile int imageCounter;
	Thread captureThread;
	Thread timingThread;
	volatile int delay;
	volatile boolean stopCaptureThread;
	volatile boolean stopTimingThread;

	volatile boolean getShot;

	public void updateWebcamList() {
		Webcam[] webcams = Webcam.getWebcams().toArray(new Webcam[0]);
		DefaultComboBoxModel<Webcam> model = new DefaultComboBoxModel<>(webcams);

		if (webcamSelect.getSelectedIndex() != -1) {
			String currentlySelected = ((Webcam) webcamSelect.getSelectedItem()).getName();
			for (Webcam webcam : webcams) {
				if (webcam.getName().equals(currentlySelected)) {
					model.setSelectedItem(webcam);
				}
			}
		}
		webcamSelect.setModel(model);
		webcamSelect.setEnabled(true);
		selectedWebcam = (Webcam) webcamSelect.getSelectedItem();
		openWebcam(selectedWebcam);
		updateViewSizeForWebcam(selectedWebcam);
	}

	private void updateViewSizeForWebcam(Webcam cam) {
		cam.setCustomViewSizes(VIEW_SIZE_DIMENSIONS);
		Dimension currentViewSize = cam.getViewSize();
		Object preselected = null;
		for (int i = 0; i < VIEW_SIZE_OPTIONS.length; i++) {
			if (currentViewSize.equals(VIEW_SIZE_OPTIONS[i].get())) {
				preselected = VIEW_SIZE_OPTIONS[i];
			}
		}
		DefaultComboBoxModel<DimensionOption> model2 = new DefaultComboBoxModel<>(VIEW_SIZE_OPTIONS);
		model2.setSelectedItem(preselected);
		viewSizeSelect.setModel(model2);
		viewSizeSelect.setEnabled(true);
	}

	private void closeCurrentWebcam() {
		if (selectedWebcam != null) {
			viewSizeSelect.setEnabled(false);
			selectedWebcam.close();
		}
		imagePanelContainer.removeAll();
	}

	private void addWebcamView(Webcam cam) {
		WebcamPanel imagePanel = new WebcamPanel(selectedWebcam);
		imagePanelContainer.add(imagePanel);
		frame.validate();
	}

	private void openWebcam(Webcam cam) {
		closeCurrentWebcam();
		this.selectedWebcam = cam;
		System.err.println("Opening " + selectedWebcam);
		selectedWebcam.open();
		updateViewSizeForWebcam(selectedWebcam);

//		printWebcamDetails(selectedWebcam);
		addWebcamView(selectedWebcam);
	}

	private void changeViewSize(DimensionOption dim) {
		frame.setEnabled(false);
		System.out.println("Updating view size of " + selectedWebcam.getName() + " from "
				+ selectedWebcam.getViewSize().width + " x " + selectedWebcam.getViewSize().height + " to "
				+ dim.get().width + " x " + dim.get().height);
		closeCurrentWebcam();
		selectedWebcam.setViewSize(dim.get());
		openWebcam(selectedWebcam);
		frame.setEnabled(true);
	}

	public Driver() {
		if (Webcam.getWebcams().size() == 0) {
			Webcam.setDriver(new WebcamDummyDriver(20));
		}

		frame = new JFrame("Time Lapse");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 500);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				stopCaptureThread();
				closeCurrentWebcam();
			}
		});

		webcamSelect = new JComboBox<Webcam>();
		webcamSelect.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				openWebcam((Webcam) e.getItem());
			}
		});
		viewSizeSelect = new JComboBox<DimensionOption>(VIEW_SIZE_OPTIONS);
		viewSizeSelect.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				changeViewSize((DimensionOption) e.getItem());
			}
		});
		delaySelect = new JTextField("1000", 10);
		resetButton = new JButton("Reset");
		resetButton.addActionListener(e -> {
			initFolderForNewTimelapse();
		});
		JButton captureNowButton = new JButton("Capture Now");
		captureNowButton.addActionListener(e -> {
			captureOneImage();
		});
		startButton = new JToggleButton("Start");
		startButton.addActionListener(e -> {
			if (startButton.isSelected()) {
				startTimelapse();
			} else {
				stopTimeLapse();
			}
		});
		
		currentStatus = new JTextField("0", 5);
		currentStatus.setEditable(false);
		

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(webcamSelect);
		buttonPanel.add(viewSizeSelect);
		buttonPanel.add(delaySelect);
		buttonPanel.add(startButton);
		buttonPanel.add(captureNowButton);
		buttonPanel.add(resetButton);
		buttonPanel.add(currentStatus);

		imagePanelContainer = new JPanel();
		imagePanelContainer.setLayout(new BorderLayout());

		frame.add(buttonPanel, BorderLayout.NORTH);
		frame.add(imagePanelContainer, BorderLayout.CENTER);
		webcamSelect.setEnabled(false);
		viewSizeSelect.setEnabled(false);

		updateWebcamList();
		initFolderForNewTimelapse();
		startCaptureThread();

		frame.setVisible(true);
	}

	private volatile Semaphore captureImage = new Semaphore(0);

	private void startTimingThread() {
		if (timingThread != null ) {
			System.out.println("ERROR: trying to start timing thread when not already null");
		}
		timingThread = new Thread(() -> {
			System.err.println("Timing thread started with delay: " + delay);
			try {
				while (!stopTimingThread) {
					Thread.sleep(delay);
					if (stopTimingThread) {
						break;
					}
					captureImage.release();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Timing thread ending");
		});
		timingThread.start();
	}
	
	private void captureOneImage() {
		captureImage.release();
	}

	public void pretimelapseSetup() {
//		JPanel imagePanel = new JPanel() {
//			@Override
//			public void paintComponent(Graphics g) {
//				super.paintComponent(g);
//				g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
//			}
//		};
//		imagePanelContainer.removeAll();
//		imagePanelContainer.add(imagePanel);
	}

	public void startCaptureThread() {
		captureThread = new Thread(() -> {
			System.err.println("Capture thread Started");
			try {
				while (!stopCaptureThread) {
					currentImage = selectedWebcam.getImage();
					String filename = String.format("image%05d.jpg", imageCounter++);
					updateCurrentStatusText();
					try {
						ImageIO.write(currentImage, "jpg", new File(newFolderName + filename));
					} catch (IOException ee) {
						ee.printStackTrace();
					}
					frame.repaint();
					captureImage.acquire();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Capture thread ending");
		});
		captureThread.start();
	}

	public void startTimelapse() {
		pretimelapseSetup();
		try {
			delay = Integer.parseInt(delaySelect.getText());
		} catch (NumberFormatException ee) {
			System.out.println("Invalid delay.");
			JOptionPane.showMessageDialog(frame, "Invalid delay. Must be an integer.");
		}
		webcamSelect.setEnabled(false);
		viewSizeSelect.setEnabled(false);
		delaySelect.setEnabled(false);
		resetButton.setEnabled(false);
		startButton.setText("Stop");

//		startCaptureThread();
		startTimingThread();
	}

	private void stopTimingThread() {
		// stop the timing thread
		stopTimingThread = true;
		captureOneImage();
		// reset to regular webcam view
//		imagePanelContainer.removeAll();
//		addWebcamView(selectedWebcam);
		
		try {
			System.out.println("Joining timing thread");
			timingThread.join();
			System.out.println("Joined timing thread");
			timingThread = null;
			stopTimingThread = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private void stopCaptureThread() {
		stopCaptureThread = true;
		captureOneImage();
		try {
			System.out.println("Joining capture thread");
			captureThread.join();
			System.out.println("Joined capture thread");
			captureThread = null;
			stopCaptureThread = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stopTimeLapse() {
		stopTimingThread();
		delay = 100;
		webcamSelect.setEnabled(true);
		viewSizeSelect.setEnabled(true);
		delaySelect.setEnabled(true);
		resetButton.setEnabled(true);
		startButton.setText("Start");
	}

	private void initFolderForNewTimelapse() {
		imageCounter = 0;
		updateCurrentStatusText();
		long name = System.currentTimeMillis() / 1000 - 1587594000L;
		newFolderName = TIME_LAPSE_SAVES + name + "/";
		File file = new File(newFolderName);
		file.mkdirs();
	}
	private void updateCurrentStatusText() {
		currentStatus.setText("" + imageCounter);
	}

	public static void main(String[] args) {
		new Driver();
	}

	private void printWebcamDetails(Webcam cam) {
		System.out.println("Name: " + cam.getName());
		System.out.println("FPS: " + cam.getFPS());
		System.out.println("View Size: " + cam.getViewSize().width + ", " + cam.getViewSize().height);

		System.out.print("View Sizes: ");
		for (Dimension dim : cam.getViewSizes()) {
			System.out.print(dim.width + " x " + dim.height + ", ");
		}
		System.out.println();

		System.out.print("Custom View Sizes: ");
		for (Dimension dim : cam.getCustomViewSizes()) {
			System.out.print(dim.width + " x " + dim.height + ", ");
		}
		System.out.println();

		System.out.println("isImageNew: " + cam.isImageNew());
		cam.setParameters(null);
	}
}