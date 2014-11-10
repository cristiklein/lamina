/* JDialogExtract.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A general GUI to visualize two parallel progresses:
 * 'current' and 'interim'.
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
import javax.swing.*;
import javax.swing.event.*;


/**
    * A general GUI to visualize three parallel progresses:
    * 'current', 'interim' and 'overall'.
*/
public class JDialogExtract extends JDialog implements ActionListener
{
	//protected JTextArea textArea;
	protected JProgressBar pbCurrent, pbInterim;
	protected JLabel labelCurrent, labelInterim;
	protected JButton buttonCancel;
	protected JFrameExt owner;

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	*/
	public JDialogExtract(JFrameExt owner, String title)
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
	public JDialogExtract(JFrameExt owner, String title, boolean setVisible)
	{
		this(owner, title, setVisible, new Dimension(400, 150) );
	}	
	
	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	* @param	setVisible	if true, the dialog is set visible upon construction.
	* @param	size	sets the dimensions of the dialog
	*/
	public JDialogExtract(JFrameExt owner, String title, boolean setVisible, Dimension size)
	{
		super(owner, title);
		
		this.owner = owner;
		this.getContentPane().setLayout(new BorderLayout());

		pbCurrent = new JProgressBar(0, 100);
		pbInterim = new JProgressBar(0, 100);
		
		labelCurrent = new JLabel();
		labelInterim = new JLabel();
		
		

		//JPanel panelTop = new JPanel( new BorderLayout() );
		//panelTop.add(labelTotal, BorderLayout.CENTER);
		//panelTop.add(pbTotal, BorderLayout.SOUTH);

		JPanel panelInterim = new JPanel( new BorderLayout() );
		panelInterim.add(labelInterim, BorderLayout.CENTER);
		panelInterim.add(pbInterim, BorderLayout.SOUTH);

		JPanel panelBottom = new JPanel( new BorderLayout() );
		panelBottom.add(labelCurrent, BorderLayout.CENTER);
		panelBottom.add(pbCurrent, BorderLayout.SOUTH);
		
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);

		this.getContentPane().setLayout( new GridLayout(3, 1) );
		//this.getContentPane().add(panelTop);
		this.getContentPane().add(panelInterim);
		this.getContentPane().add(panelBottom);
		this.getContentPane().add(buttonCancel);


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

	/**
	* Gets the progressbar that displays info regarding the 'interim' progress.
	*
	* @return	the progressbar that displays the 'interim' progress.
	*/
	public JProgressBar getPBInterim()
	{
		return this.pbInterim;
	}

	/**
	* 'Catch-all' function for various events
	* @param	e	An ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonCancel)
		{
			owner.setCancelled(true);
			buttonCancel.setText("Cancelling...");
			buttonCancel.setEnabled(false);
			
		}
	}
	

}
