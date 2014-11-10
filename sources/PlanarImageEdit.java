/* PlanarImageEdit.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A class with functions to perform basic image editing
 * procedures for the PlanarImage class.
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

import java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;
import java.awt.image.renderable.ParameterBlock;
import javax.swing.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;

/**
    * A class the contains methods for  manipulating
    * PlanarImages.
*/
public class PlanarImageEdit
{
	public final static byte BAND_GREYSCALE = 0;
	public final static byte BAND_R = 0;
	public final static byte BAND_G = 1;
	public final static byte BAND_B = 2;

	public final static int PIXEL_INT_MIN = 0;
	public final static int PIXEL_INT_MAX = 65535;
	public final static int PIXEL_SCALED_INT_MIN = 0;
	public final static int PIXEL_SCALED_INT_MAX = 255;



	/**
	* Crops an image
	* 
	* @param  img	The image
	* @param	  x	The x origin of the crop area
	*  @param	  y	The y origin of the crop area
	*  @param	  w	The width of the crop area
	*  @param	  h	The height of the crop area
	* @return	The modified image
	*/	
	public static PlanarImage crop(PlanarImage img, int x, int y, int w, int h)
	{
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add( (float) x);
		pb.add( (float) y);
		pb.add( (float) w);
		pb.add( (float) h);
		
		// Create the output image by cropping the input image.
		PlanarImage output = JAI.create("crop", pb, null);
		
		return(output);
	}
	
	/**
	* Crops an image
	* 
	* @param  img	The image
	*	  rect	A rectangle desribing the crop area
	* @return	The modified image
	*/	
	public static PlanarImage crop(PlanarImage img, Rectangle rect)
	{
		return( crop(img, (int)rect.getX(), (int)rect.getY(), (int)rect.getWidth(), (int)rect.getHeight()) );
	}


	/**
	* Crops an image
	* 
	* @param  img	The image
	*	  x	The x origin of the crop area
	*	  y	The y origin of the crop area
	*	  w	The width of the crop area
	*	  h	The height of the crop area
	* @return	The modified image
	*/	
	public static PlanarImage crop(BufferedImage img, int x, int y, int w, int h)
	{
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add( (float) x);
		pb.add( (float) y);
		pb.add( (float) w);
		pb.add( (float) h);
		
		// Create the output image by cropping the input image.
		PlanarImage output = JAI.create("crop", pb, null);
		
		return(output);
	}
	
	/**
	* Crops an image
	* 
	* @param  img	The image
	*	  rect	A rectangle desribing the crop area
	* @return	The modified image
	*/	
	public static PlanarImage crop(BufferedImage img, Rectangle rect)
	{
		return( crop(img, (int)rect.getX(), (int)rect.getY(), (int)rect.getWidth(), (int)rect.getHeight()) );
	}

	
	
	/**
	* Adjusts the origin (minX, minY) of an image
	* 
	* @param  img	The image
	*	  x	The number of pixels that the x origin should be moved 
	*	  y	The number of pixels that the y origin should be moved
	* @return	The modified image
	*/	
	public static PlanarImage translate(PlanarImage img, int x, int y)
	{
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add( (float) x);
		pb.add( (float) y);

		// Shift the output image coordinates
		PlanarImage output = JAI.create("translate", pb, null);

		return(output);
	
	}
	
	/**
	* Adds padding around an image
	* 
	* @param  img	The image
	*	  lPad	The size of the padding on the left side
	*	  rPad	The size of the padding on the right side
	*	  tPad	The size of the padding on the top
	*	  bPad	The size of the padding on the bottom
	*	  clr	The color of the padding
	* @return	The modified image
	*/	
	public static PlanarImage addPadding(PlanarImage img, int lPad, int rPad, int tPad, int bPad, Color clr)
	{
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(img);
		pb.add(lPad);
		pb.add(rPad);
		pb.add(tPad);
		pb.add(bPad);

		//adjust the color depending of the number of bands
		int numBands = img.getSampleModel().getNumBands();
		double[] fillValue = new double[numBands];
		if (numBands == 3)
		{
			fillValue[0] = clr.getRed();
			fillValue[1] = clr.getGreen();
			fillValue[2] = clr.getBlue();
		} else
		{
			for (int i = 0; i < numBands; i++)
				fillValue[i] = (clr.getRed() + clr.getGreen() + clr.getBlue()) /3;
		}

		
		pb.add(new BorderExtenderConstant(fillValue));
		//pb.add(val);

		// Create the output image by translating itself.
		PlanarImage output = JAI.create("border", pb, null);

		return(output);
	
	}	
	
	
	/**
	* Adds a symmetrical border around an image
	* 
	* @param  img	The image
	*	  bSize	The size of the border
	*	  clr	The color of the border
	* @return	The modified image
	*/	
	public static PlanarImage addBorder(PlanarImage img, int bSize, Color clr)
	{
		return(addPadding(img, bSize, bSize, bSize, bSize, clr));
	}
	
	
	/**
	* Scales an image using either nearest-neighbor, bilinear or bicubic interpolation
	* and returns the new image.
	* 
	* @param  img		The image which should be scaled
	*	  scale		The scaling factor
	*         interpType	The scaling type (nearest-neighbor, bilinear or bicubic)
	* @return		The modified image
	*/
	public static PlanarImage scale(PlanarImage img, double scale, Interpolation interpType)
	{
		float s = (float) scale;
		
		ParameterBlock pb = new ParameterBlock();
        	pb.addSource(img);
        	pb.add(s);
        	pb.add(s);
        	pb.add(0.0F);
        	pb.add(0.0F);
		pb.add(interpType);

        	PlanarImage scaled = JAI.create("scale", pb, null);
        	return (scaled);
	}
	
	/**
	* Overlays a text on top of an image and returns the new image
	* 
	* @param  img	The image on which the text should be added
	*	  text	The text to overlay
	*	  x	The X coordinate where the text will be placed
	*	  y 	The Y coordinate where the text will be placed
	*	  f	The font of the text
	*	  clr	The color of the text
	* @return	The modified image
	*/
	public static PlanarImage overlayText(PlanarImage img, String text, int x, int y, Font f, Color clr)
	{
		TiledImage ti = new TiledImage(img, true);
		Graphics2D g2 = (Graphics2D) ti.createGraphics();
		g2.setFont(f);
		g2.setColor(clr);
		g2.drawString(text, x, y);

		return (ti);
	}	


	/**
	* Generates a composite RGB image from two gray-scale images
	* which are placed in the red and green channels, respectively.
	* 
	* @param  imgRed	The image which should be placed in the red channel
	*	  imgGreen	The image which should be placed in the green channel
	*	  brightness	Image brightness
	* @return	The composite image
	*/
	public static BufferedImage generateBufferedCompositeImage(PlanarImage imgRed, PlanarImage imgGreen, int brightness, JProgressBar pb)
	{
		BufferedImage imgComposite = new BufferedImage(imgRed.getWidth(), imgGreen.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster wrComposite = imgComposite.getRaster();
		
		Raster rasterCh1 = imgRed.getData();
		Raster rasterCh2 = imgGreen.getData();
		
		int width = imgComposite.getWidth();
		int height = imgComposite.getHeight();
		int colors = imgComposite.getColorModel().getNumColorComponents();
		
		//System.err.println("Colors: " + colors);
		
		int[] col = new int[height*colors];
		
		for (int xx = 0; xx < width; xx++)
		{
			for (int yy = 0; yy < height; yy++)
			{
				//square-root transform data
						
				//int red = (int)Math.min(Math.round(Math.sqrt(rasterCh1.getSample(xx, yy, BAND_GREYSCALE))) + brightness, PIXEL_SCALED_INT_MAX);
				//int green = (int)Math.min(Math.round(Math.sqrt(rasterCh2.getSample(xx, yy, BAND_GREYSCALE))) + brightness, PIXEL_SCALED_INT_MAX);
				
				
				/*
				int red = (int)Math.min( Math.sqrt(rasterCh1.getSample(xx, yy, BAND_GREYSCALE)) + brightness, PIXEL_SCALED_INT_MAX);
				int green = (int)Math.min( Math.sqrt(rasterCh2.getSample(xx, yy, BAND_GREYSCALE)) + brightness, PIXEL_SCALED_INT_MAX);
				int blue = (red == PIXEL_SCALED_INT_MAX && green == PIXEL_SCALED_INT_MAX) ? PIXEL_SCALED_INT_MAX : PIXEL_SCALED_INT_MIN;
				*/
				
				int index = yy*3;
				col[index] = (int)Math.min( Math.sqrt(rasterCh1.getSample(xx, yy, BAND_GREYSCALE)) + brightness, PIXEL_SCALED_INT_MAX);
				col[index+1] = (int)Math.min( Math.sqrt(rasterCh2.getSample(xx, yy, BAND_GREYSCALE)) + brightness, PIXEL_SCALED_INT_MAX);
				col[index+2] = (col[index] == PIXEL_SCALED_INT_MAX && col[index+1] == PIXEL_SCALED_INT_MAX) ? PIXEL_SCALED_INT_MAX : PIXEL_SCALED_INT_MIN;
				
				
				//int red = ( (int)rasterCh1.getSample(xx, yy, BAND_GREYSCALE) ) << 4;
				//int green = ( (int)rasterCh2.getSample(xx, yy, BAND_GREYSCALE) ) << 4;
						
						
				/*
				wrComposite.setSample(xx, yy, BAND_B, blue);
				wrComposite.setSample(xx, yy, BAND_R, red);
				wrComposite.setSample(xx, yy, BAND_G, green);
				*/
			}
			
			//write to raster 'on column at a time'
			//this is approximately 20% faster than 'on pixel at a time'
			wrComposite.setPixels(xx, 0, 1, height, col);
			
			if (pb != null)
				pb.setValue( (int) ( 100 * ( (double) xx / (double)(width-1) ) ) );
		}
			
		imgComposite.setData(wrComposite);
		return imgComposite;
	}
	
	/**
	* Generates a composite RGB image from two gray-scale images
	* which are placed in the red and green channels, respectively.
	* 
	* @param  imgRed	The image which should be placed in the red channel
	*	  imgGreen	The image which should be placed in the green channel
	*	  brightness	Image brightness
	* @return	The composite image
	*/
	public static PlanarImage generateCompositeImage(PlanarImage imgRed, PlanarImage imgGreen, int brightness)
	{
		BufferedImage imgComposite = generateBufferedCompositeImage(imgRed, imgGreen, brightness, null);
		return (PlanarImage.wrapRenderedImage(imgComposite));
	}	
	


	/**
	* Alters the brightness of a BufferedImage by a specific offset.
	* 
	* @param  img	The image which should be altered
	*	  brightness	Image brightness change
	* @return	The brightened/darkened image
	*/
	public static BufferedImage alterBrightness(BufferedImage img, int brightness)
	{
		BufferedImage imgComposite = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster wrComposite = imgComposite.getRaster();
		
		Raster raster = img.getData();
		
		
		for (int xx = 0; xx < imgComposite.getWidth(); xx++)
			for (int yy = 0; yy < imgComposite.getHeight(); yy++)
			{
				
				int red = (int)Math.max(Math.min(raster.getSample(xx, yy, BAND_R) + brightness, PIXEL_SCALED_INT_MAX), PIXEL_SCALED_INT_MIN);
				int green = (int)Math.max(Math.min(raster.getSample(xx, yy, BAND_G) + brightness, PIXEL_SCALED_INT_MAX), PIXEL_SCALED_INT_MIN);
				int blue = (red == PIXEL_SCALED_INT_MAX && green == PIXEL_SCALED_INT_MAX) ? PIXEL_SCALED_INT_MAX : PIXEL_SCALED_INT_MIN;
				
				
				wrComposite.setSample(xx, yy, BAND_B, blue);
				wrComposite.setSample(xx, yy, BAND_R, red);
				wrComposite.setSample(xx, yy, BAND_G, green);
						
			}
			
		imgComposite.setData(wrComposite);
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		return (imgComposite);
	}	
	
	
	/**
	* Applies a binary mask to a (copy of a) RGB image, where the remaining entries are set to 0 intensity 
	* 
	* @param  img	The image which should be altered
	* @param	mask	Mask to apply (should be 0 and 1 only)
	* @return	The image with the mask applied
	*/
	public static BufferedImage applyMask(BufferedImage img, byte[][] mask)
	{
		BufferedImage imgComposite = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster wrComposite = imgComposite.getRaster();
		
		Raster raster = img.getData();
		
		
		for (int xx = 0; xx < imgComposite.getWidth(); xx++)
			for (int yy = 0; yy < imgComposite.getHeight(); yy++)
			{
				
				wrComposite.setSample(xx, yy, BAND_B, raster.getSample(xx, yy, BAND_B)*mask[yy][xx]);
				wrComposite.setSample(xx, yy, BAND_R, raster.getSample(xx, yy, BAND_R)*mask[yy][xx]);
				wrComposite.setSample(xx, yy, BAND_G, raster.getSample(xx, yy, BAND_G)*mask[yy][xx]);
			}
			
		imgComposite.setData(wrComposite);
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		return (imgComposite);
	}
	
	/**
	* Applies a binary mask to a RGB image, where the remaining entries are set to 0 intensity.
	* 
	* @param  img	The image which should be altered
	* @param	mask	Mask to apply (should be 0 and 1 only)
	*/
	public static void applyMaskSide(BufferedImage img, byte[][] mask)
	{
		//BufferedImage imgComposite = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster wrComposite = img.getRaster();
		
		//Raster raster = img.getData();
		
		
		for (int xx = 0; xx < img.getWidth(); xx++)
			for (int yy = 0; yy < img.getHeight(); yy++)
			{
				
				wrComposite.setSample(xx, yy, BAND_B, wrComposite.getSample(xx, yy, BAND_B)*mask[yy][xx]);
				wrComposite.setSample(xx, yy, BAND_R, wrComposite.getSample(xx, yy, BAND_R)*mask[yy][xx]);
				wrComposite.setSample(xx, yy, BAND_G, wrComposite.getSample(xx, yy, BAND_G)*mask[yy][xx]);
			}
			
		img.setData(wrComposite);
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		//return (img);
	}	

	/**
	* Construct a reconstructed version of a BufferedImage where the 
	* regions of interests are defined by a Vector of Rectangles.
	* 
	* @param  img	The image which is to be cropped
	* @param  dims	A Vector of Rectangles containing the regions to crop
	* @param	scale	A scaling factor for the image size
	*/
	public static BufferedImage cropImages(BufferedImage img, Vector dims, double scale)
	{
		//BufferedImage imgCropped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		//WritableRaster wrCropped = imgCropped.getRaster();	
		//Raster raster = img.getData();
		
		
		//first find out the height and width of the image to be reconstructed
		int imgHeight = -1, imgWidth = 0;
		Rectangle rect;
		for (int i = 0; i < dims.size(); i++)
		{
			rect = (Rectangle)dims.get(i);
			if (rect.getHeight() > imgHeight)
				imgHeight = (int)rect.getHeight();
			imgWidth += (int)rect.getWidth();
		}
		
		System.err.println("Image is " + img.getWidth() + " x " + img.getHeight() + " ...");

		//now we try to merge to images into one single image
		BufferedImage newImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g2d = newImage.getGraphics();
		int currX = 0;
		for (int i = 0; i < dims.size(); i++)
		{
			rect = (Rectangle)dims.get(i);
			int minX = (int)Math.max( rect.getLocation().getX(), 0	);
			int maxX = (int)Math.min(rect.getLocation().getX()+rect.getWidth(), img.getWidth()/scale-1 );
			int minY = (int)Math.max(rect.getLocation().getY(), 0);
			int maxY = (int)Math.min(rect.getLocation().getY()+rect.getHeight(), img.getHeight()/scale-1);
			
			System.err.println("Cropping image from (" + minX + "," + minY + ") to (" + maxX + "," + maxY + ")");
			
			PlanarImage pi = PlanarImageEdit.crop(img, minX, minY, maxX-minX, maxY-minY);
			pi = PlanarImageEdit.translate(pi, -pi.getMinX(), -pi.getMinY());
			
			BufferedImage biCurr = new BufferedImage( (int)rect.getWidth(), (int)rect.getHeight(), BufferedImage.TYPE_INT_RGB);
			//BufferedImage bufferedImage = new BufferedImage(croppedImage.getWidth(), croppedImage.getHeight(), BufferedImage.TYPE_USHORT_GRAY);

			
			biCurr.setData(pi.copyData());
			//JAI.create("filestore", biCurr, "Test_image" + i + ".tif", "TIFF", null);
			
			g2d.drawImage(biCurr, currX, 0, Color.BLACK, null);
			
			currX += rect.getWidth();
			
		}
			
		
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		return (newImage);
	}	


	

	/**
	* Alters the brightness of a BufferedImage by a specific offset.
	* 
	* @param  img	The image which should be altered
	*	  brightness	Image brightness change
	* @return	The brightened/darkened image
	*/
	public static BufferedImage emphasizeChannel(BufferedImage img, int channel, double threshold, JProgressBar pb)
	{
		BufferedImage imgComposite = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		WritableRaster wrComposite = imgComposite.getRaster();
		
		Raster raster = img.getData();
		
		int colors = imgComposite.getColorModel().getNumColorComponents();
		
		for (int xx = 0; xx < imgComposite.getWidth(); xx++)
		{
			for (int yy = 0; yy < imgComposite.getHeight(); yy++)
			{
				int currCol = 0;
				int otherCol = 0;
				
				for (int i = 0; i < colors; i++)
				{
					int col = raster.getSample(xx, yy, i);
					
					if (i != channel)
					{
						//get the maximum of the other colors
						if (col > otherCol)
							otherCol = col;
					} else
						currCol = col;
				}
						
								
				
				
				
				//generate a "white" pixel if the current color
				//is below threshold
				if ( (double)currCol/(double)otherCol < threshold)
				{
					for (int i = 0; i < colors; i++)
						wrComposite.setSample(xx, yy, i, PIXEL_SCALED_INT_MAX);
				} else
				{
					for (int i = 0; i < colors; i++)
					{
						if (i != channel)
							wrComposite.setSample(xx, yy, i, PIXEL_SCALED_INT_MIN);
						else
							wrComposite.setSample(xx, yy, i, currCol);
					}
				}
						
						
			}
		
			if (pb != null)
				pb.setValue( (int) ( 100 * ( (double) xx / (double)(imgComposite.getWidth()-1) ) ) );
		}
			
		imgComposite.setData(wrComposite);
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		return (imgComposite);
	}	

	public static Rectangle suggestCropRect(BufferedImage img, int channel, int width, int height, int intensityThreshold, double occuranceThreshold, JProgressBar pb)
	{
		Raster raster = img.getData();
		
		//Rectangle rect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
		Point pointUpperLeft = new Point(img.getWidth(), img.getHeight());
		Point pointLowerRight = new Point(0, 0);
		
		int colors = img.getColorModel().getNumColorComponents();
		
		//calculate num. of pixels that should fulfill the criterion
		int numAccept = (int)Math.ceil(width*height*occuranceThreshold);
		
		//System.err.println("Number that must be accepted: " + numAccept);
		
		for (int x = 0; x < ( img.getWidth() - width); x+=width/2)
		{
			for (int y = 0; y < ( img.getHeight() - height); y+=height/2)
			{
		
				int numOcc = 0;
				
				for (int xx = 0; xx < width; xx++)
					for (int yy = 0; yy < height; yy++)
					{
						int currCol = 0;
						int otherCol = 0;
				
						for (int i = 0; i < colors; i++)
						{
							int col = raster.getSample(x+xx, y+yy, i);
					
							if (i != channel)
							{
								//get the maximum of the other colors
								if (col > otherCol)
									otherCol = col;
							} else
								currCol = col;
						}
						
					
						//if ( otherCol == 0 || (double)currCol/(double)otherCol > intensityThreshold )
						if ( otherCol == 0 && currCol >= intensityThreshold )
							numOcc++;
					}
			
				if (numOcc >= numAccept)
				{
					if (x < pointUpperLeft.getX())
						pointUpperLeft.setLocation(x, pointUpperLeft.getY() );
					if (y < pointUpperLeft.getY())
						pointUpperLeft.setLocation(pointUpperLeft.getX(), y);
					
					if ( (x + width) > pointLowerRight.getX() )
						pointLowerRight.setLocation(x + width, pointLowerRight.getY() );
					if ( (y + height) > pointLowerRight.getY() )
						pointLowerRight.setLocation(pointLowerRight.getX(), y + height);
					
					//System.err.println("Accepted rectangle found at ("  + x + "," + y + ") with " + numOcc + " pixels accepted");
				}
			}

			if (pb != null)
				pb.setValue( (int) ( 100 * ( (double) x / (double)(img.getWidth()-1) ) ) );
			
		}
		
		//set a outer rectangle with some slack
		Rectangle rect = new Rectangle( (int)Math.max( pointUpperLeft.getX() - width, 0),
			(int)Math.max(pointUpperLeft.getY() - height, 0),
			(int)Math.min(pointLowerRight.getX() - pointUpperLeft.getX() + width*2,
				img.getWidth() - pointUpperLeft.getX() - width),
			(int)Math.min(pointLowerRight.getY() - pointUpperLeft.getY() + height*2,
				img.getHeight() - pointUpperLeft.getY() - height));
						


		return rect;
	}


	/**
	* Paints constant pixel values from a Color vector onto a Graphics object.
	* The painting is only performed when a int[][] matrix has elements > 0,
	* where the element in that case defines the object id (and corresponds to the
	* Color index).
	* 
	* @param	seg	The integer matrix, typically defining segmented objects
	* @param	c	Color vector for the colors to be painted (one for each value in seg)
	* @param	g2d	 A graphics object
	*/
	public static void paintIntegerMatrix(int[][] seg, Color c, Graphics g2d)
	{
		int segWidth = seg[0].length;
		int segHeight = seg.length;
		
		g2d.setColor( c );
		
		//paint
		int id = -1;
		for (int x = 0; x < segWidth; x++)
			for (int y = 0; y < segHeight; y++)
			{
				id = seg[y][x];
				if (id > 0)
				{
					
					g2d.drawLine(x, y, x, y);
				}
			}

	}
			
	/**
	* Paints constant pixel values from a Color vector onto a Graphics object.
	* The painting is only performed when a byte[][] matrix has elements > 0,
	* where the element in that is painted using the user-defined color.
	* 
	* @param	seg	The byte matrix, where values are either 0 or != 0
	* @param	c	Color to painr (when seg[][] > 0)
	* @param	g2d	 A graphics object
	*/
	public static void paintIntegerMatrix(byte[][] seg, Color c, Graphics g2d)
	{
		int segWidth = seg[0].length;
		int segHeight = seg.length;

		g2d.setColor(c);
		
		//paint
		for (int x = 0; x < segWidth; x++)
			for (int y = 0; y < segHeight; y++)
			{
				if (seg[y][x] > 0)
					g2d.drawLine(x, y, x, y);
			}
			
	}
	
	/**
	* Paints constant pixel values from a Color vector onto a Graphics object.
	* The painting is performed on a Vector of Vector of available Points[]
	*
	* 
	* @param	vec	The Vector of Vector of Points
	* @param	c	Color vector for the colors to be painted (one for each value in seg)
	* @param	g2d	 A graphics object
	*/
	public static void paintVector(Vector vec, Color c, Graphics g2d)
	{
		
		g2d.setColor( c );
		
		//paint
		for (int i = 0; i < vec.size(); i++)
		{
			
			Vector vecCurr = (Vector)vec.get(i);
			Point p1;
			int x,y;
			
			for (int j = 0; j < vecCurr.size(); j++)
			{
				p1 = (Point)vecCurr.get(j);
				x = (int)p1.getX();
				y = (int)p1.getY();
		
		
				g2d.drawLine(x, y, x, y);
				
			}
		}
	}
	
		
}
