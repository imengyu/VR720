package com.dreamfish.com.vr720.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.dreamfish.com.vr720.R;

public class MainThumbnailImageView extends AppCompatImageView {

    private Paint paint;
    private Paint paint2;
    private Paint paint3;
    private Paint paint4;
    private Paint paintSelectBox;
    private Paint paintSelectBoxBg;
    private String imageText = "";
    private String imageSize = "未知";
    private int imageTextColor = Color.WHITE;
    private int roundWidth = 20;
    private int roundHeight = 20;
    private int imageTextSize = 20;
    private boolean checked = false;
    private int checkIndex = 0;

    public MainThumbnailImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public MainThumbnailImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MainThumbnailImageView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MainThumbnailImageView);
            imageSize = a.getString(R.styleable.MainThumbnailImageView_imageSize);
            imageText = a.getString(R.styleable.MainThumbnailImageView_imageText);
            imageTextColor = a.getColor(R.styleable.MainThumbnailImageView_imageTextColor, imageTextColor);
            imageTextSize = a.getDimensionPixelSize(R.styleable.MainThumbnailImageView_imageTextSize, imageTextSize);
            roundWidth = a.getDimensionPixelSize(R.styleable.MainThumbnailImageView_imageRoundWidth, roundWidth);
            roundHeight = a.getDimensionPixelSize(R.styleable.MainThumbnailImageView_imageRoundHeight, roundHeight);
            a.recycle();
        } else {
            float density = context.getResources().getDisplayMetrics().density;
            roundWidth = (int) (roundWidth * density);
            roundHeight = (int) (roundHeight * density);
            imageTextSize = (int) (imageTextSize * density);
        }

        paint4 = new Paint();
        paint4.setColor(Color.WHITE);
        paint4.setAntiAlias(true);
        paint4.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        paint = new Paint();
        paint.setColor(imageTextColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        paint.setTextSize(imageTextSize);

        paint2 = new Paint();
        paint2.setColor(imageTextColor);
        paint2.setAntiAlias(true);
        paint2.setStrokeWidth(5);
        paint2.setTextSize(imageTextSize);
        paint2.setTextAlign(Paint.Align.RIGHT);

        paintSelectBox = new Paint();
        paintSelectBox.setColor(getResources().getColor(R.color.colorPrimary, null));
        paintSelectBox.setAntiAlias(true);
        paintSelectBox.setStrokeWidth(16);
        paintSelectBox.setStyle(Paint.Style.STROKE);

        paintSelectBoxBg = new Paint();
        paintSelectBoxBg.setColor(getResources().getColor(R.color.colorPrimary, null));
        paintSelectBoxBg.setAntiAlias(true);
        paintSelectBoxBg.setStyle(Paint.Style.FILL);

        paint3 = new Paint();
        paint3.setXfermode(null);
    }

    @Override
    public void draw(Canvas canvas) {

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap);
        super.draw(canvas2);

        drawLiftUp(canvas2);
        drawLiftDown(canvas2);
        drawRightUp(canvas2);
        drawRightDown(canvas2);

        if(!imageText.isEmpty()) canvas2.drawText(imageText, checked ? 95 : 20, getHeight() - imageTextSize, paint);
        if(!imageSize.isEmpty()) canvas2.drawText(imageSize, getWidth() - 20, getHeight() - imageTextSize, paint2);
        if(checked) drawSelectedBox(canvas2);

        canvas.drawBitmap(bitmap, 0, 0, paint3);
    }

    private void drawLiftUp(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, roundHeight);
        path.lineTo(0, 0);
        path.lineTo(roundWidth, 0);
        path.arcTo(new RectF(0, 0, roundWidth * 2, roundHeight * 2), -90, -90);
        path.close();
        canvas.drawPath(path, paint4);
    }

    private void drawLiftDown(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, getHeight() - roundHeight);
        path.lineTo(0, getHeight());
        path.lineTo(roundWidth, getHeight());
        path.arcTo(new RectF(0, getHeight() - roundHeight * 2, roundWidth * 2, getHeight()), 90, 90);
        path.close();
        canvas.drawPath(path, paint4);
    }

    private void drawRightDown(Canvas canvas) {
        Path path = new Path();
        path.moveTo(getWidth() - roundWidth, getHeight());
        path.lineTo(getWidth(), getHeight());
        path.lineTo(getWidth(), getHeight() - roundHeight);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, getHeight() - roundHeight * 2, getWidth(), getHeight()), -0, 90);
        path.close();
        canvas.drawPath(path, paint4);
    }

    private void drawRightUp(Canvas canvas) {
        Path path = new Path();
        path.moveTo(getWidth(), roundHeight);
        path.lineTo(getWidth(), 0);
        path.lineTo(getWidth() - roundWidth, 0);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, 0, getWidth(), roundHeight * 2), -90, 90);
        path.close();
        canvas.drawPath(path, paint4);
    }

    private void drawSelectedBox(Canvas canvas) {
        int y = getHeight() - imageTextSize;
        if(checkIndex > 0) {
            canvas.drawRect(26, y - imageTextSize, 20 + imageTextSize + 12, y + 12, paintSelectBoxBg);
            canvas.drawText(String.valueOf(checkIndex), 36, y, paint);
        }
        canvas.drawRect(0,0, getWidth(),getHeight(), paintSelectBox);
    }

    public int getCheckIndex() {
        return checkIndex;
    }

    public void setCheckIndex(int checkIndex) {
        this.checkIndex = checkIndex;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        postInvalidate();
    }

    public String getImageText() { return imageText; }

    public void setImageText(String imageText) {
        this.imageText = imageText;
        postInvalidate();
    }

    public String getImageSize() { return imageSize; }

    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
        postInvalidate();
    }

}
