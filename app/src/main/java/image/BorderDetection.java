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
    private int[] maskLB = new int[maskSize];
    private int[] maskRT = new int[maskSize];
    private int[] maskRB = new int[maskSize];
    ArrayList borders = new ArrayList();

    public BorderDetection() {
        initMask();
    }

    // This method sets variables to 31-element int array.
    private void initMask() {
        // Left top corner
        for (int i = 0; i < 11; i++)
            maskLT[i] = 2147483647;
        for (int i = 11; i < maskLT.length; i++)
            maskLT[i] = 2146435072;

        // Left bottom corner
        for (int i = 0; i < maskLB.length - 11; i++)
            maskLB[i] = 2146435072;
        for (int i = maskLB.length - 11; i < maskLB.length; i++)
            maskLB[i] = 2147483647;

        // Right top corner
        for (int i = 0; i < 11; i++)
            maskRT[i] = 2147483647;
        for (int i = 11; i < maskRT.length; i++)
            maskRT[i] = 2047;

        // Right bottom corner
        for (int i = 0; i < maskRB.length - 11; i++)
            maskRB[i] = 2047;
        for (int i = maskRB.length - 11; i < maskRB.length; i++)
            maskRB[i] = 2147483647;

    }

    private void printBinIntArr(int[] binIntArr) {
        if (binIntArr == null)
            binIntArr = maskLT;

        for (int i = 0; i < binIntArr.length; i++)
            System.out.println(Integer.toBinaryString(binIntArr[i]));
    }

    public float[] findBorders(ImageInfo imageInfo) {
        float[] borders = new float[8];
        int minLTDiff = Integer.MAX_VALUE;
        int minLBDiff = Integer.MAX_VALUE;
        int minRTDiff = Integer.MAX_VALUE;
        int minRBDiff = Integer.MAX_VALUE;

        Log.d("BD", "width=" + imageInfo.getPixelArrH().length);

        // start with 1, not 0 (to skip areas close to corner)
        for (int i = 1; i < imageInfo.getPixelArrH().length; i++) {
            // This loop doesn't cover the last piece in the bottom (if size of it is less than maskSize)
            for (int j = 1; j < imageInfo.getHeight() / maskSize; j++) {
                int currSumLT = 0;
                int currSumLB = 0;
                int currSumRT = 0;
                int currSumRB = 0;
                for (int k = 0; k < maskSize; k++) {
                    int val = maskLT[k] ^ imageInfo.getPixelArrH()[i][j * maskSize + k];
                    currSumLT += Integer.bitCount(val);
                    val = maskLB[k] ^ imageInfo.getPixelArrH()[i][j * maskSize + k];
                    currSumLB += Integer.bitCount(val);
                    val = maskRT[k] ^ imageInfo.getPixelArrH()[i][j * maskSize + k];
                    currSumRT += Integer.bitCount(val);
                    val = maskRB[k] ^ imageInfo.getPixelArrH()[i][j * maskSize + k];
                    currSumRB += Integer.bitCount(val);
                }
                if (currSumLT < minLTDiff) {
                    borders[0] = i * maskSize + maskSize / 2;
                    borders[1] = j * maskSize + maskSize / 2;
                    minLTDiff = currSumLT;
                }
                if (currSumRT < minRTDiff) {
                    borders[2] = i * maskSize + maskSize / 2;
                    borders[3] = j * maskSize + maskSize / 2;
                    minRTDiff = currSumRT;
                }
                if (currSumRB < minRBDiff) {
                    borders[4] = i * maskSize + maskSize / 2;
                    borders[5] = j * maskSize + maskSize / 2;
                    minRBDiff = currSumRB;
                }
                if (currSumLB < minLBDiff) {
                    borders[6] = i * maskSize + maskSize / 2;
                    borders[7] = j * maskSize + maskSize / 2;
                    minLBDiff = currSumLB;
                }
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
        brdDetect.printBinIntArr(brdDetect.maskLT);
        brdDetect.printBinIntArr(brdDetect.maskLB);
        brdDetect.printBinIntArr(brdDetect.maskRT);
        brdDetect.printBinIntArr(brdDetect.maskRB);
    }
}
