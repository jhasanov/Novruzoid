/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment.elements;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 *
 * @author itjamal
 */
public class TextLine extends Element {

    private TreeMap<Integer, Word> wordsMap = new TreeMap();

    public TreeMap getSubElements() {
        return wordsMap;
    }

    public void addWord(Integer xPosition, Word word) {
        wordsMap.put(xPosition, word);
    }

    public void mergeWords(Word wordToBeAdded) {
        Word word = wordsMap.get(0);
        if (word == null) {
            word = new Word();
            word.setBorders(borders);
            word.setElementId(0);
        }
        TreeMap symbols = wordToBeAdded.getSymbols();
        Iterator it = symbols.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            System.out.println("Meging: KEY=" + key + " Symbol=" + symbols.get(key));
            word.addSymbol(key, (Symbol) symbols.get(key));
        }

        //word.getSymbols().putAll(symbols);
        wordsMap.put(0, word);
    }

    public void addSymbol(Integer xPosition, Symbol smb) {
        Word word = wordsMap.get(0);
        if (word == null) {
            word = new Word();
            word.setBorders(borders);
            word.setElementId(0);
        }
        word.addSymbol(xPosition, smb);
        wordsMap.put(0, word);
    }

    public TreeMap<Integer, Word> getWords() {
        return wordsMap;
    }

    public void setWordsMap(TreeMap<Integer, Word> wordsMap) {
        this.wordsMap = wordsMap;
    }

    // gets the average height of the words
    public int getAverageTextLineHeight() {
        int heightSum = 0;
        Iterator<Word> it = wordsMap.values().iterator();
        while (it.hasNext()) {
            Word word = it.next();
            heightSum += word.getAverageWordHeight();
        }

        return heightSum / wordsMap.size();
    }

    public void splitToWords(boolean bSymbolWidthBased) throws Exception {
        if ((wordsMap == null) || (wordsMap.size() == 0)) {
            throw new Exception("TextLine should not contain empty words for this operation.");
        } else if (wordsMap.size() > 1) {
            throw new Exception("TextLine contains more than 1 word - probably this operation has already been performed");
        } else if (wordsMap.size() == 1) {
            /**
             * Calculating the histogram for the text line. There is no problem of 
             * calculation the histogram here - it doesn't matter how words are 
             * organized in text line. It doesn't affect the text line's histogram.
             */
            calculateHistogram(true);
            //System.out.println("Histogram MAX hist VAL = " + this.getMaxHistogramVal());
            //System.out.println("Upper: " + this.getUpperBaseline());
            //System.out.println("Bottom: " + this.getBottomBaseline());


            // Splitting to words
            TreeMap newWordsMap = new TreeMap();
            int wordIdx = 0;
            int spaceWidth = 0;

            // Getting word (the only, unsplit word. At the start line has only 1 word)
            Word word = wordsMap.values().iterator().next();

            /**
             * Finding average space width. We use 2 approaches (given in parameter):
             * 1. Space width is as an average symbol width
             * 2. Space width is a mean of average and maximum space.
             */
            HashMap<String, Integer> spaceInfo = word.getSpaceInfo();
            if (bSymbolWidthBased) {
                spaceWidth = Element.AVERAGE_SYMBOL_WIDTH;
            } else {
                // getting average space width
                // If space between symbols is more than avgSpace*1.5, then it's a word delimeter
                int avgSpace = spaceInfo.get("AVERAGE_SPACE");
                int maxSpace = spaceInfo.get("MAXIMAL_SPACE");
                spaceWidth = (maxSpace + avgSpace) / 2;
            }



            // Iterating through all symbols of the word.
            TreeMap<Integer, Symbol> symbolsMap = word.getSymbols();
            Iterator it = symbolsMap.keySet().iterator();
            Word newWord = new Word();
            int dotCnt = 0;
            while (it.hasNext()) {
                Integer key = (Integer) it.next();
                Symbol symbol = symbolsMap.get(key);

                dotCnt = 0;

                /**
                 * Checking for dots and diacritic signs.
                 * a) All dots and diacritic signs should have a small size (proportional to AVERAGE_SYMBOL_WIDTH)
                 * b) Diacritic signs should be on the top 
                 * c) Dots should be on bottom and have more pixel density
                 */
/*                System.out.println("Sympol param: w=" + symbol.getBorders().getWidth()
                + " h=" + symbol.getBorders().getHeight()
                + " d=" + symbol.getPixelDensity()
                + " X=" + symbol.getBorders().getX()        
                + " Y=" + symbol.getBorders().getY());
*/
                if ((symbol.getBorders().getWidth() < 0.5 * Element.AVERAGE_SYMBOL_WIDTH)
                        && (symbol.getBorders().getHeight() < 0.5 * Element.AVERAGE_SYMBOL_WIDTH)
                        && (symbol.getPixelDensity() > 0.70)) {
  
                    if ((symbol.getBorders().getY() + symbol.getBorders().getHeight()) < (upperBaseline + bottomBaseline) / 2) {
                        symbol.setsType(SymbolType.DIACRITIC);
                    } else {
                        symbol.setsType(SymbolType.DOT);
                        dotCnt++;
                    }
                    
                }

                // truncating the word and adding it to map.
                if (symbol.getSpaceWithPreviousSymbol() > spaceWidth) {
                    newWord.setDotCnt(dotCnt);
                    newWord.setUpperBaseline(upperBaseline);
                    newWord.setBottomBaseline(bottomBaseline);
                    newWord.setSymbolTypes();
                    newWordsMap.put(wordIdx++, newWord);
                    newWord = new Word();
                }
                newWord.addSymbol(key, symbol);
            }
            // Now we have a histogram (should be called at the start of this method) and we can calcualte the baselines.
            newWord.setDotCnt(dotCnt);
            newWord.setUpperBaseline(upperBaseline);
            newWord.setBottomBaseline(bottomBaseline);
            newWord.setSymbolTypes();
            newWordsMap.put(wordIdx++, newWord);
            setWordsMap(newWordsMap);
        }
    }

    @Override
    public void calculateHistogram(boolean bFilled) {
        getSubElementBasedHistogram(bFilled);
        calculateBaselines();
    }
}
