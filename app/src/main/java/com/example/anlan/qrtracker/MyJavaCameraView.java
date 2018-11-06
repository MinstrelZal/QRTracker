package com.example.anlan.qrtracker;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

public class MyJavaCameraView extends CameraBridgeViewBase implements Camera.PreviewCallback{
    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "MyJavaCameraView";

    private byte mBuffer[];
    private ArrayBlockingQueue<Mat> mFrameChain;
    private Thread mThread;
    private boolean mStopThread;

    protected Camera mCamera;
    protected ArrayBlockingQueue<MyJavaCameraFrame> mCameraFrame;
    private SurfaceTexture mSurfaceTexture;
    private int mPreviewFormat = ImageFormat.NV21;

    private static final int upperlimit = 100;
    private ImageScanner mScanner;
    byte[] grayData;
    Image image;
    int[] bound = null;
    private Rect lastArea;
    public Vector<Rect> mAreas;
    private Vector<int[]> mBounds;
    private boolean findqr,findqr2;
    public long mTime;
    private ArrayBlockingQueue<Mat> mGrays;
    private MyQRcode mQRcode;
    private MyARUco mARUco;

    private boolean DropFrame = false;
    private int FrameCounter = 0;

    public MyARUco getmARUco() {
        return this.mARUco;
    }

    public MyQRcode getmQRcode() {
        return this.mQRcode;
    }

    public MyJavaCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int[] getBounds(){
        if(MainActivity.useQR) {
            return mQRcode.getBounds();
        } else{
            return mARUco.getBounds();
        }
    }

    public float getmScale(){
        return mScale;
    }

    private void initMyVals(){
        // init scanner
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.ENABLE, 0);
        mScanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
        //mScanner.setConfig(Symbol.QRCODE, Config.X_DENSITY, 0);
        //mScanner.setConfig(Symbol.QRCODE, Config.Y_DENSITY, 0);
        //mScanner.enableCache(true);

        grayData = new byte[640*480];
        image = new Image(640, 480, "Y800");
        mAreas = new Vector<Rect>();
        mBounds = new Vector<int[]>();
        findqr = false;
        findqr2 = false;
        mTime=0;
        mGrays = new ArrayBlockingQueue<Mat>(MyJavaCameraView.upperlimit);
    }

    public static class MyJavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;

            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    mCamera = Camera.open();
                }
                catch (Exception e){
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if(mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            mCamera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }

            if (mCamera == null)
                return false;

            /* Now set camera parameters */
            try {
                Camera.Parameters params = mCamera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();

                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    Size frameSize = calculateCameraFrameSize(sizes, new MyJavaCameraView.MyJavaCameraSizeAccessor(), width, height);

                    /* Image format NV21 causes issues in the Android emulators */
                    if (Build.FINGERPRINT.startsWith("generic")
                            || Build.FINGERPRINT.startsWith("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")
                            || Build.MANUFACTURER.contains("Genymotion")
                            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                            || "google_sdk".equals(Build.PRODUCT))
                        params.setPreviewFormat(ImageFormat.YV12);  // "generic" or "android" = android emulator
                    else
                        params.setPreviewFormat(ImageFormat.NV21);

                    mPreviewFormat = params.getPreviewFormat();

                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
                    params.setPreviewSize((int)frameSize.width, (int)frameSize.height);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
                        params.setRecordingHint(true);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }

                    mCamera.setParameters(params);
                    params = mCamera.getParameters();

                    mFrameWidth = params.getPreviewSize().width;
                    mFrameHeight = params.getPreviewSize().height;

                    if ((getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) && (getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT))
                        mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
                    else
                        mScale = 0;

                    if (mFpsMeter != null) {
                        mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                    }

                    int size = mFrameWidth * mFrameHeight;
                    size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    mBuffer = new byte[size];

                    mCamera.addCallbackBuffer(mBuffer);
                    mCamera.setPreviewCallbackWithBuffer(this);

                    mFrameChain = new ArrayBlockingQueue<Mat>(MainActivity.upperlimit);

                    AllocateCache();

                    mCameraFrame = new ArrayBlockingQueue<MyJavaCameraView.MyJavaCameraFrame>(MainActivity.upperlimit);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                        mCamera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview");
                    mCamera.startPreview();
                }
                else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                while(mFrameChain.size() > 0){
                    Mat temp = mFrameChain.remove();
                    temp.release();
                }
            }
            if (mCameraFrame != null) {
                while(mCameraFrame.size() > 0){
                    MyJavaCameraView.MyJavaCameraFrame temp = mCameraFrame.remove();
                    temp.release();
                }
            }
        }
    }

    @Override
    protected boolean connectCamera(int width, int height) {
        if(MainActivity.useQR) {
            mQRcode = new MyQRcode();
        }
        else {
            mARUco = new MyARUco();
        }

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;

        mThread = new Thread(new MyJavaCameraView.CameraWorker());
        mThread.start();

        return true;
    }

    @Override
    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Waiting for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread =  null;
        }

        /* Now release camera */
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        //if (BuildConfig.DEBUG)
            //Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
        /*
        if(MainActivity.mStart && MainActivity.D_value.size() > 100 && MainActivity.T_value.size() > 100 && MainActivity.R_value.size() > 100){
            Log.d(MainActivity.TAG, "STOP!");
            Log.d(MainActivity.TAG, "AVG_D: "+MainActivity.avg_d);
            Log.d(MainActivity.TAG, "AVG_T: "+MainActivity.avg_t);
            Log.d(MainActivity.TAG, "AVG_R: "+MainActivity.avg_r);
            MainActivity.mStart = false;
            double std_d = 0;
            double std_t = 0;
            double std_r = 0;
            for(int i = 0; i < 100; i++){
                std_d += (MainActivity.D_value.get(i) - MainActivity.avg_d) * (MainActivity.D_value.get(i) - MainActivity.avg_d);
                std_t += (MainActivity.T_value.get(i) - MainActivity.avg_t)*(MainActivity.T_value.get(i) - MainActivity.avg_t);
                std_r += (MainActivity.R_value.get(i) - MainActivity.avg_r)*(MainActivity.R_value.get(i) - MainActivity.avg_r);
            }
            Log.d(MainActivity.TAG, "STD_D: "+Math.sqrt(std_d/100));
            Log.d(MainActivity.TAG, "STD_T: "+Math.sqrt(std_t/100));
            Log.d(MainActivity.TAG, "STD_R: "+Math.sqrt(std_r/100));
        }
        */
        synchronized (this) {
            // create new space for coming frame
            Mat mFrame = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
            MyJavaCameraView.MyJavaCameraFrame mCFrame = new MyJavaCameraView.MyJavaCameraFrame(mFrame, mFrameWidth, mFrameHeight);
            mFrame.put(0, 0, frame);

            if(MainActivity.useQR) {
                if (!MainActivity.mStart) {
                    if (mQRcode.findqr) {
                        mQRcode.findqr = false;
                    }
                } else {
                    // pass the frame to detect thread, which create a copy of that frame
                    try {
                        mQRcode.mGrays.put(mCFrame.gray().clone());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mQRcode.lastArea != null) {
                        postDelayed(mQRcode.trackTask, MainActivity.track_delay);
                    } else {
                        postDelayed(mQRcode.detectTask, MainActivity.detect_delay);
                    }
                    postDelayed(mQRcode.renderTask, MainActivity.render_delay);
                }
            }
            else{
                if (!MainActivity.mStart) {
                    if (mARUco.findaruco) {
                        mARUco.findaruco = false;
                    }
                } else {
                    // pass the frame to detect thread, which create a copy of that frame
                    try {
                        mARUco.mFrames.put(mCFrame.gray().clone());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mARUco.lastArea != null) {
                        postDelayed(mARUco.trackTask, MainActivity.track_delay);
                    } else {
                        postDelayed(mARUco.detectTask, MainActivity.detect_delay);
                    }
                    postDelayed(mARUco.renderTask, MainActivity.render_delay);
                }
            }

            // push the frame to the queue
            try{
                mFrameChain.put(mFrame);
                mCameraFrame.put(mCFrame);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    private class MyJavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

            return mRgba;
        }

        public MyJavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do{
                synchronized (MyJavaCameraView.this) {
                    try {
                        if (mFrameChain.size() > 0) {
                            Mat mFrame = mFrameChain.take();
                            MyJavaCameraView.MyJavaCameraFrame mCFrame = mCameraFrame.take();
                            /*
                            if(!DropFrame || (DropFrame && FrameCounter < 1)) {
                                deliverAndDrawFrame(mCFrame);
                                FrameCounter += 1;
                            }
                            else{
                                FrameCounter = 0;
                            }
                            */

                            if(!DropFrame || (DropFrame && FrameCounter == 2)) {
                                deliverAndDrawFrame(mCFrame);
                                FrameCounter = 0;
                            }
                            else if(FrameCounter < 2){
                                FrameCounter += 1;
                            }

                            mFrame.release();
                            mCFrame.release();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }


}
