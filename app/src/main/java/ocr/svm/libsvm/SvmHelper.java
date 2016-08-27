package ocr.svm.libsvm;

import java.io.IOException;

import ocr.LabelManager;

/**
 * Created by itjamal on 7/24/2016.
 */
public class SvmHelper {


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
        svmProblem.y = new double[dataArr.length];

        for (int i = 0; i < dataArr.length; i++)
            svmProblem.y[i] = dataArr[i][0];


        // Setting X values
        svm_node[][] features = new svm_node[sampleCnt][featureCnt];
        System.out.println("Sample count: " + sampleCnt + ", Feature count: " + featureCnt);
        for (int i = 0; i < sampleCnt; i++) {
            int featIdx = 0;

            for (int j = 1; j <= featureCnt; j++) { // "<=" sign is used to consider the last element (featureCnt is length-1)
                features[i][featIdx] = new svm_node();
                features[i][featIdx].index = j; // index of svm_node starts with 1, not 0.
                features[i][featIdx].value = dataArr[i][j - 1];
                featIdx++;
            }
            /*
            features[i][featIdx] = new svm_node();
            features[i][featIdx].index = -1; // the end of features.
            features[i][featIdx].value = 0;
            */
        }
        svmProblem.x = features;

        return svmProblem;
    }

    // Converts features (for the recognition, without given Y value) to a SVM_PROBLEM format
    public static svm_node[] featuresToSvmNodes(double[] features) {
        svm_problem svmProblem = new svm_problem();

        int featureCnt = features.length; // there's no Y in the 1st column

        svm_node[] nodes = new svm_node[featureCnt];

        int featIdx = 0;

        for (int i = 0; i < featureCnt; i++) {
            nodes[i] = new svm_node();
            nodes[i].index = i + 1; // index of svm_node starts with 1, not 0.
            nodes[i].value = features[i];
        }

        return nodes;
    }

    public static void main(String[] args) {
        SvmHelper capitalSvm = new SvmHelper();
        double[][] trainingSamples = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP", LabelManager.LabelTypeEnum.DIGITS);

        long l = System.currentTimeMillis();
        System.out.println("Training started. Elements: " + trainingSamples.length + ", features: " + trainingSamples[0].length);
        svm_model capitalSvmModel = capitalSvm.train(trainingSamples);
        try {
            svm.svm_save_model("D:\\Dropbox\\TEMP\\JML_DIGITS_model", capitalSvmModel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Training finished. Duration : " + (int) ((System.currentTimeMillis() - l) / 1000) + " seconds");
    }

}
