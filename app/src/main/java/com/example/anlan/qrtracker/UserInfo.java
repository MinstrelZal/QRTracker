package com.example.anlan.qrtracker;

import android.util.Log;

public class UserInfo {
    // participant
    private static final String TAG = "QRTRACKER";
    private String name;
    private String age;
    private String gender;
    private String tech_level;
    private String arexp;

    // latency
    private static final String[] parameters = {"Recognition", "Tracking", "Rendering"};
    private long detect_delay = 0;
    private long track_delay = 0;
    private long render_delay = 0;

    // application values
    private long mTime = 0;
    private long totalTime = 0;
    private double dist = 0;

    // nasa tlx
    private String mental;
    private String successful;
    private String frustration;

    // last question
    private String use;

    public void setName(String name){
        this.name = name;
        Log.d(TAG, "Name: " + name);
    }

    public void setAge(String age){
        this.age = age;
        Log.d(TAG, "Age: " + age);
    }

    public void setGender(String gender){
        this.gender = gender;
        Log.d(TAG, "Gender: " + gender);
    }

    public void setTech_level(String tech_level){
        this.tech_level = tech_level;
        Log.d(TAG, "Tech Level: " + tech_level);
    }

    public void setArexp(String arexp){
        this.arexp = arexp;
        Log.d(TAG, "AR Exp: " + arexp);
    }

    public void setDetect_delay(long detect_delay){
        this.detect_delay = detect_delay;
        Log.d(TAG, "D Delay: " + detect_delay);
    }

    public void setTrack_delay(long track_delay){
        this.track_delay = track_delay;
        Log.d(TAG, "T Delay: " + track_delay);
    }

    public void setRender_delay(long render_delay){
        this.render_delay = render_delay;
        Log.d(TAG, "R Delay: " + render_delay);
    }

    public void setMTime(long mTime){
        this.mTime = mTime;
        Log.d(TAG, "mTime: " + mTime);
    }

    public void setTotalTime(long totalTime){
        this.totalTime = totalTime;
        Log.d(TAG, "totalTime: " + totalTime);
    }

    public void setDist(double dist){
        this.dist = dist;
        Log.d(TAG, "distance: " + dist);
    }

    public void setMental(String mental){
        this.mental = mental;
        Log.d(TAG, "Mental Demand: "+ mental);
    }

    public void setSuccessful(String successful){
        this.successful = successful;
        Log.d(TAG, "Successful: " + successful);
    }

    public void setFrustration(String frustration){
        this.frustration = frustration;
        Log.d(TAG, "Frustration: " + frustration);
    }

    public void setUse(String use){
        this.use = use;
        Log.d(TAG, "Use: " + use);
    }

    @Override
    public String toString(){
        return name+","+gender+","+tech_level+","+age+","+arexp+","
                +parameters[0]+","+detect_delay+","+parameters[1]+","+track_delay+","+parameters[2]+","+render_delay+","
                +mTime+","+totalTime+","+dist+","
                +mental+","+successful+","+frustration+","
                +use+"\n";
    }
}
