package ocr;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import image.segment.elements.Column;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import ocr.svm.libsvm.svm;
import ocr.svm.libsvm.svm_model;
import ocr.svm.libsvm.svm_node;

/**
 * Created by itjamal on 7/12/2016.
 */
public class Recognition {

    public enum RecognitionModel {
        NeuralNet, SVM,
    }

    public void init() {
        LabelManager.loadHashes("SAFA");
    }

    public svm_model loadModel(RecognitionModel recModel) {
        svm_model model = null;

        // load SVM model
        String modelFile = "";
        try {
            if (recModel == RecognitionModel.SVM) {
                modelFile = Environment.getExternalStorageDirectory() + File.separator + "SAFA_letters_model.txt";
            }
            svm_model svmLettersModel = svm.svm_load_model(modelFile);
        } catch (Exception ex) {
            Log.e(getClass().toString(), "loadModel: " + ex);
        }

        return model;
    }

    // Go through lines and recognize every symbol in words.
    // Function returns array of String - each index corresponds to the column
    public String[] recognize(RecognitionModel recModel, TreeMap<Integer, Column> columnsMap) {
        String[] resultText = new String[0];
        int colIdx = 0;


        try {
            svm_model model = loadModel(recModel);
            // check if segmentation info is not empty
            if ((columnsMap != null) || (columnsMap.size() > 0)) {
                resultText = new String[columnsMap.size()];

                Iterator<Integer> colIt = columnsMap.keySet().iterator();
                //iterating through the columns
                while (colIt.hasNext()) {
                    // get key and value of the next column
                    Integer colKey = colIt.next();// ID of the columns
                    Column column = columnsMap.get(colKey); // Column object

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
                                Integer smbKey = smbIt.next();
                                Symbol symbol = symbolsMap.get(smbKey);
                                // recognize the symbol

                                svm_node[] svmNodes = new svm_node[784];
                                // TODO : convert 2D 28x28 array to 1D 784x1 array
                                int [][] pixels = symbol.getPixels();
                                double classId = svm.svm_predict(model, svmNodes);
                                // get label name by ID
                                resultText[colIdx] += LabelManager.getSymbol(LabelManager.LabelTypeEnum.LETTERS,(int)classId);
                            }
                            // word is finished, add space before the next word.
                            resultText[colIdx] += " ";

                        }
                        // line is ended, new line.
                        resultText[colIdx] += "/n";
                    }
                    colIdx++;
                }

            }

        } catch (Exception ex) {
            Log.e(getClass().toString(), "recognize(): " + ex);
        } finally {
            return resultText;
        }
    }
}


