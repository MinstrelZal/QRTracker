package com.example.anlan.qrtracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.rajawali3d.view.ISurface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import static java.lang.Math.pow;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String TAG = "QRTRACKER";

    public static final int CAMERA_REQ_CODE = 111;
    public static final Scalar COLOR_GREEN = new Scalar(0, 255, 0);
    public static final Scalar COLOR_RED = new Scalar(255, 0, 0);
    public static final Scalar COLOR_BLUE = new Scalar(0, 0, 255);
    public static final int upperlimit = 10000;

    public static final boolean useQR = true;

    public static boolean mStart = false;

    private Mat mRgba;
    //private Mat mGray;

    public static MyRenderer mRenderer = null;

    public static double avg_d = 0;
    public static long count_d = 0;
    public static double avg_t = 0;
    public static long count_t = 0;
    public static double avg_r = 0;
    public static long count_r = 0;

    public static long detect_delay = 0;
    public static long track_delay = 0;
    public static long render_delay = 0;

    public static Vector<Long> D_value = new Vector<Long>();
    public static Vector<Long> T_value = new Vector<Long>();
    public static Vector<Long> R_value = new Vector<Long>();

    private double mDistance;
    private long mTime;
    private long mTime2;
    private long mStartTime;

    private UserInfo mUserInfo;

    private static final String FILENAME = "QRtrackerResult.csv";

    //private int skiped_frame = 0;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("iconv");
        System.loadLibrary("zbar");
    }
    public void showAlertDialogButtonClicked(View view) {

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations");
        long temp = System.currentTimeMillis()-mStartTime;
        builder.setMessage("Time:\t\t\t\t\t\t "+mTime+"\nTotal Time:\t "+temp+"\nDistance:\t\t\t "+(int)mDistance);

        // add a button
        builder.setPositiveButton("OK", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        mUserInfo.setMTime(mTime);
        mUserInfo.setTotalTime(temp);
        mUserInfo.setDist(mDistance);
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE" };
    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestPermission() {
        verifyStoragePermissions(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Log.i(TAG, "You may not be able to use CAMERA!\n");
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQ_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQ_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                else {
                    Log.i(TAG, "You may not be able to use CAMERA!\n");
                }
                return ;
            }
        }
    }

    private MyJavaCameraView javaCameraView;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    javaCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void initTimeVals(){
        avg_d = 0;
        count_d = 0;
        avg_t = 0;
        count_t = 0;
        avg_r = 0;
        count_r = 0;
    }

    private void printTimeVals(){
        Log.i(TAG, "avg_D: "+avg_d);
        Log.i(TAG, "avg_T: "+avg_t);
        Log.i(TAG, "avg_R: "+avg_r);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserInfo = new UserInfo();

        // keep screen always light
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        requestPermission();

        // init java camera view
        javaCameraView = (MyJavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setMaxFrameSize(640, 480);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);


        // init button
        Button trackStartButton = (Button) findViewById(R.id.track_start);
        Button trackStopButton = (Button) findViewById(R.id.track_stop);
        Button delayDetectButton = (Button) findViewById(R.id.delay_detect);
        Button delayTrackButton = (Button) findViewById(R.id.delay_track);
        Button delayRenderButton = (Button) findViewById(R.id.delay_render);
        trackStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Start");
                initTimeVals();
                mRenderer.removeChild();
                mStart = true;
                mStartTime = System.currentTimeMillis();
            }
        });
        trackStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStart = false;
                printTimeVals();
                mRenderer.removeChild();
                detect_delay = 0;
                track_delay = 0;
                render_delay = 0;
                mUserInfo.setMTime(0);
                mUserInfo.setTotalTime(0);
                mUserInfo.setDist(0);
                mUserInfo.setDetect_delay(0);
                mUserInfo.setTrack_delay(0);
                mUserInfo.setRender_delay(0);
                Log.d(TAG, "Stop");
            }
        });
        delayDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                mStart = false;
                mRenderer.removeChild();
                detect_delay += 250;
                mUserInfo.setDetect_delay(detect_delay);
            }
        });
        delayTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                mStart = false;
                mRenderer.removeChild();
                track_delay += 250;
                mUserInfo.setTrack_delay(track_delay);
            }
        });
        delayRenderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                mStart = false;
                mRenderer.removeChild();
                render_delay += 250;
                mUserInfo.setRender_delay(render_delay);
            }
        });

        // init rendering surface
        org.rajawali3d.view.SurfaceView mSurface = new org.rajawali3d.view.SurfaceView(this);
        mSurface.setScaleX((float)1.75);
        mSurface.setScaleY((float)1.75);
        mSurface.setFrameRate(60.0);
        mSurface.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
        mSurface.setTransparent(true);
        addContentView(mSurface, javaCameraView.getLayoutParams());
        final float mXdpi = getBaseContext().getResources().getDisplayMetrics().xdpi;
        mRenderer = new MyRenderer(this, mXdpi, 640, 480);
        mSurface.setSurfaceRenderer(mRenderer);
        javaCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mStart) {
                    Log.d(TAG, "It works");
                    // Event coordinates
                    //Log.d(TAG, "X:" + event.getX() + ", Y:" + event.getY());
                    //Log.d(TAG, "RX:" + event.getRawX() + ", RY:" + event.getRawY());


                    // X and Y offset
                    float Xoffset = (javaCameraView.getWidth() - javaCameraView.getmScale() * 640) / 2;
                    //Log.d(TAG, "Xoffset:" + Xoffset);
                    float Yoffset = (javaCameraView.getHeight() - javaCameraView.getmScale() * 480) / 2;
                    //Log.d(TAG, "Yoffset:" + Yoffset);
                    // Center of the sphere
                    double x1 = mRenderer.x1 * javaCameraView.getmScale() + Xoffset;
                    double y1 = (mRenderer.y1 * javaCameraView.getmScale() + Yoffset);
                    double rad = Math.sqrt(pow(x1 - event.getX(), 2) + pow(y1 - event.getY(), 2));
                    double rad1 = mRenderer.rad * javaCameraView.getmScale();
                    if (rad > rad1) {
                        Log.d(TAG, "Outside");
                        Log.d(TAG, "Distance: " + rad + ", Radius:" + rad1);
                    } else {
                        //Log.d(TAG, "Inside");
                        //Log.d(TAG, "Distance: " + rad + ", Radius:" + rad1);
                        mTime = System.currentTimeMillis() - javaCameraView.getmQRcode().mTime;
                        mDistance = rad;
                        mStart = false;
                        showAlertDialogButtonClicked(javaCameraView);
                        mRenderer.removeChild();
                        javaCameraView.getmQRcode().findqr2=false;

                    }
                }
                    return true;
            }
        });

        // UserInfo
        Button mUser = (Button)findViewById(R.id.info);
        mUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        // experiment result
        Spinner mental = (Spinner)findViewById(R.id.mental_select);
        Spinner successful = (Spinner)findViewById(R.id.success_select);
        Spinner frustration = (Spinner)findViewById(R.id.frustration_select);
        mental.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] mentalvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setMental(mentalvalues[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] mentalvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setMental(mentalvalues[0]);
                }
            }
        });
        successful.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] successfulvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setSuccessful(successfulvalues[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] successfulvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setSuccessful(successfulvalues[0]);
                }
            }
        });
        frustration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] frustrationvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setFrustration(frustrationvalues[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] frustrationvalues = getResources().getStringArray(R.array.nasa_tlx);
                if(mUserInfo != null){
                    mUserInfo.setFrustration(frustrationvalues[0]);
                }
            }
        });

        // last question
        Spinner use = (Spinner) findViewById(R.id.use_select);
        use.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] usevalues = getResources().getStringArray(R.array.yorn);
                if(mUserInfo != null){
                    mUserInfo.setUse(usevalues[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String[] usevalues = getResources().getStringArray(R.array.yorn);
                if(mUserInfo != null){
                    mUserInfo.setUse(usevalues[0]);
                }
            }
        });

        // Save file
        Button mSave = (Button)findViewById(R.id.save);
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory(), FILENAME);
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    try{
                        FileOutputStream fos = new FileOutputStream(file, true);
                        fos.write(mUserInfo.toString().getBytes());
                        fos.close();
                        Log.d(TAG, mUserInfo.toString());
                    }
                    catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
                else{
                    Log.d(TAG, "No SD Card");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(data == null){
            return;
        }
        if(requestCode == 0) {
            String[] result = data.getStringArrayExtra("userinfo");
            mUserInfo.setName(result[0]);
            mUserInfo.setGender(result[1]);
            mUserInfo.setTech_level(result[2]);
            mUserInfo.setAge(result[3]);
            mUserInfo.setArexp(result[4]);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
        else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        //mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();

        // draw bounding box here
        if(useQR) {
            if (javaCameraView.getmQRcode().mAreas.size() > 0) {
                Rect mArea = javaCameraView.getmQRcode().mAreas.remove(0);
                Imgproc.rectangle(mRgba, mArea.br(), mArea.tl(), COLOR_GREEN, 2);
            }
        }
        else{
            if (javaCameraView.getmARUco().mAreasForBound.size() > 0) {
                Rect mArea = javaCameraView.getmARUco().mAreasForBound.remove(0);
                Imgproc.rectangle(mRgba, mArea.br(), mArea.tl(), COLOR_GREEN, 2);
            }
        }

        return mRgba;
    }

}
