package utils;

/**
 * Created by itjamal on 8/10/2016.
 * This helper class is created to support matrix operations that are not included in standard Java library
 */
public class MatrixOperations {

    // transforms 2-D array (matrix) into a 1-D array(vector)
    public static double[] oneDimensional(double[][] source) {
        int arrLen = 0;

        for (int i = 0; i < source.length; i++)
            arrLen += source[i].length;

        double[] resultArr = new double[arrLen];

        int offset = 0;
        for (int i = 0; i < source.length; i++) {
            if (i > 0)
                offset += source[i - 1].length;
            System.arraycopy(source[i], 0, resultArr, offset, source[i].length);
        }
        return resultArr;
    }

    // adds elem to source array
    public static double[] addElement(double[] source, double elem) {
        double[] resultArr = new double[source.length + 1];

        System.arraycopy(source, 0, resultArr, 0, source.length);
        resultArr[source.length] = elem;

        return resultArr;
    }

    // method merges arr1 and arr2
    public static double[] mergeArrays(double[] arr1, double[] arr2) {
        double[] resultArr = new double[arr1.length + arr2.length];

        System.arraycopy(arr1, 0, resultArr, 0, arr1.length);
        System.arraycopy(arr2, 0, resultArr, arr1.length, arr2.length);

        return resultArr;
    }

    public static void main(String[] args) {
        double[][] test = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};


        double[] res = MatrixOperations.oneDimensional(test);

        for (int i = 0; i < res.length; i++) {
            System.out.print((int) res[i] + " ");
        }
        System.out.println();

        double[][] arr = new double[3][];
        arr[0] = new double[]{69.9, 79.0, 89.0};
        arr[1] = res;
        arr[2] = new double[]{5 * 1.0 / 6};

        double[] res2 = MatrixOperations.oneDimensional(arr);
        for (int i = 0; i < res2.length; i++) {
            System.out.print(res2[i] + " ");
        }

    }


}

