package com.example.vincent.opticalcam;


import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.bluetooth.*;
import android.widget.Button;


import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//OpeCV Classes
public class MainActivity_show_camera extends Activity implements CvCameraViewListener2 {

    //Used for logging sucess or failure messages
    private static final String TAG = "OCVSample::Activity";


    //Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;
    private Camera myCamera;

    //Used in Camera selection from menu (when implemented)
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    // These variables are used (at the moment to fix camera orientation from 270deg to 0 deg
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG,"OpenCV loaded sucessfully");
                    mOpenCvCameraView.enableView();
                }break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"called on Create");
        //Set Activity layout for Camera Preview
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_show_camera );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //Load Camera Surface
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);


        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);


        mOpenCvCameraView.setCvCameraViewListener(this);

        //Bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG,"Internal OpenCV library not found. Using OpenCV Manager for Initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,this,mLoaderCallback);
        }else{
            Log.d(TAG,"OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height){
        Log.d(TAG, "Received image size " + width + ", " + height);
        mRgba = new Mat(height,width,CvType.CV_8UC4);
        mRgbaF = new Mat(height,width,CvType.CV_8UC4);
        mRgbaT = new Mat(width,width,CvType.CV_8UC4);
    }

    public void onCameraViewStopped(){
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame){
        //TODO

        mRgba = inputFrame.rgba();

        //Rotate mRgba 90 deg
//        Core.transpose(mRgba,mRgbaT);
//        Imgproc.resize(mRgbaT,mRgbaF,mRgba.size(),0,0,0);
//        Core.flip(mRgbaF,mRgba,1);
        return mRgba;
    }

    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }
        }
    };

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = "Heute";//new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }


    /*public void SaveImage(View view){
        Mat mInter= new Mat(mRgba.width(),mRgba.height(),CvType.CV_8UC4);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String filename = "TestBild.png";
        File file = new File(path, filename);
        Boolean my_boolean = null;
        filename = file.toString();
        mInter = mRgba;
        mInter.convertTo(mInter,CvType.CV_8UC3);
        List<Mat> mInterColors = new ArrayList<Mat>();
        Core.split(mInter, mInterColors);
        Mat mysave = mInterColors.get(0);
        mInterColors.set(0,mInterColors.get(2));
        mInterColors.set(2,mysave);
        Core.merge(mInterColors,mInter);

        if(mRgba.height() > mRgba.width()){
            Core.flip(mRgba,mInter,1);
            my_boolean = Imgcodecs.imwrite(filename, mInter);
        }
        else{
            my_boolean = Imgcodecs.imwrite(filename, mInter);
        }

        if (my_boolean){
            Log.i(TAG, "SUCCESS writing image to external storage");
        }
        else{
            Log.i(TAG, "Fail writing image to external storage");
        }
    }*/

}
