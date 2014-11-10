/* JComponentDisplay.java
 *
 * Copyright (c) Max Bylesjö, 2004-2008
 *
 * A class to display scaled images with optional line-based objects,
 * which are drawn on top of the Graphics object.
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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.io.PrintStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.Vector;


/**
*A JComponent derivative used to display scaled graphical objects from image files.
*/
public class JComponentDisplay extends JComponent
{
	public final static byte SCALE_MODE_ORG = 0;
	public final static byte SCALE_MODE_PRESCALE = 1;
	public final static byte SCALE_MODE_DYNAMIC = 2;
	public final static byte BORDER_RECT_OFFSET = 5;
	public final static byte BORDER_RECT_OFFSET_SMALL = 3;
	public final static byte BORDER_RECT_OFFSET_TINY = 2;
	
	protected final static String OBJECT_STR_PREFIX = "Object #";

	//protected PlanarImage source;
	protected BufferedImage source, sourceOrg;
	protected SampleModel sampleModel;
	protected ColorModel colorModel;
	protected int minTileX;
	protected int maxTileX;
	protected int minTileY;
	protected int maxTileY;
	protected int tileWidth;
	protected int tileHeight;
	protected int tileGridXOffset;
	protected int tileGridYOffset;
	protected int originX;
	protected int originY;
	protected int shift_x;
	protected int shift_y;
	protected int componentWidth;
	protected int componentHeight;
	protected float scaleFactor = (float)1.0;
	protected float epsilon = (float)0.0001;
	protected BufferedImageOp biop;
	protected Point pointVarBorder, pointFixedBorder1, pointFixedBorder2;
	protected int objectId = -1;
	protected String objectIdStr;
	protected Point pointObjectLocation;
	protected Vector vecLines, vecLandmarkPoints;
	protected byte scaleType = SCALE_MODE_ORG;

	/**
	* Initializes the graphical area.
	* 
	*/
	private synchronized void initialize()
	{
		if(source == null)
			return;
		
		if (scaleType == this.SCALE_MODE_DYNAMIC)
		{
			componentWidth = (int)(source.getWidth()*scaleFactor);
			componentHeight = (int)(source.getHeight()*scaleFactor);
		} else
		{
			componentWidth = source.getWidth();
			componentHeight = source.getHeight();
		}
		setPreferredSize(new Dimension(componentWidth, componentHeight));
		sampleModel = source.getSampleModel();
		colorModel = source.getColorModel();
		if(colorModel == null)
 		{
			colorModel = PlanarImage.createColorModel(sampleModel);
			if(colorModel == null)
				throw new IllegalArgumentException("no color model");
		}
			
		minTileX = source.getMinTileX();
		maxTileX = (source.getMinTileX() + source.getNumXTiles()) - 1;
		minTileY = source.getMinTileY();
		maxTileY = (source.getMinTileY() + source.getNumYTiles()) - 1;
		tileWidth = source.getTileWidth();
		tileHeight = source.getTileHeight();
		tileGridXOffset = source.getTileGridXOffset();
		tileGridYOffset = source.getTileGridYOffset();
 	}

	/**
	* Main constructor. Resets the graphical area and the associated image files.
	*/
	public JComponentDisplay()
	{
		originX = 0;
		originY = 0;
		shift_x = 0;
		shift_y = 0;
		//brightnessEnabled = false;
		//brightness = 0;
		source = null;
		sourceOrg = null;
		componentWidth = 64;
		componentHeight = 64;
		setPreferredSize(new Dimension(componentWidth, componentHeight));
		//setOrigin(0, 0);
		//setBrightnessEnabled(true);
	}

	/**
	* Constructor - resets the graphical area and assigns an image file to display.
	* 
	* @param	img	The image to display
	*/
	public JComponentDisplay(BufferedImage img)
	{
		originX = 0;
		originY = 0;
		shift_x = 0;
		shift_y = 0;
		//brightnessEnabled = false;
		//brightness = 0;
		sourceOrg = img;
		source = img;
		initialize();
		//setOrigin(0, 0);
		//setBrightnessEnabled(true);
	}

	/**
	* Constructor - sets the initial dimension of the graphic area.
	* 
	* @param	i	The width of the area
	* @param	j	The height of the area
	*/
	public JComponentDisplay(int i, int j)
	{
		originX = 0;
		originY = 0;
		shift_x = 0;
		shift_y = 0;
		//brightnessEnabled = false;
		//brightness = 0;
		source = null;
		sourceOrg = null;
		componentWidth = i;
		componentHeight = j;
		setPreferredSize(new Dimension(componentWidth, componentHeight));
		//setOrigin(0, 0);
		//setBrightnessEnabled(true);
	}
	
	/**
	* Assigns an image file to the graphic area
	* 
	* @param	img	The image to display
	*/
	public void set(BufferedImage img)
	{
		sourceOrg = img;
		//source = img;
		setScaleType(scaleType, scaleFactor);
		initialize();
		repaint();
	}
	
	/**
	* Checks whether the object has a 'variable border point' assigned
	* 
	* @return	True if the point is assigned, false otherwise
	*/
	public boolean hasPointVarBorder()
	{
		return (pointVarBorder != null);
	}
	
	/**
	* Checks whether the object has 'fixed border point #1' (starting point) assigned
	* 
	* @return	True if the point is assigned, false otherwise
	*/
	public boolean hasPointFixedBorder1()
	{
		return (pointFixedBorder1 != null);
	}
	
	/**
	* Checks whether the object has 'fixed border point #2' (end point) assigned
	* 
	* @return	True if the point is assigned, false otherwise
	*/
	public boolean hasPointFixedBorder2()
	{
		return (pointFixedBorder2 != null);
	}

	/**
	* Sets the variable border point.
	* 
	* @param	pointVarBorder	The point to use
	*/
	public void setBorderVarPoint( Point pointVarBorder )
	{
		this.pointVarBorder = pointVarBorder;
	}
	
	/**
	* Sets the fixed border point 1 (starting point)
	* 
	* @param	pointFixedBorder	The point to use
	*/
	public void setBorderFixedPoint1( Point pointFixedBorder )
	{
		this.pointFixedBorder1 = pointFixedBorder;
	}
	
	/**
	* Sets the fixed border point 2 (end point)
	* 
	* @param	pointFixedBorder	The point to use
	*/
	public void setBorderFixedPoint2( Point pointFixedBorder )
	{
		this.pointFixedBorder2 = pointFixedBorder;
	}
	
	/**
	* Returns the assigned variable border point
	* 
	* @return	The current point
	*/
	public Point getBorderVarPoint()
	{
		return this.pointVarBorder;
	}
	
	/**
	* Returns the assigned fixed border point #1 (starting point)
	* 
	* @return	The current point
	*/
	public Point getBorderFixedPoint1()
	{
		return this.pointFixedBorder1;
	}
	
	/**
	* Returns the assigned fixed border point #2 (end point)
	* 
	* @return	The current point
	*/
	public Point getBorderFixedPoint2()
	{
		return this.pointFixedBorder2;
	}
	
	/**
	* Returns the ID # of the object
	* 
	* @return	The object ID #
	*/
	public int getObjectId()
	{
		return this.objectId;
	}
	
	/**
	* Assigns a Vector of lines that define the width/height properties of the object.
	* 
	* @param	vecLines	The Vector of lines
	*/
	public void setVectorOfLines(Vector vecLines)
	{
		this.vecLines = vecLines;
	}
	
	/**
	* Sets the object's ID #
	* 
	* @param	objectId	The ID#
	*/
	public void setObjectId(int objectId)
	{
		this.objectId = objectId;
	}
	
	/**
	* Sets the object's display ID (can differ from the internal ID)
	* 
	* @param	oId	The ID#
	*/
	public void setObjectDisplayId(int oId)
	{
		if (oId >= 0)
			this.objectIdStr = OBJECT_STR_PREFIX + oId;
		else
			this.objectIdStr = null;
	}
	
	/**
	* Sets the location of the object
	* 
	* @param	pointObjLoc	The location of the object as a Point
	*/
	public void setObjectLocation(Point pointObjLoc)
	{
		this.pointObjectLocation = pointObjLoc;
	}
	
	/**
	* Assigns  a Vector of Points that define the landmark Points
	* 
	* @param	vecLandmarkPoints	Vector of Points with landmark coordinates
	*/
	public void setBorderLandmarks( Vector vecLandmarkPoints)
	{
		this.vecLandmarkPoints = vecLandmarkPoints;
	}
	
	/**
	* Sets the scale factor that is used when displaying the image
	* 
	* @param	scaleFactor	The scale factor
	*/
	public void setScaleFactor(float scaleFactor)
	{
		//removing scaling if scaleFactor is close to 1.0
		if ( Math.abs(scaleFactor - 1.0) > epsilon)
			this.scaleFactor = scaleFactor;
		else
			this.scaleFactor = (float)1.0;
		
		//setPreferredSize(new Dimension(componentWidth, componentHeight));
	}
	
	/**
	* Sets the scale type (either unscaled, pre-scaled or scaled-on-request).
	* 
	* @param	scaleType	The scale type
	*/
	public void setScaleType(byte scaleType)
	{
		this.setScaleType(scaleType, this.scaleFactor);
	}

	/**
	* Sets the scale type (either unscaled, pre-scaled or scaled-on-request).
	* 
	* @param	scaleType	The scale type
	* @param	scaleFactor	The scale factor
	*/
	public void setScaleType(byte scaleType, float scaleFactor)
	{
		if (scaleType == this.SCALE_MODE_ORG)
		{
			this.scaleType = scaleType;
			this.setScaleFactor( (float)1.0);
			source = sourceOrg;
		} 
		else if (scaleType == this.SCALE_MODE_PRESCALE)
		{
			this.scaleType = scaleType;
			this.setScaleFactor( scaleFactor);
			this.scaleImage();
			
		} else if (scaleType == this.SCALE_MODE_DYNAMIC)
		{
			this.scaleType = scaleType;
			this.setScaleFactor( scaleFactor);
			source = sourceOrg;
		}
		
		initialize();
	}
	
	/**
	* Sets the scale factor (either unscaled, pre-scaled or scaled-on-request),
	* determined automatically from the scale factor.
	* 
	* @param	scaleFactor	The scale factor
	*/
	public void setAutoScaleType(float scaleFactor)
	{
		this.scaleFactor = scaleFactor;
		if ( Math.abs(scaleFactor - 1.0) < epsilon)
		{
			this.setScaleType(this.SCALE_MODE_ORG, scaleFactor);
			
		} else if (scaleFactor > 1.0)
		{
			this.setScaleType(this.SCALE_MODE_DYNAMIC, scaleFactor);
		} else
		{
			this.setScaleType(this.SCALE_MODE_PRESCALE, scaleFactor);
		}
	
	}
	
	/*
	public void setBrush(Color c, int brushSize)
	{
		this.brushColor = c;
		this.brushSize = brushSize;
		
	}
	*/

	/**
	* Paints the image onto the graphics area with some additional graphical objects (e.g. lines, points, etc.).
	* 
	* @param	g	The graphics object
	*/
	public void paintComponent(Graphics g)
	{
		Graphics2D graphics2d = null;
		if(g instanceof Graphics2D)
			graphics2d = (Graphics2D)g;
		else
			return;
		if(source == null)
		{
			graphics2d.setColor(getBackground());
			graphics2d.fillRect(0, 0, componentWidth, componentHeight);
			return;
		}
		
		
		int i = -originX;
		int j = -originY;
		Rectangle rectangle = g.getClipBounds();
		
		//System.err.println("Bounds: " + rectangle);

		/*
		if(rectangle == null)
			rectangle = new Rectangle(0, 0, componentWidth, componentHeight);
		if(i > 0 || j > 0 || i < componentWidth - source.getWidth() || j < componentHeight - source.getHeight())
		{
			graphics2d.setColor(getBackground());
			graphics2d.fillRect(0, 0, componentWidth, componentHeight);
		}
		rectangle.translate(-i, -j);
		
		int k = XtoTileX(rectangle.x);
		k = Math.max(k, minTileX);
		k = Math.min(k, maxTileX);
		int l = XtoTileX((rectangle.x + rectangle.width) - 1);
		l = Math.max(l, minTileX);
		l = Math.min(l, maxTileX);
		int i1 = YtoTileY(rectangle.y);
		i1 = Math.max(i1, minTileY);
		i1 = Math.min(i1, maxTileY);
		int j1 = YtoTileY((rectangle.y + rectangle.height) - 1);
		j1 = Math.max(j1, minTileY);
		j1 = Math.min(j1, maxTileY);
		Insets insets = getInsets();
		
		for(int l1 = i1; l1 <= j1; l1++)
		{
			for(int k1 = k; k1 <= l; k1++)
			{
				int i2 = TileXtoX(k1);
				int j2 = TileYtoY(l1);
				Raster raster = source.getTile(k1, l1);
				if(raster != null)
				{
					java.awt.image.DataBuffer databuffer = raster.getDataBuffer();
					java.awt.image.WritableRaster writableraster = Raster.createWritableRaster(sampleModel, databuffer, null);
					BufferedImage bufferedimage = new BufferedImage(colorModel, writableraster, colorModel.isAlphaPremultiplied(), null);
					
					//if(brightnessEnabled)
					//{
					//	SampleModel samplemodel = sampleModel.createCompatibleSampleModel(raster.getWidth(), raster.getHeight());
					//	java.awt.image.WritableRaster writableraster1 = RasterFactory.createWritableRaster(samplemodel, null);
					//	BufferedImage bufferedimage1 = new BufferedImage(colorModel, writableraster1, colorModel.isAlphaPremultiplied(), null);
					//	graphics2d.drawImage(bufferedimage1, biop, i2 + i + insets.left, j2 + j + insets.top);
					//} else
					{
						AffineTransform affinetransform = AffineTransform.getTranslateInstance(i2 + i + insets.left, j2 + j + insets.top);
						graphics2d.drawRenderedImage(bufferedimage, affinetransform);
						//graphics2d.drawImage(bufferedimage, null, 0, 0);
					}
				}
			}

		}
		*/
		
		/*
		PlanarImage imgCropped = PlanarImageEdit.crop(source, (int)rectangle.getX(), (int)rectangle.getY(),
			(int)rectangle.getWidth(), (int)rectangle.getHeight());
		
		imgCropped = PlanarImageEdit.translate(imgCropped, (int)-rectangle.getWidth(), (int)-rectangle.getHeight());
		
		graphics2d.drawImage(imgCropped.getAsBufferedImage(), null, 0, 0);
		*/
		
		if (this.scaleType != this.SCALE_MODE_DYNAMIC)
		{
			
			
			//make sure we stay within bounds
			if ( (rectangle.getX() + rectangle.getWidth() ) > source.getWidth() )
				rectangle.setBounds( (int)rectangle.getX(), (int)rectangle.getY(),
					(int) (source.getWidth() - rectangle.getX() ), (int)rectangle.getHeight() );
			if ( (rectangle.getY() + rectangle.getHeight() ) > source.getHeight() )
				rectangle.setBounds( (int)rectangle.getX(), (int)rectangle.getY(),
					(int)rectangle.getWidth(), (int) (source.getHeight() - rectangle.getY() ));
			
			
			if (rectangle.getWidth() > 0 && rectangle.getHeight() > 0)
			{
				try
				{
					Raster r = source.getData(rectangle);
					Raster r2 = r.createTranslatedChild(0, 0);

					BufferedImage bufferedImage = new BufferedImage( (int)rectangle.getWidth(), (int)rectangle.getHeight(), source.getType());
					bufferedImage.setData(r2);
		
					graphics2d.drawImage(bufferedImage, null, (int)rectangle.getX(), (int)rectangle.getY());
					
				
					//display a rectangle around a border point
					if (pointVarBorder != null)
					{
						graphics2d.setColor( Color.YELLOW );
						
						//graphics2d.drawRect( (int)Math.round( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
						//	(int)Math.round( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
						//	(int)Math.round( (BORDER_RECT_OFFSET*2+1)*scaleFactor),
						//	(int)Math.round( (BORDER_RECT_OFFSET*2+1)*scaleFactor) );
						
						
						//two lines to form a  cross
						/*
						graphics2d.drawLine( (int)Math.round( (pointVarBorder.getX())*scaleFactor),
							(int)Math.round( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getX())*scaleFactor),
							(int)Math.round( (pointVarBorder.getY()+BORDER_RECT_OFFSET)*scaleFactor) );
							
						graphics2d.drawLine( (int)Math.round( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getY())*scaleFactor),
							(int)Math.round( (pointVarBorder.getX()+BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getY())*scaleFactor) );
						*/
						
						graphics2d.drawLine( (int)( (pointVarBorder.getX())*scaleFactor),
							(int)( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getX())*scaleFactor),
							(int)( (pointVarBorder.getY()+BORDER_RECT_OFFSET)*scaleFactor) );
							
						graphics2d.drawLine( (int)( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getY())*scaleFactor),
							(int)( (pointVarBorder.getX()+BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getY())*scaleFactor) );
					}
					
					
					//display a rectangle around a border point
					if (pointFixedBorder1 != null)
					{
						graphics2d.setColor( Color.RED );
						
						graphics2d.drawRect( (int)Math.round( (pointFixedBorder1.getX()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (pointFixedBorder1.getY()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor) );
						
						//connect the variable point with the fixed one
						if (pointVarBorder != null && pointFixedBorder1 != null && pointFixedBorder2 == null)
						{
							graphics2d.setColor( Color.RED );
							graphics2d.drawLine( (int)Math.round(pointFixedBorder1.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder1.getY()*scaleFactor),
								(int)Math.round(pointVarBorder.getX()*scaleFactor),
								(int)Math.round(pointVarBorder.getY()*scaleFactor) );
						}
					}
					
					//display a rectangle around a border point
					if (pointFixedBorder2 != null)
					{
						graphics2d.setColor( Color.RED );
						
						graphics2d.drawRect( (int)Math.round( (pointFixedBorder2.getX()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (pointFixedBorder2.getY()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor) );
						
						//connect the variable point with the fixed one
						if (pointFixedBorder1 != null && pointFixedBorder2 != null)
						{
							graphics2d.setColor( Color.CYAN );
							graphics2d.drawLine( (int)Math.round(pointFixedBorder1.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder1.getY()*scaleFactor),
								(int)Math.round(pointFixedBorder2.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder2.getY()*scaleFactor) );
						}
					}
					
					if (vecLines != null)
					{
						for (int n = 0; n < vecLines.size(); n++)
						{
							//contains horizontal/vertical lines
							Vector[] currVec = (Vector[])vecLines.get(n);
							for (int k = 0; k < currVec.length; k++)
							{
								graphics2d.setColor( Color.CYAN );
								for (int m = (currVec[k].size()-1); m >= 0; m--)
								{
									Point[] pArr = (Point[])currVec[k].get(m);
									
									try
									{
										graphics2d.drawLine( (int)Math.round(pArr[0].getX()*scaleFactor),
											(int)Math.round(pArr[0].getY()*scaleFactor),
											(int)Math.round(pArr[1].getX()*scaleFactor),
											(int)Math.round(pArr[1].getY()*scaleFactor) );
									} catch (Exception ex)
									{
										//ignore for now
									}
										
									if (m == 1)
									{
										//if (k == 0)
										//	graphics2d.setColor( Color.PINK );
										//else
											graphics2d.setColor( Color.PINK );
									}
								}
							}
							
							//display the intersection point
							/*
							Point2D.Double pIntersect = MiscMath.lineIntersection( (Point[])currVec[0].get(0), (Point[])currVec[1].get(0));
							
							graphics2d.setColor( Color.BLUE );
							graphics2d.fillArc( (int)Math.round(pIntersect.getX()*scaleFactor),
									(int)Math.round(pIntersect.getY()*scaleFactor),
									(int)(5*scaleFactor), (int)(5*scaleFactor), 0, 360);
							*/
						}
					}
					
					if (objectIdStr != null && pointObjectLocation != null)
					{
						Point pScaled = new Point( (int)Math.round(pointObjectLocation.getX()*scaleFactor),
							(int)Math.round(pointObjectLocation.getY()*scaleFactor) );
						
						graphics2d.setColor( Color.WHITE );
						graphics2d.setFont( new Font("Times New Roman", Font.PLAIN, 14) );
						graphics2d.drawString( objectIdStr, (int)pScaled.getX(), (int)pScaled.getY() );
					}
					
					if (vecLandmarkPoints != null)
					{
						Point p = new Point();
						for (int k = 0; k < vecLandmarkPoints.size(); k++)
						{
							Vector currSkelVec = (Vector)vecLandmarkPoints.get(k);
							for (int m = 0; m < currSkelVec.size(); m++)
							{
								p = (Point)currSkelVec.get(m);
								
						
								//display a rectangle around a border point
								graphics2d.setColor( Color.WHITE );
									
								int n = BORDER_RECT_OFFSET_TINY;
								graphics2d.fillArc( (int)Math.round( (p.getX()-n)*scaleFactor),
									(int)Math.round( (p.getY()-n)*scaleFactor),
									(int)Math.round( (n*2+1)*scaleFactor),
									(int)Math.round( (n*2+1)*scaleFactor),
									0, 360);
									
								
							}
						}
					}
					
				

				} catch (Exception ex)
				{
					ex.printStackTrace();
					System.err.println("dOh #1! " + rectangle + " :: (" + source.getWidth() + "," + source.getHeight() + ")");
				}
			}
			

		} else
		{
			//apply a zommed-in graphics rendering... fairly fast without
			//keeping scaled-up image in memory.
			//note that the grid uses the same coordinate system as the scaled-up image,
			//not the original image
			
			Rectangle rectScaled = new Rectangle();
			
		
			rectScaled.setBounds( (int) (rectangle.getX()/scaleFactor),
				(int) (rectangle.getY()/scaleFactor), (int) (rectangle.getWidth()/scaleFactor),
				(int) (rectangle.getHeight()/scaleFactor) );
			System.err.println(rectScaled);
			
			//make sure we stay within bounds
			if ( (rectScaled.getX() + rectScaled.getWidth() ) >= source.getWidth() )
			{
				System.err.print("Setting new width of rectangle: " + rectScaled.getWidth() );
				
				double newWidth = source.getWidth() - rectScaled.getX() - 1;
				double scaleFactor2 =   (double)rectScaled.getWidth() / newWidth;
				
				rectScaled.setBounds( (int)rectScaled.getX(), (int)rectScaled.getY(),
					(int) (rectScaled.getWidth()*scaleFactor2), (int) (rectScaled.getHeight()*scaleFactor2) );
					
				rectangle.setBounds( (int)rectangle.getX(), (int)rectangle.getY(),
					(int) (rectangle.getWidth()*scaleFactor2), (int) (rectangle.getHeight()*scaleFactor2) );
			
				//also adjust rectangle here
			
				System.err.println (" to " + rectScaled.getWidth() );
			}
			if ( (rectScaled.getY() + rectScaled.getHeight() ) >= source.getHeight() )
			{
				System.err.print("Setting new height of rectangle: " + rectScaled.getHeight() );
				
				double newWidth = source.getWidth() - rectScaled.getX() - 1;
				double scaleFactor2 =  (double)rectScaled.getWidth() / newWidth;
				
				rectScaled.setBounds( (int)rectScaled.getX(), (int)rectScaled.getY(),
					(int) (rectScaled.getWidth()*scaleFactor2), (int) (rectScaled.getHeight()*scaleFactor2) );
					
				rectangle.setBounds( (int)rectangle.getX(), (int)rectangle.getY(),
					(int) (rectangle.getWidth()/scaleFactor2), (int) (rectangle.getHeight()/scaleFactor2) );

					
				System.err.println (" to " + rectScaled.getHeight() );
			
			}
			
			
			if (rectScaled.getWidth() > 0 && rectScaled.getHeight() > 0 && rectScaled.getX() < source.getWidth() && rectScaled.getY() < source.getHeight() )
			{
				
				try
				{
					Raster r = source.getData(rectScaled);
					Raster r2 = r.createTranslatedChild(0, 0);
			
					BufferedImage bufferedImage = new BufferedImage( (int)rectScaled.getWidth(), (int)rectScaled.getHeight(), source.getType());
					bufferedImage.setData(r2);
		
					//graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					//graphics2d.drawImage(bufferedImage, null, (int)rectangle.getX(), (int)rectangle.getY());
					graphics2d.drawImage(bufferedImage, (int)rectangle.getX(), (int)rectangle.getY(),
						(int)rectangle.getWidth(), (int)rectangle.getHeight(), null);
						
					//display a rectangle around a border point
					if (pointVarBorder != null)
					{
						graphics2d.setColor( Color.YELLOW );
						
						//graphics2d.drawRect( (int)Math.round( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
						//	(int)Math.round( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
						//	(int)Math.round( (BORDER_RECT_OFFSET*2+1)*scaleFactor),
						//	(int)Math.round( (BORDER_RECT_OFFSET*2+1)*scaleFactor) );
						
						
						//two lines to form a  cross
						/*
						graphics2d.drawLine( (int)Math.round( (pointVarBorder.getX())*scaleFactor),
							(int)Math.round( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getX())*scaleFactor),
							(int)Math.round( (pointVarBorder.getY()+BORDER_RECT_OFFSET)*scaleFactor) );
							
						graphics2d.drawLine( (int)Math.round( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getY())*scaleFactor),
							(int)Math.round( (pointVarBorder.getX()+BORDER_RECT_OFFSET)*scaleFactor),
							(int)Math.round( (pointVarBorder.getY())*scaleFactor) );
						*/
						graphics2d.drawLine( (int)( (pointVarBorder.getX())*scaleFactor),
							(int)( (pointVarBorder.getY()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getX())*scaleFactor),
							(int)( (pointVarBorder.getY()+BORDER_RECT_OFFSET)*scaleFactor) );
							
						graphics2d.drawLine( (int)( (pointVarBorder.getX()-BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getY())*scaleFactor),
							(int)( (pointVarBorder.getX()+BORDER_RECT_OFFSET)*scaleFactor),
							(int)( (pointVarBorder.getY())*scaleFactor) );
					}
					
					
					//display a rectangle around a border point
					if (pointFixedBorder1 != null)
					{
						graphics2d.setColor( Color.RED );
						
						graphics2d.drawRect( (int)Math.round( (pointFixedBorder1.getX()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (pointFixedBorder1.getY()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor) );
						
						//connect the variable point with the fixed one
						if (pointVarBorder != null && pointFixedBorder1 != null && pointFixedBorder2 == null)
						{
							graphics2d.setColor( Color.RED );
							graphics2d.drawLine( (int)Math.round(pointFixedBorder1.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder1.getY()*scaleFactor),
								(int)Math.round(pointVarBorder.getX()*scaleFactor),
								(int)Math.round(pointVarBorder.getY()*scaleFactor) );
						}
					}
					
					//display a rectangle around a border point
					if (pointFixedBorder2 != null)
					{
						graphics2d.setColor( Color.RED );
						
						graphics2d.drawRect( (int)Math.round( (pointFixedBorder2.getX()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (pointFixedBorder2.getY()-BORDER_RECT_OFFSET_SMALL)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor),
							(int)Math.round( (BORDER_RECT_OFFSET_SMALL*2+1)*scaleFactor) );
						
						//connect the variable point with the fixed one
						if (pointFixedBorder1 != null && pointFixedBorder2 != null)
						{
							graphics2d.setColor( Color.CYAN );
							graphics2d.drawLine( (int)Math.round(pointFixedBorder1.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder1.getY()*scaleFactor),
								(int)Math.round(pointFixedBorder2.getX()*scaleFactor),
								(int)Math.round(pointFixedBorder2.getY()*scaleFactor) );
						}
					}
					
					if (vecLines != null)
					{
						for (int n = 0; n < vecLines.size(); n++)
						{
							//contains horizontal/vertical lines
							Vector[] currVec = (Vector[])vecLines.get(n);
							for (int k = 0; k < currVec.length; k++)
							{
								graphics2d.setColor( Color.CYAN );
								for (int m = (currVec[k].size()-1); m >= 0; m--)
								{
									Point[] pArr = (Point[])currVec[k].get(m);
									
									graphics2d.drawLine( (int)Math.round(pArr[0].getX()*scaleFactor),
										(int)Math.round(pArr[0].getY()*scaleFactor),
										(int)Math.round(pArr[1].getX()*scaleFactor),
										(int)Math.round(pArr[1].getY()*scaleFactor) );
										
									if (m == 1)
									{
										if (k == 0)
											graphics2d.setColor( Color.PINK );
										else
											graphics2d.setColor( Color.PINK );
									}
								}
							}
						}
					}
					
					if (objectIdStr != null && pointObjectLocation != null)
					{
						Point pScaled = new Point( (int)Math.round(pointObjectLocation.getX()*scaleFactor),
							(int)Math.round(pointObjectLocation.getY()*scaleFactor) );
						
						graphics2d.setColor( Color.WHITE );
						graphics2d.setFont( new Font("Times New Roman", Font.PLAIN, 14) );
						graphics2d.drawString( objectIdStr, (int)pScaled.getX(), (int)pScaled.getY() );
					}
					
			
					//System.err.println("Update: scaling at a factor of " + this.scaleFactor);
				} catch (Exception ex)
				{
					//ex.printStackTrace();
					System.err.println("dOh #2! " + rectangle + " :: (" + source.getWidth() + "," + source.getHeight() + ")");
					System.err.println("... " + rectScaled);
				}
			}

		}
		
		/*
		else if (this.scaleType == this.SCALE_MODE_HALF)
		{
				
			Raster r = sourceHalf.getData(rectangle);
			Raster r2 = r.createTranslatedChild(0, 0);

			BufferedImage bufferedImage = new BufferedImage( (int)rectangle.getWidth(), (int)rectangle.getHeight(), BufferedImage.TYPE_INT_RGB);
			bufferedImage.setData(r2);
		
			graphics2d.drawImage(bufferedImage, null, (int)rectangle.getX(), (int)rectangle.getY());
					
		} else if (this.scaleType == this.SCALE_MODE_QUARTER)
		{
			Raster r = sourceQuarter.getData(rectangle);
			Raster r2 = r.createTranslatedChild(0, 0);

			BufferedImage bufferedImage = new BufferedImage( (int)rectangle.getWidth(), (int)rectangle.getHeight(), BufferedImage.TYPE_INT_RGB);
			bufferedImage.setData(r2);
		
			graphics2d.drawImage(bufferedImage, null, (int)rectangle.getX(), (int)rectangle.getY());
				
		}
		
		*/
		//System.err.println("paintComponent accessed...");
		//System.err.println(rectangle.getX() + "::" + rectangle.getY() );
	
	}
	
	/**
	* Un-assignes lines etc. that are otherwise drawn on top of the image
	* 
	*/
	public void removeDrawnObjects()
	{
		objectId = -1;
		objectIdStr = null;
		pointObjectLocation = null;
		vecLines = null;
		pointVarBorder = null;
		pointFixedBorder1 = null;
		pointFixedBorder2 = null;
		vecLandmarkPoints = null;
	}
	
	/**
	* Checks if any image is assigned to the object
	* 
	* @return	True if an image is assigned, false otherwise
	*/
	public boolean isSet()
	{
		return (sourceOrg != null);
	}
	
	/**
	* Retrieves the scale version of the assigned image
	* 
	* @return	The scaled image
	*/
	public BufferedImage getScaled()
	{
		return source;
	}
	
	/**
	* Retrieves the original version of the assigned image
	* 
	* @return	The original image
	*/
	public BufferedImage get()
	{
		return sourceOrg;
	}

	/**
	* Apply scaling to the assigned image
	* 
	*/
	private void scaleImage()
	{
		if (sourceOrg == null)
			return;
		
		BufferedImage scaledImage = new BufferedImage( (int)(sourceOrg.getWidth()*scaleFactor), 
			(int)(sourceOrg.getHeight()*scaleFactor), BufferedImage.TYPE_INT_RGB);
			
		//System.err.println("New image dimensions: (" + scaledImage.getWidth() + "," + scaledImage.getHeight() + ")");
		
		Graphics2D g2 = scaledImage.createGraphics();
		//g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(sourceOrg, 0, 0, scaledImage.getWidth(), scaledImage.getHeight(), null);
		
		source = scaledImage;
	}
	
	/**
	* Draws a rectangle on top of the underlying BufferedImage
	* and flush the changes to the graphics device.
	*
	* @param	rect	The rectangle
	* @param	c	The color of the rectangle
	* @return	Returns a raster of the region _before_ drawing
	*/
	public Raster drawRect(Rectangle rect, Color c)
	{
		Raster rasterRet;
		
		//check if we are using the scaled image
		if ( Math.abs(scaleFactor - 1.0) > epsilon)
		{
			//adjust the bounds to cope with the scaling
			Rectangle rectScaled = new Rectangle();
			rectScaled.setBounds( (int)(rect.getX()/scaleFactor),
				(int)(rect.getY()/scaleFactor),
				(int)(rect.getWidth()/scaleFactor),
				(int)(rect.getHeight()/scaleFactor) );
		
			
			rasterRet = sourceOrg.getData(rectScaled).createTranslatedChild( (int)rectScaled.getX(),
				(int)rectScaled.getY());


			WritableRaster wrTemp = sourceOrg.getData(rectScaled).createCompatibleWritableRaster();
			//wrTemp.createTranslatedChild( (int)rectScaled.getX(),
			//	(int)rectScaled.getY());

			//System.err.println("Drawing rect: " + rect);
			//System.err.println("WritableRaster: " + wrTemp);
			
			for (int x = 0; x < wrTemp.getWidth(); x++)
				for (int y = 0; y < wrTemp.getHeight(); y++)
				{
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_R, c.getRed() );
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_G, c.getGreen() );
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_B, c.getBlue() );
				}
		
			Raster r = wrTemp.createTranslatedChild( (int)rectScaled.getX(),
				(int)rectScaled.getY());
			sourceOrg.setData(r);
			

			//non-dynamic resizing calls for some extra features
			if (this.scaleType != this.SCALE_MODE_DYNAMIC)
			{
				// first draw the original image ...
				/*
				rectScaled.setBounds( (int)(rect.getX()/scaleFactor),
					(int)(rect.getY()/scaleFactor),
					(int)(rect.getWidth()/scaleFactor),
					(int)(rect.getHeight()/scaleFactor) );
		
				System.err.println("Drawing at zoomed-out image...");
				
				WritableRaster wrTemp = sourceOrg.getData(rectScaled).createCompatibleWritableRaster();
				//System.err.println("Drawing rect: " + rect);
				//System.err.println("WritableRaster: " + wrTemp);
			
				for (int x = 0; x < wrTemp.getWidth(); x++)
					for (int y = 0; y < wrTemp.getHeight(); y++)
					{
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_R, c.getRed() );
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_G, c.getGreen() );
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_B, c.getBlue() );
					}
		
				Raster r = wrTemp.createTranslatedChild( (int)rectScaled.getX(),
					(int)rectScaled.getY());
				sourceOrg.setData(r);
				*/
				
				// ... and then draw the scaled image
				wrTemp = source.getData(rect).createCompatibleWritableRaster();
				//System.err.println("Drawing rect: " + rect);
				//System.err.println("WritableRaster: " + wrTemp);
			
				for (int x = 0; x < wrTemp.getWidth(); x++)
					for (int y = 0; y < wrTemp.getHeight(); y++)
					{
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_R, c.getRed() );
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_G, c.getGreen() );
						wrTemp.setSample(x, y, PlanarImageEdit.BAND_B, c.getBlue() );
					}
		
				Raster r2 = wrTemp.createTranslatedChild( (int)rect.getX(), (int)rect.getY());
				source.setData(r2);
				
			} else
			{
				source = sourceOrg;
			}



			/*
			// finally, draw to the screen
			Graphics2D graphics2d = (Graphics2D)this.getGraphics();
			graphics2d.setColor(c);
			graphics2d.fillRect( (int)rect.getX(), (int)rect.getY(),
				(int)rect.getWidth(), (int)rect.getHeight() );
			*/
			this.repaint(rect);

		} else
		{
			System.err.println("Drawing at zoom 1.0...");
			
			rasterRet = source.getData(rect).createTranslatedChild( (int)rect.getX(), (int)rect.getY());
			
			
			WritableRaster wrTemp = source.getData(rect).createCompatibleWritableRaster();
			//System.err.println("Drawing rect: " + rect);
			//System.err.println("WritableRaster: " + wrTemp);
		
			for (int x = 0; x < wrTemp.getWidth(); x++)
				for (int y = 0; y < wrTemp.getHeight(); y++)
				{
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_R, c.getRed() );
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_G, c.getGreen() );
					wrTemp.setSample(x, y, PlanarImageEdit.BAND_B, c.getBlue() );
				}
		
			Raster r = wrTemp.createTranslatedChild( (int)rect.getX(), (int)rect.getY());
			
			source.setData(r);
			
			//System.err.println("Raster: " + r);
			//source = sourceOrg;
			
			sourceOrg = source;
		
			this.repaint(rect);
		}
		
		//System.err.println("Raster bounds: " + rasterRet.getBounds() );
		return rasterRet;
		
	}

	/*
	private final int XtoTileX(int i)
	{
		return (int)Math.floor((double)(i - tileGridXOffset) / (double)tileWidth);
	}

	private final int YtoTileY(int i)
	{
		return (int)Math.floor((double)(i - tileGridYOffset) / (double)tileHeight);
	}

	private final int TileXtoX(int i)
	{
		return i * tileWidth + tileGridXOffset;
	}

	private final int TileYtoY(int i)
	{
		return i * tileHeight + tileGridYOffset;
	}
	*/


	/*
	public final void mouseEntered(MouseEvent mouseevent)
	{
	}

	public final void mouseExited(MouseEvent mouseevent)
	{
	}

	public void mousePressed(MouseEvent mouseevent)
	{
		System.err.println("Mouse pressed at " + mouseevent.getPoint() );
		
		//Point point = mouseevent.getPoint();
		//int i = mouseevent.getModifiers();
	}

	public final void mouseReleased(MouseEvent mouseevent)
	{
		//Point point = mouseevent.getPoint();
		
		//System.err.println("Mouse release: " + point);
	}

	public final void mouseClicked(MouseEvent mouseevent)
	{
	}

	public final void mouseMoved(MouseEvent mouseevent)
	{
		//Point point = mouseevent.getPoint();
	}

	public final void mouseDragged(MouseEvent mouseevent)
	{
		mousePressed(mouseevent);
	}
	*/

}
