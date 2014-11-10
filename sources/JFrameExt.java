/* JFrameExt.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * An extension of the JFrame class, containing options to
 * set flags for starting/stopping the frame from running.
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


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
* Extension of the JFrame class with some additional parameters and functions.
*/
public class JFrameExt extends JFrame
{
	protected boolean cancelled = false;
	protected boolean error = false;
	protected boolean running = false;
	
	
	/**
	* Main constructor.
	* 
	*/
	public JFrameExt() 
	{
		super();
		this.cancelled = false;
		this.error = false;
		this.running = false;
	}
	
	/**
	* Optional constructor
	* 
	* @param	title	The title displayed on the JFrame
	*/
	public JFrameExt(String title) 
	{
		super(title);
		this.cancelled = false;
		this.error = false;
		this.running = false;
	}
	
	/**
	* Set the status of the frame to 'cancelled'
	* 
	* @param	status	The new status
	*/
	public void setCancelled(boolean status)
	{
		cancelled = status;
	}
	
	/**
	* Checks if the current status of the frame is 'cancelled'
	* 
	* @return	True if the status is 'cancelled', false otherwise
	*/
	public boolean getCancelled()
	{
		return(cancelled);
	}
	
	/**
	* Set the status of the frame to 'error'
	* 
	* @param	status	The new status
	*/
	public void setError(boolean status)
	{
		error = status;
	}
	
	/**
	* Checks if the current status of the frame is 'error'
	* 
	* @return	True if the status is 'error', false otherwise
	*/
	public boolean getError()
	{
		return(error);
	}
	
	/**
	* Set the status of the frame to 'running'
	* 
	* @param	status	The new status
	*/
	public void setRunning(boolean status)
	{
		running = status;
	}
	
	/**
	* Checks if the current status of the frame is 'running'
	* 
	* @return	True if the status is 'running', false otherwise
	*/
	public boolean getRunning()
	{
		return(running);
	}
	
}