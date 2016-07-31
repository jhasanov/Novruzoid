/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segment;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

import utils.Rectangle;

/**
 *
 * @author jamal
 */
public class SegmentedElement {

    private int seId;
    private int lineId;
    private Rectangle borders;
    private List<Point> pixels = new ArrayList<Point>();

    public Rectangle getBorders() {
        return borders;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public int getSeId() {
        return seId;
    }

    public void setSeId(int seId) {
        this.seId = seId;
    }

    public void addPixel(Point p) {
        borders.x       = Math.min(borders.x, p.x);
        borders.y       = Math.min(borders.y, p.y);
        borders.height  = Math.max(borders.height, p.x - borders.x);
        borders.width   = Math.max(borders.width, p.y - borders.y);

        pixels.add(p);
    }
}
