/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment;


import java.util.ArrayList;
import java.util.List;

import image.ImageInfo;
import utils.Rectangle;

/**
 *
 * @author itjamal
 */
public class TextImage {

    public static double HORIZ_BORDER_THRES = 0.0;//0.05;
    public static double COLUMN_BORDER_THRES = 0.0;
    public static int MIN_COLUMN_WIDTH = 100;
    public static int MIN_COLUMN_SPACE = 20;

    // If you don't need to have multiple columns, then set it to TRUE
    private boolean bNoColumnsNeeded;

    public void setNoColumns(boolean val) {
        bNoColumnsNeeded = val;
    }

    /**
     * Calculates a horizontal histogram for bit based images.
     * @param pixelArr an array of INTs, where each byte of first dimension
     * keeps information about 8 pixels.
     * @return value of histogram bars.
     */
    public int[] horizontalBitHistogram(int[][] pixelArr, int xStart, int yStart, int xEnd, int yEnd) {
        if (pixelArr == null) {
            throw new NullPointerException();
        }

        int[] hist = new int[pixelArr[0].length + 1];
        int maxVal = 0; // maximum value of the histogram

        for (int j = yStart; j < pixelArr[0].length; j++) {
            if ((yEnd != 0) && (j == yEnd)) {
                continue;
            }

            for (int i = xStart / 31; i < pixelArr.length; i++) {
                if ((xEnd != 0) && (i > xEnd / 31)) {
                    continue;
                }
                hist[j + 1] += Integer.bitCount(pixelArr[i][j]);
            }
            maxVal = (maxVal > hist[j + 1]) ? maxVal : hist[j + 1];
        }

        //the first element keeps the maximum value of the histogram
        hist[0] = maxVal;

        return hist;
    }

    public int[] horizontalBitHistogram(int[][] pixelArr) {
        if (pixelArr == null) {
            throw new NullPointerException();
        }

        int[] hist = new int[pixelArr[0].length + 1];
        int maxVal = 0; // maximum value of the histogram

        for (int j = 0; j < pixelArr[0].length; j++) {

            for (int i = 0; i < pixelArr.length; i++) {

                hist[j + 1] += Integer.bitCount(pixelArr[i][j]);
            }
            maxVal = (maxVal > hist[j + 1]) ? maxVal : hist[j + 1];
        }

        //the first element keeps the maximum value of the histogram
        hist[0] = maxVal;

        return hist;
    }

    /**
     * Calculates a vertical histogram for bit based images.
     * @param pixelArr an array of INTs, where each byte of first dimension
     * keeps information about 8 pixels.
     * @return value of histogram bars.
     */
    public int[] verticalBitHistogram(int[][] pixelArr) {

        if (pixelArr == null) {
            throw new NullPointerException();
        }

        int[] hist = new int[pixelArr.length + 1];
        int maxVal = 0; // maximum value of the histogram

        for (int i = 0; i < pixelArr.length; i++) {
            for (int j = 0; j < pixelArr[0].length; j++) {
                hist[i + 1] += Integer.bitCount(pixelArr[i][j]);
            }
            //Log.d("TextImage.verticalBitHistogram()", "hist[" + i + "]=" + hist[i + 1]);

            maxVal = (maxVal > hist[i + 1]) ? maxVal : hist[i + 1];
        }

        //the first element keeps the maximum value of the histogram
        hist[0] = maxVal;

        return hist;
    }

    public List<Integer> findHorizontalBorders(ImageInfo imageInfo, int xStart, int yStart, int xEnd, int yEnd) {
        List<Integer> borders = new ArrayList<Integer>();

        int[] horizHist = horizontalBitHistogram(imageInfo.getPixelArrH(), xStart, yStart, xEnd, yEnd);

        int height = horizHist.length;
        int maxVal = horizHist[0];
        boolean bThresStarted = true;


        for (int i = 1; i < height; i++) {

            if (horizHist[i] <= maxVal * HORIZ_BORDER_THRES) {
                if (!bThresStarted) {
                    borders.add(i - 1);
                    bThresStarted = true;
                }
            } else {
                if (bThresStarted) {
                    borders.add(i - 1);
                }
                bThresStarted = false;
            }
        }

        if (!bThresStarted) {
            borders.add(height - 1);
        }
        return borders;

    }

    public List<Integer> findColumns(ImageInfo imageInfo) {
        List<Integer> borders = new ArrayList<Integer>();

        int[] horizHist = verticalBitHistogram(imageInfo.getPixelArrV());
        int height = horizHist.length;
        int maxVal = horizHist[0];

        if (bNoColumnsNeeded) {
            borders.add(0);
            borders.add(height - 1);
        } else {
            boolean bThresStarted = true;

            for (int i = 1; i < height; i++) {
                if (horizHist[i] <= maxVal * COLUMN_BORDER_THRES) {
                    if (!bThresStarted) {
                        borders.add(i - 1);
                        bThresStarted = true;
                    }
                } else {
                    if (bThresStarted) {
                        borders.add(i - 1);
                    }
                    bThresStarted = false;
                }
            }

            if (!bThresStarted) {
                borders.add(height - 1);
            }
        }
        return borders;
    }

    public List<Rectangle> getTextAreas(ImageInfo imageInfo) {
        List<Rectangle> textAreaBorders = new ArrayList<Rectangle>();

        List<Integer> columnBorders = findColumns(imageInfo);

        Integer globalLeft = columnBorders.get(0);
        Integer lastRight = Integer.MAX_VALUE;
        Integer left = 0;
        Integer right = 0;

        for (int idx = 0; idx < columnBorders.size()+1;) {
            try {
                if (idx < columnBorders.size()) {
                    left = columnBorders.get(idx++);
                    right = columnBorders.get(idx++);
                }
                else
                    idx++;

                /*
                 * If it's the last column  - merge previous parts into a column
                 *
                 * OR
                 * 
                 * 1) If space between columns are less than should be, then it's 
                 * not a column.
                 * AND
                 * 2) If width of the column is too small, then it's 
                 * not a column
                 */
                if ((idx > columnBorders.size())
                        || (((left - lastRight) > MIN_COLUMN_SPACE) && ((lastRight - globalLeft) > MIN_COLUMN_WIDTH))) {

                    List<Integer> horizBorders = findHorizontalBorders(imageInfo, globalLeft, 0, lastRight, 0);
                    int top = horizBorders.get(0);
                    int bottom = horizBorders.get(horizBorders.size() - 1);
                    textAreaBorders.add(new Rectangle(globalLeft, top, lastRight - globalLeft, bottom - top));

                    globalLeft = left;
                }

                lastRight = right;
            } catch (Exception ex) {
                System.out.println("TextImage.getTextAreas : TextImage.getTextAreas(ImageInfo) : SIZE = " + columnBorders.size());
                System.out.println("TextImage.getTextAreas : TextImage.getTextAreas(ImageInfo) : " + ex);
            }
        }

        return textAreaBorders;
    }
}
