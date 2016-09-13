package ocr;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import image.ImageTransform;
import image.segment.elements.Column;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import ocr.svm.libsvm.SvmHelper;
import ocr.svm.libsvm.svm;
import ocr.svm.libsvm.svm_model;
import ocr.svm.libsvm.svm_node;
import utils.MatrixOperations;

/**
 * Created by itjamal on 7/12/2016.
 */
public class Recognition {

    HashMap<LabelManager.LabelTypeEnum, svm_model> svmModels = new HashMap<LabelManager.LabelTypeEnum, svm_model>();

    public enum RecognitionModel {
        NeuralNet, SVM,
    }

    public void init() {
        LabelManager.loadHashes("JML");
    }

    public void loadModels(RecognitionModel recModel, String formName) {
        // load SVM model
        String modelFile = "";
        try {
            if (recModel == RecognitionModel.SVM) {
                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_CAPITAL_model";
                svm_model capitalModel = SvmHelper.loadFromMatlabFile(modelFile);

                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_DIGITS_model";
                svm_model digitsModel = SvmHelper.loadFromMatlabFile(modelFile);

                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_LETTERS_model";
                svm_model lettersModel = SvmHelper.loadFromMatlabFile(modelFile);

                svmModels.put(LabelManager.LabelTypeEnum.CAPITAL, capitalModel);
                svmModels.put(LabelManager.LabelTypeEnum.DIGITS, digitsModel);
                svmModels.put(LabelManager.LabelTypeEnum.LETTERS, lettersModel);
            }
        } catch (Exception ex) {
            Log.e(getClass().toString(), "loadModel: " + ex);
        }
    }

    // Go through lines and recognize every symbol in words.
    // Function returns array of String - each index corresponds to the column
    public String[] recognize(RecognitionModel recModel, String formName, TreeMap<Integer, Column> columnsMap) {
        String[] resultText = new String[0];
        int colIdx = 0;

        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + formName + "_recognition.dat");
            FileWriter fw = new FileWriter(file);

            loadModels(recModel, formName);
            // check if segmentation info is not empty
            if ((columnsMap != null) || (columnsMap.size() > 0)) {
                resultText = new String[columnsMap.size()];

                Iterator<Integer> colIt = columnsMap.keySet().iterator();
                //iterating through the columns
                while (colIt.hasNext()) {
                    // get key and value of the next column
                    Integer colKey = colIt.next();// ID of the columns
                    Column column = columnsMap.get(colKey); // Column object
                    resultText[colIdx] = "";

                    // Get text lines in this column
                    TreeMap<Integer, TextLine> textLinesMap = column.getLines();
                    //Iterate through the text lines
                    Iterator<Integer> tlIt = textLinesMap.keySet().iterator();
                    int ii = 0;
                    while (tlIt.hasNext()) {
                        // get key and value of the next text line
                        Integer tlKey = tlIt.next(); // ID of the text line
                        TextLine textLine = textLinesMap.get(tlKey); // Text line itself

                        // Get words in current textline
                        TreeMap<Integer, Word> wordsMap = textLine.getWords();
                        //Iterate through the words
                        Iterator<Integer> wordsIt = wordsMap.keySet().iterator();

                        while (wordsIt.hasNext()) {
                            Integer wordKey = wordsIt.next();
                            Word word = wordsMap.get(wordKey);

                            TreeMap<Integer, Symbol> symbolsMap = word.getSymbols();
                            //Iterate through the symbols
                            Iterator<Integer> smbIt = symbolsMap.keySet().iterator();
                            while (smbIt.hasNext()) {
                                try {
                                    Integer smbKey = smbIt.next();
                                    Symbol symbol = symbolsMap.get(smbKey);

                                    // Recognize the symbol

                                    // 1) resize image to 28x28 size
                                    // 2) convert 2D 28x28 array to 1D 784x1 array
                                    // 3) add 1 "ratio" feature
                                    // 3) convert 1D array to svmNodes
                                    int[][] pixels = symbol.getPixels();

                                    // Get features from the pixel array
                                    ImageTransform imtrans = new ImageTransform();
                                    double[][] newImg = imtrans.resizeAndFill(pixels, ImageTransform.InterpolationMode.BICUBIC, 28);
                                    // convert 2D array (of BILINEAR scaling result) to 1D array
                                    double[] new1Darr = MatrixOperations.oneDimensional(newImg);

                                    // add image ratio (multiplied by 5) to the end of the array
                                    int w = pixels.length;
                                    int h = pixels[0].length;
                                    double[] features = MatrixOperations.addElement(new1Darr, (w * 5.0 / h));

                                    for (double feat : features)
                                        fw.write(feat + " ");
                                    fw.write("\n");

                                    svm_node[] svmNodes = SvmHelper.featuresToSvmNodes(features);

                                    LabelManager.LabelTypeEnum winnerType = LabelManager.LabelTypeEnum.CAPITAL;
                                    double winnerClassId;

                                    // Recognize in CAPITAL LETTERS DB and set it's probability as winner (default)
                                    winnerClassId = svm.svm_predict(svmModels.get(LabelManager.LabelTypeEnum.CAPITAL), svmNodes);

                                    // Recognize in DIGITS DB
                                    double digitsClassId = svm.svm_predict(svmModels.get(LabelManager.LabelTypeEnum.DIGITS), svmNodes);
                                    if (digitsClassId > winnerClassId) {
                                        winnerType = LabelManager.LabelTypeEnum.DIGITS;
                                        winnerClassId = digitsClassId;
                                    }

                                    // Recognize in LETTERS
                                    double lettersClassId = svm.svm_predict(svmModels.get(LabelManager.LabelTypeEnum.LETTERS), svmNodes);
                                    if (lettersClassId > winnerClassId) {
                                        winnerType = LabelManager.LabelTypeEnum.LETTERS;
                                        winnerClassId = lettersClassId;
                                    }


                                    // get label name by ID from the highest probability model
                                    resultText[colIdx] += LabelManager.getSymbol(winnerType, (int) winnerClassId);
                                } catch (Exception ex) {
                                    Log.e(getClass().toString(), "recognize(): " + ex.toString());
                                }
                            }
                            // word is finished, add space before the next word.
                            resultText[colIdx] += " ";

                        }
                        // line is ended, new line.
                        resultText[colIdx] += "\n";
                    }
                    colIdx++;
                }

            }
            fw.close();

        } catch (Exception ex) {
            Log.e(getClass().toString(), "recognize(): " + ex);
        } finally {
            return resultText;
        }
    }
}


