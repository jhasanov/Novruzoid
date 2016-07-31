/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;

import android.graphics.Bitmap;

/**
 *
 * @author itjamal
 */
public interface AndroidImageProcessor {
        public abstract void convertToPixelArr(Bitmap image, int thres,boolean bType);

    public abstract Bitmap getBitmapImage();
}
