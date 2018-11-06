package com.example.anlan.qrtracker;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;

import java.util.Random;

public class MyRenderer extends Renderer {

    private Context context;
    private DirectionalLight directionalLight;
    private Material material;
    private final Random random = new Random();
    private float mXdpi;
    private float half_FrameSizeX;
    private float half_FrameSizeY;
    private static final String TAG = "QRTRACKER";
    public double x1,y1,rad;

    public MyRenderer(Context context, float mXdpi, int FrameSizeX, int FrameSizeY){
        super(context);
        this.context = context;
        setFrameRate(60);
        this.mXdpi = mXdpi;
        this.half_FrameSizeX = FrameSizeX / 2;
        this.half_FrameSizeY = FrameSizeY / 2;
    }

    public void initScene(){
        directionalLight = new DirectionalLight(1f, .2f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
        getCurrentScene().addLight(directionalLight);

        material = new Material();
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(0);

        Texture earthTexture = new Texture("Earth", R.drawable.earthtruecolor_nasa_big);

        try {
            material.addTexture(earthTexture);
        }
        catch (ATexture.TextureException error){
            error.printStackTrace();
        }
        getCurrentCamera().setZ(6f);
    }

    @Override
    public void onTouchEvent(MotionEvent event){
        Log.d(TAG, "It works");
    }
    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j){

    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime){
        try {
            super.onRender(elapsedTime, deltaTime);
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
        catch (IndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }

    private double pxToCMX(double original){
        return (original - half_FrameSizeX) * 2.54 / mXdpi;
    }
    private double cmxToPx(double original) { return (mXdpi*original/2.54)+half_FrameSizeX;}

    private double pxToCMY(double original){
        return (half_FrameSizeY - original) * 2.54 / mXdpi;
    }
    private double cmxToPY(double original) { return (mXdpi*original/2.54)+half_FrameSizeY;}

    public void addSphere(double radius, double x, double y, double z){
        int count = getCurrentScene().getNumChildren();
        if(count == 0) {
            x1 = x;
            y1 = y;
            rad = radius;

            Sphere earthSphere = new Sphere((float) (radius * 2.54 / mXdpi), 24, 24);
            earthSphere.setMaterial(material);
            earthSphere.setPosition(pxToCMX(x), pxToCMY(y), z);

            RotateOnAxisAnimation anim = new RotateOnAxisAnimation(Vector3.Axis.Y, 360);
            anim.setTransformable3D(earthSphere);
            anim.setDurationMilliseconds(3000 + (int) (random.nextDouble() * 5000));
            anim.setRepeatMode(Animation.RepeatMode.INFINITE);
            getCurrentScene().registerAnimation(anim);
            anim.play();
            getCurrentScene().addChild(earthSphere);
        }
    }

    public void removeChild(){
        try {
            final int count = getCurrentScene().getNumChildren();
            for (int i = 0; i < count; i++) {
                final Object3D child = getCurrentScene().getChildrenCopy().get(i);
                getCurrentScene().removeChild(child);
                child.destroy();
            }
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
        catch (IndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }

    public void moveSelectedObject(double x, double y, double z) {
        x1=x;y1=y;
        try {
            final int count = getCurrentScene().getNumChildren();
            for (int i = 0; i < count; i++) {
                final Object3D child = getCurrentScene().getChildrenCopy().get(i);
                child.setX(pxToCMX(x));
                child.setY(pxToCMY(y));
                child.setZ(z);
                //Log.d(TAG,"BB"+ cmxToPx(child.getBoundingBox().getTransformedMin().x)+","+cmxToPY(child.getBoundingBox().getTransformedMin().y));
            }
        }
        catch (IndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }
}
