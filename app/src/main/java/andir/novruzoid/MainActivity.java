package andir.novruzoid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import image.segment.elements.Column;
import ocr.LabelManager;
import ocr.Recognition;


public class MainActivity extends Activity {
    public static final String APP_NAME = "Novruzoid";
    static final int CAMSHOT_ACTIVITY = 1;
    static final int LEARN_LIBRARY_ACTIVITY = 2;
    TreeMap<Integer, Column> columnsMap;
    //ArrayList<Symbol> symbolList = new ArrayList<Symbol>();
    LinearLayout camImgView;
    public DrawingView drawView;
    String fileName = "";
    Bitmap capturedBmp, croppedBmp;
    double inSampleSize = 1.0;
    double scaleX = 1.0;
    double scaleY = 1.0;
    int scrWidth, scrHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        drawView = new DrawingView(this);
        camImgView = (LinearLayout) findViewById(R.id.CamImageView);
        camImgView.addView(drawView);
        drawView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMSHOT_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                scrWidth = drawView.getWidth();
                scrHeight = drawView.getHeight();

                capturedBmp = decodeSampledBitmapFromFile(fileName, scrWidth, scrHeight);
                drawView.setBitmap(capturedBmp);
                drawView.invalidate();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        } else if (requestCode == LEARN_LIBRARY_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                // success case
                Log.i(APP_NAME, "LEARN_LIBRARY_ACTIVITY.RESULT_OK");
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Log.i(APP_NAME, "LEARN_LIBRARY_ACTIVITY.RESULT_CANCELLED");
            } else {
                // Image capture failed, advise user
                Log.i(APP_NAME, "LEARN_LIBRARY_ACTIVITY.OTHER");
            }

        }
    }

    public Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // Avoids memory allocation
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);

        // Calculate inSampleSize, Raw height and width of image
        int height = opts.outHeight;
        int width = opts.outWidth;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;

        // In my case, height is always more than width (portrait photo)
        scaleX = (double) Math.max(height, width) / Math.max(reqHeight, reqWidth);
        scaleY = (double) Math.min(height, width) / Math.min(reqHeight, reqWidth);
        inSampleSize = Math.max(scaleX, scaleY);

        Log.i(APP_NAME, "height = " + height + "; width=" + width);
        Log.i(APP_NAME, "reqHeight = " + reqHeight + "; reqWidth=" + reqWidth);
        Log.i(APP_NAME, "scaleX=" + scaleX + "; scaleY=" + scaleY);
        Log.i(APP_NAME, "Sample size = " + inSampleSize);

        Bitmap bm = BitmapFactory.decodeFile(path);

        ExifInterface exif = null;
        int orientation = 0;

        try {
            exif = new ExifInterface(path);
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        } catch (IOException ex) {
            Log.e(APP_NAME, "decodeSampleBitmapFromFile.exif = " + ex);
        }

        Log.i(APP_NAME, "orientation = " + orientation);
        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            rotationAngle = 90;
        }
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

        Matrix matrix = new Matrix();
        matrix.preRotate(rotationAngle);
        matrix.postScale((float) (1.0 / inSampleSize), (float) (1.0 / inSampleSize));
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    public Bitmap decodeCroppedBitmapFromFile(String path, int x, int y, int width, int height, int scrWidth, int scrHeight) {
        Bitmap bm = BitmapFactory.decodeFile(path);
        Bitmap croppedBitmap = null;

        ExifInterface exif = null;
        int orientation = 0;

        try {
            exif = new ExifInterface(path);
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

            Log.i(APP_NAME, "orientation2 = " + orientation);
            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                int temp = y;
                y = bm.getHeight() - width - x; // when rotated, Y becomes X but from opposite side.
                x = temp;
                temp = width;
                width = height;
                height = temp;
                rotationAngle = 90;
            }
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            Log.i(APP_NAME, "rotationAngle = " + rotationAngle);
            Log.i(APP_NAME, "bm.width=" + bm.getWidth() + "; bm.height=" + bm.getHeight());
            Log.i(APP_NAME, "x=" + x + "; y=" + y + "; width=" + width + "; height=" + height);

            scaleX = (double) height / drawView.getHeight();
            scaleY = (double) width / drawView.getWidth();
            inSampleSize = Math.max(scaleX, scaleY);

            Matrix matrix = new Matrix();
            matrix.preRotate(rotationAngle);

            matrix.postScale((float) (1.0 / inSampleSize), (float) (1.0 / inSampleSize));
            croppedBitmap = Bitmap.createBitmap(bm, x, y, width, height, matrix, true);
        } catch (Exception ex) {
            Log.e(APP_NAME, ex.toString());
        }

        return croppedBitmap;
    }

    public void captureImage(View v) {
        // Example used: http://blog-emildesign.rhcloud.com/?p=590
        fileName = Environment.getExternalStorageDirectory() + File.separator + "image.jpg";

        Intent camShotIntent = new Intent(getApplicationContext(), CameraShot.class);
        startActivityForResult(camShotIntent, CAMSHOT_ACTIVITY);

        /*Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(file));
        camIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION,ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        startActivityForResult(camIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        */
    }

    public void analyzeImage(View v) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -1;
        float maxY = -1;

        float[] points = drawView.getPoints();

        for (int i = 0; i < points.length / 2; i++) {
            minX = Math.min(minX, points[i * 2]);
            minY = Math.min(minY, points[i * 2 + 1]);
            maxX = Math.max(maxX, points[i * 2]);
            maxY = Math.max(maxY, points[i * 2 + 1]);
        }
        Log.i(APP_NAME, "minX=" + minX + "; minY=" + minY + "; width=" + (maxX - minX) + "; height=" + (maxY - minY));
        croppedBmp = decodeCroppedBitmapFromFile(fileName, (int) (minX * inSampleSize), (int) (minY * inSampleSize),
                (int) ((maxX - minX) * inSampleSize), (int) ((maxY - minY) * inSampleSize),
                scrWidth, scrHeight);
        /*croppedBmp = decodeCroppedBitmapFromFile(fileName,(int)(minX*scaleX),(int)(minY*scaleY),
                (int)((maxX-minX)*scaleX), (int)((maxY-minY)*scaleY),
                scrWidth,scrHeight);*/

        drawView.setBitmap(croppedBmp);
        drawView.setDrawLines(false);
        drawView.invalidate();

        // Call uncle JNI
        ImageProc imgProc = new ImageProc();
        String msg = imgProc.getMessageFromJni();
        Log.i(APP_NAME, "JNI says: " + msg);

        Bitmap bmp = drawView.getBitmap();
        Bitmap.Config config = bmp.getConfig();
        Log.i(APP_NAME, "BITMAP CONFIG: " + config);

        int w = bmp.getWidth();
        int h = bmp.getHeight();
        //int [] pixels = new int[w*h];
        //bmp.getPixels(pixels,0,w,0,0,w,h);

        Log.i(APP_NAME, "Starting to segment...");
        columnsMap = imgProc.getSegments(bmp, w, h);

        drawView.setSegmentElements(columnsMap);
        drawView.setSegments(true);
    }


    public void learnSegments(View v) {
        //symbolList = drawView.getSymbols();// getSymbols() shall be called not only after drawing but after some time.

        if ((LabelManager.symbolList != null) && (LabelManager.symbolList.size() > 0)) {
            try {
                Intent learnIntent = new Intent(getApplicationContext(), LearnLibrary.class);
                //learnIntent.putExtra("andir.novruzoid.SymbolList",symbolList);
                startActivityForResult(learnIntent, LEARN_LIBRARY_ACTIVITY);
            }
            catch (Exception ex) {
                Log.e(APP_NAME,"Error.learnSegments : "+ex);
            }
        }
        else {
            // Show notification that, no elements.
            CharSequence text = "Zero segmentation elements";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }

    public void insertNewRecord(View v) {
        // Perform recognition here.
        Recognition recognize = new Recognition();
        if (!LabelManager.isbInitialized()) {
            LabelManager.loadHashes("JML");
        }
        String[] textResult = recognize.recognize(Recognition.RecognitionModel.SVM, "JML", columnsMap);

        Log.i(APP_NAME,"Recognitze. Columns : "+textResult.length);
        for (String column: textResult) {
            // print recognition results
        }

        /*
        String receiptDataJSON = "{ " +
                " 'obj_name' : 'Bolmart'," +
                " 'address' : 'Tbilisi Ave 61 A'," +
                " 'issue_date' : '01.07.2016 23:45'," +
                " 'recog_date' : '01.07.2016 23:45'," +
                " 'grand_total' : 12.34," +
                " 'x_coord' : '1232131'," +
                " 'y_coord' : '1232132'," +
                " 'records' : [ " +
                "               { 'rec_id': '001', 'rec_name': 'COCA COLA', 'rec_type' : 'ZERO', 'quan': 2.0, 'price' : 0.8, 'total': '1.6' }, " +
                "               { 'rec_id': '002', 'rec_name': 'SU', 'rec_type' : 'SLAVYANKA', 'quan': 5.0, 'price' : 0.4, 'total': '2.0' }, " +
                "               { 'rec_id': '003', 'rec_name': 'COREK', 'rec_type' : 'ZAVOD', 'quan': 1.0, 'price' : 0.4, 'total': '0.4' }, " +
                "               { 'rec_id': '004', 'rec_name': 'YAG', 'rec_type' : 'DOYARUSKA', 'quan': 1.0, 'price' : 2.8, 'total': '2.8' } " +
                "               ] " +
                "}";

        RecordManager.addRecord(this, receiptDataJSON);
        */
    }

    public void getOffers() {
        HashMap hmap = new HashMap<Integer, Double>();

        hmap.put(1, 1.2);
        hmap.put(2, 5);
        hmap.put(3, 567);
    }

}
