package ocr.svm.libsvm;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by itjamal on 7/24/2016.
 */
public class SvmTrain {


    // trains and return SVM model
    public svm_model train(double[][] trainData) {
        svm_model svmModel;

        // Get training data in LibSVM format
        svm_problem svmProblem = arrToSvmProblem(trainData);

        // Set parameters of the SVM with RBF kernel
        svm_parameter params = new svm_parameter();
        params.svm_type = svm_parameter.C_SVC;
        params.kernel_type = svm_parameter.RBF;
        params.gamma = 0.23;
        params.C = 14;
        params.eps = 0.0001;

        svmModel = svm.svm_train(svmProblem, params);

        return svmModel;
    }

    /*
    Convert 2D training (or test) data array to LibSVM format. See description:
   	struct svm_problem
	{
		int l;
		double *y;
		struct svm_node **x;
	};

    where `l' is the number of training data, and `y' is an array containing
    their target values. (integers in classification, real numbers in
    regression) `x' is an array of pointers, each of which points to a sparse
    representation (array of svm_node) of one training vector.

    For example, if we have the following training data:

    LABEL    ATTR1    ATTR2    ATTR3    ATTR4    ATTR5
    -----    -----    -----    -----    -----    -----
      1        0        0.1      0.2      0        0
      2        0        0.1      0.3     -1.2      0
      1        0.4      0        0        0        0
      2        0        0.1      0        1.4      0.5
      3       -0.1     -0.2      0.1      1.1      0.1

    then the components of svm_problem are:

    l = 5

    y -> 1 2 1 2 3

    x -> [ ] -> (2,0.1) (3,0.2) (-1,?)
         [ ] -> (2,0.1) (3,0.3) (4,-1.2) (-1,?)
         [ ] -> (1,0.4) (-1,?)
         [ ] -> (2,0.1) (4,1.4) (5,0.5) (-1,?)
         [ ] -> (1,-0.1) (2,-0.2) (3,0.1) (4,1.1) (5,0.1) (-1,?)

    where (index,value) is stored in the structure `svm_node':

	struct svm_node
	{
		int index;
		double value;
	};

    index = -1 indicates the end of one vector. Note that indices must
    be in ASCENDING order.
     */
    public svm_problem arrToSvmProblem(double[][] dataArr) {
        svm_problem svmProblem = new svm_problem();

        int sampleCnt = dataArr.length;
        int featureCnt = dataArr[0].length - 1; // first column is Y values

        // count of training samples
        svmProblem.l = sampleCnt;

        // First column are Y values
        svmProblem.y = dataArr[0];

        // Setting X values
        svm_node[][] features = new svm_node[sampleCnt][];
        for (int i = 0; i < sampleCnt; i++) {
            for (int j = 1; i <= featureCnt; j++) { // "<=" sign is used to consider the last element (featureCnt is length-1)
                features[i][j - 1] = new svm_node();
                features[i][j - 1].index = j; // index of svm_node starts with 1, not 0.
                features[i][j - 1].value = dataArr[i][j];
            }
        }
        svmProblem.x = features;

        return svmProblem;
    }


    /*
    This method transforms the given image data to the standard size
     */
    public double[] getFeatures(Bitmap image) throws Exception {
        double [] features;
        final int IMAGE_WIDTH = 20; // width (also height) of the normalized image
        final int RATIO_COEF = 5; // coefficient of the ratio feature

        int height = image.getWidth();
        int width = image.getHeight();
        double ratio = (double) width / height;

        // resize image to 20x20 size
        Matrix matrix = new Matrix();
        matrix.postScale((float) width / IMAGE_WIDTH, (float) height / IMAGE_WIDTH);
        Bitmap scaledBitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

        // get pixels of the resized image
        int[] pixels = new int[IMAGE_WIDTH ^ 2];
        scaledBitmap.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_WIDTH);


        features = new double[pixels.length+1];

        if (pixels.length != (IMAGE_WIDTH^2)) {
            throw new Exception("NovruzoidError->Pixel data doesn't fit required size : "+IMAGE_WIDTH);
        }

        // I couldn't find better way to copy int[x] array to double[x+1] array
        for (int i = 0; i < pixels.length; i++) {
                features[i] = pixels[i];
            }

        // last element is the ratio
        features[pixels.length] = RATIO_COEF*ratio;

    return features;
    }


}
