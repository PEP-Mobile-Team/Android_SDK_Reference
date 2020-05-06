/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.thumbnail.createpage.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;

public class ColorPickerView extends FrameLayout implements ColorObserver {

    private final ObservableColor observableColor = new ObservableColor(0);

    private SparseIntArray mDefaultColors;

    private ImageView mIvOriginalColor;
    private ImageView mIvCurrentColor;
    private ColorWheelView mHueSatView;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        observableColor.addObserver(this);

        if (AppDisplay.getInstance(context).isPad()) {
            LayoutInflater.from(context).inflate(R.layout.rd_pick_color_layout_pad, this);
        } else {
            LayoutInflater.from(context).inflate(R.layout.rd_pick_color_layout, this);
        }
        mHueSatView = (ColorWheelView) findViewById(R.id.rd_pick_color_hs);
        mHueSatView.observeColor(observableColor);

        ColorSlideView valueView = (ColorSlideView) findViewById(R.id.rd_pick_color_value);
        valueView.observeColor(observableColor);

        mIvOriginalColor = findViewById(R.id.iv_old_color);
        mIvCurrentColor = findViewById(R.id.iv_new_color);

        mDefaultColors = new SparseIntArray();
        mDefaultColors.put(R.id.iv_createpdf_while_color, Color.argb(255, 255, 255, 255));
        mDefaultColors.put(R.id.iv_createpdf_paper_color, Color.argb(255, 233, 224, 199));
        mDefaultColors.put(R.id.iv_createpdf_dark_grey_color, Color.argb(255, 50, 50, 50));
        initListeners();
    }

    private void initListeners() {
        for (int i = 0; i < mDefaultColors.size(); i++) {
            int id = mDefaultColors.keyAt(i);
            findViewById(id).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = 0; i < mDefaultColors.size(); i++) {
                        int id = mDefaultColors.keyAt(i);
                        if (id == v.getId()) {
                            findViewById(id).setSelected(true);
                            setCurrentColor(mDefaultColors.get(id));
                        } else {
                            findViewById(id).setSelected(false);
                        }
                    }
                }
            });
        }
    }

    public int getColor() {
        return observableColor.getColor();
    }

    public void setColor(int color) {
        setOriginalColor(color);
        setCurrentColor(color);
    }

    public void setOriginalColor(int color) {
        mIvOriginalColor.setColorFilter(color);
    }

    public void setCurrentColor(int color) {
        mHueSatView.setColor(color);
    }

    public void addColorObserver(ColorObserver observer) {
        observableColor.addObserver(observer);
    }

    @Override
    public void updateColor(ObservableColor observableColor) {
        mIvCurrentColor.setColorFilter(observableColor.getColor());
        int nSize = mDefaultColors.size();
        for (int i = 0; i < nSize; i++) {
            int defaultColor = mDefaultColors.valueAt(i);
            if (defaultColor == observableColor.getColor()) {
                findViewById(mDefaultColors.keyAt(i)).setSelected(true);
            } else {
                findViewById(mDefaultColors.keyAt(i)).setSelected(false);
            }
        }
    }

}
