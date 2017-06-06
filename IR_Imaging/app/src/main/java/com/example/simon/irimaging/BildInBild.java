package com.example.simon.irimaging;
/**
 * Created by Vincent Michel & Simon Schweizer on 04.05.2017.
 * This Class contains one static function getMergedImage, that was implement along the
 * bachelor thesis.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static java.lang.Math.PI;
import static java.lang.Math.round;
import static java.lang.Math.tan;
import static org.opencv.core.Core.BORDER_CONSTANT;
import static org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE;
import static org.opencv.core.Core.mean;
import static org.opencv.core.Core.meanStdDev;
import static org.opencv.core.Core.multiply;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.COLORMAP_JET;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.applyColorMap;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.floodFill;
import static org.opencv.imgproc.Imgproc.matchTemplate;
import static org.opencv.imgproc.Imgproc.medianBlur;
import static org.opencv.imgproc.Imgproc.threshold;


 class BildInBild {

    /**
     * Saves the result of the merging of one thermal and one optical image taken with the setup
     * developed along the bachelor thesis.
     * @param opticalPath:  Path of the optical Image
     * @param thermalPath:  Path of the thermal Image
     * @param context:      Context on which the toast should appear
     * @param prefs:        SharedPreferences of the application
     * @param gamma:        Weighting factor for the optical image in the merged image
     * @param threshold:    Threshold selection
     */
     static void getMergedImage(String opticalPath, String thermalPath,
                                      Context context, SharedPreferences prefs, double gamma,
                                      int threshold) {
        // Declaration of the variables needed to merge the images.
        int BORDER = 200;
        float MAGNIFICATION = 1.15f;
        float opticalDistance;
        float thermalDistance;
        float opticalFov;
        float thermalFov;
        float opticalMinDistance;
        float thermalMinDistance;
        float opticalVerMinDistance;
        float thermalVerMinDistance;
        float[] cameraOffset = {138.5f, 2.7f, 5.4f}; // measured offset of the thermal camera(x,y,z)
        double beta = 1 - gamma;
        Scalar black = new Scalar(0, 0, 0);
        Scalar white = new Scalar(255,255,255);
        Mat opticalImage = imread(opticalPath);
        Mat thermalImage = imread(thermalPath);
        Mat mergedImage;
        SharedPreferences.Editor prefsEditor = prefs.edit();
        ExifInterface thermalExif = null;
        ExifInterface opticalExif = null;

        // Handling the exception that no optical or thermal image has been saved under the given
        // path.
        if (thermalImage.size().height == 0) {
            Toast.makeText(context, "ERROR: No thermal picture found ", Toast.LENGTH_LONG).show();
        } else if (opticalImage.size().height == 0) {
            Toast.makeText(context, "ERROR: No optical picture found ", Toast.LENGTH_LONG).show();
        } else {
            // Saving the sizes of both images and rotating the thermal image by 90 degrees.
            Size opticalSize = opticalImage.size();
            Size thermalSize;//= thermalImage.size();
            Core.rotate(thermalImage,thermalImage,ROTATE_90_COUNTERCLOCKWISE);
            thermalSize = thermalImage.size();

            // Creating the ExifInterface to read out the Exif-file of both images.
            try {
                thermalExif = new ExifInterface(thermalPath);
                opticalExif = new ExifInterface(opticalPath);
            } catch (Exception e) {
                Toast.makeText(context, "ERROR: No EXIF found ", Toast.LENGTH_SHORT).show();
            }

            // Reading out the Exif-file and writing the values into the variables.
            try {
                char[] opticalFovString = new char[4];
                opticalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).getChars(6, 10,
                        opticalFovString, 0);
                opticalFov = Float.parseFloat(String.valueOf(opticalFovString));
                opticalFov = (float) (opticalFov * PI / 180.0f);

                char[] thermalFovString = new char[4];
                thermalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).getChars(6, 10,
                        thermalFovString, 0);
                thermalFov = Float.parseFloat(String.copyValueOf(thermalFovString));
                thermalFov = (float) (thermalFov * PI / 180.0f);

                char[] opticalDistanceString = new char[4];
                opticalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).getChars(22,
                        opticalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).length(),
                        opticalDistanceString, 0);
                opticalDistance = Float.parseFloat(String.valueOf(opticalDistanceString));
                char[] thermalDistanceString = new char[5];
                thermalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).getChars(22,
                        thermalExif.getAttribute(ExifInterface.TAG_USER_COMMENT).length(),
                        thermalDistanceString, 0);
                thermalDistance = Float.parseFloat(String.copyValueOf(thermalDistanceString));
            } catch (Exception e) {
                Toast.makeText(context, "ERROR: could not read EXIF", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculating the vertical field of view using the horizontal field of view.
            float opticalVerFov = ((float) (opticalSize.height / opticalSize.width)) * opticalFov;
            float thermalVerFov = ((float) (thermalSize.height / thermalSize.width)) * thermalFov;

            // Reading out the SharedPreferences, if none is found the value 0.0f is set in tester
            float tester = prefs.getFloat("opticalMinDistance", 0.0f);

            // If no SharedPreferences is found, calculating the minimal distances, else reading
            // the minimal distance out of prefs.
            if (tester == 0.0f) {
                thermalMinDistance = (float)((cameraOffset[2] * tan(thermalFov / 2) +
                        cameraOffset[0]) / (tan(opticalFov / 2) - tan(thermalFov / 2)));
                prefsEditor.putFloat("thermalMinDistance", thermalMinDistance);
                prefsEditor.commit();
                opticalMinDistance = thermalMinDistance - cameraOffset[2];
                prefsEditor.putFloat("opticalMinDistance", opticalMinDistance);
                prefsEditor.commit();

                thermalVerMinDistance = (float)((cameraOffset[2] * tan(thermalVerFov / 2) +
                        cameraOffset[1]) / (tan(opticalVerFov / 2) - tan(thermalVerFov / 2)));
                prefsEditor.putFloat("thermalVerMinDistance", thermalVerMinDistance);
                prefsEditor.commit();
                opticalVerMinDistance = thermalVerMinDistance - cameraOffset[2];
                prefsEditor.putFloat("opticalVerMinDistance", opticalVerMinDistance);
                prefsEditor.commit();

            } else {
                opticalMinDistance = prefs.getFloat("opticalMinDistance", 0.0f);
                thermalMinDistance = prefs.getFloat("thermalMinDistance", 0.0f);
                opticalVerMinDistance = prefs.getFloat("opticalVerMinDistance", 0.0f);
                thermalVerMinDistance = prefs.getFloat("thermalVerMinDistance", 0.0f);
            }

            // Calculating the physical size and offsets of the picture out of the field of view
            // and the distance to the object.
            // NOTE: the MAGNIFICATION factor is needed to scale the thermal image as it is
            // too small.
            float opticalWidth = 2 * ((float) (tan(opticalFov / 2) * opticalDistance));
            float thermalWidth = 2 * ((float) (tan(thermalFov / 2) * thermalDistance))
                    * MAGNIFICATION;
            float opticalHeight = 2 * ((float) (tan(opticalVerFov / 2) * opticalDistance));
            float thermalHeight = 2 * ((float) (tan(thermalVerFov / 2) * thermalDistance))
                    * MAGNIFICATION;

            float rightEdge = (float) (tan(opticalFov / 2) * (opticalDistance - opticalMinDistance)
                    - tan(thermalFov / 2) * (thermalDistance - thermalMinDistance));
            float leftEdge = (opticalWidth - rightEdge - thermalWidth);
            float topEdge = (float) (tan(opticalVerFov / 2) *
                    (opticalDistance - opticalVerMinDistance)
                    - tan(thermalVerFov / 2) * (thermalDistance - thermalVerMinDistance));

            // Calculating the pixel density of the optical image.
            float opticalWidthPmm = (float) (opticalSize.width / opticalWidth);
            float opticalHeightPmm = (float) (opticalSize.height / opticalHeight);

            // Convert the physical sizes into pixel sizes
            int thermalWidthP = round(thermalWidth * opticalWidthPmm);
            int leftEdgeP = round(leftEdge * opticalWidthPmm);
            int topEdgeP = round(topEdge * opticalHeightPmm);
            int thermalHeightP = round(thermalHeight * opticalHeightPmm);

            // Resizing the thermal image
            Size thermalSizeP = new Size();
            thermalSizeP.height = thermalHeightP;
            thermalSizeP.width = thermalWidthP;
            mergedImage = Mat.zeros(thermalSizeP, 3);
            Mat thermalImageResize = new Mat();
            Imgproc.resize(thermalImage, thermalImageResize, thermalSizeP);

            // Adding a white Border to the optical image to insure that the thermal image is in the
            // optical image in marginal cases.
            Mat opticalImageBorder = new Mat();
            Core.copyMakeBorder(opticalImage, opticalImageBorder, BORDER, BORDER, BORDER, BORDER,
                    BORDER_CONSTANT, white);
            Mat opticalResize;
            try {
                opticalResize = opticalImageBorder.submat(topEdgeP,
                        (topEdgeP + thermalHeightP + 2 * BORDER), leftEdgeP,
                        (leftEdgeP + thermalWidthP + 2 * BORDER));
            } catch (Exception e) {
                Toast.makeText(context, "ERROR: Distance too small", Toast.LENGTH_LONG).show();
                return;
            }

            Mat opticalBW = new Mat();
            Scalar opticalMean;
            switch (threshold) {

                case 0:
                    cvtColor(opticalResize, opticalBW, Imgproc.COLOR_BGR2GRAY);
                    opticalMean = mean(opticalBW);
                    threshold(opticalBW, opticalBW, opticalMean.val[0], 255, 0);
                    break;

                case 1:
                    cvtColor(opticalResize, opticalBW, Imgproc.COLOR_BGR2GRAY);
                    opticalMean = mean(opticalBW);
                    threshold(opticalBW, opticalBW, opticalMean.val[0], 255, 1);
                    break;

                case 2:
                    // Difference of Gaussian of the optical image.
                    cvtColor(opticalResize, opticalBW, Imgproc.COLOR_BGR2GRAY);
                    medianBlur(opticalBW, opticalBW, 15);
                    Mat smallMat = new Mat();
                    Mat bigMat = new Mat();
                    Size blurSize = new Size(35, 35);
                    GaussianBlur(opticalBW, smallMat, blurSize, 0);
                    blurSize = new Size(81, 81);
                    GaussianBlur(opticalBW, bigMat, blurSize, 0);
                    try {
                        Core.subtract(bigMat, smallMat, opticalBW);
                    } catch (Exception e) {
                        Toast.makeText(context, "ERROR: OpenCV needs to be reinstalled",
                                Toast.LENGTH_LONG).show();
                    }
                    medianBlur(opticalBW, opticalBW, 15);

                    // Closing edges, filling in background.
                    Mat kernel = Mat.ones(3, 3, CV_8UC1);
                    kernel.put(0, 0, 0);
                    kernel.put(2, 2, 0);
                    kernel.put(0, 2, 0);
                    kernel.put(2, 0, 0);
                    Point anchor = new Point(-1, -1);
                    Point p1 = new Point(202, 202);
                    Point p2 = new Point(thermalSizeP.width - 2, 2);
                    Mat opticalBW2 = new Mat();
                    Core.copyMakeBorder(opticalBW, opticalBW2, 1, 1, 1, 1, BORDER_CONSTANT, white);
                    dilate(opticalBW2, opticalBW2, kernel, anchor, 25);
                    erode(opticalBW2, opticalBW2, kernel, anchor, 25);
                    floodFill(opticalBW, opticalBW2, p1, white);
                    floodFill(opticalBW, opticalBW2, p2, white);
                    break;

                default:
                    cvtColor(opticalResize, opticalBW, Imgproc.COLOR_BGR2GRAY);
                    threshold(opticalBW, opticalBW, 128, 255, 0);
                    break;
            }

            // Threshold thermal image with mean value of image.
            Mat thermalBW = new Mat();
            cvtColor(thermalImageResize, thermalBW, Imgproc.COLOR_BGR2GRAY);
            Scalar thermalMean = mean(thermalBW);
            threshold(thermalBW, thermalBW, thermalMean.val[0], 255, 1);

            // Correlating optical and thermal image. Calculating adjustment needed to offsets for
            // a better matching of both images.
            Mat corrMat = new Mat();
            matchTemplate(opticalBW, thermalBW, corrMat, 3);
            Core.MinMaxLocResult location = Core.minMaxLoc(corrMat);

            // Preparing thermal image for merging, setting values under threshold to zero
            // keep the values over the threshold.
            cvtColor(thermalImageResize, thermalImageResize, Imgproc.COLOR_BGR2GRAY);
            threshold(thermalImageResize, thermalImageResize, thermalMean.val[0], 255, 3);

            // Coloring of the thermal image, does not color Pixel that deviate more than 2 sigma
            // from the mean of the masked image.
            thermalImageResize.convertTo(thermalImageResize, CV_32FC1);
            MatOfDouble mean = new MatOfDouble();
            MatOfDouble std = new MatOfDouble();
            Mat mask = new Mat();
            threshold(thermalImageResize,mask,1,255,0);
            mask.convertTo(mask, CV_8UC1);
            meanStdDev(thermalImageResize, mean, std,mask);
            double mean0 = mean.toArray()[0];
            double std0 = std.toArray()[0];
            threshold(thermalImageResize, thermalImageResize, mean0 + 2 * std0, 255, 4);
            threshold(thermalImageResize, thermalImageResize, mean0 - 2 * std0, 255, 3);
            thermalImageResize.convertTo(thermalImageResize, CV_8UC3);
            Imgproc.equalizeHist(thermalImageResize, thermalImageResize);
            applyColorMap(thermalImageResize, thermalImageResize, COLORMAP_JET);
            thermalImageResize.setTo(black, thermalBW);

            // Creating black border around thermal image, then overlaying both images.
            Mat thermalImageResizeBorder = new Mat();
            try {
                if (location.maxVal > 0.5) {
                    Core.copyMakeBorder(thermalImageResize, thermalImageResizeBorder,
                            (int) (location.maxLoc.y),
                            (int) (2 * BORDER - location.maxLoc.y),
                            (int) (location.maxLoc.x),
                            (int) (2 * BORDER - location.maxLoc.x), BORDER_CONSTANT, black);
                    Core.addWeighted(opticalResize, gamma, thermalImageResizeBorder,
                            beta, 0.0, mergedImage);
                } else {
                    Core.copyMakeBorder(thermalImageResize, thermalImageResizeBorder,
                            BORDER, BORDER, BORDER, BORDER, BORDER_CONSTANT, black);
                    Core.addWeighted(opticalResize, gamma, thermalImageResizeBorder,
                            beta, 0.0, mergedImage);
                    Toast.makeText(context, "ATTENTION: No Correlation", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                return;
            }

            // Saving merged image in the same path as the optical image.
            String mergedPath = opticalPath;
            mergedPath = mergedPath.replace("IMG", "MRG");
            File mergedFile = new File(mergedPath);
            Imgcodecs.imwrite(mergedPath, mergedImage);

            // refresh internal storage
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(mergedFile)));
            /*
            * Commented out as it is not further needed to know how the binary images look like.
            String thermalBWPath = opticalPath.replace("IMG", "TBW");
            Imgcodecs.imwrite(thermalBWPath, thermalBW);
            String opticalBWPath = opticalPath.replace("IMG", "OBW");
            Imgcodecs.imwrite(opticalBWPath, opticalBW);*/
        }
    }

}
