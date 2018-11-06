package com.example.anlan.qrtracker;

import android.util.Log;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

public class MyQRcode {
    private static final String TAG = "QRTRACKER";
    private final boolean detail = false;
    public Rect lastArea;
    public Vector<Rect> mAreas;
    public boolean findqr,findqr2;
    public long mTime;
    public ArrayBlockingQueue<Mat> mGrays;

    private ImageScanner mScanner;
    private byte[] grayData;
    private Image image;
    private int[] bound = null;
    public Vector<int[]> mBounds;
    private int x1,x2,y1,y2;

    public int[] getBounds(){
        return new int[] {x1,x2,y1,y2};
    }
    public MyQRcode(){
        // init scanner
        this.mScanner = new ImageScanner();
        this.mScanner.setConfig(0, Config.ENABLE, 0);
        this.mScanner.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
        //mScanner.setConfig(Symbol.QRCODE, Config.X_DENSITY, 0);
        //mScanner.setConfig(Symbol.QRCODE, Config.Y_DENSITY, 0);
        //mScanner.enableCache(true);

        this.grayData = new byte[640*480];
        this.image = new Image(640, 480, "Y800");
        this.mAreas = new Vector<Rect>();
        this.mBounds = new Vector<int[]>();
        this.findqr = false;
        this.findqr2 = false;
        this.mTime=0;
        this.mGrays = new ArrayBlockingQueue<Mat>(MainActivity.upperlimit);
    }

    private void tracking(Mat mGray){
        if(lastArea != null){
            int symbolCount = 0;
            try {
                long startTracking = System.currentTimeMillis();
                Mat patch = new Mat(mGray, lastArea);
                byte small[] = new byte[lastArea.width * lastArea.height];
                patch.get(0, 0, small);
                Image tempimg = new Image(lastArea.width, lastArea.height, "Y800");
                tempimg.setData(small);
                symbolCount = mScanner.scanImage(tempimg);
                //Log.i(TAG, "" + symbolCount);
                patch.release();
                long stopTracking = System.currentTimeMillis();
                if(detail){
                    Log.d(TAG, "T:"+(stopTracking-startTracking + MainActivity.track_delay));
                }
                MainActivity.avg_t = (MainActivity.avg_t * MainActivity.count_t - startTracking + stopTracking + MainActivity.track_delay) / (MainActivity.count_t + 1);
                MainActivity.count_t += 1;
                //MainActivity.T_value.add(stopTracking-startTracking);
            }
            catch (CvException e){
                e.printStackTrace();
                symbolCount = 0;
            }
            if (symbolCount > 0) {
                Symbol temp = mScanner.getResults().iterator().next();

                bound = temp.getBounds();
                bound[0] += lastArea.tl().x;
                bound[1] += lastArea.tl().y;

                //Log.i(MainActivity.TAG, "("+bound[0]+","+bound[1]+")");
                Point p1 = new Point(bound[0], bound[1]);
                Point p4 = new Point(bound[0] + bound[2], bound[1] + bound[3]);
                Rect mArea = new Rect(p1, p4);
                Point p2 = new Point(bound[0] - 100, bound[1] - 100);
                Point p3 = new Point(bound[0] + bound[2] + 100, bound[1] + bound[3] + 100);
                lastArea = new Rect(p2, p3);

                mAreas.add(mArea);
                mBounds.add(bound);
            }
            else{
                lastArea = null;
                findqr = false;
            }
        }
    }

    public Runnable trackTask = new Runnable() {
        @Override
        public void run() {
            try {
                Mat mGray = mGrays.take();
                tracking(mGray);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    };

    private void detecting(Mat mGray){
        if(lastArea == null){
            int symbolCount = 0;
            try {
                long startDetection = System.currentTimeMillis();
                mGray.get(0, 0, grayData);
                image.setData(grayData);
                symbolCount = mScanner.scanImage(image);
                //Log.i(MainActivity.TAG, "d");
                long stopDetection = System.currentTimeMillis();
                if(detail){
                    Log.d(TAG, "D:"+(stopDetection-startDetection+MainActivity.detect_delay));
                }
                MainActivity.avg_d = (MainActivity.avg_d * MainActivity.count_d - startDetection + stopDetection + MainActivity.detect_delay) / (MainActivity.count_d + 1);
                MainActivity.count_d += 1;
                //MainActivity.D_value.add(stopDetection-startDetection);
            }
            catch (CvException e){
                e.printStackTrace();
                symbolCount = 0;
            }
            if (symbolCount > 0) {
                Symbol temp = mScanner.getResults().iterator().next();

                bound = temp.getBounds();

                //Log.i(MainActivity.TAG, "("+bound[0]+","+bound[1]+")");
                Point p1 = new Point(bound[0], bound[1]);
                Point p4 = new Point(bound[0] + bound[2], bound[1] + bound[3]);
                Rect mArea = new Rect(p1, p4);
                Point p2 = new Point(bound[0] - 100, bound[1] - 100);
                Point p3 = new Point(bound[0] + bound[2] + 100, bound[1] + bound[3] + 100);
                lastArea = new Rect(p2, p3);

                mAreas.add(mArea);
                mBounds.add(bound);
            }
            else{
                lastArea = null;
                findqr = false;
            }
        }
    }

    public Runnable detectTask = new Runnable() {
        @Override
        public void run() {
            try{
                Mat mGray = mGrays.take();
                detecting(mGray);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    };

    private void rendering(int[] bound){
        long startRender = System.currentTimeMillis();
        if (!findqr) {
            MainActivity.mRenderer.addSphere((float)bound[2] / 2, bound[0] + (float)bound[2] / 2, bound[1] + (float)bound[3] / 2, 0.0);
            findqr = true;
        } else {
            MainActivity.mRenderer.moveSelectedObject(bound[0] + (float)bound[2] / 2, bound[1] + (float)bound[3] / 2, 0.0);
        }
        long stopRender = System.currentTimeMillis();
        if(detail){
            Log.d(TAG, "R:"+(stopRender - startRender + MainActivity.render_delay));
        }
        MainActivity.avg_r = (MainActivity.avg_r * MainActivity.count_r - startRender + stopRender + MainActivity.render_delay) / (MainActivity.count_r + 1);
        MainActivity.count_r += 1;
        //MainActivity.R_value.add(stopRender-startRender);
    }

    public Runnable renderTask = new Runnable() {
        @Override
        public void run() {
            if(mBounds.size() > 0){
                int[] bound = mBounds.remove(0);
                if (!findqr2) {
                    findqr2 = true;
                    mTime = System.currentTimeMillis();
                    Log.d(TAG, "mTime" + mTime);
                }
                rendering(bound);
            }
            if(!findqr){
                MainActivity.mRenderer.removeChild();
            }
        }
    };
}
