/* LaminaSettings.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that implements a GUI for a 'settings window',
 * that allows the users to change settings certain parameters.
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
import java.awt.*;
import java.text.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.net.URL;

/**
    * A GUI to present a "settings" dialog.
*/
public class LaminaSettings extends JDialog implements ActionListener
{
	/*
	protected File inputDir, outputDir;
	protected double minObjSizeRel, minObjDensRel;
	protected boolean forceOrtho, thresholdSearchGreedy;
	protected int pixelExpansionContour;
	protected double thresholdSearchStepLength;
	protected double thresholdManual;
	protected double scaleParam; //scales from pixels to mm
	protected File iniFile;
	protected Point windowLocation;
	protected Dimension windowSize;
	
	protected Properties iniProperties;
	protected String applName;

	*/
	
	public final static int STATUS_OK = 0;
	public final static int STATUS_CANCEL = -1;
	public final static int STATUS_FAILED = -2;
	
	public final static byte RB_UNKNOWN = 0;
	public final static byte RB_GREEDY = 1;
	public final static byte RB_EXHAUSTIVE = 2;
	public final static byte RB_MANUAL = 3;
	

	protected int currentStatus = STATUS_OK;
	protected boolean showBatch = false;

	//protected JTextArea textArea;
	protected JTabbedPane jtp;
	protected JPanel panelBottom, panelTabs, panelGeneral, panelThresholds, panelObjectSizes, panelSearches,
		panelMisc, panelDirectories, panelBatchOutput, panelNumLandmarks;
	protected JButton buttonOK, buttonCancel, buttonFilterAdd, buttonFilterRemove,
		buttonInputDir, buttonOutputDir;
		//buttonColorCorners, buttonColorLines, buttonColorSkewPoints, buttonColorLinesAbsent;
	protected JSpinner spinnerThresholdStepLength, spinnerThresholdManual, spinnerMinObjSizeRel, spinnerMinObjDensRel,
		spinnerPixelContourThresh, spinnerNumLandmarks;
	protected JLabel labelSpinnerThresholdStepLength, labelSpinnerThresholdManual, labelSpinnerMinObjSizeRel, labelSpinnerMinObjDensRel,
		labelSpinnerPixelContourThresh, labelNumLandmarks, labelNumLandmarksSpinner;
	protected JPanel panelSpinnerThresholdStepLength, panelSpinnerThresholdManual, panelSpinnerMinObjSizeRel, panelSpinnerMinObjDensRel,
		panelSpinnerPixelContourThresh;
	protected JCheckBox chbForceOrtho, chbForceHorizVert, chbFindContour, 
		chbBatchWriteLogFile, chbBatchWriteCroppedImage, chbBatchWriteFullImage, chbBatchWriteLocalStatFile;
	protected JRadioButton rbSearchGreedy, rbSearchExhaustive, rbSearchManual;
    protected ButtonGroup bgSearch;
	protected JTextField jtfExternalURL;
	protected JList filterList;
	protected Vector filterListData;
	protected boolean autoApplyFilter = true;
	protected boolean displayInfoBar = true;
	protected boolean displayFileBar = true;
	protected boolean displayBrightnessBar = true;
	protected Color colorCorners = Color.WHITE,
		colorLines = Color.WHITE,
		colorSkewPoints = Color.WHITE,
		colorLinesAbsent = Color.WHITE;
	protected JTextField tfInputDir, tfOutputDir;

	
	protected byte rbStatus;
	protected ApplicationSettings settings;

	protected static JFrame owner;

	/**
	* Default constructor.
	*
	*/
	public LaminaSettings()
	{
		this( (JFrame)null);
	}

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	*/
	public LaminaSettings(JFrame owner)
	{
		this(owner, "Settings");
	}

	/**
	* Optional constructor
	*
	* @param	title	title of the dialog
	*/
	public LaminaSettings(String title)
	{
		this(null, title, true);
	}

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	*/
	public LaminaSettings(JFrame owner, String title)
	{
		this(owner, title, true);

	}

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	* @param	setVisible	if true, the dialog is set visible upon construction.
	*/
	public LaminaSettings(JFrame owner, String title, boolean setVisible)
	{
		this(owner, title, setVisible, false);
	}
	
	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	* @param	setVisible	if true, the dialog is set visible upon construction.
	*/
	public LaminaSettings(JFrame owner, String title, boolean setVisible, boolean showBatch)
	{
		this(owner, title, setVisible, false, null);
	}
	
	
	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	* @param	setVisible	if true, the dialog is set visible upon construction.
	* @param	showBatch	if true, the bar showing the 'batch output' options will be display, otherwise not
	* @param	settings	A settings object
	*/
	public LaminaSettings(JFrame owner, String title, boolean setVisible, boolean showBatch, ApplicationSettings settings)
	{
		super(owner, title);

		this.owner = owner;

		this.getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 250);
		this.showBatch = showBatch;
		this.settings = settings;

		//add buttons
		buttonOK = new JButton("OK");
		buttonOK.addActionListener(this);

		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);

		//for filters
		buttonFilterAdd = new JButton("Add filter entry");
		buttonFilterAdd.addActionListener(this);

		buttonFilterRemove = new JButton("Remove selected entries");
		buttonFilterRemove.addActionListener(this);


		JPanel panelFilterInfo = new JPanel( new GridLayout(1, 1) );

		JTextPane tpFilterInfo = new JTextPane();
		tpFilterInfo.setEditable(false);
		tpFilterInfo.setBackground( buttonOK.getBackground() );
		SimpleAttributeSet sasDef = new SimpleAttributeSet();
		addLine(tpFilterInfo, "Define a set of text strings that will be used to select "+
			"absent spots in the grid. The selected (grayed) spots will not be used " +
			"when auto-adjusting the blocks. The strings will be matched " +
			"against spot IDs and names using case-insensitive partial matching.", sasDef);
		panelFilterInfo.add(tpFilterInfo);

		JPanel panelFilterButtons = new JPanel( new GridLayout(1, 2) );
		panelFilterButtons.add(buttonFilterAdd);
		panelFilterButtons.add(buttonFilterRemove);

		filterList = new JList();
		filterList.setCellRenderer(new DefaultListCellRenderer() );
		JScrollPane spFilterList = new JScrollPane(filterList);

		filterListData = new Vector();

		JPanel panelFilters = new JPanel( new BorderLayout() );

		JPanel panelFiltersBottom = new JPanel( new BorderLayout() );
		
		panelThresholds = new JPanel( new GridLayout(6, 1) );



		rbSearchGreedy = new JRadioButton("Use greedy search");
		rbSearchGreedy.addActionListener(this);
		rbSearchGreedy.setSelected(true);
		rbStatus = RB_GREEDY;
		rbSearchExhaustive = new JRadioButton("Use exhaustive search");
		rbSearchExhaustive.addActionListener(this);
		rbSearchManual = new JRadioButton("Use manual threshold value (1-255)");
		rbSearchManual.addActionListener(this);
		
		bgSearch = new ButtonGroup();
		bgSearch.add(rbSearchGreedy);
		bgSearch.add(rbSearchExhaustive);
		bgSearch.add(rbSearchManual);
		
		
		
		
		spinnerThresholdStepLength = new JSpinner( new SpinnerNumberModel(10, 1, 255, 1) );
		spinnerThresholdStepLength.setMaximumSize( new Dimension(100, spinnerThresholdStepLength.getPreferredSize().height) );
		spinnerThresholdStepLength.setPreferredSize( new Dimension(100, spinnerThresholdStepLength.getPreferredSize().height) );

		labelSpinnerThresholdStepLength = new JLabel("Step size for search (1 - 255): ");
		panelSpinnerThresholdStepLength = new JPanel( new BorderLayout() );
		panelSpinnerThresholdStepLength.add(labelSpinnerThresholdStepLength, BorderLayout.CENTER);
		panelSpinnerThresholdStepLength.add(spinnerThresholdStepLength, BorderLayout.EAST);
		
			
		spinnerThresholdManual = new JSpinner( new SpinnerNumberModel(100, 1, 255, 1) );
		spinnerThresholdManual.setMaximumSize( new Dimension(100, spinnerThresholdManual.getPreferredSize().height) );
		spinnerThresholdManual.setPreferredSize( new Dimension(100, spinnerThresholdManual.getPreferredSize().height) );

		labelSpinnerThresholdManual = new JLabel("Manual threshold value (1 - 255): ");
		panelSpinnerThresholdManual = new JPanel( new BorderLayout() );
		panelSpinnerThresholdManual.add(labelSpinnerThresholdManual, BorderLayout.CENTER);
		panelSpinnerThresholdManual.add(spinnerThresholdManual, BorderLayout.EAST);
		
		//make sure the radiobutton changeEvent is fired
		this.actionPerformed( new ActionEvent(rbSearchGreedy, 0, "") );


		//panelThresholds.add(panelSearches);
		//panelSearches = new JPanel( new GridLayout(3,1) );
		panelThresholds.add(rbSearchGreedy);
		panelThresholds.add(rbSearchExhaustive);
		panelThresholds.add(rbSearchManual);
		panelThresholds.add( new JSeparator() );
		panelThresholds.add(panelSpinnerThresholdStepLength);
		//panelThresholds.add( new JSeparator() );
		panelThresholds.add(panelSpinnerThresholdManual);
		//panelThresholds.add(panelFiltersBottom, BorderLayout.SOUTH);
		
		
		
		// ------- Object sizes
		panelObjectSizes = new JPanel( new GridLayout(5, 1) );
		
		
		spinnerMinObjSizeRel = new JSpinner( new SpinnerNumberModel(0.1, 0.000000, 100.0, 0.01) );
		spinnerMinObjSizeRel.setMaximumSize( new Dimension(80, spinnerMinObjSizeRel.getPreferredSize().height) );
		spinnerMinObjSizeRel.setPreferredSize( new Dimension(80, spinnerMinObjSizeRel.getPreferredSize().height) );

		labelSpinnerMinObjSizeRel = new JLabel("Min. object size (% of tot. image size): ");
		panelSpinnerMinObjSizeRel = new JPanel( new BorderLayout() );
		panelSpinnerMinObjSizeRel.add(labelSpinnerMinObjSizeRel, BorderLayout.CENTER);
		panelSpinnerMinObjSizeRel.add(spinnerMinObjSizeRel, BorderLayout.EAST);
		
		
		spinnerMinObjDensRel = new JSpinner( new SpinnerNumberModel(0.1, 0.00000, 100.0, 0.01) );
		spinnerMinObjDensRel.setMaximumSize( new Dimension(80, spinnerMinObjDensRel.getPreferredSize().height) );
		spinnerMinObjDensRel.setPreferredSize( new Dimension(80, spinnerMinObjDensRel.getPreferredSize().height) );

		labelSpinnerMinObjDensRel = new JLabel("Min. object density (% of tot. image size): ");
		panelSpinnerMinObjDensRel = new JPanel( new BorderLayout() );
		panelSpinnerMinObjDensRel.add(labelSpinnerMinObjDensRel, BorderLayout.CENTER);
		panelSpinnerMinObjDensRel.add(spinnerMinObjDensRel, BorderLayout.EAST);
	
		panelObjectSizes.add(panelSpinnerMinObjSizeRel);
		panelObjectSizes.add(panelSpinnerMinObjDensRel);


		//-------- EXTERNAL URL

		
		

		/*
		JTextPane tpExternalInfo = new JTextPane();
		tpExternalInfo.setEditable(false);
		tpExternalInfo.setBackground( buttonOK.getBackground() );
		sasDef = new SimpleAttributeSet();
		addLine(tpExternalInfo, "Define the location for the external database location. "+
			"This location will be launched when you double-click a spot." +
			"A %I tag in the URL will be replaced by the spot ID whereas a %N tag will "+
			"be replaced by the spot name prior to launching the browser.", sasDef);

		
		

		JPanel panelExternalURL = new JPanel( new GridLayout(2, 1) );
		jtfExternalURL = new JTextField();

		panelExternalURL.add(new JLabel("Location: "));
		panelExternalURL.add(jtfExternalURL);

		gbcExternal.gridheight = 1;
		gbcExternal.fill = GridBagConstraints.BOTH;
		gbcExternal.weightx = 1.0;
		gbcExternal.weighty = 1.0;
		gbcExternal.gridwidth = GridBagConstraints.REMAINDER;


		gbExternal.setConstraints(panelExternalURL, gbcExternal);
		panelExternal.add( panelExternalURL );
		*/


		// ----- MISC PANEL
		
		JPanel panelMisc = new JPanel( new GridLayout(6, 1) );
		
		
		chbForceHorizVert = new JCheckBox("Use horizontal/vertical lines to initially approximate width/height");
		chbForceHorizVert.setSelected(displayInfoBar);
		
		chbFindContour = new JCheckBox("Find serrations");
		chbFindContour.setSelected(displayInfoBar);
		
		chbForceOrtho = new JCheckBox("Force perpendicular lines in object distance calc.");
		chbForceOrtho.setSelected(displayInfoBar);

		spinnerPixelContourThresh = new JSpinner( new SpinnerNumberModel(3, 0, 100, 1) );
		spinnerPixelContourThresh.setMaximumSize( new Dimension(100, spinnerPixelContourThresh.getPreferredSize().height) );
		spinnerPixelContourThresh.setPreferredSize( new Dimension(100, spinnerPixelContourThresh.getPreferredSize().height) );

		labelSpinnerPixelContourThresh = new JLabel("Serration detection pixel threshold (lower = more sensitive): ");
		panelSpinnerPixelContourThresh = new JPanel( new BorderLayout() );
		panelSpinnerPixelContourThresh.add(labelSpinnerPixelContourThresh, BorderLayout.CENTER);
		panelSpinnerPixelContourThresh.add(spinnerPixelContourThresh, BorderLayout.EAST);
		
		labelNumLandmarks = new JLabel("Boundary coordinates: ");
		spinnerNumLandmarks = new JSpinner( new SpinnerNumberModel(50, 0, 10000, 1) );
		panelNumLandmarks = new JPanel( new BorderLayout() );
		//panelNumLandmarks.setBorder( BorderFactory.createEtchedBorder() );
		panelNumLandmarks.add(labelNumLandmarks, BorderLayout.CENTER);
		panelNumLandmarks.add(spinnerNumLandmarks, BorderLayout.EAST);
		
		
		
		panelMisc.add(chbForceHorizVert);
		panelMisc.add(chbForceOrtho);
		panelMisc.add(chbFindContour);
		panelMisc.add( new JSeparator() );
		panelMisc.add(panelSpinnerPixelContourThresh);
		panelMisc.add(panelNumLandmarks);
		
		
		// --- Directories panel
		JPanel panelDirectories = new JPanel( new GridLayout(4, 1) );
		
		
		tfInputDir = new JTextField();
		tfInputDir.setEditable(false);
		tfInputDir.setText( settings.getInputDir().getAbsolutePath() );
		
		buttonInputDir = new JButton("...");
		buttonInputDir.addActionListener(this);
		
		JLabel labelInputDir = new JLabel("Default input directory");
		
		JPanel panelInputDir = new JPanel( new BorderLayout() );
		panelInputDir.add(labelInputDir, BorderLayout.NORTH);
		panelInputDir.add(tfInputDir, BorderLayout.CENTER);
		panelInputDir.add(buttonInputDir, BorderLayout.EAST);
		//panelInputDir.add( new JSeparator(), BorderLayout.SOUTH );
		
		tfOutputDir = new JTextField();
		tfOutputDir.setEditable(false);
		tfOutputDir.setText( settings.getOutputDir().getAbsolutePath() );
		
		buttonOutputDir = new JButton("...");
		buttonOutputDir.addActionListener(this);
		
		JLabel labelOutputDir = new JLabel("Default output directory");
		
		JPanel panelOutputDir = new JPanel( new BorderLayout() );
		panelOutputDir.add(labelOutputDir, BorderLayout.NORTH);
		panelOutputDir.add(tfOutputDir, BorderLayout.CENTER);
		panelOutputDir.add(buttonOutputDir, BorderLayout.EAST);
		
		
		
		panelDirectories.add(panelInputDir);
		panelDirectories.add(panelOutputDir);
		
		//tfOutputDir = new JTextField();
		//tfOutputDir.setEditable(false);
		
		
		/// - Output settings for the batch application
		panelBatchOutput = new JPanel( new GridLayout(5, 1) );
			
		chbBatchWriteLogFile = new JCheckBox("Output log for each processed file");
		chbBatchWriteCroppedImage = new JCheckBox("Output cropped image for each processed file");
		chbBatchWriteFullImage = new JCheckBox("Output segmented (full) image for each processed file");
		chbBatchWriteLocalStatFile = new JCheckBox("Output individual stat. for each processed file");
	
		panelBatchOutput.add(chbBatchWriteLogFile);
		panelBatchOutput.add(chbBatchWriteCroppedImage);
		panelBatchOutput.add(chbBatchWriteFullImage);
		panelBatchOutput.add(chbBatchWriteLocalStatFile);
		
		
		// ------ Skip the hotspot contour expansion for now
		//panelMisc.add(panelSpinnerPixelContourThresh);
		
		
		/*
		tpExternalInfo = new JTextPane();
		tpExternalInfo.setEditable(false);
		tpExternalInfo.setBackground( buttonOK.getBackground() );
		sasDef = new SimpleAttributeSet();
		addLine(tpExternalInfo, "Click buttons below to change grid colors.", sasDef);
		*/
		



		//add panels

		panelBottom = new JPanel( new GridLayout(1, 2) );
		panelBottom.add(buttonOK);
		panelBottom.add(buttonCancel);

		//panelFilters.add(panelFilters, BorderLayout.CENTER);
		//panelFilters.add(panelBottom, BorderLayout.NORTH);



		

		//panelGeneral = new JPanel( new GridLayout(5, 1) );
		//panelGeneral.add(chbForceOrtho);
		//panelGeneral.add(panelSearches);
		//panelGeneral.add(panelSpinnerThresholdStepLength);
		
		
		//panelFilters = new JPanel( new GridLayout(1, 1) );
		//panelFilters.add(spCiting);


		jtp = new JTabbedPane();
		//jtp.addTab("General", null, panelGeneral, "General settings");
		jtp.addTab("Thresholding", null, panelThresholds, "Thresholding settings");
		jtp.addTab("Object sizes", null, panelObjectSizes, "Settings for allowed object sizes");
		jtp.addTab("Misc.", null, panelMisc, "Misc. settings");
		jtp.addTab("Directories", null, panelDirectories, "Default directory settings");
		if (showBatch)
			jtp.addTab("Batch output", null, panelBatchOutput, "Output settings for batch application");
		
		//jtp.addTab("License", null, panelLicense, "Software license");


		this.flushSettingsToGUI();


		this.getContentPane().add(jtp, BorderLayout.CENTER);
		this.getContentPane().add(panelBottom, BorderLayout.SOUTH);

		this.setLocationRelativeTo(owner);


		this.setVisible(setVisible);
	}


	/**
	* Gets the status of the dialog (which depends on if the user pressed 'OK' or 'Cancel')
	*
	* @return	the status of the dialog
	*/
	public int getStatus()
	{
		return this.currentStatus;
	}

	/**
	* Gets the filter list containing filter strings
	*
	* @return	the vector of filter strings
	*/
	public Vector getFilterEntries()
	{
		return this.filterListData;
	}


	


	/**
	* Gets the status of the "force ortho" checkbox
	*
	* @return	true or false, depending on the checkbox status
	*/
	public boolean getForceOrtho()
	{
		return chbForceOrtho.isSelected();
	}
	
	/**
	* Gets the status of the "force ortho" checkbox
	*
	* @return	true or false, depending on the checkbox status
	*/
	public boolean getForceHorizVert()
	{
		return chbForceHorizVert.isSelected();
	}

	
	/**
	* Gets the URL of the external database when double-clicking a spot
	*
	* @return	the URL of the external database location
	*/
	public String getExternalURL()
	{
		return this.jtfExternalURL.getText();
	}


	


	/**
	* Sets the filter list containing filter strings
	*
	* @param	fld	the vector of filter strings
	*/
	public void setFilterEntries(Vector fld)
	{
		this.filterListData = fld;
		filterList.setListData(filterListData);
	}


	/**
	* Sets the default value for the 'force orthogonality' checkbox
	*
	* @param	forceOrtho	sets the 'force orthogonality' checked or not
	*/
	public void setForceOrtho(boolean forceOrtho)
	{
		
		chbForceOrtho.setSelected(forceOrtho);
	}
	
	/**
	* Sets the default value for the 'force horitzontal/vertical' checkbox
	*
	* @param	forceHorizVert	sets the 'force horitzontal/vertical' checked or not
	*/
	public void setForceHorizVert(boolean forceHorizVert)
	{
		
		chbForceHorizVert.setSelected(forceHorizVert);
	}

	
	
	/**
	* Sets the URL of the external database when double-clicking a spot
	*
	* @param	url	the new URL
	*/
	public void setExternalURL(String url)
	{
		this.jtfExternalURL.setText(url);
	}


	/**
	* Invoked when the 'Save' button is clicked.
	*
	* @param	e	the invoked ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonOK)
		{

			this.currentStatus = LaminaSettings.STATUS_OK;
			this.saveSettings();
			this.setVisible(false);
			this.dispose();

		} else if (e.getSource() == buttonCancel)
		{
			this.currentStatus = LaminaSettings.STATUS_CANCEL;
			this.flushSettingsToGUI(); //restore previous settings
			this.setVisible(false);
			this.dispose();

		} else if (e.getSource() == buttonFilterAdd)
		{
			String filterEntry = JOptionPane.showInputDialog("Please input a new string to use for absent spot filtering");

			filterListData.add(filterEntry);
			filterList.setListData(filterListData);

		} else if (e.getSource() == buttonFilterRemove)
		{
			int[] selIndices = filterList.getSelectedIndices();
			for (int i = (selIndices.length-1); i >= 0; i--)
				filterListData.remove(selIndices[i]);
			filterList.setListData(filterListData);

		} else if (e.getSource() == rbSearchGreedy || e.getSource() == rbSearchExhaustive || e.getSource() == rbSearchManual)
		{
			//System.err.print("rbSearch* changed, now at ");
			
			if (rbSearchGreedy.isSelected())
			{
				rbStatus = RB_GREEDY;
				//System.err.println("greedy");
				
				panelSpinnerThresholdManual.setEnabled(false);
				labelSpinnerThresholdManual.setEnabled(false);
				spinnerThresholdManual.setEnabled(false);
				panelSpinnerThresholdStepLength.setEnabled(true);
				labelSpinnerThresholdStepLength.setEnabled(true);
				spinnerThresholdStepLength.setEnabled(true);
				
				
			} else if (rbSearchExhaustive.isSelected())
			{
				rbStatus = RB_EXHAUSTIVE;
				//System.err.println("exhaustive");
				
				panelSpinnerThresholdManual.setEnabled(false);
				labelSpinnerThresholdManual.setEnabled(false);
				spinnerThresholdManual.setEnabled(false);
				panelSpinnerThresholdStepLength.setEnabled(true);
				labelSpinnerThresholdStepLength.setEnabled(true);
				spinnerThresholdStepLength.setEnabled(true);
				
			} else if (rbSearchManual.isSelected())
			{
				rbStatus = RB_MANUAL;
				//System.err.println("manual");
				
				panelSpinnerThresholdManual.setEnabled(true);
				labelSpinnerThresholdManual.setEnabled(true);
				spinnerThresholdManual.setEnabled(true);
				panelSpinnerThresholdStepLength.setEnabled(false);
				labelSpinnerThresholdStepLength.setEnabled(false);
				spinnerThresholdStepLength.setEnabled(false);
				
			} else
			{
				rbStatus = RB_UNKNOWN;
				//System.err.println("unknown");
				
			}
			
			
		} else if (e.getSource() == buttonInputDir || e.getSource() == buttonOutputDir)
		{
			boolean input = (e.getSource() == buttonInputDir);
			String str = (input) ? "input" : "output";
			
			//System.err.println(str + " clicked...");
			
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			//jfc.setFileFilter(new FileFilterImage() );
			
			if (input)
			{
				//jfc.setCurrentDirectory(settings.getInputDir().getParentFile());
				jfc.setCurrentDirectory(settings.getInputDir());
				jfc.setSelectedFile(settings.getInputDir());
			} else
			{
				//jfc.setCurrentDirectory(settings.getOutputDir().getParentFile());
				jfc.setCurrentDirectory(settings.getOutputDir());
				jfc.setCurrentDirectory(settings.getOutputDir());
			}
			
			
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jfc.setDialogType(JFileChooser.OPEN_DIALOG);
			
			
			int retVal = jfc.showDialog(owner, "Set " + str + " directory");
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				if (input)
				{
					settings.setInputDir( jfc.getSelectedFile() );
					tfInputDir.setText( jfc.getSelectedFile().getAbsolutePath() );
				} else
				{
					settings.setOutputDir( jfc.getSelectedFile() );
					tfOutputDir.setText( jfc.getSelectedFile().getAbsolutePath() );
				}
			
			}
		
		}
	}


	/**
	* Adds a line of text to a text pane.
	*
	* @param	tp	The text pane
	* @param	str	The text to be added to the textpane
	* @param	sas	The style of the text
	*/
	protected void addLine(JTextPane tp, String str, SimpleAttributeSet sas)
	{
		if (!str.endsWith("\n"))
			str += "\n";

		try
		{
			if (tp != null)
			{
				StyledDocument doc = (StyledDocument)tp.getDocument();
				doc.insertString(doc.getLength(), str, sas);
			}

			//messages.add(str);
			//System.out.print(str);
		} catch (BadLocationException e)
		{
		}
	}
	
	protected boolean flushSettingsToGUI()
	{
		if (this.settings != null)
		{
			try
			{
				
				chbFindContour.setSelected(settings.getFindContour());
				chbForceOrtho.setSelected(settings.getForceOrtho());
				chbForceHorizVert.setSelected(settings.getForceHorizVert());
				
				if (settings.getThresholdSearchGreedy())	
					rbSearchGreedy.setSelected(true);
				else if (settings.getThresholdSearchExhaustive())
					rbSearchExhaustive.setSelected(true);
				else
					rbSearchManual.setSelected(true);
				
				( (SpinnerNumberModel)spinnerThresholdStepLength.getModel()).setValue( new Double(settings.getThresholdSearchStepLength() ) );
				( (SpinnerNumberModel)spinnerThresholdManual.getModel()).setValue( new Double(settings.getThresholdManual() ) );
				( (SpinnerNumberModel)spinnerNumLandmarks.getModel()).setValue( new Integer(settings.getNumLandmarks() ) );
				
				// fire action event
				this.actionPerformed( new ActionEvent(rbSearchGreedy, 0, ""));
				
				
				( (SpinnerNumberModel)spinnerMinObjSizeRel.getModel()).setValue( new Double(settings.getMinObjSizeRel() ) );
				( (SpinnerNumberModel)spinnerMinObjDensRel.getModel()).setValue( new Double(settings.getMinObjDensRel() ) );
				
				( (SpinnerNumberModel)spinnerPixelContourThresh.getModel()).setValue( new Double(settings.getPixelContourThresh() ) );
			
				jtp.getModel().setSelectedIndex( settings.getSettingGUITab() );
				
				chbBatchWriteLogFile.setSelected(settings.getBatchWriteLogFile());
				chbBatchWriteCroppedImage.setSelected(settings.getBatchWriteCroppedImage());
				chbBatchWriteFullImage.setSelected(settings.getBatchWriteFullImage());
				chbBatchWriteLocalStatFile.setSelected(settings.getBatchWriteLocalStatFile());
				
			
				return true;
			
			} catch (Exception ex)
			{
				System.err.println("Incorrect settings for GUI");
				ex.printStackTrace();
				return false;
			}
			
		} else
		{
			return false;
		}
	}

	
	protected boolean saveSettings()
	{
		if (this.settings != null)
		{
			try
			{
				//yada yada here
				settings.setForceOrtho(chbForceOrtho.isSelected() );
				settings.setForceHorizVert(chbForceHorizVert.isSelected() );
				settings.setFindContour(chbFindContour.isSelected() );
				
				settings.setThresholdSearchGreedy( rbSearchGreedy.isSelected() );
				settings.setThresholdSearchExhaustive( rbSearchExhaustive.isSelected() );
				
				settings.setThresholdSearchStepLength( ( (SpinnerNumberModel)spinnerThresholdStepLength.getModel()).getNumber().doubleValue() );
				settings.setThresholdManual( ( (SpinnerNumberModel)spinnerThresholdManual.getModel()).getNumber().doubleValue()  );
				settings.setNumLandmarks( ( (SpinnerNumberModel)spinnerNumLandmarks.getModel()).getNumber().intValue()  );
				
				settings.setMinObjSizeRel( ( (SpinnerNumberModel)spinnerMinObjSizeRel.getModel()).getNumber().doubleValue() );
				settings.setMinObjDensRel( ( (SpinnerNumberModel)spinnerMinObjDensRel.getModel()).getNumber().doubleValue()  );
				
				settings.setPixelContourThresh( ( (SpinnerNumberModel)spinnerPixelContourThresh.getModel()).getNumber().intValue()  );
		
				settings.setSettingGUITab( jtp.getModel().getSelectedIndex() );
				
				settings.setBatchWriteLogFile(chbBatchWriteLogFile.isSelected() );
				settings.setBatchWriteCroppedImage(chbBatchWriteCroppedImage.isSelected() );
				settings.setBatchWriteFullImage(chbBatchWriteFullImage.isSelected() );
				settings.setBatchWriteLocalStatFile(chbBatchWriteLocalStatFile.isSelected() );
		
				return true;
			
			} catch (Exception ex)
			{
				System.err.println("Incorrect settings for GUI");
				ex.printStackTrace();
				return false;
			}
			
		} else
		{
			return false;
		}
	}
	
	protected ApplicationSettings getSettings()
	{
		return this.settings;
	}
		


	/**
	* Main function to start the program from command line.
	*
	* @param	args	array of arguments during function call
	*/
	/*
	public static void main(String[] args)
	{
		//start the GUI by calling the constructor
    		new JDialogGridSettings();
	}
	*/

}
