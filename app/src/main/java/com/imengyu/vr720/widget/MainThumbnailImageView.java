package com.imengyu.vr720.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
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
import com.imengyu.vr720.utils.StringUtils;

public class MainThumbnailImageView extends AppCompatImageView {

    private Paint paint;
    private Paint paint0;
    private Paint paintTextBackground;
    private Paint paintBorder;
    private String imageText = "";
    private String imageSize = "未知";
    private int imageTextColor = Color.WHITE;
    private int roundWidth = 20;
    private int roundHeight = 20;
    private int imageTextSize = 20;
    private boolean leftTextReserveSpace = false;
    private boolean enableRenderExtras = true;
    private Context context;
    private final BlendModeColorFilter blendModeColorFilter = new BlendModeColorFilter(Color.GRAY, BlendMode.MULTIPLY);

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
            roundWidth = PixelTool.dp2px(context, roundWidth);
            roundHeight = PixelTool.dp2px(context,roundHeight);
            imageTextSize = PixelTool.dp2px(context,imageTextSize);
        }

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

        dpTextPadding = PixelTool.dp2px(context, 0);
        dp7 = PixelTool.dp2px(context, 7);
        dp50 = PixelTool.dp2px(context, 50);

        paintTextBackground = new Paint();
        paintTextBackground.setAntiAlias(true);
        paintTextBackground.setStyle(Paint.Style.FILL);
        paintTextBackground.setColor(Color.WHITE);


    }

    private int dpTextPadding;
    private int dp7;
    private int dp50;

    private int lastHeight = 0;
    private int lastWidth = 0;

    @Override
    public void draw(Canvas canvas) {

        if(!enableRenderExtras) {
            super.draw(canvas);
            return;
        }

        int height = getHeight();
        int width = getWidth();

        if(lastHeight != height || lastWidth != width) {
            lastHeight = height;
            lastWidth = width;

            LinearGradient mShader = new LinearGradient(
                    0, getHeight() - PixelTool.dp2px(context, imageTextSize) - dpTextPadding,
                    0, getHeight(),
                    context.getResources().getColor(R.color.colorTextBgStart, null),
                    context.getResources().getColor(R.color.colorTextBgEnd, null),
                    Shader.TileMode.MIRROR);
            paintTextBackground.setShader(mShader);
        }

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap);
        super.draw(canvas2);

        //文字背景
        canvas2.drawRect(0,getHeight() - PixelTool.dp2px(context, imageTextSize) - dpTextPadding,
                getWidth(), getHeight() , paintTextBackground);

        //圆角
        drawRound(canvas2, paintBorder);

        //文字
        if(!imageText.isEmpty()) {
            paint.setColor(imageTextColor);
            paint.setTextAlign(Paint.Align.LEFT);

            int textBoxWidth = getWidth() - (leftTextReserveSpace ? dp50 : dp7) - dp50;
            String subText = imageText;
            if(!StringUtils.isNullOrEmpty(imageText)) {
                float textWidth = paint.measureText(imageText);
                if (textWidth > textBoxWidth) {
                    int subIndex = paint.breakText(imageText, 0, imageText.length(), true, textBoxWidth, null);
                    subText = imageText.substring(0, subIndex-3) + "...";
                }
            }

            canvas2.drawText(subText, leftTextReserveSpace ? dp50 : dp7, getHeight() - imageTextSize - (int)(dpTextPadding / 2.0), paint);
        }

        if(!imageSize.isEmpty()) {
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(imageTextColor);
            canvas2.drawText(imageSize, getWidth() - dp7, getHeight() - imageTextSize - (int)(dpTextPadding / 2.0), paint);
        }

        canvas.drawBitmap(bitmap, 0, 0, paint0);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
    @SuppressLint("ClickableViewAccessibility")
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
    @Override
    public boolean performClick() {
        return super.performClick();
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
            drawable.setColorFilter(blendModeColorFilter);
    }
    /**
     *   清除滤镜
     */
    public void removeFilter() {
        Drawable drawable = getDrawable();
        if(drawable != null)
            drawable.clearColorFilter();
    }

    public void setImageText(String imageText) {
        this.imageText = imageText;
    }
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    public void setLeftTextReserveSpace(boolean leftTextReserveSpace) {
        this.leftTextReserveSpace = leftTextReserveSpace;
    }
    public void setEnableRenderExtras(boolean enableRenderExtras) {
        this.enableRenderExtras = enableRenderExtras;
    }
}
