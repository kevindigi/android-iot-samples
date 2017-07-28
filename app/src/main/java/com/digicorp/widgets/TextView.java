package com.digicorp.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;

import com.digicorp.androidiotsamples.R;

import java.io.FileNotFoundException;
import java.util.WeakHashMap;

/**
 *
 */
public class TextView extends android.support.v7.widget.AppCompatTextView {

    private static WeakHashMap<String, Typeface> fontMap = new WeakHashMap<>();

    private static final int HTML_STYLE_STRIKE_THROUGH = 0;

    private Rect lineBoundsRect;
    private Paint underlinePaint;

    public TextView(Context context) throws FileNotFoundException {
        super(context);
        init(context, null, 0);
    }

    public TextView(Context context, AttributeSet attrs) throws FileNotFoundException {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyle) throws FileNotFoundException {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) throws FileNotFoundException {
        if (isInEditMode())
            return;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DigicorpComponents, defStyle, 0);
        if (typedArray != null) {
            if (typedArray.hasValue(R.styleable.DigicorpComponents_font_path)) {
                String assetFontFileName = typedArray.getString(R.styleable.DigicorpComponents_font_path);
                if (fontMap.containsKey(assetFontFileName) && fontMap.get(assetFontFileName) != null) {
                    setTypeface(fontMap.get(assetFontFileName));
                } else {
                    Typeface typeface = Typeface.createFromAsset(context.getAssets(), assetFontFileName);
                    if (typeface == null) {
                        throw new FileNotFoundException("Font file not found in asset : " + assetFontFileName);
                    }
                    fontMap.put(assetFontFileName, typeface);
                    setTypeface(typeface);
                }
            }
            if (typedArray.hasValue(R.styleable.DigicorpComponents_html_style)) {
                int style = typedArray.getInt(R.styleable.DigicorpComponents_html_style, -1);
                if (style != -1) {
                    switch (style) {
                        case HTML_STYLE_STRIKE_THROUGH:
                            setPaintFlags(getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    }
                }
            }

            if (typedArray.hasValue(R.styleable.DigicorpComponents_underlineColor)) {
                int mColor = typedArray.getColor(R.styleable.DigicorpComponents_underlineColor, 0xFFFF0000);
                lineBoundsRect = new Rect();
                underlinePaint = new Paint();
                underlinePaint.setStyle(Paint.Style.STROKE);
                underlinePaint.setColor(mColor); //color of the underline
                underlinePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 2);
            }
            typedArray.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (underlinePaint != null) {
            int count = getLineCount();

            final Layout layout = getLayout();
            float x_start, x_stop, x_diff;
            int firstCharInLine, lastCharInLine;

            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, lineBoundsRect);
                firstCharInLine = layout.getLineStart(i);
                lastCharInLine = layout.getLineEnd(i);

                x_start = layout.getPrimaryHorizontal(firstCharInLine);
                x_diff = layout.getPrimaryHorizontal(firstCharInLine + 1) - x_start;
                x_stop = layout.getPrimaryHorizontal(lastCharInLine - 1) + x_diff;

                canvas.drawLine(x_start, baseline + underlinePaint.getStrokeWidth(),
                        x_stop, baseline + underlinePaint.getStrokeWidth(), underlinePaint);
            }
        }

        super.onDraw(canvas);
    }


}
