package com.example.anlan.qrtracker;

import android.os.Environment;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.MarkerDetector;

public class MyARUco {
    public Rect lastArea;
    public boolean findaruco;
    public ArrayBlockingQueue<Mat> mFrames;
    public Vector<Rect> mAreasForBound;

    private Vector<Rect> mAreasForRender;
    private MarkerDetector mDetector;
    private CameraParameters mCamParam;

    private static final float MARKER_SIZE = (float) 0;

    private int x1,x2,y1,y2;

    public int[] getBounds(){
        return new int[] {x1,x2,y1,y2};
    }

    public MyARUco(){
        this.findaruco = false;
        this.mFrames = new ArrayBlockingQueue<Mat>(MainActivity.upperlimit);
        this.mCamParam = new CameraParameters();
        this.mCamParam.readFromFile(Environment.getExternalStorageDirectory().toString() + "/camCalib/camCalibData.csv");
        this.mDetector = new MarkerDetector();
        this.mAreasForRender = new Vector<Rect>();
        this.mAreasForBound = new Vector<Rect>();
    }

    private void tracking(Mat mRgba){
        if(lastArea != null){
            long startTracking = System.currentTimeMillis();
            Mat patch = new Mat(mRgba, lastArea);
            Vector<Marker> mDetectedMarkers = new Vector<Marker>();
            mDetector.detect(patch, mDetectedMarkers, mCamParam, MARKER_SIZE);
            patch.release();
            long stopTracking = System.currentTimeMillis();
            MainActivity.avg_t = (MainActivity.avg_t * MainActivity.count_t - startTracking + stopTracking) / (MainActivity.count_t + 1);
            MainActivity.count_t += 1;
            if (mDetectedMarkers.size() > 0) {
                Marker mMarker = mDetectedMarkers.get(0);
                mMarker.renewPoints(lastArea);

                Rect mArea = mMarker.getRect();
                mAreasForRender.add(mArea);
                mAreasForBound.add(mArea);
            }
            else{
                lastArea = null;
                findaruco = false;
            }
        }
    }

    public Runnable trackTask = new Runnable() {
        @Override
        public void run() {
            try {
                Mat mRgba = mFrames.take();
                tracking(mRgba);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    };

    private void detecting(Mat mRgba){
        if(lastArea == null){
            long startDetection = System.currentTimeMillis();
            Vector<Marker> mDetectedMarkers = new Vector<Marker>();
            mDetector.detect(mRgba, mDetectedMarkers, mCamParam, MARKER_SIZE);
            long stopDetection = System.currentTimeMillis();
            MainActivity.avg_d = (MainActivity.avg_d * MainActivity.count_d - startDetection + stopDetection) / (MainActivity.count_d + 1);
            MainActivity.count_d += 1;
            if (mDetectedMarkers.size() > 0) {
                Marker mMarker = mDetectedMarkers.get(0);

                Rect mArea = mMarker.getRect();
                mAreasForRender.add(mArea);
                mAreasForBound.add(mArea);
            }
            else{
                lastArea = null;
                findaruco = false;
            }
        }
    }

    public Runnable detectTask = new Runnable() {
        @Override
        public void run() {
            try{
                Mat mRgba = mFrames.take();
                detecting(mRgba);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    };

    private void rendering(Rect mArea){
        long StartRender = System.currentTimeMillis();
        if (!findaruco) {
            MainActivity.mRenderer.addSphere((float)mArea.height/2, (mArea.tl().x+mArea.br().x)/2, (mArea.tl().y+mArea.br().y)/2, 0.0);
            findaruco = true;
        } else {
            MainActivity.mRenderer.moveSelectedObject((mArea.tl().x+mArea.br().x)/2, (mArea.tl().y+mArea.br().y)/2, 0.0);
        }
        long stopRender = System.currentTimeMillis();
        MainActivity.avg_r = (MainActivity.avg_r * MainActivity.count_r - StartRender + stopRender) / (MainActivity.count_r + 1);
        MainActivity.count_r += 1;
    }

    public Runnable renderTask = new Runnable() {
        @Override
        public void run() {
            if(mAreasForRender.size() > 0){
                Rect mArea = mAreasForRender.remove(0);
                rendering(mArea);
            }
            if (!findaruco) {
                MainActivity.mRenderer.removeChild();
            }
        }
    };
}
