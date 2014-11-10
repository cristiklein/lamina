/* FilenameFilterImage
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A class that holds a filename filter for image files
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
import java.util.regex.*;

class FilenameFilterImage implements FilenameFilter
{
	String[] acceptedFormats = {"JPG", "JPEG", "TIF", "TIFF", "BMP", "GIF"};
	String acceptedDescr = new String("Image formats (*.jpg; *.tif; *.bmp; *.gif)");
	
	public boolean accept(File file, String str)
	{
		
		String[] delim = str.split("\\.");
		String ext = delim[delim.length-1];
	
		boolean accepted = false;
		for (int i = 0; (i < acceptedFormats.length && !accepted); i++)
			if (ext.equalsIgnoreCase(acceptedFormats[i]))
				accepted = true;
		
		return accepted;
	}
}
