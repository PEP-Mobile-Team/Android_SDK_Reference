/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.controls.propertybar.imp;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;
import java.util.List;

public class ColorView extends FrameLayout {

    private Context mContext;
    private AppDisplay display;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private ViewGroup mParent;
    private ViewPager mColorViewPager;
    private LinearLayout mLlColorDots;
    private LinearLayout mPBLlColors;
    private LinearLayout mColorsPickerRoot;

    private int[] mColorDotPics = new int[]{R.drawable.pb_ll_colors_dot_selected, R.drawable.pb_ll_colors_dot};
    private int[] mColors;

    private int mCurrentColorIndex;
    private int mColor;
    private int mCurrentWidth;
    private int mPadWidth;
    private boolean canEdit;

    public ColorView(Context context, ViewGroup parent, int color, int[] colors, boolean canEdit) {
        super(context);
        this.mContext = context;
        this.mParent = parent;
        this.canEdit = canEdit;
        this.mColor = color;
        this.mColors = colors;
        display = AppDisplay.getInstance(mContext);
        mPadWidth = display.dp2px(320.0f);

        init();
    }

    private void init() {
        View colorItemView = LayoutInflater.from(mContext).inflate(R.layout.pb_color, this);
        TextView pb_tv_colorTitle = (TextView) colorItemView.findViewById(R.id.pb_tv_colorTitle);
        pb_tv_colorTitle.setText(mContext.getApplicationContext().getString(R.string.fx_string_color));
        mColorViewPager = (ViewPager) colorItemView.findViewById(R.id.pb_ll_colors_viewpager);
        mLlColorDots = (LinearLayout) colorItemView.findViewById(R.id.pb_ll_colors_dots);
        LinearLayout.LayoutParams vpParams = (LinearLayout.LayoutParams) mColorViewPager.getLayoutParams();
        if (display.isPad()) {
            vpParams.height = display.dp2px(130.0f);
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                vpParams.height = display.dp2px(80.0f);
            } else {
                vpParams.height = display.dp2px(130.0f);
            }
        }
        mColorViewPager.setLayoutParams(vpParams);

        // ViewPager view1
        mPBLlColors = new LinearLayout(mContext);
        mPBLlColors.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mPBLlColors.setOrientation(LinearLayout.HORIZONTAL);
        initColorOne(mPBLlColors);

        // ViewPager view2
        mColorsPickerRoot = new LinearLayout(mContext);
        mColorsPickerRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mColorsPickerRoot.setOrientation(LinearLayout.HORIZONTAL);
        mColorsPickerRoot.setGravity(Gravity.CENTER);
        ColorPicker colorPicker = new ColorPicker(mContext, mParent);

        colorPicker.setEditable(canEdit);
        mColorsPickerRoot.addView(colorPicker);

        ImageView selfColor = new ImageView(mContext);
        LinearLayout.LayoutParams selfColorParams = new LinearLayout.LayoutParams(display.dp2px(30.0f), display.dp2px(120.0f));
        if (display.isPad()) {
            selfColorParams.height = display.dp2px(120.0f);
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                selfColorParams.height = display.dp2px(80.0f);
            } else {
                selfColorParams.height = display.dp2px(120.0f);
            }
        }
        selfColorParams.leftMargin = display.dp2px(10.0f);
        selfColor.setLayoutParams(selfColorParams);

        selfColor.setImageDrawable(new ColorDrawable(mColor));
        mColorsPickerRoot.addView(selfColor);

        colorPicker.setOnUpdateViewListener(new PropertyBar.UpdateViewListener() {
            @Override
            public void onUpdate(long property, int value) {
                mColor = value;
                ((ImageView) mColorsPickerRoot.getChildAt(1)).setImageDrawable(new ColorDrawable(value));
                initColorOne(mPBLlColors);

                if (mPropertyChangeListener != null) {
                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_SELF_COLOR, value);
                }
            }
        });

        List<View> colorViewList = new ArrayList<View>();
        colorViewList.add(mPBLlColors);
        colorViewList.add(mColorsPickerRoot);

        ColorVPAdapter colorVPAdapter = new ColorVPAdapter(colorViewList);
        mColorViewPager.setAdapter(colorVPAdapter);
        mColorViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentColorIndex = position;

                for (int i = 0; i < mLlColorDots.getChildCount(); i++) {
                    ImageView imageView = (ImageView) mLlColorDots.getChildAt(i);
                    if (i == position) {
                        imageView.setImageResource(mColorDotPics[0]);
                    } else {
                        imageView.setImageResource(mColorDotPics[1]);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        for (int i = 0; i < mLlColorDots.getChildCount(); i++) {
            ImageView imageView = (ImageView) mLlColorDots.getChildAt(i);
            imageView.setTag(i);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (Integer) v.getTag();

                    if (mCurrentColorIndex != index) {
                        for (int j = 0; j < mLlColorDots.getChildCount(); j++) {
                            ImageView iv = (ImageView) mLlColorDots.getChildAt(j);
                            if (j == index) {
                                iv.setImageResource(mColorDotPics[0]);
                            } else {
                                iv.setImageResource(mColorDotPics[1]);
                            }
                        }

                        mColorViewPager.setCurrentItem(index);
                    }
                }
            });

            if (i == 0) {
                imageView.setImageResource(mColorDotPics[0]);
            } else {
                imageView.setImageResource(mColorDotPics[1]);
            }
        }
        mColorViewPager.setCurrentItem(mCurrentColorIndex);

        if (!canEdit) {
            colorItemView.setAlpha(PropertyBar.PB_ALPHA);
        }
    }

    private void initColorOne(LinearLayout pb_ll_colors) {
        pb_ll_colors.removeAllViews();
        int colorWidth = display.dp2px(30.0f);
        final int padding = display.dp2px(3.0f);
        int space = display.dp2px(5.0f);

        int tempWidth = mParent.getWidth();
        int tempHeight = mParent.getHeight();

        if (display.isPad()) {
            mCurrentWidth = mPadWidth;
        } else {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mCurrentWidth = Math.max(tempWidth, tempHeight);
                if (mCurrentWidth == AppDisplay.getInstance(mContext).getRawScreenWidth())
                    mCurrentWidth -= AppDisplay.getInstance(mContext).getRealNavBarHeight();
            } else {
                mCurrentWidth = Math.min(tempWidth, tempHeight);
            }
        }

        int length;
        if (display.isPad()) {
            length = mCurrentWidth - display.dp2px(16.0f + 4.0f) * 2;
        } else {
            length = mCurrentWidth - display.dp2px(16.0f) * 2;
        }

        boolean isColorSelected = false;
        int reserveSpace = display.dp2px(6.0f);
        // one Row
        if (length > (colorWidth + padding * 2 + reserveSpace) * mColors.length - reserveSpace) {
            if (mColors.length > 1) {
                space = (length - colorWidth * mColors.length - padding * 2 * mColors.length) / (mColors.length - 1);
            } else {
                space = 0;
            }
            // 1 rows
            pb_ll_colors.setOrientation(LinearLayout.HORIZONTAL);
            pb_ll_colors.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

            for (int i = 0; i < mColors.length; i++) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(colorWidth + padding * 2, colorWidth + padding * 2);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER);
                if (i > 0) {
                    linearLayoutParams.leftMargin = space;
                } else {
                    linearLayoutParams.leftMargin = 0;
                }
                linearLayout.setLayoutParams(linearLayoutParams);
                linearLayout.setTag(i);

                LinearLayout oneColor = new LinearLayout(mContext);
                LinearLayout.LayoutParams oneColorParams = new LinearLayout.LayoutParams(colorWidth, colorWidth);
                oneColor.setOrientation(LinearLayout.HORIZONTAL);
                oneColor.setGravity(Gravity.CENTER);
                oneColor.setLayoutParams(oneColorParams);
                oneColor.setBackgroundResource(R.drawable.pb_color_bg_border);
                linearLayout.addView(oneColor);

                ImageView color = new ImageView(mContext);
                color.setLayoutParams(new LinearLayout.LayoutParams(colorWidth - 2, colorWidth - 2));
                color.setImageDrawable(new ColorDrawable(mColors[i]));
                oneColor.addView(color);

                pb_ll_colors.addView(linearLayout);

                if (!isColorSelected && mColor == mColors[i]) {
                    isColorSelected = true;
                    linearLayout.setBackgroundResource(R.drawable.pb_color_bg);
                } else {
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }

                if (canEdit) {
                    linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v instanceof LinearLayout) {
                                int tag = ((Integer) v.getTag()).intValue();
                                mColor = mColors[tag];
                                for (int j = 0; j < mColors.length; j++) {
                                    if (j == tag) {
                                        v.setBackgroundResource(R.drawable.pb_color_bg);
                                    } else {
                                        LinearLayout colorRow = (LinearLayout) ((LinearLayout) v).getParent();
                                        LinearLayout otherColor = (LinearLayout) colorRow.getChildAt(j);
                                        otherColor.setBackgroundColor(Color.TRANSPARENT);
                                    }
                                }
                                if (mPropertyChangeListener != null) {
                                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_COLOR, mColor);
                                }
                            }
                        }
                    });
                } else {
                    linearLayout.setEnabled(false);
                }
            }
        } else if (length > (colorWidth + padding * 2 + reserveSpace) * (mColors.length / 2)) { // two Row
            if (mColors.length > 1) {
                if (mColors.length % 2 == 0) {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 2));
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 2 - 1));
                    } else {
                        space = 0;
                    }
                } else {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 2) + 1);
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 2));
                    } else {
                        space = 0;
                    }
                }
            } else {
                space = 0;
            }

            // 2 rows
            pb_ll_colors.setOrientation(LinearLayout.VERTICAL);
            pb_ll_colors.setGravity(Gravity.CENTER);

            LinearLayout ll_ColorRow1 = new LinearLayout(mContext);
            ll_ColorRow1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow1.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow1.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow1);

            LinearLayout ll_ColorRow2 = new LinearLayout(mContext);
            ll_ColorRow2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow2.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow2.setPadding(0, display.dp2px(5.0f), 0, 0);
            ll_ColorRow2.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow2);

            for (int i = 0; i < mColors.length; i++) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(colorWidth + padding * 2, colorWidth + padding * 2);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER);
                if (mColors.length % 2 == 0) {
                    if ((i > 0 && i < mColors.length / 2) || (i > mColors.length / 2)) {
                        linearLayoutParams.leftMargin = space;
                    }
                } else {
                    if ((i > 0 && i < mColors.length / 2 + 1) || (i > mColors.length / 2 + 1)) {
                        linearLayoutParams.leftMargin = space;
                    }
                }
                linearLayout.setLayoutParams(linearLayoutParams);
                linearLayout.setTag(i);

                LinearLayout oneColor = new LinearLayout(mContext);
                LinearLayout.LayoutParams oneColorParams = new LinearLayout.LayoutParams(colorWidth, colorWidth);
                oneColor.setOrientation(LinearLayout.HORIZONTAL);
                oneColor.setGravity(Gravity.CENTER);
                oneColor.setLayoutParams(oneColorParams);
                oneColor.setBackgroundResource(R.drawable.pb_color_bg_border);
                linearLayout.addView(oneColor);

                ImageView color = new ImageView(mContext);
                color.setLayoutParams(new LinearLayout.LayoutParams(colorWidth - 2, colorWidth - 2));
                color.setImageDrawable(new ColorDrawable(mColors[i]));
                oneColor.addView(color);

                if (mColors.length % 2 == 0) {
                    if (i < mColors.length / 2) {
                        ll_ColorRow1.addView(linearLayout);
                    } else {
                        ll_ColorRow2.addView(linearLayout);
                    }
                } else {
                    if (i < mColors.length / 2 + 1) {
                        ll_ColorRow1.addView(linearLayout);
                    } else {
                        ll_ColorRow2.addView(linearLayout);
                    }
                }

                if (!isColorSelected && mColor == mColors[i]) {
                    isColorSelected = true;
                    linearLayout.setBackgroundResource(R.drawable.pb_color_bg);
                } else {
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }

                if (canEdit) {
                    linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v instanceof LinearLayout) {
                                int tag = ((Integer) v.getTag()).intValue();
                                mColor = mColors[tag];

                                for (int j = 0; j < mColors.length; j++) {
                                    if (j == tag) {
                                        v.setBackgroundResource(R.drawable.pb_color_bg);
                                    } else {
                                        LinearLayout otherColor;
                                        if (mColors.length % 2 == 0) {
                                            if (j < mColors.length / 2) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(0)).getChildAt(j);

                                            } else {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(1)).getChildAt(j - mColors.length / 2);
                                            }
                                        } else {
                                            if (j < mColors.length / 2 + 1) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(0)).getChildAt(j);

                                            } else {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) ((LinearLayout) v).getParent()).getParent()).getChildAt(1)).getChildAt(j - (mColors.length / 2 + 1));
                                            }
                                        }
                                        otherColor.setBackgroundColor(Color.TRANSPARENT);
                                    }
                                }
                                if (mPropertyChangeListener != null) {
                                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_COLOR, mColor);
                                }
                            }
                        }
                    });
                } else {
                    linearLayout.setEnabled(false);
                }
            }
        } else { // three Row
            if (mColors.length > 1) {
                if (mColors.length % 3 == 0) {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 3));
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 3 - 1));
                    } else {
                        space = 0;
                    }
                } else {
                    int spaces = length - (colorWidth + padding * 2) * ((int) (mColors.length / 3) + 1);
                    if (spaces > 0) {
                        space = spaces / ((int) (mColors.length / 3));
                    } else {
                        space = 0;
                    }
                }
            } else {
                space = 0;
            }

            // 3 rows
            pb_ll_colors.setOrientation(LinearLayout.VERTICAL);
            pb_ll_colors.setGravity(Gravity.CENTER);

            LinearLayout ll_ColorRow1 = new LinearLayout(mContext);
            ll_ColorRow1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow1.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow1.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow1);

            LinearLayout ll_ColorRow2 = new LinearLayout(mContext);
            ll_ColorRow2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow2.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow2.setPadding(0, display.dp2px(5.0f), 0, 0);
            ll_ColorRow2.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow2);

            LinearLayout ll_ColorRow3 = new LinearLayout(mContext);
            ll_ColorRow3.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll_ColorRow3.setOrientation(LinearLayout.HORIZONTAL);
            ll_ColorRow3.setPadding(0, display.dp2px(5.0f), 0, 0);
            ll_ColorRow3.setGravity(Gravity.CENTER);
            pb_ll_colors.addView(ll_ColorRow3);

            for (int i = 0; i < mColors.length; i++) {
                LinearLayout linearLayout = new LinearLayout(mContext);
                LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(colorWidth + padding * 2, colorWidth + padding * 2);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER);
                if (mColors.length % 3 == 0) {
                    if ((i > 0 && i < mColors.length / 3)
                            || (i > mColors.length / 3 && i < mColors.length / 3 * 2)
                            || (i > mColors.length / 3 * 2)) {
                        linearLayoutParams.leftMargin = space;
                    }
                } else {
                    if ((i > 0 && i < mColors.length / 3 + 1)
                            || ((i > mColors.length / 3 + 1) && i < (mColors.length / 3 + 1) * 2)
                            || i > (mColors.length / 3 + 1) * 2) {
                        linearLayoutParams.leftMargin = space;
                    }
                }
                linearLayout.setLayoutParams(linearLayoutParams);
                linearLayout.setTag(i);

                LinearLayout oneColor = new LinearLayout(mContext);
                LinearLayout.LayoutParams oneColorParams = new LinearLayout.LayoutParams(colorWidth, colorWidth);
                oneColor.setOrientation(LinearLayout.HORIZONTAL);
                oneColor.setGravity(Gravity.CENTER);
                oneColor.setLayoutParams(oneColorParams);
                oneColor.setBackgroundResource(R.drawable.pb_color_bg_border);
                linearLayout.addView(oneColor);

                ImageView color = new ImageView(mContext);
                color.setLayoutParams(new LinearLayout.LayoutParams(colorWidth - 2, colorWidth - 2));
                color.setImageDrawable(new ColorDrawable(mColors[i]));
                oneColor.addView(color);

                if (mColors.length % 3 == 0) {
                    if (i < mColors.length / 3) {
                        ll_ColorRow1.addView(linearLayout);
                    } else if (i < mColors.length / 3 * 2) {
                        ll_ColorRow2.addView(linearLayout);
                    } else {
                        ll_ColorRow3.addView(linearLayout);
                    }
                } else {
                    if (i < mColors.length / 3 + 1) {
                        ll_ColorRow1.addView(linearLayout);
                    } else if (i < (mColors.length / 3 + 1) * 2) {
                        ll_ColorRow2.addView(linearLayout);
                    } else {
                        ll_ColorRow3.addView(linearLayout);
                    }
                }

                if (!isColorSelected && mColor == mColors[i]) {
                    isColorSelected = true;
                    linearLayout.setBackgroundResource(R.drawable.pb_color_bg);
                } else {
                    linearLayout.setBackgroundColor(Color.TRANSPARENT);
                }

                if (canEdit) {
                    linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v instanceof LinearLayout) {
                                int tag = ((Integer) v.getTag()).intValue();
                                mColor = mColors[tag];

                                for (int j = 0; j < mColors.length; j++) {
                                    if (j == tag) {
                                        v.setBackgroundResource(R.drawable.pb_color_bg);
                                    } else {
                                        LinearLayout otherColor;
                                        if (mColors.length % 3 == 0) {
                                            if (j < mColors.length / 3) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(0)).getChildAt(j);
                                            } else if (j < mColors.length / 3 * 2) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(1)).getChildAt(j - mColors.length / 3);
                                            } else {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(2)).getChildAt(j - mColors.length / 3 * 2);
                                            }
                                        } else {
                                            if (j < mColors.length / 3 + 1) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(0)).getChildAt(j);
                                            } else if (j < (mColors.length / 3 + 1) * 2) {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(1)).getChildAt(j - (mColors.length / 3 + 1));
                                            } else {
                                                otherColor = (LinearLayout) ((LinearLayout) ((LinearLayout)
                                                        (v.getParent()).getParent()).getChildAt(2)).getChildAt(j - (mColors.length / 3 + 1) * 2);
                                            }
                                        }
                                        otherColor.setBackgroundColor(Color.TRANSPARENT);
                                    }
                                }
                                if (mPropertyChangeListener != null) {
                                    mPropertyChangeListener.onValueChanged(PropertyBar.PROPERTY_COLOR, mColor);
                                }
                            }
                        }
                    });
                } else {
                    linearLayout.setEnabled(false);
                }
            }
        }
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void setEditable(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public void setPropertyChangeListener(PropertyBar.PropertyChangeListener listener) {
        this.mPropertyChangeListener = listener;
    }

    public void reset(){
        removeAllViews();
        init();
    }

}
