/* FilenameFilterJPEG.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A class that holds a filename filter for '*.jp?g' files
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

/**
*A FileFilter specified for the JPEG format only.
*/
public class FilenameFilterJPEG implements FilenameFilter
{
	Pattern regex;
	
	/**
	* Main constructor.
	*/
	FilenameFilterJPEG()
	{
		regex = Pattern.compile("^[\\S\\s]*.jp[e]*g$", Pattern.CASE_INSENSITIVE);
	}
	
	/**
	* Function that is called whenever a decision has to be made whether a putative filename
	* is accepted or not.
	*
	* @param	dir	The current directory
	* @param	name	The file name
	* @return	True if 'file' is accepted by the filter, false otherwise
	*/
	public boolean accept(File dir, String name)
	{
		String f = new File(name).getName();

		Matcher regexMatch = regex.matcher(f);		
		return (regexMatch.matches());
	}
}
