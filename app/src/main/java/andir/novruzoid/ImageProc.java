package andir.novruzoid;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.TreeMap;

import image.segment.ImageSegment;
import image.segment.elements.Column;

/**
 * Created by itjamal on 3/22/2016.
 */
public class ImageProc {


    static {
        System.loadLibrary("imageproc-jni");
    }


    public float [] getEdges() {
        float [] edges;

        edges = new float[]{59, 69, 169,69, 188,210,55,340};

        return edges;
    }

    public native String getMessageFromJni();

    //public native int [] getSegments(int [] pixels, int w, int h);
    public TreeMap<Integer,Column> getSegments(Bitmap image, int w, int h) {
        ImageSegment imgSeg = new ImageSegment();
        Log.i(this.getClass().toString(), "Calling processImage");
        imgSeg.processImage(image, true);
        Log.i(this.getClass().toString(), "Calling segmentImage");
        imgSeg.segmentImage();
        Log.i(this.getClass().toString(), "Calling postSegmentProcess");
        imgSeg.postSegmentProcess(false);

        TreeMap<Integer, Column> columnsMap;
        columnsMap = imgSeg.getColumnsMap();
        Log.i(this.getClass().toString(), "Segmentation finished");

        return columnsMap;
    }

}
