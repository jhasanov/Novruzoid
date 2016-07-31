/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment;

import android.graphics.Bitmap;
import android.util.Log;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import image.AndroidBinaryImageInfo;
import image.ImageInfo;
import image.segment.elements.Column;
import image.segment.elements.Element;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import utils.Point;
import utils.Rectangle;

/**
 *
 * @author jamal ve Elshan
 */
public class ImageSegment {

    private TreeMap<Integer, Column> columnsMap = new TreeMap<Integer, Column>();
    // minimal height of the textLines
    private int MINIMUM_SYMBOL_HEIGHT = 3;
    private int MAXIMUM_SYMBOL_HEIGHT = 300;
    private ImageInfo imageInfo;
    public static int GRAY_THRES = 200;
    MultiMap newObjsMap = new MultiValueMap();

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public TreeMap<Integer, Column> getColumnsMap() {
        return columnsMap;
    }

    /*
     * Finds borders of objects
     */
    public void findPrintedObjects(Rectangle borders) {
        int h = imageInfo.getHeight();
        int w = imageInfo.getWidth();
        newObjsMap = new MultiValueMap();

        int[][] objArray = new int[2][w]; // holds only 2 lines
        Arrays.fill(objArray[0], Integer.MAX_VALUE);
        Arrays.fill(objArray[1], Integer.MAX_VALUE);

        int objId = 0;
        int leftBdr = 0;
        int topBdr = 0;
        int rightBdr = w;
        int bottomBdr = h;

        if (borders != null) {
            leftBdr = borders.x;
            rightBdr = leftBdr + borders.width;
            topBdr = borders.y;
            bottomBdr = topBdr + borders.height;
        }

        int[][] pixels = imageInfo.getPixelArrH();
        int startX = 0;
        Set<Integer> idsToReplaceSet = new TreeSet<Integer>();  // adds only new

        for (int j = topBdr; j < bottomBdr; j++) {
            boolean bChainStarted = false;
            boolean bBlackPixelsEnded = false;

            // neighbourId holds the minimal value of the neighboring pixels (top left, top and left)
            int neighbourId = Integer.MAX_VALUE;
            for (int i = leftBdr; i < rightBdr; i++) {
                int idx = (i / 31);
                int bitPos = 30 - (i - 31 * idx);
                int pixelVal = (int) Math.pow(2, bitPos);


                //  Black pixel found.
                if ((pixels[idx][j] & pixelVal) == pixelVal) {

                    // if it's the first black pixel in the sequence of plack pixels
                    if (!bChainStarted) {
                        // get the minimal value of neighbours (top and top left)
                        if (i > 0) {
                            neighbourId = Math.min(objArray[0][i - 1], objArray[0][i]);
                        } else {
                            neighbourId = objArray[0][i];
                        }

                        // if top left pixel has value greater than minimal (neighbourId) then replace this value
                        if ((i > 0) && (objArray[0][i - 1] != Integer.MAX_VALUE) && (objArray[0][i - 1] > neighbourId)) {
                            idsToReplaceSet.add(objArray[0][i - 1]);
                        }
                        // if top pixel has value greater than minimal (neighbourId) then replace this value
                        if ((objArray[0][i] != Integer.MAX_VALUE) && (objArray[0][i] > neighbourId)) {
                            idsToReplaceSet.add(objArray[0][i]);
                        }

                        bChainStarted = true;
                        startX = i;
                    } else { // sequence of black pixels has already started.
                        int minVal = Math.min(neighbourId, objArray[0][i]);

                        // if top pixel has value greater than minimal (neighbourId) then replace this value
                        if ((objArray[0][i] != Integer.MAX_VALUE) && (objArray[0][i] > minVal)) {
                            idsToReplaceSet.add(objArray[0][i]);
                            // if minimal value (neighbourId) is greater that the top pixel then replace this value
                        } else if ((neighbourId != Integer.MAX_VALUE) && (neighbourId > minVal)) {
                            idsToReplaceSet.add(neighbourId);
                        }
                        neighbourId = minVal;
                    }
                } else if (bChainStarted) {
                    bBlackPixelsEnded = true;
                }

                // If the sequence is ended or top right border is reached, then add them to list.
                if ((bChainStarted) && ((bBlackPixelsEnded) || (i == (rightBdr - 1)))) {
                    // if right border hasn't reached (sequence ended in the middle of the line) 
                    if (i < rightBdr - 1) {
                        int minVal = Math.min(neighbourId, objArray[0][i]);
                        if ((objArray[0][i] != Integer.MAX_VALUE) && (objArray[0][i] > minVal)) {
                            idsToReplaceSet.add(objArray[0][i]);
                        } else if ((neighbourId != Integer.MAX_VALUE) && (neighbourId > minVal)) {
                            idsToReplaceSet.add(neighbourId);
                        }
                        neighbourId = minVal;
                    }
                    if (neighbourId == Integer.MAX_VALUE) {
                        neighbourId = ++objId;
                    }

                    //replace connected ID's with grater value.
                    for (Integer oldId : idsToReplaceSet) {
                        //System.out.println("ImageSegment.findPrintedObjects(); replacing " + oldId + " with " + neighbourId);
                        replaceObjIds(oldId, neighbourId);

                        // replacing values in upper line.
                        for (int ri = i; ri < objArray[0].length; ri++) {
                            if (objArray[0][ri] == oldId) {
                                objArray[0][ri] = neighbourId;
                            }
                        }
                        // replacing values in bottom line.
                        for (int ri = leftBdr; ri < i; ri++) {
                            if (objArray[1][ri] == oldId) {
                                objArray[1][ri] = neighbourId;
                            }
                        }

                    }
                    idsToReplaceSet.clear();

                    for (int x = startX; x < i; x++) {
                        objArray[1][x] = neighbourId;
                        newObjsMap.put(neighbourId, new Point(x, j));
                        //System.out.println("i=" + (i - leftBdr) + "; j=" + (j - topBdr) + "; ID=" + neighbourId);
                    }


                    bChainStarted = false;
                    bBlackPixelsEnded = false;
                    neighbourId = Integer.MAX_VALUE;
                }
            }
            objArray[0] = objArray[1];
            /*for (int a = 0; a < objArray[1].length; a++) {
            if (objArray[1][a] == Integer.MAX_VALUE) {
            System.out.print("_");
            } else {
            System.out.print(objArray[1][a]);
            }
            }
            System.out.println();             
             */

            objArray[1] = new int[w];
            Arrays.fill(objArray[1], Integer.MAX_VALUE);
        }
    }

    void replaceObjIds(int oldObjId, int newObjId) {
        List pixelList = (List) newObjsMap.get(oldObjId);
        newObjsMap.remove(oldObjId);

        if (pixelList != null) {
            Iterator pixelsItr = pixelList.iterator();
            while (pixelsItr.hasNext()) {
                Point p = (Point) pixelsItr.next();
                newObjsMap.put(newObjId, p);
            }
        }
    }

    // Initial image processing: converts picture to a binary array
    public void processImage(Bitmap image, boolean bSquareBased) {
        Log.i(this.getClass().toString(), "starting to process");
        AndroidBinaryImageInfo abii = new AndroidBinaryImageInfo();
        Log.i(this.getClass().toString(), "conversion is finished");

        abii.convertToPixelArr(image, 1, bSquareBased);
        //bii.testIt();
        Log.i(this.getClass().toString(), "conversion is finished");

        setImageInfo(abii);
    }

    /*
     * Segmenting image
     */
    public void segmentImage() {//(Bitmap bimg) {
        // 1. Get columns  
        // 2. Get lines inside columns
        // 3. Get text/words inside columns
        int totalSymbolWidth = 0;
        int symbolCnt = 0;

        TextImage.MIN_COLUMN_WIDTH = getImageInfo().getWidth() / 3;
        TextImage ti = new TextImage();

        List<Rectangle> textAreas = ti.getTextAreas(getImageInfo());

        /**
         * Finding columns and text lines inside this columns.
         * Each Column object hold a map of lines.
         */
        int columnIdx = 0;
        for (Rectangle columnBdr : textAreas) {
            //Find lines
            List<Integer> lines = ti.findHorizontalBorders(getImageInfo(), columnBdr.x, columnBdr.y, columnBdr.x + columnBdr.width, columnBdr.y + columnBdr.height);

            TreeMap linesMap = new TreeMap<Integer, Rectangle>();

            for (int idx = 0; idx < lines.size(); idx += 2) {
                TextLine textLine = new TextLine();

                textLine.setElementId(idx / 2);
                int lineHeight = lines.get(idx + 1).intValue() - lines.get(idx).intValue();
                textLine.setBorders(new Rectangle(columnBdr.x, lines.get(idx).intValue(), columnBdr.width, lineHeight));
                linesMap.put(idx / 2, textLine);
            }
            Column col = new Column();
            col.setBorders(columnBdr);
            col.setColumnID(columnIdx);
            col.setLines(linesMap);
            columnsMap.put(columnIdx++, col);
        }

        /**
         * Searching for symbols inside of each column.
         * 1. Iterating over the columnsMap
         * 2. Finding elements in columns area and saving them as MultiMap (object id, Point())
         * 3. Iterating over the MultiMap and create a Symbol element.
         */
        for (columnIdx = 0; columnIdx < columnsMap.size(); columnIdx++) {
            Column col = (Column) columnsMap.get(columnIdx);

            //ImageSegment imgSeg = new ImageSegment();
            //imgSeg.getBorders(GRAY_THRES, col.getBorders());
            //MultiMap objsMap = imgSeg.saveAsMap(getImageInfo().getWidth(), getImageInfo().getHeight());
            long startTime = System.currentTimeMillis();
            //getBorders(GRAY_THRES, col.getBorders());
            findPrintedObjects(col.getBorders());

            //Log.d(this.getClass().toString(), "saveAsMap()");
            startTime = System.currentTimeMillis();
//            MultiMap objsMap = saveAsMap(getImageInfo().getWidth(), getImageInfo().getHeight());
//            Log.d(this.getClass().toString(), "saveAsMap() is finished. Seconds spent : " + (System.currentTimeMillis() - startTime) / 1000);
//            Iterator<Integer> objKeysItr = objsMap.keySet().iterator();

            Iterator<Integer> objKeysItr = newObjsMap.keySet().iterator();
            while (objKeysItr.hasNext()) {
                Integer objId = objKeysItr.next();
                //Log.i("segmentImage", "keys in multi map: " + objId);
                List pixelList = (List) newObjsMap.get(objId);
                Symbol smb = new Symbol();
                if (pixelList != null) {
                    //Log.i("segmentImage", "pixels in multi map: " + pixelList.size());
                    Iterator pixelsItr = pixelList.iterator();
                    int minX = -1;
                    int minY = -1;
                    int maxX = -1;
                    int maxY = -1;

                    // finding symbol boundaries.
                    while (pixelsItr.hasNext()) {
                        Point p = (Point) pixelsItr.next();
                        //Log.i("segmentImage", "pixels : " + p.x + "," + p.y + ")");

                        if (minX == -1) {
                            minX = (int) p.x;
                            minY = (int) p.y;
                            maxX = (int) p.x;
                            maxY = (int) p.y;
                        }

                        if (p.x < minX) {
                            minX = (int) p.x;
                        } else if (p.x > maxX) {
                            maxX = (int) p.x;
                        }

                        if (p.y < minY) {
                            minY = (int) p.y;
                        } else if (p.y > maxY) {
                            maxY = (int) p.y;
                        }
                    }

                    int symbolHeight = maxY - minY + 1;
                    int symbolWidth = maxX - minX + 1;

                    if ((symbolHeight > MINIMUM_SYMBOL_HEIGHT) && (symbolHeight < MAXIMUM_SYMBOL_HEIGHT)) {
                        totalSymbolWidth += symbolWidth;
                        symbolCnt++;

                        // holding pixels in INT array
                        int[][] pixelArr = new int[symbolWidth][symbolHeight];
                        double pixCnt = 0;

                        pixelsItr = pixelList.iterator();
                        while (pixelsItr.hasNext()) {
                            Point p = (Point) pixelsItr.next();
                            try {
                                pixelArr[(int) p.x - minX][(int) p.y - minY] = 1;
                                pixCnt++;
                            } catch (Exception ex) {
                                System.out.println("ImageSegment.segmentImage() : minX=" + minX + "; minY=" + minY + "; maxX=" + maxX + "; maxY=" + maxY + "; W=" + (maxX - minX) + "; H=" + (maxY - minY) + "; getX()=" + p.x + "; getY()=" + p.y);
                            }
                        }

                        // calculate the pixel density in char
                        double pixelDensity = pixCnt / (symbolWidth * symbolHeight);

                        // defining the line where this symbol exists.
                        int middleY = minY + (maxY - minY) / 2;
                        int lineIdx = getLineIdx(columnIdx, middleY);

                        smb.setPixels(pixelArr);
                        smb.setBorders(new Rectangle(minX, minY, maxX - minX, maxY - minY));
                        smb.setColumnId(columnIdx);
                        smb.setLineId(lineIdx);
                        smb.setPixelDensity(pixelDensity);


                        //update TextLine object in Column
                        TextLine tLine = col.getTextLine(lineIdx);
                        if (tLine != null) {
                            tLine.addSymbol(minX, smb);
                            col.setTextLine(lineIdx, tLine);
                        } else {
                            System.out.println("ImageSegment.segmentImage() : S");
                        }
                    }
                }
            }
            System.out.println("ImageSegment.segmentImage() : Total symbols: " + symbolCnt);

        }
        Element.AVERAGE_SYMBOL_WIDTH = totalSymbolWidth / symbolCnt;
        System.out.println("ImageSegment.segmentImage() : Average symbol width: " + Element.AVERAGE_SYMBOL_WIDTH);

    }

    /**
     * Gets the id of the text line by given vertical location of the symbol.
     * @param columnIdx index of the column where symbol exists.
     * @param yCoord y axis of the symbol. It's better to choose a middle vertical point of symbols.
     * @return id of the line where this point exist.
     */
    private int getLineIdx(int columnIdx, int yCoord) {
        Column column = columnsMap.get(columnIdx);
        TreeMap textLinesMap = column.getLines();
        Iterator it = textLinesMap.keySet().iterator();
        while (it.hasNext()) {
            Integer lineId = (Integer) it.next();
            TextLine textLine = (TextLine) textLinesMap.get(lineId);
            Rectangle borders = textLine.getBorders();
            if (borders.contains(borders.x + 10, yCoord)) {
                return textLine.getElementId();
            }
        }

        /*
         * In future, next iteration can check for symbols which reside between the lines (not inside).
         * For example, if symbol is between the line 4 and 5, then it could be added to line 4 or 5.
         */
        return -1;
    }

    /**
     * This procedure removes useless lines, symbols, etc.
     */
    public void postSegmentProcess(boolean bSymbolWidthBased) {

        // Remove lines that doesn't hold symbols.
        Iterator it = columnsMap.keySet().iterator();
        while (it.hasNext()) {
            Integer colKey = (Integer) it.next();
            Column col = (Column) columnsMap.get(colKey);
            TreeMap textLinesMap = col.getLines();

            Iterator it2 = textLinesMap.keySet().iterator();
            while (it2.hasNext()) {
                Integer tlKey = (Integer) it2.next();
                TextLine textLine = (TextLine) textLinesMap.get(tlKey);

                // if text line has no symbols ( little symbols are eliminated) then remove this line
                if (textLine.getWords().isEmpty()) {
                    it2.remove();
                    //textLinesMap.remove(tlKey);
                } // If height of the list is less then average text width and contains only dots, then merge it with next line (bottom)
                else if (textLine.getBorders().getHeight() < 0.4 * Element.AVERAGE_SYMBOL_WIDTH) {
                    try {
                        Word wordOfSmallLine = (Word) textLine.getWords().get(0);
                        Rectangle prevBdrs = textLine.getBorders();
                        // If all of symbols are dots
                        //if (wordOfSmallLine.getDotCnt() == wordOfSmallLine.getSymbols().size()) {
                        it2.remove();

                        Integer nextKey = (Integer) it2.next();
                        TextLine tl = (TextLine) textLinesMap.get(nextKey);
                        Rectangle bdrs = tl.getBorders();

                        tl.setBorders(new Rectangle(bdrs.x, prevBdrs.y, bdrs.width, bdrs.height + (bdrs.y - prevBdrs.y)));

                        tl.mergeWords(wordOfSmallLine);

                        // Split text line into words
                        tl.splitToWords(bSymbolWidthBased);
                        textLinesMap.put(nextKey, tl);
                    } catch (Exception ex) {
                        System.out.println("ImageSegment.postSegmentProcess1 :" + ex.getMessage());
                    }
                    // }

                } else {
                    // Split text line into words
                    try {
                        textLine.splitToWords(bSymbolWidthBased);
                        textLinesMap.put(tlKey, textLine);
                    } catch (Exception ex) {
                        System.out.println("ImageSegment.postSegmentProcess2 :" + ex.getMessage());
                    }
                }
            }
            col.setLines(textLinesMap);
            columnsMap.put(colKey, col);
        }


        // Remove very small and very big elements that are not symbols.

    }
}
