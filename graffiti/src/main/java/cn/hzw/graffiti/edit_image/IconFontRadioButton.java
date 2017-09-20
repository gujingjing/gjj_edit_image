package cn.hzw.graffiti.edit_image;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import cn.hzw.graffiti.R;


/**
 * Created by jingjinggu on 2017/9/13.
 */

public class IconFontRadioButton extends RadioButton {

    private int nomalColor, selectedColor, nomalTextSize, selectedTextSize;

    public IconFontRadioButton(Context context) {
        super(context);
        init(context, null, 0);
    }

    public IconFontRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public IconFontRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(final Context context, AttributeSet attrs, int defStyleAttr) {
        Typeface iconfont = Typeface.createFromAsset(context.getAssets(), "iconfont.ttf");
        this.setTypeface(iconfont);


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.icon_font_style);
        nomalColor = a.getColor(R.styleable.icon_font_style_nomalColor, context.getResources().getColor(R.color.white));
        selectedColor = a.getColor(R.styleable.icon_font_style_nomalColor, context.getResources().getColor(R.color.selected_color));
        nomalTextSize = a.getInt(R.styleable.icon_font_style_nomalTextSize, 18);
        selectedTextSize = a.getInt(R.styleable.icon_font_style_selectedTextsize, 18);

        this.setTextColor(context.getResources().getColor(R.color.white));
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP,nomalTextSize);

        this.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    IconFontRadioButton.this.setTextSize(TypedValue.COMPLEX_UNIT_SP,selectedTextSize);
//                    IconFontRadioButton.this.setTextColor(selectedColor);
                } else {
                    IconFontRadioButton.this.setTextSize(TypedValue.COMPLEX_UNIT_SP,nomalTextSize);
//                    IconFontRadioButton.this.setTextColor(nomalColor);
                }
            }
        });

    }
}
