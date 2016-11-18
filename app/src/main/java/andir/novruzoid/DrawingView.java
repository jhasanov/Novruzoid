package andir.novruzoid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.OverScroller;

import java.util.Iterator;
import java.util.TreeMap;

import image.segment.elements.Column;
import image.segment.elements.Symbol;
import image.segment.elements.TextLine;
import image.segment.elements.Word;
import ocr.LabelManager;
import utils.Rectangle;

public class DrawingView extends ImageView {
    Paint mPaint, mSegment, gridPaint;
    float[] points;

    enum OperationType {CAMERA, SEGMENT}

    private OperationType operationType = OperationType.CAMERA;

    TreeMap<Integer, Column> columnsMap;

    boolean bScroll = false;
    ImageProc imgProc = new ImageProc();
    float downX, downY;
    int pointIdx = -1;
    boolean bDrawLines = true, bDrawSegments = false, bDrawGrids = false;

    private GestureDetectorCompat gestureDetector;
    private OverScroller overScroller;

    private int screenW;
    private int screenH;

    private int positionX = 0;
    private int positionY = 0;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        points = imgProc.getEdges();

        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);

        gridPaint = new Paint();
        gridPaint.setColor(Color.RED);
        gridPaint.setStyle(Paint.Style.STROKE);

        mSegment = new Paint();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;

        gestureDetector = new GestureDetectorCompat(context, gestureListener);
        overScroller = new OverScroller(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (operationType == OperationType.CAMERA) {
            int action = event.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    pointIdx = findIndex(downX, downY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    points[pointIdx * 2] = event.getX();
                    points[pointIdx * 2 + 1] = event.getY();
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
        } else {
            gestureDetector.onTouchEvent(event);
        }
        return true;
    }

    public int findIndex(float x, float y) {
        int pIdx = -1;
        float sum = Float.MAX_VALUE;

        for (int i = 0; i < (points.length / 2); i++) {
            float diff = Math.abs(points[i * 2] - x) + Math.abs(points[i * 2 + 1] - y);
            if (diff < sum) {
                pIdx = i;
                sum = diff;
            }
        }

        return pIdx;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bDrawGrids) {
            //draw 31x31 grids
            for (int i = 1; i < getWidth() / 31; i++) {
                canvas.drawLine(i * 31, 0, i * 31, getHeight(), gridPaint);
            }
            for (int i = 1; i < getHeight() / 31; i++) {
                canvas.drawLine(0, i * 31, getWidth(), i * 31, gridPaint);
            }
        }

        if (bDrawLines) {
            //LabelManager.symbolList = new ArrayList<Symbol>();
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
            if ((columnsMap != null) || (columnsMap.size() > 0)) {
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
                    int x = rect.getX();
                    int y = rect.getY();
                    int w = rect.getWidth();
                    int h = rect.getHeight();
                    canvas.drawRect(new Rect(x, y, x + w, y + h), mSegment);

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
                                x = rect.getX();
                                y = rect.getY();
                                w = rect.getWidth();
                                h = rect.getHeight();
                                canvas.drawRect(new Rect(x, y, x + w, y + h), mSegment);
                            }
                        }
                    }
                }

            }
        }
    }

    public void setScroll(boolean bScroll) {
        this.bScroll = bScroll;
    }

    public float[] getPoints() {
        return points;
    }

    public void setPoints(float[] p) {
        points = p;
    }

    public void setDrawLines(boolean bDrawLines) {
        this.bDrawLines = bDrawLines;
    }

    public void setDrawGrids(boolean bDrawGrids) {
        this.bDrawGrids = bDrawGrids;
    }

    public void setSegments(boolean bDrawSegments) {
        this.bDrawSegments = bDrawSegments;
    }

    public void setSegmentElements(TreeMap<Integer, Column> columnsMap) {
        this.columnsMap = columnsMap;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (!bScroll)
            return;

        // computeScrollOffset() returns true only when the scrolling isn't
        // already finished
        if (overScroller.computeScrollOffset()) {
            positionX = overScroller.getCurrX();
            positionY = overScroller.getCurrY();
            scrollTo(positionX, positionY);
        } else {
            // when scrolling is over, we will want to "spring back" if the
            // image is overscrolled
            overScroller.springBack(positionX, positionY, 0, getMaxHorizontal(), 0, getMaxVertical());
        }
    }

    private int getMaxHorizontal() {
        return (Math.abs(getDrawable().getBounds().width() - screenW));
    }

    private int getMaxVertical() {
        return (Math.abs(getDrawable().getBounds().height() - screenH));
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            overScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(DrawingView.this);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            overScroller.forceFinished(true);
            overScroller.fling(positionX, positionY, (int) -velocityX, (int) -velocityY, 0, getMaxHorizontal(), 0,
                    getMaxVertical());
            ViewCompat.postInvalidateOnAnimation(DrawingView.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            overScroller.forceFinished(true);
            // normalize scrolling distances to not overscroll the image
            int dx = (int) distanceX;
            int dy = (int) distanceY;
            int newPositionX = positionX + dx;
            int newPositionY = positionY + dy;
            if (newPositionX < 0) {
                dx -= newPositionX;
            } else if (newPositionX > getMaxHorizontal()) {
                dx -= (newPositionX - getMaxHorizontal());
            }
            if (newPositionY < 0) {
                dy -= newPositionY;
            } else if (newPositionY > getMaxVertical()) {
                dy -= (newPositionY - getMaxVertical());
            }
            overScroller.startScroll(positionX, positionY, dx, dy, 0);
            ViewCompat.postInvalidateOnAnimation(DrawingView.this);
            return true;
        }
    };

    public void resetScrollPos() {
        positionX = 0;
        positionY = 0;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    /*public ArrayList<Symbol> getSymbols() {
        return symbolList;
    }*/
}