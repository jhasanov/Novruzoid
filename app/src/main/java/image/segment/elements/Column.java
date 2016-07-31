/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package image.segment.elements;

import java.util.TreeMap;

import utils.Rectangle;

/**
 *
 * @author itjamal
 */
public class Column {
    private int columnId;
    private Rectangle borders;
    private TreeMap<Integer,TextLine> lines = new TreeMap();

    public Rectangle getBorders() {
        return borders;
    }

    public void setBorders(Rectangle borders) {
        this.borders = borders;
    }

    public int getColumnID() {
        return columnId;
    }

    public void setColumnID(int columnID) {
        this.columnId = columnID;
    }

    public TreeMap<Integer,TextLine> getLines() {
        return lines;
    }

    public void setLines(TreeMap lines) {
        this.lines = lines;
    }

    public TextLine getTextLine(Integer lineIdx) {
        return lines.get(lineIdx);
    }

    public void setTextLine(Integer lineIdx,TextLine textLine) {
        lines.put(lineIdx,textLine);
    }

    @Override
    public String toString() {
        return columnId + " - ("+borders.x+","+borders.y+","+borders.width+","+borders.height+")";
    }
}
