package ocr;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeMap;

import image.ImageTransform;
import image.segment.elements.Column;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import ocr.lexicon.LexClass1;
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

    enum RecogPhase {AUTHOR_DETECTION, DATE_DETECTION, TIME_DETECTION, RECORD_DETECTION, TOTAL_DETECTION}

    public void init() {
        LabelManager.loadHashes("JML");
    }

    public void loadModels(RecognitionModel recModel, String formName) {
        // load SVM model
        String modelFile = "";
        try {
            if (recModel == RecognitionModel.SVM) {
                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_ALL_model";
                svm_model allModel = SvmHelper.loadFromMatlabFile(modelFile);

                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_CAPITAL_model";
                svm_model capitalModel = SvmHelper.loadFromMatlabFile(modelFile);

                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_DIGITS_model";
                svm_model digitsModel = SvmHelper.loadFromMatlabFile(modelFile);

                modelFile = Environment.getExternalStorageDirectory() + File.separator + formName + "_LETTERS_model";
                svm_model lettersModel = SvmHelper.loadFromMatlabFile(modelFile);

                svmModels.put(LabelManager.LabelTypeEnum.ALL, allModel);
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
        LabelManager.LabelTypeEnum labelType;
        LexClass1 lexClass = new LexClass1();

        String dateText = "";
        String timeText = "";
        String itemDesc = "";
        // This array will store Quantity, Price and Total
        float[] quan_pri_tot = new float[3];
        // Index of the quan_pri_tot array
        int qpt_idx = 0;

        RecogPhase recogPhase = RecogPhase.AUTHOR_DETECTION;

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
                    // In the start, recognize all symbols as capital
                    labelType = LabelManager.LabelTypeEnum.CAPITAL;

                    while (tlIt.hasNext()) {
                        // get key and value of the next text line
                        Integer tlKey = tlIt.next(); // ID of the text line
                        TextLine textLine = textLinesMap.get(tlKey); // Text line itself

                        // Get words in current textline
                        TreeMap<Integer, Word> wordsMap = textLine.getWords();

                        /*
                         During RECORD detection, analyze text from back to front:
                         1st 3 digits shall be TOTAL, PRICE and COUNT (order is reversed )
                         The remaining part is the name of the item.
                         */

                        NavigableSet nKeySet;
                        if (recogPhase == RecogPhase.RECORD_DETECTION) {
                            itemDesc = "";
                            quan_pri_tot = new float[3];
                            qpt_idx = 2;
                            labelType = LabelManager.LabelTypeEnum.DIGITS;
                            nKeySet = wordsMap.descendingKeySet();
                            resultText[colIdx] += "Reverse nKeySet size : " + nKeySet.size() + "\n";
                        } else
                            nKeySet = wordsMap.navigableKeySet();

                        //Iterate through the words
                        Iterator<Integer> wordsIt = nKeySet.iterator();


                        while (wordsIt.hasNext()) {
                            if (recogPhase == RecogPhase.RECORD_DETECTION)
                                resultText[colIdx] += "+";
                            Integer wordKey = wordsIt.next();
                            Word word = wordsMap.get(wordKey);

                            TreeMap<Integer, Symbol> symbolsMap = word.getSymbols();
                            //Iterate through the symbols
                            Iterator<Integer> smbIt = symbolsMap.keySet().iterator();
                            String wordText = "";

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
                                    double[][] newImg;
                                    double[] new1Darr;
                                    try {
                                        newImg = imtrans.resizeAndFill(pixels, ImageTransform.InterpolationMode.BICUBIC, 28);
                                        // convert 2D array (of BILINEAR scaling result) to 1D array
                                        new1Darr = MatrixOperations.oneDimensional(newImg);
                                    } catch (Exception ex) {
                                        Log.e(getClass().toString(), "Error. recognize().resize: size1=" + pixels.length + "; size2=" + pixels[0].length);
                                        continue;
                                    }

                                    // add image ratio (multiplied by 5) to the end of the array
                                    int w = pixels.length;
                                    int h = pixels[0].length;
                                    double[] features = MatrixOperations.addElement(new1Darr, (w * 5.0 / h));

                                    for (double feat : features)
                                        fw.write(feat + " ");
                                    fw.write("\n");

                                    svm_node[] svmNodes = SvmHelper.featuresToSvmNodes(features);
                                    double[] svmResult;

                                    svmResult = svm.svm_predict(svmModels.get(labelType), svmNodes);
                                    double classId = svmResult[0];

                                    // get label name by ID from the winner model
                                    wordText += LabelManager.getSymbol(labelType, (int) classId);

                                    // Real-time analysis for DATE and TIME (for split case like "TARIHABCD")
                                    if (labelType == LabelManager.LabelTypeEnum.CAPITAL) {
                                        if (recogPhase == RecogPhase.DATE_DETECTION) {
                                            if (lexClass.isDate(wordText)) {
                                                resultText[colIdx] += wordText + "(d)";
                                                wordText = "";
                                                labelType = LabelManager.LabelTypeEnum.DIGITS;
                                            }

                                        } else if (recogPhase == RecogPhase.TIME_DETECTION) {
                                            if (lexClass.isTime(wordText)) {
                                                resultText[colIdx] += wordText + "(t)";
                                                wordText = "";
                                                labelType = LabelManager.LabelTypeEnum.DIGITS;
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    Log.e(getClass().toString(), "recognize(): " + ex.getMessage());
                                }
                            }

                            // ----------------------------------------------------------------------------------
                            // Word is ready: check what it is
                            // 1) If document type is not defined, then try to find what type of document is this.
                            if (recogPhase == RecogPhase.AUTHOR_DETECTION) {

                                // First check if it's DIGITS (VOEN numbers)
                                if (labelType == LabelManager.LabelTypeEnum.DIGITS) {
                                    Log.d(getClass().toString(), "VOEN: " + wordText);
                                    String voenOwner = lexClass.defineDocAuthorbyVOEN(wordText);
                                    if (voenOwner != "") {
                                        wordText += "(" + voenOwner + ")";
                                        labelType = LabelManager.LabelTypeEnum.CAPITAL;
                                        recogPhase = RecogPhase.DATE_DETECTION;
                                        dateText = "";
                                    }
                                }

                                // If not DIGITS, then scan for AUTHOR or "VOEN" text
                                else {
                                    String docTypeName = lexClass.defineDocAuthor(wordText.toUpperCase());
                                    if (docTypeName != "") {
                                        wordText = docTypeName + "(a)";

                                        // If Author ( receipt issuer) is detected, then move to the DATE detection
                                        // otherwise check the VOEN
                                        labelType = LabelManager.LabelTypeEnum.CAPITAL;
                                        recogPhase = RecogPhase.DATE_DETECTION;
                                        dateText = "";
                                    } else {
                                        if (lexClass.isVOEN(wordText)) {
                                            wordText += "(v)";
                                            labelType = LabelManager.LabelTypeEnum.DIGITS;
                                        }
                                    }
                                }
                            }
                            // 2) Try to find DATE value
                            else if (recogPhase == RecogPhase.DATE_DETECTION) {
                                if (labelType == LabelManager.LabelTypeEnum.DIGITS) {
                                    // Parse date form text
                                    wordText = wordText.replaceAll("[^0-9]", "");
                                    dateText += lexClass.buildDate(wordText);

                                    // this number will depend on receipt's date format
                                    if (dateText.length() < 6) {
                                        wordText = "";
                                        continue;
                                    }

                                    wordText += " (" + dateText + ") ";

                                    labelType = LabelManager.LabelTypeEnum.CAPITAL;
                                    recogPhase = RecogPhase.TIME_DETECTION;
                                    timeText = "";
                                }
                            }
                            // 2) Try to find TIME value
                            else if (recogPhase == RecogPhase.TIME_DETECTION) {
                                if (labelType == LabelManager.LabelTypeEnum.DIGITS) {
                                    // Parse date form text
                                    timeText += wordText.replaceAll("[^0-9]", "");

                                    // this number will depend on receipt's date format
                                    if (timeText.length() < 4) {
                                        wordText = "";
                                        continue;
                                    }

                                    wordText += " (" + timeText + ") ";
                                    labelType = LabelManager.LabelTypeEnum.CAPITAL;
                                    recogPhase = RecogPhase.RECORD_DETECTION;
                                }
                            }
                            // check numbers in reverse order
                            else if ((recogPhase == RecogPhase.RECORD_DETECTION)) {
                                if (qpt_idx < 0) { // item name
                                    itemDesc = wordText + " " + itemDesc;
                                } else { // quan / price / total
                                    wordText = wordText.replaceAll("[^,0-9]", "").replace(",", ".").replace("..", ".");

                                    try {
                                        quan_pri_tot[qpt_idx--] = Float.parseFloat(wordText);
                                        if (qpt_idx == -1) {
                                            // Check if these 3 numbers are Quan, Price and Total.
                                            if (Math.abs(quan_pri_tot[2] - (quan_pri_tot[1] * quan_pri_tot[0])) < 0.1) {
                                                itemDesc = " : " + quan_pri_tot[0] + "/" + quan_pri_tot[1] + "/" + quan_pri_tot[2];
                                                labelType = LabelManager.LabelTypeEnum.CAPITAL;
                                            } else {
                                                itemDesc = "";
                                            }
                                        }
                                    } catch (Exception ex) {
                                        Log.d(getClass().toString(), "RECORD_DETECTION: " + ex);
                                    }
                                }
                            }

                            // word is finished, add space before the next word.
                            resultText[colIdx] += wordText + " ";
                        }

                        if (recogPhase == RecogPhase.RECORD_DETECTION) {
                            resultText[colIdx] += " (" + itemDesc + ")";
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
            resultText[0] += "\n Exception: " + ex;
            resultText[0] += "\n" + ex.getMessage();
        } finally {
            return resultText;
        }
    }
}


