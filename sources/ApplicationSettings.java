/* ApplicationSettings.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * An object that contains various settings and read/write
 * functions to store into INI files.
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
import java.util.EventListener;
import java.beans.*;
import java.lang.Math.*;
import java.awt.geom.*;
import java.awt.*;


/**
* Class that contains various parameter settings that can be read or written to INI files.
* The variables of the class are designed to be accessed implicitly from a GUI.
*/
public class ApplicationSettings
{
	protected File inputDir, outputDir, calibFile;
	protected double minObjSizeRel, minObjDensRel;
	protected boolean forceOrtho, forceHorizVert, thresholdSearchGreedy, thresholdSearchExhaustive, findContour,
		batchWriteLogFile, batchWriteCroppedImage, batchWriteFullImage, batchWriteLocalStatFile;
	protected int pixelContourThresh;
	protected int numLandmarks;
	protected int settingGUITab;
	protected double thresholdSearchStepLength;
	protected double thresholdManual;
	protected double scaleParam; //scales from pixels to mm
	protected double calibHeight, calibWidth;
	protected File iniFile;
	protected Point windowLocation;
	protected Dimension windowSize;
	
	
	protected Properties iniProperties;
	protected String applName;

	
	/**
	* Main constructor.
	*
	* @param	applicationName	Name of the application, used for internal reference
	*/
	ApplicationSettings(String applicationName)
	{
		inputDir = new File(".");
		outputDir = new File(".");
		calibFile = new File(".");
		minObjSizeRel = 0.05;
		minObjDensRel = 10.0;
		forceOrtho = true;
		forceHorizVert = true;
		pixelContourThresh = 10;
		thresholdSearchGreedy = true;
		thresholdSearchExhaustive = false;
		findContour = true;
		thresholdSearchStepLength = 10;
		scaleParam = 1.0;
		thresholdManual = 100;
		settingGUITab = 0;
		calibHeight = 10.0;
		calibWidth = 10.0;
		numLandmarks = 50;
		
		batchWriteLogFile = true;
		batchWriteCroppedImage = true;
		batchWriteFullImage = false;
		batchWriteLocalStatFile = false;
		
		applName = applicationName;
	}
	
	/**
	* Reads an INI-file and stores all the values in the INI file in the variables supported
	* by the object.
	*
	* @param	iniFile	The INI file name
	* @return	True if successful, otherwise false.
	*/
	public boolean readIni(File iniFile)
	{
		boolean retValue = true;
		
		this.iniFile = iniFile;
		this.iniProperties = new Properties();
	
		// make sure the INI file exists
		if (iniFile == null || !iniFile.exists())
			return false;
		else
		{
			try
			{

				iniProperties.load(new FileInputStream(iniFile.getAbsolutePath()));


			} catch (FileNotFoundException ex)
			{
				System.err.println("Unable to find INI file " + iniFile.getAbsolutePath() );
				return false;

			} catch (IOException ex)
			{
				System.err.println("Unable to read INI file " + iniFile.getAbsolutePath() );
				return false;
			}

		}
	
	
		
		String ifWindowLocation = iniProperties.getProperty("WindowLocation");
		try
		{
			if (ifWindowLocation != null)
			{
				String[] ifWindowLocations = ifWindowLocation.split(",");
				windowLocation = new Point( Math.max(0, (new Integer(ifWindowLocations[0])).intValue()),
					Math.max(0, (new Integer(ifWindowLocations[1])).intValue() ) );
				//System.err.println("Window location: " + p);
				//frame.setLocation(windowLocation);
			}

		} catch (Exception ex)
		{
			System.err.println("ifWindowLocation");
			retValue = false;
		}


		String ifWindowSize = iniProperties.getProperty("WindowSize");
		try
		{
			if (ifWindowSize != null)
			{
				String[] ifWindowSizes = ifWindowSize.split(",");
				windowSize = new Dimension( Math.max(20, (new Double(ifWindowSizes[0])).intValue()),
					Math.max(20, (new Double(ifWindowSizes[1])).intValue() ) );
				//frame.setSize(windowSize);
			}

		} catch (Exception ex)
		{
			System.err.println("ifWindowSize");
			retValue = false;
		}
		

	
		String ifLastInDir = iniProperties.getProperty("LastInDir");
		try
		{
			if (ifLastInDir != null)
			{
				inputDir = new File(ifLastInDir);
			}

		} catch (Exception ex)
		{
			System.err.println("ifLastInDir");
			retValue = false;
		}
		
		String ifLastOutDir = iniProperties.getProperty("LastOutDir");
		try
		{
			if (ifLastOutDir != null)
			{
				outputDir = new File(ifLastOutDir);
			}

		} catch (Exception ex)
		{
			System.err.println("ifLastOutDir");
			retValue = false;
		}
		
		String ifCalibFile = iniProperties.getProperty("CalibFile");
		try
		{
			if (ifCalibFile != null)
			{
				calibFile = new File(ifCalibFile);
			}

		} catch (Exception ex)
		{
			System.err.println("ifCalibFile");
			retValue = false;
		}


		String ifPixelContourThresh = iniProperties.getProperty("PixelContourThresh");
		try
		{
			if (ifPixelContourThresh != null)
			{
				pixelContourThresh = (new Integer(ifPixelContourThresh)).intValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifPixelContourThresh");
			retValue = false;
		}
		
		String ifNumLandmarks = iniProperties.getProperty("NumLandmarks");
		try
		{
			if (ifNumLandmarks != null)
			{
				numLandmarks = (new Integer(ifNumLandmarks)).intValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifNumLandmarks");
			retValue = false;
		}
		
		String ifSettingGUITab = iniProperties.getProperty("SettingGUITab");
		try
		{
			if (ifSettingGUITab != null)
			{
				settingGUITab = (new Integer(ifSettingGUITab)).intValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifSettingGUITab");
			retValue = false;
		}

		String ifForceHorizVert = iniProperties.getProperty("ForceHorizVert");
		try
		{
			if (ifForceHorizVert != null)
			{
				int val = (new Integer(ifForceHorizVert)).intValue();
				forceHorizVert = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifForceHorizVert");
			retValue = false;
		}
		
		String ifForceOrtho = iniProperties.getProperty("ForceOrtho");
		try
		{
			if (ifForceOrtho != null)
			{
				int val = (new Integer(ifForceOrtho)).intValue();
				forceOrtho = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifForceOrtho");
			retValue = false;
		}
		
		String ifThresholdSearchGreedy = iniProperties.getProperty("ThresholdSearchGreedy");
		try
		{
			if (ifThresholdSearchGreedy != null)
			{
				int val = (new Integer(ifThresholdSearchGreedy)).intValue();
				thresholdSearchGreedy = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifThresholdSearchGreedy");
			retValue = false;
		}
		
		String ifThresholdSearchExhaustive = iniProperties.getProperty("ThresholdSearchExhaustive");
		try
		{
			if (ifThresholdSearchExhaustive != null)
			{
				int val = (new Integer(ifThresholdSearchExhaustive)).intValue();
				thresholdSearchExhaustive = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifThresholdSearchExhaustive");
			retValue = false;
		}
		
		String ifFindContour = iniProperties.getProperty("FindContour");
		try
		{
			if (ifFindContour != null)
			{
				int val = (new Integer(ifFindContour)).intValue();
				findContour = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifFindContour");
			retValue = false;
		}
		
		String ifCalibHeight = iniProperties.getProperty("CalibHeight");
		try
		{
			if (ifCalibHeight != null)
			{
				calibHeight = (new Double(ifCalibHeight)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifCalibHeight");
			retValue = false;
		}

		String ifCalibWidth = iniProperties.getProperty("CalibWidth");
		try
		{
			if (ifCalibWidth != null)
			{
				calibWidth = (new Double(ifCalibWidth)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifCalibWidth");
			retValue = false;
		}
		
		String ifMinObjSizeRel = iniProperties.getProperty("MinObjSizeRel");
		try
		{
			if (ifMinObjSizeRel != null)
			{
				minObjSizeRel = (new Double(ifMinObjSizeRel)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifMinObjSizeRel");
			retValue = false;
		}

		String ifMinObjDensRel = iniProperties.getProperty("MinObjDensRel");
		try
		{
			if (ifMinObjDensRel != null)
			{
				minObjDensRel = (new Double(ifMinObjDensRel)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifMinObjDensRel");
			retValue = false;
		}
		
		
		String ifThresholdSearchStepLength = iniProperties.getProperty("ThresholdSearchStepLength");
		try
		{
			if (ifThresholdSearchStepLength != null)
			{
				thresholdSearchStepLength = (new Double(ifThresholdSearchStepLength)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifThreshSearchStepLength");
			retValue = false;
		}
		
		
		String ifScaleParam = iniProperties.getProperty("ScaleParam");
		try
		{
			if (ifScaleParam != null)
			{
				scaleParam = (new Double(ifScaleParam)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifScaleParam");
			retValue = false;
		}
		
		String ifThresholdManual = iniProperties.getProperty("ThresholdManual");
		try
		{
			if (ifThresholdManual != null)
			{
				thresholdManual = (new Double(ifThresholdManual)).doubleValue();
			}

		} catch (Exception ex)
		{
			System.err.println("ifThresholdManual");
			retValue = false;
		}
		
		String ifBatchWriteLogFile = iniProperties.getProperty("BatchWriteLogFile");
		try
		{
			if (ifBatchWriteLogFile != null)
			{
				int val = (new Integer(ifBatchWriteLogFile)).intValue();
				batchWriteLogFile = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifBatchWriteLogFile");
			retValue = false;
		}
		
		String ifBatchWriteCroppedImage = iniProperties.getProperty("BatchWriteCroppedImage");
		try
		{
			if (ifBatchWriteCroppedImage != null)
			{
				int val = (new Integer(ifBatchWriteCroppedImage)).intValue();
				batchWriteCroppedImage = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifBatchWriteCroppedImage");
			retValue = false;
		}
		
		String ifBatchWriteFullImage = iniProperties.getProperty("BatchWriteFullImage");
		try
		{
			if (ifBatchWriteFullImage != null)
			{
				int val = (new Integer(ifBatchWriteFullImage)).intValue();
				batchWriteFullImage = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifBatchWriteFullImage");
			retValue = false;
		}
		
		String ifBatchWriteLocalStatFile = iniProperties.getProperty("BatchWriteLocalStatFile");
		try
		{
			if (ifBatchWriteLocalStatFile != null)
			{
				int val = (new Integer(ifBatchWriteLocalStatFile)).intValue();
				batchWriteLocalStatFile = (val != 0);
			}

		} catch (Exception ex)
		{
			System.err.println("ifBatchWriteLocalStatFile");
			retValue = false;
		}

		return retValue;

	}
	
	/**
	* Writes the contents of the variables in the object to an INI file.
	*
	* @param	iniFile	The INI file name
	* @return	True if successful, otherwise false.
	*/
	public boolean writeIni(File iniFile)
	{
		this.iniFile = iniFile;
		this.iniProperties = new Properties();
		
		return this.writeIni();
	}
	
	/**
	* Writes the contents of the variables in the object to the last read INI file.
	*
	* @return	True if successful, otherwise false.
	*/
	public boolean writeIni()
	{
		boolean retValue = true;
	
		try
		{

			iniProperties.put("WindowSize", windowSize.getWidth() + "," + windowSize.getHeight() );
			iniProperties.put("WindowLocation", (int)windowLocation.getX() + ","
				+ (int)windowLocation.getY() );

			iniProperties.put("LastOutDir", outputDir.getAbsolutePath() );
			iniProperties.put("LastInDir", inputDir.getAbsolutePath() );
			iniProperties.put("CalibFile", calibFile.getAbsolutePath() );



			iniProperties.put("PixelContourThresh", (new Integer(pixelContourThresh)).toString() );
			iniProperties.put("NumLandmarks", (new Integer(numLandmarks)).toString() );
			iniProperties.put("SettingGUITab", (new Integer(settingGUITab)).toString() );

			iniProperties.put("ForceHorizVert", forceHorizVert ? "1" : "0");
			iniProperties.put("ForceOrtho", forceOrtho ? "1" : "0");
			iniProperties.put("ThresholdSearchGreedy", thresholdSearchGreedy ? "1" : "0");
			iniProperties.put("ThresholdSearchExhaustive", thresholdSearchExhaustive ? "1" : "0");
			iniProperties.put("FindContour", findContour ? "1" : "0");
			
			iniProperties.put("CalibHeight", (new Double(calibHeight)).toString() );
			iniProperties.put("CalibWidth", (new Double(calibWidth)).toString() );
			iniProperties.put("MinObjSizeRel", (new Double(minObjSizeRel)).toString() );
			iniProperties.put("MinObjDensRel", (new Double(minObjDensRel)).toString() );
			iniProperties.put("ThresholdSearchStepLength", (new Double(thresholdSearchStepLength)).toString() );
			iniProperties.put("ScaleParam", (new Double(scaleParam)).toString() );
			iniProperties.put("ThresholdManual", (new Double(thresholdManual)).toString() );
			
			iniProperties.put("BatchWriteLogFile", batchWriteLogFile ? "1" : "0");
			iniProperties.put("BatchWriteFullImage", batchWriteFullImage ? "1" : "0");
			iniProperties.put("BatchWriteCroppedImage", batchWriteCroppedImage ? "1" : "0");
			iniProperties.put("BatchWriteLocalStatFile", batchWriteLocalStatFile ? "1" : "0");
			
			FileOutputStream out = new FileOutputStream(iniFile.getAbsolutePath() );
			iniProperties.store(out, applName);

		} catch (Exception ex)
		{
			System.err.println("INI file writing failed");
			ex.printStackTrace();
			retValue = false;
		}

		return retValue;
	}
	
	/* get /set */
	
	public Point getWindowLocation()
	{
		return windowLocation;
	}
	
	public Dimension getWindowSize()
	{
		return windowSize;
	}
	
	
	public File getInputDir()
	{
		return inputDir;
	}
	
	public File getOutputDir()
	{
		return outputDir;
	}
	
	public File getCalibFile()
	{
		return calibFile;
	}
	
	public double getCalibHeight()
	{
		return calibHeight;
	}
	
	public double getCalibWidth()
	{
		return calibWidth;
	}
	
	
	public double getMinObjSizeRel()
	{
		return minObjSizeRel;
	}
	
	public double getMinObjDensRel()
	{
		return minObjDensRel;
	}
	
	public boolean getForceHorizVert()
	{
		return forceHorizVert;
	}
	
	public boolean getForceOrtho()
	{
		return forceOrtho;
	}
	
	public int getPixelContourThresh()
	{
		return pixelContourThresh;
	}	

	public int getNumLandmarks()
	{
		return numLandmarks;
	}
	
	public int getSettingGUITab()
	{
		return settingGUITab;
	}
	
	public boolean getThresholdSearchGreedy()
	{
		return thresholdSearchGreedy;
	}
	
	public boolean getThresholdSearchExhaustive()
	{
		return thresholdSearchExhaustive;
	}
	
	public boolean getFindContour()
	{
		return findContour;
	}

	public double getThresholdSearchStepLength()
	{
		return thresholdSearchStepLength;
	}
	
	public double getThresholdManual()
	{
		return thresholdManual;
	}
	
	// pixels/mm
	public double getScaleParam()
	{
		return scaleParam;
	}
	
	public boolean getBatchWriteLogFile()
	{
		return batchWriteLogFile;
	}
	
	public boolean getBatchWriteCroppedImage()
	{
		return batchWriteCroppedImage;
	}
	
	public boolean getBatchWriteFullImage()
	{
		return batchWriteFullImage;
	}
	
	
	public boolean getBatchWriteLocalStatFile()
	{
		return batchWriteLocalStatFile;
	}
	
	
	public void setInputDir(File inputDir)
	{
		this.inputDir = inputDir;
	}
	
	public void setOutputDir(File outputDir)
	{
		this.outputDir = outputDir;
	}
	
	public void setCalibFile(File calibFile)
	{
		this.calibFile = calibFile;
	}
	
	public void setCalibHeight(double calibHeight)
	{
		this.calibHeight = calibHeight;
	}
	
	public void setCalibWidth(double calibWidth)
	{
		this.calibWidth = calibWidth;
	}
	
	public void setMinObjSizeRel(double minObjSizeRel)
	{
		this.minObjSizeRel = minObjSizeRel;
	}
	
	public void setMinObjDensRel(double minObjDensRel)
	{
		this.minObjDensRel = minObjDensRel;
	}
	
	
	public void setPixelContourThresh(int pixelContourThresh)
	{
		this.pixelContourThresh = pixelContourThresh;
	}
	
	public void setNumLandmarks(int numLandmarks)
	{
		this.numLandmarks = numLandmarks;
	}
	
	public void setSettingGUITab(int settingGUITab)
	{
		this.settingGUITab = settingGUITab;
	}
	
	public void setForceHorizVert(boolean forceHorizVert)
	{
		this.forceHorizVert = forceHorizVert;
	}
	
	
	public void setForceOrtho(boolean forceOrtho)
	{
		this.forceOrtho = forceOrtho;
	}
	
	public void setThresholdSearchGreedy(boolean thresholdSearchGreedy)
	{
		this.thresholdSearchGreedy = thresholdSearchGreedy;
	}
	
	public void setThresholdSearchExhaustive(boolean thresholdSearchExhaustive)
	{
		this.thresholdSearchExhaustive = thresholdSearchExhaustive;
	}

	public void setThresholdSearchStepLength(double thresholdSearchStepLength)
	{
		this.thresholdSearchStepLength = thresholdSearchStepLength;
	}
	
	public void setFindContour(boolean findContour)
	{
		this.findContour = findContour;
	}
	
	public void setThresholdManual(double thresholdManual)
	{
		this.thresholdManual = thresholdManual;
	}
	
	// pixels/mm
	public void setScaleParam(double scaleParam)
	{
		this.scaleParam = scaleParam;
	}
	
	public void setWindowLocation(Point windowLocation)
	{
		this.windowLocation = windowLocation;
	}
	
	public void setWindowSize(Dimension windowSize)
	{
		this.windowSize = windowSize;
	}
	
	public void setBatchWriteLogFile(boolean batchWriteLogFile)
	{
		this.batchWriteLogFile = batchWriteLogFile;
	}
	
	public void setBatchWriteLocalStatFile(boolean batchWriteLocalStatFile)
	{
		this.batchWriteLocalStatFile = batchWriteLocalStatFile;
	}
	
	public void setBatchWriteCroppedImage(boolean batchWriteCroppedImage)
	{
		this.batchWriteCroppedImage = batchWriteCroppedImage;
	}

	public void setBatchWriteFullImage(boolean batchWriteFullImage)
	{
		this.batchWriteFullImage = batchWriteFullImage;
	}
	
	
	public void setIni(File iniFile)
	{
		this.iniFile = iniFile;
	}
		
	
}
