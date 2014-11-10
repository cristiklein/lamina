/* Misc.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A class that contains miscellaneous functions.
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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;


/**
    * A class the contains a collection of various function.
*/
public class Misc
{
	/**
	* A function that checks if a string is numeric
	* by checking the string character-wise.
	* The allowed characters are [0-9.,- ]
	*
	* @param 	str	The string to check
	* @return	true if numeric, false otherwise
	*/
	public static boolean isNumeric(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if(! (Character.isDigit(c) || c == '.' || c == ',' || c == '-' || c == ' '))
				return false;
		}
        	return true;
	}


	/**
	* A function that puts double quotation marks
	* around alpha-numeric strings.
	*
	* @param	str	The string to quote
	* @return	the quoted string if alpha-numeric, the original string otherwise
	*/
	public static String quoteAlphaNumeric(String str)
	{
		if (!isNumeric(str))
			return ("\"" + str + "\"");
		else
			return str;
	}


	/**
	* A function that removes surrounding quotation marks from a string.
	* The function assumes that the string is trimmed (i.e. contains
	* no leading/trailing whitespace) and handles both single and
	* double quotes.
	*
	* @param	str	The string to unquote
	* @return	the unquoted string
	*/
	public static String unquote(String str)
	{
		if ( str.length() > 1 && ( (str.startsWith("\"")) && (str.endsWith("\""))) || ((str.startsWith("\'")) && (str.endsWith("\'"))) )
			return(str.substring(1, str.length()-1));
		else
			return(str);
	}

	/**
	* A function that determines if a file exists (and is readable)
	*
	* @param	filename		The full path
	* @return	true if the file exists, false otherwise
	*/
	public static boolean fileExists(String filename)
	{
		boolean result = true;
		try
		{
			FileReader fr = new FileReader(filename);
		} catch (FileNotFoundException e)
		{
			result = false;
		}

		return(result);
	}


	/**
	* A function that extracts the directory from a full path
	*
	* @param	fName		The full path
	* @return	The extracted directory
	*/
	public static String extractDirectory(String fName)
	{
		String path = new String();

		//support both Windows and Unix file path styles
		int pathEndWindows = fName.lastIndexOf('\\');
		int pathEndUnix = fName.lastIndexOf('/');

		//extract path and filename
		if (pathEndWindows != -1)
		{
			path = new String(fName.substring(0, pathEndWindows) + '\\');
		} else if (pathEndUnix != -1)
		{
			path = new String(fName.substring(0, pathEndUnix) + '/');
		}

		return(path);
	}

	/**
	* A function that extracts the filename from a full path
	*
	* @param 	fName		The full path
	* @return	The extracted filename
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
	* A function that removes all whitespace from a string
	* using regular expressions.
	*
	* @param	str	The string from which to remove whitespace
	* @return	the modified string
	*/
	public static String removeAllWhitespace(String str)
	{
		String result = new String(str);

       		result = result.replaceAll("\\s+", "");

		return(result);
	}

	/**
	* A function that replaces a file extension (.*)
	* with a new extension.
	*
	* @param	str	The string where the extension should be replaced
	* @param	ext	The new extension
	* @return	the modified string
	*/
	public static String replaceExtension(String str, String ext)
	{
		String[] parts = str.split("\\.");
		String newStr = new String(parts[0]);
		for (int i = 1; i < (parts.length-1); i++)
			newStr = newStr  + "." + parts[i];

		return (newStr + "." + ext);
	}

	/**
	* A function that removes a file extension (.*)
	* from a string
	*
	* @param	str	The string where the extension should be removed
	* @return	the modified string
	*/
	public static String removeExtension(String str)
	{
		String[] parts = str.split("\\.");
		String newStr = new String(parts[0]);
		for (int i = 1; i < (parts.length-1); i++)
			newStr = newStr + "." + parts[i];

		return (newStr);
	}



	/**
	* Adds a line of text to a TextPane containing log messages.
	*
	* @param	tp	A textpane containing log text.
	* @param	str	The text to be added to the textpane
	* @param	messages	A vector of String object, containing a history of the added messages
	*/
	public static void addMessage(JTextPane tp, String str, Vector messages)
	{
		if (!str.endsWith("\n"))
			str += "\n";

		try
		{
			if (tp != null)
			{
				StyledDocument doc = (StyledDocument)tp.getDocument();
				doc.insertString(doc.getLength(), str, doc.getStyle("Normal"));
			}

			if (messages != null)
				messages.add(str);
			//System.out.print(str);
		} catch (BadLocationException e)
		{
		}
	}
	
	/**
	* Adds a line of text to a TextPane containing log messages.
	*
	* @param	tp	A textpane containing log text.
	* @param	str	The text to be added to the textpane
	* @param	messages	A vector of String object, containing a history of the added messages
	* @param	c	Color to set to the text
	*/
	public static void addMessage(JTextPane tp, String str, Vector messages, Color c)
	{
		if (!str.endsWith("\n"))
			str += "\n";

		try
		{
			if (tp != null)
			{
				MutableAttributeSet attrs = tp.getInputAttributes();
				StyledDocument doc = (StyledDocument)tp.getDocument();
				
				StyleConstants.setForeground(attrs, c);
				
				
				
				doc.insertString(doc.getLength(), str, doc.getStyle("Normal"));
				doc.setCharacterAttributes(doc.getLength()-str.length(), doc.getLength(), attrs, false);
				
			}

			if (messages != null)
				messages.add(str);
			//System.out.print(str);
		} catch (BadLocationException e)
		{
		}
	}


	/**
	* Adds a line of text to a TextPane containing log errors.
	*
	* @param	tp	A textpane containing log errors.
	* @param	str	The error text to be added to the textpane
	* @param	errors	A vector of String object, containing a history of the added messages
	*/
	public static void addError(JTextPane tp, String str, Vector errors)
	{
		if (!str.endsWith("\n"))
			str += "\n";

		try
		{
			if (tp != null)
			{
				StyledDocument doc = (StyledDocument)tp.getDocument();
				doc.insertString(doc.getLength(), str, doc.getStyle("Error"));
			}

			if (errors != null)
				errors.add(str);
				
			//System.err.print(str);
		} catch (BadLocationException e)
		{
		}
	}
	
	/**
	* Pads int with leading zeros and converts to string
	*
	* @param	i	The integer to be padded
	* @param	strLength	The output string length, after concatenation
	* @return	The string, with leading zeros (if applicable), with output length 'strLength'
	*/
	public static String padLeadingZeros(int i, int strLength)
	{
		return padLeadingZeros("" + i, strLength);
	}


	
	/**
	* Pads string with leading zeros
	*
	* @param	str	The string to be padded
	* @param	strLength	The output string length, after concatenation
	* @return	The string, with leading zeros (if applicable), with output length 'strLength'
	*/
	public static String padLeadingZeros(String str, int strLength)
	{
		String outStr = new String(str);
		while (outStr.length() < strLength)
			outStr = "0" + outStr;
		
		return outStr;
	}


}
