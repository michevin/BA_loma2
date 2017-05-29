package com.example.simon.irimaging;

/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * The Class CameraPreview is used to show the images recorded by the smartphone camera on to
 * it's display.
 */


import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

@SuppressWarnings("deprecation")

public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    /**
     * Constructor that obtains context and camera
     * Creates the surfaceHolder needed to show the images on screen.
     * @param context: context on which the holder should be set.
     * @param camera: camera that should be shown on screen.
     */
    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Starts the frame streaming from the camera as soon as the surfaceHolder is created.
     * @param surfaceHolder: selected surfaceHolder.
     */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            // suppress warning
        }
    }

    /**
     * Needed by the SurfaceView implement, but leaven empty in this case.
     * @param surfaceHolder: selected surfaceHolder.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    /**
     * Restarts the preview once the surfaceHolder was modified.
     * @param surfaceHolder: selected surfaceHolder.
     * @param format: format of the surfaceHolder.
     * @param width: width of the surfaceHolder.
     * @param height:height of the surfaceHolder.
     */
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
                               int width, int height) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            // suppress warning
        }
    }
}
