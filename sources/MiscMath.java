/* MiscMath.java
*
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class with functions to perform basic arithmetic
 * operations, e.g. mean, median etc.
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
import java.util.Arrays;
import java.util.Vector;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
    * A class that performs basic mathematical operations
*/
public class MiscMath
{
	/**
	* Calculates the median value of an integer array.
	*
	* @param 	v	Array
	* @return	The median of the values in the vector.
	*/
	public static double median(int[] v)
	{
	    if (v.length == 0)
		{
			return Double.NaN;
		} else if (v.length == 1)
		{
			return v[0];
		} else
		{
		
			//first copy array
			int[] vSorted = new int[v.length];
			for (int i = 0; i < v.length; i++)
				vSorted[i]=v[i];
			//sort it..
			Arrays.sort(vSorted);
			
			//.. and extract median
			int centr = vSorted.length/2;  // subscript of centr elem.
		    if ( (vSorted.length % 2) == 1) {
		        // Odd number of elements -- return the middle one.
		        return vSorted[centr];
		    } else
			{
		       // Even number -- return average of middle two
		       // Must cast the numbers to double before dividing.
		       return (vSorted[centr-1] + vSorted[centr]) / 2.0;
		    }
		}
	}
	
	/**
	* Calculates the median value of a double array.
	*
	* @param 	v	Array
	* @return	The median of the values in the vector.
	*/
	public static double median(double[] v)
	{
	    if (v.length == 0)
		{
			return Double.NaN;
		} else if (v.length == 1)
		{
			return v[0];
		} else
		{
		
			//first copy array
			double[] vSorted = new double[v.length];
			for (int i = 0; i < v.length; i++)
				vSorted[i]=v[i];
			//sort it..
			Arrays.sort(vSorted);
			
			//.. and extract median
			int centr = vSorted.length/2;  // subscript of centr elem.
		    if ( (vSorted.length % 2) == 1) {
		        // Odd number of elements -- return the middle one.
		        return vSorted[centr];
		    } else
			{
		       // Even number -- return average of middle two
		       // Must cast the numbers to double before dividing.
		       return (vSorted[centr-1] + vSorted[centr]) / 2.0;
		    }
		}
	}
	
	/**
	* Calculates the median value of a Vector of Numbers
	*
	* @param 	v	Vector
	* @return	The median of the values in the vector.
	*/
	public static double median(Vector v)
	{
	    //first copy array
		double[] vCopy = new double[v.size()];
		for (int i = 0; i < v.size(); i++)
			vCopy[i] = ( (Number)v.get(i) ).doubleValue();
		
		return median(vCopy);
	}
	
	/**
	* Calculates a quantile from values in an int[][] array.
	*
	* @param 	v	array
	* @param	q	Quantile ( >= 0 and <= 1 )
	* @return	The quantile of the values in the vector.
	*/
	public static double quantile(int[][] v, double q)
	{
	    //first copy array...
		int totLength = v.length*v[0].length;
		int[] vCopy = new int[totLength];
		int index = 0;
		for (int y = 0; y < v.length; y++)
			for (int x = 0; x < v[0].length; x++)
				vCopy[index++] = v[y][x];
		
		//...then use the normal function to find the quantile
		return quantile(vCopy, q);
	}
	
	/**
	* Calculates a quantile from values in an integer array.
	*
	* @param 	v	array
	* @param	q	Quantile ( >= 0 and <= 1 )
	* @return	The quantile of the values in the vector.
	*/
	public static double quantile(int[] v, double q)
	{
	    if (v.length == 0)
		{
			return Double.NaN;
		} else if (v.length == 1)
		{
			return v[0];
		} else
		{
		
			//first copy array
			int[] vSorted = new int[v.length];
			for (int i = 0; i < v.length; i++)
				vSorted[i]=v[i];
			//sort it..
			Arrays.sort(vSorted);
			
			//.. and extract median
			int centr1 = (int)Math.floor(vSorted.length*q);
			int centr2 = (int)Math.ceil(vSorted.length*q);
		    if ( (vSorted.length % 2) == 1) {
		        // Odd number of elements -- return the middle one.
		        return vSorted[centr1];
		    } else
			{
		       // Even number -- return average of middle two
		       // Must cast the numbers to double before dividing.
		       return (vSorted[centr1] + vSorted[centr2]) / 2.0;
		    }
		}
	}
	
	/**
	* Calculates a quantile from values in a double array.
	*
	* @param 	v	array
	* @param	q	Quantile ( >= 0 and <= 1 )
	* @return	The quantile of the values in the vector.
	*/
	public static double quantile(double[] v, double q)
	{
	    if (v.length == 0)
		{
			return Double.NaN;
		} else if (v.length == 0)
		{
			return v[0];
		} else
		{
			//first copy array
			double[] vSorted = new double[v.length];
			for (int i = 0; i < v.length; i++)
				vSorted[i]=v[i];
			//sort it..
			Arrays.sort(vSorted);
			
			//.. and extract median
			int centr1 = (int)Math.floor(vSorted.length*q);
			int centr2 = (int)Math.ceil(vSorted.length*q);
		    if ( (vSorted.length % 2) == 1) {
		        // Odd number of elements -- return the middle one.
		        return vSorted[centr1];
		    } else
			{
		       // Even number -- return average of middle two
		       // Must cast the numbers to double before dividing.
		       return (vSorted[centr1] + vSorted[centr2]) / 2.0;
		    }
		}
	}

	/**
	* Calculates the mean from the values in an integer vector.
	*
	* @param 	v	array
	* @return	The mean
	*/
	public static double mean(int[] v)
	{
	    // calculate total sum
		double sum = 0;
		for (int i = 0; i < v.length; i++)
			sum += v[i];
		
		// return the average
		return (sum/v.length);
	}
	
	/**
	* Calculates the mean from the values in a double vector.
	*
	* @param 	v	array
	* @return	The mean
	*/
	public static double mean(double[] v)
	{
	    // calculate total sum
		double sum = 0;
		for (int i = 0; i < v.length; i++)
			sum += v[i];
		
		// return the average
		return (sum/v.length);
	}
	
	/**
	* Calculates the mean from the values in a Vector of Numbers
	*
	* @param 	v	array
	* @return	The mean
	*/
	public static double mean(Vector v)
	{
	    // calculate total sum
		double sum = 0;
		double d;
		for (int i = 0; i < v.size(); i++)
		{
			d = ( (Number)v.get(i)).doubleValue();
			sum += d;
		}
		
		// return the average
		return (sum/v.size());
	}
	
	/**
	* Calculates the sum of the values in a double vector.
	*
	* @param 	v	array
	* @return	The sum
	*/
	public static double sum(double[] v)
	{
	    // calculate total sum
		double sum = 0;
		for (int i = 0; i < v.length; i++)
			sum += v[i];
		
		// return the average
		return sum;
	}

	/**
	* Calculates the standard deviation of the values in an integer vector,
	* (with sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @return	The standard deviation 
	*/
	public static double stdev(int[] v, double mean)
	{
	    return stdev(v, mean, 1); //default stddev is with (sample) bias correction
	}
	
	/**
	* Calculates the standard deviation of the values in an integer vector,
	* (with or without sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @param	correction	Sample size will be corrected by this number (typically 0 or 1).
	* @return	The standard deviation 
	*/
	public static double stdev(int[] v, double mean, int correction)
	{
	    // calculate total sum
		double stdsum = 0;
		for (int i = 0; i < v.length; i++)
			stdsum += ( (v[i]-mean)*(v[i]-mean) );
		
		// return the average
		return Math.sqrt(stdsum/(v.length-correction));
	}
	
	
	/**
	* Calculates the standard deviation of the values in a double vector,
	* (with sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @return	The standard deviation 
	*/
	public static double stdev(double[] v, double mean)
	{
	    return stdev(v, mean, 1); //default stddev is with (sample) bias correction
	}
	
	/**
	* Calculates the standard deviation of the values in a double vector,
	* (with or without sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @param	correction	Sample size will be corrected by this number (typically 0 or 1).
	* @return	The standard deviation 
	*/
	public static double stdev(double[] v, double mean, int correction)
	{
	    // calculate total sum
		double stdsum = 0;
		for (int i = 0; i < v.length; i++)
			stdsum += ( (v[i]-mean)*(v[i]-mean) );
		
		// return the average
		return Math.sqrt(stdsum/(v.length-correction));
	}
	
	/**
	* Calculates the standard deviation of the values in a Vector.
	* (with sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @return	The standard deviation 
	*/
	public static double stdev(Vector v, double mean)
	{
		return stdev(v, mean, 1); //default stddev is with (sample) bias correction
	}
	
	/**
	* Calculates the standard deviation of the values in a Vector.
	* (with or without sample bias correction).
	*
	* @param 	v	array
	* @param	mean	The mean value in the vector
	* @param	correction	Sample size will be corrected by this number (typically 0 or 1).
	* @return	The standard deviation 
	*/
	public static double stdev(Vector v, double mean, int correction)
	{
	    // calculate total sum
		double stdsum = 0;
		double d;
		for (int i = 0; i < v.size(); i++)
		{
			d = ( (Number)v.get(i)).doubleValue();
			stdsum += ( (d-mean)*(d-mean) );
		}
		
		// return the average
		return Math.sqrt(stdsum/(v.size()-correction));
	}
	
	
	/**
	* Calculates the maximum value in an integer array.
	*
	* @param 	v	array
	* @return	The maximum value.
	*/
	public static double max(int[] v)
	{
		double maxVal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < v.length; i++)
			if (v[i] > maxVal)
				maxVal = v[i];
	
		return maxVal;
	}
	
	/**
	* Calculates the maximum value in an int[][] array
	*
	* @param 	v	array
	* @return	The maximum value.
	*/
	public static double max(int[][] v)
	{
		double maxVal = Double.NEGATIVE_INFINITY;
		double currVal = 0.0;
		for (int i = 0; i < v.length; i++)
		{
			currVal = MiscMath.max(v[i]);
			if ( currVal > maxVal)
				maxVal = currVal;
		}
		
		return maxVal;
	}
	
	/**
	* Calculates the minimum value in an integer array
	*
	* @param 	v	array
	* @return	The minimum value.
	*/
	public static double min(int[] v)
	{
		double minVal = Double.POSITIVE_INFINITY;
		for (int i = 0; i < v.length; i++)
			if (v[i] < minVal)
				minVal = v[i];
	
		return minVal;
	}
	
	/**
	* Calculates the minimum value in an int[][] array
	*
	* @param 	v	array
	* @return	The minimum value.
	*/
	public static double min(int[][] v)
	{
		double minVal = Double.POSITIVE_INFINITY;
		double currVal = 0.0;
		for (int i = 0; i < v.length; i++)
		{
			currVal = MiscMath.min(v[i]);
			if (currVal < minVal)
				minVal = currVal;
		}
		return minVal;
	}
	
	/**
	* Calculates the sum of a boolean array (false = 0, true = 1).
	*
	* @param 	v	array
	* @return	The sum.
	*/
	public static int sum(boolean[] v)
	{
		int retVal = 0;
		for (int i = 0; i < v.length; i++)
		{
			retVal += (v[i] ? 1 : 0);
		}
		return retVal;
	}
	
	
	/**
	* Calculates the angle between two points
	*
	* @param 	p1	The first point
	* @param	p2	The second point
	* @return	The angle between the points
	*/
	public static double pointAngle(Point p1, Point p2)
	{
		double angle = 90.0;
		double deltaX = p1.getX() - p2.getX();
		double deltaY = p1.getY() - p2.getY();
		if (deltaX != 0)
			angle = Math.atan( deltaY / deltaX )*180/Math.PI;
		while (angle <= 0)
			angle += 360;
	
		return angle;
	}
	
	/**
	* calculates the intersection point for two lines, described as Point[2] objects
	*
	* @param 	line1	The first line
	* @param	line2	The second line
	* @return	The intersection point of the two lines (if any)
	*/
	public static Point2D.Double lineIntersection(Point[] line1, Point[] line2)
	{
		//assess the most probably horizontal/vertical lines
		double absDiffXY1 = Math.abs(line1[0].getX()-line1[1].getX()) - Math.abs(line1[0].getY()-line1[1].getY());
		double absDiffXY2 = Math.abs(line2[0].getX()-line2[1].getX()) - Math.abs(line2[0].getY()-line2[1].getY());
		
		Point[] lineHoriz, lineVert;
		if (absDiffXY1 > absDiffXY2)
		{
			lineHoriz = line1;
			lineVert = line2;
		} else
		{
			lineHoriz = line2;
			lineVert = line1;
		}
		
		double slope1 = (double)(lineHoriz[1].getY() - lineHoriz[0].getY())/(double)(lineHoriz[1].getX() - lineHoriz[0].getX() );
		double intercept1 = 0.0;
		boolean slopeMissing1 = (slope1 == Double.POSITIVE_INFINITY || slope1 == Double.NEGATIVE_INFINITY || Double.isNaN(slope1));
		// || slope1 == 0.0
		if (slopeMissing1)
		{
			slope1 = 0.0;
			intercept1 = lineHoriz[1].getY(); //not the intercept, but can be useful later
			//System.err.println("  ** 1 slope is undefined");
		} else
		{
			intercept1 = -slope1*lineHoriz[1].getX() + lineHoriz[1].getY();
		}
		
		//System.err.println("  Line descr. the first gradient: y = " + slope1 + "*x + " + intercept1 );

		double slope2 = (double)(lineVert[1].getY() - lineVert[0].getY())/(double)(lineVert[1].getX() -lineVert[0].getX() );
		double intercept2 = 0.0;
		boolean slopeMissing2 =  (slope2 == Double.POSITIVE_INFINITY || slope2 == Double.NEGATIVE_INFINITY || Double.isNaN(slope2) );
		if (slopeMissing2)
		{
			slope2 = 0.0;
			intercept2 = lineVert[1].getX(); //not the intercept, but can be useful later
			//System.err.println("  ** 2 slope is undefined");
		} else
		{
			intercept2 = -slope2*lineVert[1].getX() + lineVert[1].getY();
		}
		//System.err.println("  Line descr. the second gradient: y = " + slope2 + "*x + " + intercept2 );

		
		//calc the intersection point
		//double intersect1 = (intercept2-intercept1)/(slope1 - slope2);
		//double intersect2 = (slope1*intersect1)+intercept1;
		double intersectX = Double.NaN;
		double intersectY = Double.NaN;
		if (slopeMissing2 && slopeMissing1)
		{
			intersectX = intercept2;
			intersectY = intercept1;
		} else if (slopeMissing1)
		{
			intersectY = intercept1;
			intersectX = intercept1*slope2 + intercept2;
			
		} else if (slopeMissing2)
		{
			intersectX = intercept2;
			intersectY = intercept2	*slope1 + intercept1; 
			
		} else
		{
			intersectX = (intercept2-intercept1)/(slope1 - slope2);
			intersectY = (slope1*intersectX)+intercept1;
		}
		
		//System.err.println("  Intersection is at (" + intersectX + "," + intersectY + ")");
		
		return (new Point2D.Double( intersectX, intersectY) );
	}

	/**
	* Finds a point on a pre-defined relative distance on a line.
	* 0 corresponds to the start of the line, 0.5 to the center and
	* 1.0 to the end of the line, where  line[0] is the starting point
	* and line[1] is the end point.
	*
	* 
	* @param	line	The point to adjust
	* @param	relDist	The relative distance adjustment
	* @return A Point[2] describing a line, forming a perpendicular ling
	*/
	public static Point2D.Double findPointOnLine(Point[] line, double relDist)
	{
		Point2D.Double pRet = new Point2D.Double();
		
		int lineDeltaX = (int)Math.abs(line[1].getX() - line[0].getX());
		int lineDeltaY = (int)Math.abs(line[1].getY() - line[0].getY());
		int origX = (int)Math.min(line[0].getX(), line[1].getX());
		int origY = (int)Math.min(line[0].getY(), line[1].getY());
		
		//double slope = (double)lineDeltaY/(double)lineDeltaX;
		//boolean slopeMissing = (slope == Double.POSITIVE_INFINITY || slope == Double.NEGATIVE_INFINITY || Double.isNaN(slope));
		
		//double intercept = 0.0:
		if (lineDeltaX == 0)
		{
			//we have no slope, hence we only adjust Y
			pRet = new Point2D.Double( origX, origY + relDist*lineDeltaY );
		
		} else if (lineDeltaY == 0)
		{
			//slope is zero, hence we only adjust X
			pRet = new Point2D.Double( origX + relDist*lineDeltaX, origY );
		} else
		{
			//we have to use simple trigonometry to find the correct position
			//first define the distance from A to B that will be used
			// this is the length of the hypotenuse
			double dist = relDist*line[0].distance(line[1]); //hypotenuse
			double angle = Math.atan( (double)lineDeltaY / (double)lineDeltaX ); //angle
			double yCoord = dist*Math.sin(angle); //opposite
			double xCoord = dist*Math.cos(angle); //adjacent
			
			pRet = new Point2D.Double( origX + xCoord, origY + yCoord );
			
		} 
	
		return pRet;
	}
}
