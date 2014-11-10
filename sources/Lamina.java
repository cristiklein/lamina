/* Lamina.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that implements a GUI for running
 * the leaf-extracting process.
 * 
 *
 * This file is part of Lamina.
 *
 * Lamina is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA.
 *
 * Software requirements:
 * *Java 1.4.x JRE or later
 *  http://java.sun.com/javase/downloads/
 * *Java Advanced Imaging (JAI) 1.1.3 or later
 *  http://java.sun.com/javase/technologies/desktop/media/jai/
 *
*/

import java.io.*;
import java.util.*;
import java.util.EventListener;
import java.beans.*;
import java.lang.Math.*;
import java.net.URL;
import java.text.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.Component;
import javax.help.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import com.sun.media.jai.codec.*;


/**
Main class for LAMINA (Leaf shApe deterMINAtion).
LAMINA is a tool for the automated analysis of
images of leaves. LAMINA has been designed to provide classical measures of leaf shape (blade dimensions) and
size (area) that are typically required for correlation analysis to biomass productivity, as well as measures that
indicate asymmetry in leaf shape, leaf serration traits, and measures of herbivory damage (missing leaf area). In
order to allow principal component analysis (PCA) to be performed, the location of a chosen number of equally
spaced markers can optionally be returned.
*/
public class Lamina extends WindowAdapter implements ActionListener, PropertyChangeListener, ChangeListener, MouseListener, MouseMotionListener, KeyListener
{
	public final static String APPL_NAME = "LAMINA";
	public final static String APPL_NAME_LONG = "LAMINA - Leaf shApe deterMINAtion";
	public final static String APPL_VERSION = "1.0.2";
	public final static String APPL_DATE = "2008-06-12";
	public final static String LOGO_FILE = "img/Lamina32x32.png";
	public final static String LOGO_ABOUT = "img/Lamina32x32.png";
	public final static String HELP_LOCATION = "help/LAMINA.hs";
	public final static String COPYRIGHT_HOLDER = "Max Bylesjo";
	public final static String COPYRIGHT_YEAR = "2007-2008";
	public final static int CROP_PADDING = 20;
	
	
	
	private final static String OUTPUT_FNAME_SEG = "_seg";
	private final static String OUTPUT_FNAME_CROPPED = "_cropped";
	private final static String OUTPUT_FNAME_STATS = "_stats";
	private final static String OUTPUT_FNAME_LOG = "_log";
	
	
	private final static String SAN_BUTTON_CAPTION_DEF = "Save and next";
	private final static String NEXT_BUTTON_CAPTION_DEF = "Next";
	
	
	private final static int SCROLLBAR_INC = 50;
	private final static float ZOOM_MAX = 20.0f;
	private final static float ZOOM_MIN = 0.01f;
	private final static float ZOOM_INC = 0.8f;
	private final static Color DEFAULT_FG_COLOR = new Color(60, 90, 20);
	private final static Color DEFAULT_BG_COLOR = new Color(255, 255, 255);
	private final static int ALLOWED_UNDO_SIZE = 50;
	
	// the minimum distance (in pixel distances)  where we connect the mouse pointer to the
	// closest border
	private final static double MAX_DIST_BORDER_MOUSE_HOVER = 50;  
	
	
	// to remove black edge segments, two constants are needed
	// 1) The relative distance from a pixel to the edge, so that only certain pixels are checked
	// 2) The average intensity value threshold used to denote the pixels as 'black edge' --> truncate or 'normal pixel' --> do nothing
	private final static double EDGE_REL_DIST = 0.15; //the (relative) distance from the center req. to be the classified as 'edge'
	private final static double EDGE_INT_THRESH = 25; //the minimum (average) intensity value allowed
	
	protected final static String INI_FILENAME = "Lamina.ini";
	
	private static Runtime runTime;
	
	protected static File iniFile, inputPath, outputPath;
	protected static String[] listOfImageFiles, listOfImageFilesPath, listOfOutputImagesCropped;
	protected static int[] listOfRowsWritten;
	protected static String outputFileBatch;
	protected static int currentImageFileInd;
	protected static String calibFilename;
	
	protected static double calibWidth, calibHeight;
	
	protected static JComboBox cbZoomLevel, cbZoomLevelCropped;
	protected static Rectangle2D.Double zoomArea, zoomAreaCropped;
	protected static int zoomHorizDelta = 0;
	protected static int zoomVertDelta = 0;
	protected static int zoomCount = 0;
	
	protected static int modifierContourPB = 2;
	protected static boolean runContourID = false;
	protected static boolean ctrlIsPressed = false;
	protected static boolean saveClickWasOK = true;
	protected static boolean statFileHeaderWritten = false;
	
	protected static double putativeScaleParam = 0.0;
	
	// for segmentation
	private static Vector vecSegObjs, vecSegObjsOrg, vecSegObjCenters, vecSegObjCentersOrg, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBordersShort,
		vecSegObjBordersShortLandmarks, vecSegObjBorderBP, vecSegObjBorderBPInner,
		vecHorizVertLines, vecIntersectPoints, vecContours, vecContourComplete, vecContourUnique,
		vecContourBorder, vecContourHotspotConnections, vecContourHotspotIndices, vecContourIndents;
	private static int[][] imgMatGrayscaleOrg, imgMatGrayscaleTemplateOrg, imgMatGrayscale, imgMatGrayscaleTemplate,
		imgSeg, imgSegCropped, contourUnique, contourComplete; //imgSegNoCavities, 
	private static byte[][] imgMatBinary, imgMatBinaryOrg;
	
	
	private static LaminaSettings dlgSettings;
	
	private static JFrameExt frame;
	private static Container content;
	private static JSplitPane splitPane;
	private static JPanel panelSplit1, panelSplit2;
	private static JPanel panelStatus, panelLeft, panelCenter, panelTop, panelTopTop, panelTopBottom,
		panelBottom, panelBottomTop, panelBottomCenter, panelBottomBottom,
		panelNumLandmarks, panelBottomCenterCropped, panelScaleParamSpinner;
	private static FilenameFilter ffJPEG;
	private static FilenameFilterImage ffImage;
	private static JLabel labelStatus, labelOutputFile, labelSpinnerMetaCol, labelSpinnerMetaRow,
		labelSpinnerRow, labelSpinnerCol, labelNumLandmarks, labelBrushSize, labelScaleParamSpinner,
		labelNumLandmarksSpinner;
	private static JButton buttonLoadImage, buttonLoadCalib, buttonLoadFolder, buttonSave,
		buttonSaveAndNext, buttonNext, buttonReanalyze, buttonApplyFilterContour,
		buttonCrop, buttonApplyNoContour, buttonCalcStats, buttonSaveImage, buttonSaveImageCropped,
		buttonZoomIn, buttonZoomOut, buttonZoomInCropped, buttonZoomOutCropped,
		buttonRevert, buttonSettings, buttonHelp, buttonAbout;
	private static JDialogExtract dialogProgress;
	private static LaminaCalibProgress dialogCalibProgress;
	private static JTextField tfScaleParam;
	private static JSpinner spinnerScaleParam, spinnerNumLandmarks, spinnerMetaCol, spinnerMetaRow, spinnerRow, spinnerCol;
	private static JDialogAbout mabout;
	
	private static Point pointMouseLastMoved;
	private static Point pointMousePressed;
	private static Point pointMouseReleased;
	protected static int selectedObjId = -1; //for keeping track of which object that is selected
	
	private boolean mouseDragged = false;
	private boolean splitPaneMinimized = true;
	
	//private FileDialog fd;
	private static JComponentDisplay componentImage, componentImageCropped;
	private static JScrollPane scrollPane, scrollPaneCropped;
	private static JProgressBar pbStatus;
	private boolean isLoaded = false, isCropped = false;
	
	private static ApplicationSettings settings;

	private static String currentFilename = new String();
	private static File currentFile, lastInDir, lastOutDir;
	
	
	// various images
	//private PlanarImage img;
	private static BufferedImage imgDisplay, imgOrg, imgCropped, imgGrayscale,
		imgCalib, imgCalibGrayscale;
	
	private float zoomLevel = 0.4f;
	private float zoomLevelNew = 1.0f;
	private float zoomLevelCropped = -1.0f;
	private float zoomLevelCroppedNew = 1.0f;
	private boolean rightButtonPressed = false;
	
	
	
	//protected Raster undoRaster;
	
	
	/**
	* Main constructor. Initializes all parameters and sets up the GUI.
	*
	*/
	public Lamina() 
	{
		runTime = Runtime.getRuntime();
		
		ffJPEG = new FilenameFilterJPEG();
		ffImage = new FilenameFilterImage();
		
		//create frame
		frame = new JFrameExt(Lamina.APPL_NAME_LONG + " version " + Lamina.APPL_VERSION);
		frame.setSize(800, 600);
		content = frame.getContentPane();
		//content.setBackground(Color.white);
		content.setLayout(new BorderLayout()); 
		frame.addWindowListener(this);
		//Image logo = Toolkit.getDefaultToolkit().getImage("masqot32x32.gif");
		//frame.setIconImage(logo);
		
		//fetch the logo from the jar file
		try
		{
			URL urlImage = this.getClass().getResource(this.LOGO_FILE);
			Image logo = Toolkit.getDefaultToolkit().getImage(urlImage);
			frame.setIconImage(logo);
		} catch (Exception ex)
		{
			//ignore this for now
		}
		
		
		JPanel panelStatus = new JPanel( new GridLayout(1, 2) );
		labelStatus = new JLabel(Lamina.APPL_NAME_LONG + " version " + Lamina.APPL_VERSION); //+ ". Copyright (c) " + COPYRIGHT_HOLDER + " " + COPYRIGHT_YEAR);
		labelOutputFile = new JLabel();
		panelStatus.add( labelStatus );
		//panelStatus.add(new JSeparator(SwingConstants.VERTICAL) );
		panelStatus.add( labelOutputFile );
		
		spinnerScaleParam = new JSpinner( new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1) );
		spinnerScaleParam.addChangeListener(this);
		spinnerScaleParam.setMaximumSize( new Dimension(50, spinnerScaleParam.getPreferredSize().height ) );
		spinnerScaleParam.setPreferredSize( new Dimension(50, spinnerScaleParam.getPreferredSize().height ) );

		labelScaleParamSpinner = new JLabel("Scaling (pixels/mm) ");
		panelScaleParamSpinner = new JPanel( new BorderLayout() );
		panelScaleParamSpinner.add(labelScaleParamSpinner, BorderLayout.CENTER);
		panelScaleParamSpinner.add(spinnerScaleParam, BorderLayout.EAST);

		labelNumLandmarks = new JLabel("Boundary coords: ");
		spinnerNumLandmarks = new JSpinner( new SpinnerNumberModel(50, 0, 10000, 1) );
		spinnerNumLandmarks.addChangeListener(this);
		panelNumLandmarks = new JPanel( new BorderLayout() );
		panelNumLandmarks.setBorder( BorderFactory.createEtchedBorder() );
		panelNumLandmarks.add(labelNumLandmarks, BorderLayout.CENTER);
		panelNumLandmarks.add(spinnerNumLandmarks, BorderLayout.EAST);
		
		
		
		//get the location of the running program
		//to deduce the INI file location
		URL location = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		String locationStr = location.getPath();
		locationStr = locationStr.replaceAll("\\%20", " "); //Windows-specific problem

		//System.err.println(locationStr);
		File path = new File(locationStr );
		if (path.isFile())
			path = path.getParentFile();

		if (path.exists())
		{
			iniFile = new File(path, INI_FILENAME);
			System.err.println("INI file: " + iniFile);

			settings = new ApplicationSettings(APPL_NAME);
			
			if (settings.readIni(iniFile))
			{
				System.err.println("Successfully read INI file");
				
				lastInDir = settings.getInputDir();
				lastOutDir = settings.getOutputDir();
				
			} else
			{
				System.err.println("Failed to read INI file");
			}
			
			if (settings.getScaleParam() > 0)
				((SpinnerNumberModel)spinnerScaleParam.getModel()).setValue( new Double( settings.getScaleParam() ) );
			
			if (settings.getNumLandmarks() > 0)
				((SpinnerNumberModel)spinnerNumLandmarks.getModel()).setValue( new Integer( settings.getNumLandmarks() ) );
			
			
			if (settings.getWindowLocation() != null)
				frame.setLocation(settings.getWindowLocation());
			if (settings.getWindowSize() != null)
				frame.setSize(settings.getWindowSize());
			if (settings.getInputDir() != null)
				lastInDir = settings.getInputDir();
			if (settings.getOutputDir() != null)
				lastOutDir = settings.getOutputDir();
			
			//read the ini file and apply the settings
			//if (this.readIni())
			//	this.applyIni();

		} else
		{
			System.err.println("Unknown path for running process! Ignoring INI file.");
		}
	
		
		
		/*
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		panelSplit1 = new JPanel( new BorderLayout() );
		tabbedPane.add("Tab 1", panelSplit1);
		panelSplit2 = new JPanel( new BorderLayout() );
		tabbedPane.add("Tab 2", panelSplit2);
		*/
		
		panelSplit1 = new JPanel(new BorderLayout() );
		panelSplit2 = new JPanel(new BorderLayout() );
		panelSplit2.setVisible(false);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
			panelSplit2, panelSplit1);
		
		splitPane.setDividerLocation( (int)frame.getSize().getHeight() );
		splitPane.setOneTouchExpandable(true);
		splitPane.addPropertyChangeListener(this);
		splitPane.setLastDividerLocation( (int)frame.getSize().getHeight()+300 );
		
		//splitPane.setDividerLocation( (int)frame.getSize().getHeight() );
		splitPane.firePropertyChange(JSplitPane.ONE_TOUCH_EXPANDABLE_PROPERTY, true, false);

		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize1 = new Dimension(0, 0);
		splitPane.setMinimumSize(minimumSize1);	
		Dimension minimumSize2 = new Dimension(0,0);
		splitPane.setMinimumSize(minimumSize2);
		

		
		panelLeft = new JPanel( new GridLayout(20, 1) );
		panelCenter = new JPanel( new BorderLayout() );
		panelTop = new JPanel( new BorderLayout() );
		panelBottom = new JPanel( new BorderLayout() );
		panelBottomTop = new JPanel( new GridLayout(1, 1) );
		panelBottomCenter = new JPanel( new GridLayout(1, 3) );
		panelBottomBottom = new JPanel( new GridLayout(1, 5) );
		
		/** Right panel **/
		labelSpinnerMetaCol = new JLabel("Meta columns: ");
		spinnerMetaCol = new JSpinner( new SpinnerNumberModel(4, 1, 100, 1) );
		spinnerMetaCol.addChangeListener(this);
		JPanel panelTopSpinnerMetaCol = new JPanel( new BorderLayout() );
		panelTopSpinnerMetaCol.setBorder( BorderFactory.createEtchedBorder() );
		panelTopSpinnerMetaCol.add(labelSpinnerMetaCol, BorderLayout.CENTER);
		panelTopSpinnerMetaCol.add(spinnerMetaCol, BorderLayout.EAST);
		
		labelSpinnerMetaRow = new JLabel("Meta rows: ");
		spinnerMetaRow = new JSpinner( new SpinnerNumberModel(12, 1, 100, 1) );
		spinnerMetaRow.addChangeListener(this);
		JPanel panelTopSpinnerMetaRow = new JPanel( new BorderLayout() );
		panelTopSpinnerMetaRow.setBorder( BorderFactory.createEtchedBorder() );
		panelTopSpinnerMetaRow.add(labelSpinnerMetaRow, BorderLayout.CENTER);
		panelTopSpinnerMetaRow.add(spinnerMetaRow, BorderLayout.EAST);

		labelSpinnerCol = new JLabel("Columns: ");
		spinnerCol = new JSpinner( new SpinnerNumberModel(24, 1, 100, 1) );
		spinnerCol.addChangeListener(this);
		JPanel panelTopSpinnerCol = new JPanel( new BorderLayout() );
		panelTopSpinnerCol.setBorder( BorderFactory.createEtchedBorder() );
		panelTopSpinnerCol.add(labelSpinnerCol, BorderLayout.CENTER);
		panelTopSpinnerCol.add(spinnerCol, BorderLayout.EAST);
		
		labelSpinnerRow = new JLabel("Rows: ");
		spinnerRow = new JSpinner( new SpinnerNumberModel(12, 1, 100, 1) );
		spinnerRow.addChangeListener(this);
		JPanel panelTopSpinnerRow = new JPanel( new BorderLayout() );
		panelTopSpinnerRow.setBorder( BorderFactory.createEtchedBorder() );
		panelTopSpinnerRow.add(labelSpinnerRow, BorderLayout.CENTER);
		panelTopSpinnerRow.add(spinnerRow, BorderLayout.EAST);

		
		
		//panelTop.add(panelTopSpinnerMetaCol);
		//panelTop.add(panelTopSpinnerMetaRow);
		//panelTop.add(panelTopSpinnerCol);
		//panelTop.add(panelTopSpinnerRow);
		
		/** Right panel **/
		buttonLoadFolder = new JButton("Open image folder");
		buttonLoadFolder.setMnemonic(KeyEvent.VK_O);
		buttonLoadFolder.addActionListener(this);
		buttonLoadImage = new JButton("Open single image");
		buttonLoadImage.setMnemonic(KeyEvent.VK_P);
		buttonLoadImage.addActionListener(this);
		buttonSave = new JButton("Save");
		buttonSave.setMnemonic(KeyEvent.VK_S);
		buttonSave.addActionListener(this);
		buttonSave.setEnabled(false);
		buttonSaveAndNext = new JButton(SAN_BUTTON_CAPTION_DEF + " >>>");
		buttonSaveAndNext.setMnemonic(KeyEvent.VK_N);
		buttonSaveAndNext.addActionListener(this);
		buttonSaveAndNext.setEnabled(false);
		buttonNext = new JButton(NEXT_BUTTON_CAPTION_DEF + " >>>");
		buttonNext.setMnemonic(KeyEvent.VK_X);
		buttonNext.addActionListener(this);
		buttonNext.setEnabled(false);
		buttonReanalyze = new JButton("Re-analyze");
		buttonReanalyze.setMnemonic(KeyEvent.VK_R);
		buttonReanalyze.addActionListener(this);
		buttonReanalyze.setEnabled(false);
	
		buttonRevert = new JButton("Revert to saved");
		buttonRevert.addActionListener(this);
		buttonRevert.setEnabled(false);
		buttonApplyFilterContour = new JButton("Extract objects");
		buttonApplyFilterContour.addActionListener(this);
		buttonApplyFilterContour.setEnabled(false);
		buttonLoadCalib = new JButton("Calibration");
		buttonLoadCalib.setMnemonic(KeyEvent.VK_C);
		buttonLoadCalib.addActionListener(this);
		buttonSettings = new JButton("Settings");
		buttonSettings.setMnemonic(KeyEvent.VK_E);
		buttonSettings.addActionListener(this);
		
		
		buttonCrop = new JButton("Crop image");
		buttonCrop.addActionListener(this);
		buttonCrop.setEnabled(false);
		buttonCrop.setVisible(false);
		
		buttonApplyNoContour = new JButton("Extract objects (no serrations)");
		buttonApplyNoContour.addActionListener(this);
		buttonApplyNoContour.setEnabled(false);

		buttonCalcStats = new JButton("Output results");
		buttonCalcStats.addActionListener(this);
		buttonCalcStats.setEnabled(false);

		
		buttonSaveImage = new JButton("Save image");
		buttonSaveImage.addActionListener(this);
		
		buttonSaveImageCropped = new JButton("Save cropped image");
		buttonSaveImageCropped.addActionListener(this);
		
		
		// -- Help
		URL urlImageAbout = this.getClass().getResource(this.LOGO_ABOUT);
		Image logoAbout = Toolkit.getDefaultToolkit().getImage(urlImageAbout);
		//buttonHelp = new JButton("Help",  new ImageIcon(logoAbout) );
		buttonHelp = new JButton("Help");

		URL url = this.getClass().getResource(this.HELP_LOCATION);

		try
		{
			HelpSet hs = new HelpSet(null, url);
			hs.setTitle(this.APPL_NAME + " help");
			HelpBroker hb = hs.createHelpBroker("Help");


			buttonHelp.addActionListener(new CSH.DisplayHelpFromSource(hb));
			//buttonHelp.addActionListener(this);

		} catch (Exception ex)
		{
			ex.printStackTrace();
		} catch (Throwable t)
		{
			System.err.println("Help error (throwable)");
			t.printStackTrace();
		}
		
		// -- About
		buttonAbout = new JButton("About");
		buttonAbout.addActionListener(this);
		buttonAbout.setPreferredSize( new Dimension(buttonAbout.getWidth(), 20) );
		buttonAbout.setToolTipText("About this application");
		buttonAbout.setFocusable(false);
		
		
		/*
		labelBrushSize = new JLabel("Brush size: ");
		spinnerBrushSize = new JSpinner( new SpinnerNumberModel(10, 1, 200, 1) );
		spinnerBrushSize.addChangeListener(this);
		JPanel panelLeftBrushSize = new JPanel( new BorderLayout() );
		panelLeftBrushSize.setBorder( BorderFactory.createEtchedBorder() );
		panelLeftBrushSize.add(labelBrushSize, BorderLayout.CENTER);
		panelLeftBrushSize.add(spinnerBrushSize, BorderLayout.EAST);
		*/
		
		/** Bottom panel **/
		buttonZoomIn = new JButton("Zoom in");
		buttonZoomIn.addActionListener(this);
		buttonZoomOut = new JButton("Zoom out");
		buttonZoomOut.addActionListener(this);

		
		/** Panels **/
		
		pbStatus = new JProgressBar(0, 100);
		pbStatus.addChangeListener(this);
		panelBottomTop.setVisible(false);
		
		panelBottomCenter.add(buttonZoomIn);
		panelBottomCenter.add(buttonZoomOut);
		panelBottomCenter.add(buttonSaveImage);
		
		panelTopTop = new JPanel( new GridLayout(1, 5) );
		panelTopBottom = new JPanel( new GridLayout(1, 5) );
		
	
		//panelBottomBottom.add(labelStatus);
		
		//panelBottomBottom.add(buttonSaveImage);
		panelTopTop.add(buttonLoadFolder);
		panelTopTop.add(buttonLoadImage);
		//panelTopTop.add(new JSeparator(SwingConstants.VERTICAL) );
		//panelTopTop.add(new JSeparator(SwingConstants.VERTICAL) );
		panelTopTop.add(buttonSettings);
		panelTopTop.add(buttonLoadCalib);
		panelTopTop.add(buttonHelp);
		panelTopTop.add(buttonAbout);
		
		//panelTopTop.add(buttonCrop);
		//panelTopTop.add( new JSeparator() );
		
		panelTopBottom.add(buttonSave);
		panelTopBottom.add(buttonSaveAndNext);
		panelTopBottom.add(buttonNext);
		panelTopBottom.add(buttonReanalyze);
		panelTopBottom.add(panelScaleParamSpinner);
		panelTopBottom.add(panelNumLandmarks);
	
		//panelTopBottom.add(buttonSettings);
		//panelTopBottom.add(buttonLoadCalib);
		
		
		
		//panelTopBottom.add(new JSeparator(SwingConstants.VERTICAL) );
		//panelTopBottom.add(new JSeparator(SwingConstants.VERTICAL) );
		//panelTopBottom.add(new JSeparator(SwingConstants.VERTICAL) );
		//panelTopBottom.add(new JSeparator(SwingConstants.VERTICAL) );
		/*
		panelTopBottom.add(buttonLoadImage);
		panelTopBottom.add(buttonRevert);
		panelTopBottom.add(buttonApplyFilterContour);
		panelTopBottom.add(buttonCalcStats);
		*/
		
		
		/*
		panelTopTop.add(panelNumLandmarks);
		panelTopTop.add(buttonCrop);
		panelTopTop.add( new JSeparator() );
		panelTopTop.add(panelLeftBrushSize);
		panelTopTop.add(buttonUndoDraw);
		panelTopTop.add(buttonForegroundColor);
		panelTopTop.add(buttonBackgroundColor);
		*/
		

		
		
		
		
		panelTop.add(panelTopTop, BorderLayout.NORTH);
		panelTop.add(new JSeparator(), BorderLayout.CENTER );
		panelTop.add(panelTopBottom, BorderLayout.SOUTH);
		
		panelBottom.add(panelBottomTop, BorderLayout.NORTH);
		panelBottom.add(panelBottomCenter, BorderLayout.CENTER);
		panelBottom.add(panelBottomBottom, BorderLayout.SOUTH);
		
		
		
		
		/** Center panel **/
		componentImage = new JComponentDisplay(100, 100);
		componentImage.setScaleType( JComponentDisplay.SCALE_MODE_PRESCALE );
		componentImage.addMouseListener(this);
		componentImage.addMouseMotionListener(this);
		
		componentImageCropped = new JComponentDisplay(100, 100);
		componentImageCropped.setScaleType( JComponentDisplay.SCALE_MODE_PRESCALE );
		componentImageCropped.addMouseListener(this);
		componentImageCropped.addMouseMotionListener(this);
		
		panelSplit1.add(panelCenter, BorderLayout.CENTER);
		panelSplit1.add(panelBottom, BorderLayout.SOUTH);


		/** Main panel **/
		content.add(panelLeft, BorderLayout.WEST);
		content.add(panelTop, BorderLayout.NORTH);
		
		
		content.add(splitPane, BorderLayout.CENTER);
		content.add(panelStatus, BorderLayout.SOUTH);
		
		cbZoomLevel = new JComboBox();
		cbZoomLevelCropped = new JComboBox();
		
		
		/* Cropped panel */
		buttonZoomInCropped = new JButton("Zoom in");
		buttonZoomInCropped.setMnemonic(KeyEvent.VK_PLUS);
		buttonZoomInCropped.addActionListener(this);
		buttonZoomInCropped.setToolTipText("Press + (plus) on the keyboard to zoom in");
		buttonZoomOutCropped = new JButton("Zoom out");
		buttonZoomOutCropped.setMnemonic(KeyEvent.VK_PLUS);
		buttonZoomOutCropped.addActionListener(this);
		buttonZoomOutCropped.setToolTipText("Press - (minus) on the keyboard to zoom out");
		
		panelBottomCenterCropped = new JPanel( new GridLayout(1, 3) );
		panelBottomCenterCropped.add(buttonZoomInCropped);
		panelBottomCenterCropped.add(buttonZoomOutCropped);
		panelBottomCenterCropped.add(buttonSaveImageCropped);
		panelSplit2.add(panelBottomCenterCropped, BorderLayout.SOUTH);
		
		//panelSplit
		//splitPane.addPropertyChangeListener(this);
		//this.propertyChange(new PropertyChangeEvent(splitPane, JSplitPane.ONE_TOUCH_EXPANDABLE_PROPERTY, new Integer(0), new Integer(1) ));
		
		
		
		//make sure all components listen to keyboard events
		addKeyListenerRec(frame);
		
		//color selection tool
		
		//display
		frame.setVisible(true);
	}
	
	/**
	* Main function that is invoked when the user externally calls the program.
	* Calls the constructor to start the GUI.
	*
	* @param	args	Arguments sent to the application
	*/
	public static void main(String[] args)
	{
		//start the GUI by calling the constructor
    		new Lamina();
	}

	/**
	* An event that is invoked when the window is closing.
	*
	* @param	e	The WindowEvent
	*/
	public void windowClosing(WindowEvent e)
	{ 
		if (e.getSource() == frame)
		{
			if (settings != null)
			{
				settings.setWindowLocation(frame.getLocation());
				settings.setWindowSize(frame.getSize());
				if (lastInDir != null)
					settings.setInputDir(lastInDir);
				if (lastOutDir != null)
					settings.setOutputDir(lastOutDir);
				settings.writeIni();
			}
			
			System.exit(0); 
		}  else if (e.getSource() == dialogCalibProgress)
		{
			//System.err.println("Closing dialogCalibProgress...");
			dialogCalibProgress.abortProgress();
			
		} else if ( e.getSource() instanceof JFrameExt)
		{
			
			( (JFrameExt)e.getSource()).dispose();
			( (JFrameExt)e.getSource()).setEnabled(false);
		} else if ( e.getSource() instanceof LaminaCalibProgress)
		{
			( (LaminaCalibProgress)e.getSource()).dispose();
			( (LaminaCalibProgress)e.getSource()).setEnabled(false);
		}
	}
	
	/**
	* An event that is invoked when the window is closed.
	*
	* @param	e	The WindowEvent
	*/
	public void windowClosed(WindowEvent e)
	{
		if (e.getSource() == dialogCalibProgress)
		{
			System.err.print("Closed dialogCalibProgress, ");
			if (dialogCalibProgress.getStatus() == LaminaCalibDialog.STATUS_OK)
			{
				System.err.println("Status: OK");
				System.err.println("Now setting scale parameter to " + putativeScaleParam);
				((SpinnerNumberModel)spinnerScaleParam.getModel()).setValue( new Double(putativeScaleParam) );
			} else
			{
				System.err.println("Status: Cancel");
			}
			
			imgCalib = null;
			imgCalibGrayscale = null;
			dialogCalibProgress = null;
			
			//invoke garbage collection to clean up calibration stuff
			System.err.println("Cleaning up memory after calibration progress...");
			runTime.gc();
			
		} else if (e.getSource() == mabout)
		{
			System.err.println("Cleaning up memory after the About dialog is closed...");
			
			System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
			mabout.removeWindowListener(this);
			mabout = null;
			runTime.gc();
			System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
		}
	}
	
	/**
	* An event that is invoked when a general action is performed, e.g. a button-click etc.
	*
	* @param	e	The ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonLoadImage)
		{
			//remember last filename
			File dir = new File (".");
			if (lastInDir != null)
				dir = lastInDir;
				
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			jfc.setFileFilter(new FileFilterImage() );
			jfc.setCurrentDirectory(dir);
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int retVal = jfc.showDialog(frame, "Select file");
			
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				currentFile = jfc.getSelectedFile();
				outputPath = settings.getOutputDir();
				
				statFileHeaderWritten = false;
				
				lastInDir = currentFile.getParentFile();
				currentFilename = new String( currentFile.getAbsolutePath() );
				
				
				buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " >>>");
				buttonSaveAndNext.setEnabled(false);
				buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " >>>");
				buttonNext.setEnabled(false);
				
				currentImageFileInd = 0;
				listOfImageFiles = new String[1];
				listOfImageFilesPath = new String[1];
				listOfOutputImagesCropped = new String[1];
				listOfRowsWritten = new int[1]; //not really needed in this case
				listOfImageFiles[0] = currentFile.getName();
				listOfImageFilesPath[0] = currentFile.getAbsolutePath();
					
				//construct a batch output filename
				Calendar cal = Calendar.getInstance(TimeZone.getDefault());
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.setTimeZone(TimeZone.getDefault());
				
				String delim = (outputPath.getAbsolutePath().lastIndexOf('\\') >= 0) ? "\\" : "/";
				
				//fetch the time, to calculate the date used to name the output file
				String timeFormatted = sdf.format(cal.getTime()).replaceAll(" ", "_").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-");
				
				outputFileBatch = outputPath.getAbsolutePath() + delim + APPL_NAME + OUTPUT_FNAME_STATS + "_" + timeFormatted + ".txt";
				File fileSave = new File(outputFileBatch);
				int outNum = 2;
				while (fileSave.isFile() )
				{
					outputFileBatch = outputPath.getAbsolutePath() + delim + APPL_NAME + OUTPUT_FNAME_STATS + "_" + timeFormatted + "-" + (outNum++) + ".txt";
					fileSave =  new File(outputFileBatch);
				}
				//System.err.println("Global stat. file: " + outputFileBatch);
				labelOutputFile.setText("[Output] " + outputFileBatch );
				labelOutputFile.setToolTipText("[Output] " + outputFileBatch );
			
					
					
				// start the processing here
				if (loadImage(listOfImageFilesPath[currentImageFileInd]))
				{
					labelStatus.setText("[File " + (currentImageFileInd+1) + "/" + listOfImageFilesPath.length + "] " +
						listOfImageFilesPath[currentImageFileInd] );
				
					//fake a 'extract objects' click
					this.actionPerformed( new ActionEvent(buttonApplyFilterContour, 0, "") );
				}
			}
			
		} else if (e.getSource() == buttonLoadFolder)
		{
			//remember last filename
			File dir = new File (".");
			if (lastInDir != null)
				dir = lastInDir;
				
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			//jfc.setFileFilter(new FileFilterImage() );
			jfc.setCurrentDirectory(dir);
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jfc.setDialogType(JFileChooser.OPEN_DIALOG);
			int retVal = jfc.showDialog(frame, "Select directory");
			
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				inputPath = jfc.getSelectedFile();
				outputPath = settings.getOutputDir();
			
				//fetch all images in the current folder
				listOfImageFiles = inputPath.list(ffImage);
				
				if (listOfImageFiles.length > 0)
				{
					currentImageFileInd = 0;
					statFileHeaderWritten = false;
				
					//buttonSaveAndNext.setEnabled(listOfImageFiles.length > 1);
					buttonNext.setEnabled(listOfImageFiles.length > 1);
						
					
					if (listOfImageFiles.length > 1)
					{
						buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
						buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
					} else
					{
						buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " >>>");
						buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " >>>");
					}
				
					
					labelStatus.setText("Found " + listOfImageFiles.length + " image file(s) in folder " + inputPath.getAbsolutePath() + "...");
					
					String delim = (outputPath.getAbsolutePath().lastIndexOf('\\') >= 0) ? "\\" : "/";
					
					//add absolute path
					listOfImageFilesPath = new String[listOfImageFiles.length];
					for (int i = 0; i < listOfImageFiles.length; i++)
						listOfImageFilesPath[i] = inputPath.getAbsolutePath() + delim + listOfImageFiles[i];
					
					//reset output file info
					listOfOutputImagesCropped = new String[listOfImageFiles.length];
					listOfRowsWritten = new int[listOfImageFiles.length];
					for (int i = 0; i < listOfImageFiles.length; i++)
						listOfRowsWritten[i] = 0;
					
					//construct a batch output filename
					Calendar cal = Calendar.getInstance(TimeZone.getDefault());
					SimpleDateFormat sdf = new SimpleDateFormat();
					sdf.setTimeZone(TimeZone.getDefault());
					
					//fetch the time, to calculate the date used to name the output file
					String timeFormatted = sdf.format(cal.getTime()).replaceAll(" ", "_").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-");
					
					outputFileBatch = outputPath.getAbsolutePath() + delim + APPL_NAME + OUTPUT_FNAME_STATS + "_" + timeFormatted + ".txt";
					File fileSave = new File(outputFileBatch);
					int outNum = 2;
					while (fileSave.isFile() )
					{
						outputFileBatch = outputPath.getAbsolutePath() + delim + APPL_NAME + OUTPUT_FNAME_STATS + "_" + timeFormatted + "-" + (outNum++) + ".txt";
						fileSave =  new File(outputFileBatch);
					}
					//System.err.println("Global stat. file: " + outputFileBatch);
					labelOutputFile.setText("[Output] " + outputFileBatch );
					labelOutputFile.setToolTipText("[Output] " + outputFileBatch );
			
					
					
					// start the processing here
					if (loadImage(listOfImageFilesPath[currentImageFileInd]))
					{
						labelStatus.setText("[File " + (currentImageFileInd+1) + "/" + listOfImageFilesPath.length + "] " +
							listOfImageFilesPath[currentImageFileInd] );
					
						//fake a 'extract objects' click
						this.actionPerformed( new ActionEvent(buttonApplyFilterContour, 0, "") );
					} else
					{
						updateSaveButtons(false);
					}
					
					
				} else
				{
					currentImageFileInd = -1;
					listOfImageFiles = null;
					listOfImageFilesPath = null;
					listOfOutputImagesCropped = null;
					listOfRowsWritten = null;
					
					JOptionPane.showMessageDialog(frame, "Failed to find any image files in the current directory.\n"+
						"Please try another directory..",
						"Failed to find image files", JOptionPane.ERROR_MESSAGE);
				}
			
			}
		} else if (e.getSource() == buttonAbout)
		{
			//if (mabout == null || mabout.isDisposed() )
			{

				//new Thread()
				//{
					//public void run()
					{
						mabout = new JDialogAbout(frame);
						mabout.addWindowListener(this);
						
						/*
						// send runnable to the Swing thread
						// the runnable is queued after the
						// results are returned
						SwingUtilities.invokeLater
						(
							new Runnable()
							{
								public void run()
								{
									System.err.println("Removing 'about' dialog from memory...");
									mabout = null;
									runTime.gc();
								}
							}
						);
						*/

					}
					
					

				//}.start();
				
			}

			
		} else if (e.getSource() == buttonSave)
		{
			if (listOfImageFiles != null && listOfImageFiles.length > 0 && currentImageFileInd >= 0 && currentImageFileInd < listOfImageFiles.length)
			{
				System.err.println("'Save' button was clicked...");
				
				saveClickWasOK = true;
			
				//start by calculating the output data...
				Vector objStats = new Vector();
				try
				{
					objStats = GrayscaleImageEdit.calcSegStats(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBordersShort,
						vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints,
						vecContourHotspotConnections, vecContourHotspotIndices, vecContourIndents,
						1/( (SpinnerNumberModel)spinnerScaleParam.getModel()).getNumber().doubleValue(),
						vecSegObjBordersShortLandmarks, vecSegObjCentersOrg, vecSegObjCenters,
						imgDisplay.getGraphics() );
						
				} catch (Throwable t)
				{
					saveClickWasOK = false;
					t.printStackTrace();
					JOptionPane.showMessageDialog(frame, "Unable to calculate object statistics.\n"+
						"This is potentially due to a bug in the software.\n" +
						"Please provide details on how to reconstruct the problem to the software developer.",
						"Unable calculate object statistics", JOptionPane.ERROR_MESSAGE);
				}
				
			
				//output data here
				//boolean foundError = false;	
				if (saveClickWasOK)
				{
					String token = "\t";
					try
					{
						//we may have to overwrite the saved output for the current file
						if (listOfRowsWritten[currentImageFileInd] > 0)
						{
							System.err.println("Reconstructing output for file " + currentImageFileInd + " which had " +
								listOfRowsWritten[currentImageFileInd] + " rows written...");
							
							
							
							//we read all the input and flush
							FileReader fr = new FileReader(outputFileBatch);
							BufferedReader br = new BufferedReader(fr);
							
							//this is the number of lines we have to read (i.e. excluding the line written for this file)
							int numLinesToRead = 0;
							for (int n = 0; n < currentImageFileInd; n++)
								numLinesToRead += listOfRowsWritten[n];
							
							//read the lines
							Vector vecLine = new Vector(numLinesToRead);
							for (int n = 0; n < numLinesToRead; n++)
								vecLine.add( br.readLine() );
							
							br.close();
							
							System.err.println("Read  " + vecLine.size() + " line(s)...");
							
							
							//flush the read line back to the output file (excluding the last lines from the current file)
							// we may have 0 lines to write, which implies truncating the file
							FileWriter fw = new FileWriter(outputFileBatch, false);
							BufferedWriter bw = new BufferedWriter(fw);
							for (int n = 0; n < vecLine.size(); n++)
							{
								bw.write( (String)vecLine.get(n) );
								bw.newLine();
							}
							bw.close(); 
						
							// we have to rewrite the header if we truncate
							statFileHeaderWritten = (vecLine.size() > 0);
							
							listOfRowsWritten[currentImageFileInd] = 0;
						}
						
						
						//if we're dealing with multiple files, we have to append
						boolean doAppend = (listOfImageFiles.length > 1 && statFileHeaderWritten); 
						FileWriter fw = new FileWriter(outputFileBatch, doAppend);
						BufferedWriter bw = new BufferedWriter(fw);
						
						
						
						
						//dump the file header first (for the first file)...
						if (!statFileHeaderWritten)	
						{	
							statFileHeaderWritten = true;
							Vector fileHeader = (Vector)objStats.get(0);
							fileHeader.insertElementAt("File", 0); //insert a new entry into the header
						
							for (int j = 0; j < fileHeader.size(); j++)
							{
								//String str = Misc.quoteAlphaNumeric((String)fileHeader.get(i));
								//String str = (String)fileHeader.get(i);
								bw.write( "\"" + (String)fileHeader.get(j) + "\"" );
								if (j < (fileHeader.size()-1))
									bw.write(token, 0, token.length());
							}
							bw.newLine();
							
							listOfRowsWritten[currentImageFileInd]++;
						
						}

						// .. and then the data
						for (int i = 1; i < objStats.size(); i++)
						{
							Vector row = (Vector)objStats.get(i);
							
							//add filename (without path)
							bw.write( listOfImageFiles[currentImageFileInd] );
							bw.write(token, 0, token.length());
							
							for (int j = 0; j < row.size(); j++)
							{
								//bw.write(Misc.quoteAlphaNumeric( (String)row.get(j)));
								bw.write( ((Number)row.get(j)).toString() );
									
								if (j < (row.size()-1))
									bw.write(token, 0, token.length());
							}
							bw.newLine();
							listOfRowsWritten[currentImageFileInd]++;
						}
						bw.flush();
						bw.close();


					} catch (IOException ex)
					{
						// catch possible io errors
						saveClickWasOK = false;
						System.out.println("Error writing file '" + outputFileBatch + "'!");
						JOptionPane.showMessageDialog(frame, "Unable to save the output data file.\n"+
							"This is potentially caused by some other application using the data file.\n" +
							"Please make sure that the specified filename is writable and try again.",
							"Unable to write output data file", JOptionPane.ERROR_MESSAGE);
					}
				}
				
				if (saveClickWasOK)
				{
				
					try
					{
						//now output the cropped image...
						//we start by creating a suitable name
						String str = listOfImageFiles[currentImageFileInd];
						String[] parts = str.split("\\.");
						String newStr = new String(parts[0]);
						for (int i = 1; i < (parts.length-1); i++)
							newStr = newStr  + "." + parts[i];
						String ext = parts[parts.length-1];
						
						// use a previous name if we've already saved this file before
						if (listOfOutputImagesCropped[currentImageFileInd] == null)
						{
							File fileCropOutput =  new File(outputPath.getAbsolutePath() + "/" + newStr + "_" + APPL_NAME + OUTPUT_FNAME_CROPPED + "." + ext);
							int outNum = 2;
							while (fileCropOutput.isFile() )
								fileCropOutput = new File(outputPath.getAbsolutePath() + "/" + newStr + "_" + APPL_NAME + OUTPUT_FNAME_CROPPED +  (outNum++) +"." + ext);
						
							listOfOutputImagesCropped[currentImageFileInd] = fileCropOutput.getAbsolutePath();
						}
						System.err.println("Cropped file output name: " + listOfOutputImagesCropped[currentImageFileInd]);
						
						//the filename desides the codec
						String codecId = ext.toUpperCase();
						if (codecId.equals("TIF"))
							codecId = "TIFF";
						else if (codecId.equals("JPG"))
							codecId = "JPEG";
						
						//now crop image
						BufferedImage imgCroppedCopy = new BufferedImage(imgCropped.getWidth(), imgCropped.getHeight(), imgCropped.getType() );
						imgCroppedCopy.setData( imgCropped.getData() );
						imgCroppedCopy = PlanarImageEdit.applyMask(imgCroppedCopy, imgMatBinary);
						GrayscaleImageEdit.paintSegmentationResults(vecSegObjs, vecSegObjNoCavities, vecSegObjCenters, vecSegObjBorders, vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints, imgCroppedCopy.getGraphics());
						//GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgCroppedCopy.getGraphics());
						
						if (contourUnique != null && vecContourHotspotConnections != null && vecContourIndents != null)
						{
							PlanarImageEdit.paintIntegerMatrix(contourUnique, Color.BLUE, imgCroppedCopy.getGraphics() );
							//PlanarImageEdit.paintVector(vecSegObjs, Color.GREEN, imgDisplay.getGraphics() );
							//PlanarImageEdit.paintVector(vecSegObjBordersShort, Color.YELLOW, imgCroppedCopy.getGraphics() );
							GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgCroppedCopy.getGraphics(), Color.RED, false);
							GrayscaleImageEdit.paintContourIndentDepths(vecContourIndents, imgCroppedCopy.getGraphics(), Color.ORANGE );
						}
						
						GrayscaleImageEdit.paintBorders(vecSegObjBordersShort, Color.YELLOW, imgCroppedCopy.getGraphics());
						GrayscaleImageEdit.paintObjectIds(vecSegObjCenters, imgCroppedCopy.getGraphics());
						
						
						//now output the file
						JAI.create("filestore", imgCroppedCopy, listOfOutputImagesCropped[currentImageFileInd], codecId);
					
					} catch (Throwable t)
					{
						saveClickWasOK = false;
						t.printStackTrace();
						JOptionPane.showMessageDialog(frame, "Unable to save the output image file.\n"+
							"This is potentially caused by some other application using the image file.\n" +
							"Please make sure that the specified filename is writable and try again.",
							"Unable to write output image file", JOptionPane.ERROR_MESSAGE);
					}
				}
				
				if (saveClickWasOK)
				{
					updateSaveButtons(false);
					labelStatus.setText("Successfully saved output from file " + listOfImageFiles[currentImageFileInd] );
				}
				
				System.err.println("Done with file " + listOfImageFiles[currentImageFileInd]);
				
			}
			
		} else if (e.getSource() == buttonSaveAndNext)
		{
			if (listOfImageFiles != null && listOfImageFiles.length > 0 && currentImageFileInd >= 0 && currentImageFileInd < listOfImageFiles.length)
			{
				System.err.println("'Save and next' button was clicked...");
				
				//fake a 'save' button click
				this.actionPerformed( new ActionEvent(buttonSave, 0, "") );
				
				if (saveClickWasOK)
				{
				
					currentImageFileInd++;
					
					buttonNext.setEnabled( currentImageFileInd < (listOfImageFiles.length-1) );
					
					if (currentImageFileInd >= listOfImageFiles.length)
					{
						//we can't go any further now
						
					} else
					{
						//move to the next file
						if (buttonNext.isEnabled() )
						{
							buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
							buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
						} else
						{
							buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " >>>");
							buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " >>>");
						}
					
						
						// start the processing here
						if (loadImage(listOfImageFilesPath[currentImageFileInd]))
						{
							labelStatus.setText("[File " + (currentImageFileInd+1) + "/" + listOfImageFilesPath.length + "] " +
								listOfImageFilesPath[currentImageFileInd]);
						
							//fake a 'extract objects' click
							this.actionPerformed( new ActionEvent(buttonApplyFilterContour, 0, "") );
							
						} else
						{
							updateSaveButtons(false);
						}
					
					}
				
				} else if (currentImageFileInd < (listOfImageFiles.length-1) )
				{
					JOptionPane.showMessageDialog(frame, "There was a problem saving at least one of the output files.\n"+
						"The next image will not be automatically loaded. Use the 'Next' button if you need to proceed.",
						"Unable to save output file", JOptionPane.ERROR_MESSAGE);
				}
			
			} else if (listOfImageFiles != null && currentImageFileInd >= listOfImageFiles.length)
			{
				buttonSaveAndNext.setEnabled(false);
				System.err.println("We're done!");
			}
			
		} else if (e.getSource() == buttonNext)
		{
			if (listOfImageFiles != null && listOfImageFiles.length > 0 && currentImageFileInd >= 0 && currentImageFileInd < listOfImageFiles.length)
			{
				System.err.println("'Next' button was clicked...");
				
				currentImageFileInd++;
				
				buttonNext.setEnabled( currentImageFileInd < (listOfImageFiles.length-1) );
				//buttonSaveAndNext.setEnabled( currentImageFileInd < (listOfImageFiles.length-1) );
				
				if (currentImageFileInd >= listOfImageFiles.length)
				{
					//we're out of bounds
					
					
				} else
				{
					//move to the next file
					if (buttonNext.isEnabled() )
					{
						buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
						buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " [" + (currentImageFileInd+2) + "/" + listOfImageFiles.length + "] >>>");
					} else
					{
						buttonSaveAndNext.setText(SAN_BUTTON_CAPTION_DEF + " >>>");
						buttonNext.setText(NEXT_BUTTON_CAPTION_DEF + " >>>");
					}
				
					
					// start the processing here
					if (loadImage(listOfImageFilesPath[currentImageFileInd]))
					{
						labelStatus.setText("[File " + (currentImageFileInd+1) + "/" + listOfImageFilesPath.length + "] " +
							listOfImageFilesPath[currentImageFileInd]);
					
						
						//fake a 'extract objects' click
						this.actionPerformed( new ActionEvent(buttonApplyFilterContour, 0, "") );
						
					} else
					{
						updateSaveButtons(false);
					}
				
				}
			
			} else if (listOfImageFiles != null && currentImageFileInd >= listOfImageFiles.length)
			{
				buttonSaveAndNext.setEnabled(false);
				System.err.println("We're done!");
			}
		
		} else if (e.getSource() == buttonReanalyze)
		{
			// start the processing here
			if (listOfImageFilesPath != null && listOfImageFilesPath.length > 0 &&  currentImageFileInd >= 0 && currentImageFileInd < listOfImageFilesPath.length)
			{
				//we have to re-load the current image and re-process it
				if (loadImage(listOfImageFilesPath[currentImageFileInd]))
				{
					//buttonSaveAndNext.setEnabled(currentImageFileInd < (listOfImageFilesPath.length-1) );
					buttonNext.setEnabled( currentImageFileInd < (listOfImageFilesPath.length-1) );
					
					labelStatus.setText("[File " + (currentImageFileInd+1) + "/" + listOfImageFilesPath.length + "] " +
						listOfImageFilesPath[currentImageFileInd]);
				
					//fake a 'extract objects' click
					this.actionPerformed( new ActionEvent(buttonApplyFilterContour, 0, "") );
				} else
				{
					updateSaveButtons(false);
				}
			}
		
		} else if (e.getSource() == buttonLoadCalib)
		{
			
			LaminaCalibDialog dlgCalib = new LaminaCalibDialog(frame, "Open calibration image");
			//dlgCreateGrid.setIconImage(frame.getIconImage());
			dlgCalib.initialize( settings.getCalibHeight(), settings.getCalibWidth(), lastInDir, settings.getCalibFile() );
			dlgCalib.setModal(true);
			frame.setFocusableWindowState(false);
			dlgCalib.setVisible(true);
			frame.setFocusableWindowState(true);

			if (dlgCalib.getStatus() == LaminaCalibDialog.STATUS_OK)
			{
				settings.setCalibFile( dlgCalib.getCalibFile() );
				settings.setCalibHeight( dlgCalib.getObjectHeight() );
				settings.setCalibWidth( dlgCalib.getObjectWidth() );
				
				lastInDir = dlgCalib.getLastDir();
				File calibFile = dlgCalib.getCalibFile();
				calibWidth = dlgCalib.getObjectWidth();
				calibHeight = dlgCalib.getObjectHeight();
				
		
				calibFilename = new String( calibFile.getAbsolutePath() );
				
				
				//System.err.println("Last dir: " + lastDir.getAbsolutePath() );
				
				//calibFilename = new String(fd.getDirectory() + fd.getFile());                   
				System.err.println("Loading (calib) image: " + calibFile + "...");
	
				pbStatus.setValue(0);
				//panelBottomTop.setVisible(true);
				
				//load image and copy
				boolean imageLoaded = false;
				
				try
				{
					PlanarImage imgTemp = JAI.create("fileload", calibFilename);
					imgCalib = imgTemp.getAsBufferedImage();
					Raster rasterCalib = imgCalib.getData();
					
					//switch to default image type if we can't recognize it
					int imgType = imgCalib.getType();
					if (imgType == 0)
						imgType = BufferedImage.TYPE_INT_RGB;
					
					System.err.println("Constructing image of size (" + imgCalib.getWidth() + "," + imgCalib.getHeight() +
						") of type " + imgType );
					//imgDisplay = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), imgType );
					//imgDisplay.setData(rasterOrg);
					//panelBottomTop.setVisible(false);
					
					//if (!isLoaded)
					//	isLoaded = true;
					
					
					/*
					componentImage.set(imgCalib);
					if (!isLoaded)
					{					
						componentImage.setAutoScaleType(zoomLevel);
						
						scrollPane = new JScrollPane(componentImage);
						//scrollPane.getVerticalScrollBar().setMaximum( imgGreen.getHeight() );
						scrollPane.getVerticalScrollBar().setUnitIncrement( (int)(imgCalib.getHeight()/(double)SCROLLBAR_INC) );
						//scrollPane.getHorizontalScrollBar().setMaximum( imgGreen.getWidth() );
						scrollPane.getHorizontalScrollBar().setUnitIncrement( (int)(imgCalib.getWidth()/(double)SCROLLBAR_INC) );
											
						//zoomLevel = 1.0f;
					
						panelCenter.add(scrollPane, BorderLayout.CENTER);
						isLoaded = true;	
					}
					
					//double spHeight = scrollPane.getViewport().getViewRect().getHeight();
					//double spWidth = scrollPane.getViewport().getViewRect().getWidth();
					double spHeight = panelCenter.getHeight()-10;
					double spWidth = panelCenter.getWidth()-10;
					double scaleHeight = spHeight / (double)imgCalib.getHeight();
					double scaleWidth = spWidth / (double)imgCalib.getWidth();
					
					//auto-scale window so that entire image can be seen
					zoomLevel = (float)Math.min(scaleHeight, scaleWidth);
					componentImage.setAutoScaleType(zoomLevel);
					
					panelCenter.validate();
					*/
					
					imageLoaded = true;
					
					
				} catch (Throwable t)
				{
					System.err.println("Failed to read image of size (" + imgCalib.getWidth() + "," + imgCalib.getHeight() +
						") of type " + imgCalib.getType() );
					t.printStackTrace();
				}
				
				
				// run calibration stuff here
				if (imageLoaded)
				{
					dialogCalibProgress = new LaminaCalibProgress(frame, Lamina.APPL_NAME + " -- processing calibration file", true,
								new Dimension(frame.getWidth()-50, frame.getHeight()-50) );
					dialogCalibProgress.addWindowListener(this);
							
					new Thread()
					{
						public void run()
						{
							frame.setCancelled(false);
							frame.setRunning(true);
							frame.setEnabled(false);

					
					
							//System.err.println("Filtering...");
							
							
							dialogCalibProgress.getLabelInterim().setText("Overall progress of calibration");
							
							dialogCalibProgress.setImage(imgCalib, calibFilename);
							
							//dialogCalibProgress.getPBTotal().setVisible(false);
							//dialogCalibProgress.getPBTotal().setValue(0);
							
							Raster rasterCalib = imgCalib.getData();
							
							dialogCalibProgress.getLabelCurrent().setText("Extracting color difference image...");
									
							imgCalibGrayscale = new BufferedImage( imgCalib.getWidth(), imgCalib.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
							WritableRaster wrGrayscale = imgCalibGrayscale.getRaster();
							
							// set up the binary and grayscale images that will be used for segmentation
							
								
							//int i[] = new int[1];
							//int[] bluePixels = new int[imgCalib.getHeight()*imgCalib.getWidth()];
							
							int[] bluePixelGrayscale = new int[imgCalib.getHeight()*imgCalib.getWidth()];
							int[][] imgMatCalibGrayscale = new int[imgCalib.getHeight()][imgCalib.getWidth()];
							int[][] imgMatCalibGrayscaleTemplate = new int[imgCalib.getHeight()][imgCalib.getWidth()];
							int[][] imgCalibSeg = new int[imgCalib.getHeight()][imgCalib.getWidth()];;
							
							byte[][] imgMatCalibBinary = new byte[imgCalib.getHeight()][imgCalib.getWidth()];
							int[][] imgMatMaxDiffChannel = new int[imgCalib.getHeight()][imgCalib.getWidth()];
							
							int edgePixelLowerX = (int)Math.ceil(EDGE_REL_DIST*imgCalib.getWidth());
							int edgePixelUpperX = (int)Math.floor( (1-EDGE_REL_DIST)*imgCalib.getWidth());
							int edgePixelLowerY = (int)Math.ceil(EDGE_REL_DIST*imgCalib.getHeight());
							int edgePixelUpperY = (int)Math.floor( (1-EDGE_REL_DIST)*imgCalib.getHeight());
							
							Vector vecSegObjsCalib = new Vector();
							Vector vecSegObjCentersCalib = new Vector();
							Vector vecSegObjBordersCalib = new Vector();
							//Vector vecSegObjBorderBPCalib = new Vector();

							double numPixels = imgCalib.getHeight()*imgCalib.getWidth();
							double meanIntensity = 0.0;
							int index = 0;
							int numLowInt = 0;
							
							int r,g,b;
							int diff;

							for (int h = 0; h < imgCalib.getHeight(); h++)
								for (int w = 0; w < imgCalib.getWidth(); w++)
								{
									r = rasterCalib.getSample(w,h,PlanarImageEdit.BAND_R);
									g = rasterCalib.getSample(w,h,PlanarImageEdit.BAND_G);
									b = rasterCalib.getSample(w,h,PlanarImageEdit.BAND_B);
									
									diff =
										255 -
										( Math.max( Math.max(r, g), b)-
										 Math.min( Math.min(r, g), b) );
									
										 
									imgMatMaxDiffChannel[h][w] = diff;
									
									
									//try to remove black edge pixels
									/*
									if ( (w < edgePixelLowerX || w > edgePixelUpperX || h < edgePixelLowerY || h > edgePixelUpperY) && (255 - imgMatMaxDiffChannel[h][w]) < EDGE_INT_THRESH)
									{
										r = 255;
										g = 255;
										b = 255;
									}
									*/
									
									
									//bluePixels[ index++ ] = b;
									
									
									//System.err.print( bluePixels[ index - 1] + "," );
									
									//ranges from 0 to 255
									/*
									imgMatMaxDiffChannel[h][w] =
										(int)(
										Math.max( Math.max(rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R),
											rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G)), 
											rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B) ) /
										
										Math.max( Math.min( Math.min(rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R),
											rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G)), 
											rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B) ), 1.0) );
									*/
									
									
									
									// low values denote a high dominance of one color, high values denote low dominance (close to grayscale)
									
										
									//meanIntensity += imgMatMaxDiffChannel[h][w]/numPixels;
											
									
									imgMatCalibGrayscaleTemplate[h][w]=(int)imgMatMaxDiffChannel[h][w];
									imgMatCalibGrayscale[h][w]=imgMatMaxDiffChannel[h][w];
									
									meanIntensity += imgMatCalibGrayscaleTemplate[h][w]/numPixels;
									
									//square
									//imgMatMaxDiffChannel[h][w] = imgMatMaxDiffChannel[h][w]*imgMatMaxDiffChannel[h][w]*imgMatMaxDiffChannel[h][w];
									
									
									//truncate pixels with a too low intensity
									/*
									if (imgMatMaxDiffChannel[h][w] < 40)
									{
										bluePixels[index-1] = 255;
										imgMatGrayscaleTemplate[h][w] = 255;
										imgMatGrayscale[h][w] = 255;
										
									}
									*/
								}
							
							//imgMatGrayscaleTemplate = GrayscaleImageEdit.applyMask(imgMatGrayscaleTemplate, imgMatMaxDiffChannel, true);
							//imgMatGrayscale = GrayscaleImageEdit.applyMask(imgMatGrayscale, imgMatMaxDiffChannel, true);
							
							if (!frame.getCancelled())
							{
								BufferedImage biTemp = new BufferedImage(imgCalib.getWidth(), imgCalib.getHeight(), BufferedImage.TYPE_INT_RGB);
								WritableRaster wrTemp = biTemp.getRaster();
								for (int h = 0; h < imgCalib.getHeight(); h++)
									for (int w = 0; w < imgCalib.getWidth(); w++)
									{
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
										
										wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxDiffChannel[h][w]);
										wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxDiffChannel[h][w]);
										wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxDiffChannel[h][w]);
									
									}
								biTemp.setData(wrTemp);
								
								dialogCalibProgress.setImage(biTemp);
								//componentImage.set(biTemp);
								
								//frame.setCancelled(true);
							}
							
								
							
							//frame.setCancelled(true); // for now
							
							System.err.println("Number of pixels below intensity threshold: " + numLowInt + "/" + (imgCalib.getHeight()*imgCalib.getWidth()));
							
												// height							//width
							System.err.println(""+imgMatCalibGrayscale.length +","+imgMatCalibGrayscale[1].length);
							System.err.println("Average intensity value: " + meanIntensity);
							
							dialogCalibProgress.getPBInterim().setValue(10);
							
					
							
							
							/*
							double quant = 0.10;
							double quantileBlue = MiscMath.quantile(bluePixels, quant);
							System.err.println("Quantile intensity (" + quant + "): " + quantileBlue);
							
							for (int i = 0; i < bluePixels.length; i++)
									if (bluePixels[i] <= quantileBlue)
										bluePixels[i] = 255;
									else
										bluePixels[i] = 0;
							*/
							/*
							int INT_THRESH_MIN=5;
							int INT_THRESH_INC=10;
							int INT_THRESH_MAX=200;
							int WINDOW_HEIGHT=3;
							int WINDOW_WIDTH=3;
							for (int i = INT_THRESH_MIN; i <= INT_THRESH_MAX; i += INT_THRESH_INC)
							{
								System.err.println("Filtering image with intensity threshold " + i + "...");
								
								for (int h = 0; h < imgCalib.getHeight(); h++)
									for (int w = 0; w < imgCalib.getWidth(); w++)
									{
										if (imgMatGrayscaleTemplate[h][w] <= i)
											imgMatGrayscale[h][w]=(int)255;
										else
											imgMatGrayscale[h][w]=(int)0;
									}
								
								System.err.print("Calculating average standard deviation..");
								double[] stds = GrayscaleImageEdit.calcStds(imgMatGrayscale, WINDOW_HEIGHT, WINDOW_WIDTH);
								double avStds = MiscMath.mean(stds);
								System.err.println(""+avStds+"");
							}
							*/
							
							
							/*
							double quant = 0.10;
							double quantileBlue = MiscMath.quantile(bluePixels, quant);
							System.err.println("Quantile intensity (" + quant + "): " + quantileBlue);
							*/
							int quantileBlue = 0;
							if (!frame.getCancelled())
							{
								if (settings.getThresholdSearchGreedy() )
								{
						
									dialogCalibProgress.getLabelCurrent().setText("Detecting optimal threshold for segmentation (greedy)...");
									quantileBlue = GrayscaleImageEdit.detectThresholdGreedy(imgMatMaxDiffChannel, imgMatCalibGrayscale,
										3, 3, (int)meanIntensity, (int)settings.getThresholdSearchStepLength(),
										frame, dialogCalibProgress.getPBCurrent() );
							
								} else if (settings.getThresholdSearchExhaustive() )
								{
									dialogCalibProgress.getLabelCurrent().setText("Detecting optimal threshold for segmentation (exhaustive)...");
									quantileBlue = (int)GrayscaleImageEdit.detectThresholdExhaustive(imgMatMaxDiffChannel, imgMatCalibGrayscale,
										3, 3, (int)settings.getThresholdSearchStepLength(), 0, 255,
										frame, dialogCalibProgress.getPBCurrent() );
										
									//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
									//	10, 10, (int)meanIntensity, 10, frame, dialogCalibProgress.getPBCurrent() );
									
								} else
								{
									dialogCalibProgress.getLabelCurrent().setText("Using manual threshold for segmentation...");
									quantileBlue = (int)settings.getThresholdManual();
								} 
									
								//GrayscaleImageEdit.truncateImage(imgMatIntensity,imgMatGrayscaleTemplate,20,255);
								
								//JAI.create("filestore", componentImage.get(), filename, codecId);
								
								
								//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
								//	10, 10, (int)meanIntensity, 10, frame, dialogCalibProgress.getPBCurrent() );
									
								dialogCalibProgress.getPBInterim().setValue(50);
								System.err.println("The selected threshold is " + quantileBlue);
							}
							
							
							if (!frame.getCancelled())
							{
								//apply the threshold value
								dialogCalibProgress.getLabelCurrent().setText("Applying (segmentation) threshold to current image...");
								GrayscaleImageEdit.thresholdImage(imgMatMaxDiffChannel,imgMatCalibGrayscale,quantileBlue);
								GrayscaleImageEdit.thresholdImage(imgMatMaxDiffChannel,imgMatCalibGrayscaleTemplate,quantileBlue);//important!
								dialogCalibProgress.getPBInterim().setValue(55);
							}
							
							
							if (!frame.getCancelled())
							{
								dialogCalibProgress.getLabelCurrent().setText("Applying median filter on binary image (noise reduction)");
								GrayscaleImageEdit.medianFilter(imgMatCalibGrayscaleTemplate,imgMatCalibGrayscale, 3, 3);	
								dialogCalibProgress.getPBInterim().setValue(70);
							}
							
							
							if (!frame.getCancelled())
							{
								BufferedImage biTemp2 = new BufferedImage(imgCalib.getWidth(), imgCalib.getHeight(), BufferedImage.TYPE_INT_RGB);
								WritableRaster wrTemp2 = biTemp2.getRaster();
								for (int h = 0; h < imgCalib.getHeight(); h++)
									for (int w = 0; w < imgCalib.getWidth(); w++)
									{
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
										
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_R, imgMatCalibGrayscale[h][w]);
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_G, imgMatCalibGrayscale[h][w]);
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_B, imgMatCalibGrayscale[h][w]);
									
									}
								biTemp2.setData(wrTemp2);
								//componentImage.set(biTemp2);
								dialogCalibProgress.setImage(biTemp2);
								
								//frame.setCancelled(true);
							}
							
							
							
							
							/*
							//count elements of each intensity level
							int[] matches = new int[256];
							for (int h = 0; h < imgMatGrayscale.length; h++)
								for (int w = 0; w < imgMatGrayscale[1].length; w++)
								{
									matches[ imgMatGrayscaleTemplate[h][w] ]++;
								}
							for (int i = 0; i < matches.length; i++)
							{
								if (i == 0 || i == 255 || (i % 2) == 0)
										System.err.println("Intensity " + i + ": " + matches[i] + " match(es).");
							}
							*/
							
							//remove spurious artifacts by replacing pixels in a non-uniform surrounding by a consensus value
							//System.err.println("Replacing pixels by consensus...");
							//GrayscaleImageEdit.replaceByConsensus(imgMatGrayscaleTemplate, imgMatGrayscale, 5, 5, 0.75, 255);
							
							
							/*
							//count elements of each intensity level
							matches = new int[256];
							for (int h = 0; h < imgMatGrayscale.length; h++)
								for (int w = 0; w < imgMatGrayscale[1].length; w++)
								{
									matches[ imgMatGrayscaleTemplate[h][w] ]++;
								}
							for (int i = 0; i < matches.length; i++)
							{
								if (i == 0 || i == 255 || (i % 2) == 0)
										System.err.println("Intensity " + i + ": " + matches[i] + " match(es).");
							}
							*/
							
							
							//set up binary image
							for (int h = 0; h < imgCalib.getHeight(); h++)
								for (int w = 0; w < imgCalib.getWidth(); w++)
								{
									imgMatCalibBinary[h][w] = (imgMatCalibGrayscale[h][w] != 0) ? (byte)1 : (byte)0;
									//System.err.print( bluePixels[ index - 1] + "," );
								}
							
							
							if (!frame.getCancelled())
							{
								dialogCalibProgress.getLabelCurrent().setText("Identifying objects in image (segmentation)");
								try
								{
									imgCalibSeg = GrayscaleImageEdit.segmentBinaryImage(imgMatCalibBinary, true);
									vecSegObjsCalib = GrayscaleImageEdit.fetchSegObjCoord(imgCalibSeg);
									dialogCalibProgress.getPBInterim().setValue(80);
								} catch (Throwable t)
								{
									frame.setCancelled(true);
									t.printStackTrace();
									JOptionPane.showMessageDialog(frame, "Object segmentation failed during identification of segmentation object.\n"+
										"This is potentially due to the utilized threshold value.\n" +
										"Please adjust the threshold value/method and try again.",
										"Segmentation failed for calibration object", JOptionPane.ERROR_MESSAGE);
								}
							}
							
							if (!frame.getCancelled())
							{
							
								//fetch borders, calculate distance measures between border pixels and sort them accordingly
								dialogCalibProgress.getLabelCurrent().setText("Identifying and rearranging border pixels...");
								Vector[] vecSegObjBordersCalibArr = GrayscaleImageEdit.fetchSegObjCoordBorder(imgCalibSeg, false, false, frame, dialogCalibProgress.getPBCurrent() );
								if (vecSegObjBordersCalibArr != null)
								{
									vecSegObjBordersCalib = vecSegObjBordersCalibArr[0]; //border points
									//vecSegObjBorderBPCalib = vecSegObjBordersCalibArr[1]; //break points
								}
								
								dialogCalibProgress.getPBInterim().setValue(85);
							}
							
							int numGoodObj = 0;
							if (!frame.getCancelled())
							{
								dialogCalibProgress.getLabelCurrent().setText("Filtering dubious objects...");
							
								//filter 'bad' objects
								long imgArea = imgMatCalibGrayscaleTemplate.length*imgMatCalibGrayscaleTemplate[0].length;
								Vector vecSegObjsTemp = new Vector(vecSegObjsCalib.size());
								Vector vecSegObjBordersTemp = new Vector(vecSegObjBordersCalib.size());
								//Vector vecSegObjBordersBPTemp = new Vector(vecSegObjBorderBPCalib.size());
								boolean[] goodObjects = GrayscaleImageEdit.filterObjects(vecSegObjsCalib, imgArea,
									settings.getMinObjSizeRel()/100.0, settings.getMinObjDensRel()/100.0 );
								for (int i = 0; i < goodObjects.length; i++)
								{
									if (goodObjects[i])
									{
										numGoodObj++;
										vecSegObjsTemp.add( (Vector) vecSegObjsCalib.get(i) );
										vecSegObjBordersTemp.add( (Vector) vecSegObjBordersCalib.get(i) );
										//vecSegObjBordersBPTemp.add( (Vector) vecSegObjBorderBPCalib.get(i) );
									}
								}
								vecSegObjsCalib = vecSegObjsTemp;
								vecSegObjBordersCalib = vecSegObjBordersTemp;
								//vecSegObjBorderBPCalib = vecSegObjBordersBPTemp;
								System.err.println("Kept " + numGoodObj + " good objects");
								
							
								//we have to filter the segmentation so that we only keep the biggest object
								
								if (numGoodObj > 1)
								{
									System.err.print("Found more than one (" + numGoodObj + ") putative calib. object, keeping the largest one");
									
									Vector vecSegObjsTemp2 = new Vector(vecSegObjsCalib.size());
									Vector vecSegObjBordersTemp2 = new Vector(vecSegObjBordersCalib.size());
									//Vector vecSegObjBordersBPTemp2 = new Vector(vecSegObjBorderBPCalib.size());
									
									int biggestObjInd = -1;
									int currArea = -1;
									int maxArea = Integer.MIN_VALUE;
									Vector currObj;
									
									for (int i = 0; i < vecSegObjsCalib.size(); i++)
									{
										currObj = (Vector) vecSegObjsCalib.get(i);
										currArea = currObj.size()*currObj.size();
										if (currArea > maxArea)
										{
											maxArea = currArea;
											biggestObjInd = i;
										}
									}
									
									if (biggestObjInd >= 0)
									{
									
										vecSegObjsTemp2.add( (Vector) vecSegObjsCalib.get(biggestObjInd) );
										vecSegObjBordersTemp2.add( (Vector) vecSegObjBordersCalib.get(biggestObjInd) );
										//vecSegObjBordersBPTemp2.add( (Vector) vecSegObjBorderBPCalib.get(biggestObjInd) );
									}
									
									vecSegObjsCalib = vecSegObjsTemp2;
									vecSegObjBordersCalib = vecSegObjBordersTemp2;
									//vecSegObjBorderBPCalib = vecSegObjBordersBPTemp2;
									System.err.println(" (Object #" + (biggestObjInd+1) + ")");
									
									
								}
								vecSegObjCentersCalib = GrayscaleImageEdit.findObjectCentroids(vecSegObjsCalib);
								imgCalibSeg = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjsCalib, imgCalibSeg[0].length, imgCalibSeg.length);
								
								
								dialogCalibProgress.getPBInterim().setValue(85);
									
							}
							
							if (!frame.getCancelled() )
							{
								if (vecSegObjsCalib.size() != 1)
								{
									System.err.println("Incorrect number of calibration objects: should be 1. Aborting");
									
									JOptionPane.showMessageDialog(frame, "Unable to locate any calibration object.\n" +
										"Please provide alternative calibration files.",
										"Failed to locate calibration object", JOptionPane.ERROR_MESSAGE);

									frame.setCancelled(true);
								}
							}
							
							
								
							if (!frame.getCancelled())
							{
								//update the segmentation image with only the 'good' objects
								dialogCalibProgress.getLabelCurrent().setText("Displaying result...");
								GrayscaleImageEdit.paintBinaryMatrix(vecSegObjsCalib, imgMatCalibGrayscale, 255);
								
							
								index = 0;
								for (int h = 0; h < imgCalib.getHeight(); h++)
									for (int w = 0; w < imgCalib.getWidth(); w++)
									{
										//bluePixels[ index++ ] = imgMatCalibGrayscale[h][w];
										
										imgMatCalibBinary[h][w] = (imgMatCalibGrayscale[h][w] != 0) ? (byte)1 : (byte)0;
										
										bluePixelGrayscale[ index++ ] = (imgMatCalibBinary[h][w] != 0) ? (int)imgCalibSeg[h][w]*(int)(255.0/numGoodObj) : (int)0;
										//System.err.print( bluePixels[ index - 1] + "," );
									}
								
								/*
								for (int i = 0; i < bluePixels.length; i++)
										if (bluePixels[i] <= quantileBlue)
											bluePixels[i] = 0;
										else
											bluePixels[i] = 255;
								*/
								
								wrGrayscale.setPixels(0,0,imgCalib.getWidth(),imgCalib.getHeight(),bluePixelGrayscale);
								imgCalibGrayscale.setData(wrGrayscale);
								
								
								System.err.println("Done.");
								dialogCalibProgress.getPBInterim().setValue(100);
								dialogCalibProgress.getPBCurrent().setValue(100);
								
								BufferedImage imgCalibDisplay = PlanarImageEdit.applyMask(imgCalib, imgMatCalibBinary);
								//componentImage.set(imgCalibDisplay);
								dialogCalibProgress.setImage(imgCalibDisplay);
								
								Vector[] vecHorizVertLinesObj = GrayscaleImageEdit.fetchHorizVertLines(imgCalibSeg, vecSegObjsCalib,
									vecSegObjBordersCalib, settings.getForceOrtho(), settings.getForceHorizVert() );
								
								Vector vecHorizVertLinesCalib = vecHorizVertLinesObj[0];
								Vector vecIntersectPointsCalib = vecHorizVertLinesObj[1];
								
								//calc some stats here
								Vector[] vecCurrHorizVertLines = (Vector[])vecHorizVertLinesCalib.get(0);
								Vector pixelsHorizCenter = (Vector)vecCurrHorizVertLines[1];
								Vector pixelsVertCenter = (Vector)vecCurrHorizVertLines[0];
								Point[] lineHorizCenter = (Point[])pixelsHorizCenter.get(0);
								Point[] lineVertCenter = (Point[])pixelsVertCenter.get(0);
								
								double distHoriz = lineHorizCenter[0].distance(lineHorizCenter[1]);
								double distVert = lineVertCenter[0].distance(lineVertCenter[1]);
								double scaleHoriz = distHoriz / calibWidth;
								double scaleVert = distVert / calibHeight;
								double scaleMean = (scaleHoriz+scaleVert)/2.0;
								
								System.err.println("Horizontal distance is " + distHoriz + " pixels, and the real distance is " + calibWidth + " mm" );
								System.err.println("This results in a horizontal scaling factor of " + scaleHoriz + " pixels / mm");
								System.err.println("Vertical distance is " + distVert + " pixels, and the real distance is " + calibHeight + " mm");
								System.err.println("This results in a vertical scaling factor of " + scaleVert + " pixels / mm");
								
								putativeScaleParam = scaleMean; //set this value only if the user presses 'OK'
								//((SpinnerNumberModel)spinnerScaleParam.getModel()).setValue( new Double(scaleMean) );
									
								dialogCalibProgress.getLabelInterim().setText("Scaling parameter is " + scaleMean + " pixels/mm");
									
								GrayscaleImageEdit.paintSegmentationResults(vecSegObjsCalib, vecSegObjsCalib, vecSegObjCentersCalib, vecSegObjBordersCalib, null, vecHorizVertLinesCalib, vecIntersectPointsCalib, imgCalibDisplay.getGraphics());
								GrayscaleImageEdit.paintBorders(vecSegObjBordersCalib, Color.YELLOW, imgCalibDisplay.getGraphics());
								GrayscaleImageEdit.paintObjectIds(vecSegObjCentersCalib, imgCalibDisplay.getGraphics());
														
								
								//componentImage.set(imgCalibDisplay);
								//componentImage.repaint();
								dialogCalibProgress.setImage(imgCalibDisplay);
								
								
								
								/*
								if (!frame.getCancelled())
								{
									//update the segmentation image with only the 'good' objects
									dialogCalibProgress.getPBInterim().setValue(95);
									dialogCalibProgress.getLabelCurrent().setText("Calculating statistics...");
							
									GrayscaleImageEdit.calcSegStats(vecSegObjsCalib, vecSegObjBordersCalib, vecHorizVertLines, vecIntersectPoints,
										1);
										
									
							
								}
								*/
								
							}
							
							dialogCalibProgress.getLabelCurrent().setText("Done");
							
							// send runnable to the Swing thread
							// the runnable is queued after the
							// results are returned
							SwingUtilities.invokeLater
							(
								new Runnable()
								{
									public void run()
									{
										
										dialogCalibProgress.getButtonOK().setEnabled(true);
										dialogCalibProgress.getButtonSave().setEnabled(true);
										dialogCalibProgress.getButtonCancel().setEnabled(true);
										
										/*
										dialogCalibProgress.dispose();
										dialogCalibProgress.setVisible(false);

										frame.setRunning(false);
										frame.setEnabled(true);
										*/
									}
								}
							);
						}

					}.start();
								
				}
			}
			
		} else if (e.getSource() == buttonSettings)
		{
			settings.setInputDir(lastInDir);
			settings.setOutputDir(lastOutDir);
			
			LaminaSettings dlgSettings = new LaminaSettings(frame, APPL_NAME + " settings", false, false,settings);
			//dlgCreateGrid.setIconImage(frame.getIconImage());
			dlgSettings.setModal(true);
			frame.setFocusableWindowState(false);
			dlgSettings.setVisible(true);
			frame.setFocusableWindowState(true);
			
		
			if (dlgSettings.getStatus() == LaminaSettings.STATUS_OK)
			{
				//update settings object
				settings = dlgSettings.getSettings();
				lastInDir = settings.getInputDir();
				lastOutDir = settings.getOutputDir();
				((SpinnerNumberModel)spinnerNumLandmarks.getModel()).setValue( new Integer( settings.getNumLandmarks() ) );
			
				updateSaveButtons(true);
				
			}
			
			
		
		} else if (e.getSource() == buttonRevert)
		{
			buttonRevert.setEnabled(false);
			buttonApplyFilterContour.setEnabled(true);
			buttonApplyNoContour.setEnabled(true);
			buttonCalcStats.setEnabled(false);
			buttonCrop.setEnabled(false);
			
			Raster rasterOrg = imgOrg.getData();
			
			
			
			//switch to default image type if we can't recognize it
			int imgType = imgOrg.getType();
			if (imgType == 0)
				imgType = BufferedImage.TYPE_INT_RGB;
			
			imgDisplay = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), imgType );
			imgDisplay.setData(rasterOrg);
			//imgDisplayBackup = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), imgType );
			//imgDisplayBackup.setData(rasterOrg);
			
			panelSplit2.setVisible(false);
			splitPane.setDividerLocation( (int)frame.getSize().getHeight() );
			//isCropped = false;
			
			
			vecSegObjs = null;
			vecSegObjBorders = null;
			vecSegObjBordersShort = null;
			runTime.gc();
			
			componentImageCropped.removeDrawnObjects();
			componentImage.set(imgDisplay);
			componentImage.revalidate();
		
		} else if (e.getSource() == buttonApplyFilterContour || e.getSource() == buttonApplyNoContour)
		{
			runContourID = settings.getFindContour();
			modifierContourPB = (runContourID) ? 1 : 2;
			
		
			new Thread()
			{
				public void run()
				{
					frame.setCancelled(false);
					frame.setRunning(true);
					frame.setEnabled(false);

					
			
					//System.err.println("Filtering...");
					dialogProgress = new JDialogExtract(frame, Lamina.APPL_NAME + " -- processing file", true,
						new Dimension(frame.getWidth()-20, 180) );
					
					dialogProgress.getLabelInterim().setText("Overall progress");
					//dialogProgress.getPBTotal().setVisible(false);
					//dialogProgress.getPBTotal().setValue(0);
					
					Raster rasterOrg = imgDisplay.getData();
					
					dialogProgress.getLabelCurrent().setText("Extracting blue band...");
							
					//imgGrayscale = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
					//WritableRaster wrGrayscale = imgGrayscale.getRaster();
					
					// set up the binary and grayscale images that will be used for segmentation
					
					//int i[] = new int[1];
					//int[] bluePixels = new int[imgOrg.getHeight()*imgOrg.getWidth()];
					
					//int[] bluePixelGrayscale = new int[imgOrg.getHeight()*imgOrg.getWidth()];
					imgMatGrayscaleOrg = new int[imgOrg.getHeight()][imgOrg.getWidth()];
					imgMatGrayscaleTemplateOrg = new int[imgOrg.getHeight()][imgOrg.getWidth()];
					
					imgMatBinaryOrg = new byte[imgOrg.getHeight()][imgOrg.getWidth()];
					//double[][] imgMatMaxDiffChannel = new double[imgOrg.getHeight()][imgOrg.getWidth()];
					
					int edgePixelLowerX = (int)Math.ceil(EDGE_REL_DIST*imgOrg.getWidth());
					int edgePixelUpperX = (int)Math.floor( (1-EDGE_REL_DIST)*imgOrg.getWidth());
					int edgePixelLowerY = (int)Math.ceil(EDGE_REL_DIST*imgOrg.getHeight());
					int edgePixelUpperY = (int)Math.floor( (1-EDGE_REL_DIST)*imgOrg.getHeight());
							
					//System.err.println("(" + edgePixelLowerX + "," + edgePixelUpperX + ")");
					//System.err.println("(" + edgePixelLowerY + "," + edgePixelUpperY + ")");
							
						
					
					double numPixels = imgOrg.getHeight()*imgOrg.getWidth();
					double meanIntensity = 0.0;
					int index = 0;
					int numLowInt = 0;
					double maxDiffChannel = Integer.MIN_VALUE;
					
					int r,g,b;
					
					for (int h = 0; h < imgOrg.getHeight(); h++)
						for (int w = 0; w < imgOrg.getWidth(); w++)
						{
							
							b = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B);
							
							//imgMatMaxDiffChannel[h][w] = (r + g + b)/3.0;
							
							/*
							r = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R);
							g = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G);
							
							imgMatMaxDiffChannel[h][w] =
								255 - 
								( Math.max( Math.max(r, g), b) -
								Math.min( Math.min(r, g), b) );
							
							if (imgMatMaxDiffChannel[h][w] > maxDiffChannel)
								maxDiffChannel = imgMatMaxDiffChannel[h][w];
							*/
							
							//try to remove black edge pixels
							/*
							if ( (w < edgePixelLowerX || w > edgePixelUpperX || h < edgePixelLowerY || h > edgePixelUpperY) && imgMatMaxDiffChannel[h][w] < EDGE_INT_THRESH)
							{
								b = 255;
							}
							*/
							
							
							
							//bluePixels[ index++ ] = b;
							imgMatGrayscaleTemplateOrg[h][w] = b;
							imgMatGrayscaleOrg[h][w] = imgMatGrayscaleTemplateOrg[h][w];
							meanIntensity += imgMatGrayscaleTemplateOrg[h][w]/numPixels;
							//System.err.print( bluePixels[ index - 1] + "," );
							
							//ranges from 0 to 255
							/*
							imgMatMaxDiffChannel[h][w] =
								(int)(
								Math.max( Math.max(rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R),
									rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G)), 
									rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B) ) /
								
								Math.max( Math.min( Math.min(rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R),
									rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G)), 
									rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B) ), 1.0) );
							*/
							
							
									
								
							
							//square
							//imgMatMaxDiffChannel[h][w] = imgMatMaxDiffChannel[h][w]*imgMatMaxDiffChannel[h][w]*imgMatMaxDiffChannel[h][w];
							
							
							//truncate pixels with a too low intensity
							/*
							if (imgMatMaxDiffChannel[h][w] < 40)
							{
								bluePixels[index-1] = 255;
								imgMatGrayscaleTemplate[h][w] = 255;
								imgMatGrayscale[h][w] = 255;
								
							}
							*/
						}
					
					/*
					//normalize the maxDiffChannel and multiply with the intensity values
					meanIntensity = 0.0;
					for (int h = 0; h < imgOrg.getHeight(); h++)
						for (int w = 0; w < imgOrg.getWidth(); w++)
						{
							imgMatGrayscaleTemplate[h][w] = (int)Math.round(imgMatGrayscaleTemplate[h][w]*(imgMatMaxDiffChannel[h][w]/maxDiffChannel));
							imgMatGrayscale[h][w] = imgMatGrayscaleTemplate[h][w];
							meanIntensity += imgMatGrayscaleTemplate[h][w]/numPixels;
						}
					*/
					
					//imgMatGrayscaleTemplate = GrayscaleImageEdit.applyMask(imgMatGrayscaleTemplate, imgMatMaxDiffChannel, true);
					//imgMatGrayscale = GrayscaleImageEdit.applyMask(imgMatGrayscale, imgMatMaxDiffChannel, true);
					
					if (!frame.getCancelled())
					{
						BufferedImage biTemp = new BufferedImage(imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_INT_RGB);
						WritableRaster wrTemp = biTemp.getRaster();
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
								
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatGrayscaleTemplateOrg[h][w]);
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatGrayscaleTemplateOrg[h][w]);
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatGrayscaleTemplateOrg[h][w]);
							
							}
						biTemp.setData(wrTemp);
						componentImage.set(biTemp);
						
						//frame.setCancelled(true);
					}
					
						
					
					//frame.setCancelled(true); // for now
					
					System.err.println("Number of pixels below intensity threshold: " + numLowInt + "/" + (imgOrg.getHeight()*imgOrg.getWidth()));
					
										// height							//width
					System.err.println(""+imgMatGrayscaleOrg.length +","+imgMatGrayscaleOrg[1].length);
					System.err.println("Average intensity value: " + meanIntensity);
					
					dialogProgress.getPBInterim().setValue(5*modifierContourPB);
					
					//// --- REMOVAL OF OBJECT SHADOW
					/*
					double shadowThresh = 0;
					if (!frame.getCancelled())
					{
						//GrayscaleImageEdit.truncateImage(imgMatIntensity,imgMatGrayscaleTemplate,20,255);
						
						//JAI.create("filestore", componentImage.get(), filename, codecId);
						
						dialogProgress.getLabelCurrent().setText("Detecting optimal threshold for shadow removal...");
						//shadowThresh = GrayscaleImageEdit.detectThresholdGreedy(imgMatMaxDiffChannel, imgMatGrayscale,
						//	5, 5,(int)meanIntensity, 5, frame, dialogProgress.getPBCurrent() );
						shadowThresh = GrayscaleImageEdit.detectThresholdExhaustive(imgMatMaxDiffChannel, imgMatGrayscale,
							3, 3, 0.05, 1.0, 1.5, frame, dialogProgress.getPBCurrent() );
						
						
						//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
						//	10, 10, (int)meanIntensity, 10, frame, dialogProgress.getPBCurrent() );
							
						dialogProgress.getPBInterim().setValue(25);
						System.err.println("The selected threshold is " + shadowThresh);
					}
					
					if (!frame.getCancelled())
					{
						//apply the threshold value
						dialogProgress.getLabelCurrent().setText("Applying (shadow) threshold to current image...");
						
						
						GrayscaleImageEdit.thresholdImage(imgMatMaxDiffChannel,imgMatGrayscale,shadowThresh);
						GrayscaleImageEdit.thresholdImage(imgMatMaxDiffChannel,imgMatGrayscaleTemplate,shadowThresh);//important!
						
						//so that the segmentation threshold will work
						//imgMatGrayscale = GrayscaleImageEdit.invertImage(imgMatGrayscale, 255);
						//imgMatGrayscaleTemplate = GrayscaleImageEdit.invertImage(imgMatGrayscaleTemplate, 255);
						
						
						dialogProgress.getPBInterim().setValue(30);
					}
					
					if (!frame.getCancelled())
					{
						BufferedImage biTemp = new BufferedImage(imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_INT_RGB);
						WritableRaster wrTemp = biTemp.getRaster();
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
								
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatGrayscaleTemplate[h][w]);
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatGrayscaleTemplate[h][w]);
								wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatGrayscaleTemplate[h][w]);
							
							}
						biTemp.setData(wrTemp);
						componentImage.set(biTemp);
						
						//frame.setCancelled(true);
					}
					*/
					
					/*
					double quant = 0.10;
					double quantileBlue = MiscMath.quantile(bluePixels, quant);
					System.err.println("Quantile intensity (" + quant + "): " + quantileBlue);
					
					for (int i = 0; i < bluePixels.length; i++)
							if (bluePixels[i] <= quantileBlue)
								bluePixels[i] = 255;
							else
								bluePixels[i] = 0;
					*/
					/*
					int INT_THRESH_MIN=5;
					int INT_THRESH_INC=10;
					int INT_THRESH_MAX=200;
					int WINDOW_HEIGHT=3;
					int WINDOW_WIDTH=3;
					for (int i = INT_THRESH_MIN; i <= INT_THRESH_MAX; i += INT_THRESH_INC)
					{
						System.err.println("Filtering image with intensity threshold " + i + "...");
						
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								if (imgMatGrayscaleTemplate[h][w] <= i)
									imgMatGrayscale[h][w]=(int)255;
								else
									imgMatGrayscale[h][w]=(int)0;
							}
						
						System.err.print("Calculating average standard deviation..");
						double[] stds = GrayscaleImageEdit.calcStds(imgMatGrayscale, WINDOW_HEIGHT, WINDOW_WIDTH);
						double avStds = MiscMath.mean(stds);
						System.err.println(""+avStds+"");
					}
					*/
					
					
					/*
					double quant = 0.10;
					double quantileBlue = MiscMath.quantile(bluePixels, quant);
					System.err.println("Quantile intensity (" + quant + "): " + quantileBlue);
					*/
					int quantileBlue = 0;
					if (!frame.getCancelled())
					{
						//GrayscaleImageEdit.truncateImage(imgMatIntensity,imgMatGrayscaleTemplate,20,255);
						
						//JAI.create("filestore", componentImage.get(), filename, codecId);
						
						
						if (settings.getThresholdSearchGreedy() )
						{
							//int startIntensity = (int)Math.round( MiscMath.quantile(imgMatGrayscale, 0.25) );
							int startIntensity = (int)Math.round(meanIntensity);
							System.err.println("Start intensity for greedy threshold is " + startIntensity);
							
						
							dialogProgress.getLabelCurrent().setText("Detecting optimal threshold for segmentation (greedy)...");
							quantileBlue = GrayscaleImageEdit.detectThresholdGreedy(imgMatGrayscaleTemplateOrg, imgMatGrayscaleOrg,
								3, 3, startIntensity, (int)settings.getThresholdSearchStepLength(),
								frame, dialogProgress.getPBCurrent() );
								
							//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
							//	10, 10, (int)meanIntensity, 10, frame, dialogProgress.getPBCurrent() );
						} else if (settings.getThresholdSearchExhaustive() )
						{
							dialogProgress.getLabelCurrent().setText("Detecting optimal threshold for segmentation (exhaustive)...");
							quantileBlue = (int)GrayscaleImageEdit.detectThresholdExhaustive(imgMatGrayscaleTemplateOrg, imgMatGrayscaleOrg,
								3, 3, (int)settings.getThresholdSearchStepLength(), 0, 255,
								frame, dialogProgress.getPBCurrent() );
								
							//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
							//	10, 10, (int)meanIntensity, 10, frame, dialogProgress.getPBCurrent() );
							
						} else
						{
							dialogProgress.getLabelCurrent().setText("Using manual threshold for segmentation...");
							quantileBlue = (int)settings.getThresholdManual();
						} 	
							
						dialogProgress.getPBInterim().setValue(25*modifierContourPB);
						System.err.println("The selected threshold is " + quantileBlue);
					}
					
					
					if (!frame.getCancelled())
					{
						//apply the threshold value
						dialogProgress.getLabelCurrent().setText("Applying (segmentation) threshold to current image...");
						GrayscaleImageEdit.thresholdImage(imgMatGrayscaleTemplateOrg,imgMatGrayscaleOrg,quantileBlue);
						GrayscaleImageEdit.thresholdImage(imgMatGrayscaleTemplateOrg,imgMatGrayscaleTemplateOrg,quantileBlue);//important!
						dialogProgress.getPBInterim().setValue(28*modifierContourPB);
					}
					
					
					if (!frame.getCancelled())
					{
						dialogProgress.getLabelCurrent().setText("Applying median filter on binary image (noise reduction)");
						GrayscaleImageEdit.medianFilter(imgMatGrayscaleTemplateOrg,imgMatGrayscaleOrg, 3, 3);	
						dialogProgress.getPBInterim().setValue(30*modifierContourPB);
					}
					
					
					//display result
					if (!frame.getCancelled())
					{
						BufferedImage biTemp2 = new BufferedImage(imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_INT_RGB);
						WritableRaster wrTemp2 = biTemp2.getRaster();
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
								
								wrTemp2.setSample(w,h,PlanarImageEdit.BAND_R, imgMatGrayscaleOrg[h][w]);
								wrTemp2.setSample(w,h,PlanarImageEdit.BAND_G, imgMatGrayscaleOrg[h][w]);
								wrTemp2.setSample(w,h,PlanarImageEdit.BAND_B, imgMatGrayscaleOrg[h][w]);
							
							}
						biTemp2.setData(wrTemp2);
						componentImage.set(biTemp2);
					}
					
					
					
					
					/*
					//count elements of each intensity level
					int[] matches = new int[256];
					for (int h = 0; h < imgMatGrayscale.length; h++)
						for (int w = 0; w < imgMatGrayscale[1].length; w++)
						{
							matches[ imgMatGrayscaleTemplate[h][w] ]++;
						}
					for (int i = 0; i < matches.length; i++)
					{
						if (i == 0 || i == 255 || (i % 2) == 0)
								System.err.println("Intensity " + i + ": " + matches[i] + " match(es).");
					}
					*/
					
					//remove spurious artifacts by replacing pixels in a non-uniform surrounding by a consensus value
					//System.err.println("Replacing pixels by consensus...");
					//GrayscaleImageEdit.replaceByConsensus(imgMatGrayscaleTemplate, imgMatGrayscale, 5, 5, 0.75, 255);
					
					
					/*
					//count elements of each intensity level
					matches = new int[256];
					for (int h = 0; h < imgMatGrayscale.length; h++)
						for (int w = 0; w < imgMatGrayscale[1].length; w++)
						{
							matches[ imgMatGrayscaleTemplate[h][w] ]++;
						}
					for (int i = 0; i < matches.length; i++)
					{
						if (i == 0 || i == 255 || (i % 2) == 0)
								System.err.println("Intensity " + i + ": " + matches[i] + " match(es).");
					}
					*/

					
					//set up binary image
					for (int h = 0; h < imgOrg.getHeight(); h++)
						for (int w = 0; w < imgOrg.getWidth(); w++)
						{
							imgMatBinaryOrg[h][w] = (imgMatGrayscaleOrg[h][w] != 0) ? (byte)1 : (byte)0;
							//System.err.print( bluePixels[ index - 1] + "," );
						}
					
					
					if (!frame.getCancelled())
					{
						
						dialogProgress.getLabelCurrent().setText("Identifying objects in image (segmentation)");
						try
						{
							imgSeg = GrayscaleImageEdit.segmentBinaryImage(imgMatBinaryOrg, true);
							vecSegObjsOrg = GrayscaleImageEdit.fetchSegObjCoord(imgSeg);
							dialogProgress.getPBInterim().setValue(33*modifierContourPB);
						} catch (Throwable t)
						{
							frame.setCancelled(true);
							t.printStackTrace();
							JOptionPane.showMessageDialog(frame, "Object segmentation failed. This is potentially due to the utilized threshold value.\n" +
								"Please adjust the threshold value/method and try again.",
								"Object segmentation failed", JOptionPane.ERROR_MESSAGE);
						}
					}
					
					
					int numGoodObj = 0;
					if (!frame.getCancelled())
					{
						dialogProgress.getLabelCurrent().setText("Filtering small/sparse objects...");
					
						//filter 'bad' objects
						long imgArea = imgMatGrayscaleTemplateOrg.length*imgMatGrayscaleTemplateOrg[0].length;
						Vector vecSegObjsTemp = new Vector(vecSegObjsOrg.size());
						//Vector vecSegObjBordersTemp = new Vector(vecSegObjBorders.size());
						//Vector vecSegObjBordersBPTemp = new Vector(vecSegObjBorderBP.size());
						boolean[] goodObjects = GrayscaleImageEdit.filterObjects(vecSegObjsOrg, imgArea,
							settings.getMinObjSizeRel()/100.0, settings.getMinObjDensRel()/100.0 );
						for (int i = 0; i < goodObjects.length; i++)
						{
							if (goodObjects[i])
							{
								numGoodObj++;
								vecSegObjsTemp.add( (Vector) vecSegObjsOrg.get(i) );
								//vecSegObjBordersTemp.add( (Vector) vecSegObjBorders.get(i) );
								//vecSegObjBordersBPTemp.add( (Vector) vecSegObjBorderBP.get(i) );
							}
						}
						vecSegObjsOrg = vecSegObjsTemp;
						vecSegObjCentersOrg = GrayscaleImageEdit.findObjectCentroids(vecSegObjsOrg);
						
						//vecSegObjBorders = vecSegObjBordersTemp;
						//vecSegObjBorderBP = vecSegObjBordersBPTemp;
						System.err.println("Kept " + numGoodObj + " good objects");
						
						//repaint the segmentation matrix, keeping only the 'good' elements
						imgSeg = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjsOrg, imgSeg[0].length, imgSeg.length);
						
						dialogProgress.getPBInterim().setValue(35*modifierContourPB);
						
						if (vecSegObjsOrg.size() == 0)
						{
							frame.setCancelled(true);
							JOptionPane.showMessageDialog(frame, "Unable to find any objects in image.\n" +
								"This may be caused by the the object size/density filtering parameters\n"+
								"being set too high or too low.",
								"Unable to find any objects in the image", JOptionPane.ERROR_MESSAGE);
						}
					}
					
					//display result
					if (!frame.getCancelled())
					{
						dialogProgress.getLabelCurrent().setText("Displaying result...");
						
						BufferedImage biTemp2 = new BufferedImage(imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_INT_RGB);
						biTemp2.setData( imgOrg.getData() );
						WritableRaster wrTemp2 = biTemp2.getRaster();
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
								//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
								if (imgSeg[h][w] == 0)
								{
									wrTemp2.setSample(w,h, PlanarImageEdit.BAND_R, 0 );
									wrTemp2.setSample(w,h,PlanarImageEdit.BAND_G, 0 );
									wrTemp2.setSample(w,h,PlanarImageEdit.BAND_B, 0 );
								} 
							}
						
						// find centerpoint of objects
						for (int i = 0; i < vecSegObjsOrg.size(); i++)
						{
							Vector vecSegObjsCurr = (Vector)vecSegObjsOrg.get(i);
							
							//find the extreme points 
							Point maxX = new Point(Integer.MIN_VALUE, 0);
							Point minX = new Point(Integer.MAX_VALUE, 0);
							Point maxY = new Point(0, Integer.MIN_VALUE);
							Point minY = new Point(0, Integer.MAX_VALUE);
						
							int x,y;
							for (int j = 0; j < vecSegObjsCurr.size(); j++)
							{
								Point p = (Point)vecSegObjsCurr.get(j);
								x = (int)p.getX();
								y = (int)p.getY();
								
								if (x > maxX.getX())
									maxX = new Point(x, y);
								if (x < minX.getX())
									minX = new Point(x, y);
									
								if (y > maxY.getY())
									maxY = new Point(x, y);
								if (y < minY.getY())
									minY = new Point(x, y);
							}
							
							Point centerPoint = new Point( (int)( minX.getX() + (maxX.getX() - minX.getX())/2.0),
								(int)( minY.getY() + (maxY.getY() - minY.getY())/2.0) );
							
							Graphics2D g2d = (Graphics2D)biTemp2.getGraphics();
							g2d.setColor( Color.WHITE );
							g2d.setFont( new Font("Times New Roman", Font.BOLD, 48) );
							g2d.drawString("#" + (i+1), (int)centerPoint.getX(), (int)centerPoint.getY() );
						}
						
						biTemp2.setData(wrTemp2);
						componentImage.set(biTemp2);
						
						dialogProgress.getPBInterim().setValue(37*modifierContourPB);
					}
					
					
					
					
					// Now crop the image to make the user interface easier
					if (!frame.getCancelled())
					{
						dialogProgress.getLabelCurrent().setText("Cropping image area containing objects...");
						
						//find the rectangles spanning the objects in the image
						Vector vec = new Vector( vecSegObjsOrg.size() );
						for (int i = 0; i < vecSegObjsOrg.size(); i++)
						{
							Vector vecCurr = (Vector)vecSegObjsOrg.get(i);
							
							//find the most extreme point
							Point p = (Point)vecCurr.get(0);
							int maxX = Integer.MIN_VALUE;
							int minX = Integer.MAX_VALUE;
							int maxY = Integer.MIN_VALUE;
							int minY = Integer.MAX_VALUE;
							int xScaled, yScaled;
							
							for (int j = 0; j < vecCurr.size(); j++)
							{
								p = (Point)vecCurr.get(j);
								//xScaled = (int)(p.getX()/zoomLevel);
								//yScaled = (int)(p.getY()/zoomLevel);
								xScaled = (int)(p.getX());
								yScaled = (int)(p.getY());
								
								if ( xScaled > maxX )
									maxX = xScaled;
								if ( xScaled < minX )
									minX = xScaled;
								if ( yScaled > maxY )
									maxY = yScaled;
								if ( yScaled < minY )
									minY = yScaled;
							}

							// create a rectangle spanning this area
							int startX = (int)Math.max(1, minX - Lamina.CROP_PADDING);
							int startY = (int)Math.max(1, minY-Lamina.CROP_PADDING);
							int width = maxX-minX+2*Lamina.CROP_PADDING;
							int height = maxY-minY+2*Lamina.CROP_PADDING;
							if ( (startX + width) >= imgOrg.getWidth() )
								width = imgOrg.getWidth() - startX - 1;
							if ( (startY + height) >= imgOrg.getHeight() )
								height = imgOrg.getHeight() - startY - 1;
							
							
							Rectangle rect = new Rectangle( startX, startY, width, height);
							vec.add(rect);
							
							
						}
						
						//crop the RGB image
						imgCropped = PlanarImageEdit.cropImages(imgDisplay, vec, 1.0);
						zoomLevelCropped = zoomLevel;
						//imgOrg = imgCropped; //for now
						
						
						//now also crop the segmentation result
						
						imgSegCropped = GrayscaleImageEdit.cropMatrix(imgSeg, vec);
						imgMatGrayscale = GrayscaleImageEdit.cropMatrix(imgMatGrayscaleOrg, vec);
						imgMatGrayscaleTemplate = GrayscaleImageEdit.cropMatrix(imgMatGrayscaleTemplateOrg, vec);
						
						//reconstruct binary matrix
						imgMatBinary = new byte[imgCropped.getHeight()][imgCropped.getWidth()];
						//set up binary image
						for (int h = 0; h < imgCropped.getHeight(); h++)
							for (int w = 0; w < imgCropped.getWidth(); w++)
								imgMatBinary[h][w] = (imgSegCropped[h][w] != 0) ? (byte)1 : (byte)0;
						
					
						
						//imgSeg = imgSegCropped; //for now
						//vecSegObjs = GrayscaleImageEdit.intMatrixToVectorOfPoints(imgSegCropped);
						//imgMatGrayscale =  GrayscaleImageEdit.cropMatrix(imgMatGrayscale, vec);
						// imgMatGrayscaleTemplate =  GrayscaleImageEdit.cropMatrix(imgMatGrayscaleTemplate, vec);
						
						//display result
						imgDisplay = imgCropped;
						componentImageCropped.set(imgCropped);
						//componentImageCropped.setScaleType(JComponentDisplay.SCALE_MODE_PRESCALE, zoomLevelCropped);
						panelSplit2.setVisible(true);
						
						if (!isCropped)
						{
							
							
							scrollPaneCropped = new JScrollPane(componentImageCropped);
							scrollPaneCropped.getVerticalScrollBar().setMaximum( imgCropped.getHeight() );
							scrollPaneCropped.getVerticalScrollBar().setUnitIncrement( (int)(imgCropped.getHeight()/(double)SCROLLBAR_INC) );
							scrollPaneCropped.getHorizontalScrollBar().setMaximum( imgCropped.getWidth() );
							scrollPaneCropped.getHorizontalScrollBar().setUnitIncrement( (int)(imgCropped.getWidth()/(double)SCROLLBAR_INC) );
						
						
							panelSplit2.add(scrollPaneCropped, BorderLayout.CENTER);
							isCropped = true;
						}
						
						splitPane.setDividerLocation( (int) Math.max(content.getSize().getHeight(), imgCropped.getHeight() + 20) );
						
						scrollPaneCropped.revalidate();
						
						double spHeight = Math.max(100, splitPane.getHeight()-50);
						double spWidth = Math.max(100, splitPane.getWidth()-25);
						double scaleHeight = spHeight / (double)imgCropped.getHeight();
						double scaleWidth = spWidth / (double)imgCropped.getWidth();
						
						//auto-scale window so that entire image can be seen
						//zoomLevelCropped = (float)Math.min(scaleHeight, scaleWidth);
						zoomLevelCropped = (float)Math.min(scaleWidth, scaleHeight);
						
						System.err.println("(Auto) Zoom level cropped: " + zoomLevelCropped);
						
						componentImageCropped.setScaleType(JComponentDisplay.SCALE_MODE_PRESCALE, zoomLevelCropped);
						imgDisplay = PlanarImageEdit.applyMask(imgCropped, imgMatBinary);
						componentImageCropped.set(imgDisplay);
						
						scrollPaneCropped.revalidate();
						
						// --- for now
						System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
						imgMatBinaryOrg = null;
						imgMatGrayscaleOrg = null;
						imgMatGrayscaleTemplate = null;
						runTime.gc();
						System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
						
						
						
						dialogProgress.getPBInterim().setValue(38*modifierContourPB);
					}
					
					//frame.setCancelled(true);
					
					
					if (!frame.getCancelled())
					{
					
						//fetch borders, calculate distance measures between border pixels and sort them accordingly
						dialogProgress.getLabelCurrent().setText("Identifying and rearranging border pixels...");
						Vector[] vecSegObjBordersArr = GrayscaleImageEdit.fetchSegObjCoordBorder(imgSegCropped, false, true, frame, dialogProgress.getPBCurrent() );
						if (vecSegObjBordersArr != null)
						{
							vecSegObjBorders = vecSegObjBordersArr[0]; //border points
							vecSegObjBorderBP = vecSegObjBordersArr[1]; //break points, for irregular perimeters
							vecSegObjBorderBPInner = vecSegObjBordersArr[2]; //break points for inner borders (cavities)
						}
						
						//re-create vector of the pixels forming each object
						vecSegObjs = GrayscaleImageEdit.intMatrixToVectorOfPoints(imgSegCropped);
						vecSegObjCenters = GrayscaleImageEdit.findObjectCentroids(vecSegObjs);
						
						/*
						for (int i = 0; i < vecSegObjBorderBP.size(); i++)
						{
							Vector vecTemp = (Vector)vecSegObjBorderBP.get(i);
							System.err.print("Object " + (i+1) + " has " + vecTemp.size() + " break points at (");
							for (int j = 0; j < vecTemp.size(); j++)
							{
								System.err.print( ( (Integer)vecTemp.get(j))  + ",");
							}
							System.err.println(")");
						}
						*/
						
						dialogProgress.getPBInterim().setValue(40*modifierContourPB);
					}
					

					
					if (!frame.getCancelled())
					{
					
						//fill in any cavities in the objects, to get an additional measurement of the perimeter/area
						dialogProgress.getLabelCurrent().setText("Filling in any cavities in the objects...");
						vecSegObjNoCavities = GrayscaleImageEdit.fillObjectCavities(vecSegObjs, vecSegObjBorders, vecSegObjBorderBPInner, imgSegCropped,  dialogProgress.getPBCurrent() );
						
						//also shorten the border, so that the short version only contains the outer border
						vecSegObjBordersShort = GrayscaleImageEdit.shortenBorder(vecSegObjBorders, vecSegObjBorderBPInner);
						
						int numLandmarks = ( (SpinnerNumberModel)spinnerNumLandmarks.getModel()).getNumber().intValue();
						int maxAllowedLength = GrayscaleImageEdit.getMaxBorderLandmarks(vecSegObjBordersShort);
						numLandmarks = (int)Math.min(numLandmarks, maxAllowedLength);
						vecSegObjBordersShortLandmarks = GrayscaleImageEdit.getBorderLandmarks(vecSegObjBordersShort, numLandmarks);
						
						
						dialogProgress.getPBInterim().setValue(45*modifierContourPB);
						
						//componentImage.set(imgDisplay);
						//componentImage.repaint();
						
						//frame.setCancelled(true);
					}
							
					
					
					
					
					if (!frame.getCancelled())
					{
						//update the segmentation image with only the 'good' objects
						
						/*
						dialogProgress.getLabelCurrent().setText("Displaying result...");
						GrayscaleImageEdit.paintBinaryMatrix(vecSegObjs, imgMatGrayscale, 255);
						
					
							
						
						index = 0;
						for (int h = 0; h < imgOrg.getHeight(); h++)
							for (int w = 0; w < imgOrg.getWidth(); w++)
							{
								//bluePixels[ index++ ] = imgMatGrayscale[h][w];
								
								imgMatBinary[h][w] = (imgMatGrayscale[h][w] != 0) ? (byte)1 : (byte)0;
								
								//bluePixelGrayscale[ index-1] = (imgMatBinary[h][w] != 0) ? (int)imgSeg[h][w]*(int)(255.0/numGoodObj) : (int)0;
								//System.err.print( bluePixels[ index - 1] + "," );
							}
						*/
						
						/*
						for (int i = 0; i < bluePixels.length; i++)
								if (bluePixels[i] <= quantileBlue)
									bluePixels[i] = 0;
								else
									bluePixels[i] = 255;
						*/
						
						//wrGrayscale.setPixels(0,0,imgOrg.getWidth(),imgOrg.getHeight(),bluePixelGrayscale);
						//imgGrayscale.setData(wrGrayscale);
						
						
						//System.err.println("Done.");
						dialogProgress.getPBInterim().setValue(50*modifierContourPB);
						dialogProgress.getPBCurrent().setValue(0);
						
					
						GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgDisplay.getGraphics());
						
						
						Vector[] vecHorizVertLinesObj = GrayscaleImageEdit.fetchHorizVertLines(imgSegCropped, vecSegObjs, vecSegObjBordersShort,
							settings.getForceOrtho(),  settings.getForceHorizVert());
							
						vecHorizVertLines = vecHorizVertLinesObj[0];
						vecIntersectPoints = vecHorizVertLinesObj[1];
						
						
						//GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgDisplay.getGraphics());
						
						//GrayscaleImageEdit.paintSegmentationResults(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints, imgDisplay.getGraphics());
						
						componentImageCropped.setVectorOfLines(vecHorizVertLines);
						componentImageCropped.setBorderLandmarks(vecSegObjBordersShortLandmarks);
						componentImageCropped.set(imgDisplay);
						componentImageCropped.repaint();
						
						//frame.setCancelled(true); //for now
						
						////// Find contour here
						
						//System.err.println(vecSegObjBorders.size() + " border pixels will be evaluated...");
						/*
						if (!frame.getCancelled())
						{
							dialogProgress.getLabelCurrent().setText("Calculating distance measures...");
							Vector distMats = GrayscaleImageEdit.calcDistanceMatrix(vecSegObjBorders, GrayscaleImageEdit.DIST_EUCLIDEAN);
							dialogProgress.getPBInterim().setValue(55);
						}
						*/
						
						if (runContourID)
						{
							
							
							int[][] imgSegCroppedNoBorders = new int[1][1]; //just to obey the java compiler...
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Creating mask w/o border pixels...");
								imgSegCroppedNoBorders = GrayscaleImageEdit.removeBorderPixels(vecSegObjBordersShort, imgSegCropped);
								dialogProgress.getPBInterim().setValue(60);
							}
							
							System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							runTime.gc();
							System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
						
							
							try
							{
								if (!frame.getCancelled())
								{
								
								
									dialogProgress.getLabelCurrent().setText("Finding serration connection points...");
									// vecContours is a global obj. and can be used by other function
									Vector[] vecContourHotspotConnectionsArr = GrayscaleImageEdit.findContourHotspotsNarrow(vecSegObjBordersShort, imgSegCroppedNoBorders, settings.getPixelContourThresh() );
									vecContourHotspotConnections = vecContourHotspotConnectionsArr[0];
									vecContourHotspotIndices = vecContourHotspotConnectionsArr[1];
									
									
									
									dialogProgress.getPBInterim().setValue(80);
								}
							} catch (Throwable t)
							{
								frame.setCancelled(true);
								t.printStackTrace();
								JOptionPane.showMessageDialog(frame, "Serration identification failed. This is potentially due to the utilized threshold value.\n" +
									"Please adjust the serration threshold value and try again.",
									"Serration identification failed", JOptionPane.ERROR_MESSAGE);
							}
							
							System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							runTime.gc();
							System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
							
							
							//int[][] contourComplete = § int[1][1];  //just to obey the java compiler
							if (!frame.getCancelled())
							{
								//GrayscaleImageEdit.findMinimalContour(vecSegObjBorders, connectMats, distMats, 0, -1, imgDisplay.getGraphics() );
								dialogProgress.getLabelCurrent().setText("Tracing complete contour...");
								contourComplete = GrayscaleImageEdit.traceContour(imgSegCropped, vecSegObjBordersShort, vecContourHotspotConnections, vecContourHotspotIndices);
								
								//Vector connectMats = GrayscaleImageEdit.findConnectablePixels(vecSegObjBordersShort, imgSegCroppedNoBorders, frame, dialogProgress.getPBCurrent());
								//contourComplete = GrayscaleImageEdit.findContour(imgSegCropped, vecSegObjBordersShort, connectMats);
								
								//double maxVal = MiscMath.max(contourComplete);
								//double minVal = MiscMath.min(contourComplete);
								//System.err.println("Complete contour is locked within " + minVal + " and " + maxVal + ".");	
								
								//vecContourComplete = GrayscaleImageEdit.intMatrixToVectorOfPoints(contourComplete);
								dialogProgress.getPBInterim().setValue(85);
							}
							
							
							//int[][] contourUnique = new int[1][1];
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Calculating unique contour area...");
								contourUnique = GrayscaleImageEdit.matrixDifference(imgSegCroppedNoBorders, contourComplete);
								vecContourUnique = GrayscaleImageEdit.intMatrixToVectorOfPoints(contourUnique);
								//PlanarImageEdit.paintIntegerMatrix(contourComplete, colors1, imgDisplay.getGraphics() );
								
								//display result
								
								//PlanarImageEdit.paintIntegerMatrix(contourComplete, Color.PINK, imgDisplay.getGraphics() );
								//GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgDisplay.getGraphics());
								PlanarImageEdit.paintIntegerMatrix(contourUnique, Color.BLUE, imgDisplay.getGraphics() );
								//PlanarImageEdit.paintVector(vecSegObjs, Color.GREEN, imgDisplay.getGraphics() );
								PlanarImageEdit.paintVector(vecSegObjBordersShort, Color.YELLOW, imgDisplay.getGraphics() );
								GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgDisplay.getGraphics(), Color.RED, false);
								//GrayscaleImageEdit.paintContourHotspotsCrosses(vecSegObjBordersShortLandmarks, imgDisplay.getGraphics(), Color.WHITE, 2);
							
								dialogProgress.getPBInterim().setValue(90);
							}
							
							
							
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Finding indent depths...");
								// vecContours is a global obj. and can be used by other function
								vecContourIndents = GrayscaleImageEdit.fetchIndentDepths(vecContourHotspotConnections, vecContourHotspotIndices, vecSegObjBordersShort, contourUnique, dialogProgress.getPBCurrent());
								GrayscaleImageEdit.paintContourIndentDepths(vecContourIndents, imgDisplay.getGraphics(), Color.ORANGE );
								
								dialogProgress.getPBInterim().setValue(95);
							}
							
							
							System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							contourComplete = null; //no need for this variable anymore
							imgSegCroppedNoBorders = null;
							runTime.gc();
							System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
							
							/*
							Vector connectMats = new Vector(1); //just to obey the java compiler
							if (!frame.getCancelled())
							{
								try
								{
									dialogProgress.getLabelCurrent().setText("Calculating connectable pixels for all objects...");
									//Vector connectMats = GrayscaleImageEdit.findConnectablePixels(vecSegObjBorders, imgSegCroppedNoBorders);
									connectMats = GrayscaleImageEdit.findConnectablePixels(vecSegObjBordersShort, imgSegCroppedNoBorders, frame, dialogProgress.getPBCurrent());
									dialogProgress.getPBInterim().setValue(65);
								} catch (Throwable t)
								{
									frame.setCancelled(true);
									t.printStackTrace();
									JOptionPane.showMessageDialog(frame, "Failed to connectable pixels for all objects.\n"+
										"This is most likely due to a memory problem, which may happen when the objects have\n" +
										"highly irregular surfaces. Adjust the memory options to the Java virtual machine and try again.",
										"Failed to find connectable pixels", JOptionPane.ERROR_MESSAGE);
								}
							}
							
							//int[][] contourComplete = new int[1][1];  //just to obey the java compiler
							if (!frame.getCancelled())
							{
								//GrayscaleImageEdit.findMinimalContour(vecSegObjBorders, connectMats, distMats, 0, -1, imgDisplay.getGraphics() );
								dialogProgress.getLabelCurrent().setText("Detecting complete contour...");
								contourComplete = GrayscaleImageEdit.findContour(imgSegCropped, vecSegObjBordersShort, connectMats);
								
								//double maxVal = MiscMath.max(contourComplete);
								//double minVal = MiscMath.min(contourComplete);
								//System.err.println("Complete contour is locked within " + minVal + " and " + maxVal + ".");	
								
								//vecContourComplete = GrayscaleImageEdit.intMatrixToVectorOfPoints(contourComplete);
								dialogProgress.getPBInterim().setValue(70);
							}
							
							//Vector contourBordersVec = new Vector(1);
							//int[][] contourBorders = new int[1][1];
							Vector vecContourBorders = new Vector(1);
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Calculating contour borders (using 4-connectivity)..");
								Vector[] vecContourBorderArr = GrayscaleImageEdit.fetchSegObjCoordBorder(contourComplete, false, false, null, null);
								if (vecContourBorderArr != null)
								{
									vecContourBorders = vecContourBorderArr[0]; //border points, index [1] contaisn break points, not of interest here
									//contourBorders = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecContourBorder, imgSeg[0].length, imgSeg.length);
								}
								dialogProgress.getPBInterim().setValue(72);
							}
							
							
							/*
							int[][] borderMat = new int[1][1];
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Calculating overlap between unique contour and original borders...");
								borderMat = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjBordersShort, imgSeg[0].length, imgSeg.length);
								//byte[][] intersectBorderContour = GrayscaleImageEdit.matrixIntersect(borderMat, contourBorders);
								dialogProgress.getPBInterim().setValue(77);
							}
							*/
								
							/*
							Vector vecContourHotspots = new Vector(1);
							//int[][] contourHotspots = new int[1][1];
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Identifying contour hotspots...");
								//vecContourHotspots = GrayscaleImageEdit.matrixIntersectAsVector(borderMat, contourBorders);
								vecContourHotspots = GrayscaleImageEdit.vectorIntersectAsVector(vecSegObjBordersShort, vecContourBorders);
								//contourHotspots = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecContourHotspots, imgSeg[0].length, imgSeg.length);
								//PlanarImageEdit.paintIntegerMatrix(borderMat, Color.YELLOW, imgDisplay.getGraphics() );
								PlanarImageEdit.paintVector(vecSegObjBordersShort, Color.YELLOW, imgDisplay.getGraphics() );
								//PlanarImageEdit.paintIntegerMatrix(intersectBorderContour, Color.RED, imgDisplay.getGraphics() );
							
								dialogProgress.getPBInterim().setValue(80);
							}
							
							
							Vector vecContourHotspotsMerged = new Vector(1);
							int[][] contourHotspotsMerged = new int[1][1];
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Merging contour hotspots...");
								vecContourHotspotsMerged = GrayscaleImageEdit.mergeContourHotspots(vecContourHotspots, vecSegObjBordersShort, 3*Math.sqrt(2), 0);
								contourHotspotsMerged = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecContourHotspotsMerged, imgSegCropped[0].length, imgSegCropped.length);
								PlanarImageEdit.paintIntegerMatrix(contourHotspotsMerged, Color.PINK, imgDisplay.getGraphics() );
								//PlanarImageEdit.paintIntegerMatrix(contourHotspots, colors3, imgDisplay.getGraphics() );
								
								dialogProgress.getPBInterim().setValue(82);
							}
						
							//Vector vecContourGroups = new Vector(1);
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Identifying contour hotspot groups...");
								// vecContours is a global obj. and can be used by other function
								vecContours = GrayscaleImageEdit.identifyContourGroups(vecContourHotspotsMerged, contourHotspotsMerged, true);
								
								dialogProgress.getPBInterim().setValue(85);
							}
							
							if (!frame.getCancelled())
							{
								dialogProgress.getLabelCurrent().setText("Identifying contour hotspot connection points...");
								// vecContours is a global obj. and can be used by other function
								vecContourHotspotConnections = GrayscaleImageEdit.fetchContourHotspotConnections(vecContours, contourUnique);
								GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgDisplay.getGraphics(), Color.RED);
								
								dialogProgress.getPBInterim().setValue(90);
							}
							
							
							*/
							
							
							
							
							
							
							//done!
							if (!frame.getCancelled())
							{
								dialogProgress.getPBInterim().setValue(100);
								dialogProgress.getLabelCurrent().setText("Done.");
							
								componentImageCropped.set(imgDisplay);
								componentImageCropped.revalidate();
							}
						} //if runContourID
						

						
						buttonCrop.setEnabled(true);
						updateSaveButtons(true);
						
						buttonCalcStats.setEnabled(true);
						buttonRevert.setEnabled(true);
						buttonApplyFilterContour.setEnabled(false);
						buttonApplyNoContour.setEnabled(false);
						
						/*
						if (!frame.getCancelled())
						{
							//update the segmentation image with only the 'good' objects
							dialogProgress.getPBInterim().setValue(95);
							dialogProgress.getLabelCurrent().setText("Calculating statistics...");
					
							GrayscaleImageEdit.calcSegStats(vecSegObjs, vecSegObjBorders, vecHorizVertLines, vecIntersectPoints,
								1);
								
							
					
						}
						*/
						
					}
					
					//imgDisplayBackup.setData(imgDisplay.getData());
					dialogProgress.getLabelCurrent().setText("Done");
					
					
					// send runnable to the Swing thread
					// the runnable is queued after the
					// results are returned
					SwingUtilities.invokeLater
					(
						new Runnable()
						{
							public void run()
							{
								buttonReanalyze.setEnabled(true);
								
								dialogProgress.dispose();
								dialogProgress.setVisible(false);

								frame.setRunning(false);
								frame.setEnabled(true);
								frame.toFront();
							}
						}
					);
				}

			}.start();
						
		} else if (e.getSource() == buttonSaveImageCropped)
		{
			System.err.println("Save click...");

			if (currentFilename.length() > 0)
			{
				//File dir = new File (".");
				//if (lastOutDir != null)
				//	dir = lastOutDir;
					
				JFileChooser jfc = new JFileChooser();
				//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
				jfc.setFileFilter(new FileFilterImage() );
				jfc.setCurrentDirectory(lastOutDir);
				jfc.setMultiSelectionEnabled(false);
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				jfc.setDialogType(JFileChooser.SAVE_DIALOG);
				
				//create a new filename
				String str = currentFile.getName();
				String[] parts = str.split("\\.");
				String newStr = new String(parts[0]);
				for (int i = 1; i < (parts.length-1); i++)
					newStr = newStr  + "." + parts[i];
				String ext = parts[parts.length-1];
				
				String fName = lastOutDir.getAbsolutePath() + "/" + newStr + "_" + APPL_NAME + OUTPUT_FNAME_CROPPED + "." + ext;
				System.err.println(fName);
				File fileSave = new File(fName);
				
				jfc.setSelectedFile(fileSave);
				
				int retVal = jfc.showDialog(frame, "Save image");
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File file = jfc.getSelectedFile();
					lastOutDir = file.getParentFile();
					String filename = new String(file.getAbsolutePath() );

					boolean doSave = true;
					if (file.isFile())
					{
						int retCode = JOptionPane.showConfirmDialog(frame,
							"Overwrite file " + file.getName() + "?",
							"Overwrite confirmation",
							JOptionPane.YES_NO_OPTION);
						
						if (retCode != JOptionPane.YES_OPTION)
						{
							doSave = false;
							System.err.println("File saving aborted");
						} else
						{
							lastOutDir = file.getParentFile();
						}
						
					}
					
					if (doSave)
					{

						System.err.print("Saving " + filename + "...");
					
						//JPEGEncodeParam params = new JPEGEncodeParam();
						//params.setQuality(1.0f);
						
						
						
						String codecId = ext.toUpperCase();
						if (codecId.equals("TIF"))
							codecId = "TIFF";
						else if (codecId.equals("JPG"))
							codecId = "JPEG";
						
						BufferedImage imgCroppedCopy = new BufferedImage(imgCropped.getWidth(), imgCropped.getHeight(), imgCropped.getType() );
						imgCroppedCopy.setData( imgCropped.getData() );
						imgCroppedCopy = PlanarImageEdit.applyMask(imgCroppedCopy, imgMatBinary);
						GrayscaleImageEdit.paintSegmentationResults(vecSegObjs, vecSegObjNoCavities, vecSegObjCenters, vecSegObjBorders, vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints, imgCroppedCopy.getGraphics());
						//GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgCroppedCopy.getGraphics());
						
						if (contourUnique != null && vecContourHotspotConnections != null && vecContourIndents != null)
						{
							PlanarImageEdit.paintIntegerMatrix(contourUnique, Color.BLUE, imgCroppedCopy.getGraphics() );
							//PlanarImageEdit.paintVector(vecSegObjs, Color.GREEN, imgDisplay.getGraphics() );
							//PlanarImageEdit.paintVector(vecSegObjBordersShort, Color.YELLOW, imgCroppedCopy.getGraphics() );
							GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgCroppedCopy.getGraphics(), Color.RED, false);
							GrayscaleImageEdit.paintContourIndentDepths(vecContourIndents, imgCroppedCopy.getGraphics(), Color.ORANGE );
						}
						
						GrayscaleImageEdit.paintBorders(vecSegObjBordersShort, Color.YELLOW, imgCroppedCopy.getGraphics());
						GrayscaleImageEdit.paintObjectIds(vecSegObjCenters, imgCroppedCopy.getGraphics());
						
						
						JAI.create("filestore", imgCroppedCopy, filename, codecId);
				
						System.err.println("done");
					}
				}
				
				
			}
			
		} else if (e.getSource() == buttonSaveImage)
		{
			System.err.println("Save (cropped) click...");

			if (currentFilename.length() > 0)
			{
				File dir = new File (".");
				if (lastOutDir != null)
					dir = lastOutDir;
					
				JFileChooser jfc = new JFileChooser();
				//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
				jfc.setFileFilter(new FileFilterImage() );
				jfc.setCurrentDirectory(dir);
				jfc.setMultiSelectionEnabled(false);
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				jfc.setDialogType(JFileChooser.SAVE_DIALOG);
				
				//create a new filename
				String str = currentFile.getName();
				String[] parts = str.split("\\.");
				String newStr = new String(parts[0]);
				for (int i = 1; i < (parts.length-1); i++)
					newStr = newStr  + "." + parts[i];
				String ext = parts[parts.length-1];
				
				String fName = lastOutDir.getAbsolutePath() + "/" + newStr + "_" + APPL_NAME + OUTPUT_FNAME_SEG + "." + ext;
				System.err.println(fName);
				
				File fileCropped = new File(fName);
				jfc.setSelectedFile(fileCropped);
				
				int retVal = jfc.showDialog(frame, "Save image");
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File file = jfc.getSelectedFile();
					
					String filename = new String(file.getAbsolutePath() );

					boolean doSave = true;
					if (file.isFile())
					{
						int retCode = JOptionPane.showConfirmDialog(frame,
							"Overwrite file " + file.getName() + "?",
							"Overwrite confirmation",
							JOptionPane.YES_NO_OPTION);
						
						if (retCode != JOptionPane.YES_OPTION)
						{
							doSave = false;
							System.err.println("File saving aborted");
						} else
						{
							lastOutDir = file.getParentFile();
						}
						
					}
					
					if (doSave)
					{

						System.err.print("Saving " + filename + "...");
					
						//JPEGEncodeParam params = new JPEGEncodeParam();
						//params.setQuality(1.0f);
						
						String codecId = ext.toUpperCase();
						if (codecId.equals("TIF"))
							codecId = "TIFF";
						else if (codecId.equals("JPG"))
							codecId = "JPEG";
						
						
						JAI.create("filestore", componentImage.get(), filename, codecId);
				
						System.err.println("done");
					}
				}
				
				
			}
				
			
		} else if (e.getSource() == buttonZoomIn && componentImage.isSet())
		{
			
			if (zoomLevel < ZOOM_MAX)
			{
				//int sbHorizontalValue = scrollPane.getHorizontalScrollBar().getValue();
				//int sbVerticalValue = scrollPane.getVerticalScrollBar().getValue();
				
				//System.err.println("Scrollbars before: " + sbHorizontalValue + "::" + sbVerticalValue);
				
				zoomLevelNew = zoomLevel/ZOOM_INC;
				
				//sbHorizontalValue *= ZOOM_INC;
				//sbVerticalValue *= ZOOM_INC;
			
				System.err.println("Zooming to " + zoomLevel);
			
				//componentImage.setAutoScaleType(zoomLevel);
				//componentImage.setScaleFactor(zoomLevel);
			
				//componentImage.revalidate();
				
				//refreshImage(sbHorizontalValue, sbVerticalValue);
				
				//trigger new zoom event (from MASQOT-GUI)
				//String zoomLevelStr = new String( new Integer( (int)(100.0*(zoomLevel+ZOOM_INC))) + "%");
				//cbZoomLevel.getEditor().setItem( zoomLevelStr );
				
				zoomArea = null; //center point will be used
				this.actionPerformed( new ActionEvent(cbZoomLevel, 0, "") );
			
				//System.err.println("Scrollbars after: " + sbHorizontalValue + "::" + sbVerticalValue);
			}
		
		} else if (e.getSource() == buttonZoomOut && componentImage.isSet())
		{
			if (zoomLevel > ZOOM_MIN)
			{
				//int sbHorizontalValue = scrollPane.getHorizontalScrollBar().getValue();
				//int sbVerticalValue = scrollPane.getVerticalScrollBar().getValue();
			
				//System.err.println("Scrollbars before: " + sbHorizontalValue + "::" + sbVerticalValue);
				
				zoomLevelNew = zoomLevel*ZOOM_INC;
				
				//sbHorizontalValue /= ZOOM_INC;
				//sbVerticalValue /= ZOOM_INC;
				
				System.err.println("Zooming to " + zoomLevel);
				
				//componentImage.setAutoScaleType(zoomLevel);
				//componentImage.setScaleFactor(zoomLevel);
				zoomArea = null; //center point will be used
				this.actionPerformed( new ActionEvent(cbZoomLevel, 0, "") );
			
				//refreshImage(sbHorizontalValue, sbVerticalValue);
			}
			
			//System.err.println("Scrollbars after: " + scrollPane.getHorizontalScrollBar().getValue() + "::" + scrollPane.getVerticalScrollBar().getValue());
		
		} else if (e.getSource() == buttonZoomInCropped && componentImageCropped.isSet())
		{
			
			if (zoomLevelCropped < ZOOM_MAX)
			{
				//int sbHorizontalValue = scrollPane.getHorizontalScrollBar().getValue();
				//int sbVerticalValue = scrollPane.getVerticalScrollBar().getValue();
				
				//System.err.println("Scrollbars before: " + sbHorizontalValue + "::" + sbVerticalValue);
				
				zoomLevelCroppedNew = zoomLevelCropped/ZOOM_INC;
				
				//sbHorizontalValue *= ZOOM_INC;
				//sbVerticalValue *= ZOOM_INC;
			
				System.err.println("(crop) Zooming to " + zoomLevelCropped);
			
				//componentImage.setAutoScaleType(zoomLevel);
			
				//componentImage.revalidate();
				
				//refreshImage(sbHorizontalValue, sbVerticalValue);
				
				//trigger new zoom event (from MASQOT-GUI)
				//String zoomLevelStr = new String( new Integer( (int)(100.0*(zoomLevel+ZOOM_INC))) + "%");
				//cbZoomLevel.getEditor().setItem( zoomLevelStr );
				
				zoomAreaCropped = null; //center point will be used
				this.actionPerformed( new ActionEvent(cbZoomLevelCropped, 0, "") );
			
				//System.err.println("Scrollbars after: " + sbHorizontalValue + "::" + sbVerticalValue);
			}
		
		
		} else if (e.getSource() == buttonZoomOutCropped && componentImageCropped.isSet())
		{
			if (zoomLevelCropped > ZOOM_MIN)
			{
				//int sbHorizontalValue = scrollPane.getHorizontalScrollBar().getValue();
				//int sbVerticalValue = scrollPane.getVerticalScrollBar().getValue();
			
				//System.err.println("Scrollbars before: " + sbHorizontalValue + "::" + sbVerticalValue);
				
				zoomLevelCroppedNew = zoomLevelCropped*ZOOM_INC;
				
				//sbHorizontalValue /= ZOOM_INC;
				//sbVerticalValue /= ZOOM_INC;
				
				System.err.println("(crop) Zooming to " + zoomLevelCropped);
				
				//componentImage.setAutoScaleType(zoomLevel);
				zoomAreaCropped = null; //center point will be used
				this.actionPerformed( new ActionEvent(cbZoomLevelCropped, 0, "") );
			
				//refreshImage(sbHorizontalValue, sbVerticalValue);
			}
			
			//System.err.println("Scrollbars after: " + scrollPane.getHorizontalScrollBar().getValue() + "::" + scrollPane.getVerticalScrollBar().getValue());
		
		
		} else if (e.getSource() == buttonCalcStats)
		{
			Vector objStats = GrayscaleImageEdit.calcSegStats(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBordersShort,
				vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints,
				vecContourHotspotConnections, vecContourHotspotIndices, vecContourIndents,
				1/( (SpinnerNumberModel)spinnerScaleParam.getModel()).getNumber().doubleValue(),
				vecSegObjBordersShortLandmarks, vecSegObjCentersOrg, vecSegObjCenters,
				imgDisplay.getGraphics() );
				
			//create a new filename
			String str = currentFile.getName();
			String[] parts = str.split("\\.");
			String newStr = new String(parts[0]);
			for (int i = 1; i < (parts.length-1); i++)
				newStr = newStr  + "." + parts[i];
			String ext = parts[parts.length-1];
			
			String fName = lastOutDir.getAbsolutePath() + "/" + newStr + "_results.txt";
			System.err.println(fName);
			File fileSave = new File(fName);
			
			
			
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			//jfc.setFileFilter(new FileFilterGrid() );
			//jfc.setCurrentDirectory(currentFile.getParent().getAbsolutePath());
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			//jfc.setSelectedFile( new File(filenameOrg) );
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			jfc.setSelectedFile(fileSave);


			int retVal = jfc.showDialog(frame, "Save results");
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				File file = jfc.getSelectedFile();
				String filename = new String(file.getAbsolutePath() );

			
				boolean doSave = true;
				if (file.isFile())
				{
					int retCode = JOptionPane.showConfirmDialog(frame,
						"Overwrite file " + file.getName() + "?",
						"Overwrite confirmation",
						JOptionPane.YES_NO_OPTION);

					if (retCode != JOptionPane.YES_OPTION)
					{
						doSave = false;
						//System.err.println("File saving aborted");
					}

				}

				if (doSave)
				{
				
					lastOutDir = file.getParentFile();
				
					//output something here
					boolean foundError = false;
					
					String token = "\t";
					try
					{
			            FileWriter fw = new FileWriter(filename);
						BufferedWriter bw = new BufferedWriter(fw);

						Vector fileHeader = (Vector)objStats.get(0);
						
						//dump the file header first ..
						for (int j = 0; j < fileHeader.size(); j++)
						{
							//String str = Misc.quoteAlphaNumeric((String)fileHeader.get(i));
							//String str = (String)fileHeader.get(i);
							bw.write( "\"" + (String)fileHeader.get(j) + "\"" );
							if (j < (fileHeader.size()-1))
								bw.write(token, 0, token.length());
						}
						bw.newLine();

						// .. and then the data
						for (int i = 1; i < objStats.size(); i++)
						{
							Vector row = (Vector)objStats.get(i);

							for (int j = 0; j < row.size(); j++)
							{
								//bw.write(Misc.quoteAlphaNumeric( (String)row.get(j)));
								bw.write( ((Number)row.get(j)).toString() );
								if (j < (row.size()-1))
									bw.write(token, 0, token.length());
							}
							bw.newLine();
						}
						bw.flush();
						bw.close();


					} catch (IOException ex)
					{
						// catch possible io errors
						foundError = true;
						System.out.println("Error writing file '" + filename + "'!");
						JOptionPane.showMessageDialog(frame, "Unable to save the output file.\n"+
							"This is potentially caused by some other application using the file.\n" +
							"Please make sure that the specified filename is writable and try again.",
							"Unable to write output file", JOptionPane.ERROR_MESSAGE);
						
					}
				}
			}
				
		} else if (e.getSource() == buttonCrop)
		{
							
			if (isLoaded)
			{
				panelSplit2.setVisible(true);
				
				Vector vec = new Vector( vecSegObjBorders.size() );
				for (int i = 0; i < vecSegObjBorders.size(); i++)
				{
					Vector vecCurr = (Vector)vecSegObjBorders.get(i);
					
					//find the most extreme point
					Point p = (Point)vecCurr.get(0);
					int maxX = Integer.MIN_VALUE;
					int minX = Integer.MAX_VALUE;
					int maxY = Integer.MIN_VALUE;
					int minY = Integer.MAX_VALUE;
					int xScaled, yScaled;
					
					for (int j = 0; j < vecCurr.size(); j++)
					{
						p = (Point)vecCurr.get(j);
						//xScaled = (int)(p.getX()/zoomLevel);
						//yScaled = (int)(p.getY()/zoomLevel);
						xScaled = (int)(p.getX());
						yScaled = (int)(p.getY());
						
						if ( xScaled > maxX )
							maxX = xScaled;
						if ( xScaled < minX )
							minX = xScaled;
						if ( yScaled > maxY )
							maxY = yScaled;
						if ( yScaled < minY )
							minY = yScaled;
					}
					
					
					// create a rectangle spanning this area
					Rectangle rect = new Rectangle(minX - this.CROP_PADDING, minY-this.CROP_PADDING,
						maxX-minX+2*this.CROP_PADDING, maxY-minY+2*this.CROP_PADDING);
					vec.add(rect);
					
					
				}
				
				//first time, use the zoom level from the main image
				if (zoomLevelCropped < 0)
					zoomLevelCropped = zoomLevel;
				
				//copy the original image and add some extra info
				BufferedImage imgDisplayCopy = new BufferedImage(imgDisplay.getWidth(), imgDisplay.getHeight(), imgDisplay.getType() );
				imgDisplayCopy.setData( imgDisplay.getData() );
				GrayscaleImageEdit.paintSegmentationResults(vecSegObjs, vecSegObjNoCavities, vecSegObjCenters, vecSegObjBorders, vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints, imgDisplayCopy.getGraphics());
				
				imgCropped = PlanarImageEdit.cropImages(imgDisplayCopy, vec, 1.0);
				componentImageCropped.set(imgCropped);
				componentImageCropped.setAutoScaleType(zoomLevelCropped);
				
				
				if (!isCropped)
				{
					scrollPaneCropped = new JScrollPane(componentImageCropped);
					scrollPaneCropped.getVerticalScrollBar().setMaximum( imgCropped.getHeight() + 50 );
					scrollPaneCropped.getVerticalScrollBar().setUnitIncrement( (int)(imgCropped.getHeight()/(double)SCROLLBAR_INC) );
					scrollPaneCropped.getHorizontalScrollBar().setMaximum( imgCropped.getWidth() );
					scrollPaneCropped.getHorizontalScrollBar().setUnitIncrement( (int)(imgCropped.getWidth()/(double)SCROLLBAR_INC) );
				
					panelSplit2.add(scrollPaneCropped, BorderLayout.CENTER);
					isCropped = true;
				
					splitPane.setDividerLocation( (int) (frame.getSize().getHeight() - imgCropped.getHeight()) );
				}
				
				scrollPaneCropped.revalidate();
				
			}
				
			
		} else if (e.getSource() == cbZoomLevel)
		{

			
			
			//if ( cbZoomLevel.getSelectedIndex() != zoomLevelCurrentIndex ||
			//	!cbZoomLevel.getEditor().getItem().toString().equalsIgnoreCase(zoomLevelCurrentVal) )
			{

				if (componentImage != null && componentImage.isSet() )
				{


					//zoomLevelCurrentIndex = (byte)cbZoomLevel.getSelectedIndex();
					//zoomLevelCurrentVal = (String)cbZoomLevel.getEditor().getItem();
					//String valString = new String(zoomLevelCurrentVal);
					//valString = valString.replaceAll("([^0-9\\.]+)", ""); //remove non-digits

					try
					{

						/*
						System.err.println("######### Zoom start ");
						System.err.println("Horiz scrollbar is now: " + scrollPane.getHorizontalScrollBar().getValue());
						System.err.println("Vertical scrollbar is now: " + scrollPane.getVerticalScrollBar().getValue());
						*/


						int sbHorizontalValue = 0;
						int sbVerticalValue = 0;

						//try to convert to a float value
						float zoomLevelOld = zoomLevel;
						zoomLevel = zoomLevelNew;
						float zoomLevelDelta = zoomLevel - zoomLevelOld;
						
						Rectangle vpRect = scrollPane.getViewport().getViewRect();
						
						//use center point if not specified
						if (zoomArea == null)
						{
							int x = scrollPane.getHorizontalScrollBar().getValue();
							int y = scrollPane.getVerticalScrollBar().getValue();
							int xMax = (int)scrollPane.getHorizontalScrollBar().getMaximum();
							int yMax = (int)scrollPane.getVerticalScrollBar().getMaximum();

							//get the current width in order to locate the new center
							
							int w = (int)vpRect.getWidth();
							int h = (int)vpRect.getHeight();

							//actual width in the image is determined by the zoomLevel
							double wOrg = (double)w / zoomLevelOld;
							double hOrg = (double)h / zoomLevelOld;

							//calculate original x/y position
							double xOrg = (double)x / zoomLevelOld;
							double yOrg = (double)y / zoomLevelOld;


							double xCenterOrg = (xOrg + wOrg/2.0);
							double yCenterOrg = (yOrg + hOrg/2.0);

							zoomArea = new Rectangle2D.Double( xCenterOrg, yCenterOrg, wOrg, hOrg );
						}

						double xNew = zoomArea.getX()*zoomLevel - (zoomArea.getWidth()*zoomLevel) / 2.0 + ( (zoomArea.getWidth()*zoomLevelDelta)/2.0);
						double yNew = zoomArea.getY()*zoomLevel - (zoomArea.getHeight()*zoomLevel) / 2.0 + ( (zoomArea.getHeight()*zoomLevelDelta)/2.0);

						//System.err.println("Xnew: " + xNew + " (" + (vpRect.getWidth()*zoomLevel) + ")" );
						//System.err.println("Ynew: " + yNew + " (" + (vpRect.getHeight()*zoomLevel) + ")" );
						sbHorizontalValue = (int)Math.round(xNew);
						sbVerticalValue = (int)Math.round(yNew);

						refreshImage(componentImage, scrollPane, sbHorizontalValue, sbVerticalValue, zoomLevel);


					} catch (NumberFormatException ex)
					{

					}

					//hasZoomed = true;

					//System.err.println("###### Done with zoom");


				} else
				{
					//cbZoomLevel.setSelectedIndex(ZOOM_LEVEL_DEFAULT);
				}



			}
			frame.repaint();
			
		} else if (e.getSource() == cbZoomLevelCropped)
		{

			
			
			//if ( cbZoomLevel.getSelectedIndex() != zoomLevelCurrentIndex ||
			//	!cbZoomLevel.getEditor().getItem().toString().equalsIgnoreCase(zoomLevelCurrentVal) )
			{

				if (componentImageCropped != null && componentImageCropped.isSet() )
				{


					//zoomLevelCurrentIndex = (byte)cbZoomLevel.getSelectedIndex();
					//zoomLevelCurrentVal = (String)cbZoomLevel.getEditor().getItem();
					//String valString = new String(zoomLevelCurrentVal);
					//valString = valString.replaceAll("([^0-9\\.]+)", ""); //remove non-digits

					try
					{

						
						//System.err.println("######### Zoom start ");
						//System.err.println("Horiz scrollbar is now: " + scrollPaneCropped.getHorizontalScrollBar().getValue());
						//System.err.println("Vertical scrollbar is now: " + scrollPaneCropped.getVerticalScrollBar().getValue());
						


						int sbHorizontalValue = 0;
						int sbVerticalValue = 0;

						//try to convert to a float value
						float zoomLevelOld = zoomLevel;
						zoomLevelCropped = zoomLevelCroppedNew;
						float zoomLevelDelta = zoomLevelCropped - zoomLevelOld;
						
						Rectangle vpRect = scrollPaneCropped.getViewport().getViewRect();
						
						//use center point if not specified
						if (zoomAreaCropped == null)
						{
							int x = scrollPaneCropped.getHorizontalScrollBar().getValue();
							int y = scrollPaneCropped.getVerticalScrollBar().getValue();
							int xMax = (int)scrollPaneCropped.getHorizontalScrollBar().getMaximum();
							int yMax = (int)scrollPaneCropped.getVerticalScrollBar().getMaximum();

							//get the current width in order to locate the new center
							
							int w = (int)vpRect.getWidth();
							int h = (int)vpRect.getHeight();

							//actual width in the image is determined by the zoomLevel
							double wOrg = (double)w / zoomLevelOld;
							double hOrg = (double)h / zoomLevelOld;

							//calculate original x/y position
							double xOrg = (double)x / zoomLevelOld;
							double yOrg = (double)y / zoomLevelOld;


							double xCenterOrg = (xOrg + wOrg/2.0);
							double yCenterOrg = (yOrg + hOrg/2.0);

							zoomAreaCropped = new Rectangle2D.Double( xCenterOrg, yCenterOrg, wOrg, hOrg );
						}

						double xNew = zoomAreaCropped.getX()*zoomLevelCropped - (zoomAreaCropped.getWidth()*zoomLevelCropped) / 2.0 + ( (zoomAreaCropped.getWidth()*zoomLevelDelta)/2.0);
						double yNew = zoomAreaCropped.getY()*zoomLevelCropped - (zoomAreaCropped.getHeight()*zoomLevelCropped) / 2.0 + ( (zoomAreaCropped.getHeight()*zoomLevelDelta)/2.0);

						//System.err.println("Xnew: " + xNew + " (" + (vpRect.getWidth()*zoomLevel) + ")" );
						//System.err.println("Ynew: " + yNew + " (" + (vpRect.getHeight()*zoomLevel) + ")" );
						sbHorizontalValue = (int)Math.round(xNew);
						sbVerticalValue = (int)Math.round(yNew);

						refreshImage(componentImageCropped, scrollPaneCropped, sbHorizontalValue, sbVerticalValue, zoomLevelCropped);


					} catch (NumberFormatException ex)
					{
						System.err.println("Failed with (crop) zooming");
						ex.printStackTrace();
					}

					//hasZoomed = true;

					//System.err.println("###### Done with zoom");


				} else
				{
					//cbZoomLevel.setSelectedIndex(ZOOM_LEVEL_DEFAULT);
				}



			}
			frame.repaint();
		}
		
	}
	
	/**
	* Refreshes the current image (older version) and updates the scroll bars
	*
	* @param	sbHorizontalValue	The size of the horizontal scrollbar
	* @param	sbVerticalValue	The size of the vertical scrollbar
	*/
	protected void refreshImageOld(int sbHorizontalValue, int sbVerticalValue)
	{
		componentImage.setAutoScaleType(zoomLevel);
		
		//Dimension d = componentImage.getPreferredSize();
		//System.err.println("New preferred size: " + d);		
		
		
		//
		scrollPane.getHorizontalScrollBar().setValue( (int)Math.min( sbHorizontalValue, scrollPane.getHorizontalScrollBar().getMaximum() ) );
		scrollPane.getVerticalScrollBar().setValue( (int)Math.min( sbVerticalValue, scrollPane.getVerticalScrollBar().getMaximum() ) );
		
		componentImage.revalidate();
		componentImage.repaint();
		//panelCenter.revalidate();
		//scrollPane.revalidate();
	}

	/**
	* Repaints the current image and sets the scrollbars to new positions.
	*
	* @param	ci	The component used to display the image
	* @param	sp	The scrollpane on which the image is display
	* @param	sbHorizontalValue	the new location of the horizontal scrollbar
	* @param	sbVerticalValue	the new location of the vertical scrollbar
	* @param	zoomLevelCurr	The current zoom level
	*/
	protected void refreshImage(JComponentDisplay ci, JScrollPane sp, int sbHorizontalValue, int sbVerticalValue, float zoomLevelCurr)
	{
		//ci.setAutoScaleType(zoomLevelCurr);
		ci.setScaleType(JComponentDisplay.SCALE_MODE_PRESCALE, zoomLevelCurr);

		//scrollPane.getHorizontalScrollBar().setValue(sbHorizontalValue);
		//scrollPane.getVerticalScrollBar().setValue(sbVerticalValue);


		ci.revalidate();
		ci.repaint();

		//System.err.println("horizMax: " + horizMax);
		//System.err.println("horizExt: " + horizExt);
		//System.err.println("sbHorizontalValue: " + sbHorizontalValue);

		//scrollPane.getHorizontalScrollBar().setMaximum(horizMax);
		//scrollPane.getVerticalScrollBar().setMaximum(vertMax);
		//scrollPane.getHorizontalScrollBar().setVisibleAmount(horizExt);
		//scrollPane.getVerticalScrollBar().setVisibleAmount(horizExt);

		sp.getHorizontalScrollBar().setValue(sbHorizontalValue);
		sp.getVerticalScrollBar().setValue(sbVerticalValue);

		/*
		if ( (sbHorizontalValue + scrollPane.getHorizontalScrollBar().getVisibleAmount() ) > scrollPane.getHorizontalScrollBar().getMaximum() )
		{
			System.err.println("### Shifting...");
			scrollPane.getHorizontalScrollBar().setValue( scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount() - 1 );
		} else
		{


		}
		*/


		zoomHorizDelta = sbHorizontalValue - sp.getHorizontalScrollBar().getValue();
		zoomVertDelta = sbVerticalValue - sp.getVerticalScrollBar().getValue();
		zoomCount = 0;


		/*
		JScrollBar sbHoriz = scrollPane.getHorizontalScrollBar();
		JScrollBar sbHoriz2 = new JScrollBar( sbHoriz.getOrientation(),
			sbHorizontalValue,
			Math.min(horizExt, horizMax - sbHorizontalValue),
			0, horizMax );

		scrollPane.setHorizontalScrollBar(sbHoriz2);
		*/

		//System.err.println("preferred image size: " + componentImage.getPreferredSize());

		//scrollPane.syncScrollPaneWithViewport();

		//System.err.println("Horiz maximum is now: " + scrollPane.getHorizontalScrollBar().getMaximum());
		//System.err.println("Horiz scrollbar is now: " + scrollPane.getHorizontalScrollBar().getValue());
		//System.err.println("Vertical scrollbar is now: " + scrollPane.getVerticalScrollBar().getValue());


	}	
	

	/**
	* A function that extracts the filename from a full path
	* 
	* @param  fName		The full path
	* @return The extracted filename
	*/
	public static String extractFilename(String fName)
	{
		String filename;
			
		//support both Windows and Unix file path styles
		int pathEndWindows = fName.lastIndexOf('\\');
		int pathEndUnix = fName.lastIndexOf('/');
			
		//extract path and filename
		if (pathEndWindows != -1)
			filename = new String(fName.substring(pathEndWindows+1));
		else
			filename = new String(fName.substring(pathEndUnix+1));
			
		return(filename);
	}	

	
	/**
	* Invoked when the state is changed of some object.
	*
	* @param	e	The ChangeEvent
	*/
	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() == spinnerScaleParam)
		{
			if (settings != null)
			{
				settings.setScaleParam( ( (SpinnerNumberModel)spinnerScaleParam.getModel()).getNumber().doubleValue() );
				//System.err.println("Updating scale parameter to " + settings.getScaleParam() );
			}
			
			updateSaveButtons(true);
			
		} else if (e.getSource() == spinnerNumLandmarks)
		{
			int val = ( (SpinnerNumberModel)spinnerNumLandmarks.getModel()).getNumber().intValue();
			//System.err.println("Changed status of LandMarks spinner to " + val);
			
			if (settings != null)
			{
				settings.setNumLandmarks( val );
			}
			if (componentImageCropped != null && vecSegObjBordersShort != null && vecSegObjBordersShort.size() > 0)
			{
				//System.err.println("... now changing componentImageCropped...");
				updateSaveButtons(true);
				
				//make sure that we don't set this value higher than the length of the borders
				int maxAllowedLength = GrayscaleImageEdit.getMaxBorderLandmarks(vecSegObjBordersShort);
				int numLandmarks = (int)Math.min(val, maxAllowedLength);
				vecSegObjBordersShortLandmarks = GrayscaleImageEdit.getBorderLandmarks(vecSegObjBordersShort, numLandmarks);
				componentImageCropped.setBorderLandmarks(vecSegObjBordersShortLandmarks);
				componentImageCropped.repaint();
			}
		}
		
		/*
		if (e.getSource() == spinnerMetaCol)
		{
			System.err.println("spinnerMetaCol change to " + val);
		} else if (e.getSource() == spinnerMetaRow)
		{
			System.err.println("spinnerMetaRow change to " + val);
		} else if (e.getSource() == spinnerCol)
		{
			System.err.println("spinnerCol change to " + val);
		} else if (e.getSource() == spinnerRow)
		{
			System.err.println("spinnerRow change to " + val);
		}*/
		/*
		else if (e.getSource() == pbStatus)
		{
			JProgressBar pb = (JProgressBar)e.getSource();
			val = pb.getValue();
			
			System.err.println("Progressbar status changed to " + val);
		}
		*/
	}

	
	public final void mouseEntered(MouseEvent mouseevent)
	{
	}

	public final void mouseExited(MouseEvent mouseevent)
	{
	}

	/**
	* Invoked when the mouse is pressed
	*
	* @param	mouseevent	The MouseEvent
	*/
	public void mousePressed(MouseEvent mouseevent)
	{
		if (mouseevent.getSource() == componentImageCropped )
		{
		
			//find out where we are 
			Point pointMousePressedScaled = new Point( (int)Math.round(mouseevent.getPoint().getX()/zoomLevelCropped),
				(int)Math.round(mouseevent.getPoint().getY()/zoomLevelCropped) );
			
			pointMousePressed = new Point( (int)Math.round(mouseevent.getPoint().getX()),
				(int)Math.round(mouseevent.getPoint().getY()) );
			
			mouseDragged = false;
			
			System.err.println("Mouse pressed at " + pointMousePressedScaled);
			
			
			int xCurr = (int)pointMousePressedScaled.getX();
			int yCurr = (int)pointMousePressedScaled.getY();
			//System.err.println("Mouse pressed at (" + xCurr + "," + yCurr + ")");
			//frame.repaint();
			
			//fix the border 'rectangle' position in order to manually adjust the 
			// length/width of the object
			if (vecSegObjBordersShort != null && vecSegObjBordersShort.size() > 0)
			{
				Point point = mouseevent.getPoint();
				Point pointScaled = new Point( (int)Math.round(mouseevent.getPoint().getX()/zoomLevelCropped),
					(int)Math.round(mouseevent.getPoint().getY()/zoomLevelCropped) );
			
				//use a 'mouse-over' hover type identification tag
				int objId = -1;
				
				if ( pointScaled.getY() < imgSegCropped.length && pointScaled.getX() < imgSegCropped[0].length)
					objId = imgSegCropped[ (int)pointScaled.getY() ][ (int)pointScaled.getX() ];
					
				if (objId > 0)
				{
					componentImageCropped.setObjectDisplayId(objId);
					Point pointScaledOffset = new Point( (int)(pointScaled.getX()+10), (int)pointScaled.getY() );
					componentImageCropped.setObjectLocation(pointScaledOffset);
				} else
				{
					componentImageCropped.setObjectDisplayId(-1);
				}
				
				
				
				int objIndex = -1;
				int objIndexIndex = -1;
				double minDist = Double.MAX_VALUE;
				double currDist;
				Point p;

				//find the closest border pixel
				for (int i = 0; i < vecSegObjBordersShort.size(); i++)
				{
					Vector vecCurrBorders = (Vector)vecSegObjBordersShort.get(i);
					for (int j = 0; j < vecCurrBorders.size(); j++)
					{
						p = (Point)vecCurrBorders.get(j);
						currDist = p.distance(pointScaled);
						
						if (currDist < minDist)
						{
							minDist = currDist;
							objIndex = i;
							objIndexIndex = j;
						}
					}
				}
				
				
				Point pMatch = (Point)( (Vector)vecSegObjBordersShort.get(objIndex)).get(objIndexIndex);
				//Point pMatchScaled = new Point( (int)Math.round(pMatch.getX()*zoomLevelCropped),
				//	(int)Math.round(pMatch.getY()*zoomLevelCropped) );
				//System.err.println("Found matching border pixel: " + pMatch);
				
				//int offset = 2;
				//Graphics g = componentImage.get().getGraphics();
				
				//rectangle surrounding identified border point
				//Rectangle r = new Rectangle( (int)( pMatch.getX()-offset),
				//	(int)( (pMatch.getY()-offset)),
				//	(int)( (offset*2+1)*zoomLevelCropped),
				//	(int)( (offset*2+1)*zoomLevelCropped) );
					
				
				//imgDisplay.setData( imgDisplayBackup.getData() );
				//Graphics g = imgDisplay.getGraphics();
				//g.setColor(Color.YELLOW);
				//draw  a rectangle around the matching border pixel
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()-offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()-offset));
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()-offset), (int)(pMatch.getX()-offset), (int)(pMatch.getY()+offset)  );
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()+offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()+offset)  );
				//g.drawLine( (int)(pMatch.getX()+offset), (int)(pMatch.getY()+offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()-offset)  );
					
				//only draw the pointed if we are within a certain distance from an object
				if (objId >= 0 || minDist < MAX_DIST_BORDER_MOUSE_HOVER)
				{
					//clear
					//System.err.println("Mousepressed: object is now " + objIndex + " and used to be " + componentImageCropped.getObjectId() );
					if ( (componentImageCropped.getObjectId() >= 0 && objIndex != componentImageCropped.getObjectId()) || (componentImageCropped.hasPointFixedBorder1() && componentImageCropped.hasPointFixedBorder2() ) )
					{
						componentImageCropped.setBorderFixedPoint1(null);
						componentImageCropped.setBorderFixedPoint2(null);
						
					}
					componentImageCropped.setObjectId(objIndex);
					
					
					if ( !componentImageCropped.hasPointFixedBorder1() )
					{
						componentImageCropped.setBorderFixedPoint1(pMatch);
						
					} else if (!componentImageCropped.hasPointFixedBorder2() )
					{
						componentImageCropped.setBorderFixedPoint2(pMatch);
						
						
						Vector[] vecCurrHorizVertLines = (Vector[])vecHorizVertLines.get(objIndex);
						Vector vecCurrVertLines = vecCurrHorizVertLines[0];
						Vector vecCurrHorizLines = vecCurrHorizVertLines[1];
						Point[] lineHorizCenter = (Point[])vecCurrHorizLines.get(0);
						Point[] lineVertCenter = (Point[])vecCurrVertLines.get(0);
						
						
						//System.err.println(line[0] + ", " + line[1]);
						//System.err.println(lineHorizCenter[0] + ", " + lineHorizCenter[1]);
						//System.err.println(lineVertCenter[0] + " , " + lineVertCenter[1]);
						
						
						Point[] line = new Point[2];
						line[0] = componentImageCropped.getBorderFixedPoint1();
						line[1] = componentImageCropped.getBorderFixedPoint2();
						
						//Vector v2 = vecCurrHorizLines;
						//for (int k = 0; k < vecCurrVertLines.size(); k++)
						//	v2.add(vecCurrVertLines.get(k));
						int absDistHoriz = (int)Math.abs(line[0].getX() - line[1].getX() );
						int absDistVert = (int)Math.abs(line[0].getY() - line[1].getY() );
						
						
						double lineAngle = 0.0;
						Point[] linePerpCenter = new Point[1];
						
						lineAngle = MiscMath.pointAngle(line[0], line[1]);
						System.err.println("Line has angle " + lineAngle);
						
						/*
						//make sure that line[0].getX() < line[1].getX() and
						// make sure that line[0].getY() < line[1].getY()
						if (line[0].getX() > line[1].getX() )
						{
							Point pTemp = new Point(line[0]);
							line[0] = new Point(line[1]);
							line[1] = new Point(pTemp);
							
						} else if (line[0].getY() > line[1].getY() )
						{
							Point pTemp = new Point(line[0]);
							line[0] = new Point(line[1]);
							line[1] = new Point(pTemp);
						}
						*/
						
						//find the intersection point between the new line and the remaining line (horizontical or vertical)
						boolean isHorizontal = true;
						Point2D.Double pointIntersectCenter;
						if (absDistHoriz > absDistVert)
						{
							//horizontal line
							isHorizontal = true;
							lineHorizCenter = line;
							
							//The user-defined line is horizontal... let's move the vertical line
							//if (settings.getForceOrtho() )
							{
								pointIntersectCenter = MiscMath.lineIntersection(line, lineVertCenter);
								if (!GrayscaleImageEdit.pointIsWithinObject(imgSegCropped, (objIndex+1), pointIntersectCenter) )
								{
									//move 
									System.err.println("Intersection is outside object for point " + pointIntersectCenter);
									pointIntersectCenter = MiscMath.findPointOnLine(lineHorizCenter, 0.5);
									System.err.println("Moved to point " + pointIntersectCenter);
								}
								
								lineVertCenter = GrayscaleImageEdit.findPerpendicularLineFast(imgSegCropped, (objIndex+1), pointIntersectCenter, line );
							}
						} else
						{
							//vertical line
							isHorizontal = false;
							lineVertCenter = line;
						
							//The user-defined line is vertical... let's move the horizontal line
							//if (settings.getForceOrtho() )
							{
								pointIntersectCenter = MiscMath.lineIntersection(line, lineHorizCenter);
								if (!GrayscaleImageEdit.pointIsWithinObject(imgSegCropped, (objIndex+1), pointIntersectCenter) )
								{
									//move 
									System.err.println("Intersection is outside object for point " + pointIntersectCenter);
									pointIntersectCenter = MiscMath.findPointOnLine(lineVertCenter, 0.5);
									System.err.println("Moved to point " + pointIntersectCenter);
								}
								lineHorizCenter = GrayscaleImageEdit.findPerpendicularLineFast(imgSegCropped, (objIndex+1), pointIntersectCenter, line);
							}
						}
						
						System.err.println("Line goes from " + line[0] + " --> " + line[1]);
						
							
						double[] adjustments = {0.25, 0.50, 0.75};
						Point2D.Double pointIntersect;
						for (int k = 0; k < adjustments.length; k++)
						{
							//find a new set of points, horizontal case
							if (isHorizontal || settings.getForceOrtho() )
							{
								pointIntersect = MiscMath.findPointOnLine(lineVertCenter, adjustments[k]);
								Point[] linePerpHoriz = GrayscaleImageEdit.findPerpendicularLineFast(imgSegCropped, (objIndex+1), pointIntersect, lineVertCenter);
								vecCurrHorizLines.setElementAt(linePerpHoriz, k+1);
							}
							
							
							if (!isHorizontal || settings.getForceOrtho() )
							{
								//find a new intersection point, vertical case
								pointIntersect = MiscMath.findPointOnLine(lineHorizCenter, adjustments[k]);
								Point[] linePerpVert = GrayscaleImageEdit.findPerpendicularLineFast(imgSegCropped, (objIndex+1), pointIntersect, lineHorizCenter);
								vecCurrVertLines.setElementAt(linePerpVert, k+1);
							} 
							
							
							
							/*
							System.err.println("Adjusted intersection point is at (" + pointIntersect.getX() + "," + pointIntersect.getY() + ")");
							componentImageCropped.get().getGraphics().setColor( Color.BLUE );
							componentImageCropped.get().getGraphics().fillArc( (int)(pointIntersect.getX()),
								(int)(pointIntersect.getY()),
								5, 5, 0, 360);
							*/
							
						}
						
						//the perpendicular line is used to find appropriately spaced line,
						//but we only replace both if the 'forced orthogonality' is checked
						if (isHorizontal || settings.getForceOrtho() )
							vecCurrHorizLines.setElementAt(lineHorizCenter, 0);
						if (!isHorizontal || settings.getForceOrtho() )
							vecCurrVertLines.setElementAt(lineVertCenter, 0);
						
						
							
							
						//linePerp = null;
						//linePerp = GrayscaleImageEdit.findLongestPerpendicularLineExhaustive( (Vector)vecSegObjBordersShort.get(objIndex), lineAngle);
						
						//splitPane.revalidate();
						
						//System.err.println("Found longest perpendicular line: " + linePerp[0] + " --> " + linePerp[1]);
						
						
						//here we have to recalc. the center/50% etc lines
						/*
						if (absDistHoriz > absDistVert)
						{
							//System.err.println("Horizonal line...");
							Vector[] v = GrayscaleImageEdit.fetchHorizVertLinesInt( lineVertCenter, line, 
								(Vector)vecSegObjBordersShort.get(objIndex)  );
							
							vecCurrHorizLines = v[1];
							
							
							if (settings.getForceOrtho() && linePerp != null)
							{
								Vector[] v2 = GrayscaleImageEdit.fetchHorizVertLinesInt( linePerp, lineHorizCenter, 
									(Vector)vecSegObjBordersShort.get(objIndex)  );
							
								vecCurrVertLines = v2[0];
							}
							
						} else
						{
							//System.err.println("Vertical line...");
							Vector[] v = GrayscaleImageEdit.fetchHorizVertLinesInt( line, lineHorizCenter,
								(Vector)vecSegObjBordersShort.get(objIndex)  );
								
							vecCurrVertLines = v[0];
							
							if (settings.getForceOrtho() && linePerp != null)
							{
								Vector[] v2 = GrayscaleImageEdit.fetchHorizVertLinesInt( lineVertCenter, linePerp, 
									(Vector)vecSegObjBordersShort.get(objIndex)  );

								vecCurrHorizLines = v2[1];
							}
						}
						*/
						
				
						//change the data permanently
						((Vector[])vecHorizVertLines.get(objIndex))[0] = vecCurrVertLines;
						((Vector[])vecHorizVertLines.get(objIndex))[1] = vecCurrHorizLines;
						
						//adjust image
						componentImageCropped.setVectorOfLines(vecHorizVertLines);
						
						//System.err.println("--------");
						
						updateSaveButtons(true);

					}
					
					
					componentImageCropped.repaint();
				}
				
			}
		}
	}

	/**
	* Invoked when the mouse is released
	*
	* @param	mouseevent	The MouseEvent
	*/
	public final void mouseReleased(MouseEvent mouseevent)
	{
		if (mouseevent.getSource() == componentImageCropped)
		{
			//pointMouseReleased = new Point( (int)Math.round(mouseevent.getPoint().getX()/zoomLevelCropped),
			//	(int)Math.round(mouseevent.getPoint().getY()/zoomLevelCropped) );
			pointMouseReleased = new Point( (int)Math.round(mouseevent.getPoint().getX()),
				(int)Math.round(mouseevent.getPoint().getY()) );
			
			
			/*
			if (mouseDragged)
			{
				int paddingX = 10, paddingY = 10;
				
				// try to zoom in on the selected area
				int minX = (int)Math.min( pointMousePressed.getX(), pointMouseReleased.getX() );
				int maxX = (int)Math.max( pointMousePressed.getX(), pointMouseReleased.getX() );
				int minY = (int)Math.min( pointMousePressed.getY(), pointMouseReleased.getY() );
				int maxY = (int)Math.max( pointMousePressed.getY(), pointMouseReleased.getY() );
				
				
				
				Rectangle gbRect = new Rectangle(minX, minY, maxX-minX, maxY-minY);
				
				//scale to this scale
				Rectangle gbRectScaled = new Rectangle( (int)( (double)gbRect.getX()/zoomLevelCropped),
					(int)( (double)gbRect.getY()/zoomLevelCropped),
					(int)( (double)gbRect.getWidth()/zoomLevelCropped),
					(int)( (double)gbRect.getHeight()/zoomLevelCropped) );

				System.err.println(gbRect);
				System.err.println(gbRectScaled);
					
					
				int xScaled = (int)gbRectScaled.getX();
				int yScaled = (int)gbRectScaled.getY();

				//set zoom point

				//get the current width in order to determin the scale factor
				Rectangle vpRect = scrollPane.getViewport().getViewRect();
				int vpWidth = (int)vpRect.getWidth();
				int vpHeight = (int)vpRect.getHeight();


				//get the current width in order to locate the new center
				//actual width in the image is determined by the zoomLevelCropped
				double wOrg = (double)vpWidth * zoomLevelCropped;
				double hOrg = (double)vpHeight * zoomLevelCropped;

				double widthOrg = gbRect.getWidth() + paddingX;
				double heightOrg = gbRect.getHeight() + paddingY;

				double xCenterOrg = (double)(Math.max(0, gbRect.getX() - paddingX)) + widthOrg/2.0;
				double yCenterOrg = (double)(Math.max(0, gbRect.getY() - paddingY)) + heightOrg/2.0;

				double widthScale = (double)vpWidth/widthOrg;
				double heightScale = (double)vpHeight/heightOrg;


				int scaleFactorNew = (int)( Math.min(widthScale, heightScale) * 100.0);


				//System.err.println("Scale factors: w=" + widthScale + ", h=" + heightScale);

				zoomArea = new Rectangle2D.Double( xCenterOrg, yCenterOrg,
					widthOrg, heightOrg);


				//make sure we are always zooming/moving
				//zoomLevelCroppedCurrentVal = "-1";

				//call zoom  function
				//trigger new zoom event
				//String zoomLevelCroppedStr = new String( new Integer( (int)(scaleFactorNew)) + "%");
				//cbzoomLevelCropped.getEditor().setItem( zoomLevelCroppedStr );
				
				zoomLevelCroppedNew = (float)scaleFactorNew*zoomLevelCropped/100.0f;
				System.err.println("New scale factor: " + zoomLevelCroppedNew);
				this.actionPerformed( new ActionEvent(cbzoomLevelCropped, 0, "") );


				//trigger again, not sure why this is needed
				//zoomLevelCroppedCurrentVal = "-1";
				//zoomLevelCropped = scaleFactorNew;
				this.actionPerformed( new ActionEvent(cbzoomLevelCropped, 0, "") );

				
				
				
				// send to zoom function
				//zoomArea = new Rectangle2D.Double( minX/zoomLevelCropped, minY/zoomLevelCropped, (maxX - minX)/zoomLevelCropped, (maxY - minY)/zoomLevelCropped );
				//this.actionPerformed( new ActionEvent(cbzoomLevelCropped, 0, "") );
				
			}
			*/
			
			mouseDragged = false;
			pointMousePressed = null;
			pointMouseReleased = null;
		}
	}

	public final void mouseClicked(MouseEvent mouseevent)
	{
	}

	/**
	* Invoked when the mouse has moved
	*
	* @param	mouseevent	The MouseEvent
	*/
	public final void mouseMoved(MouseEvent mouseevent)
	{
		if (mouseevent.getSource() == componentImageCropped )
		{
		
			Point point = mouseevent.getPoint();
			//Point pointScaled = new Point( (int)Math.round(mouseevent.getPoint().getX()/zoomLevelCropped),
			//		(int)Math.round(mouseevent.getPoint().getY()/zoomLevelCropped) );
			Point pointScaled = new Point( (int)(mouseevent.getPoint().getX()/zoomLevelCropped),
					(int)(mouseevent.getPoint().getY()/zoomLevelCropped) );
			
			boolean mouseHasMoved = ( pointMouseLastMoved == null ||
				(Math.abs( pointScaled.getX() - pointMouseLastMoved.getX() ) < GrayscaleImageEdit.EPS &&
				 Math.abs( pointScaled.getY() - pointMouseLastMoved.getY() ) < GrayscaleImageEdit.EPS ) );
			
			

			if (vecSegObjBordersShort != null && vecSegObjBordersShort.size() > 0)
			{
				pointMouseLastMoved = pointScaled;
				
				//use a 'mouse-over' hover type identification tag
				int objId = -1;
				
				if ( pointScaled.getY() < imgSegCropped.length && pointScaled.getX() < imgSegCropped[0].length)
					objId = imgSegCropped[ (int)pointScaled.getY() ][ (int)pointScaled.getX() ];
				
				//System.err.println("mousemoved: object is now " + objId + " and used to be " + componentImage.getObjectId() );
				
				if (objId > 0)
				{
					componentImageCropped.setObjectDisplayId(objId);
					Point pointScaledOffset = new Point( (int)(pointScaled.getX()+2), (int)pointScaled.getY() );
					componentImageCropped.setObjectLocation(pointScaledOffset);
				} else
				{
					componentImageCropped.setObjectDisplayId(-1);
				}
				
				
				
				
				int objIndex = -1;
				int objIndexIndex = -1;
				double minDist = Double.MAX_VALUE;
				double currDist;
				Point p;
				
				
				//find the closest border pixel
				for (int i = 0; i < vecSegObjBordersShort.size(); i++)
				{
					Vector vecCurrBorders = (Vector)vecSegObjBordersShort.get(i);
					for (int j = 0; j < vecCurrBorders.size(); j++)
					{
						p = (Point)vecCurrBorders.get(j);
						currDist = p.distance(pointScaled);
						
						if (currDist < minDist)
						{
							minDist = currDist;
							objIndex = i;
							objIndexIndex = j;
						}
					}
				}
				
				Point pMatch = (Point)( (Vector)vecSegObjBordersShort.get(objIndex)).get(objIndexIndex);
				//Point pMatchScaled = new Point( (int)Math.round(pMatch.getX()/zoomLevelCropped),
				//	(int)Math.round(pMatch.getY()/zoomLevelCropped) );
				//System.err.println("Found matching border pixel: " + pMatch);
				
				//int offset = 2;
				//Graphics g = componentImage.get().getGraphics();
				
				//Pointangle r = new Pointangle( (int)( pMatch.getX()-offset),
				//	(int)( (pMatch.getY()-offset)),
				//	(int)( (offset*2+1)*zoomLevelCropped),
				//	(int)( (offset*2+1)*zoomLevelCropped) );
					
				
				
				//imgDisplay.setData( imgDisplayBackup.getData() );
				//Graphics g = imgDisplay.getGraphics();
				//g.setColor(Color.YELLOW);
				//draw  a rectangle around the matching border pixel
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()-offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()-offset));
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()-offset), (int)(pMatch.getX()-offset), (int)(pMatch.getY()+offset)  );
				//g.drawLine( (int)(pMatch.getX()-offset), (int)(pMatch.getY()+offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()+offset)  );
				//g.drawLine( (int)(pMatch.getX()+offset), (int)(pMatch.getY()+offset), (int)(pMatch.getX()+offset), (int)(pMatch.getY()-offset)  );
				
				/*
				if (objId > 0 || minDist < MAX_DIST_BORDER_MOUSE_HOVER)
				{
					componentImageCropped.setBorderVarPoint(pMatch);
					componentImageCropped.repaint();
				} else
				{
					componentImageCropped.setBorderVarPoint(null);
					componentImageCropped.setBorderFixedPoint1(null);
					componentImageCropped.setBorderFixedPoint2(null);
				}
				*/
				
				componentImageCropped.setBorderVarPoint(pMatch);
				componentImageCropped.repaint();
			}
		}
	}

	/**
	* Invoked when the mouse has been dragged
	*
	* @param	mouseevent	The MouseEvent
	*/
	public final void mouseDragged(MouseEvent mouseevent)
	{
		//find out where we are 
		if (mouseevent.getSource() == componentImageCropped)
		{
			Point pointScaled = new Point( (int)Math.round(mouseevent.getPoint().getX()/zoomLevelCropped),
				(int)Math.round(mouseevent.getPoint().getY()/zoomLevelCropped) );
			Point point = new Point( (int)Math.round(mouseevent.getPoint().getX()),
				(int)Math.round(mouseevent.getPoint().getY()) );
		
			//just flag that the mouse has been dragged
			mouseDragged = true;
			//System.err.println("Mouse dragged, now at " + pointScaled);
		}
	}
	
	public final void propertyChange(PropertyChangeEvent evt) 
	{

	}
	
	/**
	* Loads an image from a file
	*
	* @param	currentFilename	The name of the image file
	* @return	True if the image was successfully loaded, false otherwise
	*/
	protected boolean loadImage(String currentFilename)
	{
		boolean retVal = true;
		
		//currentFilename = new String(fd.getDirectory() + fd.getFile());                   
		System.err.println("Loading image: " + currentFilename + "...");
		labelStatus.setText("Loading image: " + currentFilename + "...");

		pbStatus.setValue(0);
		//panelBottomTop.setVisible(true);
		
		//load image and copy
		try
		{

			imgOrg = JAI.create("fileload", currentFilename).getAsBufferedImage();
			Raster rasterOrg = imgOrg.getData();
			
			//switch to default image type if we can't recognize it
			int imgType = imgOrg.getType();
			if (imgType == 0)
				imgType = BufferedImage.TYPE_INT_RGB;
			
			System.err.println("Constructing image of size (" + imgOrg.getWidth() + "," + imgOrg.getHeight() +
				") of type " + imgType );
			imgDisplay = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), imgType );
			imgDisplay.setData(rasterOrg);
			//imgDisplayBackup = new BufferedImage( imgOrg.getWidth(), imgOrg.getHeight(), imgType );
			//imgDisplayBackup.setData(rasterOrg);
			//panelBottomTop.setVisible(false);
			
			if (componentImageCropped != null)
				componentImageCropped.removeDrawnObjects();
			componentImage.set(imgDisplay);
			
			//componentImage.set(imgGrayscale);
			
			buttonRevert.setEnabled(false);
			buttonApplyFilterContour.setEnabled(true);
			buttonApplyNoContour.setEnabled(true);
			buttonCalcStats.setEnabled(false);
			buttonCrop.setEnabled(false);
			
		} catch (Throwable t)
		{
			t.printStackTrace();
			System.err.println("Failed to read image of size (" + imgOrg.getWidth() + "," + imgOrg.getHeight() +
				") of type " + imgOrg.getType() );
			
			retVal = false;
		}
		
		if (retVal)
		{
		
			panelSplit2.setVisible(false);
			splitPane.setDividerLocation( (int)frame.getSize().getHeight() );
			//isCropped = false;
			
			
			if (!isLoaded)
			{
				double spHeight = panelCenter.getHeight()-20;
				double spWidth = panelCenter.getWidth()-20;
				double scaleHeight = spHeight / (double)imgDisplay.getHeight();
				double scaleWidth = spWidth / (double)imgDisplay.getWidth();
					
				//auto-scale window so that entire image can be seen
				zoomLevel = (float)Math.min(scaleHeight, scaleWidth);
				System.err.println("Automatic scale factor: " + zoomLevel);
				componentImage.setScaleType(JComponentDisplay.SCALE_MODE_PRESCALE, zoomLevel);
				//componentImage.setScaleFactor(zoomLevel);
				//componentImage.setAutoScaleType(zoomLevel);
				
				scrollPane = new JScrollPane(componentImage);
				//scrollPane.getVerticalScrollBar().setMaximum( imgGreen.getHeight() );
				scrollPane.getVerticalScrollBar().setUnitIncrement( (int)(imgDisplay.getHeight()/(double)SCROLLBAR_INC) );
				//scrollPane.getHorizontalScrollBar().setMaximum( imgGreen.getWidth() );
				scrollPane.getHorizontalScrollBar().setUnitIncrement( (int)(imgDisplay.getWidth()/(double)SCROLLBAR_INC) );
									
				//zoomLevel = 1.0f;
			
				panelCenter.add(scrollPane, BorderLayout.CENTER);
				isLoaded = true;	
			}
			
			panelCenter.validate();
			buttonRevert.setEnabled(false);
			buttonApplyFilterContour.setEnabled(true);
			buttonApplyNoContour.setEnabled(true);
			
			
			labelStatus.setText("Active file: " + currentFilename);
			//panelCenter.add(componentImage, BorderLayout.CENTER);
		}
									
			
		//System.err.println("Done");
		return retVal;
	}

	
	/**
	* Updates the status of the 'save' buttons ('save' and 'save next')
	*
	* @param	enable	The status of the buttons will be set to 'enable'
	*/
	protected static void updateSaveButtons(boolean enable)
	{
		boolean canEnableBasic = (buttonSave != null && buttonSaveAndNext != null &&
			componentImageCropped != null && vecSegObjBordersShort != null &&
			vecSegObjBordersShort.size() > 0 && !frame.getCancelled() );
		
		if (canEnableBasic)
		{
			buttonSave.setEnabled(enable && !frame.getCancelled());
			buttonSaveAndNext.setEnabled(enable && !frame.getCancelled() && listOfImageFiles != null && currentImageFileInd < (listOfImageFiles.length-1) );
		}
	}
	
	/**
	* Invoked when a key is pressed, e.g. a shortcut pressed by the user.
	*
	* @param	e	The KeyEvent used to track what key was pressed
	*/
	public final void keyPressed(KeyEvent e)
	{
		//convert keyboard events to button action event
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			ctrlIsPressed = true;
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_S && buttonSave != null && buttonSave.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonSave, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_X && buttonNext != null && buttonNext.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonNext, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_N && buttonSaveAndNext != null && buttonSaveAndNext.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonSaveAndNext, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_R && buttonReanalyze != null && buttonReanalyze.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonReanalyze, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_E && buttonSettings != null && buttonSettings.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonSettings, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_O && buttonLoadFolder != null && buttonLoadFolder.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonLoadFolder, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_P && buttonLoadImage != null && buttonLoadImage.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonLoadImage, 0, "") );
		} else if (ctrlIsPressed && e.getKeyCode() == KeyEvent.VK_C && buttonLoadCalib != null && buttonLoadCalib.isEnabled() )
		{
			this.actionPerformed( new ActionEvent(buttonLoadCalib, 0, "") );
		} else if (e.getKeyCode() == KeyEvent.VK_PLUS && buttonZoomInCropped != null && !frame.getCancelled() )
		{
			this.actionPerformed( new ActionEvent(buttonZoomInCropped, 0, "") );
		}else if (e.getKeyCode() == KeyEvent.VK_MINUS && buttonZoomOutCropped != null && !frame.getCancelled() )
		{
			this.actionPerformed( new ActionEvent(buttonZoomOutCropped, 0, "") );
		}
	}
	
	/**
	* Invoked when a key is released, e.g. after a shortcut was pressed by the user.
	*
	* @param	e	The KeyEvent used to track what key was released
	*/
	public final void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
			ctrlIsPressed = false;
	}
	public final void keyTyped(KeyEvent e) {}
	
	/**
	* Recursively adds a key listener event to all component.
	*
	* @param	c	the current component
	*/
	protected void addKeyListenerRec(Component c)
	{
		c.addKeyListener(this);

		//Containers can contain several components,
		//we must check all
		if(c instanceof Container)
		{
			Container cont = (Container)c;

			Component[] children = cont.getComponents();

			for(int i = 0; i < children.length; i++)
			{
				addKeyListenerRec(children[i]);
			}
		}

	}
	

}
