package edu.doane.ist.sudokuharvester;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Custom View for displaying a SuDoKu grid.
 *
 * Created by mark.meysenburg on 3/26/2018.
 */

public class SuDoKuGridView extends View {

    private final Paint THICK_BLACK_LINE;
    private final Paint THIN_BLACK_LINE;
    private final Paint TEXT_PAINT;

    private float width;
    private float height;

    private int[][] grid;

    private DecodeActivity parent;

    private boolean isUnlocked = true;

    public SuDoKuGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // configure lines used for drawing the grid
        THICK_BLACK_LINE = new Paint();
        THICK_BLACK_LINE.setARGB(255, 0, 0, 0);
        THICK_BLACK_LINE.setStrokeWidth(20.0f);
        THICK_BLACK_LINE.setStyle(Paint.Style.STROKE);

        THIN_BLACK_LINE = new Paint();
        THIN_BLACK_LINE.setARGB(255, 0, 0, 0);
        THIN_BLACK_LINE.setStrokeWidth(10.0f);
        THIN_BLACK_LINE.setStyle(Paint.Style.STROKE);

        // configure the Paint used to draw the numbers
        TEXT_PAINT = new Paint();
        TEXT_PAINT.setARGB(255, 0, 0, 0);
        TEXT_PAINT.setStrokeWidth(5.0f);
        TEXT_PAINT.setTextSize(80.0f);
        TEXT_PAINT.setTextAlign(Paint.Align.CENTER);

        // create initial grid
        grid = new int[9][9];
    }

    public void setParent(DecodeActivity da) {
        this.parent = da;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isUnlocked) {
            isUnlocked = false;

            this.playSoundEffect(SoundEffectConstants.CLICK);

            float x = event.getX();
            float y = event.getY();

            final int col = (int) (x / width * 9.0f);
            final int row = (int) (y / height * 9.0f);

            // configure and display a pop-up to get the new cell value
            final EditText txtValue = new EditText(parent);
            txtValue.setHint(Integer.toString(grid[row][col]));
            new AlertDialog.Builder(parent)
                    .setTitle("New value")
                    .setMessage("Enter new value for cell (" + row + ", " + col + ")")
                    .setView(txtValue)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String s = txtValue.getText().toString();
                            grid[row][col] = Integer.parseInt(s);
                            isUnlocked = true;
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isUnlocked = true;
                        }
                    })
                    .show();
        }
        return true;
    }

    /**
     * Set the grid that will be displayed in the control.
     *
     * @param newGrid 9x9 integer array containing new grid information.
     */
    public void setGridValues(int[][] newGrid) {
        for(int i = 0; i < 9; i++) {
            for(int j = 0; j < 9; j++) {
                grid[i][j] = newGrid[i][j];
            }
        }
    }

    /**
     * Get the grid values from the control.
     *
     * @return New 9x9 array containing the values being displayed in the grid.
     */
    public int[][] getGridValues() {
        int[][] newGrid = new int[9][9];

        for(int i = 0; i < 9; i++) {
            for(int j = 0; j < 9; j++) {
                newGrid[i][j] = grid[i][j];
            }
        }

        return newGrid;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // set height to be the same as the width
        setMeasuredDimension(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // save width and height, they're used multiple times
        width = canvas.getWidth();
        height = canvas.getHeight();

        // background color
        canvas.drawARGB(255, 200, 200, 200);

        // outer rectangle
        canvas.drawRect(0.0f, 0.0f, width, height, THICK_BLACK_LINE);

        // grid lines
        float incr = width / 9.0f;
        float x = incr;
        Paint p = null;
        for(int i = 0; i < 8; i++) {
            p = (i == 2 || i == 5) ? THICK_BLACK_LINE : THIN_BLACK_LINE;
            canvas.drawLine(x, 0.0f, x, height, p); // vertical line
            canvas.drawLine(0.0f, x, width, x, p); // horizontal line
            x += incr;
        }

        // numbers
        x = incr / 2;
        float y = incr / 2;
        for(int i = 0; i < 9; i++) {
            for(int j = 0; j < 9; j++) {
                String c = (grid[i][j] == 0) ? " " : Integer.toString(grid[i][j]);
                canvas.drawText(c, x - 10.0f, y + 25.0f, TEXT_PAINT);
                x += incr;
            }
            y += incr;
            x = incr / 2;
        }
    }


}
