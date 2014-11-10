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
import java.net.URL;

/**
    * A GUI to present an "about program" dialog.
*/
public class JDialogAbout extends JDialog implements ActionListener, WindowListener
{
	protected final static String APPL_NAME = Lamina.APPL_NAME;
	protected final static String APPL_VERSION = Lamina.APPL_VERSION;
	protected final static String APPL_DATE = Lamina.APPL_DATE;


	//protected JTextArea textArea;
	protected JTextPane tpAbout, tpCiting, tpLicense;
	protected JPanel panelBottom, panelTabs, panelAbout, panelCiting, panelLicense;
	protected JButton buttonClose;

	protected static boolean isDisposed = false;


	/**
	* Default constructor.
	*
	*/
	public JDialogAbout()
	{
		this( (JFrame)null);
	}

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	*/
	public JDialogAbout(JFrame owner)
	{
		this(owner, "About " + APPL_NAME);
	}

	/**
	* Optional constructor
	*
	* @param	title	title of the dialog
	*/
	public JDialogAbout(String title)
	{
		this(null, title, true);
	}

	/**
	* Optional constructor
	*
	* @param	owner	owner of the dialog
	* @param	title	title of the dialog
	*/
	public JDialogAbout(JFrame owner, String title)
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
	public JDialogAbout(JFrame owner, String title, boolean setVisible)
	{
		super(owner, title);

		this.isDisposed = false;

		this.getContentPane().setLayout(new BorderLayout());
		this.setSize(400, 480);

		tpAbout = new JTextPane();
		//textArea.getDocument().addDocumentListener(this);
		tpAbout.setEditable(false);
		tpAbout.setBorder(BorderFactory.createEmptyBorder());

		tpCiting = new JTextPane();
		//textArea.getDocument().addDocumentListener(this);
		tpCiting.setEditable(false);
		tpCiting.setBorder(BorderFactory.createEmptyBorder());

		tpLicense = new JTextPane();
		//textArea.getDocument().addDocumentListener(this);
		tpLicense.setEditable(false);
		tpLicense.setBorder(BorderFactory.createEmptyBorder());

		//predefine text styles
		StyledDocument doc = (StyledDocument)tpAbout.getDocument();
		Style styleError = doc.addStyle("Error", null);
		StyleConstants.setForeground(styleError, Color.RED);
		Style styleNormal = doc.addStyle("Normal", null);
		StyleConstants.setForeground(styleNormal, Color.BLACK);

		//add to scroll pane
		JScrollPane spAbout = new JScrollPane(tpAbout);
		JScrollPane spCiting = new JScrollPane(tpCiting);
		JScrollPane spLicense = new JScrollPane(tpLicense);

		//this.addLine(APPL_NAME + " version " + APPL_VERSION, "Error");

		SimpleAttributeSet sasDef = new SimpleAttributeSet();

		SimpleAttributeSet sasBold = new SimpleAttributeSet();
		//StyleConstants.setFontFamily(sasDef, "SansSerif");
		//StyleConstants.setFontSize(sasBold, 10);
		StyleConstants.setBold(sasBold, true);

		SimpleAttributeSet sasItalic = new SimpleAttributeSet();
		//StyleConstants.setFontFamily(sasDef, "SansSerif");
		//StyleConstants.setFontSize(sasBold, 10);
		StyleConstants.setItalic(sasItalic, true);

		SimpleAttributeSet sasBlue = new SimpleAttributeSet();
		//StyleConstants.setItalic(sasItalic, true);
		StyleConstants.setBold(sasBlue, true);
		StyleConstants.setFontSize(sasBlue, 12);
		StyleConstants.setForeground(sasBlue, Color.BLUE);
		//StyleConstants.setUnderline(sasBlue, true);


		//----- ABOUT ------
		addLine(tpAbout, APPL_NAME, sasBold);
		addLine(tpAbout, "Copyright (C) 2007-2008 Max Bylesjö", sasBold);
		addLine(tpAbout, "Version " + APPL_VERSION + ", release date: " + APPL_DATE, sasBold);
		//newLine();

		newLine(tpAbout);
		addLine(tpAbout, "Coded by Max Bylesjö <max.bylesjo@chem.umu.se>", sasDef);
		newLine(tpAbout);
		addString(tpAbout, "LAMINA ", sasBold);
		addLine(tpAbout, "(Leaf shApe deterMINAtion) is a tool for the automated analysis of "+
			"images of leaves. LAMINA has been designed to provide classical measures of leaf shape "+
			"(blade dimensions) and size (area) that are typically required for correlation analysis "+
			"to biomass productivity, as well as measures that indicate asymmetry in leaf shape, "+
			"leaf serration traits, and measures of herbivory damage (missing leaf area). "+
			"The location of a chosen number of equally spaced markers can optionally be returned "+
			" to support multivariate analysis, e.g. principal component analysis");

		newLine(tpAbout);
		


		//-------CITING----------
		/*
		addLine(tpCiting, "When using this software in publications, please cite: ", sasDef);
		newLine(tpCiting);
		addString(tpCiting, "Bylesjö M, Eriksson D, Sjödin A, Sjöström M, Jansson S, Antti H and Trygg J. ", sasDef);
		addString(tpCiting, "MASQOT: a method for cDNA microarray spot quality control. ", sasBold);
		addString(tpCiting, "BMC Bioinformatics ", sasItalic);
		addLine(tpCiting, "2005, 6:250.", sasDef);


		newLine(tpCiting);
		addLine(tpCiting, "and/or", sasDef);
		newLine(tpCiting);
		addString(tpCiting, "Bylesjö M, Sjödin A, Eriksson D, Antti H, Moritz T, Jansson S and Trygg J. ", sasDef);
		addString(tpCiting, "MASQOT-GUI: spot quality assessment for the two-channel microarray platform. ", sasBold);
		addString(tpCiting, "Bioinformatics ", sasItalic);
		addLine(tpCiting, "2006, 22(20):2554-5.", sasDef);
		*/

		//---------LICENSE---------
		addLine(tpLicense, "LAMINA is free software; you can redistribute it and/or " +
			"modify it under the terms of the GNU General Public License " +
			"version 2 as published by the Free Software Foundation", sasDef);
		newLine(tpLicense);
		addLine(tpLicense, "This program is distributed in the hope that it will be useful, " +
			"but WITHOUT ANY WARRANTY; without even the implied warranty of " +
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU " +
			"General Public License for more details. ", sasDef);
		newLine(tpLicense);
		addLine(tpLicense, "You should have received a copy of the GNU General Public " +
			"License along with this library; if not, write to the Free Software " +
			"Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA. ",
			sasDef);

		//add button
		buttonClose = new JButton("Close this window");
		buttonClose.addActionListener(this);
		//add panel

		panelBottom = new JPanel(new BorderLayout());
		panelBottom.add(buttonClose, BorderLayout.CENTER);

		//panelTabs = new JPanel( new GridLayout(1, 1) );
		panelAbout = new JPanel( new GridLayout(1, 1) );
		panelAbout.add(spAbout);

		panelCiting = new JPanel( new GridLayout(1, 1) );
		panelCiting.add(spCiting);

		panelLicense = new JPanel( new GridLayout(1, 1) );
		panelLicense.add(spLicense);

		JTabbedPane jtp = new JTabbedPane();
		jtp.addTab("About", null, panelAbout, "About this software");
		//jtp.addTab("Citing", null, panelCiting, "Citing this software in publications");
		jtp.addTab("License", null, panelLicense, "Software license");


		this.getContentPane().add(jtp, BorderLayout.CENTER);
		this.getContentPane().add(panelBottom, BorderLayout.SOUTH);

		this.setLocationRelativeTo(null); //center of the screen is better

		this.setModal(false);
		this.setVisible(setVisible);
	}


	/**
	* Gets the textpane object that stores the event- and error log.
	*
	* @return	The textpane object that stores the event- and error log.
	*/
	public JTextPane getTextPane()
	{
		return tpAbout;
	}

	/**
	* Invoked when the 'Save' button is clicked.
	*
	* @return	e	the invoked ActionEvent
	*/
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttonClose)
		{

			this.setVisible(false);
			this.dispose();
		}

	}



	/**
	* Adds a line of text to a text pane.
	*
	* @param	tp	The text pane
	* @param	str	The text to be added to the textpane
	*/
	protected static void addLine(JTextPane tp, String str)
	{
		addLine(tp, str, "Normal");
	}

	/**
	* Adds a line of text to a text pane.
	*
	* @param	tp	The text pane
	* @param	str	The text to be added to the textpane
	* @param	styleStr	A text representation of the style of the line
	*/
	protected static void addLine(JTextPane tp, String str, String styleStr)
	{
		addLine(tp, str, ( (StyledDocument)tp.getDocument()).getStyle(styleStr) );
	}


	/**
	* Adds a line of text to a text pane.
	*
	* @param	tp	The text pane
	* @param	str	The text to be added to the textpane
	* @param	style	The style of the text
	*/
	protected static void addLine(JTextPane tp, String str, Style style)
	{
		if (!str.endsWith("\n"))
			str += "\n";

		try
		{
			if (tp != null)
			{
				StyledDocument doc = (StyledDocument)tp.getDocument();
				doc.insertString(doc.getLength(), str, style);
			}

			//messages.add(str);
			//System.out.print(str);
		} catch (BadLocationException e)
		{
		}
	}

	/**
	* Adds a line of text to a text pane, with no line feed
	*
	* @param	tp	The text pane
	* @param	str	The text to be added to the textpane
	* @param	sas	The style of the text
	*/
	protected static void addString(JTextPane tp, String str, SimpleAttributeSet sas)
	{
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

	/**
	* Adds a (empty) line of text to a text pane.
	*
	* @param	tp	The text pane
	*/
	protected void newLine(JTextPane tp)
	{
		this.addLine(tp, "", "Normal");
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
    		new JDialogAbout();
	}
	*/

	public void windowClosing(WindowEvent e)
	{
		this.isDisposed = true;
	}

/**
	* Gets the state of the frame object (disposed or not).
	*
	* @return	true if the frame has been disposed, false otherwise
	*/
	public boolean isDisposed()
	{
		return this.isDisposed;
	}

	/**
	* Sets the state of the frame object (disposed or not).
	*
	* @param	disposed	true or false
	*/
	public void setDisposed(boolean disposed)
	{
		this.isDisposed = disposed;
	}




	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
}
