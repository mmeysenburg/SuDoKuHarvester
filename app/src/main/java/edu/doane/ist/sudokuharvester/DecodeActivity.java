package edu.doane.ist.sudokuharvester;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;

public class DecodeActivity extends AppCompatActivity {

    private String mCurrentFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        Spinner spnDifficultieis = (Spinner)findViewById(R.id.difficulties_spinner);
        // configure spinner content; first, make an ArrayAdapter connecting the array of
        // difficulties to a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.difficulties_array,
                android.R.layout.simple_spinner_item);

        // specify the layout to use when the list of choices apapears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // apply adapter to the spinner
        spnDifficultieis.setAdapter(adapter);

        // configure handler for the upload button
        final Button btnUpload = findViewById(R.id.upload_button);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadData();
            }
        });

        // get the image path from the previous activity
        Intent startingIntent = getIntent();
        mCurrentFilePath = startingIntent.getStringExtra("EXTRA_PATH");

        // build the decoder
        NumberFinder nf = new NumberFinder(mCurrentFilePath, this);

        // connect this Activity to the grid view
        SuDoKuGridView gv = findViewById(R.id.sdk_grid);
        gv.setParent(this);
    }

    /**
     * Method called when the user taps on the upload button. Upload the grid and difficulty
     * to the cloud database, and then delete the local image file.
     */
    public void uploadData() {
        boolean wasDeleted;
        File imageFile = new File(mCurrentFilePath);
        wasDeleted = imageFile.delete();
        if(wasDeleted) {
            Toast.makeText(this, "Successfully uploaded", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Update the guesses to be displayed on the grid
     *
     * @param guesses
     */
    public void setGuesses(int[][] guesses) {
        SuDoKuGridView gv = findViewById(R.id.sdk_grid);
        gv.setGridValues(guesses);
    }

}
