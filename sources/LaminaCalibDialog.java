/* LaminaCalibDialog.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that implements a GUI for displaying the initial
 * steps of a calibration procedure.
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
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.media.jai.*;


/**
* A GUI to display the calibration progress
*/
public class LaminaCalibDialog extends JDialog implements ActionListener, WindowListener
{
	public final static int STATUS_OK = 0;
	public final static int STATUS_CANCEL = -1;
	public final static int STATUS_FAILED = -2;

	protected final static int SPINNER_PREF_WIDTH = 65;
	protected final static double SPINNER_HEIGHTS_INC = 0.05;



	protected JLabel labelSpinnerHeight, labelSpinnerWidth;
	protected JPanel panelSpinnerHW, panelSpinnerHeight, panelSpinnerWidth ;

	protected JFrame owner;
	protected File lastDir, calibFile;

	protected JButton buttonOK, buttonCancel, buttonOpen;
	protected JSpinner spinnerHeight, spinnerWidth;
	protected JTextField tfFileName;
	protected JPanel panelTop, panelCenter, panelBottom;
	protected String currentFile;
	protected int currentStatus = STATUS_CANCEL;
	


	//JDialogGridSpotInfo dlgGridSpotInfo;

	/**
	* Constructor
	*
	* @param	owner	the owner frame
	* @param	title	the title of the dialog
	*/
	public LaminaCalibDialog(JFrame owner, String title)
	{
		super(owner, title, true);

		this.owner = owner;

		panelTop = new JPanel( new GridLayout(5, 2) );
		panelCenter = new JPanel( new GridLayout(1, 1) );
		panelBottom = new JPanel( new BorderLayout() );

		JPanel panelBottomBottom = new JPanel( new GridLayout(1, 2) );
		JPanel panelBottomTop = new JPanel( new BorderLayout() );

		panelBottom.add(panelBottomBottom, BorderLayout.SOUTH);
		panelBottom.add(panelBottomTop, BorderLayout.NORTH);

		//buttonOpen = new JButton("Select file...");
		//buttonOpen.addActionListener(this);
		buttonOK = new JButton("Run calibration");
		buttonOK.addActionListener(this);
		buttonOK.setEnabled(false);
		//buttonOK.setEnabled(false);
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);

		buttonOpen = new JButton("Browse calibration file...");
		buttonOpen.addActionListener(this);

		tfFileName = new JTextField();
		tfFileName.setEditable(false);

		panelBottomBottom.add(buttonOK);
		panelBottomBottom.add(buttonCancel);
		panelBottomTop.add(buttonOpen, BorderLayout.CENTER);
		panelBottomTop.add(tfFileName, BorderLayout.NORTH);



		spinnerWidth = new JSpinner( new SpinnerNumberModel(10.0, 0.0, 10000.0, SPINNER_HEIGHTS_INC) );
		spinnerWidth.setMaximumSize( new Dimension(SPINNER_PREF_WIDTH, spinnerWidth.getPreferredSize().height) );
		spinnerWidth.setPreferredSize( new Dimension(SPINNER_PREF_WIDTH, spinnerWidth.getPreferredSize().height) );

		labelSpinnerWidth = new JLabel("Width of calib. object (mm) ");
		panelSpinnerWidth = new JPanel( new BorderLayout() );
		panelSpinnerWidth.add(labelSpinnerWidth, BorderLayout.CENTER);
		panelSpinnerWidth.add(spinnerWidth, BorderLayout.EAST);


		spinnerHeight = new JSpinner( new SpinnerNumberModel(10.0, 0.0, 10000.0, SPINNER_HEIGHTS_INC) );
		spinnerHeight.setMaximumSize( new Dimension(SPINNER_PREF_WIDTH, spinnerHeight.getPreferredSize().height) );
		spinnerHeight.setPreferredSize( new Dimension(SPINNER_PREF_WIDTH, spinnerHeight.getPreferredSize().height) );

		labelSpinnerHeight = new JLabel("Height of calib. object (mm) ");
		panelSpinnerHeight = new JPanel( new BorderLayout() );
		panelSpinnerHeight.add(labelSpinnerHeight, BorderLayout.CENTER);
		panelSpinnerHeight.add(spinnerHeight, BorderLayout.EAST);


		
		//panelTop.add(buttonOpen);
		panelTop.add(panelSpinnerWidth);
		panelTop.add(panelSpinnerHeight);
		/*
		panelTop.add(panelSpinnerRow);
		panelTop.add(panelSpinnerCol);
		panelTop.add( new JSeparator() );
		panelTop.add( new JSeparator() );
		panelTop.add(panelSpinnerResolution);
		panelTop.add( new JLabel() );
		panelTop.add(panelSpinnerSpotDist);
		panelTop.add(panelSpinnerBlockDist);
		*/
		//panelCenter.add(tfFileName);
		//panelCenter.add(buttonOpen);


		this.setAllEnabled(true);

		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().add(panelTop, BorderLayout.NORTH);
		this.getContentPane().add(panelCenter, BorderLayout.CENTER);
		this.getContentPane().add(panelBottom, BorderLayout.SOUTH);
		this.pack();
		this.setBounds(0, 0, 400, 200);
		this.addWindowListener(this);
		this.setLocationRelativeTo(owner);
		//this.setVisible(true);
	}

	/**
	* Initializes the spinner to a set of values
	*
	* @param	height	The height of the spinner
	* @param	width	The width of the spinner
	* @param	lastDir	Last directory used
	* @param	calibFile	Last calibration file used
	*/
	public void initialize(double height, double width, File lastDir, File calibFile)
	{
		this.spinnerHeight.getModel().setValue( new Double(height) );
		this.spinnerWidth.getModel().setValue( new Double(width) );
		this.lastDir = lastDir;
		this.calibFile = calibFile;
		
		if (this.calibFile.isFile())
		{
			buttonOK.setEnabled(true);
			tfFileName.setText( this.calibFile.getAbsolutePath() );
		}
	}


	/**
	* Invoked when the user closes the dialog.
	*
	* @param	e	the invoked WindowEvent
	*/
	public void windowClosing(WindowEvent e)
	{
		this.dispose();
		this.setVisible(false);
	}

	/**
	* Invoked when the user presses a button on the dialog.
	*
	* @param	e	the invoked ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonOK)
		{
			currentStatus = STATUS_OK;

			this.dispose();
			this.setVisible(false);

		} else if (e.getSource() == buttonCancel)
		{
			currentStatus = STATUS_CANCEL;
			this.dispose();
			this.setVisible(false);
		} else if (e.getSource() == buttonOpen)
		{
			
			//remember last filename
			if (lastDir == null)
				lastDir = new File(".");
				
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			jfc.setFileFilter(new FileFilterImage() );
			jfc.setCurrentDirectory(lastDir);
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int retVal = jfc.showDialog(owner, "Select calibration file");
			
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				calibFile = jfc.getSelectedFile();
				lastDir = calibFile.getParentFile();
				String currentFilename = new String( calibFile.getAbsolutePath() );
				
				tfFileName.setText(currentFilename);
				
				buttonOK.setEnabled(true);
				
				//System.err.println("Last dir: " + lastDir.getAbsolutePath() );
				
				//currentFilename = new String(fd.getDirectory() + fd.getFile());                   
				//System.err.println("Loading (calib) image: " + currentFilename + "...");
	
				//pbStatus.setValue(0);
				//panelBottomTop.setVisible(true);
			}

		}

		//System.err.println("Something happened...");

	}


	/**
	* Gets the height from the spinner object.
	*
	* @return	height
	*/
	public double getObjectHeight()
	{
		return  ((SpinnerNumberModel)this.spinnerHeight.getModel()).getNumber().doubleValue();
	}

	/**
	* Gets the width from the spinner object.
	*
	* @return	width
	*/
	public double getObjectWidth()
	{
		//return  ( (Double)this.spinnerWidth.getModel().getValue()).doubleValue();
		return  ((SpinnerNumberModel)this.spinnerWidth.getModel()).getNumber().doubleValue();
	}
	
	
	
	/**
	* Last active directory
	*
	* @return	width
	*/
	public File getLastDir()
	{
		return lastDir;
	}
	
	/**
	* Calibration file
	*
	* @return	Calibration file
	*/
	public File getCalibFile()
	{
		return calibFile;
	}


	
	/**
	* Selects or deselects a group of components on the dialog
	*
	* @param	enabled	if enabled, the components will be selected, or otherwise deselected
	*/
	protected void setAllEnabled(boolean enabled)
	{
		this.labelSpinnerHeight.setEnabled(enabled);
		this.panelSpinnerHeight.setEnabled(enabled);
		this.spinnerHeight.setEnabled(enabled);

		this.labelSpinnerWidth.setEnabled(enabled);
		this.panelSpinnerWidth.setEnabled(enabled);
		this.spinnerWidth.setEnabled(enabled);

	}


	
	

	/**
	* Gets the status of the dialog (which depends on if the user pressed 'OK' or 'Cancel')
	*
	* @return	the status of the dialog
	*/
	public int getStatus()
	{
		return currentStatus;
	}

	/*
	public static void main (String[] argv)
	{
		LaminaCalibDialog dlgCalib = new LaminaCalibDialog( (JFrame)null, "Open calibration file");
		dlgCalib.setVisible(true);
	}
	*/


	//implementation of (unused) abstract classes -->
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void stateChanged(ChangeEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void itemStateChanged(ItemEvent e) {}

}
