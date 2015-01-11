/* GrayscaleImageEdit.java
 *
 * Copyright (c) Max Bylesjö, 2007-2008
 *
 * A class with functions to perform basic grayscale image editing
 * procedures.
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
import java.util.Random;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.*;
import javax.swing.*;

/**
    * A class that performs basic edit/statistics operations for grayscale images
*/
public class GrayscaleImageEdit
{
	public final static double FILTER_DENS = 0.25; // pixel area / (height*width)
	public final static double FILTER_AREA = 0.0005; //pixels in object / (heigh*width) of image
	public final static double STDDEV_MIN = 0.05; //assume something went wrong if stdev is lower than this value
	public final static double ZERO_REL_MIN = 0.001; //assume something went wrong if we get mostly zero entries in matrix
	public final static double ANGLE_INDENT_ORTHO_SLACK = 25.0; //the allowed slack when finding approximately orthogonal projections (for indents)
	public final static double ANGLE_DIST_ORTHO_SLACK = 5.0; //the allowed slack when finding approximately orthogonal projections (for indents)
	public final static double INDENT_DEPTH_THRESH = 1.0; //if the indent depth is shallower than this, don't count it as a 'real' indent, just a connection between two hotspots
	
	//public final static byte OPTION_SIZE_NORMAL = 0;
	//public final static byte OPTION_SIZE_FORCE_ORTHO = 1;
	public final static byte DIRECTION_NONE = 0;
	public final static byte DIRECTION_SE = 1;
	public final static byte DIRECTION_SW = 2;
	public final static byte DIRECTION_NW = 3;
	public final static byte DIRECTION_NE = 4;
	public final static byte DIST_EUCLIDEAN = 0;
	public final static byte DIST_MANHATTAN = 1;
	public final static double EPS = 1e-12;
	public final static double SQRT2 = Math.sqrt(2);
	
	

	/**
	* Calculates the standard deviation using a sliding window over an image. 
	* The window size is specified by the user but should be of uneven size.
	*
	* @param	img	Grayscale image (as int[][] matrix)
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	* @return	a vector of standard deviations, one for each window
	*/
	public static double[] calcStds(int[][] img, int h, int w)
	{
		//fetch height/width...
		int imgWidth = img[0].length;
		int imgHeight = img.length;
		//...and calculate the maximum area we can search with this window size
		int xDelta = (int)((w-1)/2.0);
		int yDelta = (int)((h-1)/2.0);
		int xMin = xDelta;
		int xMax = imgWidth-xDelta;
		int yMin = yDelta;
		int yMax = imgHeight-yDelta;
		
		int[] window = new int[h*w];
		double[] ret = new double[(imgWidth-2*xDelta)*(imgHeight-2*yDelta)];
		
		//x works the width and y the height
		int ind1 = 0;
		int ind2 = 0;
		
		for (int y = yMin; y < yMax; y++)
			for (int x = xMin; x < xMax; x++)
			{
				ind2 = 0;
				for (int xx = (x - xDelta); xx <= (x + xDelta); xx++)
					for (int yy = (y - yDelta); yy <= (y + yDelta); yy++)
					{
						window[ind2++] = img[yy][xx];
					}
				
				double mean = MiscMath.mean(window);
				double stdev = MiscMath.stdev(window, mean);
				ret[ind1++]=stdev;
			}
		
		return ret;
	}
	
	/**
	* Replaces the values of all pixels by the median of all pixels in a window
	* surrounding the current window.
	* The window size is specified by the user but should be of uneven size.
	*
	* @param	imgTemplate	Grayscale image (as int[][] matrix)
	* @param	img	Grayscale image that will be replaced with median values
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	*/
	public static void medianFilter(int[][] imgTemplate, int[][] img, int h, int w)
	{
		//fetch height/width...
		int imgWidth = imgTemplate[0].length;
		int imgHeight = imgTemplate.length;
		//...and calculate the maximum area we can search with this window size
		int xDelta = (int)((w-1)/2.0);
		int yDelta = (int)((h-1)/2.0);
		int xMin = xDelta;
		int xMax = imgWidth-xDelta;
		int yMin = yDelta;
		int yMax = imgHeight-yDelta;
		
		int[] window = new int[h*w];
		//double[] ret = new double[(imgWidth-2*xDelta)*(imgHeight-2*yDelta)];
		
		//x works the width and y the height
		//int ind1 = 0;
		int ind2 = 0;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			{
				if (x < xMin || x >= xMax || y < yMin || y >= yMax)
					img[y][x] = (int)255; //borders
				else
				{
					
					ind2 = 0;
					for (int yy = (y - yDelta); yy <= (y + yDelta); yy++)
						for (int xx = (x - xDelta); xx <= (x + xDelta); xx++)
						{
							window[ind2++] = imgTemplate[yy][xx];
						}
					
					double medianVal = MiscMath.median(window);
					img[y][x] = (int)medianVal;
					
					//if ( (int)medianVal != 255)
					//	System.err.print( (int)medianVal + ",");
					
				}
			}
	}
	
	
	
	/**
	*Thresholds an image (to 0 or 255) using a fixed threshold.
	*
	* @param	imgTemplate	Grayscale image used as template (as int[][] matrix)
	* @param	img	Grayscale image to be altered (thresholded) (as int[][] matrix)
	* @param	thresh	Threshold
	*/
	public static void thresholdImage(int[][] imgTemplate, int[][] img, int thresh)
	{
		for (int h = 0; h < imgTemplate.length; h++)
				for (int w = 0; w < imgTemplate[0].length; w++)
				{
					img[h][w] = (imgTemplate[h][w] <= thresh) ? (int)255 : (int) 0;
				}
		
	}
	
	/**
	*Thresholds an image (to 0 or 255) using a fixed threshold.
	*
	* @param	imgTemplate	Double image used as template (as double[][] matrix)
	* @param	img	Grayscale image to be altered (thresholded) (as int[][] matrix)
	* @param	thresh	Threshold
	*/
	public static void thresholdImage(double[][] imgTemplate, int[][] img, double thresh)
	{
		for (int h = 0; h < imgTemplate.length; h++)
				for (int w = 0; w < imgTemplate[0].length; w++)
				{
					img[h][w] = (imgTemplate[h][w] <= thresh) ? (int)255 : (int) 0;
				}
		
	}
	
	
	/**
	* Truncates an image with a value below a certain threshold to a certain value.
	*
	* @param	imgTemplate	Grayscale image used as template (as int[][] matrix)
	* @param	img	Grayscale image to be altered (thresholded) (as int[][] matrix)
	* @param	thresh	Threshold
	* @param	lowValue	New value of pixel which is below threshold
	*/
	public static void truncateImage(int[][] imgTemplate, int[][] img, double thresh, int lowValue)
	{
		for (int h = 0; h < imgTemplate.length; h++)
				for (int w = 0; w < imgTemplate[0].length; w++)
				{
					img[h][w] = (imgTemplate[h][w] <= thresh) ? lowValue : imgTemplate[h][w];
				}
		
	}
	
	
	/**
	* Counts the fraction of entries in the a binary matrix that is zero
	*
	* @param	img	Grayscale image to be counted
	* @return	The fraction of zeros in the binary matrix
	*/
	public static double countFractionZero(byte[][] img)
	{
		long total = img.length*img[0].length;
		long zero = 0;
		
		for (int h = 0; h < img.length; h++)
			for (int w = 0; w < img[0].length; w++)
			{
				if (img[h][w] == 0)
					zero++;	
			}
		
		return (double)zero/(double)total;
	}
	
	/**
	* Counts the fraction of entries in a matrix that is zero
	*
	* @param	img	Grayscale image to be counted
	* @return	The fraction of zeros in the binary matrix
	*/
	public static double countFractionZero(int[][] img)
	{
		long total = img.length*img[0].length;
		long zero = 0;
		
		for (int h = 0; h < img.length; h++)
			for (int w = 0; w < img[0].length; w++)
			{
				if (img[h][w] == 0)
					zero++;	
			}
		
		return (double)zero/(double)total;
	}
	
	
	/**
	* Applies a double[][] mask to a int[][] matrix
	* 
	* @param  mat	The image which should be altered
	* @param	mask	Mask to apply 
	* @param	truncate	If true, the values will be truncated at 255
	* @return	The image with the mask applied
	*/
	public static int[][] applyMask(int[][] mat, double[][] mask, boolean truncate)
	{
		int matWidth = mat[0].length;
		int matHeight = mat.length;
		int truncVal = 255;

		//construct object
		int[][] retMat = new int[matHeight][matWidth];
		
		for (int xx = 0; xx < matWidth; xx++)
			for (int yy = 0; yy < matHeight; yy++)
			{
				
				retMat[yy][xx] = (int)Math.round(mat[yy][xx]*mask[yy][xx]);
				if (truncate && retMat[yy][xx] > truncVal)
					retMat[yy][xx] = truncVal;
			}
			
		return retMat;
	}	

	
	
	/**
	* Detects an approximate threshold of an object that minimizes the variation (standard deviation)
	* of the thresholded image. The searches all possible settings from 0 to 255 (with a pre-specified step length)
	* and uses the value with the lowest stddev.
	*
	* @param	imgTemplate	Grayscale image to be used as a template (as int[][] matrix)
	* @param	img	Grayscale image that can be modified (as int[][] matrix)
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	* @param	deltaThresh	The step length used for the threshold
	* @param	minVal	The minimum allowed value in the iteration
	* @param	maxVal	The maximum allowed value in the iteration
	* @param	frame	A (extended) frame object, which can be used to keep track of whether the run was terminated beforehand
	* @param	pb	A progressbar object, which is used to set the progress of the algorithm
	* @return	The best threshold value according to the search function
	*/
	public static double detectThresholdExhaustive(double[][] imgTemplate, int[][] img, int h, int w, double deltaThresh, double minVal, double maxVal, JFrameExt frame, JProgressBar pb)
	{
		int numSteps = (int)Math.ceil( (maxVal-minVal)/deltaThresh)+1;
		System.err.println("Number of steps: "+ numSteps);
		
		double currVal = minVal;
		double stdDevMin = Double.POSITIVE_INFINITY;
		double stdDevMinVal = -1;
		
		for (int i = 0; i < numSteps; i++)
		{
	
			thresholdImage(imgTemplate, img, currVal);
			double stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
			//System.err.println("Stddev at threshold " + currVal + " is " + stdDevCurrent);
			
			
			double fracZero = countFractionZero(img);
			double fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
			//System.err.println("  Majority/minority pixel count is " + fracZeroComp);
			if ( Math.abs(fracZeroComp) < EPS || Math.abs(stdDevCurrent) < EPS)
				stdDevCurrent = Double.POSITIVE_INFINITY;
			
			if (stdDevCurrent < stdDevMin)
			{
				if (fracZeroComp > GrayscaleImageEdit.ZERO_REL_MIN)
				{
					stdDevMin = stdDevCurrent;
					stdDevMinVal = currVal;
				} else
				{
				
					//we have a too low value of stddev, which is usually synonymous with a total truncatation,
					//that we hope to avoid
				}
			}
			
			pb.setValue( (int)Math.round( (double)i*100/numSteps) );
			if (frame.getCancelled())
				return -1;
				
			currVal += deltaThresh;
		}
		pb.setValue(100);
		
		return stdDevMinVal;
	}
	
	/**
	* Detects an approximate threshold of an object that minimizes the variation (standard deviation)
	* of the thresholded image. The searches all possible settings from 0 to 255 (with a pre-specified step length)
	* and uses the value with the lowest stddev.
	*
	* @param	imgTemplate	Grayscale image to be used as a template (as int[][] matrix)
	* @param	img	Grayscale image that can be modified (as int[][] matrix)
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	* @param	deltaThresh	The step length used for the threshold
	* @param	minVal	The minimum allowed value in the iteration
	* @param	maxVal	The maximum allowed value in the iteration
	* @param	frame	A (extended) frame object, which can be used to keep track of whether the run was terminated beforehand
	* @param	pb	A progressbar object, which is used to set the progress of the algorithm
	* @return	The best threshold value according to the search function
	*/
	public static double detectThresholdExhaustive(int[][] imgTemplate, int[][] img, int h, int w, int deltaThresh, int minVal, int maxVal, JFrameExt frame, JProgressBar pb)
	{
		while (minVal <= 0)
			minVal += deltaThresh;
		
		int numSteps = (int)Math.ceil( (maxVal-minVal)/deltaThresh)+1;
		//System.err.println("Number of steps: "+ numSteps);
		
		int currVal = minVal;
		double stdDevMin = Double.POSITIVE_INFINITY;
		double stdDevMinVal = -1;
		
		for (int i = 0; i < numSteps; i++)
		{
	
			thresholdImage(imgTemplate, img, currVal);
			
			double stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
			
			
			double fracZero = countFractionZero(img);
			double fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
			//System.err.println("  Majority/minority pixel count is " + fracZeroComp);
			
			if ( Math.abs(fracZeroComp) < EPS || Math.abs(stdDevCurrent) < EPS)
				stdDevCurrent = Double.POSITIVE_INFINITY;
			//else
			//	stdDevCurrent /= fracZeroComp;
				
			//System.err.println("Stddev at threshold " + currVal + " is " + stdDevCurrent);
			
			if (stdDevCurrent < stdDevMin)
			{
			
				if (fracZeroComp > GrayscaleImageEdit.ZERO_REL_MIN)
				{
					stdDevMin = stdDevCurrent;
					stdDevMinVal = currVal;
				} else
				{
					//System.err.println("  (This entry is ignored due to truncation)");
					
					//we have an extreme bias of one pixel type, which is usually synonymous to a complete truncation,
					//which we hope to avoid
				}
				
			}
			
			pb.setValue( (int)Math.round( (double)i*100/numSteps) );
			if (frame.getCancelled())
				return -1;
				
			currVal += deltaThresh;
		}
		pb.setValue(100);
		
		return stdDevMinVal;
	}
	
	/**
	* Detects an approximate threshold of an object that minimizes the variation (standard deviation)
	* of the thresholded image. The function starts by setting the threshold at 128
	* (halfway between 0 and 255) and then searches in the most promising direction using
	* a greedy search algorithm type.
	*
	* @param	imgTemplate	Grayscale image to be used as a template (as int[][] matrix)
	* @param	img	Grayscale image that can be modified (as int[][] matrix)
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	* @param	startValue	The initial threshold
	* @param	deltaThresh	The step length used for the threshold
	* @param	frame	A (extended) frame object, which can be used to keep track of whether the run was terminated beforehand
	* @param	pb	A progressbar object, which is used to set the progress of the algorithm
	* @return	The best threshold value according to the search function
	*/
	public static int detectThresholdGreedy(int[][] imgTemplate, int[][] img, int h, int w, int startValue, int deltaThresh, JFrameExt frame, JProgressBar pb)
	{
		//if (verbose)
		//	System.err.println("Setting up images for threshold detection...");
		pb.setValue(0);
		
		int threshCurr = startValue;
		int threshDirection = deltaThresh;
		int threshMin = threshCurr;
		double stdDevCurrent, stdDevLower, stdDevUpper;
		double stdDevMin = Double.POSITIVE_INFINITY;
		
		thresholdImage(imgTemplate, img, threshCurr);
		stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
		pb.setValue(10);
	
		double fracZero = countFractionZero(img);
		double fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevCurrent = Double.POSITIVE_INFINITY;
			
		if (frame.getCancelled())
			return -1;
		
		
		thresholdImage(imgTemplate, img, threshCurr-deltaThresh);
		stdDevLower = MiscMath.mean( GrayscaleImageEdit.calcStds(img, h, w) );
		pb.setValue(20);
	
		double fracZeroLower = countFractionZero(img);
		double fracZeroLowerComp = (fracZeroLower < 0.5) ? fracZeroLower : (1-fracZeroLower); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroLowerComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevLower = Double.POSITIVE_INFINITY;
		
		if (frame.getCancelled())
			return -1;
		
		
		thresholdImage(imgTemplate, img, threshCurr+deltaThresh);
		stdDevUpper = MiscMath.mean( GrayscaleImageEdit.calcStds(img, h, w) );
		pb.setValue(30);
		
		double fracZeroUpper = countFractionZero(img);
		double fracZeroUpperComp = (fracZeroUpper < 0.5) ? fracZeroUpper : (1-fracZeroUpper); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroUpperComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevUpper = Double.POSITIVE_INFINITY;
		
		if (frame.getCancelled())
			return -1;
		
		
		//if (verbose)
		{
			System.err.println("StdDev at current position (" + threshCurr + ") is " + stdDevCurrent + ", fracZero is " + fracZeroComp);
			System.err.println("StdDev at lower position (" + (threshCurr-deltaThresh) + ") is " + stdDevLower + ", fracZero is " + fracZeroLowerComp);
			System.err.println("StdDev at upper position (" + (threshCurr+deltaThresh) + ") is " + stdDevUpper + ", fracZero is " + fracZeroUpperComp);
		}
		
		
		boolean keepSearching = true;
		boolean lower = false;
		
		if ( stdDevLower < stdDevCurrent || stdDevUpper < stdDevCurrent || stdDevCurrent < GrayscaleImageEdit.STDDEV_MIN)
		{

			if ( stdDevLower < stdDevCurrent && stdDevUpper < stdDevCurrent)
				lower = (stdDevLower < stdDevUpper);
			else if ( stdDevLower < stdDevCurrent )
				lower = true;
			
			if (lower)
			{
				threshDirection = -deltaThresh;
				stdDevMin=stdDevLower;
				
				//if (verbose)
				//	System.err.println("Threshold will be iteratively decreased...");
					
			} else
			{
				stdDevMin=stdDevUpper;
				
				//if (verbose)
				//	System.err.println("Threshold will be iteratively increased...");
			}
			threshCurr += threshDirection; //move to the next (best) place, greedy style
			threshMin = threshCurr;
			
		} else
		{
			keepSearching = false;
			//if (verbose)
			//	System.err.println("Stopped at initial position with threshold " + stdDevCurrent);
		}
		
		
		while(keepSearching)
		{
			//if (verbose)
			//	System.err.print("Threshold is now " + threshCurr);
			
			threshCurr += threshDirection;
			
			if (threshCurr <= 0 || threshCurr > 255)
				keepSearching = false;
			else
			{
			
				//if (verbose)
				//	System.err.println(", searching with threshold " + threshCurr);
				
				thresholdImage(imgTemplate, img, threshCurr);
				stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
				
				fracZero = countFractionZero(img);
				fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
				if ( Math.abs(fracZeroComp) < EPS || Math.abs(stdDevCurrent) < EPS)
					stdDevCurrent = Double.POSITIVE_INFINITY;
				
				if (frame.getCancelled())
					return -1;
				
				// we can't know when we reach the goal so just show the user that something is happening
				pb.setValue( (int)(pb.getValue() + (100.0 - pb.getValue())/2.0) );
				
				//if (verbose)
				//	System.err.println("StdDev at current position is " + stdDevCurrent);
				
				if (stdDevCurrent < stdDevMin)
				{
					if (fracZeroComp < GrayscaleImageEdit.ZERO_REL_MIN)
					{
						//we may have a too low stddev, suggesting the entire image is only one color
						threshDirection = (lower) ? deltaThresh : -deltaThresh;
						threshCurr += threshDirection;
						//System.err.println("Changing direction, image may be truncated...");
					} else
					{
						stdDevMin = stdDevCurrent;
						threshMin = threshCurr;
					}
				} else
				{
					keepSearching = false;
				}
			
			}
			
		}
		pb.setValue(100);
		
		return threshMin;
	}
	
	/**
	* Detects an approximate threshold of an object that maximizes the variation (standard deviation)
	* of the thresholded image. The function starts by setting the threshold at 128
	* (halfway between 0 and 255) and then searches in the most promising direction using
	* a greedy search algorithm type.
	*
	* @param	imgTemplate	Grayscale image to be used as a template (as int[][] matrix)
	* @param	img	Grayscale image that can be modified (as int[][] matrix)
	* @param	h	Sliding window height
	* @param	w	Sliding window width
	* @param	startValue	The initial threshold
	* @param	deltaThresh	The step length used for the threshold
	* @param	frame	A (extended) frame object, which can be used to keep track of whether the run was terminated beforehand
	* @param	pb	A progressbar object, which is used to set the progress of the algorithm
	* @return	The best threshold value according to the search function
	*/
	public static int detectThresholdGreedyMax(int[][] imgTemplate, int[][] img, int h, int w, int startValue, int deltaThresh, JFrameExt frame, JProgressBar pb)
	{
		//if (verbose)
		//	System.err.println("Setting up images for threshold detection...");
		pb.setValue(0);
		
		int threshCurr = startValue;
		int threshDirection = deltaThresh;
		int threshMax = threshCurr;
		double stdDevCurrent, stdDevLower, stdDevUpper;
		double stdDevMax = Double.NEGATIVE_INFINITY;
		
		thresholdImage(imgTemplate, img, threshCurr);
		stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
		pb.setValue(10);
	
		double fracZero = countFractionZero(img);
		double fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevCurrent = Double.POSITIVE_INFINITY;
			
		if (frame.getCancelled())
			return -1;
		
		
		thresholdImage(imgTemplate, img, threshCurr-deltaThresh);
		stdDevLower = MiscMath.mean( GrayscaleImageEdit.calcStds(img, h, w) );
		pb.setValue(20);
	
		fracZero = countFractionZero(img);
		fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevLower = Double.POSITIVE_INFINITY;
		
		if (frame.getCancelled())
			return -1;
		
		
		thresholdImage(imgTemplate, img, threshCurr+deltaThresh);
		stdDevUpper = MiscMath.mean( GrayscaleImageEdit.calcStds(img, h, w) );
		pb.setValue(30);
		
		fracZero = countFractionZero(img);
		fracZeroComp = (fracZero < 0.5) ? fracZero : (1-fracZero); //this value is always < 0.5 and a measure of relative composition zero vs non-zero
		if ( Math.abs(fracZeroComp) < ZERO_REL_MIN || Math.abs(stdDevCurrent) < EPS)
			stdDevUpper = Double.POSITIVE_INFINITY;
		
		if (frame.getCancelled())
			return -1;
		
		
		/*
		if (verbose)
		{
			System.err.println("StdDev at current position is " + stdDevCurrent);
			System.err.println("StdDev at lower position is " + stdDevLower);
			System.err.println("StdDev at upper position is " + stdDevUpper);
		}
		*/
		
		boolean keepSearching = true;
		boolean lower = false;
		
		if ( stdDevLower > stdDevCurrent || stdDevUpper > stdDevCurrent)
		{

			if ( stdDevLower > stdDevCurrent && stdDevUpper > stdDevCurrent)
				lower = (stdDevLower > stdDevUpper);
			else if ( stdDevLower > stdDevCurrent )
				lower = true;
			
			if (lower)
			{
				threshDirection = -deltaThresh;
				stdDevMax=stdDevLower;
				
				//if (verbose)
				//	System.err.println("Threshold will be iteratively decreased...");
					
			} else
			{
				stdDevMax=stdDevUpper;
				
				//if (verbose)
				//	System.err.println("Threshold will be iteratively increased...");
			}
			threshCurr += threshDirection; //move to the next (best) place, greedy style
			threshMax = threshCurr;
			
		} else
		{
			keepSearching = false;
			//if (verbose)
			//	System.err.println("Stopped at initial position with threshold " + stdDevCurrent);
		}
		
		
		while(keepSearching)
		{
			//if (verbose)
			//	System.err.print("Threshold is now " + threshCurr);
			
			threshCurr += threshDirection;
			
			if (threshCurr <= 0 || threshCurr > 255)
				keepSearching = false;
			else
			{
			
				//if (verbose)
				//	System.err.println(", searching with threshold " + threshCurr);
				
				thresholdImage(imgTemplate, img, threshCurr);
				stdDevCurrent = MiscMath.mean( GrayscaleImageEdit.calcStds( img , h, w) );
				if (frame.getCancelled())
					return -1;
				
				// we can't know when we reach the goal so just show the user that something is happening
				pb.setValue( (int)(pb.getValue() + (100.0 - pb.getValue())/2.0) );
				
				//if (verbose)
				//	System.err.println("StdDev at current position is " + stdDevCurrent);
				
				if (stdDevCurrent > stdDevMax)
				{
					
					if (fracZeroComp < GrayscaleImageEdit.ZERO_REL_MIN)
					{
						//we may have a too low stddev, suggesting the entire image is only one color
						threshDirection = (lower) ? deltaThresh : -deltaThresh;
						threshCurr += threshDirection;
						//System.err.println("Changing direction, image may be truncated...");
					} else
					
					{
						stdDevMax = stdDevCurrent;
						threshMax = threshCurr;
					}
				} else
				{
					keepSearching = false;
				}
			
			}
			
		}
		pb.setValue(100);
		
		return threshMax;
	}
	
	
	

	/**
	* Segments a binary image, using either 8- or 4-connectivity
	*
	* @param	img	Binary image (as byte[][] matrix), where elements are
	*			either 0 or not 0.
	* @param	use8	if true, 8-connectivity will be used,
	*		otherwise 4-connectivity will be used
	* @return	a matrix of the same dimension, with objects characterized by integer numbers
	*		(in arbitrary order)
	*/
	public static int[][] segmentBinaryImage(byte[][] img, boolean use8)
	{
		int imgWidth = img[0].length;
		int imgHeight = img.length;

		//construct segmention object matrix and reset
		int[][] seg = new int[imgHeight][imgWidth];
		
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			 seg[y][x] = 0;


		//now start segmentation
		int objId=1;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			{
				//find starting point
				if (img[y][x] != 0 && seg[y][x] == 0)
				{
					System.err.println("Starting segmentation of object at (" + x + "," + y + ")...");
					segmentBinaryImageInt(img,seg,use8,x,y,objId++);
				}
				
			}
			
		return seg;
	}
	
	/**
	* Inverts an images
	*
	* @param	img	The (integer) image to be inverted
	* @param	maxVal	The maximum value in the image, typically 255 or 1 (for binary images)
	* @return	a matrix of the same dimension, with objects characterized by integer numbers
	*		(in arbitrary order)
	*/
	public static int[][] invertImage(int[][] img, int maxVal)
	{
		int imgWidth = img[0].length;
		int imgHeight = img.length;

		//construct segmention object matrix and reset
		int[][] invImg = new int[imgHeight][imgWidth];
		
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			 invImg[y][x] = maxVal-img[y][x];
			 
		return invImg;
	}
	
	/**
	* Internal function for segmentation using recursion
	* 
	* @param	img	Binary image (as byte[][] matrix), where elements are
	*			either 0 or not 0.
	* @param	imgSeg	Segmentation image (as int[][] matrix) with object ids
	* @param	useEighConnect	if true, 8-connectivity will be used,
	*		otherwise 4-connectivity will be used
	* @param	x	current x-coordinate
	* @param	y	current y-coordinate
	* @param	objId	internal id of segmentation object
	*/
	private static void segmentBinaryImageInt(byte[][] img, int[][] seg, boolean use8, int x, int y, int objId)
	{
		if ( x >= 0 && x < img[0].length && y >= 0 && y < img.length)
		{
			if (img[y][x] != 0 && seg[y][x] == 0)
			{
				//System.err.println("Adding object id " + objId + " to pixel at (" + x + "," + y + ")");
				seg[y][x]=objId;
				
				segmentBinaryImageInt(img,seg,use8,x-1,y,objId);
				segmentBinaryImageInt(img,seg,use8,x,y-1,objId);
				segmentBinaryImageInt(img,seg,use8,x+1,y,objId);
				segmentBinaryImageInt(img,seg,use8,x,y+1,objId);
				
				if (use8)
				{
					segmentBinaryImageInt(img,seg,use8,x-1,y-1,objId);
					segmentBinaryImageInt(img,seg,use8,x-1,y+1,objId);
					segmentBinaryImageInt(img,seg,use8,x+1,y-1,objId);
					segmentBinaryImageInt(img,seg,use8,x+1,y+1,objId);
				}
			}
		}
	}
	
	/**
	* Generates an object containing the coordinates of each object, which can
	* later be used to calculate various statistics.
	* The returned object is a Vector of Vectors containing Point objects of (x,y)
	* coordinates.
	* 
	* @param	seg	Segmentation matrix
	* @return	A Vector of Vector of Points of coordinates
	*/
	public static Vector fetchSegObjCoord(int[][] seg)
	{
		Vector v = new Vector();
		Vector currVec;
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;
		
		int objId=0;
		int maxObjId=0;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				if (seg[y][x] > 0)
				{
					objId = seg[y][x];
					if (objId > maxObjId)
					{
						//construct empty vector objects
						for (int i = maxObjId; i < objId; i++)
						{
							//System.err.println("Adding segmentation object vector " + objId);
							v.add( new Vector() );
						}
						maxObjId = objId;
					}
					currVec = (Vector)v.get(objId-1);
					currVec.add( new Point(x,y) );
				}
	
		return v;
	}
	
	/**
	* Removes border pixels from a segmentation images
	* 
	* @param	seg	Segmentation matrix
	* @return	A Vector of Vector of Points of coordinates
	*/
	public static int[][] removeBorderPixels(Vector vecBorder, int[][] seg)
	{
		int[][] segNew = new int[seg.length][seg[0].length];
		
		for (int y = 0; y < segNew.length; y++)
			for (int x = 0; x < segNew[0].length; x++)
			{
				segNew[y][x] = seg[y][x];
			}
		
		int x,y;
		Point p;
		for (int i = 0; i < vecBorder.size(); i++)
		{
			Vector currVec = (Vector)vecBorder.get(i);
			for (int j = 0; j < currVec.size(); j++)
			{
				p = (Point)currVec.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				segNew[y][x] = 0; //reset
			}
		}
		
		return segNew;
	}
	
	/**
	* Calculates distance matrices between pixels for all objects (typically to be used for border pixels).
	* Distance measure could be either 'Euclidean' or 'Manhattan'.
	* 
	* @param	vec	Vector of Vector of Points
	* @param	distMeasure	Type of distance measure
	* @return	A Vector of double[][] containing distance measures.
	*/
	public static Vector calcDistanceMatrix(Vector vec, byte distMeasure)
	{
		Vector v = new Vector(vec.size());		
		
		for (int i = 0; i < vec.size(); i++)
		{
			Vector vecCurr = (Vector)vec.get(i);
			double dist1 = 0.0;
			//int dist2 = 0;
			Point p1 = null,p2 = null;
			double[][] distMat1 = new double[vecCurr.size()][vecCurr.size()];
			//int[][] distMat2 = null;
			//if (distMeasure == GrayscaleImageEdit.DIST_MANHATTAN)
			//	distMat2 = new int[vecCurr.size()][vecCurr.size()];
			
			//distance measures are symmetric so we only have to process
			// (n^2)/n+n

			for (int j = 0; j < (vecCurr.size()-1); j++)
			{
				for (int k = (j+1); k < vecCurr.size(); k++)
				{
					p1 = (Point)vecCurr.get(j);
					p2 = (Point)vecCurr.get(k);
					dist1 = GrayscaleImageEdit.calcDistance(p1, p2, distMeasure);
					
					distMat1[j][k] = dist1;
					distMat1[k][j] = dist1;
					
					//dist2 not used here
				}
			}
			
			v.add(distMat1);
			//v[0].add(distMat2);
		}
		return v;
	}
	
	/**
	* Calculates distance matrices between two points.
	* Distance measure could be either 'Euclidean' or 'Manhattan'.
	* 
	* @param	p1	Point 1
	* @param	p2	Point 2
	* @param	distMeasure	The type of distance measure, either 'Euclidean' or 'Manhattan'
	* @return	a double value denoting the distance
	*/
	public static double calcDistance(Point p1, Point p2, byte distMeasure)
	{
		if (distMeasure == GrayscaleImageEdit.DIST_EUCLIDEAN)
		{
			return p1.distance(p2);
		} else if (distMeasure == GrayscaleImageEdit.DIST_MANHATTAN)
		{
			double a = Math.abs( p1.getX() - p2.getX() );
			double b = Math.abs( p1.getY() - p2.getY() );
			return (a + b);
		} else
		{
			return -1;
		}
	}
	
	
	/**
	* Calculates all possible pairs of pixels (j, k) that can be connected without crossing
	* pixels from the current or from other objects.
	* 
	* @param	vec	Vector of Vector of Pixels
	* @param	seg	Segmentation result, should be 0 for non-object or != for object
	* @return	A Vector of boolean[][] containing connectable Points
	*/
	public static Vector findConnectablePixels(Vector vec, int[][] seg, JFrameExt frame, JProgressBar pb)
	{
		Vector retVec = new Vector(vec.size());
		
		double pbStepLength = 100.0/vec.size();
		
		for (int i = 0;  i < vec.size(); i++)
		{
			Vector currVec = (Vector)vec.get(i);
			boolean[][] connectMat = new boolean[currVec.size()][currVec.size()];
			boolean connectable = false;
			Point p1, p2;
			int totConnect = 0, totPixels = 0;
			
			for (int j = 0; j < (currVec.size()-1); j++)
			{
				pb.setValue( (int)Math.round( ((j+1)*100.0/(double)currVec.size())/vec.size() + i*pbStepLength ));
				
				
				
				for (int k = (j+1); k < currVec.size(); k++)
				{
					if (frame.getCancelled())
						return null;
					
					p1 = (Point)currVec.get(j);
					p2 = (Point)currVec.get(k);
					connectable = pixelsConnectable(seg, p1, p2);
					connectMat[j][k] = connectable;
					connectMat[k][j] = connectable;
					
					totPixels++;
					if (connectable)
					{
						totConnect++;
						//if (g2d != null)
						//	drawPixelConnection(p1, p2, g2d, Color.GREEN);
					}	
					
				}
			}
			
			System.err.println("Object " + (i+1) + " has " + totConnect + "/" + totPixels + " connectable pixels..."); 
			retVec.add(connectMat);
		}
		return retVec;
	}
	
	/**
	* Calculates all possible pairs of pixels (j, k) that can be connected without crossing
	* pixels from the current or from other objects.
	* 
	* @param	vec	Vector of Vector of Pixels
	* @param	seg	Segmentation result, should be 0 for non-object or != 0 for object
	* @param	startIndex	The starting index of the border pixels to run over
	* @param	stopIndex	The stop index of the border pixels to run over
	* @return	A Vector of boolean[][] containing connectable Points
	*/
	public static boolean[][] findConnectablePixelsInd(Vector vec, int[][] seg, int startIndex, int stopIndex)
	{
		Vector currVec = vec;
		int range = stopIndex - startIndex;
		boolean[][] connectMat = new boolean[range][range];
		boolean connectable = false;
		Point p1, p2;
		int totConnect = 0, totPixels = 0;
		
		for (int j = 0; j < (range-1); j++)
		{
			//pb.setValue( (int)((j+1)*100.0/(double)currVec.size()) );
			
			for (int k = (j+1); k < range; k++)
			{
				p1 = (Point)currVec.get(j+startIndex);
				p2 = (Point)currVec.get(k+startIndex);
				connectable = pixelsConnectable(seg, p1, p2);
				connectMat[j][k] = connectable;
				connectMat[k][j] = connectable;
				
				totPixels++;
				if (connectable)
				{
					totConnect++;
					//if (g2d != null)
					//	drawPixelConnection(p1, p2, g2d, Color.GREEN);
				}	
				
			}
		}
		
		//System.err.println("Object has " + totConnect + "/" + totPixels + " connectable (cavity) pixels..."); 
		return connectMat;	
	}
	
	/**
	* Calculates all possible pairs of pixels (j, k) that can be connected without crossing
	* pixels from the current or from other objects. Returns the result
	* as an Vector of Vector of Points with connection indices.
	* 
	* @param	vec	Vector of Vector of Pixels
	* @param	seg	Segmentation result, should be 0 for non-object or != for object
	* @return	A Vector of Vector of Points with connection indices.
	*/
	/*
	public static Vector findConnectablePixelsAsVector(Vector vec, int[][] seg)
	{
		Vector retVec = new Vector(vec.size());
		for (int i = 0;  i < vec.size(); i++)
		{
			Vector currVec = (Vector)vec.get(i);
			Vector currRetVec = new Vector( currVec.size() );
			
			boolean connectable = false;
			Point p1, p2;
			int totConnect = 0, totPixels = 0;
			
			for (int j = 0; j < (currVec.size()-1); j++)
			{
				for (int k = (j+1); k < currVec.size(); k++)
				{
					p1 = (Point)currVec.get(j);
					p2 = (Point)currVec.get(k);
					connectable = pixelsConnectable(seg, p1, p2);
					
					totPixels++;
					if (connectable)
					{
						currRetVec.add( new Point(j, k) );
						totConnect++;
					}
				}
			}
			
			System.err.println("Object " + (i+1) + " has " + totConnect + "/" + totPixels + " connectable pixels..."); 
			retVec.add(currRetVec);
		}
		return retVec;
	}
	*/
	
	/**
	* Calculates all possible pairs of pixels (j, k) that can be connected without crossing
	* pixels from the current or from other objects. Returns the result
	* as an Vector of Vector of Points with connection indices.
	* 
	* @param	vec	Vector of Vector of Pixels
	* @param	seg	Segmentation result, should be 0 for non-object or != for object
	* @return	A Vector of Vector of Points with connection indices.
	*/
	public static Vector findConnectablePixelsAsVector(Vector vec, int[][] seg)
	{
		Vector retVec = new Vector(vec.size());
		for (int i = 0;  i < vec.size(); i++)
		{
			Vector currVec = (Vector)vec.get(i);
			Vector currRetVec = new Vector( currVec.size() );
			
			boolean connectable = false;
			Point p1, p2;
			int totConnect = 0, totPixels = 0;
			
			for (int j = 0; j < (currVec.size()-1); j++)
			{
				for (int k = (j+1); k < currVec.size(); k++)
				{
					p1 = (Point)currVec.get(j);
					p2 = (Point)currVec.get(k);
					connectable = pixelsConnectable(seg, p1, p2);
					
					totPixels++;
					if (connectable)
					{
						currRetVec.add( new Point(j, k) );
						totConnect++;
					}
				}
			}
			
			System.err.println("Object " + (i+1) + " has " + totConnect + "/" + totPixels + " connectable pixels..."); 
			retVec.add(currRetVec);
		}
		return retVec;
	}
	
	
	/**
	* Calculates the minimal contour (distance between connecting point) that can be achieved.
	* 
	* @param	vecPoints	Vector of Vector of Points
	* @param	connectMat	Vector of boolean[][] with information of two Points are connectable
	* @param	distMat	Vector of double[][] containing distances between Points
	* @param	startIndex	The starting index
	* @param	stopIndex	The stopping index
	* @param	g2d	Graphics object to draw lines between connecting Points
	* @return	A Vector of Vector of Points with connecting Points.
	*/
	public static Vector findMinimalContour(Vector vecPoints, Vector connectMat, Vector distMat, int startIndex, int stopIndex, Graphics g2d)
	{
		Vector retVec = new Vector(vecPoints.size());
		
		if (g2d != null)
		{
			g2d.setColor( Color.PINK );
		}
		
		for (int i = 0;  i < vecPoints.size(); i++)
		{
			Vector currVec = (Vector)vecPoints.get(i);
			
			
			boolean[][] currConnectMat = (boolean[][])connectMat.get(i);
			double[][] currDistMat = (double[][])distMat.get(i);
			
			//copy connection matrix so that we can remove used-up points
			boolean[][] currConnectMatCopy = new boolean[currConnectMat[0].length][currConnectMat.length];
			for (int x = 0; x < currConnectMat[0].length; x++)
				for (int y = 0; y < currConnectMat.length; y++)
					currConnectMatCopy[y][x] = currConnectMat[y][x];
			
			int maxAllowedIndexDiff = (int)(currVec.size()/2.0); //to ensure that the pixels are moving counter-clockwise
			
			int currIndex = startIndex;
			Point currPoint = (Point)currVec.get(currIndex);
			
			int stopIndexCurr = (stopIndex < 0) ? (currVec.size()-1) : stopIndex;
			
			System.err.println("Contour finding for object " + (i+1) + " for indices " + startIndex + ":" + stopIndexCurr + "...");
			
			
			while (currIndex < stopIndexCurr)
			{
			
			
				//searches for connections from the current Point to any adjacent Points
				//should be adapted so that the index can 'switch over' to 0 and continute
				//double maxDistance = 2*SQRT2; //neighbors have a Euclidean distance of 1 or sqrt(2), not interested in those
				//double minDistance = 2*SQRT2
				double maxDistance = 0;
				double minDistance = 0;
				int maxDistanceIndex = -1;
				double distComp = -1;
				//int numSteps = stopIndexCurr-1;
				//int relativeIndex = 0;
				
				for (int j = (currIndex+1); j <= Math.min(stopIndexCurr, currIndex + maxAllowedIndexDiff); j++)
				{
					
					if (currConnectMatCopy[currIndex][j])
					{
						distComp = currDistMat[currIndex][j];
						if (distComp > maxDistance)
						//if (j > maxDistanceIndex && currDistMat[currIndex][j] > minDistance )
						{
							maxDistance = distComp;
							maxDistanceIndex = j;
						}
					}
				}
				
				if (maxDistanceIndex < 0 )
				{
					//System.err.println("  Current point " + currIndex + " has no neighbors, moving to next point...");
					currIndex++;
				} else
				{
					System.err.println("  Current point " + currIndex + " is connected to point " + maxDistanceIndex +
						" with distance " + maxDistance);
						
					currConnectMatCopy[currIndex][maxDistanceIndex] = false;
					//currConnectMatCopy[maxDistanceIndex][currIndex] = false;
					
					currIndex = maxDistanceIndex;
					
				}
				
				if (g2d != null)
				{
					Color c = (maxDistanceIndex < 0) ? Color.WHITE : Color.BLUE;
					g2d.setColor(c);
					currPoint = (Point)currVec.get(currIndex);
					
					g2d.drawLine( (int)currPoint.getX(), (int)currPoint.getY(),
						(int)currPoint.getX(), (int)currPoint.getY() );
				}
				
			} 
		}
		return retVec;
	}
	
	
	/**
	* Traces a contour across the border of an object. When enough non-connectable pixels
	* have been found, the previous region is called an 'indent' and the current pixel a
	* 'contour hotspot' which is used to connect the entire contour of the border.
	* 
	* @param	vecBorders	Vector of Vector of Pixels of border elements
	* @param	seg	Segmentation result, should be 0 for non-object or != for object
	* @param	numNonConnPixelsThresh	The allowed number of (consecutive) non-connectable pixels
	*		allowed before a new hotspot is assigned.
	* @return	A Vector[] containing points (of hotspots) and the corresponding indices
	*/
	public static Vector[] findContourHotspotsNarrow(Vector vecBorders, int[][] seg, int numNonConnPixelsThresh)
	{
		Vector retVecPoints = new Vector(vecBorders.size());
		Vector retVecIndices = new Vector(vecBorders.size());
		
		//double pbStepLength = 100.0/vec.size();
		
		for (int i = 0;  i < vecBorders.size(); i++)
		{
			//System.err.println("Hotspot detection: Object #" + (i+1));
			
			Vector currVec = (Vector)vecBorders.get(i);
			Vector currRetVecPoints = new Vector( vecBorders.size() );
			Vector currRetVecIndices = new Vector( vecBorders.size() );
			
			Point pRef, pCurr, pPrev;
			pRef = (Point)currVec.get(0);
			
			
			int refInd = 0, lastGoodInd = 1, lastLastGoodInd = -1, currInd = 1, lastAddedInd = -1;
			int numNonConnectable = 0;
			currRetVecPoints.add( new Point(pRef) );
			currRetVecIndices.add( new Integer(refInd) );
			
			int stopInd = currVec.size();
			
			while (currInd < stopInd )
			{
				//pb.setValue( (int)Math.round( ((j+1)*100.0/(double)currVec.size())/vec.size() + i*pbStepLength ));
				pPrev = (Point)currVec.get(currInd-1);
				pCurr = (Point)currVec.get(currInd);
				
				
				if (pCurr.distance(pPrev) > SQRT2)
				{
					//apparently the border breaks here, we have to deal with this somehow
					
					
					//stopInd = currInd-1;
					//System.err.println("----> Distance between " + (currInd-1) + " and " + (currInd) + " is " + pCurr.distance(pPrev) );
					System.err.println("----> Contour trace for object #" + (i+1)  + ": gap at " + currInd);
					
					
					//first add the previous hit (if not already added)
					if (lastGoodInd != (currInd-1) )
					{
						currRetVecPoints.add( new Point( (Point)currVec.get(currInd-1) ) );
						currRetVecIndices.add( new Integer(currInd-1) );
					}
					
					//now add a break sign (null)
					currRetVecPoints.add( null );
					currRetVecIndices.add( null );
					
					//now add the next hit
					currInd++;
					if (currInd < stopInd)
					{
						pCurr = new Point( (Point)currVec.get(currInd) );
						lastLastGoodInd = lastGoodInd;
						lastGoodInd = currInd;
						currRetVecPoints.add( new Point( pCurr) );
						currRetVecIndices.add( new Integer(currInd) );
					}
						
					
							
					
				} else
				{
				
					boolean connectable = pixelsConnectable(seg, pRef, pCurr);
					
					if (connectable)
					{
						lastGoodInd = currInd;
						lastLastGoodInd = lastGoodInd;
						numNonConnectable = 0;
						
						//System.err.println("  lastGoodInd: " + lastGoodInd + "/" + currVec.size() );
					} else
					{
						numNonConnectable++;
						
						if (numNonConnectable > numNonConnPixelsThresh)
						{
							lastAddedInd = lastGoodInd;
							currRetVecPoints.add( new Point( (Point)currVec.get(lastGoodInd) ) );
							currRetVecIndices.add( new Integer(lastGoodInd) );
							
							//System.err.println("Connecting (" + refInd + "," + lastGoodInd + ")" );
								
							//this happens if there are no connectable pixels at all for this refPoint
							if (lastGoodInd == lastLastGoodInd)
								lastGoodInd++;
							
							
							currInd = lastGoodInd;
							refInd = lastGoodInd;
							
							
							numNonConnectable = 0;
							
							pRef = new Point( (Point)currVec.get(lastGoodInd) );
							
						}
					}
					
				}
				pPrev = new Point(pCurr);
				currInd++;
			}
			
			System.err.println("Contour: Last good index is " + lastGoodInd + "/" + stopInd);
			
			//add the last connection
			if (lastGoodInd != lastAddedInd)
			{
				System.err.println("Contour: Adding " + lastGoodInd);
				
				currRetVecPoints.add( new Point( (Point)currVec.get(lastGoodInd) ) );
				currRetVecIndices.add( new Integer(lastGoodInd) );
			}
			
			//also add the second-to-last point (should really be '0' to wrap around, but more difficult to implement for other functions)
			if (lastGoodInd < (stopInd-1))
			{
				System.err.println("Contour: Adding " + (stopInd-1));
				currRetVecPoints.add( new Point( (Point)currVec.get(stopInd-1) ) );
				currRetVecIndices.add( new Integer(stopInd-1) );
			}
						
			retVecPoints.add(currRetVecPoints);
			retVecIndices.add(currRetVecIndices);
			
		}
		
		Vector retVec[] = new Vector[2];
		retVec[0] = retVecPoints;
		retVec[1] = retVecIndices;
		
		return retVec;
	}
	
	

	/**
	* Generates a contour from a segmentation object, based on how the border pixels
	* are (linearly) connected to one another. The contour is a int[][] matrix which expands
	* the segmented image with contour points.
	* 
	* @param	seg	The segmentation result: the original image with integers denoting object identity
	* @param	vecBorders	Vector of Vector of Points of border pixels
	* @param	connectMat	Vector of boolean[][] with information of two Points are connectable
	* @return	A Vector of Vector of Points with connecting Points.
	*/
	public static int[][] findContour(int[][] seg, Vector vecBorders, Vector connectMat)
	{
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;

		//copy segmentation object to make sure we don't edit the original one
		int[][] retMat = new int[imgHeight][imgWidth];
		
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			 retMat[y][x] = seg[y][x];

		
		for (int i = 0;  i < vecBorders.size(); i++)
		{
			Vector currVec = (Vector)vecBorders.get(i);
			
			
			boolean[][] currConnectMat = (boolean[][])connectMat.get(i);
			Point p1, p2;
			
			for (int j = 0; j < (currVec.size()-1); j++)
				for (int k = (j+1); k < currVec.size(); k++)
				{
					if (currConnectMat[j][k]) //pre-calculated
					{
						//draw the line between them here
						p1 = (Point)currVec.get(j);
						p2 = (Point)currVec.get(k);
						paintPixelConnection(retMat, p1, p2, i+1, true);
					}
				}
			
		}
		return retMat;
	}
	
	
	/**
	* Traces a contour surface around an object.
	* 
	* @param	seg	The segmentation result: the original image with integers denoting object identity
	* @param	vecBorders	Vector of Vector of Points of border pixels
	* @param	contourHotspotConnections	Vector of Vector of Points with hot spot connection
	* @param	contourHotspotIndices	Vector of Vector of Points with the corresponding hot spot indices
	* @return	A Vector of Vector of Points with connecting Points.
	*/
	public static int[][] traceContour(int[][] seg, Vector vecBorders, Vector contourHotspotConnections, Vector contourHotspotIndices)
	{
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;

		//copy segmentation object to make sure we don't edit the original one
		int[][] retMat = new int[imgHeight][imgWidth];
		
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			 retMat[y][x] = seg[y][x];

		
		for (int i = 0;  i < contourHotspotConnections.size(); i++)
		{
			Vector currCHSC = (Vector)contourHotspotConnections.get(i);
			Vector currCHSI = (Vector)contourHotspotIndices.get(i);
			Vector currBorders = (Vector)vecBorders.get(i);
			
			int lastInd = ( (Integer)currCHSI.get(0)).intValue();
			int currInd = lastInd;
			Point p1, p2;
			Point pRef = (Point)currCHSC.get(0);
			Point pCurr;
			
			for (int j = 1; j < currCHSI.size(); j++)
			{
				pCurr = (Point)currCHSC.get(j);
				if (pCurr != null)
					currInd = ( (Integer)currCHSI.get(j)).intValue();
				
				if (pRef != null && pCurr != null)
				{
				
					if ( currInd > (lastInd+1) )
					{
						//System.err.println("* Running contour trace of object #" + (i+1) + " for indices #" + (lastInd) + " to " + currInd + "...");
						
						for (int k = lastInd; k < currInd; k++)
						{
							for (int m = (lastInd+1); m < (currInd-1); m++)
							{
								//draw the line between them here
								p1 = (Point)currBorders.get(k);
								p2 = (Point)currBorders.get(m);
								paintPixelConnection(retMat, p1, p2, i+1, true);
							}
						}
					}
					
					
				} else
				{
					System.err.println("*** Skipping contour trace for object #" + (i+1) + " at index " + j);
					
				}
				
				pRef = (pCurr == null) ? null : new Point(pCurr);
				if (pCurr != null)
					lastInd = currInd;
			}
		}
		return retMat;
	}
	

	
	/**
	* Generates a contour from a segmentation object, based on how the border pixels
	* are (linearly) connected to one another. The contour is a int[][] matrix which expands
	* the segmented image with contour points. The side-effect is that 'seg' is edited.
	* 
	* @param	seg	The segmentation result: the original image with integers denoting object identity
	* @param	vecBorders	Vector of Vector of Points of border pixels
	* @param	startIndex	The starting index
	* @param	stopIndex	The stopping index
	* @param	objectId	The object id number (to pain on seg)
	*/
	public static void findContourInd(int[][] seg, Vector vecBorders, int startIndex, int stopIndex, int objectId)
	{
		
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;

		int range = stopIndex - startIndex;
		
		/*
		//copy segmentation object to make sure we don't edit the original one
		
		int[][] retMat = new int[imgHeight][imgWidth];
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				retMat[y][x] = seg[y][x];
		*/
		
		Vector currVec = vecBorders;
			
		//fetch the connectable pixels
		boolean[][] currConnectMat = findConnectablePixelsInd(currVec, seg, startIndex, stopIndex);
		Point p1, p2;
		
		
		for (int j = 0; j < (range-1); j++)
			for (int k = (j+1); k < range; k++)
			{
				if (currConnectMat[j][k])
				{
					//draw the line between them here, not allowing overwriting of existing values in retMat
					p1 = (Point)currVec.get(j+startIndex);
					p2 = (Point)currVec.get(k+startIndex);
					
					//paintPixelConnection(retMat, p1, p2, objectId, false);
					paintPixelConnection(seg, p1, p2, objectId, false);
				}
			}
		
		//count the number of pixels that have a value != 0, before and after
		/*
		int absSumPre = 0;
		int absSumPost = 0;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
			{
				absSumPre += (int)Math.abs(seg[y][x]);
				absSumPost += (int)Math.abs(retMat[y][x]);
			}

		System.err.println("Number of painted pixels: before=" + absSumPre + ", after=" + absSumPost);
		*/
		
		//return retMat;
	}
	
	
	
	/**
	* Calculates if the pair (j,k) of pixels can be connected without crossing
	* a pixel that belongs to the object, or other objects (i.e. if a line can be drawn between j and k
	* without interfering with other pixels).
	* 
	* @param	seg	Segmentation result, should be 0 for non-object pixels and != 0 for object pixel
	* @param	p1	First point (start/stop point)
	* @param	p2	Second point (start/stop point)
	* @return	A Vector of boolean[][] with 'true' element where the pixels are connected and 'false' elsewhere.
	*/
	public static boolean pixelsConnectable(int[][] seg, Point p1, Point p2)
	{
		return pixelsConnectable(seg,p1,p2,true);
	}
	
	/**
	* Calculates if the pair (j,k) of pixels can be connected without crossing
	* a pixel that belongs to the object, or other objects (i.e. if a line can be drawn between j and k
	* without interfering with other pixels).
	* 
	* @param	seg	Segmentation result, should be 0 for non-object pixels and != 0 for object pixel
	* @param	p1	First point (start/stop point)
	* @param	p2	Second point (start/stop point)
	* @param	isZero	If true, the algorithm checks explicitly for a 'zero slope', otherwise not
	* @return	A Vector of boolean[][] with 'true' element where the pixels are connected and 'false' elsewhere.
	*/
	public static boolean pixelsConnectable(int[][] seg, Point p1, Point p2, boolean isZero)
	{
		boolean ret = true;
		int startX, stopX, startY, stopY, deltaX, deltaY;
		
		if (p1.getX() < p2.getX() )
		{
			startX = (int)p1.getX();
			stopX = (int)p2.getX();
			startY = (int)p1.getY();
			stopY = (int)p2.getY();
			
		} else
		{
			startX = (int)p2.getX();
			stopX = (int)p1.getX();
			startY = (int)p2.getY();
			stopY = (int)p1.getY();
		}
		deltaX = startX - stopX;
		deltaY = startY - stopY;
		
		boolean slopeMissing = (stopX == startX);
		boolean slopeZero = (stopY == startY);
		double slope = 0.0, intercept = 0.0;
		if (!slopeMissing && !slopeZero)
		{
			slope = (double)(stopY - startY)/(double)(stopX - startX);
			intercept = -slope*stopX + stopY;
			//System.err.println("Rand. line with slope " + slope + " and intercept " + intercept + "...");
		}
		
		if (!slopeMissing || !slopeZero)
		{
			if ( slopeMissing || Math.abs(deltaY) > Math.abs(deltaX) )
			{
				int stopYfinal = (int)Math.max(startY, stopY);
				int xx = startX;
				int yy = (int)(Math.min(startY, stopY)+1);
				while (ret && yy < stopYfinal)
				{
					if (!slopeMissing)
						xx = (int)Math.round((yy - intercept)/slope);
					
					//skip test if we end up in the start/stop pixels, or neighbors thereof, otherwise evaluate the pixel property
					if ( !( ( Math.abs(xx - startX) <= 1 && Math.abs(yy - startY) <= 1) ||
						( Math.abs(xx - stopX) <= 1 && Math.abs(yy - stopY) <= 1) ) )
						ret = ( (seg[yy][xx] == 0 && isZero) || (seg[yy][xx] != 0 && !isZero) );
						
					yy++;
				}
				
			} else
			{
				int yy = startY;
				int xx = (startX+1);
				while (ret && xx < stopX)
				{
					if (!slopeZero)
						yy = (int)Math.round( slope*xx + intercept);
					
					if ( !( ( Math.abs(xx - startX) <= 1 && Math.abs(yy - startY) <= 1) ||
						( Math.abs(xx - stopX) <= 1 && Math.abs(yy - stopY) <= 1) ) )
						ret = ( (seg[yy][xx] == 0 && isZero) || (seg[yy][xx] != 0 && !isZero) );
					
					xx++;
				}
			}
		} else
		{
			//something is fishy, proably startPixel == stopPixel
			ret = false;
		}
		
		return ret;
	}
	
	
	
	/**
	* Calculates if the pair (j,k) of pixels can be connected without crossing
	* a pixel that belongs to the object and returns all the intercepting pixels.
	* 
	* @param	seg	Segmentation result, should be 0 for non-object pixels and != 0 for object pixel
	* @param	p1	First point (start/stop point)
	* @param	p2	Second point (start/stop point)
	* @param	isZero	If true, the algorithm checks explicitly for a 'zero slope', otherwise not
	* @return	A Vector of boolean[][] with 'true' element where the pixels are connected and 'false' elsewhere.
	*/
	public static Vector pixelsConnectableAsVector(int[][] seg, Point p1, Point p2, boolean isZero)
	{
		Vector retVec = new Vector();
		boolean ret = true;
		int startX, stopX, startY, stopY, deltaX, deltaY;
		
		if (p1.getX() < p2.getX() )
		{
			startX = (int)p1.getX();
			stopX = (int)p2.getX();
			startY = (int)p1.getY();
			stopY = (int)p2.getY();
			
		} else
		{
			startX = (int)p2.getX();
			stopX = (int)p1.getX();
			startY = (int)p2.getY();
			stopY = (int)p1.getY();
		}
		deltaX = startX - stopX;
		deltaY = startY - stopY;
		
		boolean slopeMissing = (stopX == startX);
		boolean slopeZero = (stopY == startY);
		double slope = 0.0, intercept = 0.0;
		if (!slopeMissing && !slopeZero)
		{
			slope = (double)(stopY - startY)/(double)(stopX - startX);
			intercept = -slope*stopX + stopY;
			//System.err.println("Rand. line with slope " + slope + " and intercept " + intercept + "...");
		}
		
		if (!slopeMissing || !slopeZero)
		{
			if ( slopeMissing || Math.abs(deltaY) > Math.abs(deltaX) )
			{
				int stopYfinal = (int)Math.max(startY, stopY);
				int xx = startX;
				int yy = (int)(Math.min(startY, stopY)+1);
				while (ret && yy < stopYfinal)
				{
					if (!slopeMissing)
						xx = (int)Math.round((yy - intercept)/slope);
					
					//skip test if we end up in the start/stop pixels, or neighbors thereof, otherwise evaluate the pixel property
					if ( !( ( Math.abs(xx - startX) <= 0 && Math.abs(yy - startY) <= 0) ||
						( Math.abs(xx - stopX) <= 0 && Math.abs(yy - stopY) <= 0) ) )
						ret = ( (seg[yy][xx] == 0 && isZero) || (seg[yy][xx] != 0 && !isZero) );
						
					if (ret)
						retVec.add( new Point(xx, yy) );
					
					yy++;
				}
				
			} else
			{
				int yy = startY;
				int xx = (startX+1);
				while (ret && xx < stopX)
				{
					if (!slopeZero)
						yy = (int)Math.round( slope*xx + intercept);
					
					if ( !( ( Math.abs(xx - startX) <= 0 && Math.abs(yy - startY) <= 0) ||
						( Math.abs(xx - stopX) <= 0 && Math.abs(yy - stopY) <= 0) ) )
						ret = ( (seg[yy][xx] == 0 && isZero) || (seg[yy][xx] != 0 && !isZero) );
						
					if (ret)
						retVec.add( new Point(xx, yy) );
						
					xx++;
				}
			}
		} else
		{
			//something is fishy, proably startPixel == stopPixel
			retVec = null;
		}
		
		return retVec;

	}
	
	/**
	* Finds all Points that are in between a straigh line between two points
	* 
	* @param	p1	First point (start/stop point)
	* @param	p2	Second point (start/stop point)
	* @return	A Vector of Points
	*/
	public static Vector pixelsBetweenPoints(Point p1, Point p2)
	{
		Vector retVec = new Vector();
		boolean ret = true;
		int startX, stopX, startY, stopY, deltaX, deltaY;
		
		if (p1.getX() < p2.getX() )
		{
			startX = (int)p1.getX();
			stopX = (int)p2.getX();
			startY = (int)p1.getY();
			stopY = (int)p2.getY();
			
		} else
		{
			startX = (int)p2.getX();
			stopX = (int)p1.getX();
			startY = (int)p2.getY();
			stopY = (int)p1.getY();
		}
		deltaX = startX - stopX;
		deltaY = startY - stopY;
		
		boolean slopeMissing = (stopX == startX);
		boolean slopeZero = (stopY == startY);
		double slope = 0.0, intercept = 0.0;
		if (!slopeMissing && !slopeZero)
		{
			slope = (double)(stopY - startY)/(double)(stopX - startX);
			intercept = -slope*stopX + stopY;
			//System.err.println("Rand. line with slope " + slope + " and intercept " + intercept + "...");
		}
		
		if (!slopeMissing || !slopeZero)
		{
			if ( slopeMissing || Math.abs(deltaY) > Math.abs(deltaX) )
			{
				int stopYfinal = (int)Math.max(startY, stopY);
				int xx = startX;
				int yy = (int)(Math.min(startY, stopY)+1);
				while (ret && yy < stopYfinal)
				{
					if (!slopeMissing)
						xx = (int)Math.round((yy - intercept)/slope);
					
					//skip test if we end up in the start/stop pixels, or neighbors thereof, otherwise evaluate the pixel property
					if ( !( ( Math.abs(xx - startX) <= 0 && Math.abs(yy - startY) <= 0) ||
						( Math.abs(xx - stopX) <= 0 && Math.abs(yy - stopY) <= 0) ) )
						ret = true;
						
					if (ret)
						retVec.add( new Point(xx, yy) );
					
					yy++;
				}
				
			} else
			{
				int yy = startY;
				int xx = (startX+1);
				while (ret && xx < stopX)
				{
					if (!slopeZero)
						yy = (int)Math.round( slope*xx + intercept);
					
					if ( !( ( Math.abs(xx - startX) <= 0 && Math.abs(yy - startY) <= 0) ||
						( Math.abs(xx - stopX) <= 0 && Math.abs(yy - stopY) <= 0) ) )
						ret = true;
						
					if (ret)
						retVec.add( new Point(xx, yy) );
						
					xx++;
				}
			}
		} else
		{
			//something is fishy, proably startPixel == stopPixel
			retVec = null;
		}
		
		return retVec;

	}
	
	
	
	/**
	* Calculates a line between two pixels and 'paints' the connecting line on a integer matrix
	* 
	* @param	seg	Segmentation matrix: original image with integer values defining objects
	* @param	p1	First point (start/stop point)
	* @param	p2	Second point (start/stop point)
	* @param	val	The value to place on the segmentation (integer) matrix
	* @param	allowOverwrite	If true, any value in seg can be overwritted. If false, only entries with 0 will be replaced.
	*/
	public static void paintPixelConnection(int[][] seg, Point p1, Point p2, int val, boolean allowOverwrite)
	{
		
		int startX, stopX, startY, stopY, deltaX, deltaY;
		
		if (p1.getX() < p2.getX() )
		{
			startX = (int)p1.getX();
			stopX = (int)p2.getX();
			startY = (int)p1.getY();
			stopY = (int)p2.getY();
			
		} else
		{
			startX = (int)p2.getX();
			stopX = (int)p1.getX();
			startY = (int)p2.getY();
			stopY = (int)p1.getY();
		}
		deltaX = startX - stopX;
		deltaY = startY - stopY;
		
		boolean slopeMissing = (stopX == startX);
		boolean slopeZero = (stopY == startY);
		double slope = 0.0, intercept = 0.0;
		if (!slopeMissing && !slopeZero)
		{
			slope = (double)(stopY - startY)/(double)(stopX - startX);
			intercept = -slope*stopX + stopY;
			//System.err.println("Rand. line with slope " + slope + " and intercept " + intercept + "...");
		}
		
		if (!slopeMissing || !slopeZero)
		{
			if ( slopeMissing || Math.abs(deltaY) > Math.abs(deltaX) )
			{
				int stopYfinal = (int)Math.max(startY, stopY);
				int xx = startX;
				int yy = (int)(Math.min(startY, stopY)+1);
				while (yy < stopYfinal)
				{
					if (!slopeMissing)
						xx = (int)Math.round((yy - intercept)/slope);
					
					if ( allowOverwrite || seg[yy][xx] == 0)
						seg[yy][xx] = val;
						
					yy++;
				}
				
			} else
			{
				int yy = startY;
				int xx = (startX+1);
				while (xx < stopX)
				{
					if (!slopeZero)
						yy = (int)Math.round( slope*xx + intercept);
					
					if ( allowOverwrite || seg[yy][xx] == 0)
						seg[yy][xx] = val;
					
					xx++;
				}
			}
		} else
		{
			//something is fishy, proably startPixel == stopPixel
			
		}
	}
	
	/**
	* Fetches the border pixels of each segmentation object
	* The returned object is a Vector of Vectors containing Point objects of (x,y)
	* coordinates of border pixels. Vector[0] contains the border pixels.
	* If sorted, Vector[1] contains break points where the border is broken into pieces
	* (if the object contains cavities)
	* 
	* @param	seg	Segmentation matrix
	* @param	use8	If true, eight-connectivity will be used, otherwise four-connectivity
	* @param	sortNeighbors	If true, the neighboring pixels will be sorted
	*					so that they are adjacent in the Vector
	* @param	frame 	A frame object, used to keep track of whether user cancelled progress
	* @param	pb	A progressbar used to display the progress of the function
	* @return	A Vector[2] of Vector of Points of coordinates
	*/
	public static Vector[] fetchSegObjCoordBorderTest(int[][] seg, boolean use8, boolean sortNeighbors, JFrameExt frame, JProgressBar pb)
	{
		if (pb != null)
			pb.setValue(0);
		
		Vector v[] = new Vector[2];
		v[0] = new Vector(); //border pixels
		v[1] = new Vector(); //border break points, if any
		
		Vector currVec;
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;
		
		//construct a temporary segmentation matrix which is used to store pixels
		//of only borders
		int[][] segTemp = new int[imgHeight][imgWidth];
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				segTemp[y][x] = 0;

		
		int objId=0;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				if (seg[y][x] == 0)
				{
					objId = 0;
					
					//check if its a border pixel
					//boolean borderPixel = ( ( x > 0 && seg[y][x-1] != objId) || ( x < (imgWidth-1) && seg[y][x+1] != objId) ||
					//						( y > 0 && seg[y-1][x] != objId) || ( y < (imgHeight-1) && seg[y+1][x] != objId) );
					if ( x > 0 && seg[y][x-1] != 0)
						objId = seg[y][x-1];
					else if ( x < (imgWidth-1) && seg[y][x+1] != 0)
						objId = seg[y][x+1];
					else if ( y > 0 && seg[y-1][x] != 0)
						objId = seg[y-1][x];
					else if ( y < (imgHeight-1) && seg[y+1][x] != 0)
						objId = seg[y+1][x];
					
					boolean borderPixel = (objId != 0);
					
					//boolean borderPixel = ( ( x > 0 && seg[y][x-1] != 0) || ( x < (imgWidth-1) && seg[y][x+1] != 0) ||
					//						( y > 0 && seg[y-1][x] != 0) || ( y < (imgHeight-1) && seg[y+1][x] != 0) );
					
					//if (use8 && !borderPixel)
					//	borderPixel = ( ( x > 0 && y > 0 &&  seg[y-1][x-1] != objId) ||
					//			( x < (imgWidth-1) && y < (imgHeight-1) &&  seg[y+1][x+1] != objId) ||
					//			( x > 0 && y < (imgHeight-1) &&  seg[y+1][x-1] != objId) ||
					//			( x < (imgWidth-1) && y > 0 &&  seg[y-1][x+1] != objId) );
					
					if (use8 && !borderPixel)
					{
						if ( x > 0 && y > 0 &&  seg[y-1][x-1] != 0)
							objId = seg[y-1][x-1];
						else if ( x < (imgWidth-1) && y < (imgHeight-1) &&  seg[y+1][x+1] != 0) 
							objId = seg[y+1][x+1];
						else if ( x > 0 && y < (imgHeight-1) &&  seg[y+1][x-1] != 0)
							objId = seg[y+1][x-1];
						else if ( x < (imgWidth-1) && y > 0 &&  seg[y-1][x+1] != 0)
							objId = seg[y-1][x+1];
							
						borderPixel = (objId != 0);
						
						
						//borderPixel = ( ( x > 0 && y > 0 &&  seg[y-1][x-1] == 0) ||
						//		( x < (imgWidth-1) && y < (imgHeight-1) &&  seg[y+1][x+1] == 0) ||
						//		( x > 0 && y < (imgHeight-1) &&  seg[y+1][x-1] == 0) ||
						//		( x < (imgWidth-1) && y > 0 &&  seg[y-1][x+1] == 0) );
					}
					
					
					if (borderPixel)
					{
						segTemp[y][x] = objId;
					}
				}
		
		
		//make object pixels that are completely surrounded by borders into borders themselves
		/*
		int numAdded = 0;
		//do
		//{
		//	numAdded = 0;
			for (int y = 0; y < imgHeight; y++)
				for (int x = 0; x < imgWidth; x++)
				{
					//this is an object pixel
					if (seg[y][x] > 0 && segTemp[y][x] == 0)
					{
						boolean hasOnlyBorderNeighbors = ( ( x > 0 && (segTemp[y][x-1] != 0) &&
							( x < (imgWidth-1) && (segTemp[y][x+1] != 0) ) ) &&
							( y > 0 && (segTemp[y-1][x] != 0) )  &&
							( y < (imgHeight-1) && (segTemp[y+1][x] != 0)) );
						
						
						if (use8 && hasOnlyBorderNeighbors)
							hasOnlyBorderNeighbors = ( ( x > 0 && y > 0 &&  segTemp[y-1][x-1] != 0) &&
									( x < (imgWidth-1) && y < (imgHeight-1) &&  segTemp[y+1][x+1] != 0) &&
									( x > 0 && y < (imgHeight-1) &&  segTemp[y+1][x-1] != 0) &&
									( x < (imgWidth-1) && y > 0 &&  segTemp[y-1][x+1] != 0) );
						
						
						if (hasOnlyBorderNeighbors)
						{
							numAdded++;
							segTemp[y][x] = seg[y][x];
						}
					}
				}
			System.err.println("Added " + numAdded + " entries where object pixels only border to border pixels");
		//} while (numAdded > 0);
		
		*/
		
		//remove border pixels that are not neighboring to the object,
		//which is useful for breaking loops
		/*
		int numRemoved = 0;
		int numMoved = 0;
		do
		{
		*/
			/*
			numRemoved = 0;
			
			
			for (int y = 0; y < imgHeight; y++)
				for (int x = 0; x < imgWidth; x++)
				{
					//this is a border pixel
					if (segTemp[y][x] > 0)
					{
						boolean hasObjectNeighbors = ( ( x > 0 && (segTemp[y][x-1] == 0 && seg[y][x-1] != 0) ||
							( x < (imgWidth-1) && (segTemp[y][x+1] == 0 && seg[y][x+1] != 0) ) ) ||
							( y > 0 && (segTemp[y-1][x] == 0 && seg[y-1][x] != 0) )  ||
							( y < (imgHeight-1) && (segTemp[y+1][x] == 0 && seg[y+1][x] != 0)) );
						
						
						if (!hasObjectNeighbors)
						{
							numRemoved++;
							segTemp[y][x] = 0;
							seg[y][x] = 0;
						}
					}
				}
			
			System.err.println("Removed " + numRemoved + " entries where border pixels only neighbor to other border pixels");
			
			*/
			//// TIE-BREAKING
			//// Now we have to break ties in order to avoid loops in the border... we start off with 2 horiz/vert and 1-2 diag (simplest case)
			/*
			
			int sumNeighbors1 = 0;
			int sumNeighbors2 = 0;
			int neighborsThresh = 3;
			numMoved = 0;
			
			// break ties with 2 horiz/vert and 1-2 diag
			
			objId = -1;
			
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] == objId) ? 1 : 0;
						
						//if ( (sumNeighbors1 >= 2 && sumNeighbors2 >= 1) || (sumNeighbors1 >= 1 && sumNeighbors2 >= 2))
						if ( (sumNeighbors1 >= 2 && sumNeighbors2 >= 1))
						{
							numMoved++;
							System.err.println("!!! Moving border with >= 3 neighbors (" + sumNeighbors1 + " horz/vert + " + sumNeighbors2 + " diag) at (x=" + x + ",y=" + y + ")" );
							
							
							//expand this pixel to a nearby, free non-object area
							segTemp[y][x] = 0;
							
							
							if (seg[y-1][x] == 0)
							{
								segTemp[y-1][x] = objId;
								seg[y-1][x] = objId;
							
							} else if (seg[y+1][x] == 0)
							{
								segTemp[y+1][x] = objId;
								seg[y+1][x] = objId;
								
							} else if (seg[y][x-1] == 0)
							{
								segTemp[y][x-1] = objId;
								seg[y][x-1] = objId;
								
							} else if (seg[y][x+1] == 0)
							{
								segTemp[y][x+1] = objId;
								seg[y][x+1] = objId;
								
							} else
							{
								//shouldn't happen...
								System.err.println("No suitable neighbors at (x=" + x + ",y=" + y + ")" );
								segTemp[y][x] = objId;
								numMoved--;
							}
							
						} 
					}
				}
				
			System.err.println("Expanded " + numMoved + " pixels..." );
			
			
		} while (numRemoved > 0 || numMoved > 0);
		*/
		
		/*
		// break ties with 1 horiz/vert + 2 diag
		do
		{
			numMoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] == objId) ? 1 : 0;
						
						if ( (sumNeighbors1 == 1 && sumNeighbors2 >= 2))
						{
							numMoved++;
							System.err.println("!!! Moving border with >= 3 neighbors (" + sumNeighbors1 + " horz/vert + " + sumNeighbors2 + " diag) at (x=" + x + ",y=" + y + ")" );
							
							//move this pixel to a nearby, free (object) area
							segTemp[y][x] = 0;
							seg[y][x] = 0;
							
							if (seg[y-1][x] == objId && segTemp[y-1][x] == 0 )
								segTemp[y-1][x] = objId;
							else if (seg[y+1][x] == objId && segTemp[y+1][x] == 0)
								segTemp[y+1][x] = objId;
							else if (seg[y][x-1] == objId && segTemp[y][x-1] == 0)
								segTemp[y][x-1] = objId;
							else if (seg[y][x+1] == objId && segTemp[y][x+1] == 0)
								segTemp[y][x+1] = objId;
							else
							{
								//shouldn't happen, but who knows...
								System.err.println("No suitable neighbors at (x=" + x + ",y=" + y + ")" );
								
								segTemp[y][x] = objId;
								seg[y][x] = objId;
								numMoved--;
							}
						} 
					}
				}
				
			System.err.println("Moved " + numMoved + " pixels..." );
			
		} while (numMoved > 0);
		
		
		
		//break pure diagonal ties (this is done by splitting the pixel into two pieces
		do
		{
			numMoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] > 0) ? 1 : 0;
						
						if ( sumNeighbors1 == 0 && sumNeighbors2 >= 3)
						{
							System.err.println("!!! Found border with 3 neighbors (0 horz/vert + >= 3 diag) at (x=" + x + ",y=" + y + ")" );
							
							numMoved++;
							
							//expand this pixel to all nearby, free (object) areas
							segTemp[y][x] = 0;
							seg[y][x] = 0;
							
							//one vertical...
							if (seg[y-1][x] == objId && segTemp[y-1][x] == 0 )
								segTemp[y-1][x] = objId;
							else if (seg[y+1][x] == objId && segTemp[y+1][x] == 0)
								segTemp[y+1][x] = objId;
							
							// ... and one horizontal
							if (seg[y][x-1] == objId && segTemp[y][x-1] == 0)
								segTemp[y][x-1] = objId;
							else if (seg[y][x+1] == objId && segTemp[y][x+1] == 0)
								segTemp[y][x+1] = objId;
							
						}
					}
				}
			
		} while (numMoved > 0);
		
		*/
		
		
		
		
		
		/*
		// filter squared border objects
		for (int y = 3; y < imgHeight-3; y++)
			for (int x = 3; x < imgWidth-3; x++)
			{
				//squares should be removed, exactly how depends on the surroundings
				boolean square = (segTemp[y-1][x-1] > 0 && segTemp[y-1][x] > 0 && segTemp[y][x-1] > 0 && segTemp[y][x] > 0);
				
				if (square)
				{
					boolean emptyAbove = (segTemp[y-2][x-2] == 0 && segTemp[y-2][x-1] == 0 && segTemp[y-2][x] == 0 && segTemp[y-2][x+1] == 0);
					boolean emptyBelow =  (segTemp[y+1][x-2] == 0 && segTemp[y+1][x-1] == 0 && segTemp[y+1][x] == 0 && segTemp[y+1][x+1] == 0);
					boolean emptyLeft = (segTemp[y-2][x-2] == 0 && segTemp[y-1][x-2] == 0 && segTemp[y][x-2] == 0 && segTemp[y+1][x-2] == 0);
					boolean emptyRight = (segTemp[y-2][x+1] == 0 && segTemp[y-1][x+1] == 0 && segTemp[y][x+1] == 0 && segTemp[y+1][x+1] == 0);
					
					if (emptyAbove)
					{
						// remove the top 2 pixels
						segTemp[y-1][x-1] = 0;
						segTemp[y-1][x] = 0;
						
					} else if (emptyBelow)
					{
						//remove the 2 bottom pixels
						segTemp[y][x-1] = 0;
						segTemp[y][x] = 0;
						
					} else if (emptyLeft)
					{
						//remove the two left-most pixels
						segTemp[y-1][x-1] = 0;
						segTemp[y][x-1] = 0;
						
					} else if (emptyRight)
					{
						//remove the right-most pixels
						segTemp[y-1][x] = 0;
						segTemp[y][x] = 0;
						
					} else
					{
						//this is tricky, ignore for now
						//System.err.println(" *** Found a tricky border pixel case");
					}
				}
				
			}
		
		
		// filter border objects with only one neighbor
		int numRemoved = 0;
		do
		{
			numRemoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
						
						sumNeighbors1 += (segTemp[y-1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] > 0) ? 1 : 0;
						
						if (segTemp[y][x] > 0 && (sumNeighbors1 + sumNeighbors2) <= 1)
						{
							segTemp[y][x] = 0;
							numRemoved++;
						}			
					}
			
			System.err.println("Removed " + numRemoved + " entries where border pixels have no/too many neighbors");
			
		} while (numRemoved > 0);
		*/
		
			
		
		int maxObjId=0;		
		//convert temporary matrix to vectors
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				if (segTemp[y][x] > 0)	
				{
					objId = segTemp[y][x];
					if (objId > maxObjId)
					{
						//construct empty vector objects
						for (int i = maxObjId; i < objId; i++)
						{
							//System.err.println("Adding segmentation object vector " + objId);
							v[0].add( new Vector(1000) );
						}
						maxObjId = objId;
					}
					currVec = (Vector)v[0].get(objId-1);
					currVec.add( new Point(x,y) );
				}
		
		if (pb != null)
			pb.setValue(33);
		
		if ( !(frame != null && frame.getCancelled()) )
		{
			if (sortNeighbors)
			{
				v = setBordersAsNeighbors(v[0], frame, pb);
			}
			return v;
		} else
		{
			return null;
		}
	}
	
	/**
	* Fetches the border pixels of each segmentation object
	* The returned object is a Vector of Vectors containing Point objects of (x,y)
	* coordinates of border pixels. Vector[0] contains the border pixels.
	* If sorted, Vector[1] contains break points where the border is broken into pieces
	* (if the object contains cavities)
	* 
	* @param	seg	Segmentation matrix
	* @param	use8	If true, eight-connectivity will be used, otherwise four-connectivity
	* @param	sortNeighbors	If true, the neighboring pixels will be sorted
	*					so that they are adjacent in the Vector
	* @param	frame 	A frame object, used to keep track of whether user cancelled progress
	* @param	pb	A progressbar used to display the progress of the function
	* @return	A Vector[2] of Vector of Points of coordinates
	*/
	public static Vector[] fetchSegObjCoordBorder(int[][] seg, boolean use8, boolean sortNeighbors, JFrameExt frame, JProgressBar pb)
	{
		if (pb != null)
			pb.setValue(0);
		
		Vector v[] = new Vector[2];
		v[0] = new Vector(); //border pixels
		v[1] = new Vector(); //border break points, if any
		
		Vector currVec;
		int imgWidth = seg[0].length;
		int imgHeight = seg.length;
		
		//construct a temporary segmentation matrix which is used to store pixels
		//of only borders
		int[][] segTemp = new int[imgHeight][imgWidth];
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				segTemp[y][x] = 0;

		
		int objId=0;
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				if (seg[y][x] > 0)
				{
					objId = seg[y][x];
					
					//check if its a border pixel
					//boolean borderPixel = ( ( x > 0 && seg[y][x-1] != objId) || ( x < (imgWidth-1) && seg[y][x+1] != objId) ||
					//						( y > 0 && seg[y-1][x] != objId) || ( y < (imgHeight-1) && seg[y+1][x] != objId) );
					boolean borderPixel = ( ( x > 0 && seg[y][x-1] == 0) || ( x < (imgWidth-1) && seg[y][x+1] == 0) ||
											( y > 0 && seg[y-1][x] == 0) || ( y < (imgHeight-1) && seg[y+1][x] == 0) );
					
					//if (use8 && !borderPixel)
					//	borderPixel = ( ( x > 0 && y > 0 &&  seg[y-1][x-1] != objId) ||
					//			( x < (imgWidth-1) && y < (imgHeight-1) &&  seg[y+1][x+1] != objId) ||
					//			( x > 0 && y < (imgHeight-1) &&  seg[y+1][x-1] != objId) ||
					//			( x < (imgWidth-1) && y > 0 &&  seg[y-1][x+1] != objId) );
					
					if (use8 && !borderPixel)
						borderPixel = ( ( x > 0 && y > 0 &&  seg[y-1][x-1] == 0) ||
								( x < (imgWidth-1) && y < (imgHeight-1) &&  seg[y+1][x+1] == 0) ||
								( x > 0 && y < (imgHeight-1) &&  seg[y+1][x-1] == 0) ||
								( x < (imgWidth-1) && y > 0 &&  seg[y-1][x+1] == 0) );
					
					if (borderPixel)
					{
						segTemp[y][x] = objId;

					}
				}
		
		
		
		//make object pixels that are completely surrounded by borders into borders themselves
		int numAdded = 0;
		//do
		//{
		//	numAdded = 0;
			for (int y = 0; y < imgHeight; y++)
				for (int x = 0; x < imgWidth; x++)
				{
					//this is an object pixel
					if (seg[y][x] > 0 && segTemp[y][x] == 0)
					{
						boolean hasOnlyBorderNeighbors = ( ( x > 0 && (segTemp[y][x-1] != 0) &&
							( x < (imgWidth-1) && (segTemp[y][x+1] != 0) ) ) &&
							( y > 0 && (segTemp[y-1][x] != 0) )  &&
							( y < (imgHeight-1) && (segTemp[y+1][x] != 0)) );
						
						
						if (use8 && hasOnlyBorderNeighbors)
							hasOnlyBorderNeighbors = ( ( x > 0 && y > 0 &&  segTemp[y-1][x-1] != 0) &&
									( x < (imgWidth-1) && y < (imgHeight-1) &&  segTemp[y+1][x+1] != 0) &&
									( x > 0 && y < (imgHeight-1) &&  segTemp[y+1][x-1] != 0) &&
									( x < (imgWidth-1) && y > 0 &&  segTemp[y-1][x+1] != 0) );
						
						
						if (hasOnlyBorderNeighbors)
						{
							numAdded++;
							segTemp[y][x] = seg[y][x];
						}
					}
				}
			System.err.println("Added " + numAdded + " entries where object pixels only border to border pixels");
		//} while (numAdded > 0);
		
		
		
		//remove border pixels that are not neighboring to the object,
		//which is useful for breaking loops
		/*
		int numRemoved = 0;
		int numMoved = 0;
		do
		{
		*/
			/*
			numRemoved = 0;
			
			
			for (int y = 0; y < imgHeight; y++)
				for (int x = 0; x < imgWidth; x++)
				{
					//this is a border pixel
					if (segTemp[y][x] > 0)
					{
						boolean hasObjectNeighbors = ( ( x > 0 && (segTemp[y][x-1] == 0 && seg[y][x-1] != 0) ||
							( x < (imgWidth-1) && (segTemp[y][x+1] == 0 && seg[y][x+1] != 0) ) ) ||
							( y > 0 && (segTemp[y-1][x] == 0 && seg[y-1][x] != 0) )  ||
							( y < (imgHeight-1) && (segTemp[y+1][x] == 0 && seg[y+1][x] != 0)) );
						
						
						if (!hasObjectNeighbors)
						{
							numRemoved++;
							segTemp[y][x] = 0;
							seg[y][x] = 0;
						}
					}
				}
			
			System.err.println("Removed " + numRemoved + " entries where border pixels only neighbor to other border pixels");
			
			*/
			//// TIE-BREAKING
			//// Now we have to break ties in order to avoid loops in the border... we start off with 2 horiz/vert and 1-2 diag (simplest case)
		
			/*
			int sumNeighbors1 = 0;
			int sumNeighbors2 = 0;
			int neighborsThresh = 3;
			numMoved = 0;
			
			// break ties with 2 horiz/vert and 1-2 diag
			
			objId = -1;
			
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] == objId) ? 1 : 0;
						
						//if ( (sumNeighbors1 >= 2 && sumNeighbors2 >= 1) || (sumNeighbors1 >= 1 && sumNeighbors2 >= 2))
						if ( (sumNeighbors1 >= 2 && sumNeighbors2 >= 1))
						{
							numMoved++;
							System.err.println("!!! Moving border with >= 3 neighbors (" + sumNeighbors1 + " horz/vert + " + sumNeighbors2 + " diag) at (x=" + x + ",y=" + y + ")" );
							
							
							//expand this pixel to a nearby, free non-object area
							segTemp[y][x] = 0;
							
							
							if (seg[y-1][x] == 0)
							{
								segTemp[y-1][x] = objId;
								seg[y-1][x] = objId;
							
							} else if (seg[y+1][x] == 0)
							{
								segTemp[y+1][x] = objId;
								seg[y+1][x] = objId;
								
							} else if (seg[y][x-1] == 0)
							{
								segTemp[y][x-1] = objId;
								seg[y][x-1] = objId;
								
							} else if (seg[y][x+1] == 0)
							{
								segTemp[y][x+1] = objId;
								seg[y][x+1] = objId;
								
							} else
							{
								//shouldn't happen...
								System.err.println("No suitable neighbors at (x=" + x + ",y=" + y + ")" );
								segTemp[y][x] = objId;
								numMoved--;
							}
							
						} 
					}
				}
				
			System.err.println("Expanded " + numMoved + " pixels..." );
			
			
		} while (numRemoved > 0 || numMoved > 0);
		*/
		
		/*
		// break ties with 1 horiz/vert + 2 diag
		do
		{
			numMoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] == objId) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] == objId) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] == objId) ? 1 : 0;
						
						if ( (sumNeighbors1 == 1 && sumNeighbors2 >= 2))
						{
							numMoved++;
							System.err.println("!!! Moving border with >= 3 neighbors (" + sumNeighbors1 + " horz/vert + " + sumNeighbors2 + " diag) at (x=" + x + ",y=" + y + ")" );
							
							//move this pixel to a nearby, free (object) area
							segTemp[y][x] = 0;
							seg[y][x] = 0;
							
							if (seg[y-1][x] == objId && segTemp[y-1][x] == 0 )
								segTemp[y-1][x] = objId;
							else if (seg[y+1][x] == objId && segTemp[y+1][x] == 0)
								segTemp[y+1][x] = objId;
							else if (seg[y][x-1] == objId && segTemp[y][x-1] == 0)
								segTemp[y][x-1] = objId;
							else if (seg[y][x+1] == objId && segTemp[y][x+1] == 0)
								segTemp[y][x+1] = objId;
							else
							{
								//shouldn't happen, but who knows...
								System.err.println("No suitable neighbors at (x=" + x + ",y=" + y + ")" );
								
								segTemp[y][x] = objId;
								seg[y][x] = objId;
								numMoved--;
							}
						} 
					}
				}
				
			System.err.println("Moved " + numMoved + " pixels..." );
			
		} while (numMoved > 0);
		
		
		
		//break pure diagonal ties (this is done by splitting the pixel into two pieces
		do
		{
			numMoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
					if (segTemp[y][x] > 0)
					{
						objId = segTemp[y][x];
						
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
							
						sumNeighbors1 += (segTemp[y-1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] > 0) ? 1 : 0;
						
						if ( sumNeighbors1 == 0 && sumNeighbors2 >= 3)
						{
							System.err.println("!!! Found border with 3 neighbors (0 horz/vert + >= 3 diag) at (x=" + x + ",y=" + y + ")" );
							
							numMoved++;
							
							//expand this pixel to all nearby, free (object) areas
							segTemp[y][x] = 0;
							seg[y][x] = 0;
							
							//one vertical...
							if (seg[y-1][x] == objId && segTemp[y-1][x] == 0 )
								segTemp[y-1][x] = objId;
							else if (seg[y+1][x] == objId && segTemp[y+1][x] == 0)
								segTemp[y+1][x] = objId;
							
							// ... and one horizontal
							if (seg[y][x-1] == objId && segTemp[y][x-1] == 0)
								segTemp[y][x-1] = objId;
							else if (seg[y][x+1] == objId && segTemp[y][x+1] == 0)
								segTemp[y][x+1] = objId;
							
						}
					}
				}
			
		} while (numMoved > 0);
		
		*/
		
		
		
		
		
		/*
		// filter squared border objects
		for (int y = 3; y < imgHeight-3; y++)
			for (int x = 3; x < imgWidth-3; x++)
			{
				//squares should be removed, exactly how depends on the surroundings
				boolean square = (segTemp[y-1][x-1] > 0 && segTemp[y-1][x] > 0 && segTemp[y][x-1] > 0 && segTemp[y][x] > 0);
				
				if (square)
				{
					boolean emptyAbove = (segTemp[y-2][x-2] == 0 && segTemp[y-2][x-1] == 0 && segTemp[y-2][x] == 0 && segTemp[y-2][x+1] == 0);
					boolean emptyBelow =  (segTemp[y+1][x-2] == 0 && segTemp[y+1][x-1] == 0 && segTemp[y+1][x] == 0 && segTemp[y+1][x+1] == 0);
					boolean emptyLeft = (segTemp[y-2][x-2] == 0 && segTemp[y-1][x-2] == 0 && segTemp[y][x-2] == 0 && segTemp[y+1][x-2] == 0);
					boolean emptyRight = (segTemp[y-2][x+1] == 0 && segTemp[y-1][x+1] == 0 && segTemp[y][x+1] == 0 && segTemp[y+1][x+1] == 0);
					
					if (emptyAbove)
					{
						// remove the top 2 pixels
						segTemp[y-1][x-1] = 0;
						segTemp[y-1][x] = 0;
						
					} else if (emptyBelow)
					{
						//remove the 2 bottom pixels
						segTemp[y][x-1] = 0;
						segTemp[y][x] = 0;
						
					} else if (emptyLeft)
					{
						//remove the two left-most pixels
						segTemp[y-1][x-1] = 0;
						segTemp[y][x-1] = 0;
						
					} else if (emptyRight)
					{
						//remove the right-most pixels
						segTemp[y-1][x] = 0;
						segTemp[y][x] = 0;
						
					} else
					{
						//this is tricky, ignore for now
						//System.err.println(" *** Found a tricky border pixel case");
					}
				}
				
			}
		
		
		// filter border objects with only one neighbor
		int numRemoved = 0;
		do
		{
			numRemoved = 0;
			
			for (int y = 1; y < imgHeight-1; y++)
				for (int x = 1; x < imgWidth-1; x++)
				{
						sumNeighbors1 = 0;
						sumNeighbors2 = 0;
						
						sumNeighbors1 += (segTemp[y-1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y+1][x] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x-1] > 0) ? 1 : 0;
						sumNeighbors1 += (segTemp[y][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x+1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y-1][x-1] > 0) ? 1 : 0;
						sumNeighbors2 += (segTemp[y+1][x-1] > 0) ? 1 : 0;
						
						if (segTemp[y][x] > 0 && (sumNeighbors1 + sumNeighbors2) <= 1)
						{
							segTemp[y][x] = 0;
							numRemoved++;
						}			
					}
			
			System.err.println("Removed " + numRemoved + " entries where border pixels have no/too many neighbors");
			
		} while (numRemoved > 0);
		*/
		
			
		
		int maxObjId=0;		
		//convert temporary matrix to vectors
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				if (segTemp[y][x] > 0)	
				{
					objId = segTemp[y][x];
					if (objId > maxObjId)
					{
						//construct empty vector objects
						for (int i = maxObjId; i < objId; i++)
						{
							//System.err.println("Adding segmentation object vector " + objId);
							v[0].add( new Vector(1000) );
						}
						maxObjId = objId;
					}
					currVec = (Vector)v[0].get(objId-1);
					currVec.add( new Point(x,y) );
				}
		
		if (pb != null)
			pb.setValue(33);
		
		if ( !(frame != null && frame.getCancelled()) )
		{
			if (sortNeighbors)
			{
				v = setBordersAsNeighbors(v[0], frame, pb);
			}
			return v;
		} else
		{
			return null;
		}
	}
	
	/**
	* Makes sure that the border pixels are neighbors so that the border line can be traced.
	* Also separates the borders into separate pieces as Vectors, if they are not connected
	* (which can happen if the object has 'cavities'
	* 
	* @param	vecBorder	The Vector of Vector of Points with border pixels
	* @param	frame 	A frame object to keep track of whether the user cancelled the operation
	* @param	pb	A progressbar to display the progress of the function
	* @return	A Vector of Vector of Vector of Points, sorted
	*/
	protected static Vector[] setBordersAsNeighbors( Vector vecBorder, JFrameExt frame, JProgressBar pb)
	{
		//if (pb != null)
		//	pb.setValue(0);
		
		Vector[] newBorderVec = new Vector[3];
		newBorderVec[0] = new Vector( vecBorder.size() ); //contains the (sorted) border elements
		newBorderVec[1] = new Vector(); //contains indices of potential 'break point', where the border breaks
		newBorderVec[2] = new Vector(); //contains indices of 'break points', but only to inner borders
		
		double pbValue = pb.getValue();
		double pbInc = (100.0-pbValue)/vecBorder.size();
		
		for (int i = 0; i < vecBorder.size(); i++)
		{
	
			Vector newBorder = new Vector( vecBorder.size() );
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			//Vector vecCurrBorderCopy = new Vector(vecCurrBorder.size());
			
			Vector vecBreakPointsAll = new Vector(); //contains indices of where the border 'breaks', if any
			Vector vecBreakPointsInner = new Vector(); //contains indices where
			
			if (vecCurrBorder.size() > 0)
			{
			
				//copy the vector so that we can safely adjust it
				//for (int j = 1; j < vecCurrBorder.size(); j++)
				//{
				//	vecCurrBorderCopy.add( vecCurrBorder.get(j) );
				//}
				
				//here we store the border entries
				boolean[] pointAdded = new boolean[vecCurrBorder.size()];
				int[] pointAddedInd = new int[vecCurrBorder.size()];
				for (int j = 0; j < vecCurrBorder.size(); j++)
				{
					pointAdded[j] = false;
					pointAddedInd[j] = -1;
				}
				
				Point refPoint = (Point)vecCurrBorder.get(0); //the point which we will compare against
				pointAdded[0] = true;
				pointAddedInd[0] = 0;
				newBorder.add( new Point(refPoint) );
				
				//stack of pixels where we have several equal matches of neighbors,
				//which we keep track of
				Vector crossRoadPixels = new Vector(); 
				
				//repeat while there still are border pixels left to sort
				int numAdded = 1;
				while( newBorder.size() < vecCurrBorder.size() )
				{
					Point newNeighbor = new Point(-1,-1), compPoint = new Point(-1,-1);
					Vector matches = new Vector();
					int minDist = Integer.MAX_VALUE;
					double minDistEucl = Double.POSITIVE_INFINITY;
					int minDistIndex = -1;
					int distX,distY,distManhattan;
					double distEucl;
					boolean pureNeighborMatched = false;
					
					//keep track of the progress
					if (pb != null)
					{
						pbValue += pbInc*(1/(double)vecCurrBorder.size());
						pb.setValue( (int)pbValue);
					}
					

					//loop over all remaining border pixels to find the one closest to the current reference point
					for (int k = 1; k < vecCurrBorder.size(); k++)
					{

						if (!pointAdded[k])
						{
						
							if (frame != null && frame.getCancelled() )
								return null;
							
							compPoint = (Point)vecCurrBorder.get(k);
							distX = (int)Math.abs( compPoint.getX() - refPoint.getX() );
							distY = (int)Math.abs( compPoint.getY() - refPoint.getY() );
							distManhattan = distX + distY;
							distEucl = compPoint.distance(refPoint);
							
							// A neighboring pixel must:
						
							// 2) Have (Manhattan) distance greater than zero, otherwise we are comparing the point to itself
							// 3) Have the smallest possible distance to the reference point
							
								/*
								if (distManhattan < minDist || ( distManhattan == minDist && distEucl < minDistEucl ) )
								{
									//we have an unique match
									minDistIndex = k;
									minDist = distManhattan;
									minDistEucl = distEucl;
									//newNeighbor = new Point( (int)compPoint.getX(), (int)compPoint.getY() );
									//System.err.println("Entry " + j + " has neighbor " + k + " with distance " + distManhattan );
									
									
									matches.add( new Integer(k) );
								
								} else 
								*/
								if ( distManhattan > 0)
								{
									
									if (distManhattan < minDist || ( distManhattan == minDist && distEucl < minDistEucl ))
									{
										minDistIndex = k;
										minDist = distManhattan;
										minDistEucl = distEucl;
										
										if (distEucl <= SQRT2 )
										{
											// make sure that we don't wipe the vector if we have had a previous matching
											//point, with distance SQRT2, and this distance is 1
											if (!pureNeighborMatched)
											{
												pureNeighborMatched = true;
												matches = new Vector();
											} 
											
										} else
										{
											matches = new Vector();
										}
										
										matches.add( new Integer(k) );
										
									} else if (distEucl <= SQRT2)
									{
										//always add neighbors within the vicinity to the stack, even if they're not the best match
										matches.insertElementAt( new Integer(k), 0);
										
									}

								}
							
						}
					}
					
					
					
					if (minDistIndex >= 0)
					{
						if ( minDistEucl > SQRT2 )
						{
							//bad match, we want to retort to an old cross-road, if available
							//System.err.println("Object " + (i+1) + ": Point " + numAdded + "/" + vecCurrBorder.size() + " at (" + refPoint.getX() + "," + refPoint.getY() + ") denotes a border breakpoint");
							
							// These should be added as well!
							vecBreakPointsAll.add( new Integer(numAdded) );
							
							//go back to crossroad and make sure it's not been incorporated already
							boolean hasCrossRoadPixel = (crossRoadPixels.size() > 0);
							int crossRoadPixelInd = -1;
							if ( hasCrossRoadPixel)
							{
								//System.err.println("  ** We potentially have " + crossRoadPixels.size() + " cross road pixels to go back to");
								
								boolean foundPixel = false;
								while (crossRoadPixels.size() > 0 && crossRoadPixelInd < 0)
								{
									//fetch first available crossroad
									int crpi = ( (Integer)crossRoadPixels.get(0)).intValue();
									crossRoadPixels.removeElementAt(0);
									if (!pointAdded[crpi])
									{
										crossRoadPixelInd = crpi;
									}
								}
							}
							
							if (crossRoadPixelInd >= 0)
							{
								//we found a previous crossroad that we can go back to
								minDistIndex = crossRoadPixelInd;
								//System.err.println("  ** Index " + numAdded + " is a dead end: moving to old border cross road at index " + minDistIndex);
								
							} else
							{
								//we have no close neighbor and no 
								//System.err.println("  ** Index " + numAdded + " denotes a break towards an inner border");
								vecBreakPointsAll.add( new Integer(numAdded) );
								vecBreakPointsInner.add( new Integer(numAdded) );
							}
							
							//update the 
							newNeighbor = (Point)vecCurrBorder.get(minDistIndex);
							pointAdded[minDistIndex] = true;
							pointAddedInd[minDistIndex] = numAdded;
							
							refPoint = new Point( (int)newNeighbor.getX(), (int)newNeighbor.getY() );
							newBorder.add(newNeighbor);
							
							numAdded++;
							
						} else
						{
							//we have at least one good (neighboring) match
							
							//if (matches.size() > 1)
							//{
							//	System.err.println(" * Reference point at " + numAdded + " has " + matches.size() + " equally good neighbors");
							//}
							
							//if we have multiple matches, we add all but one to the 'cross-roads' stack,
							//which may be used later
							while (matches.size() > 1)
							{
								crossRoadPixels.insertElementAt(matches.get(0), 0);
								matches.removeElementAt(0);
							}
							
							//use the matching index to fetch the border pixel
							minDistIndex = ( (Integer)matches.get(0)).intValue();
							newNeighbor = (Point)vecCurrBorder.get(minDistIndex);
							pointAdded[minDistIndex] = true;
							pointAddedInd[minDistIndex] = numAdded;
							
							refPoint = new Point( (int)newNeighbor.getX(), (int)newNeighbor.getY() );
							newBorder.add(newNeighbor);
							
							numAdded++;
							//vecCurrBorderCopy.removeElementAt(minDistIndex);	
						}
						
					} else
					{
						System.err.println("[WARNING] Object " + (i+1) + ": Point at (" + refPoint.getX() + "," + refPoint.getY() + ") has no neighbor");
					}
					
					
					
					
				}
				
				vecBreakPointsAll.add( new Integer(vecCurrBorder.size() ) ); //always add last element
				vecBreakPointsInner.add( new Integer(vecCurrBorder.size() ) ); //always add last element
				
				// Store the separate pieces of the border into vectors
				/*
				Vector vecNewBorderExt = new Vector();
				if ( vecBreakPoint.size() > 0)
				{
					int lastIndex = 0;
					int currIndex = -1;
					for (int j = 0; j < vecBreakPoint.size(); j++)
					{
							Vector vecTemp = new Vector();
							currIndex = ( (Integer)vecBreakPoint.get(j)).intValue();
							for (int k = lastIndex; k < currIndex; k++)
							{
								vecTemp.add( newBorder.get(k) );
							}
							lastIndex = currIndex;
							vecNewBorderExt.add( vecTemp );
					}
					
				} else
				{
					vecNewBorderExt.add( newBorder );
				}
				
				newBorderVec.add(vecNewBorderExt);
				*/
			
				System.err.println("Object " + (i+1) + ": We have " + vecBreakPointsInner.size() +
					" inner break points and " + vecBreakPointsAll.size() + " in total");
			
				newBorderVec[0].add(newBorder);
				newBorderVec[1].add(vecBreakPointsAll);
				newBorderVec[2].add(vecBreakPointsInner);
				
			} else
			{
				// the border is empty, so just return it
				System.err.println(" * Object " + (i+1) + " has no border elements");
				
				newBorderVec[0].add( new Vector() );
				newBorderVec[1].add( new Vector() );
			}
			
			//pbValue += pbInc;
			
		}
		
		if (pb != null)
			pb.setValue(100);
		
		if (frame == null || !frame.getCancelled())
			return newBorderVec;
		else
			return null;
	}	
	
	/**
	* Filter that removes extremely large or small objects, which are either background
	* or artifacts, respectively.
	* Returns a boolean[] object that reveals if the returned object is to be kept or not.
	* 
	* @param	vec	Vector of Vector of Points
	* @param	imageArea	The total area (number of pixels) of the image, used to estimate the object size threshold
	* @return	Returns a boolean[] object that reveals if the returned object is to be kept or not.
	*/
	public static boolean[] filterObjects(Vector vec, long imageArea, double minObjSizeRel, double minObjDensRel)
	{
		boolean[] retVec = new boolean[ vec.size() ];
		
		int x,y;
		
		
		for (int i = 0; i < vec.size(); i++)
		{
			//find the extreme points 
			Point maxX = new Point(Integer.MIN_VALUE, 0);
			Point minX = new Point(Integer.MAX_VALUE, 0);
			Point maxY = new Point(0, Integer.MIN_VALUE);
			Point minY = new Point(0, Integer.MAX_VALUE);
		
			Vector vecCurr = (Vector)vec.get(i);
			for (int j = 0; j < vecCurr.size(); j++)
			{
				Point p = (Point)vecCurr.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				if (x > maxX.getX())
					maxX = new Point(x, y);
				if (x < minX.getX())
					minX = new Point(x, y);
					
				if (y > maxY.getY())
					maxY = new Point(x, y);
				if (y < minY.getY())
					minY = new Point(x, y);
			}
			
			double distX = maxX.distance(minX);
			double distY = maxY.distance(minY);
			double dens =  vecCurr.size()/(distX*distY);
			double relArea = vecCurr.size()/(double)imageArea;
			
			if (dens < minObjDensRel)
			{
				System.err.println("The density of object " + i + " is " + dens + ", which is below limit of " + minObjDensRel);
				retVec[i] = false;
			} else if (relArea < minObjSizeRel)
			{
				System.err.println("The relative area of object " + i + " is " + relArea + ", which is below limit of " + minObjSizeRel);
				retVec[i] = false;
			} else
			{
				retVec[i] = true;
			}
			
			//System.err.println("Width of object " + i + " is " + lengthX + ", height is " + lengthY + " and area is " +
			//	vecCurr.size());
			//System.err.println("The density of the object " + i + " is " + dens);
			
		}
		
		return retVec;
	}
	
	/**
	* Paints a binary image matrix with values from a segmentation object (Vector of Vector of Points)
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar
	* @param	img	integer matrix, where the objects will be painted
	* @param	highVal	The value of entries with an object. Typically 255 or 1. All other entries will be set to 0.
	*/
	public static void paintBinaryMatrix(Vector vec, int[][] img, int highVal)
	{
		int imgWidth = img[0].length;
		int imgHeight = img.length;
		
		int objId=0;
		int maxObjId=0;
		
		for (int y = 0; y < imgHeight; y++)
			for (int x = 0; x < imgWidth; x++)
				img[y][x] = 0;
		
		int x,y;
		
		for (int i = 0; i < vec.size(); i++)
		{
			Vector vecCurr = (Vector)vec.get(i);
			for (int j = 0; j < vecCurr.size(); j++)
			{
				Point p = (Point)vecCurr.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				img[y][x] = highVal;
			}
		}
		
	}
	
	/**
	* Contains a calculation of lines crossing the objects and their intersections.
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	forceOrtho	If true, the distance lines (measuring width and height) will be ~perpendicular. If false, they will form the longest respective distances
	* @param	forceHorizVert	If true, the distance lines will always be horizontal and vertical. If false, they will form the longest respective distances
	* @return	A Vector[2] of Vector[2] (vertical/horizontal) of Vector of Point[2] containing start and end of the lines
	*/
	public static Vector[] fetchHorizVertLines(int[][] imgSeg, Vector vec, Vector vecBorder, boolean forceOrtho, boolean forceHorizVert)
	{
		
		int x,y;
		
		Vector[] retVecMain = new Vector[2];
		
		Vector retVecIntersect = new Vector(vec.size()); //contains the intersection points for all objects
		Vector retVec = new Vector(vec.size()); //contains all the remaining information
		
		for (int i = 0; i < vec.size(); i++)
		{
			Vector[] retVecCurr = new Vector[2];
			
			//find the extreme points 
			Point maxX = new Point(Integer.MIN_VALUE, 0);
			Point minX = new Point(Integer.MAX_VALUE, 0);
			Point maxY = new Point(0, Integer.MIN_VALUE);
			Point minY = new Point(0, Integer.MAX_VALUE);
		
			Vector vecCurr = (Vector)vec.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			
			//find the minimum/maximum horizontal and vertical points
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
				Point p = (Point)vecCurrBorder.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				if (x > maxX.getX())
					maxX = new Point(x, y);
				if (x < minX.getX())
					minX = new Point(x, y);
					
				if (y > maxY.getY())
					maxY = new Point(x, y);
				if (y < minY.getY())
					minY = new Point(x, y);
			}
			
			if (forceHorizVert)
			{
				//horizontal/vertical lines is enforces, so we must find horizontal/vertical lines
				//that maximize the width/length	
				Point maxXortho = new Point(Integer.MIN_VALUE, 0);
				Point minXortho = new Point(Integer.MAX_VALUE, 0);
				Point maxYortho = new Point(0, Integer.MIN_VALUE);
				Point minYortho = new Point(0, Integer.MAX_VALUE);
				
				Point currPoint1 = new Point(0,0);
				Point currPoint2 = new Point(0,0);
				
				//find maximum height
				double maxDist = 0.0;
				double currDist = 0.0;
				for (int xx = (int)minX.getX(); xx <= (int)maxX.getX(); xx++)
				{
					//find the border pixel(s) that matches this coordinate
					Vector candidatePoints = new Vector(20);
					for (int j = 0; j < vecCurrBorder.size(); j++)
					{
						currPoint1 = (Point)vecCurrBorder.get(j);
						if ( (int)currPoint1.getX() == xx)
						{
							candidatePoints.add(currPoint1);
						}
					}
					//System.err.println("  ... Dealing with " + candidatePoints.size() + " candidate points");
					
					//use the combination of pixels that gets the greatest distance
					for (int j = 0; j < (candidatePoints.size()-1); j++)
						for (int k = (j+1); k < candidatePoints.size(); k++)
						{
							currPoint1 = (Point)candidatePoints.get(j);
							currPoint2 = (Point)candidatePoints.get(k);
							currDist = currPoint1.distance(currPoint2);
							
							if (currDist > maxDist)
							{
								maxDist = currDist;
								if (currPoint1.getY() > currPoint2.getY() )
								{
									maxYortho = new Point( (int)currPoint1.getX(), (int)currPoint1.getY() );
									minYortho = new Point( (int)currPoint2.getX(), (int)currPoint2.getY() );
								} else
								{
									maxYortho = new Point( (int)currPoint2.getX(), (int)currPoint2.getY() );
									minYortho = new Point( (int)currPoint1.getX(), (int)currPoint1.getY() );
								}
							}
						}
					
				}
				maxY=maxYortho;
				minY=minYortho;
				
				
				//find maximum width
				maxDist = 0.0;
				currDist = 0.0;
				for (int yy = (int)minY.getY(); yy <= (int)maxY.getY(); yy++)
				{
					//find the border pixel(s) that matches this coordinate
					Vector candidatePoints = new Vector(20);
					for (int j = 0; j < vecCurrBorder.size(); j++)
					{
						currPoint1 = (Point)vecCurrBorder.get(j);
						if ( (int)currPoint1.getY() == yy)
						{
							candidatePoints.add(currPoint1);
						}
					}
					
					//use the combination of pixels that gets the greatest distance
					for (int j = 0; j < (candidatePoints.size()-1); j++)
						for (int k = (j+1); k < candidatePoints.size(); k++)
						{
							currPoint1 = (Point)candidatePoints.get(j);
							currPoint2 = (Point)candidatePoints.get(k);
							currDist = currPoint1.distance(currPoint2);
							
							if (currDist > maxDist)
							{
								maxDist = currDist;
								if (currPoint1.getX() > currPoint2.getX() )
								{
									maxXortho = new Point( (int)currPoint1.getX(), (int)currPoint1.getY() );
									minXortho = new Point( (int)currPoint2.getX(), (int)currPoint2.getY() );
								} else
								{
									maxXortho = new Point( (int)currPoint2.getX(), (int)currPoint2.getY() );
									minXortho = new Point( (int)currPoint1.getX(), (int)currPoint1.getY() );
								}
							}
						}
					
				}
				maxX=maxXortho;
				minX=minXortho;
				
			}
			
			
			
			
			//find the intersection point between the new line and the remaining line (horizontical or vertical)
			Point[] lineHorizCenter = new Point[2];
			lineHorizCenter[0] = minX;
			lineHorizCenter[1] = maxX;
				
			Point[] lineVertCenter = new Point[2];
			lineVertCenter[0] = minY;
			lineVertCenter[1] = maxY;
				
				
			//
			Point2D.Double pointIntersectCenter = MiscMath.lineIntersection(lineHorizCenter, lineVertCenter);
			if (!GrayscaleImageEdit.pointIsWithinObject(imgSeg, (i+1), pointIntersectCenter) )
				pointIntersectCenter = MiscMath.findPointOnLine(lineHorizCenter, 0.5);
			
			//System.err.println("Horizontal line goes from " + lineHorizCenter[0] + " --> " + lineHorizCenter[1]);
			//System.err.println("Intersection center is " + pointIntersectCenter);
			
			
			
			//orthogonal to the horizontal line
			Point[] lineHorizOrtho = GrayscaleImageEdit.findPerpendicularLineFast(imgSeg, (i+1), pointIntersectCenter, lineHorizCenter );
			//orthogonal to the vertical line
			
			//System.err.println("Perpendicular line goes from " + lineHorizOrtho[0] + " --> " + lineHorizOrtho[1]);
			
			
			Point[] lineVertOrtho;

			if (forceOrtho && !forceHorizVert)
			{
				lineVertCenter = lineHorizOrtho;
				lineVertOrtho = lineHorizCenter;
			} else
			{
				
				lineVertOrtho = GrayscaleImageEdit.findPerpendicularLineFast(imgSeg, (i+1), pointIntersectCenter, lineVertCenter );
			}
			
			Vector vecCurrVertLines = new Vector();
			vecCurrVertLines.add( null ); //for now, will be filled later
			Vector vecCurrHorizLines = new Vector();
			vecCurrHorizLines.add( null ); //for now, will be filled later
			
			
			
			double[] adjustments = {0.25, 0.50, 0.75};
			Point2D.Double pointIntersect;
			for (int k = 0; k < adjustments.length; k++)
			{
				pointIntersect = MiscMath.findPointOnLine(lineVertCenter, adjustments[k]);
				//System.err.println("Horiz. coordinate at adjust " + adjustments[k] + " is " + pointIntersect );
				Point[] linePerpHoriz;
				linePerpHoriz = GrayscaleImageEdit.findPerpendicularLineFast(imgSeg, (i+1), pointIntersect, lineHorizOrtho);
				vecCurrHorizLines.add(linePerpHoriz);
				
				pointIntersect = MiscMath.findPointOnLine(lineHorizCenter, adjustments[k]);
				Point[] linePerpVert;
				linePerpVert = GrayscaleImageEdit.findPerpendicularLineFast(imgSeg, (i+1), pointIntersect, lineVertOrtho);
				vecCurrVertLines.add(linePerpVert);
			
			}
				
			//the perpendicular line is used to find appropriately spaced line,
			//but we only replace both if the 'forced orthogonality' is checked
			vecCurrVertLines.setElementAt(lineVertCenter, 0);
			vecCurrHorizLines.setElementAt(lineHorizCenter, 0);
				
			//System.err.println("Vertical line goes from " + lineVertCenter[0] + " --> " + lineVertCenter[1]);
			//System.err.println("Horizontal line goes from " + lineHorizCenter[0] + " --> " + lineHorizCenter[1]);
				
						
			/*
			double lineAngle = MiscMath.pointAngle(minY, maxY);
					
			Point[] linePerp = GrayscaleImageEdit.findLongestPerpendicularLineExhaustive( vecCurrBorder, lineAngle);
					
			if (linePerp[0].getX() < linePerp[1].getX())
			{
				minX = linePerp[0];
				maxX = linePerp[1];
			} else
			{
				minX = linePerp[1];
				maxX = linePerp[0];
			}
			*/
			
			
			
			
			Point pIntersect = new Point((int)Math.round(pointIntersectCenter.getX()), (int)Math.round(pointIntersectCenter.getY()) );
			retVecIntersect.add(pIntersect);
			//System.err.println("  Intersect: (" + intersectX + "," + intersectY + ")");
			
			
			/*
			Point[] minDistVert25 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.25,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			Point[] minDistVert50 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.50,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			Point[] minDistVert75 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.75,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
				
			Point[] minDistHoriz25 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.25,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			Point[] minDistHoriz50 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.50,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			Point[] minDistHoriz75 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.75,
				interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			
			
			//create a Point[] object for the 'maximum distance' lines
			Point[] maxDistVert = new Point[2];
			Point[] maxDistHoriz = new Point[2];
			maxDistVert[0] = new Point((int)minX.getX(), (int)minX.getY());
			maxDistVert[1] = new Point((int)maxX.getX(), (int)maxX.getY());
			maxDistHoriz[0] = new Point((int)minY.getX(), (int)minY.getY());
			maxDistHoriz[1] = new Point((int)maxY.getX(), (int)maxY.getY());
			
			//add to the vector
			Vector vertDists = new Vector();
			vertDists.add(maxDistVert);
			vertDists.add(minDistVert25);
			vertDists.add(minDistVert50);
			vertDists.add(minDistVert75);
			
			Vector horizDists = new Vector();
			horizDists.add(maxDistHoriz);
			horizDists.add(minDistHoriz25);
			horizDists.add(minDistHoriz50);
			horizDists.add(minDistHoriz75);
			*/
			
			retVecCurr[1] = vecCurrHorizLines;
			retVecCurr[0] = vecCurrVertLines;
			retVec.add(retVecCurr);
			
		}
		
		retVecMain[0] = retVec;
		retVecMain[1] = retVecIntersect;
		
		return retVecMain;
	}
	
	/**
	* Contains a calculation of vertical lines crossing the objects and their intersections, where the first line
	* is pre-specified by the user.
	* 
	* @param	horizLine	An array of Point[s with start and stop coordinates of the horizontal line
	* @param	vertLine	An array of Point[] with start and stop coordinates of the vertical line
	* @return	A Vector[2] (vertical/horizontal) of Vector of Point[2] containing start and end of the vertical lines
	*/
	public static Vector[] fetchHorizVertLinesInt(Point[] vertLine, Point[] horizLine, Vector vecCurrBorder)
	{
		int x,y;
		
		Vector[] retVecCurr = new Vector[2];
		
		Point maxX = new Point(Integer.MIN_VALUE, 0);
		Point minX = new Point(Integer.MAX_VALUE, 0);
		Point maxY = new Point(0, Integer.MIN_VALUE);
		Point minY = new Point(0, Integer.MAX_VALUE);
		
		if (vertLine[0].getY() < vertLine[1].getY())
		{
			minY = vertLine[0];
			maxY = vertLine[1];
		} else
		{
			minY = vertLine[1];
			maxY = vertLine[0];
		}
		
		if (horizLine[0].getX() < horizLine[1].getX())
		{
			minX = horizLine[0];
			maxX = horizLine[1];
		} else
		{
			minX = horizLine[1];
			maxX = horizLine[0];
		}
		
		/*
		Point[] linesBoth = new Point[4];
		linesBoth[0] = horizLine[0];
		linesBoth[1] = horizLine[1];
		linesBoth[2] = vertLine[0];
		linesBoth[3] = vertLine[1];
		
		
		//find the extreme points 
		Point maxX = new Point(Integer.MIN_VALUE, 0);
		Point minX = new Point(Integer.MAX_VALUE, 0);
		Point maxY = new Point(0, Integer.MIN_VALUE);
		Point minY = new Point(0, Integer.MAX_VALUE);
	
		for (int j = 0; j < linesBoth.length; j++)
		{
			Point pVert = linesBoth[j];
			x = (int)p.getX();
			y = (int)p.getY();
			
			if (x > maxX.getX())
				maxX = new Point(x, y);
			if (x < minX.getX())
				minX = new Point(x, y);
				
			if (y > maxY.getY())
				maxY = new Point(x, y);
			if (y < minY.getY())
				minY = new Point(x, y);
		}
		*/
			
		
		double slopeX = (maxX.getY() - minX.getY())/(maxX.getX() - minX.getX() );
		double interceptX = 0.0;
		boolean slopeMissingX = (slopeX == Double.POSITIVE_INFINITY || slopeX == Double.NEGATIVE_INFINITY || Double.isNaN(slopeX));
		if (slopeMissingX)
		{
			slopeX = 0.0;
			interceptX = maxX.getY(); //not the intercept, but can be useful later
			//System.err.println("  ** X slope is undefined");
		} else
		{
			interceptX = -slopeX*maxX.getX() + maxX.getY();
		}
		
		//System.err.println("  Line descr. the X gradient: y = " + slopeX + "*x + " + interceptX );

		double slopeY = (maxY.getY() - minY.getY())/(maxY.getX() - minY.getX() );
		double interceptY = 0.0;
		boolean slopeMissingY =  (slopeY == Double.POSITIVE_INFINITY || slopeY == Double.NEGATIVE_INFINITY || Double.isNaN(slopeY));
		if (slopeMissingY)
		{
			slopeY = 0.0;
			interceptY = maxY.getX(); //not the intercept, but can be useful later
			//System.err.println("  ** Y slope is undefined");
		} else
		{
			interceptY = -slopeY*maxY.getX() + maxY.getY();
		}
		//System.err.println("  Line descr. the Y gradient: y = " + slopeY + "*x + " + interceptY );

		
		
		//calc the intersection point
		//double intersectX = (interceptY-interceptX)/(slopeX - slopeY);
		//double intersectY = (slopeX*intersectX)+interceptX;
		double intersectX = 0;
		double intersectY = 0;
		if (slopeMissingY && slopeMissingX)
		{
			intersectX = interceptY;
			intersectY = interceptX; 
		} else if (slopeMissingX)
		{
			intersectY = interceptX;
			intersectX = interceptX*slopeY + interceptY;
			
		} else if (slopeMissingY)
		{
			intersectX = interceptY;
			intersectY = interceptY	*slopeX + interceptX; 
			
		} else
		{
			intersectX = (interceptY-interceptX)/(slopeX - slopeY);
			intersectY = (slopeX*intersectX)+interceptX;
		}
		
		
		Point intersect = new Point((int)intersectX, (int)intersectY);
		//retVecIntersect.add(intersect);
		
		//System.err.println("  Intersect: (" + intersectX + "," + intersectY + ")");
		
		
		Point[] minDistVert25 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.25,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
		Point[] minDistVert50 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.50,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
		Point[] minDistVert75 = findBorderPixelsInt(true, vecCurrBorder, minY.getY() + (maxY.getY() - minY.getY() )*0.75,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
			
		Point[] minDistHoriz25 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.25,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
		Point[] minDistHoriz50 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.50,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
		Point[] minDistHoriz75 = findBorderPixelsInt(false, vecCurrBorder, minX.getX() + (maxX.getX() - minX.getX() )*0.75,
			interceptX, interceptY, slopeX, slopeY, slopeMissingX, slopeMissingY, intersectX, intersectY);
		
		
		//create a Point[] object for the 'maximum distance' lines
		Point[] maxDistVert = new Point[2];
		Point[] maxDistHoriz = new Point[2];
		maxDistVert[0] = new Point((int)minX.getX(), (int)minX.getY());
		maxDistVert[1] = new Point((int)maxX.getX(), (int)maxX.getY());
		maxDistHoriz[0] = new Point((int)minY.getX(), (int)minY.getY());
		maxDistHoriz[1] = new Point((int)maxY.getX(), (int)maxY.getY());
		
		//add to the vector
		Vector vertDists = new Vector();
		vertDists.add(maxDistVert);
		vertDists.add(minDistVert25);
		vertDists.add(minDistVert50);
		vertDists.add(minDistVert75);
		
		Vector horizDists = new Vector();
		horizDists.add(maxDistHoriz);
		horizDists.add(minDistHoriz25);
		horizDists.add(minDistHoriz50);
		horizDists.add(minDistHoriz75);
		
		retVecCurr[0] = horizDists;
		retVecCurr[1] = vertDists;
		
		
		return retVecCurr;		
		
	}
	
	/**
	* Fetches the contour indent connecting points (aka contour hotspots), described as Point[2] for
	* each indent.
	* 
	* @param	vecContours	Vector of Vector of Points containing contour hotspot elements
	* @param	segMat	Segmentation matrix, used to determine if two hotspot pixels are 'connectable'
	* @return	A Vector of Vector of Point[2]s describing connecting lines for the indents for each object
	*/
	public static Vector fetchContourHotspotConnections(Vector vecContours, int[][] segMat)
	{
		Vector retVec = new Vector();
		for (int i = 0; i < vecContours.size(); i++)
		{
			
			Vector vecCurrContour = (Vector)vecContours.get(i);

			
			//calculate average distance between indents
			Point p1, p2;
			Vector v1, v2;
			double distTotGlobal = 0.0;
			
			Vector vecContourHotspotCenters = new Vector(); //contains Point[2] centers of contour hotspots
			for (int j = 0; j < vecCurrContour.size(); j++)
			{
				// compare this indent to the neighboring indent (counterclock-wise)
				int k = j-1;
				if (k < 0)
					k = vecCurrContour.size()-1;
					
				v1 = (Vector)vecCurrContour.get(j);
				v2 = (Vector)vecCurrContour.get(k);
					
				//find the two closest spots in the two neighboring hotspots
				int index1 = -1, index2 = -1;
				double maxDist = Double.NEGATIVE_INFINITY;
				double currDist;
				for (int m = 0; m < v1.size(); m++)
					for (int n = 0; n < v2.size(); n++)
					{
						p1 = (Point)v1.get(m);
						p2 =  (Point)v2.get(n);
						if (pixelsConnectable(segMat, p1, p2, false) )
						{
						
							currDist = p1.distance(p2);
							if (currDist > maxDist)
							{
								maxDist = currDist;
								index1 = m;
								index2 = n;
							}
						}
					}
				
				Point[] pVec = new Point[2];
				
				if (index1 >= 0 && index2 >= 0)
				{
					pVec[0] = (Point)v1.get(index1);
					pVec[1] = (Point)v2.get(index2);
				}
				
				vecContourHotspotCenters.add(pVec);
			}
			
			retVec.add(vecContourHotspotCenters);
		
		}
		
		return retVec;
	
	}
	
	
	
	
	
	/**
	* Fetches the indent depths as connecting lines (Vector of Vector of Point[2]s)
	* 
	* @param	vecContourHotspotConnections	Vector of Vector of Point[2]s containing connections btw contour hotspot elements
	* @param	vecContourHotspotIndices	Vector of Vector of Integer containing the corresponding indices
	* @param	vecBorders	Vector of Vector of Points containing border elements for object
	* @param	segMat	The segmentation matrix
	* @param	pb	Progress bar object, to display progress of the algorithm
	* @return	A Vector of Vector of Point[2]s describing connecting lines for the indents for each object
	*/
	public static Vector fetchIndentDepths(Vector vecContourHotspotConnections, Vector vecContourHotspotIndices, Vector vecBorders, int[][] segMat, JProgressBar pb)
	{
		if (pb != null)
			pb.setValue(0);
		
		double pbStepLength = 100.0/vecContourHotspotConnections.size();
		
		Vector retVec = new Vector();
		
		
		for (int i = 0; i < vecContourHotspotConnections.size(); i++)
		{
			Vector retVecCurr = new Vector();
			Vector vecCurrCHSC = (Vector)vecContourHotspotConnections.get(i);
			Vector vecCurrCHSI = (Vector)vecContourHotspotIndices.get(i);
			Vector vecCurrBorder = (Vector)vecBorders.get(i);
			

			int lastInd = ( (Integer)vecCurrCHSI.get(0)).intValue();
			int currInd = lastInd;
			Point lastPoint = (Point)vecCurrCHSC.get(0);
			Point currPoint = new Point( lastPoint);
			

			for (int j = 1; j < vecCurrCHSC.size(); j++)
			{
				pb.setValue( (int)Math.round( (100.0*j/(vecCurrCHSC.size()-1))/vecContourHotspotConnections.size() + i*pbStepLength ) );
				
				currPoint = (Point)vecCurrCHSC.get(j);
					
				//calc the angle btw hotspots
				if (lastPoint != null && currPoint != null)
				{
					currInd = ( (Integer)vecCurrCHSI.get(j)).intValue();
					double contourAngle = MiscMath.pointAngle(lastPoint, currPoint);
					
					//System.err.println("   --> Angle btw contour points is " + contourAngle);
					
					//this vector contains contour border points that are inside indent j (btw hotspot j and k)
					Vector vecIndentSurface = GrayscaleImageEdit.pixelsBetweenPoints(lastPoint, currPoint);
					
					if (vecIndentSurface != null &&  currInd > (lastInd+1))
					{
						//add possible border pixels (that are between the two hotspot pixels)
						Vector vecPossibleBorders = new Vector(currInd - lastInd);
						for (int k = lastInd+1; k < currInd; k++)
							vecPossibleBorders.add( new Point( (Point)vecCurrBorder.get(k) ) );
						
						double maxDist = Double.NEGATIVE_INFINITY;
						double currAngle;
						int maxDistIndex1 = -1;
						int maxDistIndex2 = -1;
						Point p1, p2;
						
						for (int k = 0; k < vecPossibleBorders.size(); k++)
						{
							double minDistCurr = Double.POSITIVE_INFINITY;
							int minDistCurrIndex = -1;
							double currDist;
							
							for (int m = 0; m < vecIndentSurface.size(); m++)
							{
								p1 = (Point)vecPossibleBorders.get(k);
								p2 = (Point)vecIndentSurface.get(m);
								
							
								if (pixelsConnectable(segMat, p1, p2, false))
								{
									currAngle = MiscMath.pointAngle(p1, p2);
									double angleDiff = Math.abs(currAngle - contourAngle);
									if (angleDiff > 180) //might be a rotation difference
										angleDiff -= 180;
									
									//System.err.println( "   ** Angle diff btw border pixel and hotspot contour pixel is " + angleDiff );
									// should be approx. 90 degree comp. to the point connecting hotspots
									if ( angleDiff >= (90 - ANGLE_INDENT_ORTHO_SLACK) && angleDiff <= (90 + ANGLE_INDENT_ORTHO_SLACK) ) 
									{
										//allow different angles but penalize the distance
										// (penalty is == 1 if angleDiff == 90 and decreases linearly with the difference from 90)
										double penalty = 1 - Math.abs(90 - angleDiff)/ANGLE_INDENT_ORTHO_SLACK;
										
										currDist = p1.distance(p2)*penalty;
										if (currDist > maxDist)
										{
											maxDist = currDist;
											maxDistIndex1 = k;
											maxDistIndex2 = m;
										}
									}
								}
							}
							
							
						
							
						}
						
						if (maxDistIndex1 >= 0)
						{
							Point[] pVecRet = new Point[2];
							pVecRet[0] = (Point)vecPossibleBorders.get(maxDistIndex1);
							pVecRet[1] = (Point)vecIndentSurface.get(maxDistIndex2);
							
							retVecCurr.add(pVecRet);
							
							//draw the minimum-distance line
							//p1 = (Point)vecCurrBorder.get(maxDistIndex1);
							//p2 = (Point)vecP.get(maxDistIndex2);
							//currAngle = MiscMath.pointAngle(p1, p2);
							//System.err.println("  ** Found suitable point, angle = " + currAngle + ", diff = " + Math.abs(currAngle - contourAngle) );
							//g.setColor(Color.ORANGE);
							//g.drawLine( (int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY() );

							
						} else
						{
							retVecCurr.add(null); //perhaps some other solution here
							
							//System.err.println("*** Indent too shallow to calculate depth for object #" + (i+1) + ", indent #" + j);
							
							//System.err.println("    contourAngle is " + contourAngle);
							//System.err.println("    vecIndentSurface is of length " + vecIndentSurface.size() + "..." );
							//System.err.println("    Start pixel: " + lastPoint);
							//System.err.println("    End pixel: " + currPoint);
							
							//extra debug
							/*
							for (int k = 0; k < vecPossibleBorders.size(); k++)
							{
								double minDistCurr = Double.POSITIVE_INFINITY;
								int minDistCurrIndex = -1;
								double currDist;
								
								for (int m = 0; m < vecIndentSurface.size(); m++)
								{
									p1 = (Point)vecPossibleBorders.get(k);
									p2 = (Point)vecIndentSurface.get(m);
									
								
									if (pixelsConnectable(segMat, p1, p2, false))
									{
										currAngle = MiscMath.pointAngle(p1, p2);
										double angleDiff = Math.abs(currAngle - contourAngle);
										if (angleDiff > 180) //might be a rotation difference
											angleDiff -= 180;
										
										System.err.println("    angleDiff=" + angleDiff);
									} else
									{
										currAngle = MiscMath.pointAngle(p1, p2);
										double angleDiff = Math.abs(currAngle - contourAngle);
										if (angleDiff > 180) //might be a rotation difference
											angleDiff -= 180;
										
										System.err.println("    [NONCONN] angleDiff=" + angleDiff);
									}
								}
							}
							*/
							
							
						}
						
					} else
					{
						System.err.println("*** No connectable pixels for #" + (i+1) + ", indent #" + j + " (indices " + lastInd + " --> " + currInd + ")");
						System.err.println("    Start pixel: " + lastPoint);
						System.err.println("    End pixel: " + currPoint);
						retVecCurr.add(null); //perhaps some other solution here
					}
				} else
				{
					//this can happen for 'cavities' totally surrounded by other object pixels
					System.err.println("Contour gap: Object #" + (i+1) +  ", contour hotspot " + j + " is NULL");
				}
				
				lastPoint = (currPoint == null) ? null : new Point( currPoint );
				if (currPoint != null)
					lastInd = currInd;
				
			}
			
			retVec.add(retVecCurr);
		}
		
	
		return retVec;
	}
	
	
	/**
	* Fetches the indent depths as connecting lines (Vector of Vector of Point[2]s)
	* 
	* @param	vecContourHotspotConnections	Vector of Vector of Point[2]s containing connections btw contour hotspot elements
	* @param	vecBorder	Vector of Vector of Points containing border elements for object
	* @param	segMat	Segmentation matrix, used to determine if two hotspot pixels are 'connectable'
	* @return	A Vector of Vector of Point[2]s describing connecting lines for the indents for each object
	*/
	public static Vector fetchIndentDepthsOld(Vector vecContourHotspotConnections, Vector vecBorder, int[][] segMat, JProgressBar pb)
	{
		if (pb != null)
			pb.setValue(0);
		
		double pbStepLength = 100.0/vecContourHotspotConnections.size();
		
		Vector retVec = new Vector();
		for (int i = 0; i < vecContourHotspotConnections.size(); i++)
		{
			Vector retVecCurr = new Vector();
			Vector vecCurrContourHotspotCenters = (Vector)vecContourHotspotConnections.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			
			
			
			//now find center btw two idents
			Vector vecBorderPixelIndentIDs = new Vector(vecCurrContourHotspotCenters.size()); //contains a mapping of border pixel --> indent id
			for (int j = 1; j < (vecCurrContourHotspotCenters.size()+1); j++)
			{
				pb.setValue( (int)Math.round( (100.0*j/(vecCurrContourHotspotCenters.size()-1))/vecContourHotspotConnections.size() + i*pbStepLength ) );
				
				// fetch the hotspot centers
				Point[] pVec = (Point[])vecCurrContourHotspotCenters.get(j);
				
				//calc the angle btw hotspots
				if (pVec != null && pVec[0] != null && pVec[1] != null)
				{
				
				
					double contourAngle = MiscMath.pointAngle(pVec[0], pVec[1]);
					
					//System.err.println("   --> Angle btw contour points is " + contourAngle);
					
					//this vector contains contour border points that are inside indent j (btw hotspot j and k)
					Vector vecP = GrayscaleImageEdit.pixelsConnectableAsVector(segMat, pVec[0], pVec[1], false);
					

					
					double maxDist = Double.NEGATIVE_INFINITY;
					double currAngle;
					int maxDistIndex1 = -1;
					int maxDistIndex2 = -1;
					Point p1, p2;
					
					for (int k = 0; k < vecCurrBorder.size(); k++)
					{
						double minDistCurr = Double.POSITIVE_INFINITY;
						int minDistCurrIndex = -1;
						double currDist;
						
						for (int m = 0; m < vecP.size(); m++)
						{
							p1 = (Point)vecCurrBorder.get(k);
							p2 = (Point)vecP.get(m);
							
						
							if (pixelsConnectable(segMat, p1, p2, false))
							{
								currAngle = MiscMath.pointAngle(p1, p2);
								double angleDiff = Math.abs(currAngle - contourAngle);
								if (angleDiff > 180) //might be a rotation difference
									angleDiff -= 180;
								
								//System.err.println( "   ** Angle diff btw border pixel and hotspot contour pixel is " + angleDiff );
								// should be approx. 90 degree comp. to the point connecting hotspots
								if ( angleDiff >= (90 - ANGLE_INDENT_ORTHO_SLACK) && angleDiff <= (90 + ANGLE_INDENT_ORTHO_SLACK) ) 
								{
									//allow different angles but penalize the distance
									// (penalty is == 1 if angleDiff == 90 and decreases linearly with the difference from 90)
									double penalty = 1 - Math.abs(90 - angleDiff)/ANGLE_INDENT_ORTHO_SLACK;
									
									currDist = p1.distance(p2)*penalty;
									if (currDist > maxDist)
									{
										maxDist = currDist;
										maxDistIndex1 = k;
										maxDistIndex2 = m;
									}
								}
							}
						}
						
						
					
						
					}
					
					if (maxDistIndex1 >= 0)
					{
						Point[] pVecRet = new Point[2];
						pVecRet[0] = (Point)vecCurrBorder.get(maxDistIndex1);
						pVecRet[1] = (Point)vecP.get(maxDistIndex2);
						
						retVecCurr.add(pVecRet);
						
						//draw the minimum-distance line
						//p1 = (Point)vecCurrBorder.get(maxDistIndex1);
						//p2 = (Point)vecP.get(maxDistIndex2);
						//currAngle = MiscMath.pointAngle(p1, p2);
						//System.err.println("  ** Found suitable point, angle = " + currAngle + ", diff = " + Math.abs(currAngle - contourAngle) );
						//g.setColor(Color.ORANGE);
						//g.drawLine( (int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY() );

						
					} else
					{
						System.err.println("Indent too shallow to calculate depth for object #" + (i+1) + ", indent #" + j);
						retVecCurr.add(null); //perhaps some other solution here
					}
				} else
				{
					//this can happen for 'cavities' totally surrounded by other object pixels
					System.err.println("ERROR: Object #" + (i+1) +  ", contour hotspot " + j + " is NULL");
				}
						
				
			}
			
			retVec.add(retVecCurr);
		}
		
	
		return retVec;
	}
	
	
	/**
	* Find longest line that is (approximately) perpendicular to another line, defined by an angle
	* 
	* @param	vecBorders	Vector of Points with candidate points
	* @param	templateAngle	The angle to match against
	* @return A Point[2] describing a line, forming a perpendicular ling
	*/
	public static Point[] findLongestPerpendicularLineExhaustive(Vector vecBorders, double templateAngle)
	{
		Point p1, p2;
		double currAngle = 0.0, angleDiff = 0.0, penalty = 0.0,
			currDist = 0.0, maxDist = Double.NEGATIVE_INFINITY;
		int maxDistIndex1 = -1, maxDistIndex2 = -1;
		
		for (int j = 0; j < (vecBorders.size()-1); j++)
		{
			p1 = (Point)vecBorders.get(j);
			for (int k = (j+1); k < vecBorders.size(); k++)
			{
				p2 = (Point)vecBorders.get(k);
				
				currAngle = MiscMath.pointAngle(p1, p2);
				angleDiff = Math.abs(currAngle - templateAngle);
				if (angleDiff > 180) //might be a rotation difference
					angleDiff -= 180;
									
				
				// should be approx. 90 degree comp. to the point connecting hotspots
				if ( angleDiff >= (90 - ANGLE_DIST_ORTHO_SLACK) && angleDiff <= (90 + ANGLE_DIST_ORTHO_SLACK) ) 
				{
					//allow different angles but penalize the distance
					// (penalty is == 1 if angleDiff == 90 and decreases linearly with the difference from 90)
					
					penalty = 1 - Math.abs(90 - angleDiff)/ANGLE_DIST_ORTHO_SLACK;
					
					currDist = p1.distance(p2)*penalty;
					if (currDist > maxDist)
					{
						maxDist = currDist;
						maxDistIndex1 = j;
						maxDistIndex2 = k;
					}
				}
			}
		}
				
		if (maxDistIndex1 >= 0 && maxDistIndex2 >= 0)
		{
		
			//here we have to recalc. the center/50% etc lines
			Point[] line = new Point[2];
			line[0] = new Point( (Point)vecBorders.get(maxDistIndex1) );
			line[1] = new Point( (Point)vecBorders.get(maxDistIndex2) );
			
			currAngle = MiscMath.pointAngle(line[0], line[1]);
			currDist = line[0].distance(line[1]);
			System.err.println("Longest line has angle " + currAngle + " and distance " + currDist);
			
			return line;
		} else
		{
			return null;
		}
	}
	
	
	/**
	* Paints the contour hotspots
	* 
	* @param	vecContourHotspots	Vector of Vector of Points containing connections btw contour hotspot elements
	* @param	g	Graphics object to paint to
	* @param	c	Color to paint
	*/
	public static void paintContourHotspots(Vector vecContourHotspots, Graphics g, Color c, boolean drawLines)
	{
		for (int i = 0; i < vecContourHotspots.size(); i++)
		{
			Vector vecCurrContourHotspots = (Vector)vecContourHotspots.get(i);
			g.setColor(c);
			
			Point prevPoint = (Point)vecCurrContourHotspots.get(0);
			Point currPoint = new Point(prevPoint);
			
			for (int j = 1; j < vecCurrContourHotspots.size(); j++)
			{
				currPoint  = (Point)vecCurrContourHotspots.get(j);
				
				
				if (prevPoint != null && currPoint != null)
				{
					if (!drawLines)
						g.drawLine( (int)prevPoint.getX(), (int)prevPoint.getY(), (int)prevPoint.getX(), (int)prevPoint.getY() );
					else
						g.drawLine( (int)prevPoint.getX(), (int)prevPoint.getY(), (int)currPoint.getX(), (int)currPoint.getY() );
					
				}
				prevPoint = (currPoint == null) ? null : new Point( currPoint );
			
			}
			
			//last entry
			if (prevPoint != null && currPoint != null)
			{
				if (!drawLines)
					g.drawLine( (int)prevPoint.getX(), (int)prevPoint.getY(), (int)prevPoint.getX(), (int)prevPoint.getY() );
				else
					g.drawLine( (int)prevPoint.getX(), (int)prevPoint.getY(), (int)currPoint.getX(), (int)currPoint.getY() );
				
			}
		
		}
	}
	
	/**
	* Paints the contour hotspots
	* 
	* @param	vecContourHotspots	Vector of Vector of Points containing connections btw contour hotspot elements
	* @param	g	Graphics object to paint to
	* @param	c	Color to paint
	*/
	public static void paintContourHotspotsCrosses(Vector vecContourHotspots, Graphics g, Color c, int crossLength)
	{
		for (int i = 0; i < vecContourHotspots.size(); i++)
		{
			Vector vecCurrContourHotspots = (Vector)vecContourHotspots.get(i);
			g.setColor(c);
			
			Point prevPoint = (Point)vecCurrContourHotspots.get(0);
			Point currPoint = new Point(prevPoint);
			
			for (int j = 1; j < vecCurrContourHotspots.size(); j++)
			{
				currPoint  = (Point)vecCurrContourHotspots.get(j);
				
				
				if (prevPoint != null && currPoint != null)
				{
					g.drawLine( (int)(prevPoint.getX()-crossLength), (int)(prevPoint.getY()-crossLength),
						(int)(prevPoint.getX()+crossLength), (int)(prevPoint.getY()+crossLength) );
					g.drawLine( (int)(prevPoint.getX()+crossLength), (int)(prevPoint.getY()-crossLength),
						(int)(prevPoint.getX()-crossLength), (int)(prevPoint.getY()+crossLength) );
					
				}
				prevPoint = (currPoint == null) ? null : new Point( currPoint );
			
			}
			
			//last entry
			if (prevPoint != null && currPoint != null)
			{
				g.drawLine( (int)(prevPoint.getX()-crossLength), (int)(prevPoint.getY()-crossLength),
					(int)(prevPoint.getX()+crossLength), (int)(prevPoint.getY()+crossLength) );
				g.drawLine( (int)(prevPoint.getX()+crossLength), (int)(prevPoint.getY()-crossLength),
					(int)(prevPoint.getX()-crossLength), (int)(prevPoint.getY()+crossLength) );	
			}
		
		}
	}
	
	
	/**
	* Paints the indent depths as connecting lines (Vector of Vector of Point[2]s)
	* 
	* @param	vecContourHotspots	Vector of Vector of Point[2]s containing connections btw contour hotspot elements
	* @param	g	Graphics object to paint to
	* @param	c	Color to paint
	*/
	public static void paintContourHotspotsOld(Vector vecContourHotspots, Graphics g, Color c)
	{
		for (int i = 0; i < vecContourHotspots.size(); i++)
		{
			Vector vecCurrContourHotspots = (Vector)vecContourHotspots.get(i);
			g.setColor(c);
			
			for (int j = 0; j < vecCurrContourHotspots.size(); j++)
			{
				Point[] pVec = (Point[])vecCurrContourHotspots.get(j);
				
				
				if (pVec != null && pVec[0] != null && pVec[1] != null)
				{
					g.drawLine( (int)pVec[0].getX(), (int)pVec[0].getY(), (int)pVec[0].getX(), (int)pVec[0].getY() );
					g.drawLine( (int)pVec[1].getX(), (int)pVec[1].getY(), (int)pVec[1].getX(), (int)pVec[1].getY() );
				}
			
			}
		
		}
	}
	
	
	
	/**
	* Paints the indent depths as connecting lines (Vector of Vector of Point[2]s)
	* 
	* @param	vecContourIndents	Vector of Vector of Point[2]s containing connections btw contour hotspot elements
	* @param	g	Graphics object to paint to
	* @param	c	Color to paint
	*/
	public static void paintContourIndentDepths(Vector vecContourIndents, Graphics g, Color c)
	{
		for (int i = 0; i < vecContourIndents.size(); i++)
		{
			Vector vecCurrContourIndents = (Vector)vecContourIndents.get(i);
			g.setColor(c);
			
			for (int j = 0; j < vecCurrContourIndents.size(); j++)
			{
				Point[] pVec = (Point[])vecCurrContourIndents.get(j);
				
				
				if (pVec instanceof Point[])
				{
					
					g.drawLine( (int)pVec[0].getX(), (int)pVec[0].getY(), (int)pVec[1].getX(), (int)pVec[1].getY() );
				}
			
			}
		
		}
	}
	
	/**
	* Shortens the border by removing (potential) inner borders, created from the presence of cavities
	* 
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	vecBorderBreakPoints	Vector of Vector of Integer indices, containing break points for the different border patches
	* @return	A int[][] matrix with the new segmentation objects, without 
	*/
	public static Vector shortenBorder(Vector vecBorder, Vector vecBorderBreakPoints)
	{
		Vector retVec = new Vector(vecBorder.size());
		
		//repeat for all objects
		for (int i = 0; i < vecBorder.size(); i++)
		{
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			Vector vecCurrBorderBreakPoints = (Vector)vecBorderBreakPoints.get(i);
			Vector vecNewBorder = new Vector(vecCurrBorder.size());
			
			//copy everything, ending at the first breakpoint (where the borders from inner cavities start, if any)
			int firstBreakPointInd = ( (Integer)vecCurrBorderBreakPoints.get(0)).intValue();
			for (int j = 0; j < firstBreakPointInd; j++)
			{
				vecNewBorder.add(vecCurrBorder.get(j));
			}
			
			retVec.add(vecNewBorder);
			
		}
		return retVec;
	}
	
	/**
	* Fills in the cavities in a segmentation image, by adding these pixels to a vector of pixels and to a int[][] matrix
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar, containing all the pixels of the object
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	vecBorderBreakPoints	Vector of Vector of Integer indices, containing break points for the different border patches
	* @param	segMat	Segmentation matrix
	* @return	A int[][] matrix with the new segmentation objects, without 
	*/
	public static Vector fillObjectCavities(Vector vec, Vector vecBorder, Vector vecBorderBreakPoints, int[][] segMat, JProgressBar pb)
	{		
		int dummyVal = -1;
		
		if (pb != null)
			pb.setValue(0);
		
		Vector retVec = new Vector(vec.size());
		
		//copy initial segmentation matrix so that we can modify it
		int[][] segMatCopy = new int[segMat.length][segMat[0].length];
		for (int x = 0; x < segMat[0].length; x++)
			for (int y = 0; y < segMat.length; y++)
				segMatCopy[y][x] = segMat[y][x];

		double pbStepLength = 100.0/vec.size();
				
		//repeat for all objects
		for (int i = 0; i < vec.size(); i++)
		{
			
			Vector vecCurr = (Vector)vec.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			Vector vecCurrBorderBreakPoints = (Vector)vecBorderBreakPoints.get(i);
			
			//copy a new vector, which will be edited and returned
			Vector vecCurrCopy = new Vector(vecCurr.size());
			for (int j = 0; j < vecCurr.size(); j++)
				vecCurrCopy.add( vecCurr.get(j) );

			
			if (vecCurrBorderBreakPoints.size() > 1)
			{
				//we have to loop through all border elements for each cavities and fill in the pixels in between
				
				
				//the procedure should mimick the one for finding the contour, just have to fix this function:
				// 1) Find all connectable pixels for each cavity
				// 2) Paint these on segMatCopy
				// 3) Fetch the pixels with index i from the int[][] matrix and store in a Vector
				// 4) Add these to vecCurr
				// 5) Add the new vecCurr to retVec
				int lastIndex = 0;
				int currIndex = -1;
				//System.err.println("Object " + (i+1) + ": Length of border pixels is " + vecCurrBorder.size() );
				
				
				for (int j = 0; j < vecCurrBorderBreakPoints.size(); j++)
				{
					if (pb != null)
						pb.setValue( (int)Math.round( (100.0*j/(double)(vecCurrBorderBreakPoints.size()-1))/vec.size() + i*pbStepLength));
					
					//find the pixels that are inside the segmentation object and give these intensities a value != 0
					currIndex = ( (Integer)vecCurrBorderBreakPoints.get(j)).intValue();
					
					//the first border part is ignored, since it is the 'outer' border, not any of the cavities
					//the first loop is only used to keep track of where the border breakpoints start/end
					if (j > 0)
					{
						//System.err.println("Object " + (i+1) + ": Now processing break point " + j + "/" + vecCurrBorderBreakPoints.size() + " with indices " + lastIndex + " --> " + currIndex);
						//finds contour and alters 'segMatCopy' directly
						GrayscaleImageEdit.findContourInd(segMatCopy, vecCurrBorder, (int)Math.min(lastIndex, currIndex), (int)Math.max(lastIndex, currIndex), dummyVal); //objectId should be i+1
					}
					
					lastIndex = currIndex+1;
				}
				
				//Convert int[][] matrix to Vector
				//g.setColor(Color.GREEN);
				for (int x = 0; x < segMatCopy[0].length; x++)
					for (int y = 0; y < segMatCopy.length; y++)
						if (segMatCopy[y][x] == dummyVal)
						{
							//g.drawLine(x,y,x,y);
							vecCurrCopy.add( new Point(x, y) );
							
							segMatCopy[y][x] = (i+1);
						}

				
				
			}
			
			retVec.add(vecCurrCopy);
			
		}
		
		//return segMatCopy;
		return retVec;
	}
	
	
	/**
	* Calculates various statistics for each segmentation object.
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar, containing all the pixels of the object
	* @param	vecNoCavities	Vector of Vector of Points from fetchSegObjCoord or similar, expanded with potential cavities
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	vecBorderShort Vector of Vector of Points containing only border elements, without any inner borders (for cavities)
	* @param	vecBorderBreakPoints	Vector of Vector of Integer containing indices for potential border break points (from cavities)
	* @param	vecHorizVertLines	Vector of Vector[2] (vertical/horizontal) of Vector of Point[2] containing start and end of the lines
	* @param	vecIntersectPoints	Vector of Point containing object intersections
	* @param	vecContourHotspotConnections	Vector of Vector of Points containing contour hotspot connections
	* @param	vecContourHotspotIndices	Vector of Vector of Integer containing the corresponding contour hotspot indices
	* @param	vecContourIndents	Vector of Vector of indents
	* @param	vecBorderShortLandmarks	Vector of Vector of landmark Points
	* @param	vecObjectCentersOrg	Vector of Points with object centers before cropping
	* @param	vecObjectCenters	Vector of Points with object centers after cropping
	* @param	scaleParam	the scale value to add for each pixel (e.g. for conversion
	*				from pixels to mm).
	* @return	A Vector of Vector with values
	*/
	public static Vector calcSegStats(Vector vec, Vector vecNoCavities, Vector vecBorder,
		Vector vecBorderShort, Vector vecBorderBreakPoints,
		Vector vecHorizVertLines, Vector vecIntersectPoints,
		Vector vecContourHotspotConnections, Vector vecContourHotspotIndices,
		Vector vecContourIndents, double scaleParam,
		Vector vecBorderShortLandmarks, Vector vecObjectCentersOrg, Vector vecObjectCenters,
		Graphics g)
	{
		int x,y;
		
		Vector retVec = new Vector(vec.size());
		
		// construct a header line for the output file
		String[] headerElem = {"Obj. id", "Inv. scale parameter", "Obj. center horiz.", "Obj. center vert.",
			"Num. cavities", "Area 1", "Area 2 (cavities filled)", "Perimeter 1", "Perimeter 2 (excl. cavities)",
			"Squared Perimeter 1/Area 1", "Squared Perimeter 2/Area 2", "Circularity",
			"Horiz. size center", "Horiz. size 25%", "Horiz. size 50%", "Horiz. size 75%",
			"Vert. size center", "Vert. size 25%", "Vert. size 50%", "Vert. size 75%",
			"Vert. size center / Horiz. size center", 
			"Horiz. size 25% / Horiz. size 75%", "Vert. size 25% / Vert. size 75%",
			"Num. Indents", "Indent width mean", "Indent width median", "Indent width SD",
			"Indent depth mean", "Indent depth median", "Indent depth SD",
			};
		
		String headerElemLandmarksTemplate = "Boundary coord. #";
		
		Vector vecHeaderElement = new Vector(headerElem.length);
		for (int j = 0; j < headerElem.length; j++)
			vecHeaderElement.add(headerElem[j]);
		//retVec.add(vecHeaderElement);
		
		//contour objects, potentially undefined
		boolean contourFound = (vecContourHotspotConnections != null && vecContourIndents != null);
		boolean landMarksFound = (vecBorderShortLandmarks != null && vecBorderShortLandmarks.size() > 0 &&
			vecObjectCenters != null && vecObjectCentersOrg != null && vecObjectCenters.size() > 0 &&
			vecObjectCentersOrg.size() > 0);
		
			
		for (int i = 0; i < vec.size(); i++)
		{
			
			Vector vecCurr = (Vector)vec.get(i);
			Vector vecCurrNoCavities = (Vector)vecNoCavities.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			Vector vecCurrBorderShort = (Vector)vecBorderShort.get(i);
			Vector vecCurrBorderBreakPoints = (Vector)vecBorderBreakPoints.get(i);
			
			//contains a vector of elements with results for this object
			Vector retVecCurr = new Vector(vecHeaderElement.size());
			retVecCurr.add( new Integer(i+1) ); //object id
			retVecCurr.add( new Double(scaleParam) ); //scale parameter
			
			//Vector vecCurrContour = new Vector();
			Vector vecCurrContourIndents = new Vector();
			Vector vecCurrCHSC = new Vector();
			Vector vecCurrCHSI = new Vector();

			if (contourFound)
			{
				//vecCurrContour = (Vector)vecContours.get(i);
				vecCurrContourIndents = (Vector)vecContourIndents.get(i);
				vecCurrCHSC = (Vector)vecContourHotspotConnections.get(i);
				vecCurrCHSI = (Vector)vecContourHotspotIndices.get(i);
			}
			
			Vector[] vecCurrHorizVertLines = (Vector[])vecHorizVertLines.get(i);
			//Point intersectCurr = (Point)vecIntersectPoints.get(i);
			
			Vector vecCurrHorizLines = vecCurrHorizVertLines[1];
			Vector vecCurrVertLines = vecCurrHorizVertLines[0];
			
			
			//retVecCurr.add( new Integer( (int)intersectCurr.getX() ) );
			//retVecCurr.add( new Integer( (int)intersectCurr.getY() ) );
			
			//add the real center, from the original image.
			Point2D.Double pCenterOrg = (Point2D.Double)vecObjectCentersOrg.get(i);
			retVecCurr.add( new Double( pCenterOrg.getX() ) );
			retVecCurr.add( new Double( pCenterOrg.getY() ) );
			
			//this is the relative center of the objects (they differ only if the image has been cropped)
			Point2D.Double pCenter = (Point2D.Double)vecObjectCenters.get(i);
			
			//System.err.println("Object " + (i+1) + " stats:");
			
			Point[] lineHorizCenter = (Point[])vecCurrHorizLines.get(0);
			Point[] lineVertCenter = (Point[])vecCurrVertLines.get(0);
			
			Point minX = lineHorizCenter[0];
			Point maxX = lineHorizCenter[1];
			Point minY = lineVertCenter[0];
			Point maxY = lineVertCenter[1];
			
			//fetch the line points from the input
			Point[] minDistHoriz25 = (Point[])vecCurrHorizLines.get(1);
			Point[] minDistHoriz50 = (Point[])vecCurrHorizLines.get(2);
			Point[] minDistHoriz75 = (Point[])vecCurrHorizLines.get(3);
			Point[] minDistVert25 = (Point[])vecCurrVertLines.get(1);
			Point[] minDistVert50 = (Point[])vecCurrVertLines.get(2);
			Point[] minDistVert75 = (Point[])vecCurrVertLines.get(3);
			
			
			
			double distHoriz = maxX.distance(minX)*scaleParam;
			double distVert = maxY.distance(minY)*scaleParam;
			//double lengthX = (maxX.getX()-minX.getX()+1)*scaleParam;
			//double lengthY = (maxY.getY()-minY.getY()+1)*scaleParam;
			//System.err.println("Width of object " + i + " is " + lengthX + ", height is " + lengthY + " and area is " +
			//	vecCurr.size()*scaleParam*scaleParam);
			
			//System.err.println("  X extremes at (" + minX.getX() + "," + minX.getY() + ") and (" + maxX.getX() + "," + maxX.getY() + ").");
			//System.err.println("  Y extremes at (" + minY.getX() + "," + minY.getY() + ") and (" + maxY.getX() + "," + maxY.getY() + ").");
			
			 //number of cavities
			retVecCurr.add( new Double(vecCurrBorderBreakPoints.size()-1) );
			
			//area does not include border pixels
			double area1 = (vecCurr.size()-vecCurrBorder.size())*scaleParam*scaleParam;
			double area2 = (vecCurrNoCavities.size()-vecCurrBorderShort.size())*scaleParam*scaleParam;
			//System.err.println("  Distance X: " + distX + ", Y: " + distY);
			//System.err.println("  Area: " + area );
			retVecCurr.add( new Double(area1) );
			retVecCurr.add( new Double(area2) );
			
			
			//simple implementation of perimeter
			double perim1 = vecCurrBorder.size()*scaleParam;
			double perim2 = vecCurrBorderShort.size()*scaleParam;
			
			/*
			//calc perimeter w/o cavities
			double perim2 = 0.0;
			Point p1, p2, p3;
			for (int j = 0; j < (vecCurrBorderShort.size()-1); j++)
			{
				p1 = (Point)vecCurrBorderShort.get(j);
				p2 = (Point)vecCurrBorderShort.get(j+1);
				perim2 += p1.distance(p2)*scaleParam;
			}
			p1 = (Point)vecCurrBorderShort.get(0);
			p2 = (Point)vecCurrBorderShort.get(vecCurrBorderShort.size()-1);
			perim2 += p1.distance(p2)*scaleParam;

			
			/*
			//calc perimeter w/ cavities (i.e. acknowledge the inner borders)
			double perim1 = 0.0;
			int lastIndex = 0;
			int currIndex = -1;
			//System.err.println("vecCurrBorder is of length " + vecCurrBorder.size() );
			for (int k = 0; k < vecCurrBorderBreakPoints.size(); k++)
			{

				currIndex = ( (Integer)vecCurrBorderBreakPoints.get(k)).intValue();
				
				//System.err.println("Processing indices " + lastIndex + " --> " + currIndex);
				
				for (int j = lastIndex; j < (currIndex-1); j++)
				{
					p1 = (Point)vecCurrBorder.get(j);
					p2 = (Point)vecCurrBorder.get(j+1);
					perim1 += p1.distance(p2)*scaleParam;
				}
				
				lastIndex = currIndex+1;
			}
			//add one final measure, but only if we don't have any cavities
			if ( vecCurrBorderBreakPoints.size() == 1)
			{
				p1 = (Point)vecCurrBorder.get(vecCurrBorder.size()-1);
				p2 = (Point)vecCurrBorder.get(0);
				perim1 += p1.distance(p2)*scaleParam;
			}
			*/
			
			
			//System.err.println("  Perim: " + perim);
			//System.err.println("  Perim^2/area: " + ( perim*perim/area ) );
			retVecCurr.add( new Double(perim1) );
			retVecCurr.add( new Double(perim2) );
			retVecCurr.add( new Double(perim1*perim1/area1) );
			retVecCurr.add( new Double(perim2*perim2/area2) );
			
			
			
			//double perim2 = vecCurrBorder.size()*scaleParam;
			//System.err.println("  Perim2: " + perim2);			
			//System.err.println("  Perim2^2/area: " + ( perim2*perim2/area ) );
			
			//calc circularity measure, by estimating the area of a circle with the same diameter as this object
			//area with cavities filled (area2) is used to measure circularity
			double distanceAverage = (distHoriz + distVert)/2.0;
			double areaCircle = Math.PI * distanceAverage*distanceAverage/4.0;
			double circularity = 100.0*Math.min(areaCircle, area2)/Math.max(areaCircle, area2);
			//System.err.println("  Circularity of object: " + circularity + "%");
			retVecCurr.add( new Double(circularity) );
			
			
			
			//System.err.println("  Horiz. lengths");
			//System.err.println("   - center: " + distHoriz );
			//System.err.println("   - 25%: " + minDistHoriz25[0].distance(minDistHoriz25[1])*scaleParam );
			//System.err.println("   - 50%: " + minDistHoriz50[0].distance(minDistHoriz50[1])*scaleParam );
			//System.err.println("   - 75%: " + minDistHoriz75[0].distance(minDistHoriz75[1])*scaleParam );
			//System.err.println("  Vert. lengths");
			//System.err.println("   - center: " + distVert );
			//System.err.println("   - 25%: " + minDistVert25[0].distance(minDistVert25[1])*scaleParam );
			//System.err.println("   - 50%: " + minDistVert50[0].distance(minDistVert50[1])*scaleParam );
			//System.err.println("   - 75%: " + minDistVert75[0].distance(minDistVert75[1])*scaleParam );
			
			retVecCurr.add( new Double(distHoriz) );
			retVecCurr.add( new Double(minDistHoriz25[0].distance(minDistHoriz25[1])*scaleParam) );
			retVecCurr.add( new Double(minDistHoriz50[0].distance(minDistHoriz50[1])*scaleParam) );
			retVecCurr.add( new Double(minDistHoriz75[0].distance(minDistHoriz75[1])*scaleParam) );
			retVecCurr.add( new Double(distVert) );
			retVecCurr.add( new Double(minDistVert25[0].distance(minDistVert25[1])*scaleParam) );
			retVecCurr.add( new Double(minDistVert50[0].distance(minDistVert50[1])*scaleParam) );
			retVecCurr.add( new Double(minDistVert75[0].distance(minDistVert75[1])*scaleParam) );
			
			
			double ratioVertHorizCenter = distVert/distHoriz;
			double ratioHoriz25over75 = minDistHoriz25[0].distance(minDistHoriz25[1]) / minDistHoriz75[0].distance(minDistHoriz75[1]);
			double ratioVert25over75 = minDistVert25[0].distance(minDistVert25[1]) / minDistVert75[0].distance(minDistVert75[1]);
			
			retVecCurr.add( new Double(ratioVertHorizCenter) );
			retVecCurr.add( new Double(ratioHoriz25over75) );
			retVecCurr.add( new Double(ratioVert25over75) );
			
			//Point intersect = new Point((int)intersectX, (int)intersectY);
			//System.err.println("  Intersect: (" + intersectCurr.getX() + "," + intersectCurr.getY() + ")");
			
			// H25 == Horizontal line at 25% from top
			// H50 == Horizontal line at 50% from top
			// H75 == Horizontal line at 75% from top
			// HWT == Half Way Total (the half-way between Ymax and Ymin)
			// HWI == Half Way Intercept (the half-way between Ymax and the Y intersection (where the X/Y lines cross)
			
			if (contourFound)
			{
				int numIndents = vecCurrCHSC.size();
				//System.err.println("  Indents: " + numIndents);
				
				
				//calculate average distance between indents
				//Point p1, p2;  //defined elsewhere
				Vector v1, v2;
				double distTotGlobal = 0.0;
				
				
				
				//calculate distances between the connecting points
				//int numHotspots = 0;
				Vector vecIndentWidths = new Vector( vecCurrCHSC.size() );
				
				Point lastPoint = (Point)vecCurrCHSC.get(0);
				Point currPoint = lastPoint;
				int lastInd = ( (Integer)vecCurrCHSI.get(0)).intValue();
				int currInd = lastInd;
				
				for (int j = 1; j < vecCurrCHSC.size(); j++)
				{
					// compare this indent to the neighboring indent (counterclock-wise)
					currPoint = (Point)vecCurrCHSC.get(j);
					
					
					if (currPoint != null && lastPoint != null)
					{
						currInd = ( (Integer)vecCurrCHSI.get(j)).intValue();
						
						double distAv = lastPoint.distance(currPoint)*scaleParam;
						vecIndentWidths.add( new Double(distAv) );
						
						//distTotGlobal += distAv;
						
						//numHotspots++;
					}
					
					lastPoint = (currPoint == null) ? null : new Point( currPoint );
					if (currPoint != null)
						lastInd = currInd;
					//g.setColor(Color.WHITE);
					//g.drawLine( (int)pVec[0].getX(), (int)pVec[0].getY(), (int)pVec[1].getX(), (int)pVec[1].getY() );

				}
				//convert vector to array and calculate various statistics
				
				//for (int j = 0; j < vecIndentWidths.size(); j++)
				//	indentWidths[j] = ( (Double)vecIndentWidths.get(j) ).doubleValue();
				
				
				//System.err.println("  Average distance between indents: " + distAvGlobal);
				
				
				int numIndentsWithDepth = 0;
				Vector indentDepths = new Vector(vecCurrContourIndents.size() );
				Vector indentWidths = new Vector(vecCurrContourIndents.size() );
				for (int j = 0; j < vecCurrContourIndents.size(); j++)
				{
					Point[] pVec = (Point[])vecCurrContourIndents.get(j);
					boolean indentOK = false;
					double depthAv = 0.0;
					if (pVec != null && pVec[0] != null && pVec[1] != null)
					{
						depthAv = pVec[0].distance(pVec[1]);
						indentOK =  (depthAv > INDENT_DEPTH_THRESH);
					}
							
					if (indentOK)
					{
						depthAv *= scaleParam;
						
						//depthTotGlobal += depthAv;
						indentWidths.add( (Double)vecIndentWidths.get(j)  );
						indentDepths.add( new Double(depthAv) );
						
						numIndentsWithDepth++;
					}
				}
				
				double widthMeanGlobal = Double.NaN;
				double widthMedianGlobal = Double.NaN;
				double widthStdevGlobal = Double.NaN;
			
				
				double depthMeanGlobal = Double.NaN;
				double depthMedianGlobal = Double.NaN;
				double depthStdevGlobal = Double.NaN;
				
				//only count indents with an accurate depth
				if (numIndentsWithDepth > 0)
				{
					widthMeanGlobal = MiscMath.mean(indentWidths);
					widthMedianGlobal = MiscMath.median(indentWidths);
					widthStdevGlobal = MiscMath.stdev(indentWidths, widthMeanGlobal);

					depthMeanGlobal = MiscMath.mean(indentDepths);
					depthMedianGlobal = MiscMath.median(indentDepths);
					depthStdevGlobal = MiscMath.stdev(indentDepths, depthMeanGlobal);
				}
				
				//System.err.println("  Average depth of indents: " + depthAvGlobal);
				retVecCurr.add( new Integer(numIndentsWithDepth) );
				
				retVecCurr.add( new Double(widthMeanGlobal) );
				retVecCurr.add( new Double(widthMedianGlobal) );
				retVecCurr.add( new Double(widthStdevGlobal) );
				
				retVecCurr.add( new Double(depthMeanGlobal) );
				retVecCurr.add( new Double(depthMedianGlobal) );
				retVecCurr.add( new Double(depthStdevGlobal) );
				
			} else
			{
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				retVecCurr.add( new Double(Double.NaN) );
				
			}
			
			
			if (landMarksFound)
			{
				Vector vecCurrLandmarks = (Vector)vecBorderShortLandmarks.get(i);
				int lengthOutputStr = ("" + vecCurrLandmarks.size()).length();
				
				// add header elements
				if (i == 0)
				{
					for (int j = 0; j < vecCurrLandmarks.size(); j++)
					{
						vecHeaderElement.add(headerElemLandmarksTemplate + Misc.padLeadingZeros(j+1, lengthOutputStr) + " X");
						vecHeaderElement.add(headerElemLandmarksTemplate + Misc.padLeadingZeros(j+1, lengthOutputStr) + " Y");
					}
				}
					
				Point p;
				double xDiff, yDiff;
				for (int j = 0; j < vecCurrLandmarks.size(); j++)
				{
					p = (Point)vecCurrLandmarks.get(j);
					xDiff = (p.getX() - pCenter.getX())*scaleParam;
					yDiff = (p.getY() - pCenter.getY())*scaleParam;
					retVecCurr.add( new Double( xDiff ) );
					retVecCurr.add( new Double( yDiff ) );
				}
			}
			
			
			/*
			//center point of obj.
			//absolute center of object
			Point centerPoint = new Point( (int)( minX.getX() + (maxX.getX() - minX.getX())/2.0),
				(int)( minY.getY() + (maxY.getY() - minY.getY())/2.0) );
			
			//calculate all distances from the center point
			double[] allDists = new double[vecCurrBorder.size()];
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
				allDists[j] = centerPoint.distance( (Point)vecCurrBorder.get(j) );
			}
			double distThresh = MiscMath.quantile(allDists, 0.50);
			*/
			
			//byte direction = GrayscaleImageEdit.DIRECTION_NONE;
			//byte prevDirection = direction;
			//double prevDist = centerPoint.distance( prevPoint );
			//double currDist = 0.0;
			
			if (i == 0)
				retVec.add(vecHeaderElement);
				
			retVec.add(retVecCurr);
		}
		
		return retVec;
	}
	
	/**
	* Finds the centroid of an object
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar
	* @return	A Vector of Point2D.double with centroids
	*/
	public static Vector findObjectCentroids(Vector vec)
	{
		Vector retVec = new Vector(vec.size());
		
		for (int i = 0; i < vec.size(); i++)
		{
			//first store all the coordinates (x and y) in two separate arrays...
			Vector currVec = (Vector)vec.get(i);
			int[] xVec = new int[ currVec.size() ];
			int[] yVec = new int[ currVec.size() ];
			Point p;
			
			for (int j = 0; j < currVec.size(); j++)
			{
				p = (Point)currVec.get(j);
				xVec[j] = (int)p.getX();
				yVec[j] = (int)p.getY();
			}
			
			//... and the calculate the median value of each ( = object centroid)
			double xMed = MiscMath.median(xVec);
			double yMed = MiscMath.median(yVec);
			Point2D.Double retPoint = new Point2D.Double(xMed, yMed);
			
			System.err.println("Centroid of object #" + (i+1) + " is " + retPoint);
			
			retVec.add(retPoint);
		}
		
		return retVec;
	}
	
	/**
	*Paints the result from segmentation onto a Graphics object.
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar
	* @param	vecNoCavities	Vector of Vector of Points, similar to vec but extended with (potential) cavities filled in
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	vecBorderBreakPoints	Vector of Vector of Integer with border break point indices
	* @param	vecHorizVertLines	Vector of Vector[2] (vertical/horizontal) of Vector of Point[2] containing start and end of the lines
	* @param	vecIntersectPoints	Vector of Point containing object intersections
	*/
	public static void paintSegmentationResults(Vector vec, Vector vecNoCavities, Vector vecObjCenters, Vector vecBorder, Vector vecBorderBreakPoints, Vector vecHorizVertLines, Vector vecIntersectPoints, Graphics g2d)
	{
	
		for (int i = 0; i < vec.size(); i++)
		{
			
			Vector vecCurr = (Vector)vec.get(i);
			Vector vecCurrNoCavities = (Vector)vec.get(i);
			if (vecNoCavities != null)
				vecCurrNoCavities = (Vector)vecNoCavities.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			Vector[] vecCurrHorizVertLines = (Vector[])vecHorizVertLines.get(i);
			
			boolean hasBorderBreakPoints = false;
			Vector vecCurrBorderBreakPoints = new Vector();
			if (vecBorderBreakPoints != null)
			{
				vecCurrBorderBreakPoints = (Vector)vecBorderBreakPoints.get(i);
				hasBorderBreakPoints = true;
			}
			
			//Point intersectCurr = (Point)vecIntersectPoints.get(i);
			Point2D.Double centerPoint = (Point2D.Double)vecObjCenters.get(i);
			Vector vecCurrHorizLines = vecCurrHorizVertLines[0];
			Vector vecCurrVertLines = vecCurrHorizVertLines[1];
			
			
			//System.err.println("Object " + (i+1) + " stats:");
			
			Point[] lineHorizCenter = (Point[])vecCurrHorizLines.get(0);
			Point[] lineVertCenter = (Point[])vecCurrVertLines.get(0);
			
			Point minX = lineHorizCenter[0];
			Point maxX = lineHorizCenter[1];
			Point minY = lineVertCenter[0];
			Point maxY = lineVertCenter[1];
			
			
			// H25 == Horizontal line at 25% from top
			// H50 == Horizontal line at 50% from top
			// H75 == Horizontal line at 75% from top
			// HWT == Half Way Total (the half-way between Ymax and Ymin)
			// HWI == Half Way Intercept (the half-way between Ymax and the Y intersection (where the X/Y lines cross)
			
			//fetch the line points from the input
			Point[] minDistHoriz25 = (Point[])vecCurrHorizLines.get(1);
			Point[] minDistHoriz50 = (Point[])vecCurrHorizLines.get(2);
			Point[] minDistHoriz75 = (Point[])vecCurrHorizLines.get(3);
			Point[] minDistVert25 = (Point[])vecCurrVertLines.get(1);
			Point[] minDistVert50 = (Point[])vecCurrVertLines.get(2);
			Point[] minDistVert75 = (Point[])vecCurrVertLines.get(3);
		
			int x,y;
			
			//draw the border
			g2d.setColor( Color.YELLOW );
			int lastIndex = 0;
			int currIndex = -1;
			
			if (hasBorderBreakPoints)
			{
				for (int k = 0; k < vecCurrBorderBreakPoints.size(); k++)
				{
					currIndex = ( (Integer)vecCurrBorderBreakPoints.get(k)).intValue();
					
					for (int j = lastIndex; j < currIndex; j++)
					{
						Point p = (Point)vecCurrBorder.get(j);
						x = (int)p.getX();
						y = (int)p.getY();
						
						//draw the 'Border' elements
						g2d.drawLine( x, y, x, y);
					}
					
					//the next elements come from interior borders, mark these differently
					g2d.setColor( Color.WHITE );
					lastIndex = currIndex;
				}
			} else
			{
				for (int j = 0; j < vecCurrBorder.size(); j++)
				{
					Point p = (Point)vecCurrBorder.get(j);
					x = (int)p.getX();
					y = (int)p.getY();
					
					//draw the 'Border' elements
					g2d.drawLine( x, y, x, y);
				}
					
			}
			
			// There may be additional elements that come from cavities that are filled in,
			// mark them with a special color
			
			if (vecCurrNoCavities.size() > vecCurr.size() )
			{
				g2d.setColor( Color.GREEN );
			
				for (int j = (vecCurr.size()-1); j < vecCurrNoCavities.size(); j++)
				{
					Point p = (Point)vecCurrNoCavities.get(j);
					x = (int)p.getX();
					y = (int)p.getY();
					g2d.drawLine(x,y,x,y);
				}
			}
			
			
			
			
			//absolute center of object
			//Point centerPoint = new Point( (int)( minX.getX() + (maxX.getX() - minX.getX())/2.0),
			//	(int)( minY.getY() + (maxY.getY() - minY.getY())/2.0) );
			
			//now try to trace the indents, by measuring how the distance to the 
			// center (centerPointX, centerPointY) differs
			// !! Assumes that the border pixels are sorted by neighbors
			/*
			Point prevPoint = (Point)vecCurrBorder.get(0);
			
			g2d.setColor( Color.WHITE );
			int offsetX = (int)Math.max(centerPoint.getX() - vecCurrBorder.size()/2.0, 0.0);
			int offsetY = (int)(minY.getY() - 200);
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
				double currDist = centerPoint.distance( (Point)vecCurrBorder.get(j) );
				g2d.drawLine( (int)(offsetX+j),  (int)(offsetY+currDist),
					 (int)(offsetX+j),  (int)(offsetY+currDist) );
			
			}
			//calculate all distances from the center point
			double[] allDists = new double[vecCurrBorder.size()];
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
				allDists[j] = centerPoint.distance( (Point)vecCurrBorder.get(j) );
			}
			double distThresh = MiscMath.quantile(allDists, 0.50);
			
			byte direction = GrayscaleImageEdit.DIRECTION_NONE;
			byte prevDirection = direction;
			double prevDist = centerPoint.distance( prevPoint );
			double currDist = 0.0;
			*/
			
			
			
			/*
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
				Point currPoint = (Point)vecCurrBorder.get(j);
				currDist = centerPoint.distance(currPoint);
				
				if (currDist > distThresh)
				{
					g2d.setColor( Color.WHITE );
				} else
				{
					g2d.setColor( Color.YELLOW );
				}
			*/
			
				/*
				if ( Math.abs( currDist - prevDist ) < GrayscaleImageEdit.EPS)
				{
					g2d.setColor( Color.CYAN );
				} else if (currDist > prevDist)
				{
					g2d.setColor( Color.WHITE );
				} else if (currDist < prevDist)
				{
					g2d.setColor( new Color(100,100,100) );
				}
				*/
				
				/*
				if (currPoint.getX() > prevPoint.getX() && currPoint.getY() > prevPoint.getY() )
				{
					//we're moving south-east direction
					g2d.setColor( Color.RED );
					direction = GrayscaleImageEdit.DIRECTION_SE;
					
				} else if (currPoint.getX() < prevPoint.getX() && currPoint.getY() > prevPoint.getY() )
				{
					//we're moving south-west direction
					g2d.setColor( new Color(125, 0, 0) );
					direction = GrayscaleImageEdit.DIRECTION_SW;
					
				} else if (currPoint.getX() < prevPoint.getX() && currPoint.getY() < prevPoint.getY() )
				{
					//we're moving north-west direction
					g2d.setColor( Color.WHITE );
					direction = GrayscaleImageEdit.DIRECTION_NW;
				} else if (currPoint.getX() > prevPoint.getX() && currPoint.getY() < prevPoint.getY() )
				{
					//we're moving north-east direction
					g2d.setColor( new Color(125, 125, 125) );
					direction = GrayscaleImageEdit.DIRECTION_NE;
				} else
				{
					//horizontal direction
					direction = prevDirection;
					//g2d.setColor( Color.YELLOW );
				}
				prevPoint = new Point( (int)currPoint.getX(), (int)currPoint.getY() );
				prevDirection = direction;
				*/
			/*	
				prevDist = currDist;
				
				g2d.drawLine( (int)currPoint.getX(),  (int)currPoint.getY(), (int)currPoint.getX(),  (int)currPoint.getY());
				
				
			}
			*/
			
			//try to find a connection between two points and draw a line connecting them
			/*
			Random rand = new Random();
			for (int j = 0; j < 10; j++)
			{
				int ind1 = rand.nextInt( vecCurrBorder.size() );
				int ind2 = rand.nextInt( vecCurrBorder.size() );
				
				//System.err.println("Randomized samples " + ind1 + " and " + ind2 + "...");
				
				Point p1 = (Point)vecCurrBorder.get(ind1);
				Point p2 = (Point)vecCurrBorder.get(ind2);
				
				
				int startX, stopX, startY, stopY, deltaX, deltaY;
				
				if (p1.getX() < p2.getX() )
				{
					startX = (int)p1.getX();
					stopX = (int)p2.getX();
					startY = (int)p1.getY();
					stopY = (int)p2.getY();
					
				} else
				{
					startX = (int)p2.getX();
					stopX = (int)p1.getX();
					startY = (int)p2.getY();
					stopY = (int)p1.getY();
				}
				deltaX = startX - stopX;
				deltaY = startY - stopY;
				
				boolean slopeMissing = (stopX == startX);
				boolean slopeZero = (stopY == startY);
				double slope = 0.0, intercept = 0.0;
				if (!slopeMissing && !slopeZero)
				{
					slope = (double)(stopY - startY)/(double)(stopX - startX);
					intercept = -slope*stopX + stopY;
					//System.err.println("Rand. line with slope " + slope + " and intercept " + intercept + "...");
				}
				
				g2d.setColor( Color.GREEN );
				if ( (slopeMissing && !slopeZero) || Math.abs(deltaY) > Math.abs(deltaX) )
				{
					//vertical line
					int xx = startX;
					for (int yy = (int)(Math.min(startY, stopY)+1); yy < (int)Math.max(startY, stopY); yy++)
					{
						if (slopeMissing)
							xx = startX;
						else
							xx = (int)Math.round((yy - intercept)/slope);
						
						g2d.drawLine(xx, yy, xx, yy);
					}
					
				} else
				{
					int yy = 0;
					for (int xx = (startX+1); xx < stopX; xx++)
					{
						if (slopeZero) //horizontal line
							yy = startY;
						else
							yy = (int)Math.round( slope*xx + intercept);
						
						g2d.drawLine(xx, yy, xx, yy);
					}
				}
		
			
			}
			
			*/
			/*
			g2d.setColor( Color.WHITE );
			int offset2 = 20;
			for (int j = 0; j < vecCurrBorder.size(); j++)
			{
					if ( (j % 10) == 0)
					{
						Point p = (Point)vecCurrBorder.get(j);
						double deltaX = (int)(p.getX() - intersect.getX() );
						double deltaY = (int)(p.getY() - intersect.getY() );
						double slope = (deltaY/deltaX);
						double intercept = -slope*p.getX() + p.getY();
						
						Point newP = new Point( (int)Math.abs(p.getX()-offset2), (int)(slope*Math.abs(p.getX()-offset2) + intercept) );
						g2d.drawLine( (int)p.getX(), (int)p.getY(), (int)newP.getX(), (int)newP.getY() );
					}
			}
			*/
			
			
			
			
			// Draw the lines denoting the leaf area/distances
			g2d.setColor( Color.PINK );
			g2d.drawLine( (int)minX.getX(), (int)minX.getY(), (int)maxX.getX(), (int)maxX.getY() );
			//g2d.setColor( Color.BLUE );
			g2d.drawLine( (int)minY.getX(), (int)minY.getY(), (int)maxY.getX(), (int)maxY.getY() );
			
			//g2d.drawLine( (int)(minX.getX()-HWTOffsetX), (int)(minX.getY()-HWTOffsetY),
			//	(int)(maxX.getX()-HWTOffsetX), (int)(maxX.getY()-HWTOffsetY) );

			//Point centerPoint = new Point( (int)( Math.min(minX.getX(), maxX.getX()) + ( Math.max(minX.getX(), maxX.getX()) - Math.min(minX.getX(), maxX.getX()) )/2.0 ),
			//(int)( Math.min(minY.getY(), maxY.getY()) + ( Math.max(minY.getY(), maxY.getY()) - Math.min(minY.getY(), maxY.getY()) )/2.0 ) );
			
			// Draw the intersection point using an 'X-mark'
			/*
			g2d.setColor( Color.BLUE );
			int offset = 5;
			g2d.drawLine( (int)Math.round(intersectCurr.getX()-offset), (int)Math.round(intersectCurr.getY()-offset),
				(int)Math.round(intersectCurr.getX()+offset), (int)Math.round(intersectCurr.getY()+offset) );
			g2d.drawLine( (int)Math.round(intersectCurr.getX()+offset), (int)Math.round(intersectCurr.getY()-offset),
				(int)Math.round(intersectCurr.getX()-offset), (int)Math.round(intersectCurr.getY()+offset) );
			*/
			
			//draw all the other lines
			g2d.setColor( Color.CYAN );
			g2d.drawLine( (int)minDistHoriz25[0].getX(), (int)minDistHoriz25[0].getY(),
				(int)minDistHoriz25[1].getX(), (int)minDistHoriz25[1].getY());
			g2d.drawLine( (int)minDistHoriz50[0].getX(), (int)minDistHoriz50[0].getY(),
				(int)minDistHoriz50[1].getX(), (int)minDistHoriz50[1].getY());
			g2d.drawLine( (int)minDistHoriz75[0].getX(), (int)minDistHoriz75[0].getY(),
				(int)minDistHoriz75[1].getX(), (int)minDistHoriz75[1].getY());
			
			g2d.setColor( Color.CYAN );
			g2d.drawLine( (int)minDistVert25[0].getX(), (int)minDistVert25[0].getY(),
				(int)minDistVert25[1].getX(), (int)minDistVert25[1].getY());
			g2d.drawLine( (int)minDistVert50[0].getX(), (int)minDistVert50[0].getY(),
				(int)minDistVert50[1].getX(), (int)minDistVert50[1].getY());
			g2d.drawLine( (int)minDistVert75[0].getX(), (int)minDistVert75[0].getY(),
				(int)minDistVert75[1].getX(), (int)minDistVert75[1].getY());
			
			/*
			// Show where the HWT points are
			g2d.setColor( Color.CYAN );
			g2d.drawLine( (int)HWTMinDistPointLeft.getX(), (int)HWTMinDistPointLeft.getY(),
				(int)HWTMinDistPointRight.getX(), (int)HWTMinDistPointRight.getY());
			
			// Show where the HWI points are
			g2d.setColor( Color.GREEN );
			g2d.drawLine( (int)HWIMinDistPointLeft.getX(), (int)HWIMinDistPointLeft.getY(),
				(int)HWIMinDistPointRight.getX(), (int)HWIMinDistPointRight.getY() );
			*/
			
		
			/*
			g2d.setColor( Color.WHITE );
			for (int yy = (int)minY.getY(); yy < (int)maxY.getY(); yy++)
			{
				int xx = (slopeMissingY) ? (int)interceptY : (int)((yy - interceptY)/slopeY);
				g2d.drawLine( xx, yy, xx, yy);
			}
			*/
			

		}
		
	}
	
	/**
	* Paints the number of the graphic object
	* 
	* @param	vecObjCenters	Vector of Points with object centers
	* @param	g2d	Graphic2D object, where the object ids are painted
	*/
	public static void paintObjectIds(Vector vecObjCenters, Graphics g2d)
	{
		for (int i = 0; i < vecObjCenters.size(); i++)
		{
			Point2D.Double centerPoint = (Point2D.Double)vecObjCenters.get(i);
		
			g2d.setFont( new Font("Times New Roman", Font.BOLD, 36) );
			g2d.setColor( Color.WHITE );
			g2d.drawString("#" + (i+1), (int)centerPoint.getX(), (int)centerPoint.getY());
		}
	}
	
	
	
	/**
	* Construct a reconstructed version of a int[][] matrix where the 
	* regions of interests are defined by a Vector of Rectangles.
	* 
	* @param  dims	A Vector of Rectangles containing the regions to crop
	* @param  img	The image which is to be cropped
	* @return	The cropped image
	*/
	public static int[][] cropMatrix(int[][] img, Vector dims)
	{
		return cropMatrix(img, dims, false);
	}
	
	/**
	* Construct a reconstructed version of a int[][] matrix where the 
	* regions of interests are defined by a Vector of Rectangles.
	* 
	* @param  dims	A Vector of Rectangles containing the regions to crop
	* @param  img	The image which is to be cropped
	* @param	keepOtherObjectIds	If true, several object ids are allowed in one crop area. If not, only the expected object id will be retained (default)
	* @return	The cropped image
	*/
	public static int[][] cropMatrix(int[][] img, Vector dims, boolean keepOtherObjectIds)
	{
		//BufferedImage imgCropped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		//WritableRaster wrCropped = imgCropped.getRaster();	
		//Raster raster = img.getData();
		
		int imgWidthOrg = img[0].length;
		int imgHeightOrg = img.length;
		
		
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
		
		System.err.println("Crop: Original image is " + imgWidthOrg + " x " + imgHeightOrg + " ...");
		System.err.println("Crop: New image is " + imgWidth + " x " + imgHeight + " ...");

		//now we try to merge to images into one single image
		int[][] newImage = new int[ imgHeight ][ imgWidth ];
		int xOffset = 0;
		for (int i = 0; i < dims.size(); i++)
		{
			int objId = (i+1);
			rect = (Rectangle)dims.get(i);
			int minX = (int)Math.max(rect.getLocation().getX(), 0	);
			int maxX = (int)Math.min(minX+rect.getWidth(), imgWidthOrg-1 );
			int minY = (int)Math.max(rect.getLocation().getY(), 0);
			int maxY = (int)Math.min(minY+rect.getHeight(), imgHeightOrg-1 );
			
			System.err.println("Cropping image from (" + minX + "," + minY + ") to (" + maxX + "," + maxY + ")");
			
			int xOld, yOld, xNew, yNew;
			for (int xx = 0; xx < (maxX-minX); xx++)
			{
				xOld = minX + xx;
				xNew = xOffset + xx;
				for (int yy = 0; yy < (maxY - minY); yy++)
				{
					yOld = minY + yy;
					yNew = yy;
					
					if (keepOtherObjectIds || img[yOld][xOld] == objId)
					{
						//System.err.println("Setting cropped frame to 0 at (" + xx + "," + yy + ")...");
						newImage[yNew][xNew] = img[yOld][xOld];
					} else
					{
						newImage[yNew][xNew] = 0;
					}
					
				}
			}
				
			xOffset += rect.getWidth();
			
		}
			
		
		//return (PlanarImage.wrapRenderedImage(imgComposite));
		return (newImage);
	}	

	/**
	*Paints the result from segmentation onto a Graphics object.
	* 
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	c	A color
	* @param	g2d	A graphics object
	*/
	public static void paintBorders(Vector vecBorder, Color c, Graphics g2d)
	{
		g2d.setColor(c);
		for (int i = 0; i < vecBorder.size(); i++)
		{
			Vector vecCurr = (Vector)vecBorder.get(i);
			Point p;
			int x,y;
			
			for (int j = 0; j < vecCurr.size(); j++)
			{
				p = (Point)vecCurr.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				//draw the 'Border' elements
				g2d.drawLine( x, y, x, y);	
			}
		}
	}
	
	
	/**
	*Paints the result from segmentation onto a Graphics object.
	* 
	* @param	vec	Vector of Vector of Points from fetchSegObjCoord or similar
	* @param	vecNoCavities	Vector of Vector of Points, similar to vec but extended with (potential) cavities filled in
	* @param	vecBorder Vector of Vector of Points containing only border elements
	* @param	vecBorderBreakPoints	Vector of Vector of Integer with border break point indices
	*/
	public static void paintBordersAndCavities(Vector vec, Vector vecNoCavities, Vector vecBorder, Vector vecBorderBreakPoints, Graphics g2d)
	{
	
		for (int i = 0; i < vec.size(); i++)
		{
			
			Vector vecCurr = (Vector)vec.get(i);
			Vector vecCurrNoCavities = (Vector)vec.get(i);
			if (vecNoCavities != null)
				vecCurrNoCavities = (Vector)vecNoCavities.get(i);
			Vector vecCurrBorder = (Vector)vecBorder.get(i);
			
			boolean hasBorderBreakPoints = false;
			Vector vecCurrBorderBreakPoints = new Vector();
			if (vecBorderBreakPoints != null)
			{
				vecCurrBorderBreakPoints = (Vector)vecBorderBreakPoints.get(i);
				hasBorderBreakPoints = true;
			}
			
			
			
			
			//System.err.println("Object " + (i+1) + " stats:");
			
			int x,y;
			
			//draw the border
			g2d.setColor( Color.YELLOW );
			int lastIndex = 0;
			int currIndex = -1;
			
			if (hasBorderBreakPoints)
			{
				for (int k = 0; k < vecCurrBorderBreakPoints.size(); k++)
				{
					currIndex = ( (Integer)vecCurrBorderBreakPoints.get(k)).intValue();
					
					for (int j = lastIndex; j < currIndex; j++)
					{
						Point p = (Point)vecCurrBorder.get(j);
						x = (int)p.getX();
						y = (int)p.getY();
						
						//draw the 'Border' elements
						g2d.drawLine( x, y, x, y);
					}
					
					//the next elements come from interior borders, mark these differently
					g2d.setColor( Color.WHITE );
					lastIndex = currIndex;
				}
			} else
			{
				for (int j = 0; j < vecCurrBorder.size(); j++)
				{
					Point p = (Point)vecCurrBorder.get(j);
					x = (int)p.getX();
					y = (int)p.getY();
					
					//draw the 'Border' elements
					g2d.drawLine( x, y, x, y);
				}
					
			}
			
			// There may be additional elements that come from cavities that are filled in,
			// mark them with a special color
			
			if (vecCurrNoCavities.size() > vecCurr.size() )
			{
				g2d.setColor( Color.GREEN );
			
				for (int j = (vecCurr.size()-1); j < vecCurrNoCavities.size(); j++)
				{
					Point p = (Point)vecCurrNoCavities.get(j);
					x = (int)p.getX();
					y = (int)p.getY();
					g2d.drawLine(x,y,x,y);
				}
			}

		}
		
	}
	
	

	/**
	* Internal function: Finds border pixels that have the shortest distance
	* to a presumed line, which is first calculated.
	* 
	* @param	horizontal	If true, a horizontal line calculation will be used. If not, a vertical line type calculation will be used.
	* @param	xy	Either the x or y reference point, depending on 'horizontal'
	* @param	interceptX	X intercept
	* @param	interceptY	Y intercept
	* @param	slopeX	X slope
	* @param	skopeY	Y slope
	* @param	slopeMissingX	If true, the corresponding slope is undefined (either purely horizontal or vertical)
	* @param	slopeMissingY	If true, the corresponding slope is undefined (either purely horizontal or vertical)
	* @param	intersectX	Intersection of X line
	* @param	intersectY	Intersection of Y line
	* @return	A Vector of Vector of Points of coordinates
	*/
	private static Point[] findBorderPixelsInt(boolean horizontal, Vector vecBorder, double xy,
		double interceptX, double interceptY, double slopeX, double slopeY, boolean slopeMissingX,
		boolean slopeMissingY, double intersectX, double intersectY)
	{
		int x,y;
		Point minDistPoint1 = (Point)vecBorder.get(0); //just any random border point
		Point minDistPoint2 = (Point)vecBorder.get(0); //just any random border point
		Point[] ret = new Point[2];
	
			
		//calc an intermediate point half-way between minY and maxY
		if (horizontal)
		{
			double x2 = (slopeMissingY) ? interceptY : ( (xy - interceptY)/slopeY );
			
			double minDist1 = Double.POSITIVE_INFINITY;
			double minDist2 = Double.POSITIVE_INFINITY;
			int minDistY = Integer.MAX_VALUE;
			int minDistX = Integer.MAX_VALUE;
			int maxDistY = Integer.MIN_VALUE;
			int maxDistX = Integer.MIN_VALUE;
			
			
			double deltaAdjust = (intersectY-xy);
			boolean update = true;
			
			for (int j = 0; j < vecBorder.size(); j++)
			{
				Point p = (Point)vecBorder.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				//calculate distance to the intersecting line from the point
				//this is not the exact distance but a comparable measure between points
				double distLine = 0.0;
				if (slopeMissingX)
					distLine = Math.abs(y-xy);
				else
					distLine = Math.abs(-x*slopeX +y - (interceptX - deltaAdjust  ) );
				//double distLine = Math.abs(-x*slopeX +y - Xintercept)/Math.sqrt(slopeX*slopeX+1);
				
				//check which side we are off the intercept
				//we want one point on each side
				//if ( x < (intersectX-OffsetX) )
				if ( x < x2 )
				{
					if (distLine < minDist1 || (distLine < (minDist1+EPS) ) ) //break ties by choosing the smallest y
					{
						update = true;
						if (!(distLine < minDist1)) //equal distances, we need a kicker
						{
							
							
							//encourage x to be as far from the center as possible
							//if we have several equal xs, pick the point with the lowest y
							//update = ( x < (minDistX+2*EPS)  || ( ( x < (minDistX+EPS) ) && (y < minDistY) ) );
							update = ( (y < minDistY) || ( y < (minDistY+EPS) && (x < minDistX) ));
							
						}
						
						if (update)
						{
							minDist1 = distLine;
							minDistPoint1 = new Point(x,y);
							minDistY = y;
							minDistX = x;
						}
						
					} 
					
				} else
				{
					
					if (distLine < minDist2 || (distLine < (minDist2+EPS)) )
					{
						
						update = true;
						if (!(distLine < minDist2)) //equal distances, we need a kicker
						{
							//encourage x to be as far from the center as possible
							//if we have several equal xs, pick the point with the lowest y
							//update = ( x > (maxDistX+EPS*2)  || ( ( (x + EPS) > maxDistX) && (y < maxDistY) ) );
							update = ( (y < maxDistY) || ( y < (maxDistY+EPS) && (x > maxDistX) ));
							
						}
						
						if (update)
						{
							minDist2 = distLine;
							minDistPoint2 = new Point(x,y);
							maxDistY = y;
							maxDistX = x;
						}
					}
				}
			}
		} else
		{
			double y2 = (slopeMissingX) ? interceptX : ( slopeX*xy + interceptX );
			//System.err.println("** y2: " + y2);
			
			//System.err.println("Intersect X: " + intersectX + ", xy: " + xy + ", y2: " + y2);
			
			//minDistPoint1 = new Point( (int)intersectX, (int)y2);
			//minDistPoint2 = new Point( (int)intersectX, (int)(y2+500) );
			
			double minDist1 = Double.POSITIVE_INFINITY;
			double minDist2 = Double.POSITIVE_INFINITY;
			int minDistY = Integer.MAX_VALUE;
			int minDistX = Integer.MAX_VALUE;
			int maxDistY = Integer.MIN_VALUE;
			int maxDistX = Integer.MIN_VALUE;
			
			double deltaAdjust = (intersectX-xy);
			boolean update = true;
			
			for (int j = 0; j < vecBorder.size(); j++)
			{
				Point p = (Point)vecBorder.get(j);
				x = (int)p.getX();
				y = (int)p.getY();
				
				//calculate distance to the intersecting line from the point
				//this is not the exact distance but a comparable measure between points
				double distLine = 0.0;
				if (slopeMissingY)
					distLine = Math.abs(x-xy);
				else
					distLine = Math.abs(-(x+deltaAdjust)*slopeY +y - (interceptY - deltaAdjust ));
				//double distLine = Math.abs(-x*slopeX +y - Xintercept)/Math.sqrt(slopeX*slopeX+1);
				
				//check which side we are off the intercept
				//we want one point on each side
				//if ( x < (intersectX-OffsetX) )
				if ( y < y2 )
				{
					if (distLine < minDist1 || (distLine < (minDist1+EPS) ) )
					{
						update = true;
						if (!(distLine < minDist1)) //equal distances, we need a kicker
						{
							//encourage y to be as far from the center as possible
							//if we have several equal ys, pick the point with the lowest x
							update = ( (x < minDistX)  || ( (x < (minDistX+EPS)) && (y < minDistY)  ) );
							
						}
						
						if (update)
						{
							minDist1 = distLine;
							minDistPoint1 = new Point(x,y);
							minDistX = x;
							minDistY = y;
						}
					}
					
				} else
				{
					if (distLine < minDist2  || (distLine < (minDist2+EPS)) )
					{
						update = true;
						if (!(distLine < minDist2)) //equal distances, we need a kicker
						{
							//encourage y to be as far from the center as possible
							//if we have several equal ys, pick the point with the lowest x
							update = ( (x < maxDistX)  || ( (x < (maxDistX+EPS)) && (y > maxDistY)  ) );
							
						}
						
						if (update)
						{
							minDist2 = distLine;
							minDistPoint2 = new Point(x,y);
							maxDistX = x;
							maxDistY = y;
						}
					}
				}
			}
			
		}
		
		ret[0] = minDistPoint1;
		ret[1] = minDistPoint2;
		
		return ret;
	}
	
	/**
	* Calculates the difference between two integer matrices, in the respect that only
	* values that are > 0 in only one of the matrices will be kept (otherwise they will be set to 0).
	* The returned object is an integer matrix with this 'difference'
	* 
	* @param	mat1	The first int[][] matrix
	* @param	mat2	The second int[][] matrix
	* @return	A int[][] matrix with only difference results retained
	*/
	public static int[][] matrixDifference(int[][] mat1, int[][] mat2)
	{
		int retMatWidth = mat1[0].length;
		int retMatHeight = mat2.length;

		//construct object
		int[][] retMat = new int[retMatHeight][retMatWidth];
		for (int y = 0; y < retMatHeight; y++)
			for (int x = 0; x < retMatWidth; x++)
			{
				if (mat1[y][x] == 0 && mat2[y][x] > 0)
					retMat[y][x] = mat2[y][x];
				else if (mat1[y][x] > 0 && mat2[y][x] == 0)
					retMat[y][x] = mat1[y][x];
				else
					retMat[y][x] = 0;
			}

		return retMat;
	}
	
	/**
	* Calculates the intersection between two integer matrices, in the respect that only
	* values that are > 0 in both of the matrices will be kept (otherwise they will be set to 0).
	* 
	* @param	mat1	The first int[][] matrix
	* @param	mat2	The second int[][] matrix
	* @return	A byte[][] matrix with 1 if the matrices intersects or 0 otherwise
	*/
	public static byte[][] matrixIntersect(int[][] mat1, int[][] mat2)
	{
		int retMatWidth = mat1[0].length;
		int retMatHeight = mat2.length;

		//construct object
		byte[][] retMat = new byte[retMatHeight][retMatWidth];
		for (int y = 0; y < retMatHeight; y++)
			for (int x = 0; x < retMatWidth; x++)
			{
				if (mat1[y][x] > 0 && mat2[y][x] > 0)
					retMat[y][x] = (byte)1;
				else
					retMat[y][x] = (byte)0;
			}

		return retMat;
	}
	
	/**
	* Calculates the intersection between two integer matrices, in the respect that only
	* values that are > 0 in both of the matrices will be kept (otherwise they will be set to 0).
	* The result is returned as a Vector of Vector of Points, where the index of the
	* first Vector is determined by the values in mat1.
	* 
	* @param	mat1	The first int[][] matrix (determines index of the first Vector)
	* @param	mat2	The second int[][] matrix
	* @return	A Vector of Vector of Points with intersection elements
	*/
	public static Vector matrixIntersectAsVector(int[][] mat1, int[][] mat2)
	{
		int retMatWidth = mat1[0].length;
		int retMatHeight = mat2.length;

		Vector vec = new Vector();
		Vector vecCurr;
		int currSize = -1;
		int objId = -1;
		
		//construct object
		//byte[][] retMat = new byte[retMatHeight][retMatWidth];
		for (int y = 0; y < retMatHeight; y++)
			for (int x = 0; x < retMatWidth; x++)
			{
				if (mat1[y][x] > 0 && mat2[y][x] > 0)
				{
					objId = mat1[y][x]-1;
					
					
					//construct empty vectors if requires
					if ( objId > currSize )
					{
						for (int i = currSize; i < objId; i++)
						{
							vec.add( new Vector() );
						}
						currSize = vec.size()-1;
					}
					
					vecCurr = (Vector)vec.get(objId);
					vecCurr.add( new Point(x, y) );
				}
			}

		return vec;
	}
	
	
	/**
	* Calculates the intersection between two Vectors of Vectors of Points matrices, in the respect that only
	* values that are present in both of the matrices will be kept.
	* The result is returned as a Vector of Vector of Points, where the index of the
	* first Vector is determined by the values in mat1.
	* The purpose is identical to the 'matrixIntersectAsVector' function using matrices, probably slower,
	* but requires less memory.
	* 
	* @param	vec1	The first vector
	* @param	vec2	The second vector
	* @return	A Vector of Vector of Points with intersection elements
	*/
	public static Vector vectorIntersectAsVector(Vector vec1, Vector vec2)
	{
		if (vec1.size() != vec2.size())
		{
			System.err.println("vectorIntersectAsVector: Different size of vectors (" + vec1.size() + " vs " + vec2.size() + ")");
			return null;
		}
		
		Vector retVec = new Vector();
		Vector vecCurr1, vecCurr2;
		Point p1, p2;
		
		for (int i = 0; i < vec1.size(); i++)
		{

			vecCurr1 = (Vector)vec1.get(i);
			vecCurr2 = (Vector)vec2.get(i);
			
			Vector retVecCurr = new Vector( (int)Math.min( vecCurr1.size(), vecCurr2.size() ) );
			
			for (int j = 0; j < vecCurr1.size(); j++)
			{
				boolean foundMatch = false;
				int k = 0;
				p1 = (Point)vecCurr1.get(j);
				
				while (!foundMatch && k < vecCurr2.size() )
				{
					p2 = (Point)vecCurr2.get(k);
					
					int distManhattan = (int)( Math.abs( p1.getX() - p2.getX() ) + Math.abs(p1.getY() - p2.getY() ) );
					
					if (distManhattan == 0)
					{
						retVecCurr.add( new Point(p1) );
						foundMatch = true;
					} else
					{
						k++;
					}
					
				}
			}
			
			retVec.add( retVecCurr );
		
		}

		return retVec;
	}
	
	
	
	
	/**
	*Converts a int matrix to a vector of vectors of Points
	* 
	* @param	mat	The int[][] matrix
	* @return	A Vector of Vector of Points
	*/
	public static Vector intMatrixToVectorOfPoints(int[][] mat)
	{
		int retMatWidth = mat[0].length;
		int retMatHeight = mat.length;

		Vector vec = new Vector();
		Vector vecCurr;
		int currSize = -1;
		int objId = -1;
		
		//construct object
		
		for (int y = 0; y < retMatHeight; y++)
			for (int x = 0; x < retMatWidth; x++)
			{
				if (mat[y][x] > 0)
				{
					objId = mat[y][x]-1;
					
					
					//construct empty vectors if requires
					if ( objId > currSize )
					{
						for (int i = currSize; i < objId; i++)
						{
							vec.add( new Vector() );
						}
						currSize = vec.size()-1;
					}
					
					vecCurr = (Vector)vec.get(objId);
					vecCurr.add( new Point(x, y) );
				}
			}

		return vec;
	}
			
	/**
	* Converts a Vector of Vector of Points into a int[][] matrix, where elements
	* are values defined by the index of the first Vector.
	* 
	* @param	vec 	Vector of Vector of Points
	* @param	width	Width of the matrix
	* @param	height	Height of the matrix
	* @return	An int[][] matrix where elements are integer values, defined by the index of the first Vector.
	*/
	public static int[][] vectorOfPointsToIntMatrix(Vector vec, int width, int height)
	{
		//construct object and reset
		int[][] retMat = new int[height][width];
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				retMat[y][x] = 0;
		
		Point p;
		int xx,yy;
		
		for (int i = 0; i < vec.size(); i++)
		{
			Vector currVec = (Vector)vec.get(i);
		
			for (int j = 0; j < currVec.size(); j++)
			{
				
				p = (Point)currVec.get(j);
				xx = (int)p.getX();
				yy = (int)p.getY();
				retMat[yy][xx] = i+1;
			}
		}

		return retMat;
	}
			
	/**
	*Adds a set of pixels (border pixels) to another set (contour hotspots)
	* if the border pixels are surrounded by contour pixels (i.e. has at least
	* a fixed amount of neighbors that are contour pixels)
	* 
	* @param	vecContour	Vector of Vector of Points with contour hotspots
	* @param	vecBorder	Vector of Vector of Points with border pixels
	* @param	lowerDistance	The lowest (Euclidean) distance allowed for two points to be considered neighbors
	* @param	numNeighbors	The minimum number of neighbors allowed before a border pixels is considered to be a contour hotspot pixel
	* @return	A Vector of Vector of Points with contour hotspot s
	*/
	public static Vector mergeContourHotspots(Vector vecContour, Vector vecBorder, double lowerDistance, int numNeighbors)
	{
		Vector retVec = new Vector();
		
		if (vecContour.size() != vecBorder.size() )
		{
			System.err.println("mergeContourHotspots: Different size of vectors (" + vecContour.size() + " vs " + vecBorder.size() + ")");
			return null;
		}
		
		for (int i = 0; i < vecBorder.size(); i++)
		{
			//it is assumed that the contour Vectors and border Vectors have the same indices
			Vector currVecContour = (Vector)vecContour.get(i);
			Vector currVecBorder = (Vector)vecBorder.get(i);
			
			//copy the initial contour vector to make sure we don't get any side-effects
			Vector currVecContourCopy = new Vector( currVecContour.size() );
			for (int j = 0; j < currVecContour.size(); j++)
				currVecContourCopy.add( new Point( (Point)currVecContour.get(j) ) );
			
			Point p1, p2;
			
			// calculate a distance matrix between all border and contour points
			double[][] distMatContour = new double[currVecBorder.size()][currVecContourCopy.size()];
			double currDist;
			for (int y = 0; y < currVecBorder.size(); y++)
				for (int x = 0; x < currVecContour.size(); x++)
				{
					p1 = (Point)currVecContour.get(x);
					p2 = (Point)currVecBorder.get(y);
					currDist = GrayscaleImageEdit.calcDistance(p1, p2, GrayscaleImageEdit.DIST_EUCLIDEAN);
					//distMatContour[x][y] = currDist;
					distMatContour[y][x] = currDist; //non-symmetric!
				}
			
			//repeat for each border pixel
			int numAdditions = 0;
			for (int j = 0; j < currVecBorder.size(); j++)
			{
				//find the closest contour point
				double minDist = Double.POSITIVE_INFINITY;
				int numNeighborsCurr = 0;
				for (int k = 0; k < currVecContour.size(); k++) //use the original vector, don't include new additions
				{
					if (distMatContour[j][k] < (lowerDistance+GrayscaleImageEdit.EPS) )
					{
						numNeighborsCurr++;
					}
				}
				if (numNeighborsCurr > numNeighbors)
				{
					//System.err.println("Border pixel " + j + " has " + numNeighborsCurr + " neighboring contour pixels and will be added to contour...");
					currVecContourCopy.add( new Point( (Point)currVecBorder.get(j) ) );
					numAdditions++;
				}
				
			}
			
			System.err.println("Object " + (i+1) + ": " + numAdditions + " putative border pixel(s) to add to contour...");
			
			//there may be duplicates in currVecContourCopy, remove them
			boolean anyDeleted = true;
			while (anyDeleted)
			{
				boolean foundAny = false;
				int distTot = -1, foundInd = -1;
				for (int j = 0; (j < (currVecContourCopy.size()-1) && !foundAny); j++)
					for (int k = (j+1); (k < currVecContourCopy.size() && !foundAny); k++)
					{
						p1 = (Point)currVecContourCopy.get(j);
						p2 = (Point)currVecContourCopy.get(k);
						distTot = (int)( Math.abs(  p1.getX() - p2.getX() ) + Math.abs( p1.getY() - p2.getY() ) );
						if (distTot == 0)
						{
							foundAny = true;
							foundInd = (int)Math.max(j, k);
						}
					}
				
				if (foundAny)
				{
					currVecContourCopy.removeElementAt(foundInd);
					numAdditions--;
					
				} else
					anyDeleted = false;
				
			}
			
			System.err.println("Object " + (i+1) + ": " + numAdditions + " border pixel(s) remains after removal of duplicates...");
			
			retVec.add(currVecContourCopy);
		}
	
		return retVec;
	}
	
	/**
	* Separates a set of contour pixels into defined clusters of 'hotspots'
	* 
	* @param	vecContour	A Vector of Vector of Points of contour elements
	* @param	contourMat	A int[][] matrix containing the same information as vecContour,
	*				but can be used for faster access to calculate neighbors
	* @param	use8	If true, 8-connectivity will be used to find neighboring pixels. Otherwise 4-connectivity will be used.
	* @return	A Vector of Vector of Vector Points, one Vector of Points for each contour group
	*/
	protected static Vector identifyContourGroups(Vector vecContour, int[][] contourMat, boolean use8)
	{
		Vector newContour = new Vector( vecContour.size() );
		
		for (int i = 0; i < vecContour.size(); i++)
		{
			
			Vector newContourCurr = new Vector( vecContour.size() );
			Vector vecCurrContour = (Vector)vecContour.get(i);
			Vector vecCurrContourCopy = new Vector(vecCurrContour.size());
			
			//copy the vector so that we can safely adjust it
			for (int j = 0; j < vecCurrContour.size(); j++)
			{
				vecCurrContourCopy.add( new Point( (Point)vecCurrContour.get(j)) );
			}
			
			boolean tooManyNeighbors = false;
			System.err.println("Total number of contour points of object " + (i+1) + " is " + vecCurrContour.size() + "...");
			
			// we remove elements from the copy to signal that they won't be used
			while (vecCurrContourCopy.size() > 0 && !tooManyNeighbors)
			{
			
				//we start by identifying any pixel which has only one neighbor (i.e. is at the outskirts of the group)
				boolean keepSearching = true;
				
				Point p;
				int x = 0, y = 0, numNeighbors = 0;
				int minNeighbors = Integer.MAX_VALUE;
				int minNeighborsIndex = -1;
				int j = 0;
				while (keepSearching)
				{
					p = (Point)vecCurrContourCopy.get(j);
					x = (int)p.getX();
					y = (int)p.getY();
					numNeighbors = 0;
					
					if ( (x > 0 && contourMat[y][x-1] != 0) )
						numNeighbors++;
					if ( (x < (contourMat[0].length-1) && contourMat[y][x+1] != 0) )
						numNeighbors++;
					if ( (y > 0 && contourMat[y-1][x] != 0) )
						numNeighbors++;
					if ( (y < (contourMat.length-1) && contourMat[y+1][x] != 0) )
						numNeighbors++;
					
					if (use8)
					{
						if ( (x > 0 && y > 0 && contourMat[y-1][x-1] != 0) )
							numNeighbors++;
						if ( (x < (contourMat[0].length-1) && y > 0 && contourMat[y-1][x+1] != 0) )
							numNeighbors++;
						if ( (x > 0 && y < (contourMat.length-1) && contourMat[y+1][x-1] != 0) )
							numNeighbors++;
						if ( (x < (contourMat[0].length-1) && y < (contourMat.length-1) && contourMat[y+1][x+1] != 0) )
							numNeighbors++;
					}
					
					
					
					if (numNeighbors > 1)
					{
						if (numNeighbors < minNeighbors)
						{
							minNeighbors = numNeighbors;
							minNeighborsIndex = j;
						}
						
						if (j == (vecCurrContourCopy.size()-1) )
						{
							//this should never happen! Added as a catch-all clause
							keepSearching = false;
							tooManyNeighbors = true;
							System.err.println("ERROR: Contour has no pixels without neighbors! Vector is of size " +
								vecCurrContourCopy.size() + " and we are indexed at " + j + ".");
							System.err.println("Minimum number of neighbors found is " + minNeighbors + " found at index " + minNeighborsIndex);
							
							
						} else
							j++;
						
					} else
					{
						keepSearching = false;
						tooManyNeighbors = false;
					}
				}
				
				
				if (!tooManyNeighbors)
				{
					//System.err.println("Found a contour starting point at (" + x + "," + y + ")..");
					int currPointInd = j; //starting point

					
					//this vector holds the result for the current contour group of Points
					Vector currContourGroup = new Vector();
					Point currPoint = new Point( (Point)vecCurrContourCopy.get(currPointInd) );
					currContourGroup.add( new Point(currPoint) );
					vecCurrContourCopy.removeElementAt(currPointInd);
					
					/*
					if (g2d != null)
					{
						g2d.setColor(Color.GREEN);
						g2d.drawLine( (int)currPoint.getX(), (int)currPoint.getY(),
							(int)currPoint.getX(), (int)currPoint.getY() );
					}
					*/
								
					
					keepSearching = true;
					j = 0;
					
					//now loop the current contour group to find neighbors
					while (keepSearching)
					{
						
						
						Point newNeighbor = new Point(-1,-1), compPoint = new Point(-1,-1);
						int minDist = Integer.MAX_VALUE;
						double minDistEucl = Double.POSITIVE_INFINITY;
						int minDistIndex = -1, minDistXYMax = Integer.MAX_VALUE;
						int distX,distY,distTot,distXYMax;
						double distEucl = 0.0;
						
						for (int k = 0; k < vecCurrContourCopy.size(); k++)
						{
							
							compPoint = (Point)vecCurrContourCopy.get(k);
							distX = (int)Math.abs( compPoint.getX() - currPoint.getX() );
							distY = (int)Math.abs( compPoint.getY() - currPoint.getY() );
							distTot = distX + distY;
							distXYMax = (int)Math.max(distX, distY);
							distEucl = currPoint.distance(compPoint);
							
							// A neighboring pixel must:
						
							// 2) Be greater than zero, otherwise we are compared a point to itself
							// 3) Have the smallest possible distance to the reference point
							if (distTot == 0)
							{
								System.err.println("  !! Found duplicate pixel: curr is at (" + (int)currPoint.getX() + "," + (int)currPoint.getY() + ")" +
									" while comp. is at (" + (int)compPoint.getX() + "," + (int)compPoint.getY() + ")");
									
							} 	else if (distTot > 0 &&  ( distTot < minDist || ( distTot == minDist && distXYMax < minDistXYMax ) ) )
							{
								
								minDistIndex = k;
								minDist = distTot;
								minDistXYMax = distXYMax;
								minDistEucl = distEucl;
								newNeighbor = new Point( (int)compPoint.getX(), (int)compPoint.getY() );
								//System.err.println("Entry " + j + " has neighbor " + k + " with distance " + distTot );
								
							}
							
						}
						
						if (minDistXYMax > 1 )
						{
							//Ending the current contour group
							
							//System.err.println("Object " + (i+1) + ": Point at (" + compPoint.getX() + "," + compPoint.getY() + ") isn't really the neighbor of " + 
							//	"(" + currPoint.getX() + "," + currPoint.getY() + "), but is the best match");
							//System.err.println("Ending contour (minimum distance is " +minDistXYMax + "/" + minDistEucl + "), with size " + currContourGroup.size());
							//System.err.println("Last pixel is at (" + (int)currPoint.getX() + "," + (int)currPoint.getY() + ")");
							//System.err.println("Number of remaining pixels is now " + vecCurrContourCopy.size() );
							
							newContourCurr.add(currContourGroup.clone());
							keepSearching = false;
							currPoint = null;
							
						} else
						{
							
							
							if (minDistIndex >= 0)
							{
								// We found a neighbor and is now adding it to the group
								
								currPoint = new Point( (Point)vecCurrContourCopy.get(minDistIndex) );
								currContourGroup.add( new Point(currPoint) );
								//System.err.println("  *Adding (" + (int)currPoint.getX() + "," + (int)currPoint.getY() + ")..");
								
								/*
								if (g2d != null)
								{
									g2d.setColor(Color.RED);
									g2d.drawLine( (int)currPoint.getX(), (int)currPoint.getY(),
										(int)currPoint.getX(), (int)currPoint.getY() );
								}
								*/
								
								currPointInd = minDistIndex;
								vecCurrContourCopy.removeElementAt(currPointInd);
								
								
								
							} else
							{
								System.err.println("Object " + (i+1) + ": Point at (" + currPoint.getX() + "," + currPoint.getY() + ") has no neighbor");
							}
						}
					}
					
				}
				
				
			}
			
			System.err.println("Contour of object " + (i+1) + " is composed of " + newContourCurr.size() + " group(s)...");
			newContour.add(newContourCurr);
			
		}
		
		
		return newContour;
	}	
	
	/**
	* Calculates the maximum number of possible landmarks (i.e. the shortest length of each border)
	* 
	* @param	vecBorders	A Vector of Vector of Points, with border coordinates
	* @return	The maximum number of possible landmarks
	*/
	protected static int getMaxBorderLandmarks(Vector vecBorders)
	{
		int minNum = Integer.MAX_VALUE;
		for (int i = 0; i < vecBorders.size(); i++)
		{
			Vector currVec = (Vector)vecBorders.get(i);
			int length = currVec.size();
			if (length  < minNum )
				minNum = length;
		}
		return minNum;
	}
	
	/**
	* Picks a fixed number of samples from the border to form a border 'landmark'.
	* 
	* @param	vecBorders	A Vector of Vector of Points, with border coordinates
	* @param	numSamples	The number of samples to pick from each border
	* @return	A Vector of Vector of Vector Points, each Vector containing
	*/
	protected static Vector getBorderLandmarks(Vector vecBorders, int numSamples)
	{
		Vector retVec = new Vector( vecBorders.size() );
		for (int i = 0; i < vecBorders.size(); i++)
		{
			Vector currVec = (Vector)vecBorders.get(i);
			Vector currRetVec = new Vector( numSamples );
			int length = currVec.size();
			
			if (numSamples >= length)
			{
				//just copy all available border pixels
				for (int j = 0; j < length; j++)
					currRetVec.add( new Point( (Point)currVec.get(j) ) );
				
			} else
			{
				//find a suitable number of border pixels
				double convertFactor = (double)length/(double)numSamples;
				double currInd = 0.0;
				int currIndInt = 0;
				//System.err.println("Boundary coord.: Object #" + (i+1) + " has length " + length + " and scale factor " + convertFactor);
				
				for (int j = 0; j < numSamples; j++)
				{
					currIndInt = (int)Math.min(Math.round(currInd), length-1);
					//System.err.println("Boundary coord.: Object #" + (i+1) + ", Adding index " + currIndInt );
					
					currRetVec.add( new Point( (Point)currVec.get(currIndInt) ) );
					currInd += convertFactor;
				}
			}
			retVec.add(currRetVec);
		}
		
		return retVec;
	}
	
	
	
	/**
	* Find a line that is perpendicular to another line, defined by an angle.
	* It does this moving from a pre-defined intersection point, with a known angle,
	* outwards from the intersection point until no more object pixels are found.
	* The two most extreme points form the perpendicular line. This does not assure
	* that the longest possible line is found, but is much faster than an exhaustive search.
	* 
	* @param	imgSeg	The segmentation image, with non-zero elements denoting objects
	* @param	objId	The object id to scan for
	* @param	pointIntersect	The intersection point, to start from
	* @param	templateLine	A template line that will be used for initiating the search.
	* @return A Point[2] describing a line, forming a perpendicular ling
	*/
	public static Point[] findPerpendicularLineFast(int[][] imgSeg, int objId, Point2D.Double pointIntersect, Point[] templateLine)
	{
		Point[] perpendicularLine = new Point[2];

		double deltaX = templateLine[1].getX() - templateLine[0].getX();
		double deltaY = templateLine[1].getY() - templateLine[0].getY();
		
		/* Normalize iteration step: delta on one axis will be 1, while
		 * delta on the other axis will be < 1. This insures that we go
		 * through all points of the object while seeking the extremities
		 * of the perpedicular */
		double maxDelta = Math.max(Math.abs(deltaX), Math.abs(deltaY));
		System.err.format("pre-norm  perpendicular delta: %f %f\n", deltaX, deltaY);

		deltaX /= maxDelta;
		deltaY /= maxDelta;
		
		System.err.format("post-norm perpendicular delta: %f %f\n", deltaX, deltaY);
		
		Point2D.Double point = new Point2D.Double();
		
		/* Iterate through perpendicular points to the left */
		point.x = pointIntersect.getX();
		point.y = pointIntersect.getY();
		while (pointIsWithinObject(imgSeg, objId, point)) {
			point.x += deltaY;
			point.y -= deltaX;
		}
		/* Make one step back (Barbara proposed this, I don't think its too elegant :P */
		perpendicularLine[0] = new Point(
				(int)(point.x - deltaY),
				(int)(point.y + deltaX));

		/* Iterate through perpendicular points to the right */
		point.x = pointIntersect.getX();
		point.y = pointIntersect.getY();
		while (pointIsWithinObject(imgSeg, objId, point)) {
			point.x -= deltaY;
			point.y += deltaX;
		}
		/* Make one step back (Barbara proposed this, I don't think its too elegant :P */
		perpendicularLine[1] = new Point(
				(int)(point.x + deltaY),
				(int)(point.y - deltaX));
				
		return perpendicularLine;
	}
	
	/**
	* Checks if a given Point is within a given object
	* 
	* @param	imgSeg	The segmentation image, with non-zero elements denoting objects
	* @param	objId	The object id to scan for
	* @param	point	The intersection point
	* @return True if the Point is within the object, otherwise false
	*/
	public static boolean pointIsWithinObject(int[][] imgSeg, int objId, Point2D.Double point)
	{
		int height = imgSeg.length;
		int width = imgSeg[0].length;
		
		int maxX = (int)Math.ceil(point.getX());
		int minX = (int)(point.getX());
		int maxY = (int)Math.ceil(point.getY());
		int minY = (int)(point.getY());
		
		
		boolean ret = (maxX >= 0 && maxX < width && maxY >= 0 && maxY < height && imgSeg[maxY][maxX] == objId) ||
			(maxX >= 0 && maxX < width && minY >= 0 && minY < height && imgSeg[minY][maxX] == objId) ||
			(minX >= 0 && minX < width && maxY >= 0 && maxY < height && imgSeg[maxY][minX] == objId) ||
			(minX >= 0 && minX < width && minY >= 0 && minY < height && imgSeg[minY][minX] == objId);
		
		return ret;
	}
	

	
}

