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
package com.foxit.uiextensions.controls.popupwindow;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.ColorPicker;
import com.foxit.uiextensions.controls.propertybar.imp.ColorVPAdapter;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;
import java.util.List;


public class ColorPopupWindow extends PopupWindow {
    private Context mContext;

    private LinearLayout mPopupView;
    private LinearLayout mLl_root;
    private LinearLayout mTopShadow;

    private LinearLayout mLlColorlayout;
    private LinearLayout mLlColorContents;

    private int[] mColors;
    private int mColor;

    private ViewPager mColorViewPager;
    private LinearLayout mLlColorDots;
    private LinearLayout mPBLlColors;// ViewPager view1
    private LinearLayout mColorsPickerRoot;// ViewPager view2
    private int[] mColorDotPics = new int[]{R.drawable.pb_ll_colors_dot_selected, R.drawable.pb_ll_colors_dot};
    private int mCurrentColorIndex = 0;

    private IColorChangedListener mColorChangeListener;
    private PropertyBar.DismissListener mDismissListener;
    private int mPadWidth;
    private int mCurrentWidth = 0;
    private boolean mShowMask = false;

    private RectF mRectF;
    private boolean isFullScreen = false;

    private AppDisplay display;
    private ViewGroup mParent = null;

    private boolean canEdit = true;

    public ColorPopupWindow(Context context, ViewGroup parent) {
        this(context, null, parent);
    }

    public ColorPopupWindow(Context context, AttributeSet attrs, ViewGroup parent) {
        this(context, attrs, 0, parent);
    }

    public ColorPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, ViewGroup parent) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mParent = parent;
        display = AppDisplay.getInstance(context);
        initVariable();
        initView();

        if (display.isPad()) {
            setWidth(mPadWidth);
        } else {
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(mPopupView);

        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (!display.isPad()) {
            setAnimationStyle(R.style.PB_PopupAnimation);
        }
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mDismissListener != null) {
                    mDismissListener.onDismiss();
                }

                if (mShowMask) {
                    mShowMask = false;
                }
                if (!display.isPad()) {
                    setPhoneFullScreen(false);
                }
            }
        });
    }

    private void initVariable() {
        mPadWidth = display.dp2px(320.0f);

        int[] colors = new int[PropertyBar.PB_COLORS_TEXT.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXT[0];
        mColors = colors;
        mColor = mColors[0];
        mRectF = new RectF();
    }

    private void initView() {
        mPopupView = new LinearLayout(mContext);
        mPopupView.setOrientation(LinearLayout.VERTICAL);
        mLl_root = new LinearLayout(mContext);
        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLl_root.setOrientation(LinearLayout.VERTICAL);
        mPopupView.addView(mLl_root);

        // ---phone top shadow
        if (!display.isPad()) {
            mTopShadow = new LinearLayout(mContext);
            mTopShadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mTopShadow.setOrientation(LinearLayout.VERTICAL);
            mLl_root.addView(mTopShadow);

            ImageView shadow = new ImageView(mContext);
            shadow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_shadow_height)));
            shadow.setImageResource(R.drawable.search_shadow_bg270);
            mTopShadow.addView(shadow);

            ImageView shadowLine = new ImageView(mContext);
            shadowLine.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            shadowLine.setImageResource(R.color.ux_color_shadow_solid_line);
            mTopShadow.addView(shadowLine);
        }

        LinearLayout mLlCenter = new LinearLayout(mContext);
        mLlCenter.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        mLlCenter.setOrientation(LinearLayout.HORIZONTAL);
        mLl_root.addView(mLlCenter);

        // color view
        mLlColorlayout = new LinearLayout(mContext);
        mLlColorlayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        mLlColorlayout.setOrientation(LinearLayout.VERTICAL);
        if (display.isPad()) {
            mLlColorlayout.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
            mLlColorlayout.setPadding(display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f), display.dp2px(4.0f));
        } else {
            mLlColorlayout.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));
        }
        mLlCenter.addView(mLlColorlayout);

        // add color view
        View view = LayoutInflater.from(mContext).inflate(R.layout.color_popup_window_layout, null, false);
        mLlColorContents = (LinearLayout) view.findViewById(R.id.pw_color_ll_tabContents);
        mLlColorlayout.addView(view);
    }

    public void setPhoneFullScreen(boolean fullScreen) {
        if (!display.isPad()) {
            isFullScreen = fullScreen;

            LinearLayout tabLayout = (LinearLayout) mLlColorContents.getParent();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tabLayout.getLayoutParams();

            LinearLayout.LayoutParams tabContentsLayoutParams = (LinearLayout.LayoutParams) mLlColorContents.getLayoutParams();
            if (fullScreen) {
                setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            } else {
                setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                tabContentsLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            tabLayout.setLayoutParams(layoutParams);
            mLlColorContents.setLayoutParams(tabContentsLayoutParams);
        }
    }

    private View getColorView() {
        View colorItemView = LayoutInflater.from(mContext).inflate(R.layout.pb_color, null, false);
        colorItemView.findViewById(R.id.pb_separator_iv).setVisibility(View.GONE);
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
                if (mColorChangeListener != null) {
                    mColorChangeListener.onValueChanged(value);
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
        return colorItemView;
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
                mCurrentWidth = tempHeight > tempWidth ? tempHeight : tempWidth;
            } else {
                mCurrentWidth = tempWidth < tempHeight ? tempWidth : tempHeight;
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
                            if (mColorChangeListener != null) {
                                mColorChangeListener.onValueChanged(mColor);
                            }

                        }
                    }
                });
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
                            if (mColorChangeListener != null) {
                                mColorChangeListener.onValueChanged(mColor);
                            }
                        }
                    }
                });
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
                            if (mColorChangeListener != null) {
                                mColorChangeListener.onValueChanged(mColor);
                            }
                        }
                    }
                });
            }
        }
    }

    public void reset() {
        for (int i = 0; i < mLlColorContents.getChildCount(); i++) {
            ViewGroup tabContent = (ViewGroup) mLlColorContents.getChildAt(i);
            tabContent.removeAllViews();
        }
        mLlColorContents.removeAllViews();
        mLl_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        resetSupportedView();
    }

    private void resetSupportedView() {
        LinearLayout ll_content = new LinearLayout(mContext);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ll_content.setOrientation(LinearLayout.VERTICAL);
        mLlColorContents.addView(ll_content, 0);
        addCustomItem(getColorView(), 0, -1);
    }

    private void doAfterAddContentItem() {
        resetContentHeight();
        mLlColorContents.getChildAt(0).setVisibility(View.VISIBLE);

    }

    private void resetContentHeight() {
        int maxTabContentHeight = 0;
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        LinearLayout child = (LinearLayout) mLlColorContents.getChildAt(0);
        child.measure(w, h);
        int childHeight = child.getMeasuredHeight();
        if (childHeight > maxTabContentHeight) {
            maxTabContentHeight = childHeight;
        }

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mLlColorContents.getLayoutParams();
        if (!(!display.isPad() && layoutParams.height == LinearLayout.LayoutParams.MATCH_PARENT)) {
            layoutParams.height = maxTabContentHeight;
            mLlColorContents.setLayoutParams(layoutParams);
        }
    }

    public void addCustomItem(View itemView, int tabIndex, int index) {
        if (itemView == null) {
            return;
        }
        if (tabIndex < 0 || tabIndex > mLlColorContents.getChildCount() - 1) {
            return;
        }
        View view = mLlColorContents.getChildAt(tabIndex);
        if (view != null) {
            LinearLayout ll_content = (LinearLayout) view;
            if (index != -1 && (index < 0 || index > ll_content.getChildCount())) {
                return;
            }

            if (index == -1) {
                ll_content.addView(itemView);
            } else {
                if (index < 0 || index > ll_content.getChildCount()) {
                    return;
                }
                ll_content.addView(itemView, index);
            }
        }
        doAfterAddContentItem();
    }

    @Override
    public View getContentView() {
        return super.getContentView();
    }

    public void update(RectF rectF) {
        update(mParent, rectF);
    }

    public void update(View parent, RectF rectF) {
//        showSystemUI();

        mRectF.set(rectF);
        int height = parent.getHeight();
        int width = parent.getWidth();
        if (display.isPad()) {
            int w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            int arrowPosition;
            if (rectF.top >= mLl_root.getMeasuredHeight()) {
                arrowPosition = PropertyBar.ARROW_BOTTOM;
            } else if (height - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                arrowPosition = PropertyBar.ARROW_TOP;
            } else if (width - rectF.right >= mPadWidth) {
                arrowPosition = PropertyBar.ARROW_LEFT;
            } else if (rectF.left >= mPadWidth) {
                arrowPosition = PropertyBar.ARROW_RIGHT;
            } else {
                arrowPosition = PropertyBar.ARROW_CENTER;
            }

            mLlColorlayout.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
            mLlColorlayout.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                    display.dp2px(4.0f), display.dp2px(4.0f));


            mLl_root.measure(w1, h1);
            if (arrowPosition == PropertyBar.ARROW_BOTTOM) {

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                    } else {
                        toLeft = width - mPadWidth;
                    }
                } else {
                    toLeft = 0;
                }

                update(toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()), -1, -1);
            } else if (arrowPosition == PropertyBar.ARROW_TOP) {

                int toLeft;
                if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                    if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                    } else {
                        toLeft = width - mPadWidth;
                    }
                } else {
                    toLeft = 0;
                }

                update(toLeft, (int) rectF.bottom, -1, -1);

            } else if (arrowPosition == PropertyBar.ARROW_LEFT) {

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                    } else {
                        toTop = height - mLl_root.getMeasuredHeight();
                    }
                } else {
                    toTop = 0;
                }

                update((int) (rectF.right), toTop, -1, -1);
            } else if (arrowPosition == PropertyBar.ARROW_RIGHT) {

                int toTop;
                if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                    if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                    } else {
                        toTop = height - mLl_root.getMeasuredHeight();
                    }
                } else {
                    toTop = 0;
                }

                update((int) (rectF.left - mPadWidth), toTop, -1, -1);
            } else if (arrowPosition == PropertyBar.ARROW_CENTER) {
                update((int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f), -1, -1);
            }
        } else {
            mLlColorlayout.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));

            if (Build.VERSION.SDK_INT == 24) {
                int screenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                mLl_root.measure(0, 0);
                int barHeight = mLl_root.getMeasuredHeight();
                int navBarHeight = AppDisplay.getInstance(mContext).getNavBarHeight();

                if (isFullScreen) {
                    update(0, 0, width, screenHeight - navBarHeight);
                } else {
                    update(0, screenHeight - barHeight - navBarHeight, width, -1);
                }
            } else {
                update(0, 0, width, -1);
            }
        }
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }


    public void show(RectF rectF, boolean showMask) {
        if (this != null && !this.isShowing()) {
            show(mParent, rectF, showMask);
        }
    }

    public void show(View parent, RectF rectF, boolean showMask) {
//        showSystemUI();
        mRectF.set(rectF);
        if (this != null && !this.isShowing()) {
            setFocusable(true);

            int height = parent.getHeight();
            int width = parent.getWidth();

            int w1;
            if (display.isPad()) {
                w1 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
            } else {
                w1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            }
            int h1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mLl_root.measure(w1, h1);

            if (display.isPad()) {
                int arrowPosition;
                if (rectF.top >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = PropertyBar.ARROW_BOTTOM;
                } else if (height - rectF.bottom >= mLl_root.getMeasuredHeight()) {
                    arrowPosition = PropertyBar.ARROW_TOP;
                } else if (width - rectF.right >= mPadWidth) {
                    arrowPosition = PropertyBar.ARROW_LEFT;
                } else if (rectF.left >= mPadWidth) {
                    arrowPosition = PropertyBar.ARROW_RIGHT;
                } else {
                    arrowPosition = PropertyBar.ARROW_CENTER;
                }
                mLlColorlayout.setBackgroundResource(R.drawable.pb_popup_bg_shadow);
                mLlColorlayout.setPadding(display.dp2px(4.0f), display.dp2px(4.0f),
                        display.dp2px(4.0f), display.dp2px(4.0f));

                int w2 = View.MeasureSpec.makeMeasureSpec(mPadWidth, View.MeasureSpec.EXACTLY);
                int h2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mLl_root.measure(w2, h2);

                if (arrowPosition == PropertyBar.ARROW_BOTTOM) {

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        } else {
                            toLeft = width - mPadWidth;
                        }
                    } else {
                        toLeft = 0;
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, toLeft, (int) (rectF.top - mLl_root.getMeasuredHeight()));
                } else if (arrowPosition == PropertyBar.ARROW_TOP) {

                    int toLeft;
                    if (rectF.left + (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                        if (width - rectF.left - (rectF.right - rectF.left) / 2.0f > mPadWidth / 2.0f) {
                            toLeft = (int) (rectF.left + (rectF.right - rectF.left) / 2.0f - mPadWidth / 2.0f);
                        } else {
                            toLeft = width - mPadWidth;
                        }
                    } else {
                        toLeft = 0;
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, toLeft, (int) rectF.bottom);
                } else if (arrowPosition == PropertyBar.ARROW_LEFT) {

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        } else {
                            toTop = height - mLl_root.getMeasuredHeight();
                        }
                    } else {
                        toTop = 0;
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP, (int) (rectF.right), toTop);
                } else if (arrowPosition == PropertyBar.ARROW_RIGHT) {

                    int toTop;
                    if (rectF.top + (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                        if (height - rectF.top - (rectF.bottom - rectF.top) / 2.0f > mLl_root.getMeasuredHeight() / 2.0f) {
                            toTop = (int) (rectF.top + (rectF.bottom - rectF.top) / 2.0f - mLl_root.getMeasuredHeight() / 2.0f);
                        } else {
                            toTop = height - mLl_root.getMeasuredHeight();
                        }
                    } else {
                        toTop = 0;
                    }

                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left - mPadWidth), toTop);
                } else if (arrowPosition == PropertyBar.ARROW_CENTER) {
                    showAtLocation(parent, Gravity.LEFT | Gravity.TOP,
                            (int) (rectF.left + (rectF.right - rectF.left) / 4.0f), (int) (rectF.top + (rectF.bottom - rectF.top) / 4.0f));
                }

            } else {
                if (showMask) {
                    mTopShadow.setVisibility(View.GONE);
                } else {
                    mTopShadow.setVisibility(View.VISIBLE);
                }

                mLlColorlayout.setBackgroundColor(mContext.getResources().getColor(R.color.ux_text_color_title_light));

                setWidth(width);
                showAtLocation(parent, Gravity.LEFT | Gravity.BOTTOM, 0, 0);

            }

            mShowMask = showMask;
        }
    }

    @Override
    public void dismiss() {
        if (this != null && this.isShowing()) {
            setFocusable(false);
//            hideSystemUI();
            super.dismiss();
        }
    }

//    private void hideSystemUI(){
//        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager) mParent.getUIExtensionsManager()).getMainFrame();
//        mainFrame.setHideSystemUI(true);
//        if (!mainFrame.isToolbarsVisible()){
//            AppUtil.hideSystemUI(((UIExtensionsManager) mParent.getUIExtensionsManager()).getAttachedActivity());
//        }
//    }
//
//    private void showSystemUI(){
//        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mParent.getUIExtensionsManager();
//        MainFrame mainFrame = (MainFrame)uiExtensionsManager.getMainFrame();
//        if (mainFrame.isToolbarsVisible()){
//            mainFrame.setHideSystemUI(false);
//        } else {
//            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
//        }
//    }


    public void setColors(int[] colors) {
        this.mColors = colors;
    }

    public void setValue(int value) {
        mColor = value;
        int r = Color.red(mColor);
        int g = Color.green(mColor);
        int b = Color.blue(mColor);
        for (int i = 0; i < mColors.length; i++) {
            int r2 = Color.red(mColors[i]);
            int g2 = Color.green(mColors[i]);
            int b2 = Color.blue(mColors[i]);
            if (Math.abs(r2 - r) <= 3 && Math.abs(g2 - g) <= 3 && Math.abs(b2 - b) <= 3) {
                mColor = mColors[i];
                break;
            }
        }
    }

    public IColorChangedListener getColorChangedListener() {
        return this.mColorChangeListener;
    }

    public void setColorChangedListener(IColorChangedListener listener) {
        this.mColorChangeListener = listener;
    }

    public interface IColorChangedListener {
        void onValueChanged(int value);
    }
}
