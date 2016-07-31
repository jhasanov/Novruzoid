/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment.elements;

import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeMap;

import utils.Rectangle;

/**
 *
 * @author itjamal
 */
public abstract class Element implements Serializable{

    public static int AVERAGE_SYMBOL_WIDTH = 0;
    public static double BASELINE_THRES = 0.5;
    public static double ASCDESC_THRES = 0.2;
    int elementId;
    int upperBaseline = 0;
    int bottomBaseline = 0;
    Integer maxHistogramVal = 0;
    Rectangle borders;
    TreeMap<Integer, Integer> histogramMap = new TreeMap();

    public abstract TreeMap getSubElements();

    /**
     * This method calculates the histogram of the symbol. If param of the method is true,
     * then the closed areas of the symbol is assumed as filled. Sample of "a":
     *
     *   **** *             ******
     *  *    *             ******
     *  *    *     --->    ******
     *  *    *  *          ********
     *   **** **            ******
     *
     * It emphasizes the base part of the symbols.
     * The histogram of the symbol is stored in a TreeMap which stores the
     * vertical location (globally) and the count of pixels in this line.
     * The Word class uses this histogram for each symbol and gets word's histogram.
     * @param bFilled if the parameter is true, then it calculates histogram in a filled mode.
     */
    public abstract void calculateHistogram(boolean bFilled);

    public Rectangle getBorders() {
        return borders;
    }

    @Override
    public String toString() {
        return "(" + borders.x + "," + borders.y + "," + borders.width + "," + borders.height + ")";
    }

    public void setUpperBaseline(int upperBaseline) {
        this.upperBaseline = upperBaseline;
    }

    public int getUpperBaseline() {
        return upperBaseline;
    }

    public void setBottomBaseline(int bottomBaseline) {
        this.bottomBaseline = bottomBaseline;
    }

    public int getBottomBaseline() {
        return bottomBaseline;
    }

    public void setBorders(Rectangle borders) {
        this.borders = borders;
    }

    public TreeMap<Integer, Integer> getHistogramMap() {
        return histogramMap;
    }

    public void setHistogramMap(TreeMap<Integer, Integer> histogramMap) {
        this.histogramMap = histogramMap;
    }

    public int getElementId() {
        return elementId;
    }

    public void setElementId(int elementId) {
        this.elementId = elementId;
    }

    public Integer getMaxHistogramVal() {
        return maxHistogramVal;
    }

    public void setMaxHistogramVal(Integer maxHistogramVal) {
        this.maxHistogramVal = maxHistogramVal;
    }

//    public Integer getHistogramMaxVal() {
//        return maxHistogramVal;
//    }
//
//    public void setHistogramMaxVal(Integer histogramMaxVal) {
//        this.maxHistogramVal = histogramMaxVal;
//    }
    /**
     * This method gets histogram of each subelement as (Y coordinate, pixel count)
     * map and creates a similar histogram map (wordHistogramMap) for element.
     */
    public void getSubElementBasedHistogram(boolean bFilled) {
        Iterator it = getSubElements().values().iterator();

        // Build a histogam for the whole word, based on symbols' histogram
        while (it.hasNext()) {
            Element element = (Element) it.next();
            element.calculateHistogram(bFilled);
            TreeMap<Integer, Integer> symbHistMap = element.getHistogramMap();
            Iterator symbolsIt = symbHistMap.keySet().iterator();
            while (symbolsIt.hasNext()) {
                Integer yCoord = (Integer) symbolsIt.next();
                Integer value = symbHistMap.get(yCoord);

                Integer histValForY = histogramMap.get(yCoord);
                //System.out.println("CLass:"+this.getClass()+"key="+yCoord+"; val = "+value);
                Integer histVal = 0;
                if (histValForY == null) {
                    histVal = value;
                } else {
                    histVal = histValForY + value;
                }
                histogramMap.put(yCoord, histVal);
                if (histVal > maxHistogramVal) {
                    maxHistogramVal = histVal;
                }

            }
        }

        //System.out.println("CLass:" + this.getClass() + "; Max val = " + maxHistogramVal + "; thresh=" + maxHistogramVal * BASELINE_THRES + "; word borders: " + borders);
    }

    /**
     * This method calculates the baseline of the word and sets type for each symbol.
     */
    public void calculateBaselines() {
        // Put initial values. If word has noo upper or bottom baseline, it will remain default.
        upperBaseline = borders.y;
        bottomBaseline = borders.y + borders.height;

        // Now calculate baseline
        for (int y = borders.y; y < borders.y + borders.height; y++) {
            Integer value = histogramMap.get(y);
            //System.out.println("y="+y+"; val="+value);

            // if upper baseline hasn't been found.
            if ((upperBaseline == borders.y) && (value != null) && (value > maxHistogramVal * BASELINE_THRES)) {
                upperBaseline = y - 1;

            }
            // if upper baseline is found.
            if ((value != null) && (upperBaseline > borders.y) && (value < maxHistogramVal * BASELINE_THRES)) {
                bottomBaseline = y;
                break; // no need top go further - we have already found the bottom baseline.
            }
        }
    }
}
