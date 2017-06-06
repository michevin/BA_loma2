package com.example.simon.irimaging;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.flironesdk.Device;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static java.lang.Thread.MAX_PRIORITY;

@SuppressWarnings("deprecation")
/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * The Fragment contains the functions needed for the primary purpose of the application which
 * is to take one optical and one thermal image as well as measure the distance to the object
 * at the same time.
 * {@link Fragment}
 */
public class CameraFragment extends Fragment implements Device.Delegate, FrameProcessor.Delegate{
    private MainActivity mActivity;
    private SharedPreferences prefs;
    /**
     * Definition of the variables needed to capture and preview the optical image
     */
    private Camera mCamera = null;
    private CameraPreview mCameraPreview;
    private String timeStamp;
    private ExifInterface exifOptic;
    private static final String FOV_OPTICAL = "61.1";
    private String Distance_Optic;
    private String opticalPath;
    private int DistanceOffset = 10;
    private String DistanceSave = "0";

    /**
     * Definition of the variables needed to capture the thermal image
     */
    private  volatile Device flirDevice = null;
    private FrameProcessor frameProcessor;
    private Bitmap picture;
    private static final String FOV_THERMAL = "46.3";
    private ExifInterface exifTherm;
    private String Distance_Therm;
    private final int cameraXOffset = (int)(5.4f);
    private String thermalPath;

    /**
     * Definition of the Runnable needed to read the Bluetooth input stream
     */
    private Runnable readBufferRunnable;
    private List<String> arrayBuffer;

    /**
     * Required empty public constructor
     */
    public CameraFragment() {
    }

    /**
     * Creates the view fragment for the camera, initialise the Layout.
     * inflates the Fragment into the MainActivity.
     * @param inflater: The LayoutInflater object that can be used to inflate
     *                  any views in the fragment
     * @param container: If non-null, this is the parent view that the fragment's
     *                   UI should be attached to. The fragment should not add the view itself,
     *                   but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState: If non-null, this fragment is being re-constructed
     *                            from a previous saved state as given here.
     * @return cameraView -> view that is inflated into MainActivity.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        System.gc();
        //initialising the view fragment for the camera
        mActivity = (MainActivity) getActivity();
        prefs = mActivity.getSharedPreferences("myPrefs",MODE_PRIVATE);
        View cameraView = inflater.inflate(R.layout.fragment_camera, container, false);

        /*Optical*/
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(getActivity(), mCamera);
        final FrameLayout preview = (FrameLayout) cameraView.findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
        setCameraParameters(mCamera);

        /*Thermal*/
        frameProcessor = new FrameProcessor(getContext(), this,
                            EnumSet.of(RenderedImage.ImageType.ThermalLinearFlux14BitImage));

        /*Bluetooth*/
        arrayBuffer = new ArrayList<>();
        if (!mActivity.streamOn) {
            fixConnect();
        }

        final ImageButton captureButton =
                            (ImageButton) cameraView.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDistance();
                take_thermal_picture();
                mCamera.takePicture(null, null, mPicture);
                Toast.makeText(getContext(), "Picture Taken", Toast.LENGTH_SHORT).show();
            }
        });
        return cameraView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onResume(){
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        try{
            Device.startDiscovery(this.getContext(), this);
        }catch(IllegalStateException e){
           //suppress discovery already started error
        }
        try {
            readBufferRunnable = new Runnable() {
                @Override
                public void run() {
                    readBuffer();
                }
            };
            Thread threadRead = new Thread(readBufferRunnable);
            threadRead.setPriority(MAX_PRIORITY);
            threadRead.start();
        }catch(Exception e){
            Log.d("myApp", "Buffer thread not started");
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        Device.stopDiscovery();
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        Device.stopDiscovery();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    //TODO: Optical Camera

    /**
     * Sets the preview size to 1280x720 , picture size to 2048x1536 (for faster image processing)
     * and enables the autofocus and the flash mode.
     * @param camera: on which the parameters are set
     */
    private void setCameraParameters(Camera camera){
        final Camera.Parameters params = camera.getParameters();
        params.setPictureSize(2048,1536);
        params.setPreviewSize(1280,720);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        camera.setParameters(params);
    }

    /**
     * Allows save opening of the camera instance.
     * @return Camera -> the camera instance
     */
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // cannot get camera or does not exist
            Toast.makeText(getContext(), "ERROR: No Camera Found", Toast.LENGTH_SHORT).show();
        }
        return camera;
    }

    /**
     * Generates a picture callback instance that is saved under the given path
     * (see: getOutputMediaFile()) with the FOV and the Distance in the user comment tag
     * of the exif interface of the picture.
     */
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
                getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(pictureFile)));
                exifOptic = new ExifInterface(pictureFile.getAbsolutePath());
                exifOptic.setAttribute(ExifInterface.TAG_USER_COMMENT,"FOV = "+ FOV_OPTICAL
                            +"\n"+"Distance = "+ Distance_Optic);
                exifOptic.saveAttributes();
                camera.startPreview();
                BildInBild.getMergedImage(opticalPath,thermalPath,getContext(),
                                            prefs,mActivity.gamma,mActivity.threshold);
            } catch (IOException e) {
                Toast.makeText(getContext(), "ERROR: Optical image not saved",
                            Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * creates if necessary a directory in which it generates a file that can be used to save
     * the optical picture.
     * @return File -> created file ready to be overwritten
     */
    @Nullable
    private File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                mActivity.folderName);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("IR-Imaging", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        opticalPath = (mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        mediaFile = new File(opticalPath);
        return mediaFile;
    }

    //TODO: Thermal Camera

    /**
     * If a FlirOne device is discovered by the discovery onDeviceConnected creates a Stream.
     * @param device: The object representing the connected device. (FlirOne Device)
     */
    @Override
    public void onDeviceConnected(Device device) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mActivity.irCamOn) {
                    mActivity.irCamOn = true;
                    try {
                        Toast.makeText(getContext(), "Thermal camera connected", Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        Log.d("myApp", "Camera Connected");
                    }
                }
                ImageView statusView = (ImageView) mActivity.findViewById(R.id.statusView);
                if (mActivity.streamOn && mActivity.irCamOn) {
                    statusView.setBackgroundColor(Color.parseColor("#ff669900")); //green
                }else{
                    statusView.setBackgroundColor(Color.parseColor("#ffff4444")); //red
                }
            }
        });
        flirDevice = device;
        try{
            device.startFrameStream(new Device.StreamDelegate() {
                @Override
                public void onFrameReceived(Frame frame) {
                    frameProcessor.processFrame(frame);
                }
            });
        }catch(Exception e){
            flirDevice = null;
            Toast.makeText(getContext(), "ERROR: No IR-Camera attached", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Informs the user that the FlirOne device has been removed.
     * @param device: The object representing the connected device. (FlirOne Device)
     */
    @Override
    public void onDeviceDisconnected(Device device) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActivity.irCamOn) {
                    mActivity.irCamOn = false;
                    Toast.makeText(mActivity.getBaseContext(), "Thermal camera disconnected", Toast.LENGTH_SHORT).show();
                }
                ImageView statusView = (ImageView) mActivity.findViewById(R.id.statusView);
                if (mActivity.streamOn && mActivity.irCamOn) {
                    try {
                        statusView.setBackgroundColor(Color.parseColor("#ff669900")); //green
                    }catch(Exception e){
                        Log.d("MyApp","not on CameraFragment");
                    }

                }else{
                    try {
                        statusView.setBackgroundColor(Color.parseColor("#ffff4444")); //red
                    }catch(Exception e){
                        Log.d("MyApp","not on CameraFragment");
                    }
                }
            }
        });

    }

    /**
     * Called when the device confirms enabling or disabling the automatic tuning function.
     * @param b: The newly applied setting for automatic tuning.
     *           True if device will automatically tune.
     */
    @Override
    public void onAutomaticTuningChanged(boolean b) {

    }

    /**
     * Called when the tuning state of the device changes,
     * which will indicate the device is in the process of being tuned, has completed tuning,
     * or may require tuning for high thermal accuracy.
     * @param tuningState: The new tuning state of the device
     */
    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {

    }

    /**
     * Called when a frame has been received and processed.
     * @param renderedImage: The image after processing has been applied.
     */
    @Override
    public void onFrameProcessed(RenderedImage renderedImage) {
        picture = renderedImage.getBitmap();
    }

    /**
     * Creates if necessary a directory in which it generates a file that can be used to save
     * the thermal picture.
     * @return File -> created file ready to be overwritten
     */
    private File getOutputThermalFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                mActivity.folderName);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("IR-Imaging", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        thermalPath = (mediaStorageDir.getPath() + File.separator
                + "THR_" + timeStamp + ".jpg");
        mediaFile = new File(thermalPath);
        return mediaFile;
    }

    /**
     * Saves the thermal image bitmap under the given path (see: getOutputThermalFile())
     * with the FOV and the distance in the user comment tag of
     * the exif interface of the picture.
     */
    public void take_thermal_picture(){
        File thermalFile = getOutputThermalFile();
        try {
            FileOutputStream fOut = new FileOutputStream(thermalFile);
            picture.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            // refresh internal storage
            getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(thermalFile)));

            exifTherm = new ExifInterface(thermalFile.getAbsolutePath());
            exifTherm.setAttribute(ExifInterface.TAG_USER_COMMENT, "FOV = " + FOV_THERMAL
                        + "\n" + "Distance = " + Distance_Therm);
            exifTherm.saveAttributes();
        }
        catch (Exception e) {
            Toast.makeText(getContext(), "ERROR: Thermal image not saved",
                        Toast.LENGTH_SHORT).show();
        }
    }


    //TODO: Bluetooth
    /**
     * Allows the connection to an already known bluetooth device when the Fragment is opened.
     */
    public void fixConnect(){
        int position;
        mActivity.searchPairedDevice();
        position= mActivity.list.indexOf("HC_05_LASER");
        try{
            mActivity.BDevice = mActivity.deviceList.get(position);
            mActivity.connectDevice(mActivity.BDevice);
            }catch(Exception e){
            Toast.makeText(getContext(), "ERROR: Device HC_05_LASER not found",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads out the input stream from the Bluetooth stream, has to be called in a thread.
     */
    public void readBuffer(){
        try {
            String line;
            int i = 0;
            while((line = mActivity.mbufferedReader.readLine()) != null) {
                final String text = String.valueOf(line);
                arrayBuffer.add(i,line);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView FocusDistance = (TextView) mActivity.findViewById(R.id.FocusDistance);
                        try {
                            Log.d("Distance = ",text);
                            FocusDistance.setText(text);
                            DistanceSave = text;
                        }catch(Exception e){
                            Log.d("myApp", "showing other fragment");
                        }
                    }
                });
                i += 1;
            }
        }catch(Exception e){
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView FocusDistance = (TextView) mActivity.findViewById(R.id.FocusDistance);
                    try {
                        FocusDistance.setText(R.string.Error_noDistance);
                    }catch(Exception e){
                        Log.d("myApp", "showing other fragment");
                    }
                }
            });
        }
    }

    /**
     * Writes the distance into the variables and on the screen.
     */
    public void getDistance() {
        TextView FocusDistance = (TextView) mActivity.findViewById(R.id.FocusDistance);
        try {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Distance_Optic = String.valueOf(Integer.parseInt(
                    DistanceSave)+DistanceOffset);
            Distance_Therm = String.valueOf(Integer.parseInt(Distance_Optic) + cameraXOffset);
            Log.d("myApp","opticalDistance written = "+Distance_Optic);
            Log.d("myApp","thermalDistance written = "+Distance_Therm);
        } catch (Exception e) {
            Distance_Optic = "empty";
            Distance_Therm = "empty";
        }
    }
}
