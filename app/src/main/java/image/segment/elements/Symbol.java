/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment.elements;

import java.io.Serializable;
import java.util.TreeMap;

/**
 *
 * @author itjamal
 */
public class Symbol extends Element implements Serializable{

    private SymbolType sType = SymbolType.NONE;
    private int[][] pixels;
    private int columnId;
    private int lineId;
    private int wordId;
    private double pixelDensity; //density of black pixels in symbol.
    private int spaceWithPreviousSymbol;

    @Override
    public TreeMap getSubElements() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SymbolType getsType() {
        return sType;
    }

    public void setsType(SymbolType sType) {
        this.sType = sType;
    }

    public int getWordId() {
        return wordId;
    }

    public void setWordId(int wordId) {
        this.wordId = wordId;
    }

    public int getColumnId() {
        return columnId;
    }

    public void setColumnId(int columnId) {
        this.columnId = columnId;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public int[][] getPixels() {
        return pixels;
    }

    public void setPixels(int[][] pixels) {
        this.pixels = pixels;
    }

    public double getPixelDensity() {
        return pixelDensity;
    }

    public void setPixelDensity(double pixelDensity) {
        this.pixelDensity = pixelDensity;
    }

    public int getSpaceWithPreviousSymbol() {
        return spaceWithPreviousSymbol;
    }

    public void setSpaceWithPreviousSymbol(int spaceWithPreviousSymbol) {
        this.spaceWithPreviousSymbol = spaceWithPreviousSymbol;
    }

    public void calculateHistogram(boolean bFilled) {        
        int histValue = 0;

        for (int j = 0; j < pixels[0].length; j++) {
            histValue = 0;
            // calculating the pixel count for each line
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i][j] == 1) {
                    if (bFilled) {
                        // This IF checks for the closed areas.
                        if (histValue > 0) {
                            histValue += (i - histValue) + 1; // I add 1 because of the 0 index.
                        } else {
                            histValue++;
                        }
                    } else {
                        histValue++;
                    }

                }
            }
            //Store only informative part (in global coordinates - it should be processed by Word)
            if (histValue > 0) {
                histogramMap.put(borders.y+j, histValue);
            }
        }
    }


}
