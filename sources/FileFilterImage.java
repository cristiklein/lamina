/* FileFilterImage.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class that holds a filename filter for common image file
 * extensions, e.g. jpeg, tif, bmp and gif.
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

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;


/**
*A FileFilter specified for common image formats (JPEG, TIF, BMP and GIF).
*/
public class FileFilterImage extends FileFilter
{
	String[] acceptedFormats = {"JPG", "JPEG", "TIF", "TIFF", "BMP", "GIF"};
	String acceptedDescr = new String("Image formats (*.jpg; *.tif; *.bmp; *.gif)");
	
	/**
	* Main constructor.
	*/
	public FileFilterImage()
	{
	
	}
	
	/**
	* Function that is called whenever a decision has to be made whether a putative filename
	* is accepted or not.
	*
	* @param	file	The filename
	* @return	True if 'file' is accepted by the filter, false otherwise
	*/
	public boolean accept(File file)
	{
		if (file.isFile())
		{
			String[] delim = file.getName().split("\\.");
			String ext = delim[delim.length-1];
		
			boolean accepted = false;
			for (int i = 0; (i < acceptedFormats.length && !accepted); i++)
				if (ext.equalsIgnoreCase(acceptedFormats[i]))
					accepted = true;
			
			return accepted;
		} else if (file.isDirectory())
			return true;
		
		return false;
	}
	
	/**
	* A description of the file filter, used by the file filtering GUI.
	* The string is typically 'Image formats (*.jpg; *.tif; *.bmp; *.gif)'
	*
	* @return	A string representation of the file filter
	*/
	public String getDescription()
	{
		return acceptedDescr;
	}
}
