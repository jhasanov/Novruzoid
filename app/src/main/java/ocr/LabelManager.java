package ocr;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import image.segment.elements.Symbol;

/**
 * Created by itjamal on 7/12/2016.
 * This class is used to :
 * - save/load hashtables that store Label ID and Names;
 * - find label's ID by name and vise versa.
 */
public class LabelManager {
    private static boolean bInitialized;
    private static final String CLASS_NAME = "LabelManager";
    private static Integer digitSeqId = 0;
    private static Integer letterSeqId = 0;
    private static Integer capitalSeqId = 0;
    private static Integer imageSeqId = 0;
    private static Hashtable<String, Integer> htDigits, htLetters, htCapitals, htImages;
    private static Hashtable<Integer, String> htrDigits, htrLetters, htrCapitals, htrImages;
    public static ArrayList<Symbol> symbolList = new ArrayList<Symbol>();

    public enum LabelTypeEnum {
        NONE, DIGITS, LETTERS, CAPITAL, IMAGES,
    }

    static {
        Log.i(CLASS_NAME, "Static{} called");
    }

    public static boolean isbInitialized() {
        return bInitialized;
    }

    // Loads all label dictionaries
    public static boolean loadHashes(String formName) {
        try {
            htDigits = loadHash(formName, LabelTypeEnum.DIGITS);
            htLetters = loadHash(formName, LabelTypeEnum.LETTERS);
            htCapitals = loadHash(formName, LabelTypeEnum.CAPITAL);
            htImages = loadHash(formName, LabelTypeEnum.IMAGES);
            htrDigits = reverseHash(htDigits);
            htrLetters = reverseHash(htLetters);
            htrCapitals = reverseHash(htCapitals);
            htrImages = reverseHash(htImages);
            bInitialized = true;
            return true;
        } catch (Exception ex) {
            Log.e(CLASS_NAME, "no hash exist : " + ex);
            return false;
        }

    }

    // Loads values from file to memory
    // If no hash exists, returns new hash
    public static Hashtable<String, Integer> loadHash(String formName, LabelTypeEnum symbolType) {
        Hashtable<String, Integer> htMap = new Hashtable<String, Integer>();
        String filePrefix = "";

        if (symbolType == LabelTypeEnum.DIGITS) {
            filePrefix = "digits";
        } else if (symbolType == LabelTypeEnum.LETTERS) {
            filePrefix = "letters";
        } else if (symbolType == LabelTypeEnum.CAPITAL) {
            filePrefix = "capital";
        } else if (symbolType == LabelTypeEnum.IMAGES) {
            filePrefix = "images";
        }

        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + formName + "_" + filePrefix + ".dat");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line = "";
            StringTokenizer tokenizer;

            while ((line = br.readLine()) != null) {
                Log.i(CLASS_NAME, "readLine: " + line);
                tokenizer = new StringTokenizer(line, " ");
                String key = tokenizer.nextToken();
                Integer val = Integer.parseInt(tokenizer.nextToken());
                htMap.put(key, val);
            }
            br.close();
            fr.close();
        } catch (Exception ex) {
            Log.e(CLASS_NAME, "loadHash() : " + ex);
        }

        return htMap;
    }

    // Save hashtable values to corresponding files
    public static void saveHash(String formName, LabelTypeEnum symbolType) {
        Hashtable<String, Integer> htMap = new Hashtable<String, Integer>();
        String filePrefix = "";
        if (symbolType == LabelTypeEnum.DIGITS) {
            htMap = htDigits;
            filePrefix = "digits_dict";
        } else if (symbolType == LabelTypeEnum.LETTERS) {
            htMap = htLetters;
            filePrefix = "letters_dict";
        } else if (symbolType == LabelTypeEnum.CAPITAL) {
            htMap = htCapitals;
            filePrefix = "capital_dict";
        } else if (symbolType == LabelTypeEnum.IMAGES) {
            htMap = htImages;
            filePrefix = "images_dict";
        }

        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + formName + "_" + filePrefix + ".dat");
            FileWriter fw = new FileWriter(file);
            Set<String> keys = htMap.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                Integer val = htMap.get(key);
                fw.write(key + " " + val + "\n");
            }
            fw.close();
        } catch (Exception ex) {
            Log.e(CLASS_NAME, "saveHash() : " + ex);
        }
    }

    // returns the ID of the Class. If class doesn't exist in hash, adds it.
    public static Integer getClassID(LabelTypeEnum symbolType, String classDesc) {
        Integer classId = -1;
        if (symbolType == LabelTypeEnum.DIGITS) {
            classId = htDigits.get(classDesc);
            if (classId == null) {
                htDigits.put(classDesc, ++digitSeqId);
                classId = digitSeqId;
            }
        } else if (symbolType == LabelTypeEnum.LETTERS) {
            classId = htLetters.get(classDesc);
            if (classId == null) {
                htLetters.put(classDesc, ++letterSeqId);
                classId = letterSeqId;
            }
        } else if (symbolType == LabelTypeEnum.CAPITAL) {
            classId = htCapitals.get(classDesc);
            if (classId == null) {
                htCapitals.put(classDesc, ++capitalSeqId);
                classId = capitalSeqId;
            }
        } else if (symbolType == LabelTypeEnum.IMAGES) {
            classId = htImages.get(classDesc);
            if (classId == null) {
                htImages.put(classDesc, ++imageSeqId);
                classId = imageSeqId;
            }
        }
        return classId;
    }

    public static Hashtable<Integer, String> reverseHash(Hashtable<String, Integer> htOriginal) {
        Hashtable newHash = new Hashtable();

        for (String key : htOriginal.keySet()) {
            newHash.put(htOriginal.get(key), key);
        }
        return newHash;
    }

    // gets symbol by ID
    public static String getSymbol(LabelTypeEnum symbolType, Integer id) {
        String symbol = "?";

        if (symbolType == LabelTypeEnum.DIGITS) {
            symbol = htrDigits.get(id);
        } else if (symbolType == LabelTypeEnum.LETTERS) {
            symbol = htrLetters.get(id);
        } else if (symbolType == LabelTypeEnum.CAPITAL) {
            symbol = htrCapitals.get(id);
        } else if (symbolType == LabelTypeEnum.IMAGES) {
            symbol = htrImages.get(id);
        }

        if (symbol == null)
            symbol = "?";

        return symbol;
    }

    // saves features in a file in comma delimited format
    public static void saveFeatures(String formName, ArrayList<double[]> featuresList) {
        try {
            Iterator it = featuresList.iterator();

            File file = new File(Environment.getExternalStorageDirectory() + File.separator + formName + "_features.dat");
            FileWriter fw = new FileWriter(file);

            while (it.hasNext()) {
                double[] record = (double[]) it.next();
                String strRecord = "";
                for (int i = 0; i < record.length; i++) {
                    strRecord += record[i] + " ";
                }
                fw.write(strRecord + "\n");
            }
            fw.close();
            fw = null;
        } catch (Exception ex) {
            Log.e(CLASS_NAME, "saveFeatures() : " + ex);
        }

    }

    // loads features from a file and converts it to ArrayList
    public static ArrayList<double[]> loadFeatures(String formName) {
        ArrayList<double[]> featuresList = new ArrayList<double[]>();

        try {
            InputStream is = new FileInputStream(Environment.getExternalStorageDirectory() + File.separator + formName + "_features.dat");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringTokenizer tokenizer;
            String line = "";
            while ((line = br.readLine()) != null) {
                tokenizer = new StringTokenizer(line, " ");

                double[] record = new double[tokenizer.countTokens()];
                int i = 0;
                while (tokenizer.hasMoreTokens()) {
                    record[i] = Double.parseDouble(tokenizer.nextToken());
                }
                featuresList.add(record);
            }
            br.close();
            isr.close();
            is.close();
        } catch (Exception ex) {
            Log.e(CLASS_NAME, "laodFeatures() : " + ex);
        }
        return featuresList;
    }


}
