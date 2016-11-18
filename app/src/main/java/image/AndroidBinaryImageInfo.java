/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 *
 * @author jamal
 */
public class AndroidBinaryImageInfo extends ImageInfo implements AndroidImageProcessor {

    private int SQUARE_SIZE = 0;

    /**
     * Calculates the mean threshold values.
     *
     * From "Digital Image Processing: a practical introduction using Java(2000)" book.
     * Page 279, algorithm 10.1
     *
     * @param colorArr array of bytes of the source grayscale image
     * @param squareSize size of subimage where thresholding is performed
     * @param i X position of the subimage. There are image_width/squareSize count of subimages in X axis.
     * This parameters indicates the index of the current index.t
     * @param j Y position of the subimage (see param i).
     * @param thres threshold value. In first iteration its value is 0 and
     * in this case as minimum average, a mean value of the corner pixels are calculated.
     * @return array on 2 ints where:
     * 1st element is the mean gray level of pixels for which f(x,y) < thres
     * 2nd element is the mean gray level of pixels for which f(x,y) >= thres.
     */
    private int[] getMinMaxGrayscale(int[] colorArr, int squareSize, int i, int j, int thres) {
        int[] retval = new int[2];

        int minSum = 0;
        int minCnt = 1;
        int maxSum = 0;
        int maxCnt = 1;

        // initial phase - get only the mean values of the corners
        if (thres == 0) {
            int leftTop = colorArr[(j * squareSize + 0) * width + (i * squareSize + 0)] & 0xFF;
            int rightTop = colorArr[(j * squareSize + 0) * width + ((i + 1) * squareSize - 1)] & 0xFF;
            int leftBtm = colorArr[((j + 1) * squareSize - 1) * width + (i * squareSize + 0)] & 0xFF;
            int rightBtm = colorArr[((j + 1) * squareSize - 1) * width + ((i + 1) * squareSize - 1)] & 0xFF;

            //minSum = (leftTop + rightTop + leftBtm + rightBtm);
            minSum = 4 * 255;
            minCnt = 4;
        }

        for (int y = 0; y < squareSize; y++) {
            for (int x = 0; x < squareSize; x++) {
                int grayVal = colorArr[(j * squareSize + y) * width + (i * squareSize + x)] & 0xFF;
                if (grayVal < thres) {
                    minSum += grayVal;
                    minCnt++;
                } else {
                    maxSum += grayVal;
                    maxCnt++;
                }
            }
        }

        retval[0] = minSum / minCnt;
        retval[1] = maxSum / maxCnt;
        return retval;
    }

    @SuppressLint("LongLogTag")
    public void convertToPixelArr(Bitmap image) {
        try {

            setHeight(image.getHeight());
            setWidth(image.getWidth());
            int partSize = Math.min(height, SQUARE_SIZE);
            Log.d("convertToPixelArr(Bitmap)", "HEIGHT=" + height + "; WIDTH=" + width);

            pixelArrH = new int[(int) Math.ceil(width / 31.0)][height];
            pixelArrV = new int[width][(int) Math.ceil(height / 31.0)];

            int[] colArr = new int[width * partSize];

            Log.d("convertToPixelArr(Bitmap)", "colArr.size=" + colArr.length + "; partSize = " + partSize + "; height = " + height / partSize);
            Log.d("convertToPixelArr(Bitmap)", "pixelArrH[" + pixelArrH.length + "," + pixelArrH[0].length + "]");
            Log.d("convertToPixelArr(Bitmap)", "pixelArrV[" + pixelArrV.length + "," + pixelArrV[0].length + "]");

            for (int i = 0; i < (height / partSize); i++) {
                Log.d("convertToPixelArr(Bitmap)", "Getting part : " + i);

                image.getPixels(colArr, 0, width, 0, i * partSize, width, partSize);
                Log.d("convertToPixelArr(Bitmap)", "loaded into colArr");

                convertToPixelArr(i, colArr);
            }

            Log.d("convertToPixelArr()", "pixels loaded into colArr");


        } catch (Exception ex) {
            Log.i("convertToPixelArr()", "Exception: " + ex);
        }
    }

    /**
     * Thresholds the parts of the grayscale image.
     * These parts are squares characterized by squareSize parameter.
     *
     * From "Digital Image Processing: a practical introduction using Java(2000)" book.
     * Page 279, algorithm 10.1
     * 
     */
    public void convertToPixelArr(int partIdx, int[] colArr) {
        int imgPartHeight = colArr.length / width;

        try {

            int[][] thresArr = new int[width / SQUARE_SIZE][height / SQUARE_SIZE];

            /* The new arrays
             * They store pixel imformation as a chunk of bits - in one integer
             * the data of 31 pixels is stored.
             * in pixelArrH array, the horizontal pixels grouped by 31
             * in pixelArrV array, the vertical pixels grouped by 31.
             */


            // this byte holds 31 pixel bits: 1 - black, 0 - white.
            int bitChunk = 0;
            int idx = 0;

            Log.d("convertToPixelArr()", "Starting loop");

            // thresholding by 31/31 squares.
            for (int j = 0; j < imgPartHeight / SQUARE_SIZE; j++) {
                //Log.i("convertToPixelArr()", "j = " + j);
                for (int i = 0; i < width / SQUARE_SIZE; i++) {

                    // finding best threshold value
                    int tOld = 0, tNew = 0;
                    int[] minMaxVal = getMinMaxGrayscale(colArr, SQUARE_SIZE, i, j, 0);
                    tNew = (minMaxVal[0] + minMaxVal[1]) / 2;
                    int cnt = 0;
                    while ((cnt++ == 10) || (tOld != tNew)) {
                        minMaxVal = getMinMaxGrayscale(colArr, SQUARE_SIZE, i, j, tNew);
                        tOld = tNew;
                        tNew = (minMaxVal[0] + minMaxVal[1]) / 2;
                    }

                    thresArr[i][j] = tNew;

                    // Setting values for horizontal array
                    for (int y = 0; y < SQUARE_SIZE; y++) {
                        for (int x = 0; x < SQUARE_SIZE; x++) {
                            if ((colArr[(j * SQUARE_SIZE + y) * width + (i * SQUARE_SIZE + x)] & 0xFF) < tNew) {
                                bitChunk = bitChunk + (int) Math.pow(2, 30 - idx);
                            }

                            if ((idx == 30) || (i == width - 1)) {
                                //Log.d("convertToPixelArr()", "pixelArrH[" + (i * (SQUARE_SIZE / 31) + (x / 31)) + "," + (partIdx * imgPartHeight + j * SQUARE_SIZE + y) + "]");
                                pixelArrH[i * (SQUARE_SIZE / 31) + (x / 31)][partIdx * imgPartHeight + j * SQUARE_SIZE + y] = bitChunk;
                                bitChunk = 0;
                                idx = 0;
                            } else {
                                idx++;
                            }

                        }
                    }



                    // Setting values for vertical array
                    for (int x = 0; x < SQUARE_SIZE; x++) {
                        for (int y = 0; y < SQUARE_SIZE; y++) {
                            if ((colArr[(j * SQUARE_SIZE + y) * width + (i * SQUARE_SIZE + x)] & 0xFF) < tNew) {
                                bitChunk = bitChunk + (int) Math.pow(2, 30 - idx);
                            }

                            if ((idx == 30) || (j == height - 1)) {
                                //Log.d("convertToPixelArr()", "pixelArrV[" + (i * SQUARE_SIZE + x) + "," + (partIdx * (imgPartHeight / SQUARE_SIZE) + j * (SQUARE_SIZE / 31) + (y / 31)) + "]");
                                pixelArrV[i * SQUARE_SIZE + x][partIdx * (imgPartHeight / SQUARE_SIZE) + j * (SQUARE_SIZE / 31) + (y / 31)] = bitChunk;
                                bitChunk = 0;
                                idx = 0;
                            } else {
                                idx++;
                            }

                        }
                    }

                }
            }
        } catch (Exception ex) {
            Log.i("convertToPixelArr()", "Exception: " + ex);

        }
    }

    /**
     * Thresholds the whole image by thres param.
     * If pixel's grayscale value is more than thres, them pixel is black.
     * Otherwise, it's white.
     * @param bimg source image
     * @param thres threshold value
     */
    @Override
    public void convertToPixelArr(Bitmap bimg,
            int thres, boolean bSquareBased) {
        Log.d("convertToPixelArr(...)", "HEIGHT=" + bimg.getHeight() + "; WIDTH=" + bimg.getWidth());

        // If captured in landscape (and probably rotated only in EXIF data)
        // then rotate it 90 degrees clockwise.
        // !!!! Commented by Jamal - image is already sent as rotated.
        /*if (bimg.getHeight() < bimg.getWidth()) {
            Log.d("convertToPixelArr(...)", "Rotating");
            Matrix mtrx = new Matrix();
            mtrx.postRotate(90);
            Bitmap bimg2 = Bitmap.createBitmap(bimg, 0, 0, bimg.getWidth(), bimg.getHeight(), mtrx, true);
            bimg = bimg2;
        }*/

        if (bSquareBased) {
            Log.d(this.getClass().toString(), "convertToPixelArr() -> SQUARE BASED");
            SQUARE_SIZE = thres * 31;
            convertToPixelArr(bimg);
        } else {
            Log.d(this.getClass().toString(), "convertToPixelArr() -> LINEAR");
            setHeight(bimg.getHeight());
            setWidth(bimg.getWidth());
            int[] pixelArr = new int[width * height];
            bimg.getPixels(pixelArr, 0, width, 0, 0, width, height);

            /* The new arrays
             * They store pixel imformation as a chunk of bits - in one integer
             * the data of 31 pixels is stored.
             * in pixelArrH array, the horizontal pixels grouped by 31
             * in pixelArrV array, the vertical pixels grouped by 31.
             */
            pixelArrH = new int[(int) Math.ceil(width / 31.0)][height];
            pixelArrV = new int[width][(int) Math.ceil(height / 31.0)];


            // this byte holds 31 pixel bits: 1 - black, 0 - white.
            int bitChunk = 0;
            // new index of width/height - width/height of array will be increased by 31 times.
            int newIdx = 0;


            for (int j = 0; j < height; j++) {
                newIdx = 0;
                //System.out.println();
                for (int i = 0; i < width; i++) {
                    //System.out.print((byteArr[j * width + i] & 0xFF)+";");
                    int idx = (i % 31);
                    if (idx == 0) {
                        bitChunk = 0;
                    }
                    //  Black pixel found.
                    if ((pixelArr[j * width + i] & 0xFF) < thres) {
                        bitChunk = bitChunk + (int) Math.pow(2, 30 - idx);
                        //System.out.print('1');
                    }
                    //else
                    //System.out.print('0');
                    if ((idx == 30) || (i == width - 1)) {
                        pixelArrH[newIdx++][j] = bitChunk;
                        //System.out.print(" -- "+bitChunk+"-"+Integer.toBinaryString(bitChunk)+ "; ");
                    }
                }
            }

            for (int i = 0; i < width; i++) {
                newIdx = 0;
                for (int j = 0; j < height; j++) {
                    int idx = (j % 31);
                    if (idx == 0) {
                        bitChunk = 0;
                    }
                    //  Black pixel found.
                    if ((pixelArr[j * width + i] & 0xFF) < thres) {
                        bitChunk = bitChunk + (int) Math.pow(2, 30 - idx);
                    }
                    if ((idx == 30) || (j == height - 1)) {
                        pixelArrV[i][newIdx++] = bitChunk;
                    }
                }

            }

        }

    }

    @Override
    public void setInfoType(int infoType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * This method is used for testing of the pixelArr data.
     * It will print "X" if byte value of pixelArr[i][j] consist of bit 1
     * and  "_" in case of bit 0.
     */
    public void testIt() {
        String s = "";
        Log.d(this.getClass().toString(), "Testing...");

        for (int j = 0; j < pixelArrH[0].length; j++) {
            for (int i = 0; i < pixelArrH.length; i++) {
                int b = pixelArrH[i][j];
                int ybyte = (int) Math.pow(2, 30);
                //System.out.print(b+"-"+Integer.toBinaryString(b));
                //System.out.print(b+"; ");

                for (int y = 30; y >= 0; ybyte = (int) Math.pow(2, --y)) {
                    if ((b & ybyte) == ybyte) {
                        s += "X";
                    } else {
                        s += "_";
                    }
                }
            }
            Log.d(this.getClass().toString(), s);
            s = "";

        }
    }

    @Override
    public Bitmap getBitmapImage() {
        int w = pixelArrH.length;// * 31;
        int h = pixelArrH[0].length;
        int[] colors = new int[w * 31 * height];
        Log.d(this.getClass().toString(), "Starting 3");
        Log.d(this.getClass().toString(), "w=" + w + "; h=" + h + "; colors.size=" + colors.length);

        Bitmap bimg = Bitmap.createBitmap( w*31, h, Bitmap.Config.ARGB_8888);
        
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int b = pixelArrH[i][j];
                int ybyte = (int) Math.pow(2, 30);

                for (int y = 30; y >= 0; ybyte = (int) Math.pow(2, --y)) {
                    int xAxis = i * 31 + (30 - y);

                    if ((b & ybyte) == ybyte) {
                        //colors[j * w + xAxis] = Color.BLACK;
                        bimg.setPixel(xAxis, j, Color.BLACK);
                        
                    } else {
                        //colors[j * w + xAxis] = Color.WHITE;
                        bimg.setPixel(xAxis, j, Color.WHITE);
                    }
                }
            }
        }

//        Bitmap bimg = Bitmap.createBitmap(colors, w*31, h, Bitmap.Config.ARGB_8888);
        return bimg;
    }
}
