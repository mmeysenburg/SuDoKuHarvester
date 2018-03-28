package edu.doane.ist.sudokuharvester;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to use OpenCV to detect numbers in a SuDoKu grid image.
 *
 * @author Mark M. Meysenburg
 * @version 03/26/2018
 */

public class NumberFinder {

    /** Tag used in Logcat log for the activity. */
    private static final String TAG = "SDKH_NF";

    /** Activity holding the grid, difficulty spinner, and upload button. */
    private DecodeActivity parent;

    /**
     * Neural network used to decoding digit images.
     */
    private Net net;

    /**
     * Method to get the path for the NN Caffe model files.
     *
     * @param file Name of file to find.
     * @param context Activity requesting the file.
     * @return Fully qualified path for loading the NN file.
     */
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();

        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.e(TAG, "Failed to access NN file: " + file);
        }
        return "";
    }

    /**
     * Constructor, which does all of the work.
     * @param fileName
     * @param parent
     */
    public NumberFinder(String fileName, DecodeActivity parent) {
        this.parent = parent;

        // load neural net from Caffe model files
        String proto = getPath("deploy.prototxt", parent);
        String weights = getPath("deploy.caffemodel", parent);
        net = Dnn.readNetFromCaffe(proto, weights);

        // load original image
        Mat originalImage =  Imgcodecs.imread(fileName.toString(), Imgcodecs.IMREAD_GRAYSCALE);

        // blur and threshold image
        Mat blurredImage = new Mat();
        Imgproc.GaussianBlur(originalImage, blurredImage, new Size(7, 7), 0);
        Mat binaryImage = new Mat();
        Imgproc.threshold(blurredImage, binaryImage, 120, 255, Imgproc.THRESH_BINARY_INV);

        // find contour with the largest area -- hopefully, that's the grid!
        List<MatOfPoint> contours = new LinkedList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint largestContour = null;
        double maxArea = Double.NEGATIVE_INFINITY;
        for(MatOfPoint p : contours) {
            double area = Imgproc.contourArea(p);
            if(area > maxArea) {
                maxArea = area;
                largestContour = p;
            }
        }

        // find best-fit rectangle of the largest contour
        MatOfPoint2f largestContour2f = new MatOfPoint2f();
        largestContour2f.fromList(largestContour.toList());
        double epsilon = 0.1 * Imgproc.arcLength(largestContour2f, true);
        MatOfPoint2f bestFit = new MatOfPoint2f();
        Imgproc.approxPolyDP(largestContour2f, bestFit, epsilon, true);

        // find center of largest contour, for finding corner points in order
        Moments moments = Imgproc.moments(largestContour);
        Point center = new Point(moments.m10 / moments.m00, moments.m01 / moments.m00);

        // four corner points of bounding rectangle, upper left to lower left, clockwise
        Point p1 = null, p2 = null;
        for(Point p : bestFit.toArray()) {
            if(p.x < center.x && p.y < center.y) {
                p1 = new Point(p.x, p.y); // upper left
            } else if(p.x > center.x && p.y < center.y) {
                p2 = new Point(p.x, p.y); // upper right
            }
        }

        // rotate image to make grid lines horizontal and vertical, using difference
        // between y coordinates in upper line of bounding box
        Mat rotatedImage = new Mat();
        if(p1.y != p2.y) {
            double opp = p1.y - p2.y;
            double adj = p2.x - p1.x;
            double hyp = Math.sqrt(opp * opp + adj * adj);
            double angle = -Math.asin(opp / hyp) * 180.0 / Math.PI;
            Mat rot = Imgproc.getRotationMatrix2D(new Point(binaryImage.width() / 2.0,
                    binaryImage.height() / 2.0), angle, 1.0);
            Imgproc.warpAffine(binaryImage, rotatedImage, rot, binaryImage.size());
        } else {
            rotatedImage = binaryImage.clone();
        }

        // find largest contour again, on the rotated image this time
        contours.clear();
        Imgproc.findContours(rotatedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        largestContour = null;
        maxArea = Double.NEGATIVE_INFINITY;
        for(MatOfPoint p : contours) {
            double area = Imgproc.contourArea(p);
            if(area > maxArea) {
                maxArea = area;
                largestContour = p;
            }
        }

        // get bounding rectangle of the largest contour
        Rect boundingBox = Imgproc.boundingRect(largestContour);

        // calculate step size and invert image for final processing
        int hIncr = boundingBox.width / 9;
        int hAdj = hIncr / 15;
        int vIncr = boundingBox.height / 9;
        int vAdj = vIncr / 15;
        Mat finalImage = new Mat();
        Core.bitwise_not(rotatedImage, finalImage);

        // now, finally, query NN about the numbers
        Size nnSize = new Size(28, 28);
        Scalar nnMean = new Scalar(0);
        int[][] grid = new int[9][9];
        int y = boundingBox.y - vIncr;
        // TODO: remove local file writing
        String fileRoot = new File(fileName).getParent();
        for(int row = 0; row < 9; row++) {
            int x = boundingBox.x - hIncr;
            y += vIncr;
            for(int col = 0; col < 9; col++) {
                x += hIncr;
                Mat sub = finalImage.submat(new Rect(x + hAdj, y + vAdj, hIncr - hAdj, vIncr - vAdj));

                Mat blob = Dnn.blobFromImage(sub, 1.0, nnSize, nnMean, false);

                // TODO: remove local file writing
                File f = new File(fileRoot + String.format("/%d_%d.png", row, col));
                Mat sub1 = new Mat();
                Imgproc.resize(sub, sub1, nnSize);
                Imgcodecs.imwrite(f.toString(), sub1);


                net.setInput(blob, "data");
                Mat predictions = net.forward();
                int guess = 0;
                double maxProb = Double.NEGATIVE_INFINITY;
                for(int i = 0; i < 10; i++) {
                    double pred = predictions.get(0, i)[0];
                    if(pred > maxProb) {
                        maxProb = pred;
                        guess = i;
                    }
                }
                grid[row][col] = maxProb == 1.0 ? 0 : guess;
            } // for col
        } // for row

        // report guesses back to the activity
        parent.setGuesses(grid);
    }


}
