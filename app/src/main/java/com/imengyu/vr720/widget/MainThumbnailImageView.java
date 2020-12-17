package com.imengyu.vr720.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.utils.PixelTool;

public class MainThumbnailImageView extends AppCompatImageView {

    private Paint paint;
    private Paint paint0;
    private Paint paintTextBackground;
    private Paint paintBorder;
    private String imageText = "";
    private String imageSize = "未知";
    private int imageTextColor = Color.WHITE;
    private int primaryColor = Color.WHITE;
    private int roundWidth = 20;
    private int roundHeight = 20;
    private int imageTextSize = 20;
    private Context context;

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
        this.context = context;

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

        primaryColor = context.getResources().getColor(R.color.colorPrimary, null);

        paint0 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint = new Paint();
        paint.setColor(imageTextColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        paint.setTextSize(imageTextSize);

        paintBorder = new Paint();
        paintBorder.setColor(Color.WHITE);
        paintBorder.setAntiAlias(true);
        paintBorder.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        paintTextBackground = new Paint();
        paintTextBackground.setAntiAlias(true);
        paintTextBackground.setStyle(Paint.Style.FILL);
        paintTextBackground.setColor(Color.WHITE);
        LinearGradient mShader = new LinearGradient(
                0, 0,
                0, PixelTool.dp2px(context, imageTextSize + 5),
                context.getResources().getColor(R.color.colorTextBgEnd, null),
                context.getResources().getColor(R.color.colorTextBgStart, null),
                    Shader.TileMode.MIRROR);
        paintTextBackground.setShader(mShader);
    }

    @Override
    public void draw(Canvas canvas) {

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap);
        super.draw(canvas2);

        //文字背景
        canvas2.drawRect(0,getHeight() - PixelTool.dp2px(context, imageTextSize + 5),
                getWidth(), getHeight() , paintTextBackground);

        //圆角
        drawRound(canvas2, paintBorder);

        //文字
        if(!imageText.isEmpty()) {
            paint.setColor(imageTextColor);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas2.drawText(imageText, 20, getHeight() - imageTextSize + 5, paint);
        }

        if(!imageSize.isEmpty()) {
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(imageTextColor);
            canvas2.drawText(imageSize, getWidth() - 20, getHeight() - imageTextSize + 5, paint);
        }

        canvas.drawBitmap(bitmap, 0, 0, paint0);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                //在按下事件中设置滤镜
                setFilter();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                //在CANCEL和UP事件中清除滤镜
                removeFilter();
                break;
        }
        return super.onTouchEvent(event);
    }


    private void drawRound(Canvas canvas, Paint paint) {
        paint.setColor(Color.WHITE);

        Path path = new Path();
        path.moveTo(0, roundHeight);
        path.lineTo(0, 0);
        path.lineTo(roundWidth, 0);
        path.arcTo(new RectF(0, 0, roundWidth * 2, roundHeight * 2), -90, -90);
        path.close();
        canvas.drawPath(path, paint);

        path = new Path();
        path.moveTo(0, getHeight() - roundHeight);
        path.lineTo(0, getHeight());
        path.lineTo(roundWidth, getHeight());
        path.arcTo(new RectF(0, getHeight() - roundHeight * 2, roundWidth * 2, getHeight()), 90, 90);
        path.close();
        canvas.drawPath(path, paint);

        path = new Path();
        path.moveTo(getWidth() - roundWidth, getHeight());
        path.lineTo(getWidth(), getHeight());
        path.lineTo(getWidth(), getHeight() - roundHeight);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, getHeight() - roundHeight * 2, getWidth(), getHeight()), -0, 90);
        path.close();
        canvas.drawPath(path, paint);

        path = new Path();
        path.moveTo(getWidth(), roundHeight);
        path.lineTo(getWidth(), 0);
        path.lineTo(getWidth() - roundWidth, 0);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, 0, getWidth(), roundHeight * 2), -90, 90);
        path.close();
        canvas.drawPath(path, paint);
    }

    /**
     *   设置滤镜
     */
    public void setFilter() {
        Drawable drawable = getDrawable();
        if(drawable != null)
            drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    }
    /**
     *   清除滤镜
     */
    public void removeFilter() {
        Drawable drawable = getDrawable();
        if(drawable != null)
            drawable.clearColorFilter();
    }



    public void setChecked(boolean checked) {
    }
    public void setImageText(String imageText) {
        this.imageText = imageText;
        postInvalidate();
    }
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
        postInvalidate();
    }

}
