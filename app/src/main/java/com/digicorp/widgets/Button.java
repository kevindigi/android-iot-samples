package com.digicorp.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.digicorp.androidiotsamples.R;

import java.io.FileNotFoundException;

/**
 * Created by kevin.adesara on 07/07/14.
 */
public class Button extends android.support.v7.widget.AppCompatButton {

    public Button(Context context) throws FileNotFoundException {
        super(context);
        init(context, null, 0);
    }

    public Button(Context context, AttributeSet attrs) throws FileNotFoundException {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public Button(Context context, AttributeSet attrs, int defStyle) throws FileNotFoundException {
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
                Typeface typeface = Typeface.createFromAsset(context.getAssets(), assetFontFileName);
                if (typeface == null) {
                    throw new FileNotFoundException("Font file not found in asset : " + assetFontFileName);
                }

                setTypeface(typeface);
            }
            typedArray.recycle();
        }
    }
}
