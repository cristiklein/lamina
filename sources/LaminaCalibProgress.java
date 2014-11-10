/* LaminaCalibProgress.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that implements a GUI for displaying the overall
 * progress of a calibration procedure
 * (an object with known size is identified and used to find
 *  the conversion parameter of pixels to millimeters).
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
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import com.sun.media.jai.codec.*;

/**
    * A GUI to display the calibration progress
*/
public class LaminaCalibProgress extends JDialog implements ActionListener, WindowListener
{
	public final static int STATUS_OK = 0;
	public final static int STATUS_CANCEL = -1;
	public final static int STATUS_FAILED = -2;
	
	//protected JTextArea textArea;
	protected JProgressBar pbCurrent, pbInterim;
	protected JLabel labelCurrent, labelInterim, labelTotal;
	protected JButton buttonOK, buttonCancel, buttonSave;
	protected JPanel panelMain, panelImage, panelProgress, panelButtons;
	protected JFrameExt owner;
	protected JScrollPane scrollPane;
	protected BufferedImage biCalib;
	protected JComponentDisplay ciCalib;
	protected File currentImageFile;
	protected boolean isImageLoaded;
	protected int status;
	
	

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	*/
	public LaminaCalibProgress(JFrameExt owner, String title)
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
	public LaminaCalibProgress(JFrameExt owner, String title, boolean setVisible)
	{
		this(owner, title, setVisible, new Dimension(600, 150) );
	}	
	
	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	* @param	setVisible	if true, the dialog is set visible upon construction.
	* @param	size	sets the dimensions of the dialog
	*/
	public LaminaCalibProgress(JFrameExt owner, String title, boolean setVisible, Dimension size)
	{
		super(owner, title);
		
		this.owner = owner;
		this.getContentPane().setLayout(new BorderLayout());

		status = STATUS_OK;
		
		pbCurrent = new JProgressBar(0, 100);
		pbInterim = new JProgressBar(0, 100);
		//pbTotal = new JProgressBar(0, 100);
		labelCurrent = new JLabel();
		labelInterim = new JLabel();
		//labelTotal = new JLabel();

		panelMain = new JPanel( new BorderLayout() );
		panelImage = new JPanel( new BorderLayout() );
		panelProgress = new JPanel( new GridLayout(4, 1) );
		//panelTop.add(labelTotal, BorderLayout.CENTER);
		//panelTop.add(pbTotal, BorderLayout.SOUTH);
		
		panelButtons = new JPanel( new GridLayout(1, 3) );

		JPanel panelInterim = new JPanel( new BorderLayout() );
		panelInterim.add(labelInterim, BorderLayout.CENTER);
		panelInterim.add(pbInterim, BorderLayout.SOUTH);

		JPanel panelBottom = new JPanel( new BorderLayout() );
		panelBottom.add(labelCurrent, BorderLayout.CENTER);
		panelBottom.add(pbCurrent, BorderLayout.SOUTH);
		
		buttonOK = new JButton("OK");
		buttonOK.addActionListener(this);
		buttonOK.setEnabled(false);
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);
		buttonSave = new JButton("Save image");
		buttonSave.addActionListener(this);
		buttonSave.setEnabled(false);
		
		panelButtons.add(buttonOK);
		panelButtons.add(buttonSave);
		panelButtons.add(buttonCancel);

		panelProgress.add( new JSeparator() );
		panelProgress.add(panelInterim);
		panelProgress.add(panelBottom);
		panelProgress.add(panelButtons);
		
		panelMain.add(panelImage, BorderLayout.CENTER);
		panelMain.add(panelProgress, BorderLayout.SOUTH);
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().add(panelMain);
		//this.getContentPane().add(panelTop);
		
		
		
		this.isImageLoaded = false;

		//this.addWindowListener(this);
		this.setDefaultCloseOperation(JFrameExt.DO_NOTHING_ON_CLOSE);
		this.pack();
		//this.setBounds(0, 0, 400, 150);
		this.setSize(size);
		this.setLocationRelativeTo(null);
		this.setLocationRelativeTo(owner);
		this.setVisible(setVisible);
	}

	/**
	* Gets the label that displays info regarding the 'current' progress.
	*
	* @return	the label that displays the 'current' progress.
	*/
	public JLabel getLabelCurrent()
	{
		return this.labelCurrent;
	}

	/**
	* Gets the label that displays info regarding the 'interim' progress.
	*
	* @return	the label that displays the 'interim' progress.
	*/
	public JLabel getLabelInterim()
	{
		return this.labelInterim;
	}

	
	/**
	* Gets the progressbar that displays info regarding the 'current' progress.
	*
	* @return	the progressbar that displays the 'current' progress.
	*/
	public JProgressBar getPBCurrent()
	{
		return this.pbCurrent;
	}
	
	public JButton getButtonOK()
	{
		return this.buttonOK;
	}
	
	public JButton getButtonSave()
	{
		return this.buttonSave;
	}
	
	public JButton getButtonCancel()
	{
		return this.buttonCancel;
	}



	/**
	* Gets the progressbar that displays info regarding the 'interim' progress.
	*
	* @return	the progressbar that displays the 'interim' progress.
	*/
	public JProgressBar getPBInterim()
	{
		return this.pbInterim;
	}

	public void setImage(BufferedImage img)
	{
		this.setImage(img, null);
	}
	

	public void setImage(BufferedImage img, String imageFileName)
	{
	
		this.biCalib = img;
		if (imageFileName != null)
			this.currentImageFile = new File(imageFileName);
		
		if (ciCalib == null)
			this.ciCalib = new JComponentDisplay();
		
		ciCalib.set(biCalib);
		
		if (!isImageLoaded)
		{
		
			//double spHeight = scrollPane.getViewport().getViewRect().getHeight();
			//double spWidth = scrollPane.getViewport().getViewRect().getWidth();
			double spHeight = panelImage.getHeight()-20;
			double spWidth = panelImage.getWidth()-20;
			double scaleHeight = spHeight / (double)biCalib.getHeight();
			double scaleWidth = spWidth / (double)biCalib.getWidth();
						
			//auto-scale window so that entire image can be seen
			float zoomLevel = (float)Math.min(scaleHeight, scaleWidth);
			ciCalib.setAutoScaleType(zoomLevel);
						
			
			scrollPane = new JScrollPane(ciCalib);
			//scrollPane.getVerticalScrollBar().setMaximum( imgGreen.getHeight() );
			//scrollPane.getVerticalScrollBar().setUnitIncrement( (int)(biCalib.getHeight()/(double)20.0) );
			//scrollPane.getHorizontalScrollBar().setMaximum( imgGreen.getWidth() );
			//scrollPane.getHorizontalScrollBar().setUnitIncrement( (int)(biCalib.getWidth()/(double)20.0) );
											
			//zoomLevel = 1.0f;
					
			panelImage.add(scrollPane, BorderLayout.CENTER);
			isImageLoaded = true;
		}
		
		panelImage.validate();
	}
				
	public int getStatus()
	{
		return this.status;
	}
	
	
	public void abortProgress()
	{
		this.actionPerformed( new ActionEvent(buttonCancel, 0, "") );
	}
	
	/**
	* 'Catch-all' function for various events
	* @param	e	An ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonCancel)
		{
			status = STATUS_CANCEL;
			
			owner.setCancelled(true);
			buttonCancel.setText("Cancelling...");
			buttonCancel.setEnabled(false);
			
			owner.setRunning(false);
			owner.setEnabled(true);
			
			this.dispose();
			this.setVisible(false);
			
		} else if (e.getSource() == buttonOK)
		{
			status = STATUS_OK;
			
			owner.setCancelled(false);
			owner.setRunning(false);
			owner.setEnabled(true);
			
			this.dispose();
			this.setVisible(false);
			
		} else if (e.getSource() == buttonSave)
		{
			//System.err.println("Save click...");
			if (currentImageFile == null)
				currentImageFile = new File(".");
			
			
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			jfc.setFileFilter(new FileFilterImage() );
			jfc.setCurrentDirectory(currentImageFile);
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			
			//create a new filename
			String str = currentImageFile.getName();
			String[] parts = str.split("\\.");
			String newStr = new String(parts[0]);
			for (int i = 1; i < (parts.length-1); i++)
				newStr = newStr  + "." + parts[i];
			String ext = parts[parts.length-1];
			
			String fName = currentImageFile.getParentFile().getAbsolutePath() + "/" + newStr + "_calib." + ext;
			System.err.println(fName);
			File fileSave = new File(fName);
			
			jfc.setSelectedFile(fileSave);
			
			int retVal = jfc.showDialog(owner, "Save image");
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				File file = jfc.getSelectedFile();
				//lastOutDir = file.getParentFile();
				String filename = new String(file.getAbsolutePath() );

				boolean doSave = true;
				if (file.isFile())
				{
					int retCode = JOptionPane.showConfirmDialog(owner,
						"Overwrite file " + file.getName() + "?",
						"Overwrite confirmation",
						JOptionPane.YES_NO_OPTION);
					
					if (retCode != JOptionPane.YES_OPTION)
					{
						doSave = false;
						System.err.println("File saving aborted");
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
					
					
					JAI.create("filestore", ciCalib.get(), filename, codecId);
			
					System.err.println("done");
				}
			}
				
				
			
		}
	}
	
	
	public void windowClosing(WindowEvent e)
	{ 

	}
	
	public void windowDeactivated(WindowEvent e) {}
	public void windowActivated(WindowEvent e)  {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}


}
