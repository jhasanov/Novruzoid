package ocr.svm.libsvm;

import java.io.IOException;
import java.util.Arrays;

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
        params.kernel_type = svm_parameter.POLY;
        params.gamma = 20;
        params.C = 0.125;
        params.degree = 2;
        params.coef0 = 0.00003;
        params.eps = 0.0001;

        svmModel = svm.svm_train(svmProblem, params);

        return svmModel;
    }

    // tries different oiptions and chooses best SVM model
    public svm_model CrossValidateTrain(double[][] trainData) {
        svm_model svmModel = null;
        double bestAccuracy = 0.0;
        String bestParams = "";

        // Get training data in LibSVM format
        svm_problem svmProblem = arrToSvmProblem(trainData);

        double[] target = new double[svmProblem.l];

        int[] svmTypeArr = {svm_parameter.POLY, svm_parameter.RBF};
        double[] CArr = {8, 32, 128};
        double[] gammaArr = {0.001, 0.01, 0.1, 1, 10};
        int[] degreeArr = {2, 3, 4};
        double[] coef0Arr = {-5, 2, 8, 20};

        svm_parameter params = new svm_parameter();
        params.svm_type = svm_parameter.C_SVC;
        params.eps = 0.0001;


        // try polynomial: (gamma*u'*v + coef0)^degree
        params.kernel_type = svm_parameter.POLY;
        for (double C : CArr) {
            for (double gamma : gammaArr) {
                for (double coef0 : coef0Arr) {
                    for (int degree : degreeArr) {
                        params.coef0 = coef0;
                        params.gamma = gamma;
                        params.C = C;
                        params.degree = degree;

                        svm.svm_cross_validation(svmProblem, params, 5, target);


                        double accuracy = calcAccuracy(target, svmProblem.y);
                        String paramStr = "Cross validation: C=" + C + "; gamma=" + gamma + "; coeg0=" + coef0 + "; degree=" + degree;
                        if (accuracy > bestAccuracy) {
                            bestAccuracy = accuracy;
                            bestParams = paramStr;
                        }
                        System.out.println(paramStr + ". Result: " + accuracy * 100 + "%" + "; Best accuracy: " + bestAccuracy * 100 + "%");
                    }
                }

            }
        }


        // TODO: try RBF: exp(-gamma*|u-v|^2)
/*        params.kernel_type = svm_parameter.RBF;
        for (double C : CArr) {
            for (double gamma : gammaArr) {
                params.gamma = gamma;
                params.C = C;

                svm.svm_cross_validation(svmProblem, params, 2, target);


                double accuracy = calcAccuracy(target, svmProblem.y);
                String paramStr = "Cross validation: C=" + C + "; gamma=" + gamma;
                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestParams = paramStr;
                }
                System.out.println(paramStr + ". Result: " + accuracy * 100 + "%" + "; Best accuracy: " + bestAccuracy * 100 + "%");
            }
        }
*/
        System.out.println("Best accuracy: " + bestAccuracy * 100 + "%. Params:" + bestParams);
        return svmModel;
    }


    // Calculates the result of accuracy
    double calcAccuracy(double[] result, double[] desired) {
        int correct = 0;

        for (int i = 0; i < result.length; i++) {
            if (result[i] == desired[i])
                correct++;
        }

        return correct * 1.0 / result.length;
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

        System.out.println("arg[0] is: " + args[0] + "\n");


        if ((args.length < 1) || (args[0] == null)) {
            System.out.println("Please pass a 'train'/'test' parameter. \n");
        } else if (args[0].equals("train")) {
            double[][] trainingSamples = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP", LabelManager.LabelTypeEnum.CAPITAL);

            long l = System.currentTimeMillis();
            System.out.println("Training started. Elements: " + trainingSamples.length + ", features: " + trainingSamples[0].length);
            svm_model capitalSvmModel = capitalSvm.train(trainingSamples);
            //svm_model capitalSvmModel = capitalSvm.CrossValidateTrain(trainingSamples);
            try {
                svm.svm_save_model("D:\\Dropbox\\TEMP\\JML_CAPITAL_model", capitalSvmModel);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Training finished. Duration : " + (int) ((System.currentTimeMillis() - l) / 1000) + " seconds");
        } else if (args[0].equals("test")) {
            System.out.println("Performing test...\n");

            String modelFile = "D:\\Dropbox\\TEMP\\JML_CAPITAL_model";
            double[][] trainingSamples = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP", LabelManager.LabelTypeEnum.CAPITAL);
            int positives = 0;
            int samples = trainingSamples.length;
            int featlen = trainingSamples[0].length;

            try {
                svm_model model = svm.svm_load_model(modelFile);
                for (double[] features : trainingSamples) {
                    double desiredClass = features[0];
                    svm_node[] nodes = featuresToSvmNodes(Arrays.copyOfRange(features, 1, featlen));
                    double classId = svm.svm_predict(model, nodes);
                    if (desiredClass == classId) {
                        positives++;
                    }
                }
                System.out.println("Test result: " + (positives * 100 / samples) + "% (" + positives + "/" + samples + ")");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
