package ocr.svm.libsvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

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
        params.degree = 2;
        params.gamma = 0.03125;
        params.coef0 = 20;
        params.C = 0.03125;
        params.eps = 0.00001;

        svmModel = svm.svm_train(svmProblem, params);

        return svmModel;
    }

    // tries different oiptions and chooses best SVM model
    public svm_model CrossValidateTrain(double[][] trainData, double[][] testData) {
        svm_model svmModel = null;
        double bestAccuracy = 0.0;
        String bestParams = "";

        // Get training data in LibSVM format
        svm_problem svmProblem = arrToSvmProblem(trainData);

        double[] target = new double[svmProblem.l];

        int[] svmTypeArr = {svm_parameter.POLY, svm_parameter.RBF};
        double[] CArr = {0.03125};//{8, 32, 50, 100};
        double[] gammaArr = {0.03125};//{0,05, 0.03,0.01};
        int[] degreeArr = {2};//{2, 3, 4};
        double[] coef0Arr = {20};//{8, 20,30};

        svm_parameter params = new svm_parameter();
        params.svm_type = svm_parameter.C_SVC;
        params.eps = 0.0001;

        svm_model model;


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

                        model = svm.svm_train(svmProblem, params);

                        double accuracy = test(model, testData);
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

    // Returns the accuracy of test
    double test(svm_model model, double[][] testData) {
        int positives = 0;
        int samples = testData.length;
        int featlen = testData[0].length;

        for (double[] features : testData) {
            double desiredClass = features[0];
            svm_node[] nodes = featuresToSvmNodes(Arrays.copyOfRange(features, 1, featlen));
            double[] classId = svm.svm_predict(model, nodes);
            if (desiredClass == classId[0]) {
                positives++;
            }
        }

        return positives * 1.0 / samples;
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

    // when trained LibSVM model saved in Matlab like
    // save '\your_dir\your_model_file' model
    // this methods parses it and returns a Java svm_model
    public static svm_model loadFromMatlabFile(String fileName) {
        svm_model model = new svm_model();

        try {
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader bReader = new BufferedReader(fr);

            String line = "";
            StringTokenizer tokenizer;

            System.out.println("Parsing document: " + fileName);

            while ((line = bReader.readLine()) != null) {
                // Rule 1. Look for SVM parameters
                if (line.contains("# name: Parameters")) {
                    // Skip 3 lines
                    for (int i = 0; i < 3; i++)
                        bReader.readLine();
                    // Read params
                    String svmType = bReader.readLine().trim();
                    String kernel = bReader.readLine().trim();
                    String degree = bReader.readLine().trim();
                    String gamma = bReader.readLine().trim();
                    String coef0 = bReader.readLine().trim();

                    System.out.println("SVM Type: " + svmType + "; kernel: " + kernel + "; degree: " + degree + "; gamma: " + gamma + "; coef: " + coef0);
                    // fill parameters
                    svm_parameter params = new svm_parameter();
                    params.svm_type = Integer.parseInt(svmType);
                    params.kernel_type = Integer.parseInt(kernel);
                    params.degree = Integer.parseInt(degree);
                    params.gamma = Double.parseDouble(gamma);
                    params.coef0 = Double.parseDouble(coef0);

                    model.param = params;
                }
                // Rule 2. Look for NR_CLASS
                else if (line.contains("# name: nr_class")) {
                    // Skip 1 line
                    bReader.readLine();
                    // Read value
                    String nrClass = bReader.readLine().trim();
                    System.out.println("nrClass: " + nrClass);

                    // Set value
                    model.nr_class = Integer.parseInt(nrClass);
                }
                // Rule 3. Look for totalSV
                else if (line.contains("# name: totalSV")) {
                    // Skip 1 line
                    bReader.readLine();
                    // Read value
                    String totalSV = bReader.readLine().trim();
                    System.out.println("totalSV: " + totalSV);

                    // Set value
                    model.l = Integer.parseInt(totalSV);
                }
                // Rule 4. Look for RHO
                else if (line.contains("# name: rho")) {
                    // Skip 1 line
                    bReader.readLine();
                    // read RHO count: "# rows: 69 "
                    line = bReader.readLine();
                    String rows = line.substring(8).trim();
                    System.out.println("RHO count: " + rows);
                    // Skip 1 line - columns
                    bReader.readLine();

                    int nRows = Integer.parseInt(rows);

                    double[] rho = new double[nRows];

                    System.out.println("RHO values: ");
                    for (int i = 0; i < nRows; i++) {
                        line = bReader.readLine().trim();
                        System.out.print(" " + line);
                        rho[i] = Double.parseDouble(line);
                    }
                    System.out.println();

                    // Set value
                    model.rho = rho;
                }
                // Rule 5. Look for Labels
                else if (line.contains("# name: Label")) {
                    // Skip 1 line
                    bReader.readLine();
                    // read Label count: "# rows: 69 "
                    line = bReader.readLine().trim();
                    String rows = line.substring(8);
                    int nRows = Integer.parseInt(rows);
                    // Skip 1 line - columns
                    bReader.readLine();

                    int[] labels = new int[nRows];

                    System.out.println("Labels: ");
                    for (int i = 0; i < nRows; i++) {
                        line = bReader.readLine().trim();
                        System.out.print(" " + line);
                        labels[i] = Integer.parseInt(line);
                    }
                    System.out.println();

                    // Set value
                    model.label = labels;
                }
                // Rule 5. Look for SV indices
                else if (line.contains("# name: sv_indices")) {
                    // Skip 1 line
                    bReader.readLine();
                    // read SV indices count: "# rows: 69 "
                    line = bReader.readLine().trim();
                    String rows = line.substring(8);
                    int nRows = Integer.parseInt(rows);
                    // Skip 1 line - columns
                    bReader.readLine();

                    int[] svIndices = new int[nRows];

                    System.out.println("svIndices: ");
                    for (int i = 0; i < nRows; i++) {
                        line = bReader.readLine().trim();
                        System.out.print(" " + line);
                        svIndices[i] = Integer.parseInt(line);
                    }
                    System.out.println();

                    // Set value
                    model.sv_indices = svIndices;
                }
                // Rule 6. Look for nSV
                else if (line.contains("# name: nSV")) {
                    // Skip 1 line
                    bReader.readLine();
                    // read nSV count: "# rows: 69 "
                    line = bReader.readLine().trim();
                    String rows = line.substring(8).trim();
                    int nRows = Integer.parseInt(rows);
                    System.out.println("nSV rows: " + rows);
                    // Skip 1 line - columns
                    bReader.readLine();

                    int[] nSV = new int[nRows];

                    for (int i = 0; i < nRows; i++) {
                        line = bReader.readLine().trim();
                        nSV[i] = Integer.parseInt(line);
                    }

                    // Set value
                    model.nSV = nSV;
                }
                // Rule 7. Look for SV_COEF
                else if (line.contains("# name: sv_coef")) {
                    // Skip 1 line
                    bReader.readLine();
                    // read rows: "# rows: 69 "
                    line = bReader.readLine();
                    String rows = line.substring(8).trim();
                    // read columns: "# columns: 69 "
                    line = bReader.readLine();
                    String cols = line.substring(11).trim();

                    int nRows = Integer.parseInt(rows);
                    int nCols = Integer.parseInt(cols);
                    System.out.println("sv_coef rows: " + rows);
                    System.out.println("sv_coef cols: " + cols);

                    double[][] svCoef = new double[nCols][nRows];

                    for (int i = 0; i < nRows; i++) {
                        line = bReader.readLine();

                        tokenizer = new StringTokenizer(line, " ");
                        int j = 0;
                        while (tokenizer.hasMoreTokens()) {
                            svCoef[j++][i] = Double.parseDouble(tokenizer.nextToken().trim());
                        }
                    }
                    System.out.println("sv_coef size: [" + svCoef.length + "][" + svCoef[0].length + "]");

                    // Set value
                    model.sv_coef = svCoef;
                }
                // Rule 8. Look for SVs
                else if (line.contains("# name: SVs")) {
                    // Skip 2 lines
                    bReader.readLine();
                    // read non zero element count: "# nnz: 69 "
                    line = bReader.readLine();
                    String nnz = line.substring(7).trim();
                    // read rows: "# rows: 69 "
                    line = bReader.readLine();
                    String rows = line.substring(8).trim();
                    // read columns: "# columns: 69 "
                    line = bReader.readLine();
                    String cols = line.substring(11).trim();

                    int nRows = Integer.parseInt(rows);
                    int nCols = Integer.parseInt(cols);
                    int nNnz = Integer.parseInt(nnz);

                    System.out.println("SV rows: " + rows);
                    System.out.println("SV cols: " + cols);
                    System.out.println("SV nnz: " + nnz);

                    svm_node[][] SV = new svm_node[nRows][nCols];
                    // keep column indexes here : there cannot be empty elements in the start or middle.
                    // Empty element means the end of parameters
                    int[] colIdx = new int[nRows];

                    for (int i = 0; i < nNnz; i++) {
                        line = bReader.readLine();

                        tokenizer = new StringTokenizer(line, " ");
                        String sampleIdx = tokenizer.nextToken().trim();
                        String featureIdx = tokenizer.nextToken().trim();
                        String value = tokenizer.nextToken().trim();
                        Integer sIdx = Integer.parseInt(sampleIdx);
                        Integer fIdx = Integer.parseInt(featureIdx);
                        //System.out.println("i="+i+"("+sIdx+", "+fIdx+") = "+value);

                        // get row index for the Ith column
                        int rowIdx = colIdx[sIdx - 1];

                        SV[sIdx - 1][rowIdx] = new svm_node();
                        SV[sIdx - 1][rowIdx].index = fIdx;
                        SV[sIdx - 1][rowIdx].value = Double.parseDouble(value);
                        // increment by 1
                        colIdx[sIdx - 1]++;
                    }

                    // Set value
                    model.SV = SV;
                    model.colIdx = colIdx;
                }


            }
            bReader.close();
            fr.close();

            System.out.println("Parsing successfully finished.");

        } catch (Exception ex) {
            System.out.println("loadFromMatlabFile: " + ex.toString());
        }

        return model;
    }

    public static void main(String[] args) {
        SvmHelper capitalSvm = new SvmHelper();

        System.out.println("arg[0] is: " + args[0] + "\n");


        if ((args.length < 1) || (args[0] == null)) {
            System.out.println("Please pass a 'train'/'test' parameter. \n");
        } else if (args[0].equals("train")) {
            double[][] trainingSamples = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP", LabelManager.LabelTypeEnum.CAPITAL);
            double[][] testData = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP\\TEST", LabelManager.LabelTypeEnum.CAPITAL);

            long l = System.currentTimeMillis();
            System.out.println("Training started. Elements: " + trainingSamples.length + ", features: " + trainingSamples[0].length);
            svm_model capitalSvmModel = capitalSvm.train(trainingSamples);
            //svm_model capitalSvmModel = capitalSvm.CrossValidateTrain(trainingSamples,testData);
            try {
                svm.svm_save_model("D:\\Dropbox\\TEMP\\JML_CAPITAL_model", capitalSvmModel);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Training finished. Duration : " + (int) ((System.currentTimeMillis() - l) / 1000) + " seconds");
        } else if (args[0].equals("test")) {
            System.out.println("Performing test...\n");

            String modelFile = "D:\\Dropbox\\TEMP\\JML_CAPITAL_model";
            String octaveModelFile = "D:\\Dropbox\\TEMP\\JML_CAPITAL_model_octave";
            double[][] trainingSamples = LabelManager.loadFeaturesAsArray("JML", "D:\\Dropbox\\TEMP\\TEST", LabelManager.LabelTypeEnum.CAPITAL);
            int positives = 0;
            int samples = trainingSamples.length;
            int featlen = trainingSamples[0].length;

            try {
                //svm_model model = svm.svm_load_model(modelFile);
                svm_model model = loadFromMatlabFile(octaveModelFile);
                for (double[] features : trainingSamples) {
                    double desiredClass = features[0];
                    svm_node[] nodes = featuresToSvmNodes(Arrays.copyOfRange(features, 1, featlen));
                    double[] svmResult = svm.svm_predict(model, nodes);
                    if (desiredClass == svmResult[0]) {
                        positives++;
                        System.out.println("Correct : " + desiredClass + "=" + svmResult[0]);
                    } else {
                        System.out.println("Wrong : " + desiredClass + "<>" + svmResult[0]);
                    }
                }
                System.out.println("Test result: " + (positives * 100 / samples) + "% (" + positives + "/" + samples + ")");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
