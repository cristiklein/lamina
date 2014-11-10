/* JFrameLog.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A GUI to visualize an event- and error log, with functionality
 * to save the log session.
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


/**
    * A GUI to visualize an event- and error log, with functionality
    * to save the log session.
*/
public class JFrameLog extends JFrame implements ActionListener
{

	//protected JTextArea textArea;
	protected JTextPane tpMessages;
	protected JPanel panelBottom;
	protected JButton buttonSave;
	protected String logSuffix;

	/**
	* Optional constructor. Automatically sets the frame visible
	* on construction.
	*
	* @param	title	title of the JFrame
	*/
	public JFrameLog(String title)
	{
		this(title, "masqot_eventlog", true);
	}

	/**
	* Optional constructor.
	*
	* @param	title	title of the JFrame
	* @param	setVisible	if true, the JFrame is set visible.
	*/
	public JFrameLog(String title, String logSuffix, boolean setVisible)
	{
		super(title);

		this.logSuffix = logSuffix;

		this.getContentPane().setLayout(new BorderLayout());
		this.setSize(400, 300);
		tpMessages = new JTextPane();
		//textArea.getDocument().addDocumentListener(this);
		tpMessages.setEditable(false);
		tpMessages.setBorder(BorderFactory.createEmptyBorder());

		//predefine text styles
		StyledDocument doc = (StyledDocument)tpMessages.getDocument();
		Style styleError = doc.addStyle("Error", null);
		StyleConstants.setForeground(styleError, Color.RED);
		Style styleNormal = doc.addStyle("Normal", null);
		StyleConstants.setForeground(styleNormal, Color.BLACK);

		//add to scroll pane
		JScrollPane spMessages = new JScrollPane(tpMessages);

		//add button
		buttonSave = new JButton("Save log file as...");
		buttonSave.addActionListener(this);
		//add panel
		panelBottom = new JPanel(new BorderLayout());
		panelBottom.add(buttonSave, BorderLayout.CENTER);

		this.getContentPane().add(spMessages, BorderLayout.CENTER);
		this.getContentPane().add(panelBottom, BorderLayout.SOUTH);

		this.setVisible(setVisible);
	}


	/**
	* Gets the textpane object that stores the event- and error log.
	*
	* @return	The textpane object that stores the event- and error log.
	*/
	public JTextPane getTextPane()
	{
		return tpMessages;
	}

	/**
	* Used to dump the contents of the log to a text file
	*
	* @param	fName	the file where the contents will be written
	* @return true if successful, otherwise false
	*/
	public boolean writeLogToFile(File fName)
	{
		return writeLogToFile(fName, tpMessages.getText());
	}
	
	/**
	* Used to dump the contents of the log to a text file
	*
	* @param	fName	the file where the contents will be written
	* @param	text	The text that will be written
	* @return true if successful, otherwise false
	*/
	public boolean writeLogToFile(File fName, String text)
	{
		boolean retVal = true;
		try
		{
			FileWriter fw = new FileWriter(fName);
			BufferedWriter bw = new BufferedWriter(fw);


			bw.write(text);
			bw.newLine();
			bw.flush();
			bw.close();
			
		} catch (IOException ioe)
		{
			retVal = false;
		}
		
		return retVal;
	}
	
	/**
	* Invoked when the 'Save' button is clicked.
	*
	* @param	e	the invoked ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonSave)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
			File newLogFile = new File(logSuffix + "_" + sdf.format(new Date()) + ".txt" );
			//FileDialog fdSave = new FileDialog(this, "Save log as..", FileDialog.SAVE);
			//fdSave.setFile();
			//fdSave.show();

			File dir = new File (".");
			JFileChooser jfc = new JFileChooser();
			//BasicFileChooserUI bfc = new BasicFileChooserUI( jfc );
			//jfc.setFileFilter(new FileFilterGrid() );
			jfc.setCurrentDirectory(dir);
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setSelectedFile( newLogFile );
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);


			int retVal = jfc.showDialog(this, "Save log as..");
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				StyledDocument doc = (StyledDocument)tpMessages.getDocument();
	
            	if ( this.writeLogToFile( jfc.getSelectedFile() ) )
				{
		
					try
					{
						doc.insertString(doc.getLength(), "Successfully wrote event log to file '" + jfc.getSelectedFile().getName() + "'.", doc.getStyle("Normal"));
					} catch (BadLocationException ble) {}

				} else
				{
					// catch possible io errors
					try
					{
						doc.insertString(doc.getLength(), "Failed when writing event log to file '" + jfc.getSelectedFile().getName() + "'.", doc.getStyle("Error"));
					} catch (BadLocationException ble) {}

				}
			}
		}

	}
}
