package edu.doane.ist.sudokuharvester;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StartActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;

    private static final String TAG = "SDKH Start Activity";

    private String mCurrentPhotoPath;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // set Doane logo image on start application
        ImageView imageView = findViewById(R.id.logoView);
        imageView.setImageResource(R.drawable.logo);

        // configure click handler for the take photo button
        final Button btnTakePhoto = findViewById(R.id.start_button);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    /**
     * Method called when user taps the take photo button. Invokes an intent to take a photo
     * using the Android device's existing camera application.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // make sure there is a camera to handle the intent
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // create file where the photo will be saved
            File photoFile = null;
            try{
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Cannot create image file!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Cannot create image file!");
            }

            // if the file was successfully created,...
            if(photoFile != null) {
                // get a uniform resource identifier for the file
                Uri photoURI = FileProvider.getUriForFile(this,
                        "edu.doane.ist.sudokuharvester.fileprovider",
                        photoFile);
                // start the intent to take the picture
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * Method called when the take picture intent is complete.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // when the pic is saved, start the decoding activity
        Intent decodeIntent = new Intent(this, DecodeActivity.class);
        // pass fully qualified path of the image to the new activity
        decodeIntent.putExtra("EXTRA_PATH", mCurrentPhotoPath);
        // start the activity
        startActivity(decodeIntent);

    }

    /**
     * Create an image file object for a photo, based on current timestamp. This app stores
     * its images in the app's private data area.
     *
     * @return File object for the saved photo.
     *
     * @throws IOException If the file cannot be created.
     */
    private File createImageFile() throws IOException {
        // create image file name using timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "SDKH_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }
}
