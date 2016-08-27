package andir.novruzoid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import image.ImageTransform;
import image.segment.elements.Symbol;
import ocr.LabelManager;
import utils.MatrixOperations;

public class LearnLibrary extends Activity {

    // UI variables
    private ImageView segmentView;
    private RadioButton digitRB, letterRB, capitalRB, imageRB;
    private EditText docTypeEB, classTypeEB;

    // All symbols and iterator
    //private ArrayList<Symbol> symbolList;
    private Iterator symbolIt;
    // Variables to save class parameters
    private SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy-HHmmss");
    private boolean bMapsLoaded;
    private String formName = "";
    private ArrayList<double[]> digitFeatureList, letterFeatureList, capitalFeatureList, imageFeatureList;
    private double[] features;

    // Info for Dropbox access
    final static private String APP_KEY = "ywci2ypov61gkoo";
    final static private String APP_SECRET = "tscn4wp9vdugjjb";
    // Dropbox vaiables
    private boolean bDropboxGranted;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    // ---------------

    private LabelManager.LabelTypeEnum selectedType = LabelManager.LabelTypeEnum.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn_library);
        // Image view
        segmentView = (ImageView) findViewById(R.id.segmentView);
        // Radio buttons
        digitRB = (RadioButton) findViewById(R.id.digitRB);
        letterRB = (RadioButton) findViewById(R.id.letterRB);
        capitalRB = (RadioButton) findViewById(R.id.capitalRB);
        imageRB = (RadioButton) findViewById(R.id.imageRB);
        // Edit boxes
        docTypeEB = (EditText) findViewById(R.id.docTypeEB);
        classTypeEB = (EditText) findViewById(R.id.classTypeEB);
        try {
            Log.i(getClass().toString(), "onCreate() - Loading symbols");
            //symbolList = (ArrayList<Symbol>) getIntent().getSerializableExtra("andir.novruzoid.SymbolList");
            //Log.i(getClass().toString(), "onCreate() - Symbols: "+symbolList.size());
        } catch (Exception ex) {
            Log.e(getClass().toString(), "onCreate() - problem with SYMBOLS : " + ex);
        }
    }

    @Override
    protected void onStart() {
        Log.d(getClass().toString(), "onStart called");
        super.onStart();
        try {
            init();
        } catch (Exception ex) {
            Log.e(getClass().toString(), "onStart() : " + ex);
        }
    }

    // implemented for Dropbox (when it gets back to activity after granting)
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(getClass().toString(), "onResume called. bDropbox=" + bDropboxGranted);

        if (bDropboxGranted && mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.e(getClass().toString(), "Error authenticating", e);
            }
        }
    }

    // I do not override super.onBackPressed(), to avoid the activity close.
    @Override
    public void onBackPressed() {
        AlertDialog.Builder confirmExit = new AlertDialog.Builder(this);
        confirmExit
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        LearnLibrary.this.finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                });
        confirmExit.show();
    }

    private void init() {
        try {
            Log.e(getClass().toString(), "init() 1");
            symbolIt = LabelManager.symbolList.iterator();
            Log.e(getClass().toString(), "init() : " + symbolIt);
            //showNext();
            Log.e(getClass().toString(), "init() 3");
        } catch (Exception ex) {
            Log.e(getClass().toString(), "problem in init(): " + ex);
        }

    }

    // Just show next, without adding class to the learning list.
    public void skip(View v) {
        showNext();
    }

    public void next(View v) {
        String charDesc = classTypeEB.getText().toString();

        // Check if all maps loaded
        if (!bMapsLoaded) {
            formName = docTypeEB.getText().toString();
            if ((formName != null) && (formName.length() > 1)) {
                bMapsLoaded = LabelManager.loadHashes(formName);
                digitFeatureList = LabelManager.loadFeatures(formName, LabelManager.LabelTypeEnum.DIGITS);
                capitalFeatureList = LabelManager.loadFeatures(formName, LabelManager.LabelTypeEnum.CAPITAL);
                letterFeatureList = LabelManager.loadFeatures(formName, LabelManager.LabelTypeEnum.LETTERS);
                imageFeatureList = LabelManager.loadFeatures(formName, LabelManager.LabelTypeEnum.IMAGES);
            }
        }

        if (((charDesc != null) || (charDesc.length() > 0)) && ((features != null) && (features.length > 0))) {
            int classId = LabelManager.getClassID(selectedType, charDesc);

            // add classId id to the 1st column and store this data in ArrayList
            if (selectedType == LabelManager.LabelTypeEnum.DIGITS)
                digitFeatureList.add(MatrixOperations.mergeArrays(new double[]{classId}, features));
            else if (selectedType == LabelManager.LabelTypeEnum.LETTERS)
                letterFeatureList.add(MatrixOperations.mergeArrays(new double[]{classId}, features));
            else if (selectedType == LabelManager.LabelTypeEnum.CAPITAL)
                capitalFeatureList.add(MatrixOperations.mergeArrays(new double[]{classId}, features));
            else if (selectedType == LabelManager.LabelTypeEnum.IMAGES)
                imageFeatureList.add(MatrixOperations.mergeArrays(new double[]{classId}, features));
        }
        // paint next character and keep pixel info in pixelArrStr string
        showNext();
    }

    // Save data in a file and upload to Dropbox
    public void save(View v) {
        // First initialize connection with Dropbox
        initDropbox();
        Log.d(getClass().toString(), "Dropbox Inited");

        // Data&Time shall be in fileName;
        Date dateTime = Calendar.getInstance().getTime();

        // Files are saved with the following naming
        String docType = "_" + docTypeEB.getText().toString();
        String timeStr = "_" + sdf.format(dateTime);

        // Save all features
        LabelManager.saveFeatures(formName, LabelManager.LabelTypeEnum.DIGITS, digitFeatureList);
        LabelManager.saveFeatures(formName, LabelManager.LabelTypeEnum.LETTERS, letterFeatureList);
        LabelManager.saveFeatures(formName, LabelManager.LabelTypeEnum.CAPITAL, capitalFeatureList);
        LabelManager.saveFeatures(formName, LabelManager.LabelTypeEnum.IMAGES, imageFeatureList);

        // Classes with their IDs will be saved in corresponding files
        LabelManager.saveHash(formName, LabelManager.LabelTypeEnum.DIGITS);
        LabelManager.saveHash(formName, LabelManager.LabelTypeEnum.LETTERS);
        LabelManager.saveHash(formName, LabelManager.LabelTypeEnum.CAPITAL);
        LabelManager.saveHash(formName, LabelManager.LabelTypeEnum.IMAGES);

        // Upload to Dropbox
        uploadToDropbox(formName + "_digits_dict" + ".dat");
        uploadToDropbox(formName + "_letters_dict" + ".dat");
        uploadToDropbox(formName + "_capitals_dict" + ".dat");
        uploadToDropbox(formName + "_images_dict" + ".dat");

        // TODO: upload feature files to Dropbox as well

        setResult(RESULT_OK);
    }

    // Show next element as a picture.
    private void showNext() {
        Log.d(getClass().toString(), "next()");
        if ((symbolIt != null) && (symbolIt.hasNext())) {
            Symbol smb = (Symbol) symbolIt.next();
            Log.i(getClass().toString(), "next.symbol :" + smb);

            // draw pixels
            int[][] pixels = smb.getPixels();
            Log.i(getClass().toString(), "next.symbol.pixels :" + pixels.length);
            int w = pixels.length;
            int h = pixels[0].length;
            int imgW = segmentView.getWidth();
            int imgH = segmentView.getHeight();

            // get half of it (keep another half for resize function)
            int scale = Math.min(imgW / w, imgH / h) / 2;
            //int scale = Math.min(imgW / w, imgH / h);
            Log.i(getClass().toString(), "next: w=" + w + " h=" + h + " imgW=" + imgW + " imgH=" + imgH + " scale=" + scale);

            Bitmap bmp = Bitmap.createBitmap(imgW, imgH, Bitmap.Config.ARGB_8888);
            Log.i(getClass().toString(), "next: Bitmap created");
            bmp.eraseColor(Color.WHITE);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setAntiAlias(false);
            paint.setStyle(Paint.Style.FILL);

            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    if (pixels[i][j] != 0)
                        canvas.drawRect(i * scale, j * scale, (i + 1) * scale, (j + 1) * scale, paint);
                }
            }

            // now draw home-made resized image centered in the box
            // bicubic
            ImageTransform imtrans = new ImageTransform();
            double[][] newImg = imtrans.resizeAndFill(pixels, ImageTransform.InterpolationMode.BICUBIC, 28);
            // draw borders of the square
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, imgH / 2, 28, imgH / 2 + 28, paint);
            // fill pixel areas
            //paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < newImg.length; i++) {
                for (int j = 0; j < newImg[0].length; j++) {
                    if (newImg[i][j] != 0)
                        canvas.drawPoint(i, imgH / 2 + j, paint);
                }
            }

            //bilinear
            newImg = imtrans.resizeAndFill(pixels, ImageTransform.InterpolationMode.BILINEAR, 28);
            // draw borders of the square
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(imgW / 2, imgH / 2, imgW / 2 + 28, imgH / 2 + 28, paint);
            // fill pixel areas
            //paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < newImg.length; i++) {
                for (int j = 0; j < newImg[0].length; j++) {
                    if (newImg[i][j] != 0)
                        canvas.drawPoint(imgW / 2 + i, imgH / 2 + j, paint);
                }
            }

            // convert 2D array (of BILINEAR scaling result) to 1D array
            double[] new1Darr = MatrixOperations.oneDimensional(newImg);
            // add image ratio (multiplied by 5) to the end of the array
            features = MatrixOperations.addElement(new1Darr, (w * 5.0 / h));

            // show symbol picture
            segmentView.setImageBitmap(bmp);
        }
    }

    // initialize connection to Dropbox
    private void initDropbox() {
        try {
            AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
            AndroidAuthSession session = new AndroidAuthSession(appKeys);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
            mDBApi.getSession().startOAuth2Authentication(LearnLibrary.this);
            Log.i(getClass().toString(), "Dropbox.init.5");
            bDropboxGranted = true;
        } catch (Exception ex) {
            Log.e(getClass().toString(), "InitDropbox : " + ex);
        }
    }

    // Uploads local file to Dropbox
    private void uploadToDropbox(String fileName) {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);
            Log.i(getClass().toString(), "Dropbox.upload.1");
            FileInputStream inputStream = new FileInputStream(file);

            Log.i(getClass().toString(), "Dropbox.upload.2");
            DropboxAPI.Entry response = null;
            try {
                response = mDBApi.putFile("/Apps/Novruzoid/" + fileName, inputStream, fileName.length(), null, null);
            } catch (DropboxException e) {
                Log.i(getClass().toString(), "DropboxException: " + response.rev);
            }
            Log.i(getClass().toString(), "The uploaded file's rev is: " + response.rev);
        } catch (Exception ex) {
            Log.e(getClass().toString(), "uploadToDropbox : " + ex);
        }
    }

    public void changeFocus(View v) {
        if (digitRB.isChecked())
            selectedType = LabelManager.LabelTypeEnum.DIGITS;
        else if (letterRB.isChecked())
            selectedType = LabelManager.LabelTypeEnum.LETTERS;
        else if (capitalRB.isChecked())
            selectedType = LabelManager.LabelTypeEnum.CAPITAL;
        else if (imageRB.isChecked())
            selectedType = LabelManager.LabelTypeEnum.IMAGES;

        classTypeEB.requestFocus();
    }


}
