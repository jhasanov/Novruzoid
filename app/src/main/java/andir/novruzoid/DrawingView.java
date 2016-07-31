/*
Sample taken from:
http://stackoverflow.com/questions/15704205/how-to-draw-line-on-imageview-along-with-finger-in-android

 */
package andir.novruzoid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import image.segment.elements.Column;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import ocr.LabelManager;
import utils.Rectangle;

public class DrawingView extends View {//} implements View.OnTouchListener {
    private static final String APP_NAME = "Novruzoid";
    Bitmap mBitmap;
    Canvas mCanvas;
    Paint mPaint,mSegment;
    Paint mBitmapPaint;
    float[] points;

    TreeMap<Integer,Column> columnsMap;
    //ArrayList<Symbol> symbolList = new ArrayList<Symbol>();

    ImageProc imgProc = new ImageProc();
    float downX, downY;
    int pointIdx = -1;
    boolean bDrawLines = true, bDrawSegments = false;


    public DrawingView(Context context) {
        super(context);

        points = imgProc.getEdges();

        mPaint = new Paint();
        mSegment = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mBitmapPaint = new Paint();

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        pointIdx = findIndex(downX,downY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        points[pointIdx*2] = event.getX();
                        points[pointIdx*2+1] = event.getY();
                        invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        //upx = event.getX();
                        //upy = event.getY();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                    default:
                        break;
                }
                return true;
            }

            public int findIndex(float x,float y) {
                int pIdx = -1;
                float sum = Float.MAX_VALUE;

                for (int i=0; i<(points.length/2); i++) {
                    float diff = Math.abs(points[i*2] - x) + Math.abs(points[i*2+1]-y);
                    if ( diff < sum ) {
                        pIdx = i;
                        sum = diff;
                    }
                }

                return pIdx;
            }
        });
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        LabelManager.symbolList = new ArrayList<Symbol>();
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

            if (bDrawLines) {
                Path path = new Path();

                path.moveTo(points[0], points[1]);
                canvas.drawCircle(points[0], points[1], 3, mPaint);
                for (int i = 1; i < points.length / 2; i++) {
                    canvas.drawCircle(points[i * 2], points[i * 2 + 1], 3, mPaint);
                    path.lineTo(points[i * 2], points[i * 2 + 1]);
                }
                path.close();

                canvas.drawPath(path, mPaint);
            }

            if (bDrawSegments) {
                if ((columnsMap != null) || (columnsMap.size() > 0 )) {
                    // Columns will be drawn with red
                    mSegment.setColor(Color.RED);
                    mSegment.setStyle(Paint.Style.STROKE);
                    mSegment.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

                    Iterator<Integer> colIt = columnsMap.keySet().iterator();
                    //iterating through the columns
                    while (colIt.hasNext()) {
                        // get key and value of the next column
                        Integer colKey = colIt.next();// ID of the columns
                        Column column = columnsMap.get(colKey); // Column object

                        Rectangle rect = column.getBorders();
                        int x = rect.getX(); int y = rect.getY(); int w = rect.getWidth(); int h = rect.getHeight();
                        canvas.drawRect(new Rect(x,y,x+w,y+h),mSegment);

                        // Segments will be drawn with green
                        mSegment.setColor(Color.GREEN);
                        mSegment.setStyle(Paint.Style.STROKE);

                        // Get text lines in this column
                        TreeMap<Integer, TextLine> textLinesMap = column.getLines();
                        //Iterate through the text lines
                        Iterator<Integer> tlIt = textLinesMap.keySet().iterator();
                        int ii = 0;
                        while (tlIt.hasNext()) {
                            // get key and value of the next text line
                            Integer tlKey = tlIt.next(); // ID of the text line
                            TextLine textLine = textLinesMap.get(tlKey); // Text line itself

                            // Get words in current textline
                            TreeMap<Integer, Word> wordsMap = textLine.getWords();
                            //Iterate through the words
                            Iterator<Integer> wordsIt = wordsMap.keySet().iterator();

                            while (wordsIt.hasNext()) {
                                Integer wordKey = wordsIt.next();
                                Word word = wordsMap.get(wordKey);

                                TreeMap<Integer, Symbol> symbolsMap = word.getSymbols();
                                //Iterate through the symbols
                                Iterator<Integer> smbIt = symbolsMap.keySet().iterator();
                                String reWord = "";
                                String mask = "";
                                while (smbIt.hasNext()) {
                                    Integer smbKey = smbIt.next();
                                    Symbol symbol = symbolsMap.get(smbKey);
                                    LabelManager.symbolList.add(symbol);
                                    rect = symbol.getBorders();
                                    x = rect.getX(); y = rect.getY(); w = rect.getWidth(); h = rect.getHeight();
                                    canvas.drawRect(new Rect(x,y,x+w,y+h),mSegment);
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    public float[] getPoints() {
        return points;
    }

    public void setDrawLines(boolean bDrawLines) {
        this.bDrawLines = bDrawLines;
    }

    public void setSegments(boolean bDrawSegments) {
        this.bDrawSegments = bDrawSegments;
    }

    public void setSegmentElements(TreeMap<Integer,Column> columnsMap) {
        this.columnsMap = columnsMap;
    }

    public void setBitmap(Bitmap bmp) {
        mBitmap = bmp;
    }

    public Bitmap getBitmap() { return mBitmap; }

    /*public ArrayList<Symbol> getSymbols() {
        return symbolList;
    }*/
}