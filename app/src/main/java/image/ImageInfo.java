/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;


import android.graphics.Bitmap;

/**
 *
 * @author jamal
 */
public abstract class ImageInfo {
    //private enum {BYTE, BINARY};

    private static final long serialVersionUID = 0;
    
    int width;
    int height;
    int[][] pixelArrH; // array in which the horizontal pixels are compressed
    int[][] pixelArrV; // array in which the vertical pixels are compressed

    public int[][] getPixelArrH() {
        return pixelArrH;
    }

    public int[][] getPixelArrV() {
        return pixelArrV;
    }
    
    public void setPixelArrH(int[][] pArrH) {
        pixelArrH = pArrH;
    }

    public void setPixelArrV(int[][] pArrV) {
        pixelArrV = pArrV;
    }
    

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }
    
    public abstract void setInfoType(int infoType);

    public abstract Bitmap getBitmapImage();

}
