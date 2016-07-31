/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package andir.novruzoid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

import java.io.File;
import java.io.FileOutputStream;

public class CameraShot extends Activity implements OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "Novruzoid";
    SurfaceView cameraView;
    SurfaceHolder surfaceHolder;
    Camera camera;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.camshot);

        cameraView = (SurfaceView) this.findViewById(R.id.CameraView);

        surfaceHolder = cameraView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        cameraView.setFocusable(true);
        cameraView.setFocusableInTouchMode(true);
        cameraView.setClickable(true);
        cameraView.setOnClickListener(this);
    }

    public void surfaceCreated(SurfaceHolder sHolder) {
        camera = Camera.open();
        try {
            Camera.Parameters parameters = camera.getParameters();
            // Defining parameters
            parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //parameters.setPictureSize(2448,3264);
            parameters.setPictureSize(2048,1536);

            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                Log.e(TAG, "Orientation set to portrait");
                parameters.set("orientation", "portrait");
                parameters.setRotation(90);
                camera.setDisplayOrientation(90); // Supported by API Level = 8 (critical)
            } else {
                //parameters.set("orientation", "landscape");
                Log.e(TAG, "Orientation was portrait. No need to change.");
                parameters.setRotation(0);
                camera.setDisplayOrientation(0);
            }
            camera.setParameters(parameters);
            camera.setPreviewDisplay(sHolder);
        } catch (Exception ex) {
            camera.release();
            Log.v(TAG, ex.getMessage());
        } finally {
            camera.startPreview();
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        camera.stopPreview();
        camera.release();
    }

    public void onClick(View view) {
        Log.i(TAG, "Clicked to capture! ");
        Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {

            public void onAutoFocus(boolean focusResult, Camera arg1) {
                Log.i(TAG, "Inside the function! ");
                if (focusResult) {
                    Log.i(TAG, "Focus done! ");
                    ImageCaptureCallback captureCallback = new ImageCaptureCallback();
                    captureCallback.setContentResolver(getContentResolver());
                    camera.takePicture(null, null, captureCallback);
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
            }
        };
        camera.autoFocus(autoFocusCB);

    }
}

class ImageCaptureCallback implements Camera.PictureCallback {

    private static final String TAG = "CamShot.PicCallback";
    ContentResolver contResolver;

    public void setContentResolver(ContentResolver resolver) {
        contResolver = resolver;
    }

    public void onPictureTaken(byte[] data, Camera cam) {
        Log.e(TAG, "dddata size: " + data.length);
        try {
            File imgFile = new File(Environment.getExternalStorageDirectory(), "image.jpg");

            FileOutputStream fos = new FileOutputStream(imgFile);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            Log.e(TAG, "Error: " + ex);
        }
    }
    /*    public void onPictureTaken(byte[] data, Camera camera) {
    Uri imageFileUri = contResolver.insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());
    try {
    OutputStream imageFileOS = contResolver.openOutputStream(imageFileUri);
    imageFileOS.write(data);
    imageFileOS.flush();
    imageFileOS.close();
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
    camera.startPreview();
    }*/
}
