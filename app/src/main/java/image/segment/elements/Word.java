/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment.elements;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import utils.Rectangle;

/**
 *
 * @author itjamal
 */
public class Word extends Element {

    TreeMap<Integer, Symbol> symbolsMap = new TreeMap();
    private int dotCnt;

    public Word() {
        // At the start I put a MAX value for left top point. It would be set to the
        // first symbol's left top position during the first iteration.
        borders = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
    }

    public int getDotCnt() {
        return dotCnt;
    }

    public void setDotCnt(int dotCnt) {
        this.dotCnt = dotCnt;
    }

    public TreeMap getSubElements() {
        return symbolsMap;
    }

    public void addSymbol(Integer xPosition, Symbol smb) {
        if (symbolsMap.containsKey(xPosition)) {
            symbolsMap.put(xPosition+1, smb);
        } else {
            symbolsMap.put(xPosition, smb);
        }
        
        if (borders.x > xPosition) {
            borders.x = xPosition;
        }
        if (borders.y > (int) smb.getBorders().getY()) {
            // doing it corrects first letter problem (bug) where first descending letter's descending part is not considered.
            if (borders.y != Integer.MAX_VALUE) {
                borders.height += borders.y - (int) smb.getBorders().getY();
            }
            //--------------------------------
            borders.y = (int) smb.getBorders().getY();
        }
        borders.width = Math.max(borders.width, (int) xPosition + (int) smb.getBorders().getWidth() - borders.x);
        borders.height = Math.max(borders.height, (int) smb.getBorders().getY() + (int) smb.getBorders().getHeight() - borders.y);
    }

    public void setSymbolsMap(TreeMap<Integer, Symbol> symbolsMap) {
        this.symbolsMap = symbolsMap;
    }

    public TreeMap<Integer, Symbol> getSymbols() {
        return symbolsMap;
    }

    public TreeMap<Integer, Integer> getWordHistogramMap() {
        return histogramMap;
    }

    @Override
    public String toString() {
        return elementId + " - (" + borders.x + "," + borders.y + "," + borders.width + "," + borders.height + ")";
    }

    // Gets average height of the symbols
    public int getAverageWordHeight() {
        int heightSum = 0;
        Iterator<Symbol> it = symbolsMap.values().iterator();
        while (it.hasNext()) {
            Symbol symbol = it.next();
            heightSum += symbol.getBorders().getHeight();
        }

        return heightSum / symbolsMap.size();
    }

    /**
     * This method gets information about the space between the characters
     * @return includes 3 values with next keys:
     * MINIMAL_SPACE - minimal space between 2 characters
     * MAXIMAL_SPACE - maximal space between 2 characters
     * AVERAGE_SPACE - average space in between neighboring characters that word consist of.
     */
    public HashMap<String, Integer> getSpaceInfo() {
        HashMap<String, Integer> hmap = new HashMap<String, Integer>();
        int minSpace = 0, maxSpace = 0, avgSpace = 0;
        int prevX = 0;
        int spaceDistance = 0;

        Iterator it = symbolsMap.keySet().iterator();

        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Symbol symbol = (Symbol) symbolsMap.get(key);

            if (prevX == 0) {
                prevX = (int) (symbol.getBorders().getX() + symbol.getBorders().getWidth());
            } else {
                spaceDistance = (int) symbol.getBorders().getX() - prevX;
                prevX = (int) (symbol.getBorders().getX() + symbol.getBorders().getWidth());
                if (spaceDistance < minSpace) {
                    minSpace = spaceDistance;
                } else if (spaceDistance >= maxSpace) {
                    maxSpace = spaceDistance;
                }
                avgSpace += spaceDistance;
                symbol.setSpaceWithPreviousSymbol(spaceDistance);
                symbolsMap.put(key, symbol);
            }
        }

        setSymbolsMap(symbolsMap);

        avgSpace = avgSpace / symbolsMap.size();

        hmap.put("MINIMAL_SPACE", minSpace);
        hmap.put("MAXIMAL_SPACE", maxSpace);
        hmap.put("AVERAGE_SPACE", avgSpace);
        return hmap;
    }

    public void setSymbolTypes() {
        //set properties to symbols
        Iterator it = symbolsMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            Symbol smb = (Symbol) symbolsMap.get(key);

            if ((smb.getsType()==SymbolType.DIACRITIC) || (smb.getsType()==SymbolType.DIACRITIC))
                continue;
            // calculate ascending and descending parts
            int ascPart = upperBaseline - smb.getBorders().y;
            int descPart = smb.getBorders().y + smb.getBorders().height - bottomBaseline;

            if (ascPart > smb.getBorders().getHeight() * ASCDESC_THRES) {
                smb.setsType(SymbolType.ASCENDING);
            } else if (descPart > smb.getBorders().getHeight() * ASCDESC_THRES) {
                smb.setsType(SymbolType.DESCENDING);
            }

            symbolsMap.put(key, smb);
        }

    }

    @Override
    public void calculateHistogram(boolean bFilled) {
        getSubElementBasedHistogram(bFilled);
        //calculateBaselines();
    }
}
