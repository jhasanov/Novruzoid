package image;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by itjamal on 11/7/2016.
 */
public class BorderDetection {

    final int maskSize = 31;
    // 31 x 31 binary matrix, where each row is represented as integer (built from 31-bit binary)
    private int[] maskLT = new int[maskSize];
    ArrayList borders = new ArrayList();

    public BorderDetection() {
        initMask();
    }

    // This method sets variables to 31-element int array.
    private void initMask() {
        for (int i = 0; i < 8; i++)
            maskLT[i] = 2147483647;
        for (int i = 8; i < maskLT.length; i++)
            maskLT[i] = 2146435072;
    }

    private void printBinIntArr(int[] binIntArr) {
        if (binIntArr == null)
            binIntArr = maskLT;

        for (int i = 0; i < binIntArr.length; i++)
            System.out.println(Integer.toBinaryString(binIntArr[i]));
    }

    public float[] findBorders(ImageInfo imageInfo) {
        float[] borders = new float[8];
        int minDiff = Integer.MAX_VALUE;

        Log.d("BD", "width=" + imageInfo.getPixelArrH().length);

        for (int i = 0; i < imageInfo.getPixelArrH().length; i++) {
            // This loop doesn't cover the last piece in the bottom (if size of it is less than maskSize)
            for (int j = 0; j < imageInfo.getHeight() / maskSize; j++) {
                int currSum = 0;
                for (int k = 0; k < maskSize; k++) {
                    int val = maskLT[k] ^ imageInfo.getPixelArrH()[i][j * maskSize + k];
                    currSum += Integer.bitCount(val);
                }
                if (currSum < minDiff) {
                    borders[0] = i * maskSize;
                    borders[1] = j * maskSize;
                    minDiff = currSum;
                }
                Log.d("BD", "x=" + borders[0] + "; y=" + borders[1] + "; MinDiff=" + minDiff + "; currVal=" + currSum);
            }

            // One more lookup for the bottom part
            /*for (int j = imageInfo.getHeight()-maskSize; j < imageInfo.getHeight(); j++) {
                int currSum = 0;
                for (int k = 0; k < maskSize; k++) {
                    int val = maskLT[k] & imageInfo.getPixelArrH()[i][j + k];
                    currSum += Integer.bitCount(val);
                }
                if (currSum > maxVal) {
                    borders[0]=i*31;
                    borders[1]=j;
                    maxVal = currSum;
                }
            }*/

        }

        return borders;
    }

    public static void main(String[] args) {
        BorderDetection brdDetect = new BorderDetection();
        brdDetect.printBinIntArr(null);
        int a = 2147450880;

        int b = 2147483647;
        int c = 2146435072;

        int val = a ^ b;
        System.out.println("a ^ b = " + Integer.toBinaryString(val) + "; val=" + Integer.bitCount(val));
        val = a ^ c;
        System.out.println("a ^ c = " + Integer.toBinaryString(val) + "; val=" + Integer.bitCount(val));

    }
}
