/* LaminaBatch.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that implements a GUI for batch-wise running
 * the leaf-extracting process on several images.
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
 * along with this library; if not, write to the Free Software Foundation, Inc.,
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
import java.lang.Math.*;
import java.net.URL;
import java.text.*;
import java.awt.*;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.Component;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.help.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import com.sun.media.jai.codec.*;



/**
Main class for LAMINA (Leaf shApe deterMINAtion) -- Batch version.
LAMINA is a tool for the automated analysis of
images of leaves. LAMINA has been designed to provide classical measures of leaf shape (blade dimensions) and
size (area) that are typically required for correlation analysis to biomass productivity, as well as measures that
indicate asymmetry in leaf shape, leaf serration traits, and measures of herbivory damage (missing leaf area). In
order to allow principal component analysis (PCA) to be performed, the location of a chosen number of equally
spaced markers can optionally be returned.
*/public class LaminaBatch extends WindowAdapter implements ActionListener, ChangeListener, MouseListener, TableModelListener
{
	public final static String APPL_NAME = "LAMINABatch";
	public final static String APPL_NAME_LONG = "LAMINA Batch";
	
	private final static String[] COMBO_YES_NO= {"No", "Yes"};
	
	protected final static String INI_FILENAME = "LaminaBatch.ini";
	protected final static String ERROR_LOG = "LaminaBatchErrors.log";
	
	private final static String OUTPUT_FNAME_SEG = "_" + APPL_NAME + "_seg";
	private final static String OUTPUT_FNAME_CROPPED = "_" + APPL_NAME + "_cropped";
	private final static String OUTPUT_FNAME_STATS = "_" + APPL_NAME + "_stats";
	private final static String OUTPUT_FNAME_LOG = "_" + APPL_NAME + "_log";
	
	private final static String OUTPUT_SEPARATOR = "---------------------------------------";
	
	private final static byte SCROLLBAR_INC = 50;
	private final static float ZOOM_MAX = 8.0f;
	private final static float ZOOM_MIN = 0.25f;
	private final static byte ZOOM_INC = 2;

	private static Runtime runTime;
	
	private static FileWriter fwErrorLog;
	//private static BufferedWriter bwErrorLog;
	private static PrintWriter pwErrorLog;
	
	
	// for segmentation
	private static Vector vecSegObjs, vecSegObjNoCavities, vecSegObjCenters,
		vecSegObjBorders, vecSegObjBordersShort, vecSegObjBordersShortLandmarks,
		vecSegObjBorderBP, vecSegObjBorderBPInner,
		vecHorizVertLines, vecIntersectPoints, vecContours, vecContourComplete, vecContourUnique,
		vecContourBorders, vecContourHotspotConnections, vecContourHotspotIndices, vecContourIndents;

	private static int[][] imgMatGrayscale, imgMatGrayscaleTemplate, imgSeg;
	private static byte[][] imgMatBinary;
	
	
	private static JFrameExt frame;
	private static Container content;
	private static JPanel panelLeft, panelCenter, panelRight, panelRightTop, panelRightBottom,
		panelBottom, panelBottomTop, panelBottomCenter, panelBottomBottom,
		panelBottomButtons, panelScaleParamSpinner;
	private static FilenameFilter ffJPEG;
	private static JLabel labelSpinnerMetaCol, labelSpinnerMetaRow, labelSpinnerRow, labelSpinnerCol,
		labelScaleParamSpinner;
	private static JButton buttonLoadImages, buttonApplyFilter, buttonStart, buttonAbort,
		buttonRemove, buttonSettings, buttonLoadCalib, buttonHelp, buttonAbout;
		//buttonCrop, buttonZoomIn, buttonZoomOut;
		
	private static JSpinner spinnerMetaCol, spinnerMetaRow, spinnerRow, spinnerCol, 
		spinnerScaleParam;
	private static FileDialog fd;
	//private JComponentDisplay componentImage;
	private static JScrollPane scrollPane;
	private static JProgressBar pbStatus, pbStatusOverall;
	private static JLabel labelStatus;
	
	protected static JTable table;
	protected static DefaultTableModel tableModel;
	
	
	protected static File iniFile;
	protected static boolean runContourID = false;
	protected static int modifierContourPB = 2;
	protected static Vector vecOutputData, vecOutputDataNames;

	protected static double putativeScaleParam = 0.0;
	
	protected static String calibFilename;
	protected static double calibWidth, calibHeight;
	private static BufferedImage imgCalib, imgCalibGrayscale;
	private static LaminaCalibProgress dialogCalibProgress;
	
	protected static JFrameLog frameMessages;
	
	private static JPanel panelCropImage;
	private static JLabel labelCropImage;
	private static JComboBox cbCropImage;
	private static JList fileList;
	private static File[] files;
	private static String [] fileNames;
	private static JDialogAbout mabout;
	
	private boolean isLoaded = false, isCropped = false;
	private boolean running = false, aborted = false;
	
	private static ApplicationSettings settings;
	
	private static String currentFilename = new String();
	private static File currentFile, lastInDir, lastOutDir;
	private static JDialogExtractExt dialogProgress;
	
	
	
	//private PlanarImage img;
	private static BufferedImage imgDisplay, imgCropped; //, imgOrg
	
	private float zoomLevel = 1.0f;
	
	
	/**
	* Main constructor. Initializes all parameters and sets up the GUI.
	*
	*/
	public LaminaBatch() 
	{
		runTime = Runtime.getRuntime();
		
		ffJPEG = new FilenameFilterJPEG();
		
		//create log frame, but don't display until required
		frameMessages = new JFrameLog(APPL_NAME_LONG + " event log", APPL_NAME +"_eventlog", false);
		
		//create frame
		frame = new JFrameExt(LaminaBatch.APPL_NAME_LONG + " version " + Lamina.APPL_VERSION);
		frame.setSize(600, 400);
		content = frame.getContentPane();
		//content.setBackground(Color.white);
		content.setLayout(new BorderLayout()); 
		frame.addWindowListener(this);
		//Image logo = Toolkit.getDefaultToolkit().getImage("masqot32x32.gif");
		//frame.setIconImage(logo);
		
		//fetch the logo from the jar file
		try
		{
			URL urlImage = this.getClass().getResource(Lamina.LOGO_FILE);
			Image logo = Toolkit.getDefaultToolkit().getImage(urlImage);
			frame.setIconImage(logo);
		} catch (Exception ex)
		{
			//ignore this for now
		}
		
		
		
		panelLeft = new JPanel( new GridLayout(1,1) );
		panelCenter = new JPanel( new BorderLayout() );
		panelRight = new JPanel( new BorderLayout() );
		panelBottom = new JPanel( new BorderLayout() );
		panelBottomTop = new JPanel( new GridLayout(1, 1) );
		panelBottomCenter = new JPanel( new GridLayout(1, 1) );
		panelBottomButtons = new JPanel( new GridLayout(1, 2) );
		panelBottomBottom = new JPanel( new GridLayout(1, 5) );
		panelBottomBottom.setBorder( BorderFactory.createEtchedBorder() );
		
		
		/** Right panel **/
		/*
		labelSpinnerMetaCol = new JLabel("Meta columns: ");
		spinnerMetaCol = new JSpinner( new SpinnerNumberModel(4, 1, 100, 1) );
		spinnerMetaCol.addChangeListener(this);
		JPanel panelRightSpinnerMetaCol = new JPanel( new BorderLayout() );
		panelRightSpinnerMetaCol.setBorder( BorderFactory.createEtchedBorder() );
		panelRightSpinnerMetaCol.add(labelSpinnerMetaCol, BorderLayout.CENTER);
		panelRightSpinnerMetaCol.add(spinnerMetaCol, BorderLayout.EAST);
		
		labelSpinnerMetaRow = new JLabel("Meta rows: ");
		spinnerMetaRow = new JSpinner( new SpinnerNumberModel(12, 1, 100, 1) );
		spinnerMetaRow.addChangeListener(this);
		JPanel panelRightSpinnerMetaRow = new JPanel( new BorderLayout() );
		panelRightSpinnerMetaRow.setBorder( BorderFactory.createEtchedBorder() );
		panelRightSpinnerMetaRow.add(labelSpinnerMetaRow, BorderLayout.CENTER);
		panelRightSpinnerMetaRow.add(spinnerMetaRow, BorderLayout.EAST);
		

		labelSpinnerCol = new JLabel("Columns: ");
		spinnerCol = new JSpinner( new SpinnerNumberModel(24, 1, 100, 1) );
		spinnerCol.addChangeListener(this);
		JPanel panelRightSpinnerCol = new JPanel( new BorderLayout() );
		panelRightSpinnerCol.setBorder( BorderFactory.createEtchedBorder() );
		panelRightSpinnerCol.add(labelSpinnerCol, BorderLayout.CENTER);
		panelRightSpinnerCol.add(spinnerCol, BorderLayout.EAST);
		
		labelSpinnerRow = new JLabel("Rows: ");
		spinnerRow = new JSpinner( new SpinnerNumberModel(12, 1, 100, 1) );
		spinnerRow.addChangeListener(this);
		JPanel panelRightSpinnerRow = new JPanel( new BorderLayout() );
		panelRightSpinnerRow.setBorder( BorderFactory.createEtchedBorder() );
		panelRightSpinnerRow.add(labelSpinnerRow, BorderLayout.CENTER);
		panelRightSpinnerRow.add(spinnerRow, BorderLayout.EAST);
		*/
		
		spinnerScaleParam = new JSpinner( new SpinnerNumberModel(1.0, 0.0, 10000.0, 0.1) );
		spinnerScaleParam.addChangeListener(this);
		spinnerScaleParam.setMaximumSize( new Dimension(50, spinnerScaleParam.getPreferredSize().height ) );
		spinnerScaleParam.setPreferredSize( new Dimension(50, spinnerScaleParam.getPreferredSize().height ) );

		labelScaleParamSpinner = new JLabel("Scaling (pixels/mm) ");
		panelScaleParamSpinner = new JPanel( new BorderLayout() );
		panelScaleParamSpinner.add(labelScaleParamSpinner, BorderLayout.CENTER);
		panelScaleParamSpinner.add(spinnerScaleParam, BorderLayout.EAST);

		

		DefaultComboBoxModel cbModelCropImage = new DefaultComboBoxModel(COMBO_YES_NO);
		cbCropImage = new JComboBox(cbModelCropImage);
		cbCropImage.setSelectedIndex(0); 
		cbCropImage.setMaximumSize( new Dimension(50, cbCropImage.getPreferredSize().height) );
		labelCropImage = new JLabel("Crop images ");
		panelCropImage = new JPanel( new GridLayout(1, 2) );
		panelCropImage.setBorder( BorderFactory.createEtchedBorder() );
		panelCropImage.add(labelCropImage);
		panelCropImage.add(cbCropImage);
		
		/** Center panel **/
		Vector tableColumns = new Vector();
		tableColumns.add("File name");
		tableColumns.add("File path");
		
	

		tableModel = new DefaultTableModel(tableColumns, 0);
		tableModel.addTableModelListener(this);

		//create a table to display the filenames
		table = new JTable(tableModel);
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		
		scrollPane = new JScrollPane(table);
		panelCenter.add(scrollPane, BorderLayout.CENTER );
		
		
		
		//String[] values = {"1", "2", "apa"};
		/*
		fileList = new JList();
		fileList.setCellRenderer(new ListCellRenderedBoxed() );
		scrollPane = new JScrollPane(fileList);
		
		*/
		
		//panelRight.add(panelRightSpinnerMetaCol);
		//panelRight.add(panelRightSpinnerMetaRow);
		//panelRight.add(panelRightSpinnerCol);
		//panelRight.add(panelRightSpinnerRow);
		
		/** Bottom panels **/
		buttonLoadImages = new JButton("Open images");
		buttonLoadImages.addActionListener(this);
		buttonSettings = new JButton("Settings");
		buttonSettings.addActionListener(this);
		buttonLoadCalib = new JButton("Calibration");
		buttonLoadCalib.addActionListener(this);
		//buttonApplyFilter = new JButton("Apply green filter");
		//buttonApplyFilter.addActionListener(this);
		//buttonCrop = new JButton("Crop image");
		//buttonCrop.addActionListener(this);
		buttonStart = new JButton("Start processing");
		buttonStart.addActionListener(this);
		buttonStart.setEnabled(false);
		
		buttonRemove = new JButton("Remove from list");
		buttonRemove.addActionListener(this);
		
		panelBottomButtons.add(buttonStart);
		panelBottomButtons.add(buttonRemove);
		
		//buttonZoomIn = new JButton("Zoom in");
		//buttonZoomIn.addActionListener(this);
		//buttonZoomOut = new JButton("Zoom out");
		//buttonZoomOut.addActionListener(this);
		
		// -- Help
		URL urlImageAbout = this.getClass().getResource(Lamina.LOGO_ABOUT);
		Image logoAbout = Toolkit.getDefaultToolkit().getImage(urlImageAbout);
		//buttonHelp = new JButton("Help",  new ImageIcon(logoAbout) );
		buttonHelp = new JButton("Help");

		URL url = this.getClass().getResource(Lamina.HELP_LOCATION);

		try
		{
			HelpSet hs = new HelpSet(null, url);
			hs.setTitle(Lamina.APPL_NAME + " help");
			HelpBroker hb = hs.createHelpBroker("Help");


			buttonHelp.addActionListener(new CSH.DisplayHelpFromSource(hb));
			//buttonHelp.addActionListener(this);

		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		// -- About
		buttonAbout = new JButton("About");
		buttonAbout.addActionListener(this);
		buttonAbout.setPreferredSize( new Dimension(buttonAbout.getWidth(), 20) );
		buttonAbout.setToolTipText("About this application");
		buttonAbout.setFocusable(false);
		
		

		pbStatusOverall = new JProgressBar(0, 100);		
		panelBottomTop.add(pbStatusOverall);
		panelBottomTop.setVisible(false);

		pbStatus = new JProgressBar(0, 100);
		//pbStatus.addChangeListener(this);
		panelBottomCenter.add(pbStatus);
		panelBottomCenter.setVisible(false);

		labelStatus = new JLabel(LaminaBatch.APPL_NAME_LONG + " version " + Lamina.APPL_VERSION);// + ". Copyright (c) " + Lamina.COPYRIGHT_HOLDER + " " + Lamina.COPYRIGHT_YEAR);
		panelBottomBottom.add(labelStatus);
		//panelBottomCenter.add(buttonZoomOut);
		
		//panelBottomBottom.add(buttonLoadImages);
		//panelBottomBottom.add(buttonCrop);
		//panelBottomBottom.add(buttonStart);
		panelRightTop = new JPanel( new GridLayout(5, 1) );
		panelRightBottom = new JPanel( new GridLayout(2, 1) );
		
		
		panelRightTop.add(buttonLoadImages);
		panelRightTop.add(buttonSettings);
		panelRightTop.add(buttonLoadCalib);
		panelRightTop.add(new JSeparator() );
		panelRightTop.add(panelScaleParamSpinner);
		//panelRight.add(buttonCrop);
		panelRightBottom.add(buttonHelp);
		panelRightBottom.add(buttonAbout);
		panelRight.add(panelRightTop, BorderLayout.NORTH);
		panelRight.add(panelRightBottom, BorderLayout.SOUTH);
		
		panelBottom.add(panelBottomTop, BorderLayout.NORTH);
		panelBottom.add(panelBottomCenter, BorderLayout.CENTER);
		panelBottom.add(panelBottomBottom, BorderLayout.SOUTH);
		
		panelCenter.add(panelBottomButtons, BorderLayout.SOUTH );
		
		/** Center panel **/
		//componentImage = new JComponentDisplay(100, 100);
		//componentImage.setScaleType( JComponentDisplay.SCALE_MODE_ORG );
		//componentImage.addMouseListener(this);
		
		/** Main panel **/
		content.add(panelLeft, BorderLayout.WEST);
		content.add(panelCenter, BorderLayout.CENTER);
		content.add(panelBottom, BorderLayout.SOUTH);
		content.add(panelRight, BorderLayout.EAST);
		
		
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
				
				
				
			} else
			{
				System.err.println("Failed to read INI file");
			}
			
			if (settings.getScaleParam() > 0)
				((SpinnerNumberModel)spinnerScaleParam.getModel()).setValue( new Double( settings.getScaleParam() ) );
			
				
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
	
		
		//prepare the error log
		try
		{
			File errorLogFile = new File(path, ERROR_LOG);
			
			fwErrorLog = new FileWriter(errorLogFile.getAbsolutePath(), true); //append
			//bwErrorLog = new BufferedWriter(fwErrorLog);
			pwErrorLog = new PrintWriter(fwErrorLog);
			
		} catch( Exception ex)
		{
			System.err.println("Failed to open error log " + ERROR_LOG + "!");
		}
		
	
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
    		new LaminaBatch();
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
		} else if (e.getSource() == dialogCalibProgress)
		{
			//System.err.println("Closing dialogCalibProgress...");

			dialogCalibProgress.abortProgress();
			
			
		} else if ( e.getSource() instanceof JFrame )
		{
			( (JFrame)e.getSource()).dispose();
			( (JFrame)e.getSource()).setEnabled(false);
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
			//System.err.println("Cleaning up memory after calibration progress...");
			//runTime.gc();
			
		}
	}

	/**
	* An event that is invoked when a general action is performed, e.g. a button-click etc.
	*
	* @param	e	The ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonLoadImages)
		{
			
			
			File dir = new File (".");
			if (lastInDir != null)
				dir = lastInDir;
				
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			jfc.setFileFilter(new FileFilterImage() );
			jfc.setCurrentDirectory(dir);
			jfc.setMultiSelectionEnabled(true);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int retVal = jfc.showDialog(frame, "Select files");
			
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				buttonStart.setEnabled(true);
				labelStatus.setText("Loading images...");
				
				//System.err.println("Approved!");
				files = jfc.getSelectedFiles();
				
				
				
				fileNames = new String[files.length];
				String[] fileNamesNoPath = new String[files.length];
				for (int i = 0; i < files.length; i++)
				{
					lastInDir = new File(files[i].getParentFile().getAbsolutePath());
					
					//System.err.println("File: " + files[i].getAbsolutePath());
					fileNames[i] = new String(files[i].getAbsolutePath() );
					fileNamesNoPath[i] = new String(files[i].getName() );
					
					Vector vec = new Vector(2);
					vec.add( files[i].getName() );
					//System.err.println(files[i].getParentFile().getAbsolutePath() );
					vec.add( files[i].getParentFile().getAbsolutePath() );
					tableModel.addRow(vec);
					
				}
				//fileList.setListData(fileNamesNoPath);
				
				if (files.length > 0)
					labelStatus.setText("Done loading " + files.length + " image(s)");
			
			}
			
		} else if (e.getSource() == buttonAbout)
		{
			if (mabout == null || mabout.isDisposed() )
			{

				if (mabout != null)
					mabout.setDisposed(false);

				new Thread()
				{
					public void run()
					{
						mabout = new JDialogAbout(frame);

					}

				}.start();
			} else
			{
				mabout.requestFocus();
				mabout.setVisible(true);
			}

			
			
		} else if (e.getSource() == buttonStart)
		{
			runContourID = settings.getFindContour();
			modifierContourPB = (runContourID) ? 1 : 2;
		
			//clear log
			frameMessages.setVisible(false);
			try
			{
				Document document = frameMessages.getTextPane().getDocument();
				document.remove( 0, document.getLength() );

			} catch (BadLocationException ex)
			{
				//ignore for now
			}
			
			//contains all the output data, and the names of the respective files
			vecOutputData = new Vector();
			vecOutputDataNames = new Vector();
			
		
			new Thread()
			{
				public void run()
				{
					
					
					frame.setError(false);
					frame.setCancelled(false);
					frame.setRunning(true);
					frame.setEnabled(false);

					int numRows = table.getRowCount();
					
					String msg = new String();
					
					Calendar cal = Calendar.getInstance(TimeZone.getDefault());
					SimpleDateFormat sdf = new SimpleDateFormat();
					sdf.setTimeZone(TimeZone.getDefault());
					
					int imgHeight = 0;
					int imgWidth = 0;
					
					for (int fNum = 0; fNum < numRows && !frame.getCancelled(); fNum++)
					{
						//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
						
						//System.err.println("Filtering...");
						if (fNum == 0)
						{
							dialogProgress = new JDialogExtractExt(frame, APPL_NAME_LONG + " -- processing files", true,
								new Dimension(frame.getWidth()+20, 180) );
							dialogProgress.getPBTotal().setValue(0);
						} else
						{
							Misc.addMessage(frameMessages.getTextPane(), OUTPUT_SEPARATOR, null);
						}
						
						//fetch the time
						String now = sdf.format(cal.getTime());
						
						Misc.addMessage(frameMessages.getTextPane(), "[" + now + "]", null, Color.ORANGE);
						
						dialogProgress.getPBInterim().setValue(0);
						dialogProgress.getPBCurrent().setValue(0);
						dialogProgress.getLabelTotal().setText("Processing file " + (fNum+1) + "/" +  table.getRowCount() +
							" (" + table.getModel().getValueAt(fNum, 0) + ") ...");
						dialogProgress.getLabelInterim().setText("File progress");
						
						//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
						
						//load image here
						dialogProgress.getLabelCurrent().setText("Loading image file...");
						
						String delim = ( ((String)table.getModel().getValueAt(fNum, 1)).lastIndexOf('\\') >= 0) ? "\\" : "/";
						currentFilename = table.getModel().getValueAt(fNum, 1) + delim + table.getModel().getValueAt(fNum, 0);
						imgDisplay = JAI.create("fileload", currentFilename).getAsBufferedImage();
						Raster rasterOrg = imgDisplay.getData();
						//imgDisplay = img.getAsBufferedImage();
						//img = null; //free memory
						
						Misc.addMessage(frameMessages.getTextPane(), "Reading file " + currentFilename + "...", null, Color.BLUE);
						
						//switch to default image type if we can't recognize it
						int imgType = imgDisplay.getType();
						if (imgType == 0)
							imgType = BufferedImage.TYPE_INT_RGB;
					
						//System.err.println("Constructing image of size (" + imgDisplay.getWidth() + "," + imgDisplay.getHeight() +
						//	") of type " + imgType );
						imgHeight = imgDisplay.getHeight();
						imgWidth = imgDisplay.getWidth();
						
						Misc.addMessage(frameMessages.getTextPane(), "Successfully read image of size (" + imgWidth + ","
							+ imgHeight + ") of type " + imgType, null);
						
						dialogProgress.getPBInterim().setValue(2*modifierContourPB);
						
						
						dialogProgress.getLabelCurrent().setText("Extracting blue band...");
						//Misc.addMessage(frameMessages.getTextPane(), "Extracting blue band...", null);
						Misc.addMessage(frameMessages.getTextPane(), "Extracting blue band...", null);
								
						//imgGrayscale = new BufferedImage( imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
						//WritableRaster wrGrayscale = imgGrayscale.getRaster();
						
						// set up the binary and grayscale images that will be used for segmentation
						
						//int i[] = new int[1];
						//int[] bluePixels = new int[imgHeight*imgWidth];
						
						//int[] bluePixelGrayscale = new int[imgHeight*imgWidth];
						imgMatGrayscale = new int[imgHeight][imgWidth];
						imgMatGrayscaleTemplate = new int[imgHeight][imgWidth];
						
						
						imgMatBinary = new byte[imgHeight][imgWidth];
						//double[][] imgMatMaxDiffChannel = new double[imgHeight][imgWidth];
						
		
						double numPixels = imgHeight*imgWidth;
						double meanIntensity = 0.0;
						int index = 0;
						int numLowInt = 0;
						//double maxDiffChannel = Integer.MIN_VALUE;
						
						int r,g,b;
						
						
						// now fetch data and try to find a suitable threshold
						// if greedy search, then a local minima may be found
						// this will cause segmentation to fail.
						// if this happens, then re-run with exhaustive instead of
						// greedy search.
						boolean forceExhaustiveRerun = false;
						boolean segmentationFailedGreedy = false;
						do
						{
						
							try
							{
							
								for (int h = 0; h < imgHeight; h++)
									for (int w = 0; w < imgWidth; w++)
									{
										b = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_B);
										
										/*
										r = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_R);
										g = rasterOrg.getSample(w,h,PlanarImageEdit.BAND_G);
									
										
										imgMatMaxDiffChannel[h][w] = (r + g + b)/3.0;
										
										
										imgMatMaxDiffChannel[h][w] =
											255 - 
											( Math.max( Math.max(r, g), b) -
											Math.min( Math.min(r, g), b) );
										
										if (imgMatMaxDiffChannel[h][w] > maxDiffChannel)
											maxDiffChannel = imgMatMaxDiffChannel[h][w];
										*/
										
										
										
										//bluePixels[ index++ ] = b;
										imgMatGrayscaleTemplate[h][w]=b;
										imgMatGrayscale[h][w]=imgMatGrayscaleTemplate[h][w];
										meanIntensity += imgMatGrayscaleTemplate[h][w]/numPixels;
										//System.err.print( bluePixels[ index - 1] + "," );
										
									
									}
									
									
							
								//System.err.println("Number of pixels below intensity threshold: " + numLowInt + "/" + (imgHeight*imgWidth));
								
													// height							//width
								System.err.println(""+imgMatGrayscale.length +","+imgMatGrayscale[1].length);
								System.err.println("Average intensity value: " + meanIntensity);
								
								dialogProgress.getPBInterim().setValue(5*modifierContourPB);
									
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to store image data as integer matrices";
								logError(msg, now, t);
							}
						
							
							
							/*
							double quant = 0.10;
							double quantileBlue = MiscMath.quantile(bluePixels, quant);
							System.err.println("Quantile intensity (" + quant + "): " + quantileBlue);
							*/
							
						
						
							int quantileBlue = 0;
							if (!frame.getError() && !frame.getCancelled() )
							{
								//GrayscaleImageEdit.truncateImage(imgMatIntensity,imgMatGrayscaleTemplate,20,255);
								
								//JAI.create("filestore", componentImage.get(), filename, codecId);
								
								try
								{
								
									if (settings.getThresholdSearchGreedy() && !forceExhaustiveRerun )
									{
										msg = "Detecting optimal threshold for segmentation (greedy)...";
										Misc.addMessage(frameMessages.getTextPane(), msg, null);
										dialogProgress.getLabelCurrent().setText(msg);
										
										//int startIntensity = (int)Math.round( MiscMath.quantile(imgMatGrayscale, 0.25) );
										int startIntensity = (int)Math.round(meanIntensity);
										
										quantileBlue = GrayscaleImageEdit.detectThresholdGreedy(imgMatGrayscaleTemplate, imgMatGrayscale,
											3, 3, startIntensity, (int)settings.getThresholdSearchStepLength(),
											frame, dialogProgress.getPBCurrent() );
											
										
											
										//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
										//	10, 10, (int)meanIntensity, 10, frame, dialogProgress.getPBCurrent() );
									} else if (settings.getThresholdSearchExhaustive() || forceExhaustiveRerun )
									{
										if (forceExhaustiveRerun)
										{
											msg = "Forcing exhaustive threshold detection since greedy search failed";
											Misc.addMessage(frameMessages.getTextPane(), msg, null);
										}
										
										msg = "Detecting optimal threshold for segmentation (exhaustive)...";
										Misc.addMessage(frameMessages.getTextPane(), msg, null);
										dialogProgress.getLabelCurrent().setText(msg);
										quantileBlue = (int)GrayscaleImageEdit.detectThresholdExhaustive(imgMatGrayscaleTemplate, imgMatGrayscale,
											3, 3, (int)settings.getThresholdSearchStepLength(), 0, 255,
											frame, dialogProgress.getPBCurrent() );
											
										//quantileBlue = GrayscaleImageEdit.detectThresholdGreedyMax(imgMatGrayscaleTemplate, imgMatGrayscale,
										//	10, 10, (int)meanIntensity, 10, frame, dialogProgress.getPBCurrent() );
										
									} else
									{
										msg = "Using manual threshold for segmentation...";
										Misc.addMessage(frameMessages.getTextPane(), msg, null);
										dialogProgress.getLabelCurrent().setText(msg);
										quantileBlue = (int)settings.getThresholdManual();
									}
									
									dialogProgress.getPBInterim().setValue(25*modifierContourPB);
									
									if (quantileBlue >= 0)
									{
										msg = "The selected threshold is " + quantileBlue + " (out of 255)";
										System.err.println(msg);								
										Misc.addMessage(frameMessages.getTextPane(), msg, null);
									}
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed when trying to detect optimal threshold for segmentation";
									logError(msg, now, t);	
								}
							}
						
						
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									//apply the threshold value
									msg = "Applying (segmentation) threshold to current image...";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									GrayscaleImageEdit.thresholdImage(imgMatGrayscaleTemplate,imgMatGrayscale,quantileBlue);
									GrayscaleImageEdit.thresholdImage(imgMatGrayscaleTemplate,imgMatGrayscaleTemplate,quantileBlue);//important!
									dialogProgress.getPBInterim().setValue(28*modifierContourPB);
								} catch( Throwable t)
								{

									msg = "[ERROR] Failed when applying optimal threshold for segmentation";
									logError(msg, now, t);
								}
							}
						
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
								
									msg = "Applying median filter on binary image (noise reduction)";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									GrayscaleImageEdit.medianFilter(imgMatGrayscaleTemplate,imgMatGrayscale, 3, 3);	
									dialogProgress.getPBInterim().setValue(30*modifierContourPB);
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed when applying median filter to binary image";
									logError(msg, now, t);
								}
							}
						
						
							if (!frame.getError() && !frame.getCancelled() )
							{
								//draw image
								/*
								BufferedImage biTemp2 = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
								WritableRaster wrTemp2 = biTemp2.getRaster();
								for (int h = 0; h < imgHeight; h++)
									for (int w = 0; w < imgWidth; w++)
									{
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_R, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_G, imgMatMaxChannel[h][w]);
										//wrTemp.setSample(w,h,PlanarImageEdit.BAND_B, imgMatMaxChannel[h][w]);
										
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_R, imgMatGrayscale[h][w]);
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_G, imgMatGrayscale[h][w]);
										wrTemp2.setSample(w,h,PlanarImageEdit.BAND_B, imgMatGrayscale[h][w]);
									
									}
								biTemp2.setData(wrTemp2);
								//componentImage.set(biTemp2);
								*/
								
								//set up binary image
								for (int h = 0; h < imgHeight; h++)
									for (int w = 0; w < imgWidth; w++)
									{
										imgMatBinary[h][w] = (imgMatGrayscale[h][w] != 0) ? (byte)1 : (byte)0;
										//System.err.print( bluePixels[ index - 1] + "," );
									}
							
							}
							
							
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
									msg = "Identifying objects in image (segmentation)";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									imgSeg = GrayscaleImageEdit.segmentBinaryImage(imgMatBinary, true);
									vecSegObjs = GrayscaleImageEdit.fetchSegObjCoord(imgSeg);
									dialogProgress.getPBInterim().setValue(33*modifierContourPB);
									segmentationFailedGreedy = false;
									
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed during segmentation";
									
									
									if (settings.getThresholdSearchGreedy())
									{
										
										
										if (!forceExhaustiveRerun)
										{
											//re-try with exhaustive threshold detection
											forceExhaustiveRerun = true;
											segmentationFailedGreedy = true;
											
											logError(msg, now, t, false);
											frame.setError(false);
											
										} else
										{
											//we tried segmentation again but exhaustive, but no luck, we have to give up
											segmentationFailedGreedy = false;
											logError(msg, now, t);
										}
									} else
									{
										logError(msg, now, t);
									}
									
								}
							}
							
						} while (segmentationFailedGreedy && !frame.getError() && !frame.getCancelled() );
						
						//now we have no need of the raster
						rasterOrg = null;
						System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
						runTime.gc();
						System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
							
						
						
						int numGoodObj = 0;
						if (!frame.getError() && !frame.getCancelled() )
						{
							try
							{
								//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
								msg = "Filtering small/sparse objects...";
								Misc.addMessage(frameMessages.getTextPane(), msg, null);
								dialogProgress.getLabelCurrent().setText(msg);
							
								//filter 'bad' objects
								long imgArea = imgMatGrayscaleTemplate.length*imgMatGrayscaleTemplate[0].length;
								Vector vecSegObjsTemp = new Vector(vecSegObjs.size());
								//Vector vecSegObjBordersTemp = new Vector(vecSegObjBorders.size());
								boolean[] goodObjects = GrayscaleImageEdit.filterObjects(vecSegObjs, imgArea,
									settings.getMinObjSizeRel()/100.0, settings.getMinObjDensRel()/100.0 );
								for (int i = 0; i < goodObjects.length; i++)
								{
									if (goodObjects[i])
									{
										numGoodObj++;
										vecSegObjsTemp.add( (Vector) vecSegObjs.get(i) );
										//vecSegObjBordersTemp.add( (Vector) vecSegObjBorders.get(i) );
									}
								}
								vecSegObjs = vecSegObjsTemp;
								vecSegObjCenters = GrayscaleImageEdit.findObjectCentroids(vecSegObjs);
								//vecSegObjBorders = vecSegObjBordersTemp;
								
								//repaint the segmentation matrix, keeping only the 'good' elements
								imgSeg = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjs, imgSeg[0].length, imgSeg.length);
						
								
								Misc.addMessage(frameMessages.getTextPane(), "Kept " + numGoodObj + " good objects", null, Color.GREEN);
								System.err.println("Kept " + numGoodObj + " good objects");
								dialogProgress.getPBInterim().setValue(35*modifierContourPB);
								
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to filter small/sparse objects";
								logError(msg, now, t);
							}
						}
						
						
						if (!frame.getError() && !frame.getCancelled() )
						{
						
							try
							{
								//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
								//fetch borders, calculate distance measures between border pixels and sort them accordingly
								msg = "Identifying and rearranging border pixels...";
								Misc.addMessage(frameMessages.getTextPane(), msg, null);
								dialogProgress.getLabelCurrent().setText(msg);
								Vector[] vecSegObjBordersArr = GrayscaleImageEdit.fetchSegObjCoordBorder(imgSeg, false, true, frame, dialogProgress.getPBCurrent() );
								if (vecSegObjBordersArr != null)
								{
									vecSegObjBorders = vecSegObjBordersArr[0]; //border points
									vecSegObjBorderBP = vecSegObjBordersArr[1]; //break points, for irregular perimeters
									vecSegObjBorderBPInner = vecSegObjBordersArr[2]; //break points for inner borders (cavotoes)
								} 
								dialogProgress.getPBInterim().setValue(40*modifierContourPB);
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to identify and rearrange border pixels (may be a memory issue)";
								logError(msg, now, t);
							}
						}
						
						//cavities here
						
						if (!frame.getError() &&!frame.getCancelled() )
						{
							try
							{
								//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
								msg = "Filling in any cavities in the objects...";
								Misc.addMessage(frameMessages.getTextPane(), msg, null);
								dialogProgress.getLabelCurrent().setText(msg);
								
								
								//fill in any cavities in the objects, to get an additional measurement of the perimeter/area
								vecSegObjNoCavities = GrayscaleImageEdit.fillObjectCavities(vecSegObjs, vecSegObjBorders, vecSegObjBorderBPInner, imgSeg,  dialogProgress.getPBCurrent() );
							
								//also shorten the border, so that the short version only contains the outer border
								vecSegObjBordersShort = GrayscaleImageEdit.shortenBorder(vecSegObjBorders, vecSegObjBorderBPInner);
								
								//fetch landmarks
								//int numLandmarks = settings.getNumLandMarks();
								//int maxAllowedLength = GrayscaleImageEdit.getMaxBorderLandmarks(vecSegObjBordersShort);
								//numLandmarks = (int)Math.min(numLandmarks, maxAllowedLength);
								vecSegObjBordersShortLandmarks = GrayscaleImageEdit.getBorderLandmarks(vecSegObjBordersShort, settings.getNumLandmarks());
						
						
							
								dialogProgress.getPBInterim().setValue(45*modifierContourPB);
								dialogProgress.getPBCurrent().setValue(100);
							
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to fill in cavities in the objects";
								logError(msg, now, t);
							}
						}
						
						
						if (!frame.getError() && !frame.getCancelled() )
						{
							try
							{
								//System.err.println("Memory: " + runTime.freeMemory()); //runTime.totalMemory()
								//update the segmentation image with only the 'good' objects
								msg = (runContourID) ? "Constructing preliminary output image..." : "Constructing output image...";
								Misc.addMessage(frameMessages.getTextPane(), msg, null);
								dialogProgress.getLabelCurrent().setText(msg);
								GrayscaleImageEdit.paintBinaryMatrix(vecSegObjs, imgMatGrayscale, 255);
								
								index = 0;
								for (int h = 0; h < imgHeight; h++)
									for (int w = 0; w < imgWidth; w++)
									{
										//bluePixels[ index++ ] = imgMatGrayscale[h][w];
										
										imgMatBinary[h][w] = (imgMatGrayscale[h][w] != 0) ? (byte)1 : (byte)0;
										
										//bluePixelGrayscale[ index++] = (imgMatBinary[h][w] != 0) ? (int)imgSeg[h][w]*(int)(255.0/numGoodObj) : (int)0;
										//System.err.print( bluePixels[ index - 1] + "," );
									}
								
								
								/*
								for (int i = 0; i < bluePixels.length; i++)
										if (bluePixels[i] <= quantileBlue)
											bluePixels[i] = 0;
										else
											bluePixels[i] = 255;
								*/
								
								//wrGrayscale.setPixels(0,0,imgWidth,imgHeight,bluePixelGrayscale);
								//imgGrayscale.setData(wrGrayscale);
								
								
								//System.err.println("Done.");
								dialogProgress.getPBInterim().setValue(49*modifierContourPB);
								
								//apply the binary mask to the display image
								PlanarImageEdit.applyMaskSide(imgDisplay, imgMatBinary);
								//componentImage.set(imgDisplay);
								
								Vector[] vecHorizVertLinesObj = GrayscaleImageEdit.fetchHorizVertLines(imgSeg, vecSegObjs, vecSegObjBordersShort,
									settings.getForceOrtho(),  settings.getForceHorizVert() );
								
								vecHorizVertLines = vecHorizVertLinesObj[0];
								vecIntersectPoints = vecHorizVertLinesObj[1];
								
								GrayscaleImageEdit.paintSegmentationResults(vecSegObjs, vecSegObjNoCavities, vecSegObjCenters, vecSegObjBorders, vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints, imgDisplay.getGraphics());
							
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to produce (segmented) output image";
								logError(msg, now, t);
							}
			
						}
						

						
						if (runContourID && !frame.getError() && !frame.getCancelled())
						{
							msg = "Starting serration identification:";
							Misc.addMessage(frameMessages.getTextPane(), msg, null);
							//dialogProgress.getLabelCurrent().setText(msg);
							
							int[][] imgSegNoBorders = new int[1][1]; //just to obey the java compiler...
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Masking border pixels for serrations...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									imgSegNoBorders = GrayscaleImageEdit.removeBorderPixels(vecSegObjBordersShort, imgSeg);
									dialogProgress.getPBInterim().setValue(60);
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to mask border pixels for serrations";
									logError(msg, now, t);
								}
							}
							
							Vector vecContourHotspots = new Vector(1);
							//int[][] contourHotspots = new int[1][1];
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Identifying serration connection points...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
								
									// vecContours is a global obj. and can be used by other function
									Vector[] vecContourHotspotConnectionsArr = GrayscaleImageEdit.findContourHotspotsNarrow(vecSegObjBordersShort, imgSegNoBorders, settings.getPixelContourThresh() );
									vecContourHotspotConnections = vecContourHotspotConnectionsArr[0];
									vecContourHotspotIndices = vecContourHotspotConnectionsArr[1];
									
									dialogProgress.getPBInterim().setValue(65);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to identify serration connection points";
									logError(msg, now, t);
								}
							}
							
							int[][] contourComplete = new int[1][1];
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Tracing complete contour area...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									
									contourComplete = GrayscaleImageEdit.traceContour(imgSeg, vecSegObjBordersShort, vecContourHotspotConnections, vecContourHotspotIndices);
								
									dialogProgress.getPBInterim().setValue(75);
									
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to calculate unique contour area";
									logError(msg, now, t);
								}
							}
							
							int[][] contourUnique = new int[1][1];
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Calculating unique contour area...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									
									
									contourUnique = GrayscaleImageEdit.matrixDifference(imgSegNoBorders, contourComplete);
									vecContourUnique = GrayscaleImageEdit.intMatrixToVectorOfPoints(contourUnique);
							
									//PlanarImageEdit.paintIntegerMatrix(contourComplete, Color.PINK, imgDisplay.getGraphics() );
									GrayscaleImageEdit.paintBordersAndCavities(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBorderBPInner, imgDisplay.getGraphics());
									PlanarImageEdit.paintIntegerMatrix(contourUnique, Color.BLUE, imgDisplay.getGraphics() );
									//PlanarImageEdit.paintVector(vecSegObjs, Color.GREEN, imgDisplay.getGraphics() );
									PlanarImageEdit.paintVector(vecSegObjBordersShort, Color.YELLOW, imgDisplay.getGraphics() );
									GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgDisplay.getGraphics(), Color.RED, false);
									//GrayscaleImageEdit.paintContourHotspotsCrosses(vecSegObjBordersShortLandmarks, imgDisplay.
									
									dialogProgress.getPBInterim().setValue(75);
									
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to calculate unique contour area";
									logError(msg, now, t);
								}
							}
							
							contourComplete = null; //no need for this variable anymore
							imgSegNoBorders = null; //no need for this variable anymore
							System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							runTime.gc();
							System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
							
							/*
							int[][] borderMat = new int[1][1];
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Calculating overlap between unique contour and original borders...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									borderMat = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjBordersShort, imgSeg[0].length, imgSeg.length);
									//byte[][] intersectBorderContour = GrayscaleImageEdit.matrixIntersect(borderMat, contourBorders);
									dialogProgress.getPBInterim().setValue(77);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to calculate overlap between unique contour and original border";
									logError(msg, now, t);
								}
							}
							
								
							//Vector vecContourHotspots = new Vector(1);
							//int[][] contourHotspots = new int[1][1];
							
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Identifying contour hotspots...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									vecContourIndents = GrayscaleImageEdit.fetchIndentDepths(vecContourHotspotConnections, vecContourHotspotIndices, vecSegObjBordersShort, contourUnique, dialogProgress.getPBCurrent());
									GrayscaleImageEdit.paintContourIndentDepths(vecContourIndents, imgDisplay.getGraphics(), Color.ORANGE );
								
								
									dialogProgress.getPBInterim().setValue(80);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to identify contour hotspots";
									logError(msg, now, t);
								}
							}
							*/
							
							/*
							Vector vecContourHotspotsMerged = new Vector(1);
							int[][] contourHotspotsMerged = new int[1][1];
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Merging contour hotspots...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									vecContourHotspotsMerged = GrayscaleImageEdit.mergeContourHotspots(vecContourHotspots, vecSegObjBordersShort, 3*Math.sqrt(2), 0);
									contourHotspotsMerged = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecContourHotspotsMerged, imgSeg[0].length, imgSeg.length);
									PlanarImageEdit.paintIntegerMatrix(contourHotspotsMerged, Color.PINK, imgDisplay.getGraphics() );
									//PlanarImageEdit.paintIntegerMatrix(contourHotspots, colors3, imgDisplay.getGraphics() );
									
									dialogProgress.getPBInterim().setValue(82);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to merge contour hotspots";
									logError(msg, now, t);
								}
							}
							
						
							//Vector vecContourGroups = new Vector(1);
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Identifying contour hotspot groups...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									// vecContours is a global obj. and can be used by other function
									vecContours = GrayscaleImageEdit.identifyContourGroups(vecContourHotspotsMerged, contourHotspotsMerged, true);
									
									dialogProgress.getPBInterim().setValue(85);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to identify contour hotspot groups";
									logError(msg, now, t);
								}
							}
							
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Identifying contour hotspot connection points...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									// vecContours is a global obj. and can be used by other function
									vecContourHotspotConnections = GrayscaleImageEdit.fetchContourHotspotConnections(vecContours, contourUnique);
									GrayscaleImageEdit.paintContourHotspots(vecContourHotspotConnections, imgDisplay.getGraphics(), Color.RED, false);
									
									dialogProgress.getPBInterim().setValue(90);
									dialogProgress.getPBCurrent().setValue(0);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to identify contour hotspot connection points";
									logError(msg, now, t);
								}
							}
							*/
							
							if (!frame.getError() && !frame.getCancelled() )
							{
								try
								{
									msg = "Calculating indent depths...";
									Misc.addMessage(frameMessages.getTextPane(), "  -" + msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									// vecContours is a global obj. and can be used by other function
									vecContourIndents = GrayscaleImageEdit.fetchIndentDepths(vecContourHotspotConnections, vecContourHotspotIndices, vecSegObjBordersShort, contourUnique, dialogProgress.getPBCurrent());
									GrayscaleImageEdit.paintContourIndentDepths(vecContourIndents, imgDisplay.getGraphics(), Color.ORANGE );
									
									dialogProgress.getPBInterim().setValue(94);
									dialogProgress.getPBCurrent().setValue(0);
								
								} catch (Throwable t)
								{
									msg = "[ERROR] Failed to calculate indent depths";
									logError(msg, now, t);
								}
							}
							
						} //if runContourID
						
						if (!frame.getError() && !frame.getCancelled() )
						{
							//preparing to write segmentation (full) image to file -->

							try
							{
								GrayscaleImageEdit.paintObjectIds(vecSegObjCenters, imgDisplay.getGraphics());
								
								if (settings.getBatchWriteFullImage())
								{
								
								
									//create a new filename
									//delim = (currentFilename.lastIndexOf('\\') >= 0) ? "\\" : "/"; //linux/windows path delimiters
									String currentFilenameNew = settings.getOutputDir().getAbsolutePath() + delim + table.getModel().getValueAt(fNum, 0);
									String[] parts = currentFilenameNew.split("\\.");
									String newStr = new String(parts[0]);
									for (int i = 1; i < (parts.length-1); i++)
										newStr = newStr  + "." + parts[i];
									String ext = parts[parts.length-1];
									
									String fName = newStr + OUTPUT_FNAME_SEG + "." + ext;
									File fileSave = new File(fName);
									int outNum = 2;
									while (fileSave.isFile() )
									{
										fName = newStr + OUTPUT_FNAME_SEG + (outNum++) + "." + ext;
										fileSave =  new File(fName);
									}
									
									msg = "Writing segmentation (full) image to file " + fName + "...";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
								
									String codecId = ext.toUpperCase();
									if (codecId.equals("TIF"))
										codecId = "TIFF";
									else if (codecId.equals("JPG"))
										codecId = "JPEG";
									
									
									JAI.create("filestore", imgDisplay, fName, codecId);
								}
								
								dialogProgress.getPBInterim().setValue(95);
							
							} catch (Throwable t)
							{
								msg = "[ERROR] Failed to output cropped image";
								logError(msg, now, t);
							}
	
						}
						
						
						if (!frame.getError() && !frame.getCancelled() )
						{
						
							
							
							try
							{
								if (settings.getBatchWriteCroppedImage())
								{
								
									msg = "Cropping output image...";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									
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
										Rectangle rect = new Rectangle(minX - Lamina.CROP_PADDING, minY-Lamina.CROP_PADDING,
											maxX-minX+2*Lamina.CROP_PADDING, maxY-minY+2*Lamina.CROP_PADDING);
										vec.add(rect);
									}
									
									imgCropped = PlanarImageEdit.cropImages(imgDisplay, vec, 1.0);
								
								}
								
								dialogProgress.getPBInterim().setValue(96);
							
							} catch (Throwable t)
							{	
								msg = "[ERROR] Failed to create cropped image";
								logError(msg, now, t);
							}
						
						}
						
						if (!frame.getError() && !frame.getCancelled() )
						{
							//preparing to write cropped image to file -->

							try
							{
								if (settings.getBatchWriteCroppedImage())
								{
								
								
									//create a new filename
									//delim = (currentFilename.lastIndexOf('\\') >= 0) ? "\\" : "/"; //linux/windows path delimiters
									String currentFilenameNew = settings.getOutputDir().getAbsolutePath() + delim + table.getModel().getValueAt(fNum, 0);
									String[] parts = currentFilenameNew.split("\\.");
									String newStr = new String(parts[0]);
									for (int i = 1; i < (parts.length-1); i++)
										newStr = newStr  + "." + parts[i];
									String ext = parts[parts.length-1];
									
									String fName = newStr + OUTPUT_FNAME_CROPPED + "." + ext;
									File fileSave = new File(fName);
									int outNum = 2;
									while (fileSave.isFile() )
									{
										fName = newStr + OUTPUT_FNAME_CROPPED + (outNum++) + "." + ext;
										fileSave =  new File(fName);
									}
									
									msg = "Writing cropped image to file " + fName + "...";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
								
									String codecId = ext.toUpperCase();
									if (codecId.equals("TIF"))
										codecId = "TIFF";
									else if (codecId.equals("JPG"))
										codecId = "JPEG";
									
									
									JAI.create("filestore", imgCropped, fName, codecId);
								}
								
								dialogProgress.getPBInterim().setValue(97);
							
							} catch (Throwable t)
							{	
								msg = "[ERROR] Failed to output cropped image";
								logError(msg, now, t);
							}
	
						}
						
						Vector objStats = new Vector(1);
						if (!frame.getError() && !frame.getCancelled() )
						{
							try
							{
								msg = "Calculating object statistics...";
								Misc.addMessage(frameMessages.getTextPane(), msg, null);
								dialogProgress.getLabelCurrent().setText(msg);
								
								objStats = GrayscaleImageEdit.calcSegStats(vecSegObjs, vecSegObjNoCavities, vecSegObjBorders, vecSegObjBordersShort,
									vecSegObjBorderBPInner, vecHorizVertLines, vecIntersectPoints,
									vecContourHotspotConnections, vecContourHotspotIndices, vecContourIndents,
									1/( (SpinnerNumberModel)spinnerScaleParam.getModel()).getNumber().doubleValue(),
									vecSegObjBordersShortLandmarks, vecSegObjCenters, vecSegObjCenters,
								imgDisplay.getGraphics() );
									
								dialogProgress.getPBInterim().setValue(98);
							
							} catch (Throwable t)
							{	
								msg = "[ERROR] Failed to calculate object statistics";
								logError(msg, now, t);
							}
						}
						
						String fName_noext = new String();
						
						if (!frame.getError() && !frame.getCancelled() )
						{
							try
							{
								if (settings.getBatchWriteLocalStatFile())
								{
								
									//create a new filename
									String currentFilenameNew = settings.getOutputDir().getAbsolutePath() + delim + table.getModel().getValueAt(fNum, 0);
									String[] parts = currentFilenameNew.split("\\.");
									String newStr = new String(parts[0]);
									for (int i = 1; i < (parts.length-1); i++)
										newStr = newStr  + "." + parts[i];
									String ext = parts[parts.length-1];
									fName_noext = newStr;
									
									String fName = fName_noext + OUTPUT_FNAME_STATS + ".txt";
									File fileSave = new File(fName);
									int outNum = 2;
									while (fileSave.isFile() )
									{
										fName = newStr + OUTPUT_FNAME_STATS + (outNum++) + ".txt";
										fileSave =  new File(fName);
									}
					
									msg = "Writing object statistics to file " + fName + "...";
									Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									
									
									String token = "\t";
									
									// start writing output
									
									FileWriter fw = new FileWriter(fName);
									BufferedWriter bw = new BufferedWriter(fw, 10000);

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
								
								}
								
								dialogProgress.getPBInterim().setValue(99);
							
							} catch (Throwable t)
							{	
								msg = "[ERROR] Failed to output object statistics to file";
								logError(msg, now, t);
							}

						}
						
						if (!frame.getError() && !frame.getCancelled() )
						{
							vecOutputData.add( objStats );
							vecOutputDataNames.add( Misc.extractFilename(currentFilename) );
						}
						
						// notify that the current file is done
						if (!frame.getError() && !frame.getCancelled() )
						{
							msg = "*** Done processing file";
							Misc.addMessage(frameMessages.getTextPane(), msg, null, Color.GREEN);
							dialogProgress.getLabelCurrent().setText(msg);
						}
						
						//write log, unless the user actively cancelled
						if (!frame.getCancelled() )
						{
							try
							{
								if (settings.getBatchWriteLogFile())
								{
								
									msg = "Writing log file of events...";
									//Misc.addMessage(frameMessages.getTextPane(), msg, null);
									dialogProgress.getLabelCurrent().setText(msg);
									
									
									String currentFilenameNew = settings.getOutputDir().getAbsolutePath() + delim + table.getModel().getValueAt(fNum, 0);
									String[] parts = currentFilenameNew.split("\\.");
									String newStr = new String(parts[0]);
									for (int i = 1; i < (parts.length-1); i++)
										newStr = newStr  + "." + parts[i];
									String ext = parts[parts.length-1];
									fName_noext = newStr;
									
									
									
									//fetch the time, to calculate the date used to name the log file
									//String timeFormatted = sdf.format(cal.getTime()).replaceAll(" ", "_").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-");
									
									String text = frameMessages.getTextPane().getText();
									
									//chop out the pieces that belong to the previous files
									if (fNum > 0)
									{
										String[] parts2 = text.split(OUTPUT_SEPARATOR);
										text = parts2[ parts2.length-1 ];
										
										//remove initial linefeed
										String[] parts3 = text.split("\n");
										text = "";
										for (int k = 1; k < parts3.length; k++)
											text = text + parts3[k] + '\n';
									}
									
									//create a unique filename
									File fLog = new File( fName_noext + OUTPUT_FNAME_LOG + ".txt");
									int outNum = 2;
									while (fLog.isFile())
										fLog = new File( fName_noext + OUTPUT_FNAME_LOG + "_" + (outNum++) + ".txt");
									
									System.err.println(fLog.getAbsolutePath());
									
									if (!frameMessages.writeLogToFile(fLog, text))
										throw new Exception();
								}
									
							} catch (Throwable t)
							{	
								msg = "[ERROR] Failed to write log file";
								logError(msg, now, t);
							}
						}
						
					
						
						//done, now just finish up
						if (!frame.getError() && !frame.getCancelled() )
						{
							dialogProgress.getPBInterim().setValue(100);
							//dialogProgress.getLabelCurrent().setText("Done.");
						
							//componentImage.set(imgDisplay);
							//componentImage.revalidate();
						}
						
						
						dialogProgress.getPBTotal().setValue( (int)Math.round( 100.0*(fNum+1)/(double)numRows ) );
						//if (fNum == (numRows-1) )
						//	frame.setCancelled(true);
						
						// canceleld() means that the user clicked to abort
						// otherwise, error() is used to note that an error has occured, and that
						// only the current file should be aborted
						if ( !frame.getCancelled() )
						{
							frame.setError(false);
						} else
						{
							Misc.addError(frameMessages.getTextPane(), "*** Aborted by the user", null);
						}
						
						
						
						//clean up some memory
						System.err.println("--- Memory (before GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
						clearMemory(runTime);
						System.err.println("--- Memory (after GC): total=" + runTime.totalMemory() +", free=" + runTime.freeMemory() );
						
						
					}
					
					//write a global output file with stats
					if (!frame.getCancelled() )
					{
						try
						{
							//first possible dir used: should change this to a 'default' directory type of entry
							String outputDir = settings.getOutputDir().getAbsolutePath();
							String delim =  (outputDir.lastIndexOf('\\') >= 0) ? "\\" : "/";
							
							//fetch the time, to calculate the date used to name the output file
							String timeFormatted = sdf.format(cal.getTime()).replaceAll(" ", "_").replaceAll(":", "-").replaceAll("/", "-").replaceAll("\\\\", "-");
							
							//vecOutputData.add( objStats );
							//vecOutputDataNames.add( Misc.extractFilename(currentFilename) );
							
							String fName = outputDir + delim + APPL_NAME + "_" + timeFormatted + ".txt";
							File fileSave = new File(fName);
							int outNum = 2;
							while (fileSave.isFile() )
							{
								fName = outputDir + delim + APPL_NAME + "_" + timeFormatted + "-" + (outNum++) + ".txt";;
								fileSave =  new File(fName);
							}
							System.err.println("Global stat. file: " + fName);
			
							String token = "\t";
							
							// start writing output
							
							FileWriter fw = new FileWriter(fName);
							BufferedWriter bw = new BufferedWriter(fw, 10000);

							//write header
							for (int k = 0; k < vecOutputData.size(); k++)
							{
							
								Vector objStats = (Vector)vecOutputData.get(k);
								Vector fileHeader = (Vector)objStats.get(0);
								
								
								//dump the file header first ..
								if (k == 0)
								{
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
									
								}

								// .. and then the data
								for (int i = 1; i < objStats.size(); i++)
								{
									Vector row = (Vector)objStats.get(i);
									
									//add filename
									bw.write( (String)vecOutputDataNames.get(k) );
									bw.write(token, 0, token.length());
									
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
							}
							bw.close();
							
						
						} catch (Exception ex)
						{
							System.err.println("Failed to write to global stat. file");
							ex.printStackTrace();
						}
					}
					
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
			
			
		} else if (e.getSource() == buttonRemove)
		{
			if (table.getSelectedRow() != -1)
			{
				for (int i = table.getSelectedRows().length-1; i >= 0 ; i--)
				{
					int row = table.getSelectedRows()[i];
					tableModel.removeRow(row);
				}
				//the focus is returned to this row, which also should be deleted
				if (table.getSelectedRow() != -1)
				{
					int row = table.getSelectedRow();
					tableModel.removeRow(row);
				}

				if (tableModel.getRowCount() == 0)
				{
					buttonRemove.setEnabled(false);
					buttonStart.setEnabled(false);
				}

			}
		
		} else if (e.getSource() == buttonSettings)
		{
			settings.setInputDir(lastInDir);
			settings.setOutputDir(lastOutDir);
			
			LaminaSettings dlgSettings = new LaminaSettings(frame, APPL_NAME_LONG + " settings", false, true, settings);
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
				
				//calibFilename = new String(fd.getDirectory() + fd.getFile());                   
				System.err.println("Loading (calib) image: " + calibFile + "...");
	
				pbStatus.setValue(0);
				//panelBottomTop.setVisible(true);
				
				//load image and copy
				boolean imageLoaded = false;
				
				try
				{
					imgCalib = JAI.create("fileload", calibFilename).getAsBufferedImage();
					Raster rasterCalib = imgCalib.getData();
					
					//switch to default image type if we can't recognize it
					int imgType = imgCalib.getType();
					if (imgType == 0)
						imgType = BufferedImage.TYPE_INT_RGB;
					
					System.err.println("Constructing image of size (w=" + imgCalib.getWidth() + ",h=" + imgCalib.getHeight() +
						") of type " + imgType );
					//imgDisplay = new BufferedImage( imgWidth, imgHeight, imgType );
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
					
					
				} catch (Exception ex)
				{
					System.err.println("Failed to read image of size (" + imgCalib.getWidth() + "," + imgCalib.getHeight() +
						") of type " + imgCalib.getType() );
					ex.printStackTrace();
				}
				
				// run calibration stuff here
				if (imageLoaded)
				{
					//System.err.println("Filtering...");
					dialogCalibProgress = new LaminaCalibProgress(frame, APPL_NAME_LONG + " -- processing calibration file", true,
						new Dimension(frame.getWidth()-50, frame.getHeight()-50) );
					dialogCalibProgress.addWindowListener(this);
					
					new Thread()
					{
						public void run()
						{
							frame.setCancelled(false);
							frame.setRunning(true);
							frame.setEnabled(false);

					
					
							
							
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
							
							/*
							int edgePixelLowerX = (int)Math.ceil(EDGE_REL_DIST*imgCalib.getWidth());
							int edgePixelUpperX = (int)Math.floor( (1-EDGE_REL_DIST)*imgCalib.getWidth());
							int edgePixelLowerY = (int)Math.ceil(EDGE_REL_DIST*imgCalib.getHeight());
							int edgePixelUpperY = (int)Math.floor( (1-EDGE_REL_DIST)*imgCalib.getHeight());
							*/
							
							Vector vecSegObjsCalib = new Vector();
							Vector vecSegObjBordersCalib = new Vector();
							Vector vecSegObjCentersCalib = new Vector();

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
									//int startIntensity = (int)Math.round( quantile(bluePixels, 0.25) );
									int startIntensity = (int)Math.round(meanIntensity);
						
									dialogCalibProgress.getLabelCurrent().setText("Detecting optimal threshold for segmentation (greedy)...");
									quantileBlue = GrayscaleImageEdit.detectThresholdGreedy(imgMatMaxDiffChannel, imgMatCalibGrayscale,
										3, 3, startIntensity, (int)settings.getThresholdSearchStepLength(),
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
								System.err.println("The selected threshold is " + quantileBlue + " (out of 255)");
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
								imgCalibSeg = GrayscaleImageEdit.segmentBinaryImage(imgMatCalibBinary, true);
								vecSegObjsCalib = GrayscaleImageEdit.fetchSegObjCoord(imgCalibSeg);
								dialogCalibProgress.getPBInterim().setValue(80);
							}
							
							if (!frame.getCancelled())
							{
							
								//fetch borders, calculate distance measures between border pixels and sort them accordingly
								dialogCalibProgress.getLabelCurrent().setText("Identifying and rearranging border pixels...");
								Vector[] vecSegObjBordersCalibArr = GrayscaleImageEdit.fetchSegObjCoordBorder(imgCalibSeg, false, false, frame, dialogCalibProgress.getPBCurrent() );
								if (vecSegObjBordersCalib != null)
								{
									vecSegObjBordersCalib = vecSegObjBordersCalibArr[0]; //contains border points, [1] contains break points which we ignore here
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
								boolean[] goodObjects = GrayscaleImageEdit.filterObjects(vecSegObjsCalib, imgArea,
									settings.getMinObjSizeRel()/100.0, settings.getMinObjDensRel()/100.0 );
								for (int i = 0; i < goodObjects.length; i++)
								{
									if (goodObjects[i])
									{
										numGoodObj++;
										vecSegObjsTemp.add( (Vector) vecSegObjsCalib.get(i) );
										vecSegObjBordersTemp.add( (Vector) vecSegObjBordersCalib.get(i) );
									}
								}
								vecSegObjsCalib = vecSegObjsTemp;
								vecSegObjBordersCalib = vecSegObjBordersTemp;
								System.err.println("Kept " + numGoodObj + " good objects");
								
							
								//we have to filter the segmentation so that we only keep the biggest object
								
							
								
								if (numGoodObj > 1)
								{
									System.err.print("Found more than one putative calib. object, keeping the largest one");
									
									Vector vecSegObjsTemp2 = new Vector(vecSegObjsCalib.size());
									Vector vecSegObjBordersTemp2 = new Vector(vecSegObjBordersCalib.size());
									
									
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
									
										vecSegObjsTemp2.add( (Vector)vecSegObjsCalib.get(biggestObjInd) );
										vecSegObjBordersTemp2.add( (Vector)vecSegObjBordersCalib.get(biggestObjInd) );
									}
									
									vecSegObjsCalib = vecSegObjsTemp2;
									vecSegObjBordersCalib = vecSegObjBordersTemp2;
									System.err.println(" (Object #" + (biggestObjInd+1) + ")");
									
									
								}
								dialogCalibProgress.getPBInterim().setValue(85);
									
							}
							
							if (!frame.getCancelled() )
							{
								if (vecSegObjsCalib.size() != 1)
								{
									System.err.println("Incorrect number of calibration objects (" + vecSegObjsCalib.size() +
										"): should be 1. Aborting");
									
									JOptionPane.showMessageDialog(frame, "Unable to locate any calibration object.\n" +
										"Please provide alternative calibration files.",
										"Failed to locate calibration object", JOptionPane.ERROR_MESSAGE);

									frame.setCancelled(true);
								} else
								{
									//find object centroids
									vecSegObjCentersCalib = GrayscaleImageEdit.findObjectCentroids(vecSegObjsCalib);
									
									//repaint integer matrix w/ objects
									imgCalibSeg = GrayscaleImageEdit.vectorOfPointsToIntMatrix(vecSegObjsCalib, imgCalibSeg[0].length, imgCalibSeg.length);
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
										
										bluePixelGrayscale[ index++] = (imgMatCalibBinary[h][w] != 0) ? (int)imgCalibSeg[h][w]*(int)(255.0/numGoodObj) : (int)0;
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
								
								BufferedImage imgCalibDisplay = PlanarImageEdit.applyMask(imgCalib, imgMatCalibBinary);
								//componentImage.set(imgCalibDisplay);
								dialogCalibProgress.setImage(imgCalibDisplay);
								
								Vector[] vecHorizVertLinesObj = GrayscaleImageEdit.fetchHorizVertLines(imgCalibSeg, vecSegObjsCalib,
									vecSegObjBordersCalib, settings.getForceOrtho(),  settings.getForceHorizVert() );
								
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
								
								
								// set this value only if the user presses 'OK'
								putativeScaleParam = scaleMean;
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
		}
		
		
	}
	
	/*
	private void refreshImage(int sbHorizontalValue, int sbVerticalValue)
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
	*/

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
        double val = -1;
		
		if (e.getSource() == spinnerScaleParam)
		{
			if (settings != null)
			{
				settings.setScaleParam( ( (SpinnerNumberModel)spinnerScaleParam.getModel()).getNumber().doubleValue() );
				//System.err.println("Updating scale parameter to " + settings.getScaleParam() );
			}
			
		} else if (e.getSource() instanceof JSpinner)
		{
			JSpinner js = (JSpinner)e.getSource();
			val = ( (Double)js.getModel().getValue()).doubleValue();
		}
		
		//prevent endless loop of changing spinners back and forth
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
		} 
		
		/*
		else if (e.getSource() == pbStatus)
		{
			JProgressBar pb = (JProgressBar)e.getSource();
			val = pb.getValue();
			
			System.err.println("Progressbar status changed to " + val);
		}
		*/
	}
	
	/**
	* A function that logs errors, both to disk and screen
	*
	* @param	msg	A string containing the message to write to the log
	* @param	now	The current time, described by a string
	* @param	t	A Throwable containing additional information regarding the error
	*/
	public void logError(String msg, String now, Throwable t)
	{
		logError(msg, now, t, true);
	}


	/**
	* A function that logs errors, both to disk and screen
	*
	* @param	msg	A string containing the message to write to the log
	* @param	now	The current time, described by a string
	* @param	t	A Throwable containing additional information regarding the error
	* @param	displayErrorLog	If true, an error log window is displayed, otherwise not
	*/
	public void logError(String msg, String now, Throwable t, boolean displayErrorLog)
	{
		frame.setError(true);
		if (displayErrorLog)
			frameMessages.setVisible(true);
		
		dialogProgress.getLabelCurrent().setText(msg);
		Misc.addError(frameMessages.getTextPane(), msg, null);
		try
		{
			pwErrorLog.println("[" + now + "]");
			pwErrorLog.println(msg + ":");
			//bwErrorLog.write(t.getMessage()); bwErrorLog.newLine();
			t.printStackTrace(pwErrorLog);
			pwErrorLog.flush();
			
		} catch (Exception ex) {}
		
	}
	
	/**
	* Tries to remove some objects from the memory and cleaning up by calling the GC.
	*
	* @param	r	The run-time object (used by GC)
	*/
	public void clearMemory(Runtime r)
	{
		vecSegObjs = null;
		vecSegObjNoCavities = null;
		vecSegObjBorders = null;
		vecSegObjBordersShort = null;
		vecSegObjBorderBP = null;
		vecSegObjBorderBPInner = null;
		vecHorizVertLines = null;
		vecIntersectPoints = null;
		vecContours = null;
		vecContourComplete = null;
		vecContourUnique = null;
		vecContourBorders = null;
		vecContourHotspotConnections = null;
		vecContourIndents = null;

		imgMatGrayscale = null;
		imgMatGrayscaleTemplate = null;
		imgSeg = null;
		imgMatBinary = null;
		
		r.gc();
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
		Point point = mouseevent.getPoint();
		int i = mouseevent.getModifiers();
	}

	/**
	* Invoked when the mouse is released
	*
	* @param	mouseevent	The MouseEvent
	*/
	public final void mouseReleased(MouseEvent mouseevent)
	{
		Point point = mouseevent.getPoint();
		
		System.err.println("Mouse release: " + point);
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
		Point point = mouseevent.getPoint();
	}

	/**
	* Invoked when the mouse has been dragged
	*
	* @param	mouseevent	The MouseEvent
	*/
	public final void mouseDragged(MouseEvent mouseevent)
	{
		mousePressed(mouseevent);
	}
	
	public void tableChanged(TableModelEvent e) {}

}
